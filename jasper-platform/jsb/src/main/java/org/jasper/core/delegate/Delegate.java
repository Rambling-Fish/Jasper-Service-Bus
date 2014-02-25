package org.jasper.core.delegate;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.log4j.Logger;
import org.jasper.core.UDE;
import org.jasper.core.constants.JasperConstants;
import org.jasper.core.constants.JasperConstants.responseCodes;
import org.jasper.core.delegate.handlers.AdminHandler;
import org.jasper.core.delegate.handlers.DataConsumer;
import org.jasper.core.delegate.handlers.DataHandler;
import org.jasper.core.delegate.handlers.SparqlHandler;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Type;
import org.json.JSONArray;
import org.json.JSONObject;

import com.hp.hpl.jena.rdf.model.Model;

public class Delegate implements Runnable, MessageListener {

	
	private boolean isShutdown;
	private Session globalSession;
	private Queue globalQueue;
	private MessageConsumer globalDelegateConsumer;
	private Session jtaSession;
	private MessageProducer producer;
	private Destination delegateQ;
	private MessageConsumer responseConsumer;
	private ExecutorService delegateHandlers;
	private ExecutorService dataConsumerService;
	private DataConsumer[] dataConsumers;

	private Map<String, Message> responseMessages;
	private Map<String, Object> locks;
	private DelegateOntology jOntology;
	
	
	public String defaultOutput;
	public int maxExpiry;
	public int maxPollingInterval;
	public int minPollingInterval;
	// used to engineer the number of data consumers to start per delegate
	private static int numDataConsumers = 1;
	Properties prop = new Properties();
	private UDE ude;

	static Logger logger = Logger.getLogger(Delegate.class.getName());
	static private AtomicInteger count = new AtomicInteger(0);

	public Delegate(UDE ude, Connection connection, Model model,DelegateOntology jOntology) throws JMSException{
		this.isShutdown = false;

		this.ude = ude;
		
		this.responseMessages = new ConcurrentHashMap<String, Message>();
		this.locks = new ConcurrentHashMap<String, Object>();

		delegateHandlers = Executors.newFixedThreadPool(2);
		dataConsumerService = Executors.newCachedThreadPool();
		this.jOntology = jOntology;


		globalSession = connection.createSession(true,Session.SESSION_TRANSACTED);
		globalQueue = globalSession.createQueue(JasperConstants.DELEGATE_GLOBAL_QUEUE);
		globalDelegateConsumer = globalSession.createConsumer(globalQueue);

		jtaSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		producer = jtaSession.createProducer(null);
		producer.setDeliveryMode(DeliveryMode.PERSISTENT);
		producer.setTimeToLive(30000);

		delegateQ = jtaSession.createQueue("jms.delegate." + ude.getBrokerTransportIp() + "." + count.getAndIncrement() + ".queue");
		responseConsumer = jtaSession.createConsumer(delegateQ);
		responseConsumer.setMessageListener(this);
		
		dataConsumers = new DataConsumer[numDataConsumers];
		
		for(int i=0;i<dataConsumers.length;i++){
			dataConsumers[i] = new DataConsumer(ude, this,jOntology,locks,responseMessages);
			dataConsumerService.execute(dataConsumers[i]);
		}
		 try {
	          //load properties file
	    		prop.load(new FileInputStream(System.getProperty("delegate-property-file")));
	    		defaultOutput = prop.getProperty("defaultOutput", "json");
	    		maxExpiry = Integer.parseInt(prop.getProperty("maxNotificationExpiry","60000"));
	    		maxPollingInterval = Integer.parseInt(prop.getProperty("maxPollingInterval","60000"));
	    		minPollingInterval = Integer.parseInt(prop.getProperty("minPollingInterval","2000"));
	    	} catch (IOException ex) {
	    		ex.printStackTrace();
	    	}
	}

	public void shutdown() throws JMSException {
		isShutdown = true;
		for(DataConsumer d:dataConsumers){
			try {
				d.shutdown();
			} catch (JMSException ex) {
				logger.error("jmsconnection caught while shutting down data consumers",ex);
			}
		}
		dataConsumerService.shutdown();
		producer.close();
		responseConsumer.close();
		jtaSession.close();
		globalDelegateConsumer.close();
		globalSession.close();
	}

	public void run() {
		do {
			try {
				Message jmsRequest;
				do {
					jmsRequest = globalDelegateConsumer.receive(500);
				} while (jmsRequest == null && !isShutdown);
				
				if (isShutdown)	break;

				if (jmsRequest instanceof ObjectMessage) {
					ObjectMessage objMessage = (ObjectMessage) jmsRequest;
					Object obj = objMessage.getObject();
					if (obj instanceof JasperAdminMessage) {
						JasperAdminMessage jam = ((JasperAdminMessage) obj);
						if (jam.getType() == Type.ontologyManagement) {
							delegateHandlers.submit(new AdminHandler(this,jOntology, jmsRequest, locks,responseMessages));
							globalSession.commit();
						}
					}
				} else if (jmsRequest instanceof TextMessage) {
					String text = ((TextMessage) jmsRequest).getText();
					if (text != null && text.contains("query")) {
						delegateHandlers.submit(new SparqlHandler(this,jOntology, jmsRequest));
						globalSession.commit();
					} else if (text != null) {
						delegateHandlers.submit(new DataHandler(ude, this, jmsRequest));
						globalSession.commit();

					} else {
						logger.error("Incoming text message has null payload - ignoring " + jmsRequest);
					}
				} else {
					logger.warn("JMS Message neither ObjectMessage nor TextMessage, ignoring request : " + jmsRequest);
				}
			} catch (Exception e) {
				logger.error("Exception caught while listening for request in delegate : ",e);
			}
		} while (!isShutdown);

	}

	public void onMessage(Message msg) {
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Message response received = " + msg);
			}

			if (locks.containsKey(msg.getJMSCorrelationID())) {
				responseMessages.put(msg.getJMSCorrelationID(), msg);
				Object lock = locks.remove(msg.getJMSCorrelationID());
				synchronized (lock) {
					lock.notifyAll();
				}
			} else {
				logger.error("response with correlationID = " + msg.getJMSCorrelationID()
						+ " received however no record of sending message with this ID, ignoring");
			}

		} catch (JMSException jmse) {
			logger.error("error occured in onMessage", jmse);
		}
	}

	public void sendMessage(Destination destination, Message message) throws JMSException {
		message.setJMSReplyTo(delegateQ);
		producer.send(destination, message);
	}

	public void sendMessage(String destination, Message message) throws JMSException {
		this.sendMessage(jtaSession.createQueue(destination), message);
	}

	public TextMessage createTextMessage(String txt) throws JMSException {
		return jtaSession.createTextMessage(txt);
	}

	public ObjectMessage createObjectMessage(Serializable obj)
			throws JMSException {
		return jtaSession.createObjectMessage(obj);
	}

	public MapMessage createMapMessage(Map<String, Serializable> map) throws JMSException {
		MapMessage mapMsg = jtaSession.createMapMessage();
		for (String key : map.keySet()) {
			mapMsg.setObject(key, map.get(key));
		}
		return mapMsg;
	}
	
	public String createJasperResponse(responseCodes respCode, String respMsg, String response, String contentType, String version) {
		JSONObject jasperResponse = new JSONObject();
		Map<String,String> map = new HashMap<String,String>();
		
		jasperResponse.put(JasperConstants.CODE_LABEL,  respCode.getCode());
		jasperResponse.put(JasperConstants.REASON_LABEL,  respCode.getDescription());
		jasperResponse.put(JasperConstants.DESCRIPTION_LABEL,  respMsg);
		
		if(version != null){
			jasperResponse.put(JasperConstants.VERSION_LABEL,  version);
		}
		else{
			jasperResponse.put(JasperConstants.VERSION_LABEL,  "unknown");
		}
		if(contentType != null){
			map.put(JasperConstants.CONTENT_TYPE_LABEL, contentType);
		}
		else{
			map.put(JasperConstants.CONTENT_TYPE_LABEL, "application/json");
		}
		JSONObject headers = new JSONObject(map);
		jasperResponse.put(JasperConstants.HEADERS_LABEL, map);
		
		if(response != null){
			byte[] bytes;
			try {
				bytes = response.getBytes(JasperConstants.ENCODING_LABEL);
				JSONArray payloadArray = new JSONArray(bytes);
				jasperResponse.put(JasperConstants.PAYLOAD_LABEL, payloadArray);
			} catch (UnsupportedEncodingException e) {
				logger.error("Exception occurred while encoding response " + e);
				return null;
			}
		}
		
		return jasperResponse.toString();
		
	}
}