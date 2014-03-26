package org.jasper.core.delegate.handlers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

import org.apache.log4j.Logger;
import org.jasper.core.UDE;
import org.jasper.core.constants.JasperConstants;
import org.jasper.core.dataprocessor.DataProcessor;
import org.jasper.core.dataprocessor.DataProcessorFactory;
import org.jasper.core.delegate.Delegate;
import org.jasper.core.delegate.DelegateOntology;
import org.jasper.core.notification.triggers.Trigger;
import org.jasper.core.persistence.PersistedObject;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hazelcast.core.MultiMap;

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
	private String method;
	private JsonParser jsonParser;
	private MultiMap<String,PersistedObject> registeredListeners;

	
	private static Logger logger = Logger.getLogger(DataConsumer.class.getName());

	public DataConsumer(UDE ude, Delegate delegate, DelegateOntology jOntology, Map<String,Object> locks, Map<String,Message> responses) {
		this.ude = ude;
		this.delegate = delegate;
		this.jOntology = jOntology;
		this.locks = locks;
		this.responses = responses;
		this.isShutdown = false;	
		this.jsonParser = new JsonParser();
	}

	public void run() {
		try{
			workQueue  = ude.getCachingSys().getQueue("tasks");
			sharedData = (Map<String, PersistedObject>) ude.getCachingSys().getMap("sharedData");
			registeredListeners = (MultiMap<String, PersistedObject>) ude.getCachingSys().getMultiMap("registeredListeners");
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
					processRequest();
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
	
	private void processRequest() throws Exception {
  	    String request = statefulData.getRequest();
  	    JsonElement response;
  	    String xmlResponse = null;
  	    key = statefulData.getKey();
  	    String ruri = statefulData.getRURI();
  	    dtaParms = statefulData.getDtaParms();
		output = statefulData.getOutput();
		contentType = statefulData.getContentType();
		version = statefulData.getVersion();
		method = statefulData.getMethod();
		
		if(method.equalsIgnoreCase(JasperConstants.SUBSCRIBE)){
			processSubscribe();
			return;
		}
		
		if(method.equalsIgnoreCase(JasperConstants.POST) || method.equalsIgnoreCase(JasperConstants.PUBLISH)){
			processPublish(ruri, request);
			return;
		}
		
		JsonElement jsonRequest = jsonParser.parse(statefulData.getRequest());
  	    String processing_scheme;
  	    if( jsonRequest.isJsonObject()
  	    		&& jsonRequest.getAsJsonObject().has("headers") 
  	    		&& jsonRequest.getAsJsonObject().get("headers").isJsonObject()
  	    		&& jsonRequest.getAsJsonObject().get("headers").getAsJsonObject().has("processing-scheme")
  	            && jsonRequest.getAsJsonObject().get("headers").getAsJsonObject().get("processing-scheme").isJsonPrimitive()
  	            && jsonRequest.getAsJsonObject().get("headers").getAsJsonObject().get("processing-scheme").getAsJsonPrimitive().isString()){
  	    	processing_scheme = jsonRequest.getAsJsonObject().get("headers").getAsJsonObject().get("processing-scheme").getAsJsonPrimitive().getAsString();
  	    }else{
  	    	processing_scheme = JasperConstants.DEFAULT_PROCESSING_SCHEME;
  	    }
  	    
  	    JsonObject parameters = new JsonObject();
  	    if( jsonRequest.isJsonObject()
  	    		&& jsonRequest.getAsJsonObject().has("parameters") 
  	    		&& jsonRequest.getAsJsonObject().get("parameters").isJsonObject()){
  	    	parameters = jsonRequest.getAsJsonObject().get("parameters").getAsJsonObject();
  	    }
		
		
  	    if(statefulData.isNotificationRequest()){ 
  	    	polling = statefulData.getTriggers().get(0).getPolling();
  	    	while(true){
  	    		response = getResponse(ruri, parameters, processing_scheme);
  	    		if(response != null){
  	    			if(isCriteriaMet(response, null)){
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
  	    	response = getResponse(ruri, parameters, processing_scheme);;
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

	private JsonElement getResponse(String ruri, JsonObject parameters, String processing_scheme) throws JMSException{		
		if(!jOntology.isRuriKnownForOutputGet(ruri)) return null;
		
		DataProcessor dataProcessor = DataProcessorFactory.createDataProcessor(processing_scheme);
		
		ArrayList<String> operations = jOntology.getProvideOperations(ruri);
		
		//TODO re-write to do operations in parallel, currently done sequentially.
		for(String operation:operations){
			JsonObject inputObject = getInputObject(jOntology.getProvideOperationInputObject(operation),parameters);
			if(inputObject == null) continue;
			JsonElement response = sendAndWaitforResponse(jOntology.getProvideDestinationQueue(operation),inputObject.toString());
			dataProcessor.add(extractRuriData(ruri, response));
//			dataProcessor.add(response);
		}
		
		return dataProcessor.process();
	}

	private JsonElement extractRuriData(String ruri, JsonElement response) {
	
		if(response.isJsonPrimitive()){
			//if primitive assume it is the data we want.
			return response;
		}else if(response.isJsonObject()){
			
			JsonObject responseObject = response.getAsJsonObject();
//			if(responseObject.has("@type") && responseObject.get("@type").isJsonPrimitive() && responseObject.get("@type").getAsJsonPrimitive().isString()){
//				String atType = responseObject.get("@type").getAsJsonPrimitive().getAsString();
//				if(jOntology.isRuriSubPropteryOf(ruri, atType)){	
//					return response;
//				}
//			}
			
			//Check 1 level deep
			for(Entry<String, JsonElement> entry : responseObject.entrySet()){
				if(entry.getKey().equals(ruri)) return entry.getValue();
			}
			
			//check n level deep
			for(Entry<String, JsonElement> entry : responseObject.entrySet()){				
				
				if(entry.getValue().isJsonPrimitive()) continue;
				
				JsonElement tmpResponse = extractRuriData(ruri, entry.getValue());
				if(tmpResponse != null) return tmpResponse;
			}
			
			return responseObject;
			
		}else if(response.isJsonArray()){
			
			JsonArray responseArray = response.getAsJsonArray();
			JsonArray array = new JsonArray();
	
			for(JsonElement item : responseArray){
				JsonElement tmpItem = extractRuriData(ruri, item);
				if(tmpItem != null) array.add(tmpItem);
			}
			return (array.size()>0)?array:null;
		}
		
		return null;
	}

	private JsonElement sendAndWaitforResponse(String provideDestinationQueue, String msgText) throws JMSException {
		Message msg = delegate.createTextMessage(msgText);	
		
        String correlationID = UUID.randomUUID().toString();
        msg.setJMSCorrelationID(correlationID);
        
        Message response;
        Object lock = new Object();
		synchronized (lock) {
			locks.put(correlationID, lock);
		    delegate.sendMessage(provideDestinationQueue, msg);
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
		
		JsonElement responseObject = null;
		try{
			responseObject = jsonParser.parse(responseString);
		}catch (Exception e){
			logger.error("response from DTA is not a valid JsonReponse, response string = " + responseString);
		}
			
		return responseObject;
	}

	private JsonObject getInputObject(String ruri, JsonObject parameters) throws JMSException {
			
			JsonObject schema = jOntology.createJsonSchema(ruri);
			
			if(schema.has("required")){
				if(!isAllRequiredParametersPassed(schema.get("required").getAsJsonArray(),parameters)){
					//TODO add more info regarding which parameter is missing
					return null;
				}	
			}
			
			JsonObject result = new JsonObject();
			
			if(!schema.has("type")){
				System.out.println("invalid schema missing, type.");
				return null;
			}
			
			String type = schema.getAsJsonPrimitive("type").getAsString();

			if(!"object".equals(type)){
				logger.info("ruri " + ruri + " is not of type object, all input objects must be of type object. Returning NULL");
				return null;
			}
			
			if(schema.has("id")){
				result.add("@type", schema.getAsJsonPrimitive("id"));
			}
			
			if(!schema.has("properties")){
				System.out.println("no properites to add, returing result");
				return result;
			}else if(!schema.get("properties").isJsonObject()){
				System.out.println("invalid schema properites is not of type object, returning null.");
				return null;
			}
			
			JsonObject properties = schema.get("properties").getAsJsonObject();
			
			for(Entry<String, JsonElement> entry:properties.entrySet()){
				if(!parameters.has(entry.getKey())){
					logger.info("property " + entry.getKey() + " is not passed as parameter, it is not mandatory (we would have failed earlier if it was), ignoring property.");
					continue;
				}
				
				String propertyName = entry.getKey();
				JsonObject propertySchema = entry.getValue().getAsJsonObject();
				JsonElement property = parameters.get(propertyName);
				
				if(isValidJsonObject(propertySchema,property)){
					result.add(propertyName,property);
				}else{
					
					JsonElement subPropertyResult = null;
								
					if(propertySchema.has("required") && isAllRequiredParametersPassed(propertySchema.get("required").getAsJsonArray(),propertySchema)){
						if(property.isJsonObject()){
							subPropertyResult = getInputObject(propertyName, property.getAsJsonObject());
						}else{
							System.out.println("cannot getInputObject of sub property " + propertyName + " and it passed parameters is not a jsonObject");
						}
					}else{
						if(property.isJsonObject()){
							String subPropertyProcessingScheme = (propertySchema.has("type") && "array".equals(property.getAsJsonObject().get("type").getAsString()))?JasperConstants.AGGREGATE_SCHEME:JasperConstants.COALESCE_SCHEME;
							if(logger.isInfoEnabled()){
								logger.info("sub property type : " + (property.getAsJsonObject().get("type")) + " setting processing scheme to : " + subPropertyProcessingScheme);
							}
							subPropertyResult = getResponse(propertyName,property.getAsJsonObject(),subPropertyProcessingScheme);
						}else{
							System.out.println("cannot getInputObject of sub property " + propertyName + " and it passed parameters is not a jsonObject");
						}

					}
					
					
					if(subPropertyResult == null && isPropertyRequired(propertyName,schema)){
						System.out.println("cannot fetch or build property " + propertyName + " and it is required, therefore we cannot build the object, returning null");
						return null;
					}else if(subPropertyResult == null){
						System.out.println("cannot fetch or build property " + propertyName + " and it is not required, ignoring");
					}else{
						result.add(propertyName, subPropertyResult);
					}
				}
				
			}
			
			
			return result;
		}
	
	private boolean isPropertyRequired(String propertyName,JsonObject schema) {
		if(!schema.has("required")) return true;
		
		JsonElement required = schema.get("required");
		
		if(!required.isJsonArray()){
			System.out.println("property " + propertyName + " schema has required JsonElement that is not a JsonArray, this is an invalid schema, returning that the property is required as default behaviour");
			return true;
		}
		
		for(JsonElement entry:required.getAsJsonArray()){
			if(propertyName.equals(entry.getAsString())) return true;
		}
		
		return false;
	}
	
	private boolean isValidJsonObject(JsonObject propertySchema,	JsonElement property) {
		if(!propertySchema.has("type")){
			System.out.println("invalid schema missing, type.");
			return false;
		}
				
		String type = propertySchema.getAsJsonPrimitive("type").getAsString();
		
		switch (type){
		case "object":
			JsonObject properties = propertySchema.get("properties").getAsJsonObject();
			if(!property.isJsonObject()) return false;
			
			if(propertySchema.has("required")){
				
				if(!property.isJsonObject()){
					System.out.println("property is not JsonObject: " + property);
				}
				
				if(!isAllRequiredParametersPassed(propertySchema.get("required").getAsJsonArray(),property.getAsJsonObject() )){
					//TODO add more info regarding which parameter is missing
					return false;
				}	
			}
			
			
			for(Entry<String, JsonElement> entry:properties.entrySet()){
				String subPropertyName = entry.getKey();
				JsonObject subPropertySchema = entry.getValue().getAsJsonObject();
				if(!isValidJsonObject(subPropertySchema, property.getAsJsonObject().get(subPropertyName))){
					return false;
				}
			}		
			return true;
		case "string":
			return (property.isJsonPrimitive() && property.getAsJsonPrimitive().isString());
		case "array":
			//TODO add array validation
			return false;
		case "integer":
			return (property.isJsonPrimitive() && property.getAsJsonPrimitive().isNumber());
		case "boolean":
			return (property.isJsonPrimitive() && property.getAsJsonPrimitive().isBoolean());
		}
		
		return true;
	}


	private boolean isAllRequiredParametersPassed(JsonArray jsonArray, JsonObject parameters) {
		boolean response = true;
		for(JsonElement entry:jsonArray){
			if(!(entry.isJsonPrimitive() && entry.getAsJsonPrimitive().isString() && parameters.has(entry.getAsString()))){
				if(entry.isJsonPrimitive() && entry.getAsJsonPrimitive().isString()){
					logger.info("required paramter missing : " + entry.getAsString());
				}else{
					logger.info("required paramter not primitive : " + entry);
				}
				response = false;
			}
		}
		
		return response;
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
	
	private void processPublish(String ruri, String request) throws Exception{
		Collection<PersistedObject> storedObjs = registeredListeners.get(ruri);
		if(dtaParms.contains("parmsArray")){
			dtaParms = dtaParms.replaceFirst("parmsArray=", "");
		}
		Iterator<PersistedObject> it = storedObjs.iterator();
		while(it.hasNext()){
			PersistedObject pObj = (PersistedObject) it.next();
			if(pObj.isNotificationRequest()){
				JsonElement jelement = new JsonParser().parse(dtaParms);
				if(isCriteriaMet(jelement, pObj)){
					sendAsyncResponse(dtaParms, pObj);
				}
			}
			else{
				sendAsyncResponse(dtaParms, pObj);
			}
		}
	}
	
	private void processSubscribe() throws Exception{
		String ruri = statefulData.getRURI();
		if(statefulData.getExpires() == 0){
			// expires == 0 is an unsubscribe
			Collection<PersistedObject> storedObjs = registeredListeners.get(ruri);
			Iterator<PersistedObject> it = storedObjs.iterator();
			while(it.hasNext()){
				PersistedObject pObj = (PersistedObject) it.next();
				if(pObj.getSubscriptionId().equalsIgnoreCase(statefulData.getSubscriptionId())){
					registeredListeners.remove(ruri, pObj);
					logger.warn("Removing subscription for " + ruri + " request: " + pObj.getRequest());
				}
			}
		}
		else{
			registeredListeners.put(ruri, statefulData);
			logger.warn("Adding subscription for " + ruri + " request: " + statefulData.getRequest());
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
	
	private void sendAsyncResponse(String response, PersistedObject pData) throws JMSException {
		JasperConstants.responseCodes code = JasperConstants.responseCodes.OK;
		String jsonResponse = delegate.createJasperResponse(code, "Success", response, contentType, version);
        Message message = delegate.createTextMessage(jsonResponse);      
        String correlationID = pData.getCorrelationID();
        if(correlationID == null) correlationID = pData.getKey();
        message.setJMSCorrelationID(correlationID);
		
        delegate.sendMessage(pData.getReplyTo(), message);
        removeSharedData();
        
	}
	
	private boolean isCriteriaMet(JsonElement response, PersistedObject pObj){
		boolean result = false;
		if(pObj != null){
			triggerList = pObj.getTriggers();
		}
		else{
			triggerList = statefulData.getTriggers();
		}
	
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
