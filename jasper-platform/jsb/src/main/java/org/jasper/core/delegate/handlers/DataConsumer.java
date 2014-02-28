package org.jasper.core.delegate.handlers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonParser;
import com.google.gson.JsonNull;

import org.apache.log4j.Logger;
import org.jasper.core.UDE;
import org.jasper.core.constants.JasperConstants;
import org.jasper.core.delegate.Delegate;
import org.jasper.core.delegate.DelegateOntology;
import org.jasper.core.notification.triggers.Trigger;
import org.jasper.core.persistence.PersistedObject;

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
	private UDE ude;
	private String version;
	private String contentType;
	
	private static Logger logger = Logger.getLogger(DataConsumer.class.getName());

	public DataConsumer(UDE ude, Delegate delegate, DelegateOntology jOntology, Map<String,Object> locks, Map<String,Message> responses) {
		this.ude = ude;
		this.delegate = delegate;
		this.jOntology = jOntology;
		this.locks = locks;
		this.responses = responses;
		this.isShutdown = false;
	}

	public void run() {
		try{
			workQueue  = ude.getCachingSys().getQueue("tasks");
			sharedData = (Map<String, PersistedObject>) ude.getCachingSys().getMap("sharedData");
			do{
				statefulData = workQueue.take();
				if(statefulData != null){
					statefulData.setUDEInstance(ude.getUdeDeploymentAndInstance());
					// Put it back in memory with updated ude instance
					sharedData.put(statefulData.getKey(), statefulData);
					if(logger.isDebugEnabled()){
						logger.debug("**************************************");
						logger.debug("  RECEIVED MSG on " +  statefulData.getUDEInstance());
						logger.debug("**************************************");
					}
					processRequest(statefulData);
				}
				if (isShutdown) break;
			} while (!isShutdown);
		}catch (Exception e){
			// Fix for JASPER-516 to prevent exception each time UDE is stopped
			if(!(e instanceof com.hazelcast.core.HazelcastInstanceNotActiveException)){
				logger.error("Exception caught in handler " + e);
			}
		}

	}
	
	private void processInvalidRequest(JasperConstants.responseCodes respCode, String respMsg) throws Exception {
		if(logger.isInfoEnabled()){
			logger.info("processingInvalidRequest, errorMsg = " + respMsg + " for request " + statefulData.getRequest() + " from " + statefulData.getReplyTo());
		}
		String msgText = delegate.createJasperResponse(respCode, respMsg, null, contentType, version);
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
		output = statefulData.getOutput();
		contentType = statefulData.getContentType();
		version = statefulData.getVersion();
  	    
  	    if(statefulData.isNotificationRequest()){ 
  	    	polling = statefulData.getTriggers().get(0).getPolling();
  	    	while(true){
  	    		response = getResponse(ruri, dtaParms);
  	    		if(response != null){
  	    			if(isCriteriaMet(response)){
  	    				break;
  	    			}
  	    			else if(isNotificationExpired()){
  	    				processInvalidRequest(JasperConstants.responseCodes.TIMEOUT, statefulData.getNotification() + " has expired");
  	    				return;
  	    			}
  	    		}
  	    		else if(isNotificationExpired()){
  	    			processInvalidRequest(JasperConstants.responseCodes.TIMEOUT, statefulData.getNotification() + " has expired");
  	    			return;
  	    		}
  	    	Thread.sleep(polling);
  	    	}
  	    }
  	    else{
  	    	response = getResponse(ruri,dtaParms);
  	    }

  	    if(response == null){
  	    	if(!statefulData.isNotificationRequest()){
  	    		processInvalidRequest(JasperConstants.responseCodes.NOTFOUND, ruri +" is not supported");
  	    		return;
  	    	}
  	    	else{
  	    		if(isNotificationExpired()){
  	    			processInvalidRequest(JasperConstants.responseCodes.TIMEOUT, statefulData.getNotification() + " has expired");
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
      	for(JsonElement j:queuesAndParams){
      		String jta        = j.getAsJsonObject().get(DelegateOntology.JTA).getAsString();
  			String q          = j.getAsJsonObject().get(DelegateOntology.QUEUE).getAsString();
  			String provides   = j.getAsJsonObject().get(DelegateOntology.PROVIDES).getAsString();
  			JsonObject jsonParams = (JsonObject) j.getAsJsonObject().get(DelegateOntology.PARAMS);
  			Object param = null;
  			Map<String,String> valuePair = new HashMap<String, String>();
  			for(Entry<String, JsonElement> key : jsonParams.entrySet()){
  	  			param = key.getValue();
  				if(param instanceof JsonPrimitive){
  					if(logger.isInfoEnabled()) logger.info("param " + key + " provided, no need to lookup, value = " + ((JsonPrimitive)param).getAsString());
  					valuePair.put(key.getKey(), param.toString());
  				}else if(param instanceof JsonArray){
  					if(logger.isInfoEnabled()) logger.info("param " + key + " not provided need to lookup");
  		  			JsonArray response = getResponse(key.getKey(), body);
  		  			for(JsonElement index:response){
  		  				if(index instanceof JsonObject || index instanceof JsonPrimitive){
	  	  		  			JsonElement value = (index instanceof JsonObject)?((JsonObject)index).get(key.getKey()):index;
	  	  		  			if(value.isJsonPrimitive()){
	  	  		  				valuePair.put(key.getKey(), value.getAsString());
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
  			JsonParser parser = new JsonParser();
  			JsonObject response = parser.parse(getResponseFromQueue(q,valuePair)).getAsJsonObject();

  			JsonElement r = (response.get(provides)==null)?response:response.get(provides);
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
			jObj.addProperty(key, (String)map.get(key));
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
		JasperConstants.responseCodes code = JasperConstants.responseCodes.OK;
		String jsonResponse = delegate.createJasperResponse(code, "Success", response, contentType, version);
        Message message = delegate.createTextMessage(jsonResponse);      
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
