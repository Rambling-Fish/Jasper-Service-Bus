package org.jasper.jsc;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.JMSSecurityException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.log4j.Logger;
import org.jasper.jsc.constants.RequestConstants;
import org.jasper.jsc.constants.RequestHeaders;
import org.jasper.jsc.constants.ResponseConstants;
import org.jasper.jsc.constants.ResponseHeaders;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Jsc {

	private static final String DELEGATE_GLOBAL_QUEUE = "jms.jasper.delegate.global.queue";

	private static final long AUDIT_TIME_IN_MILLISECONDS = 15000;

	private static Logger log = Logger.getLogger(Jsc.class.getName());
	
	private Properties prop;
	
	private Connection connection = null;
	private Session session = null;
	private Queue globalDelegateQueue;
	private MessageProducer producer;
	private Queue jscSyncQueue;
	private Queue jscAsyncQueue;
	private MessageConsumer syncResponseConsumer;
	private MessageConsumer asyncResponseConsumer;
	private Map<String,Message> responses;
	private Map<String,Object> locks;
	
	private ScheduledExecutorService mapAuditExecutor;
	
	private ExecutorService asyncEventHandlingExecutor;
	
	private int jscTimeout = 0;
	private int jscPollPeriod = 0;

	private Map<Listener, Request> listeners;
	private Map<String, Listener> asyncResponses;
	
	public Jsc(Properties prop){
		this.prop = prop;
	}
	
	class AsyncReponseHandler implements Runnable{
		private Listener listener;
		private Response response;

		public AsyncReponseHandler(Listener listener, Response response) {
			this.listener = listener;
			this.response = response;
		}
		
		public void run(){
			listener.processMessage(response);
			log.info("JSC: TIMECHECK request from UDE completed at " + System.currentTimeMillis());
		}
	}
	
	public void init() throws JMSException {

		listeners = new ConcurrentHashMap<Listener, Request>();
		asyncResponses = new ConcurrentHashMap<String, Listener>();
		
    	responses = new ConcurrentHashMap<String, Message>();
    	locks = new ConcurrentHashMap<String, Object>();
    	
    	String user = prop.getProperty("jsc.username");
    	String password = prop.getProperty("jsc.password");
    	String timeout  = prop.getProperty("jsc.timeout", "60000");
    	String pollPeriod = prop.getProperty("jsc.poll-period", "2000");
 
    	try{
    		jscTimeout = Integer.parseInt(timeout);
    	} catch (NumberFormatException ex) {
    		jscTimeout = 60000; // set to 60 seconds on error
    	}
    	
    	try{
    		jscPollPeriod = Integer.parseInt(pollPeriod);
    	} catch (NumberFormatException ex) {
    		jscPollPeriod = 2000; // set to 2 seconds on error
    	}
 
		try {
			String transportURL = prop.getProperty("jsc.transport");
			ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(transportURL);
			connection = connectionFactory.createConnection(user, password);
			connection.setExceptionListener(new ExceptionListener() {
				public void onException(JMSException arg0) {
					log.error("Exception caught in JSC, ignoring : ", arg0);
				}
			});
			
			if(log.isInfoEnabled()){
				log.info("Queue Connection successfully established with " + prop.getProperty("jsc.transport"));
			}
			
		} catch (JMSSecurityException se) {
			log.error("client authentication failed due to an invalid user name or password.", se);
			throw se;
		} catch (JMSException e) {
			log.error("the JMS provider failed to create the queue connection ", e);
			throw e;
		}	
		
		try {
			connection.start();
			session = connection.createSession(false,Session.AUTO_ACKNOWLEDGE);
			globalDelegateQueue = session.createQueue(DELEGATE_GLOBAL_QUEUE);
			
			producer = session.createProducer(globalDelegateQueue);
			producer.setDeliveryMode(DeliveryMode.PERSISTENT);
			producer.setTimeToLive(30000);
			
			jscSyncQueue = session.createQueue("jms.jsc." + System.nanoTime() + ".sync.queue");
	        syncResponseConsumer = session.createConsumer(jscSyncQueue);
	        MessageListener messageListenerForSyncResponse = new MessageListener() {
				
				public void onMessage(Message msg) {
					onMessageForSyncResponse(msg);
				}
			};
			syncResponseConsumer.setMessageListener(messageListenerForSyncResponse);
			
			jscAsyncQueue = session.createQueue("jms.jsc." + System.nanoTime() + ".async.queue");
	        asyncResponseConsumer = session.createConsumer(jscAsyncQueue);
	        MessageListener messageListenerForAsyncResponse = new MessageListener() {
				
				public void onMessage(Message msg) {
					onMessageForAsyncResponse(msg);
				}
			};
			asyncResponseConsumer.setMessageListener(messageListenerForAsyncResponse);

			
		} catch (JMSException e) {
			log.error("Exception when connecting to UDE",e);
			throw e;
		}
		
		asyncEventHandlingExecutor = Executors.newFixedThreadPool(2);
		
		mapAuditExecutor = Executors.newSingleThreadScheduledExecutor();
		Runnable command = new Runnable() {
			public void run() {
				auditMap();
			}
		};;;
		
		mapAuditExecutor.scheduleAtFixedRate(command , AUDIT_TIME_IN_MILLISECONDS, AUDIT_TIME_IN_MILLISECONDS, TimeUnit.MILLISECONDS);
		
    }
	
    public void destroy(){

    	for(Listener listener:listeners.keySet()){
    		listener.processMessage(new Response(404, "Not Found", "JSC in process of shutting down", null, null));
    		listeners.remove(listener);
    	}
    	
    	try {
        	mapAuditExecutor.shutdown();
			mapAuditExecutor.awaitTermination(AUDIT_TIME_IN_MILLISECONDS, TimeUnit.MILLISECONDS);
		} catch (InterruptedException ie) {
			log.error("mapAuditExecutor failed to terminate, forcing shutdown",ie);
		}finally{
			if(!mapAuditExecutor.isShutdown()) mapAuditExecutor.shutdownNow();
		}
    	
		try {
	    	syncResponseConsumer.close();
	    	asyncResponseConsumer.close();
	    	producer.close();
	    	session.close();
	    	connection.stop();
	    	connection.close();
		} catch (JMSException e) {
			log.error("Exception when destroying servlet and cleaning resources conencted to jasper",e);
		}
		
    }

	private void auditMap() {
		synchronized (responses) {
			long currentTime = System.currentTimeMillis();
			for(String key:responses.keySet()){
				try {
					if((responses.get(key).getJMSTimestamp() + AUDIT_TIME_IN_MILLISECONDS) > currentTime){
						log.warn("Map audit found response that has timed out and weren't forwarded to JSC, removing response from map and droping response for JMSCorrelationID : " + key);
						responses.remove(key);
						locks.remove(key).notifyAll();
					}
				} catch (JMSException e) {
					log.error("Exception caught when getting JMSExpiration",e);
				}
			}
		}
		
	}
	
	public Response get(Request request){
		if(request.getMethod() != Method.GET){
			log.error("400 Bad Request returned. Incorrect method type, expecting GET but received " + request.getMethod());
			return new Response(400, "Bad Request", "Incorrect method type, expecting GET but received " + request.getMethod() ,null,null);
		}
		if(request.getRuri().length() < 1){
			log.error("400 Bad Request returned. Request does not contain a valid RURI");
			return new Response(400, "Bad Request", "Request does not contain a valid RURI", null,null);
		}
		return processSyncRequest(request);
	}
	
	public Response post(Request request){
		if(request.getMethod() != Method.POST){
			log.error("400 Bad Request returned. Incorrect method type, expecting POST but received " + request.getMethod());
			return new Response(400, "Bad Request", "Incorrect method type, expecting POST but received " + request.getMethod() ,null,null);
		}
		
		return processSyncRequest(request);
	}
	
	public boolean registerListener(Listener listener, Request request){
		if(listeners.containsKey(listener)){
			return false;
		}else{
			JsonObject headers = request.getHeaders();
			String subscriptionID = UUID.randomUUID().toString();
			headers.addProperty("subscription-id", subscriptionID);
			request.setHeaders(headers);
			request.setMethod(Method.SUBSCRIBE);
			listeners.put(listener,request);
			asyncResponses.put(subscriptionID,listener);
			try{	
				TextMessage message = session.createTextMessage(toJsonFromRequest(request));
				message.setJMSCorrelationID(subscriptionID);
				message.setStringProperty("subscription-id", subscriptionID);
				message.setJMSReplyTo(jscAsyncQueue);
				producer.send(message);
			}catch (JMSException e){
				log.error("JMSException when sending request : " + request + " for listener " + listener, e);
				return false;
			}
			
			return true;
		}
	}
	
	public boolean deregisterListener(Listener listener){
		Request request = listeners.remove(listener);
		String subscriptionID = request.getHeaders().get("subscription-id").getAsString();
		asyncResponses.remove(subscriptionID);
		if(request != null){
			JsonObject headers = request.getHeaders();
			headers.addProperty(RequestHeaders.EXPIRES, "0");
			request.setMethod(Method.SUBSCRIBE);
			try{	
				TextMessage message = session.createTextMessage(toJsonFromRequest(request));
				message.setJMSCorrelationID(subscriptionID);
				message.setStringProperty("subscription-id", subscriptionID);
				message.setJMSReplyTo(jscAsyncQueue);
				producer.send(message);
			}catch (JMSException e){
				log.error("JMSException when sending request : " + request + " for listener " + listener, e);
				return false;
			}		}
		return true;
	}
		
	private Response processSyncRequest(Request request){
		try{
			
			TextMessage message = session.createTextMessage(toJsonFromRequest(request));

			String correlationID = UUID.randomUUID().toString();
			message.setJMSCorrelationID(correlationID);
			message.setJMSReplyTo(jscSyncQueue);
			
			Message responseJmsMsg = null;
			Object lock = new Object();
			synchronized (lock) {
				locks.put(correlationID, lock);
				producer.send(message);
			    int count = 0;
			    while(!responses.containsKey(correlationID)){
			    	try {
						lock.wait(jscTimeout);
					} catch (InterruptedException e) {
						log.error("Interrupted while waiting for lock notification",e);
					}
			    	count++;
			    	if(count >= 1)break;
			    }
			    responseJmsMsg = responses.remove(correlationID);
			}
			
			if(responseJmsMsg == null){
				log.warn("jms response from UDE is null for request <" + request + "> with correlationID = " + correlationID + " returning null");
				return null;
			}else if(!(responseJmsMsg instanceof TextMessage)){
				log.warn("jms response from UDE is not instanceof TextMessage, ignoring for request <" + request + "> with correlationID = " + correlationID + " and returning null");
				return null;
			}else{
				if(log.isInfoEnabled()){
					log.info("jms response from UDE for request <" + request + "> with correlationID = " + correlationID + " is : " + ((TextMessage)responseJmsMsg).getText());
				}
				return toResponsefromJson(((TextMessage)responseJmsMsg).getText());
			}

		}catch (JMSException e){
			log.error("JMSException during get(String request), for request : " + request + " logging and returning null", e);
			return null;
		}
	}
	
	
	private String toJsonFromRequest(Request request) {	
		Gson gson = new Gson();
		return gson.toJson(request);
	}
	
	private String toJsonFromResponse(Response response) {	
		Gson gson = new Gson();
		return gson.toJson(response);
	}
	
	private Response toResponsefromJson(String response) {
		JsonElement jelement = new JsonParser().parse(response);
	    JsonObject  jobject = jelement.getAsJsonObject();
		int code = jobject.get(ResponseConstants.CODE_LABEL).getAsInt();
		String reason = jobject.get(ResponseConstants.REASON_LABEL).getAsString();
		String description =  (jobject.get(ResponseConstants.DESCRIPTION_LABEL)!=null)?jobject.get(ResponseConstants.DESCRIPTION_LABEL).getAsString():null;
		Map<String, String> headers = (jobject.get(ResponseConstants.HEADERS_LABEL)!=null)?getMap(jobject.get(ResponseConstants.HEADERS_LABEL).getAsJsonObject()):null;
		byte[] payload = (jobject.get(ResponseConstants.PAYLOAD_LABEL)!=null)?getByteArray(jobject.get(ResponseConstants.PAYLOAD_LABEL).getAsJsonArray()):null;
		
		return new Response(code,reason,description,headers,payload);
	}
	
	private Request toRequestfromJson(String request) {	
		JsonElement jelement = new JsonParser().parse(request);
	    JsonObject  jobject = jelement.getAsJsonObject();
    	Method method = Method.valueOf(Method.class, jobject.get(RequestConstants.METHOD_LABEL).getAsString());
		String ruri = jobject.get(RequestConstants.REQUEST_URI_LABEL).getAsString();
		Map<String, String> headers = getMap(jobject.get(RequestConstants.HEADERS_LABEL).getAsJsonObject());
		Map<String, String> parameters = getMap(jobject.get(RequestConstants.PARAMETERS_LABEL).getAsJsonObject());
		String rule = jobject.get(RequestConstants.RULE_LABEL).getAsString();
		byte[] payload = getByteArray(jobject.get(RequestConstants.PAYLOAD_LABEL).getAsJsonArray());
		
		return new Request(method, ruri, headers, parameters, rule, payload);
	}

	private Map<String, String> getMap(JsonObject json) {
		Map<String, String> map = new HashMap<String, String>();
		for(Entry<String, JsonElement> entry:json.entrySet()){
			map.put(entry.getKey(),entry.getValue().getAsString());
		}
		return map;
	}
	
	private byte[] getByteArray(JsonArray array) {
		byte[] result = new byte[array.size()];
		for(int index = 0 ; index < array.size() ;  index++){
			result[index] = array.get(index).getAsByte();
		}
		return result;
	}

	public void onMessageForSyncResponse(Message msg) {
		try{
			if(msg.getJMSCorrelationID() == null){
				log.warn("jms response message received with null JMSCorrelationID, ignoring message.");
				return;
			}

			msg.setJMSTimestamp(System.currentTimeMillis());
			
			if(locks.containsKey(msg.getJMSCorrelationID())){
				responses.put(msg.getJMSCorrelationID(), msg);
				Object lock = locks.remove(msg.getJMSCorrelationID());
				synchronized (lock) {
					lock.notifyAll();
				}
			}else{
				log.error("response with correlationID = " + msg.getJMSCorrelationID() + " received however no record of sending message with this ID, ignoring");
			}

		} catch (JMSException e) {
			log.error("JMSException when processing sync reponse",e);
		}		
	}
	
	public void onMessageForAsyncResponse(Message msg) {
		try{
			if(msg.getJMSCorrelationID() == null){
				log.warn("jms response message received with null JMSCorrelationID, ignoring message.");
				return;
			}
			
			Listener listener = asyncResponses.get(msg.getJMSCorrelationID());
			asyncEventHandlingExecutor.submit(new AsyncReponseHandler(listener, toResponsefromJson(((TextMessage)msg).getText())) );

		} catch (JMSException e) {
			log.error("JMSException when processing async reponse",e);
		} catch (Exception e){
			log.error("Exception when processing async reponse",e);

		}
	}
	
}
