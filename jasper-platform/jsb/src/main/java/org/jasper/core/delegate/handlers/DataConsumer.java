package org.jasper.core.delegate.handlers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonNull;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.atlas.json.JsonString;
import org.apache.jena.atlas.json.JsonValue;
import org.apache.log4j.Logger;
import org.jasper.core.JECore;
import org.jasper.core.delegate.Delegate;
import org.jasper.core.delegate.DelegateOntology;
import org.jasper.core.notification.triggers.Trigger;
import org.jasper.core.persistence.PersistedObject;
import org.jasper.core.persistence.PersistenceFacade;

public class DataConsumer implements Runnable {

	private Delegate delegate;
	private DelegateOntology jOntology;
	private Map<String, Object> locks;
	private Map<String, Message> responses;
	private String output;
	private String dtaParms;
	private int polling;
	private List<Trigger> triggerList;
	private PersistedObject statefulData;
	private Map<String,PersistedObject> sharedData;
	private String key;
	private BlockingQueue<PersistedObject> workQueue;
	private boolean isShutdown;
	
	private static Logger logger = Logger.getLogger(DataHandler.class.getName());

	public DataConsumer(Delegate delegate, DelegateOntology jOntology, Map<String,Object> locks, Map<String,Message> responses) {
		this.delegate = delegate;
		this.jOntology = jOntology;
		this.locks = locks;
		this.responses = responses;
		this.isShutdown = false;
	}

	public void run() {
		try{
			workQueue  = PersistenceFacade.getInstance().getQueue("tasks");
			do{
				statefulData = workQueue.take();
				if(statefulData != null){
					statefulData.setUDEInstance(JECore.getInstance().getUdeDeploymentAndInstance());
                    if(logger.isDebugEnabled()){
                            logger.debug("**************************************");
                            logger.debug("  GOT IT on " +  statefulData.getUDEInstance());
                            logger.debug("**************************************");
                    }
					processRequest(statefulData);
				}
				if (isShutdown) break;
			} while (!isShutdown);
		}catch (Exception e){
			logger.error("Exception caught in handler " + e);
		}

	}
	
	private void processInvalidRequest(String errorMsg) throws Exception {
		if(logger.isInfoEnabled())logger.info("processingInvalidRequest, errorMsg = " + errorMsg + " for request " + statefulData.getRequest() + " from " + statefulData.getReplyTo());
		String msgText = "{\"error\":\"".concat(errorMsg).concat("\"}");
        Message message = delegate.createTextMessage(msgText);
        
        if(statefulData.getCorrelationID() == null){
            message.setJMSCorrelationID(statefulData.getKey());
  	  	}else{
            message.setJMSCorrelationID(statefulData.getCorrelationID());
  	  	}
        
        delegate.sendMessage(statefulData.getReplyTo(),message);
        removeSharedData();
	}
	
	private void processRequest(PersistedObject statefulData) throws Exception {
  	    String request = statefulData.getRequest();
  	    JsonArray response;
  	    String xmlResponse = null;
  	    key = statefulData.getKey();
  	    String ruri = statefulData.getRURI();
  	    dtaParms = statefulData.getDtaParms();
		sharedData = PersistenceFacade.getInstance().getMap("sharedData");
		output = statefulData.getOutput();
  	    
  	    if(statefulData.isNotificationRequest()){ 
  	    	polling = statefulData.getTriggers().get(0).getPolling();
  	    	while(true){
  	    		response = getResponse(ruri, dtaParms);
  	    		if(response != null){
  	    			if(isCriteriaMet(response)){
  	    				break;
  	    			}
  	    			else if(isNotificationExpired()){
  	    				processInvalidRequest("notification: " + statefulData.getNotification() + " has expired");
  	    				break;
  	    			}
  	    		}
  	    		else if(isNotificationExpired()){
  	    			processInvalidRequest("notification: " + statefulData.getNotification() + " has expired");
  	    			break;
  	    		}
  	    	Thread.sleep(polling);
  	    	}
  	    }
  	    else{
  	    	response = getResponse(ruri,request);
  	    }

  	    if(response == null){
  	    	if(!statefulData.isNotificationRequest()){
  	    		processInvalidRequest(ruri +" is not supported");
  	    		return;
  	    	}
  	    	else{
  	    		if(isNotificationExpired()){
  	    			processInvalidRequest("notification " + statefulData.getNotification() + " has expired");
  	    			return;
  	    		}
  	    	}
  	    }
    	
  	    if(output != null && output.equalsIgnoreCase("xml")){
  	    	//TODO convert to xml here IF YOU CAN!
  	    	xmlResponse = response.toString();
  	    	sendResponse(xmlResponse);
  	    }
  	    else{
  	    	sendResponse(response.toString());
  	    }
    	
	}

	private JsonArray getResponse(String ruri, String body) throws JMSException {		
		JsonArray result = new JsonArray();
		Map<String,String> paramsMap = getParams(body);
        JsonArray queuesAndParams = jOntology.getQandParams(ruri, paramsMap);
        if(queuesAndParams == null)return null;
      	for(JsonValue j:queuesAndParams){
      		String jta        = ((JsonString)j.getAsObject().get(DelegateOntology.JTA).getAsString()).value();
  			String q          = ((JsonString)j.getAsObject().get(DelegateOntology.QUEUE).getAsString()).value();
  			String provides   = ((JsonString)j.getAsObject().get(DelegateOntology.PROVIDES).getAsString()).value();
  			JsonObject jsonParams = (JsonObject) j.getAsObject().get(DelegateOntology.PARAMS);
  			Object param = null;
  			Map<String,String> valuePair = new HashMap<String, String>();
  			for(String key:jsonParams.keys()){
  				param = jsonParams.get(key);
  				if(param instanceof JsonString){
  					if(logger.isInfoEnabled()) logger.info("param " + key + " provided, no need to lookup, value = " + ((JsonString)param).value());
  					valuePair.put(key, ((JsonString)param).value());
  				}else if(param instanceof JsonArray){
  					if(logger.isInfoEnabled()) logger.info("param " + key + " not provided need to lookup");
  		  			JsonArray response = getResponse(key, body);
  		  			for(JsonValue index:response){
  	  		  			if(index instanceof JsonObject || index instanceof JsonString){
	  	  		  			JsonValue value = (index instanceof JsonObject)?((JsonObject)index).get(key):index;
	  	  		  			if(value.isString()){
	  	  		  				valuePair.put(key, ((JsonString)value).value());
	  	  		  			}else{
	  	  		  			if(logger.isInfoEnabled()) logger.info("value is not String" + value);
	  	  		  			}
  	  		  			}else{
  	  		  			if(logger.isInfoEnabled()) logger.info("index is neither JsonObject nor JsonString, index = " + index);
  	  		  			}
  		  			}
  				}else{
  					if(logger.isInfoEnabled()) logger.info("param is neither JsonString nor JsonArray, param = " + param);
  				}
  			}
  			
  			/*
  			 * check to see if we have all the info the JTA needs before sending the request
  			 */
  			boolean isResolveable = true;
  			for(String jtaParam:jOntology.getJtaParams(jta).toArray(new String[]{})){
  				if(!valuePair.containsKey(jtaParam)) isResolveable = false;
  			}
  			
  			if(!isResolveable){
				if(logger.isInfoEnabled()) logger.info("the valuePair map doesn't have all the params the DTA needs, therefore we will not send request to DTA");
				continue;
  			}
  			
  			JsonObject reponse = JSON.parse(getResponseFromQueue(q,valuePair));
  			JsonValue r = (reponse.get(provides)==null)?reponse:reponse.get(provides);
  			if( r !=null && !(r instanceof JsonNull) )result.add(r);
      	}
      	return result;
	}
	
	/*
	 * assumes the request will be in the format of ruri?param1=val1&parm2=val2
	 * if there are no params, than we return an empty map, if there's an error,
	 * we return null.
	 */
	private Map<String, String> getParams(String request) {
		if(request == null)return null;
		String[] splitRequest = request.split("\\?");
		if(splitRequest.length == 1){
			return getMapFromValuePairString(splitRequest[0]);
		}else if(splitRequest.length == 2){
			return getMapFromValuePairString(splitRequest[1]);
		}else{
			return null;
		}		
	}

	private String getResponseFromQueue(String q, Map<String, String> map) throws JMSException {
		
		JsonObject jObj = new JsonObject();
		
		for(String key:map.keySet()){
			jObj.put(key, (String)map.get(key));
		}
		Message msg = delegate.createTextMessage(jObj.toString());	
		
        String correlationID = UUID.randomUUID().toString();
        msg.setJMSCorrelationID(correlationID);
        
        Message response;
        Object lock = new Object();
		synchronized (lock) {
			locks.put(correlationID, lock);
		    delegate.sendMessage(q, msg);
		    int count = 0;
		    while(!responses.containsKey(correlationID)){
		    	try {
					lock.wait(10000);
				} catch (InterruptedException e) {
					logger.error("Interrupted while waiting for lock notification",e);
				}
		    	count++;
		    	if(count >= 6)break;
		    }
		    response = responses.remove(correlationID);
		}

		String responseString = null;
		if(response != null && response.getJMSCorrelationID().equals(correlationID) && response instanceof TextMessage){
			responseString = ((TextMessage)response).getText();
		}
		return responseString;
	}
	
	private Map<String, String> getMapFromValuePairString(String str) {
		Map<String, String> result = new HashMap<String, String>();
		String[] keyValuePairs = str.split("&");
		String[] keyValue;
		for(String s:keyValuePairs){
			keyValue = s.split("=");
			if(keyValue.length == 2 ) result.put(keyValue[0], keyValue[1]);
		}
		return result;
	}
	
	private void sendResponse(String response) throws JMSException {
        Message message = delegate.createTextMessage(response);      
        String correlationID = statefulData.getCorrelationID();
        if(correlationID == null) correlationID = statefulData.getKey();
        message.setJMSCorrelationID(correlationID);
		
        delegate.sendMessage(statefulData.getReplyTo(), message);
        removeSharedData();
        
	}
	
	private boolean isCriteriaMet(JsonArray response){
		boolean result = false;
		triggerList = statefulData.getTriggers();
		for(int i=0;i<triggerList.size();i++){
			if(triggerList.get(i).evaluate(response)){
				result = true;
			}
		}
	
		return result;
	}
	
	/*
	 * Checks all triggers in the shared map for expiry
	 */
	private boolean isNotificationExpired(){
		boolean result = false;
		triggerList = statefulData.getTriggers();
		for(int i=0;i<triggerList.size();i++){
			if(triggerList.get(i).isNotificationExpired()){
				result = true;
			}
		}
	
		return result;
	}
	
	private void removeSharedData(){
		sharedData.remove(key);
	}
	
	public void shutdown() throws JMSException {
		isShutdown = true;
	}

}
