package org.jasper.core.delegate.handlers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonNull;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.atlas.json.JsonString;
import org.apache.jena.atlas.json.JsonValue;
import org.apache.log4j.Logger;
import org.jasper.core.delegate.Delegate;
import org.jasper.core.delegate.DelegateOntology;
import org.jasper.core.notification.triggers.Trigger;
import org.jasper.core.notification.triggers.TriggerFactory;
import org.jasper.core.persistence.PersistenceFacade;

public class DataHandler implements Runnable {

	private Delegate delegate;
	private DelegateOntology jOntology;
	private Message jmsRequest;
	private Map<String, Object> locks;
	private Map<String, Message> responses;
	private String notification;
	private String output;
	private String jtaParms;
	private int expiry;
	private int polling;
	private boolean isNotificationRequest = false;
	private List<Trigger> triggerList;
	private static final int MILLISECONDS = 1000;
	private Map<String,List<Trigger>> sharedTriggers;
	private String key;
	
	private static Logger logger = Logger.getLogger(DataHandler.class.getName());



	public DataHandler(Delegate delegate, DelegateOntology jOntology, Message jmsRequest, Map<String,Object> locks, Map<String,Message> responses) {
		this.delegate = delegate;
		this.jOntology = jOntology;
		this.jmsRequest = jmsRequest;
		this.locks = locks;
		this.responses = responses;	
	}

	public void run() {
		try{
			if(jmsRequest instanceof TextMessage){
				processRequest( (TextMessage) jmsRequest);
			}else{
				logger.warn("jmsRequest is not an instanceof TextMessage, dropping request as only TextMessage is currently supported for DataHandler requests. jmsRequest = " + jmsRequest);
			}
		}catch (Exception e){
			logger.error("Exception caught in handler " + e);
		}
	}
	
	private void processInvalidRequest(TextMessage msg, String errorMsg) throws Exception {
		if(logger.isInfoEnabled())logger.info("processingInvalidReqeust, errorMsg = " + errorMsg + " for request " + msg.getText() + " from " + msg.getJMSReplyTo());
		String msgText = "{".concat(errorMsg).concat("}");
        Message message = delegate.createTextMessage(msgText);
        
        if(msg.getJMSCorrelationID() == null){
            message.setJMSCorrelationID(msg.getJMSMessageID());
  	  	}else{
            message.setJMSCorrelationID(msg.getJMSCorrelationID());
  	  	}
        
        delegate.sendMessage(msg.getJMSReplyTo(),message);
	}
	
	private void processRequest(TextMessage txtMsg) throws Exception {
  	    String request = txtMsg.getText();
  	    isNotificationRequest = false;
  	    JsonArray response;
  	    String xmlResponse = null;
this.key = txtMsg.getJMSMessageID(); //TODO need a key all UDEs would know
  	    if(request == null || request.length() == 0){
  	    	processInvalidRequest(txtMsg, "Invalid request received, request is null or empty string");
  	    	return;
  	    }
  	    
  	    String ruri = getRuri(request);
  	    if(ruri == null){
  	    	processInvalidRequest(txtMsg, "Invalid request received, request does not contain a URI");
  	    	return;
  	    }
  	    
  	    parseRequest(request);
  	    
  	    if(triggerList != null){
  	    	sharedTriggers.put(key, triggerList);
  	    }
  	    
  	    isNotificationRequest = (notification != null);

  	    if(isNotificationRequest){  	    	
  	    	while(true){
  	    		response = getResponse(ruri, jtaParms);
  	    		if(response != null){
  	    			if(isCriteriaMet(response)){
  	    				break;
  	    			}
  	    			else if(isNotificationExpired()){
  	    				processInvalidRequest(txtMsg, "notification: " + notification + " has expired");
  	    				return;
  	    			}
  	    		}
  	    		else if(isNotificationExpired()){
  	    			processInvalidRequest(txtMsg, "notification: " + notification + " has expired");
  	    			return;
  	    		}
  	    		Thread.sleep(polling);
  	    	}
  	    }
  	    else{
  	    	response = getResponse(ruri,request);
  	    }

  	    if(response == null){
  	    	if(!isNotificationRequest){
  	    		processInvalidRequest(txtMsg, ""+ ruri +" is not supported");
  	    		return;
  	    	}
  	    	else{
  	    		if(isNotificationExpired()){
  	    			processInvalidRequest(txtMsg, "notification " + notification + " has expired");
  	    			return;
  	    		}
  	    	}
  	    }
    	
  	    if(output != null && output.equalsIgnoreCase("xml")){
  	    	//TODO convert to xml here IF YOU CAN!
  	    	xmlResponse = response.toString();
  	    	sendResponse(txtMsg,xmlResponse);
  	    }
  	    else{
  	    	sendResponse(txtMsg,response.toString());
  	    }
    	
	}
	
	/*
	 * assumes the request will be in the format of ruri?param1=val1
	 */
	private String getRuri(String request) {
		if(request == null)return null;
		String ruri = request.split("\\?")[0];
		if(ruri.isEmpty()) return null;
		return ruri;
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
  			Map<String,Serializable> valuePair = new HashMap<String, Serializable>();
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
				if(logger.isInfoEnabled()) logger.info("the valuePair map doesn't have all the params the JTA needs, therefore we will not send reqeust to JTA");
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

	private String getResponseFromQueue(String q, Map<String, Serializable> map) throws JMSException {
		
		Message msg = delegate.createMapMessage(map);
		
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
	
	private void sendResponse(TextMessage jmsRequestMsg,String response) throws JMSException {
        Message message = delegate.createTextMessage(response);      
        String correlationID = jmsRequestMsg.getJMSCorrelationID();
        if(correlationID == null) correlationID = jmsRequestMsg.getJMSMessageID();
        message.setJMSCorrelationID(correlationID);
		
        delegate.sendMessage(jmsRequestMsg.getJMSReplyTo(), message);
	}
	
	private boolean isCriteriaMet(JsonArray response){
		boolean result = false;
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
		triggerList = sharedTriggers.get(key);
		for(int i=0;i<triggerList.size();i++){
			if(triggerList.get(i).isNotificationExpired()){
				result = true;
			}
		}
	
		return result;
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
			trigger = factory.createTrigger(functions[i], expiry, polling, triggerParms);
			if(trigger != null){
				trigger.setNotificationExpiry();
				triggerList.add(trigger);
			}
			else{
				logger.error("Invalid notification request received - cannot create trigger: " + triggerParms.toString());
			}
		}		
	}
	
	private void parseRequest(String text) {
		String[] result = null;
		notification = null;
		expiry = -1;
		polling = -1;
		
		result = text.split("\\?");
		
		if(result.length < 1) return;
		
		try {
			for(int i=1;i<result.length;i++){
				result[i] = URIUtil.decode(result[i]);
				if(result[i].toLowerCase().contains("trigger=")){
					notification = result[i].replaceFirst("trigger=", "");
					triggerList = new ArrayList<Trigger>();
				}
				else if(result[i].toLowerCase().contains("output=")){
					output = result[i].toLowerCase().replaceFirst("output=", "");
				}
				else if(result[i].toLowerCase().contains("expiry=")){
					String tmp = result[i].toLowerCase().replaceFirst("expiry=",  "");
					try{
						expiry = Integer.parseInt(tmp);
						expiry = (expiry * MILLISECONDS); // convert from seconds to milliseconds
					}catch (NumberFormatException ex) {
						// give one shot at getting data on error since we do
						// know what to set expiry to
						expiry = 0;
					}
				}
				else if(result[i].toLowerCase().contains("polling=")){
					String tmpPoll = result[i].toLowerCase().replaceFirst("polling=",  "");
					try{
						polling = Integer.parseInt(tmpPoll);
						polling = (polling * MILLISECONDS); // convert from seconds to milliseconds
						
					}catch (NumberFormatException ex) {
		    			polling = delegate.maxPollingInterval;
					}
				}
				else{
					jtaParms = result[i];
				}
			}
			
			if(notification != null){
				if(expiry == -1) expiry = delegate.maxExpiry; // if not supplied set to max
				if(expiry > delegate.maxExpiry) expiry = delegate.maxExpiry;
				if(polling == -1) polling = delegate.maxPollingInterval; // if not supplied set to max
				if(polling < delegate.minPollingInterval) polling = delegate.minPollingInterval;
				if(polling > delegate.maxPollingInterval) polling = delegate.maxPollingInterval;
				if(polling > expiry) polling = (int) expiry;
				if(output == null) output =  delegate.defaultOutput;
				
				sharedTriggers = PersistenceFacade.getInstance().getMap("sharedTriggers");
				parseTrigger(notification);
			}
			
		} catch (URIException e) {
			logger.error("Exception when decoding encoded notification request " ,e);
		}
	}

}