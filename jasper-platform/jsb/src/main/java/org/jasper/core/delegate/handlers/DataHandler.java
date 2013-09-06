package org.jasper.core.delegate.handlers;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
import org.jasper.core.delegate.Delegate;
import org.jasper.core.delegate.DelegateOntology;

public class DataHandler implements Runnable {

	private Delegate delegate;
	private DelegateOntology jOntology;
	private Message jmsRequest;
	private Map<String, Object> locks;
	private Map<String, Message> responses;
	
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

  	    if(request == null || request.length() == 0){
  	    	processInvalidRequest(txtMsg, "Invalid text message received, request is null or empty string");
  	    	return;
  	    }
  	    String ruri = getRuri(request);
  	    JsonArray response = getResponse(ruri,request);
    	if(response == null){
  	    	processInvalidRequest(txtMsg, "ruri is not supported");
  	    	return;
    	}
  	    sendResponse(txtMsg,response.toString());
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
	  	  		  				logger.warn("value is not String" + value);
	  	  		  			}
  	  		  			}else{
  	  		  				logger.warn("index is neither JsonObject nor JsonString, index = " + index);
  	  		  			}
  		  			}
  				}else{
	  				logger.warn("param is neither JsonString nor JsonArray, param = " + param);
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
			return new HashMap<String, String>();
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

}