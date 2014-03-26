package org.jasper.core.delegate;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
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
import org.jasper.core.delegate.handlers.SparqlHandler;
import org.jasper.core.notification.triggers.Trigger;
import org.jasper.core.notification.triggers.TriggerFactory;
import org.jasper.core.persistence.PersistedObject;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Type;
import org.json.JSONException;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
	private Map<String,PersistedObject> sharedData;
	private String key;
	private String errorTxt;
	private BlockingQueue<PersistedObject> workQueue;
	private String contentType;
	private String version;
	private String notification;
	private String output;
	private String dtaParms;
	private String ruri;
	private String method;
	private String subscriptionId;
	private int expires;
	private int pollPeriod;
	private List<Trigger> triggerList;
	private static final int MILLISECONDS = 1000;
	private PersistedObject statefulData;
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
		
		workQueue  = ude.getCachingSys().getQueue("tasks");
		sharedData = (Map<String, PersistedObject>) ude.getCachingSys().getMap("sharedData");
		
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
						persistData((TextMessage)jmsRequest);
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
		JsonObject jasperResponse = new JsonObject();
		Map<String,String> map = new HashMap<String,String>();
		Gson gson = new Gson();
		
		jasperResponse.addProperty(JasperConstants.CODE_LABEL,  respCode.getCode());
		jasperResponse.addProperty(JasperConstants.REASON_LABEL,  respCode.getDescription());
		jasperResponse.addProperty(JasperConstants.DESCRIPTION_LABEL,  respMsg);
		
		if(version != null){
			jasperResponse.addProperty(JasperConstants.VERSION_LABEL,  version);
		}
		else{
			jasperResponse.addProperty(JasperConstants.VERSION_LABEL,  "unknown");
		}
		if(contentType != null){
			map.put(JasperConstants.CONTENT_TYPE_LABEL, contentType);
		}
		else{
			map.put(JasperConstants.CONTENT_TYPE_LABEL, "application/json");
		}
		
		JsonObject headers = new JsonObject();
		JsonElement jsonTree = gson.toJsonTree(map, Map.class);
		
		jasperResponse.add(JasperConstants.HEADERS_LABEL, jsonTree);
		
		if(response != null){
			byte[] bytes;
			try {
				bytes = response.getBytes(JasperConstants.ENCODING_LABEL);
				JsonParser parser = new JsonParser();
				JsonArray payloadArray = parser.parse(gson.toJson(bytes)).getAsJsonArray();
				jasperResponse.add(JasperConstants.PAYLOAD_LABEL, payloadArray);
			} catch (UnsupportedEncodingException e) {
				logger.error("Exception occurred while encoding response " + e);
				return null;
			}
		}
		
		return jasperResponse.toString();
		
	}
	
	private void processInvalidRequest(TextMessage msg, JasperConstants.responseCodes responseCode, String responseMsg) throws Exception {
		if(logger.isInfoEnabled()){
			logger.info("processingInvalidRequest, errorMsg = " + responseMsg + " for request " + msg.getText() + " from " + msg.getJMSReplyTo());
		}
		String response = createJasperResponse(responseCode, responseMsg, null, contentType, version);
        Message message = createTextMessage(response);

        
        if(msg.getJMSCorrelationID() == null){
            message.setJMSCorrelationID(msg.getJMSMessageID());
  	  	}else{
            message.setJMSCorrelationID(msg.getJMSCorrelationID());
  	  	}
        
        sendMessage(msg.getJMSReplyTo(),message);
        removeSharedData();
	}
	
	private void persistData(TextMessage txtMsg) throws Exception{
		String request = txtMsg.getText();
		boolean requestOK = false;
		key = txtMsg.getJMSCorrelationID();
		
		 if(request == null || request.length() == 0){
	  	    	processInvalidRequest(txtMsg, JasperConstants.responseCodes.BADREQUEST, "Invalid request received - request is null or empty string");
	  	    	return;
	  	    }
		 
		 requestOK =  parseJasperRequest(request);

		 if (!requestOK){
			 processInvalidRequest(txtMsg, JasperConstants.responseCodes.BADREQUEST, errorTxt);
			 return;
		 }
		 
		 if(ruri.length() == 0){
	  	    	processInvalidRequest(txtMsg, JasperConstants.responseCodes.BADREQUEST, "Invalid request received - request does not contain a URI");
	  	    	return;
	  	    }
		 
		// create object that contains stateful data
		 statefulData = new PersistedObject(key, txtMsg.getJMSCorrelationID(), request, ruri, dtaParms,
				 txtMsg.getJMSReplyTo(), false, null, output, version, contentType, method, expires);
		 
		 if(subscriptionId != null){
			 statefulData.setSubscriptionId(subscriptionId);
		 }
	  	   
		 if(triggerList != null){
			 statefulData.setTriggers(triggerList);
			 statefulData.setNotification(notification);
			 statefulData.setIsNotificationRequest(true);
		 }

		 sharedData.put(key, statefulData);
		 workQueue.offer(statefulData);
		 cleanup();
	}
	
	private void removeSharedData(){
		sharedData.remove(key);
	}
	
	private boolean parseJasperRequest(String req) {
		boolean validMsg = false;
		expires = -1;
		pollPeriod = -1;
		JsonObject parms = new JsonObject();
		StringBuilder sb = new StringBuilder();
		
		try {
			JsonElement jelement = new JsonParser().parse(req);
			JsonObject jsonObj = jelement.getAsJsonObject();
			// parse out mandatory parameters
			ruri = jsonObj.get(JasperConstants.REQUEST_URI_LABEL).getAsString();
			version = jsonObj.get(JasperConstants.VERSION_LABEL).getAsString();
			method = jsonObj.get(JasperConstants.METHOD_LABEL).getAsString();
			
			if(ruri != null && version != null && method != null) {
				validMsg = true;
			}	
			
			if(!isValidMethod(method)){
				validMsg = false;
				errorTxt = ("Invalid request type: " + method);
			}
			
			if(jsonObj.has(JasperConstants.PARAMETERS_LABEL)) {
				parms = jsonObj.getAsJsonObject(JasperConstants.PARAMETERS_LABEL);
				
				int len = parms.entrySet().size();
				for (Entry<String, JsonElement> key_val: parms.entrySet()) {
    	            sb.append(key_val.getKey()).append("=").append(key_val.getValue().getAsString());
					if(len > 1) {
						sb.append("&");
						len--;
					}
				}
				
				if(sb.indexOf("parmsArray") < 0){
					dtaParms = parms.toString();
				}
				else{
					dtaParms = sb.toString();
				}
			}
			
			if(jsonObj.has(JasperConstants.HEADERS_LABEL)){
				Map<String, String> headers = getMap(jsonObj.get(JasperConstants.HEADERS_LABEL).getAsJsonObject());
				for(String s:headers.keySet()){
					switch (s.toLowerCase()) {
					case JasperConstants.POLL_PERIOD_LABEL :
						try{
							pollPeriod = Integer.parseInt(headers.get(s));
							pollPeriod = (pollPeriod * MILLISECONDS); // convert to milliseconds
						}catch(JSONException ex){
							pollPeriod = maxPollingInterval;
						}
						break;
					case JasperConstants.EXPIRES_LABEL :
						try{
							expires = Integer.parseInt(headers.get(s));
							expires = (expires * MILLISECONDS); // convert to milliseconds
						} catch(JSONException ex){
							expires = maxExpiry;
						}
						break;
					case JasperConstants.CONTENT_TYPE_LABEL :
						contentType = headers.get(s);
						break;
					case JasperConstants.SUBSCRIPTION_ID_LABEL :
						subscriptionId = headers.get(s);
						break;
					case JasperConstants.RESPONSE_TYPE_LABEL :
						output = headers.get(s);
						break;
					}
				}
					
			}
			
			if(jsonObj.has(JasperConstants.RULE_LABEL)){
				notification = jsonObj.get(JasperConstants.RULE_LABEL).getAsString();
				triggerList = new ArrayList<Trigger>();
			}
			
			if(notification != null){
				if(expires == -1) expires = maxExpiry; // if not supplied set to max
				if(expires > maxExpiry) expires = maxExpiry;
				if(pollPeriod == -1) pollPeriod = maxPollingInterval; // if not supplied set to max
				if(pollPeriod < minPollingInterval) pollPeriod = minPollingInterval;
				if(pollPeriod > maxPollingInterval) pollPeriod = maxPollingInterval;
				if(pollPeriod > expires) pollPeriod = (int) expires;
				if(output == null) output = defaultOutput;
				
				parseTrigger(notification);
			}
			
		} catch (JSONException e) {
			logger.error("Exception caught while creating JSONObject " + e);
			validMsg = false;
			errorTxt = "Invalid / Malformed JSON object received";
		}
		
		return validMsg;
		
	}
		
		private Map<String, String> getMap(JsonObject json) {
			Map<String, String> map = new HashMap<String, String>();
			for(Entry<String, JsonElement> entry:json.entrySet()){
				map.put(entry.getKey(),entry.getValue().getAsString());
			}
			return map;
		}
	
	/*
	 * Parses out the different trigger types from the inbound notification string
	 * Will create and link a trigger for each function in the inbound request
	 */
	private void parseTrigger(String notification){
		String[] triggers = notification.split("&");
		String[] tmp = new String[triggers.length];
		String[] parms = new String[triggers.length];
		String[] functions = new String[triggers.length];
		for(int i=0;i<triggers.length;i++){
			tmp = triggers[i].split("\\(");
			if(tmp[0] != null) {
				functions[i] = tmp[0];
				parms[i] = tmp[1];
				parms[i] = parms[i].replaceFirst("\\)", "");
			}
			
		}
		TriggerFactory factory = new TriggerFactory();
		Trigger trigger;
		String[] triggerParms;
		for(int i=0; i<functions.length;i++){
			triggerParms = parms[i].split(",");
			trigger = factory.createTrigger(functions[i], expires, pollPeriod, triggerParms);
			if(trigger != null){
				trigger.setNotificationExpiry();
				triggerList.add(trigger);
			}
			else{
				logger.error("Invalid notification request received - cannot create rule: " + triggerParms.toString());
			}
		}		
	}
	
	private boolean isValidMethod(String method){
		if((!method.equalsIgnoreCase(JasperConstants.GET)) && (!method.equalsIgnoreCase(JasperConstants.POST))
				&& (!method.equalsIgnoreCase(JasperConstants.SUBSCRIBE)) && (!method.equalsIgnoreCase(JasperConstants.PUBLISH))){
			return false;
		}
		return true;
	}
	
	private void cleanup(){
		key            = null;
		contentType    = null;
		version        = null;
		dtaParms       = null;
		notification   = null;
		triggerList    = null;
		subscriptionId = null;
	}
	
}