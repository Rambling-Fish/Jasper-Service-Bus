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
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.atlas.json.JsonString;
import org.apache.jena.atlas.json.JsonValue;
import org.apache.log4j.Logger;
import org.jasper.core.delegate.Delegate;
import org.jasper.core.delegate.DelegateFactory;
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
			if(isOntologyRequest( (TextMessage) jmsRequest)){
				handleOntologyRequest( (TextMessage) jmsRequest);
			}
			else{	
	        	logger.warn("JMS Message is not a valid, ignoring request : " + jmsRequest);
	        	System.out.println("JMS Message is not a valid, ignoring request : " + jmsRequest);
	        }
		}catch (Exception e){
			logger.error("Exception caught in handler " + e);
		}
	}
	

	private boolean isOntologyRequest(TextMessage txtMsg) {
			//TODO how do we determine a valid ontology request?
			return true;
	}
	
	/*
	 * Whenever an error is detected in incoming request rather than letting
	 * the client timeout, the core responds with an empty JSON message.
	 * Currently this occurs if incoming URI cannot be mapped to a JTA or the
	 * incoming correlationID is not unique
	 */
	private void processInvalidRequest(TextMessage msg, String errorMsg) throws Exception {

        // For now we always send back empty JSON for all error scenarios
		String msgText = "{".concat(errorMsg).concat("}");
        Message message = delegate.createTextMessage(msgText);
        if(msg.getJMSCorrelationID() == null){
            message.setJMSCorrelationID(msg.getJMSMessageID());
  	  	}else{
            message.setJMSCorrelationID(msg.getJMSCorrelationID());
  	  	}
     
        delegate.sendMessage(msg.getJMSReplyTo(),message);

	}
	
	private void handleOntologyRequest(TextMessage txtMsg) throws Exception {
  	    String request = txtMsg.getText();

  	    if(request == null || request.length() == 0){
  	    	processInvalidRequest(txtMsg, "Invalid text message received");
  	    	return;
  	    }
  	    String ruri = getRuri(request);
  	    JsonArray response = getResponse(ruri,request);
  	    String responseTxt;
  	    try{
  	    	responseTxt=(response == null)?"{}":response.toString();
  	    }catch (Exception e){
  	    	responseTxt="{}";
  	    }
  	    sendResponse(txtMsg,responseTxt);
	}

	private void sendResponse(TextMessage jmsRequestMsg,String response) throws JMSException {
        
        Message message = delegate.createTextMessage(response);
        
        String correlationID = jmsRequestMsg.getJMSCorrelationID();
        if(correlationID == null) correlationID = jmsRequestMsg.getJMSMessageID();
        message.setJMSCorrelationID(correlationID);
		
        delegate.sendMessage(jmsRequestMsg.getJMSReplyTo(), message);
	}

	private JsonArray getResponse(String ruri, String body) throws JMSException {
		JsonArray result = new JsonArray();
		Map<String,String> params = getParams(body);
        JsonArray queuesAndParams = jOntology.getQandParams(ruri, params);
        if(queuesAndParams == null)return null;
      	for(JsonValue j:queuesAndParams){
      		String jta      = ((JsonString)j.getAsObject().get("jta").getAsString()).value();
  			String q        = ((JsonString)j.getAsObject().get("queue").getAsString()).value();
  			String provides = ((JsonString)j.getAsObject().get("provides").getAsString()).value();
  			JsonObject p    = (JsonObject) j.getAsObject().get("params");
  			Object tmp = null;
  			Map<String,String> valuePair = new HashMap<String, String>();
  			for(String key:p.keys()){
  				tmp = p.get(key);
  				if(tmp instanceof JsonString){
  					valuePair.put(key, ((JsonString)tmp).value());
  				}else if(tmp instanceof JsonArray){
  		  			JsonArray response = getResponse(key, body);
  		  			for(JsonValue index:response){
  	  		  			if(index instanceof JsonObject || index instanceof JsonString){
	  	  		  			JsonValue value = (index instanceof JsonObject)?((JsonObject)index).get(key):index;
	  	  		  			if(value.isString()){
	  	  		  				valuePair.put(key, ((JsonString)value).value());
	  	  		  			}else{
	  	  		  				System.out.println("ERROR --------");
	  	  		  			}
  	  		  			}else{
  	  		  				System.out.println("ERROR --------");
  	  		  			}
  		  			}
  				}
  			}
  			
  			if(valuePair.isEmpty()){
  				//TODO change is empty to is all parms available
  				continue;
  			}
  			
  			JsonObject reponse = JSON.parse(getResponseFromQueue(q,valuePair));
  			JsonValue r = reponse.get(provides);
  			result.add(r);
      	}
      	return result;
	}

	private String getResponseFromQueue(String q, Object obj) throws JMSException {
		
		Message msg;
		
		if(obj instanceof Map){
			msg = delegate.createMapMessage((Map<String, ?>)obj);
		}else if(obj instanceof String){
			msg = delegate.createTextMessage((String)obj);
		}else if(obj instanceof Serializable){
			msg = delegate.createObjectMessage((Serializable)obj);
		}else{
			logger.warn("Creating an empty ObjectMessage as obj is neither a Map<String,?>, a String nor a Serializable object, obj = " + obj);
			msg = delegate.createObjectMessage(null);
		}
		
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

	/*
	 * assumes the request will be in the format of ruri?param1=val1
	 * and that the ruri can be be abbreviated. If the abbreviated or
	 * long form ruri is in the uriMapper we return the long
	 * form otherwise we return null
	 */
	private String getRuri(String request) {
		if(request == null)return null;
		String ruri = request.split("\\?")[0];
		if(ruri.isEmpty()) return null;
//		if(DelegateFactory.URI_MAPPER.containsValue(ruri)) return ruri;
//		return (DelegateFactory.URI_MAPPER.containsKey(ruri))?DelegateFactory.URI_MAPPER.get(ruri):null;
		return ruri;
	}
	
	/*
	 * assumes the request will be in the format of ruri?param1=val1&parm2=val2
	 * if there are no params, than we return an empty map, if there's an error,
	 * we return null. All params must be in the abbreviated form in the uriMapper
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

	private Map<String, String> getMapFromValuePairString(String str) {
		Map<String, String> result = new HashMap<String, String>();
		String[] keyValuePairs = str.split("&");
		String[] keyValue;
		for(String s:keyValuePairs){
			keyValue = s.split("=");
//			if(keyValue.length == 2 && DelegateFactory.URI_MAPPER.containsKey(keyValue[0])) result.put(DelegateFactory.URI_MAPPER.get(keyValue[0]), keyValue[1]);
			if(keyValue.length == 2 ) result.put(keyValue[0], keyValue[1]);
		}
		return result;
	}

}