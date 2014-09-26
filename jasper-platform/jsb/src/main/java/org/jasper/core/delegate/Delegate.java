package org.jasper.core.delegate;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.log4j.Logger;
import org.jasper.core.UDE;
import org.jasper.core.acl.pep.soap.JasperPEP;
import org.jasper.core.constants.JasperConstants;
import org.jasper.core.constants.JasperConstants.ResponseCodes;
import org.jasper.core.constants.JasperOntologyConstants;
import org.jasper.core.delegate.handlers.AdminHandler;
import org.jasper.core.delegate.handlers.DataRequestHandler;
import org.jasper.core.delegate.handlers.SparqlHandler;
import org.jasper.core.notification.triggers.Trigger;
import org.jasper.core.persistence.PersistedDataRequest;
import org.jasper.core.persistence.PersistedSubscriptionRequest;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Type;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hazelcast.core.MultiMap;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class Delegate {

	private Session globalSession;
	private Queue globalQueue;
	private MessageConsumer globalDelegateConsumer;
	private Session jtaSession;
	private MessageProducer producer;
	private Destination delegateQ;
	private MessageConsumer responseConsumer;

	private Map<String, Message> responseMessages;
	private Map<String, Object> locks;
	private DelegateOntology jOntology;


	public String defaultOutput;
	private MultiMap<String,PersistedDataRequest> distributedDataStore;
	private MultiMap<String, PersistedSubscriptionRequest>	persistedSubscriptions;

	public int maxExpiry;
	public int maxPollingInterval;
	public int minPollingInterval;

	Properties prop = new Properties();

	private Connection connection = null;
	private UDE ude;
	private ExecutorService delegateRequestThreadPool;

	static Logger logger = Logger.getLogger(Delegate.class.getName());
	static private AtomicInteger count = new AtomicInteger(0);

	public Delegate(UDE ude){

		this.responseMessages = new ConcurrentHashMap<String, Message>();
		this.locks = new ConcurrentHashMap<String, Object>();
		this.ude = ude;
		jOntology = new DelegateOntology(ude.getCachingSys(), createJasperOntModel());		
	}

    private OntModel createJasperOntModel() {
    	OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        for(String prefix:JasperOntologyConstants.PREFIX_MAP.keySet()){
        	model.setNsPrefix(prefix, JasperOntologyConstants.PREFIX_MAP.get(prefix));
        }
        return model;
    }
    

	public void start() throws JMSException{

    	ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost");
        // Create a Connection
        connectionFactory.setUserName(JasperConstants.JASPER_ADMIN_USERNAME);
        connectionFactory.setPassword(JasperConstants.JASPER_ADMIN_PASSWORD);
        connection = connectionFactory.createConnection();
        connection.start();
        
		delegateRequestThreadPool = Executors.newFixedThreadPool(5);

		globalSession = connection.createSession(false,Session.AUTO_ACKNOWLEDGE);
		globalQueue = globalSession.createQueue(JasperConstants.DELEGATE_GLOBAL_QUEUE);
		globalDelegateConsumer = globalSession.createConsumer(globalQueue);
		MessageListener globalListener = new MessageListener() {

			@Override
			public void onMessage(Message msg) {
				processGlobalQMsg(msg);

			}
		};
		globalDelegateConsumer.setMessageListener(globalListener);

		jtaSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		producer = jtaSession.createProducer(null);
		producer.setDeliveryMode(DeliveryMode.PERSISTENT);
		producer.setTimeToLive(30000);

		delegateQ = jtaSession.createQueue("jms.delegate." + ude.getBrokerTransportIp() + "." + count.getAndIncrement() + ".queue");
		responseConsumer = jtaSession.createConsumer(delegateQ);
		MessageListener delegateQListener = new MessageListener() {

			@Override
			public void onMessage(Message msg) {
				processDelegateQMsg(msg);	
			}
		};
		responseConsumer.setMessageListener(delegateQListener);

		distributedDataStore = ude.getCachingSys().getMultiMap("distributedDataStore");	
		persistedSubscriptions = ude.getCachingSys().getMultiMap("persistedSubscriptions");

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
		delegateRequestThreadPool.shutdown();
		try {
			delegateRequestThreadPool.awaitTermination(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			logger.error("delegateRequestThreadPool interrupted",e);
		}

		producer.close();
		responseConsumer.close();
		jtaSession.close();
		globalDelegateConsumer.close();
		globalSession.close();
		connection.stop();
		connection.close();
	}

	public Session getGlobalSession() {
		return globalSession;
	}

	public Map<String, Object> getLocksMap() {
		return locks;
	}

	public Map<String, Message> getResponsesMap() {
		return responseMessages;
	}

	public DelegateOntology getJOntology() {
		return jOntology;
	}
	
	public JasperPEP getPep(){
		return ude.getPep();
	}

	protected void processGlobalQMsg(Message jmsRequest) {
		try{		
			if(jmsRequest instanceof ObjectMessage && ((ObjectMessage)jmsRequest).getObject() instanceof JasperAdminMessage && ((JasperAdminMessage)((ObjectMessage)jmsRequest).getObject()).getType() == Type.ontologyManagement){
				if(logger.isInfoEnabled()) logger.info("received Admin request : " + ((JasperAdminMessage)((ObjectMessage)jmsRequest).getObject()).toString());
				delegateRequestThreadPool.submit(new AdminHandler(this, jmsRequest));
			}else if (jmsRequest instanceof TextMessage && ((TextMessage) jmsRequest).getText().contains("sparql") ){
				delegateRequestThreadPool.submit(new SparqlHandler(this, jmsRequest));
			}else if (jmsRequest instanceof TextMessage	&& ((TextMessage) jmsRequest).getText() !=null){
				PersistedDataRequest pData = null;
				try {
					pData  = persistDataRequest((TextMessage)jmsRequest);
				} catch (Exception e) {
					logger.error("unable to persist data request",e);
				}
				delegateRequestThreadPool.submit(new DataRequestHandler(this,pData));
			} else {
				logger.warn("JMS Message neither ObjectMessage nor TextMessage, ignoring request : " + jmsRequest);
			}
		}catch (JMSException jmse){
			logger.error("error occured in processGlobalQMsg", jmse);
		}	
	}

	public void removePersistedRequest(PersistedDataRequest request){
		if(logger.isDebugEnabled()) logger.debug("persisted request removed : " + request.getCorrelationID());
		distributedDataStore.remove(ude.getUdeInstance(), request);
	}

	public PersistedDataRequest persistDataRequest(TextMessage jmsRequest) throws JMSException {
		PersistedDataRequest persistedDataRequest = new PersistedDataRequest(jmsRequest.getJMSCorrelationID(), jmsRequest.getJMSReplyTo(), jmsRequest.getText(), System.currentTimeMillis());
		distributedDataStore.put(ude.getUdeInstance(), persistedDataRequest);
		if(logger.isDebugEnabled()) logger.debug("persisted request added : " + persistedDataRequest.getCorrelationID());
		return persistedDataRequest;

	}

	public void persistSubscriptionRequest(String ruri, String subscriptionId, String correlationID, String responseType, Destination reply2q, List<Trigger> triggerList, int expiry) {
		for(PersistedSubscriptionRequest entry:persistedSubscriptions.get(ruri)){
			if(entry.getSubscriptionId().equals(subscriptionId)){
				persistedSubscriptions.remove(ruri, entry);
				if(logger.isInfoEnabled()) logger.info("updating subscription, removing old and will add new for ruri : " + ruri + " and subscriptionId : " + subscriptionId);
			}
		}
		PersistedSubscriptionRequest persistedSubscriptionRequest = new PersistedSubscriptionRequest(ruri,subscriptionId,correlationID,responseType,reply2q,triggerList,expiry, System.currentTimeMillis());
		persistedSubscriptions.put(ruri, persistedSubscriptionRequest);
	}

	public void removePersistedSubscriptionRequest(String ruri, String subscriptionId) {
		for(PersistedSubscriptionRequest entry:persistedSubscriptions.get(ruri)){
			if(entry.getSubscriptionId().equals(subscriptionId)){
				persistedSubscriptions.remove(ruri, entry);
				return;
			}
		}
		logger.warn("subscription : " + subscriptionId + " for ruri " + ruri + " not found in persistedSubscriptions map, ignoring request");
	}

	public Collection<PersistedSubscriptionRequest> getDataSubscriptions(String ruri){
		return persistedSubscriptions.get(ruri);
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

	public ObjectMessage createObjectMessage(Serializable obj) throws JMSException {
		return jtaSession.createObjectMessage(obj);
	}

	protected void processDelegateQMsg(Message msg) {
		String correlationId = null;
		try {
			correlationId = msg.getJMSCorrelationID();
			if (logger.isDebugEnabled()) {
				logger.debug("Message response received for correlationID " + correlationId + " = " + msg);
			}

			if (locks.containsKey(correlationId)) {
				responseMessages.put(correlationId, msg);
				Object lock = locks.remove(correlationId);
				synchronized (lock) {
					lock.notifyAll();
				}
			} else {
				logger.error("response with correlationID = " + correlationId
						+ " received however no record of sending message with this ID, ignoring");
			}

		} catch (JMSException jmse) {
			logger.error("error occured in processDelegateQMsg for CorrelationID " + correlationId, jmse);
		}		
	}

	public String createJasperResponse(ResponseCodes respCode, String respMsg, String response, String contentType, String version) {
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

	public void connectionToRemoteUdeLost(String udeInstance) {
		Collection<PersistedDataRequest> failoverRequests = distributedDataStore.remove(udeInstance);
		logger.error("Connection to remote ude lost for " + udeInstance + " getting failoverRequests : " + failoverRequests);
		if(failoverRequests==null)return;
		for(PersistedDataRequest entry:failoverRequests){
			distributedDataStore.put(ude.getUdeInstance(), entry);
			delegateRequestThreadPool.submit(new DataRequestHandler(this,entry));
			if(logger.isInfoEnabled()) logger.info("submitted failed over request : " + entry);
		}
	}

}