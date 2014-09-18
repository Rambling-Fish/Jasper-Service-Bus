package org.jasper.core.delegate.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

import org.apache.log4j.Logger;
import org.jasper.core.constants.JasperConstants;
import org.jasper.core.dataprocessor.DataProcessor;
import org.jasper.core.dataprocessor.DataProcessorFactory;
import org.jasper.core.delegate.Delegate;
import org.jasper.core.delegate.DelegateOntology;
import org.jasper.core.exceptions.JasperRequestException;
import org.jasper.core.notification.triggers.Trigger;
import org.jasper.core.notification.triggers.TriggerFactory;
import org.jasper.core.persistence.PersistedDataRequest;
import org.jasper.core.persistence.PersistedSubscriptionRequest;
import org.jasper.core.util.JsonLDTransformer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class DataRequestHandler implements Runnable {

	private JsonParser				jsonParser;
	private DelegateOntology		jOntology;
	private Delegate				delegate;
	private Map<String, Object>		locks;
	private Map<String, Message>	responses;
	private PersistedDataRequest	persistedRequest;
	private JsonLDTransformer       jsonLDTransformer;
	private String                  response_type;
	private StringBuilder           errorDescription;

	private static Logger			logger	= Logger.getLogger(DataRequestHandler.class.getName());

	public DataRequestHandler(Delegate delegate, PersistedDataRequest persistedRequest) {
		jsonParser = new JsonParser();
		this.jOntology = delegate.getJOntology();
		this.delegate = delegate;
		this.locks = delegate.getLocksMap();
		this.responses = delegate.getResponsesMap();
		this.persistedRequest = persistedRequest;
		jsonLDTransformer = new JsonLDTransformer();
		errorDescription = new StringBuilder();
	}

	@Override
	public void run() {
		try {
			processRequest(persistedRequest.getCorrelationID(), persistedRequest.getReply2Q(), persistedRequest.getRequest(),persistedRequest.getTimestampMillis());
			delegate.removePersistedRequest(persistedRequest);
		} catch (JMSException e) {
			logger.error("error in processRequest", e);
		} catch (JasperRequestException e) {
			logger.error("JasperRequstException, " + e.getResponseCode() +  " " + e.getDetails() + " sending error response back" );
			try {
				processInvalidRequest(persistedRequest.getCorrelationID(), persistedRequest.getReply2Q(), e.getResponseCode(), e.getDetails());
			} catch (JMSException e1) {
				logger.error("error in processInvalidRequest", e1);

			}
		}
	}

	private void processRequest(String correlationID, Destination reply2q, String request, long timestampMillis) throws JMSException, JasperRequestException {
		
		//TODO add catch of parse exception and throw JasperReqeustException
		JsonElement jsonRequest = jsonParser.parse(request);
		
		String method = null;
		if (jsonRequest.isJsonObject() && jsonRequest.getAsJsonObject().has(JasperConstants.METHOD_LABEL)
				&& jsonRequest.getAsJsonObject().get(JasperConstants.METHOD_LABEL).isJsonPrimitive()
				&& jsonRequest.getAsJsonObject().get(JasperConstants.METHOD_LABEL).getAsJsonPrimitive().isString()) {
			method = jsonRequest.getAsJsonObject().get(JasperConstants.METHOD_LABEL).getAsJsonPrimitive().getAsString();
		} else {
			logger.error("no method defined in jasperReqeust, sending back bad reqeust response");
			throw new JasperRequestException(JasperConstants.ResponseCodes.BADREQUEST, "No method defined in jasperReqeust");
		}
		
		String ruri = null;
		if (jsonRequest.isJsonObject() && jsonRequest.getAsJsonObject().has(JasperConstants.REQUEST_URI_LABEL)
				&& jsonRequest.getAsJsonObject().get(JasperConstants.REQUEST_URI_LABEL).isJsonPrimitive()
				&& jsonRequest.getAsJsonObject().get(JasperConstants.REQUEST_URI_LABEL).getAsJsonPrimitive().isString()) {
			ruri = jsonRequest.getAsJsonObject().get(JasperConstants.REQUEST_URI_LABEL).getAsJsonPrimitive().getAsString();
		}else{
			logger.error("ruri is null, sending back error response for reqeust for correlation ID " + correlationID);
			throw new JasperRequestException(JasperConstants.ResponseCodes.BADREQUEST, ruri + " is null");
		}
		
		if (jsonRequest.isJsonObject() && jsonRequest.getAsJsonObject().has(JasperConstants.HEADERS_LABEL) && jsonRequest.getAsJsonObject().get(JasperConstants.HEADERS_LABEL).isJsonObject()
				&& jsonRequest.getAsJsonObject().get(JasperConstants.HEADERS_LABEL).getAsJsonObject().has(JasperConstants.RESPONSE_TYPE_LABEL)
				&& jsonRequest.getAsJsonObject().get(JasperConstants.HEADERS_LABEL).getAsJsonObject().get(JasperConstants.RESPONSE_TYPE_LABEL).isJsonPrimitive()
				&& jsonRequest.getAsJsonObject().get(JasperConstants.HEADERS_LABEL).getAsJsonObject().get(JasperConstants.RESPONSE_TYPE_LABEL).getAsJsonPrimitive().isString()) {
			response_type = jsonRequest.getAsJsonObject().get(JasperConstants.HEADERS_LABEL).getAsJsonObject().get(JasperConstants.RESPONSE_TYPE_LABEL).getAsJsonPrimitive().getAsString();
		} else {
			response_type = JasperConstants.DEFAULT_RESPONSE_TYPE;
		}
				
		switch (method.toLowerCase()) {
		case JasperConstants.GET:
			processGetRequest(jsonRequest, ruri, correlationID, reply2q, timestampMillis);
			break;
		case JasperConstants.PUBLISH:
			processPublishRequest(jsonRequest, ruri);
			break;
		case JasperConstants.POST:
			processPostRequest(jsonRequest, ruri, correlationID, reply2q,timestampMillis);
			break;
		case JasperConstants.SUBSCRIBE:
			processSubscribeRequest(jsonRequest, ruri, correlationID, reply2q);	
			break;
		default:
			logger.error("unsupported method type " + method);
			throw new JasperRequestException(JasperConstants.ResponseCodes.BADREQUEST, "unsupported method type " + method);
		}
		
	}

	private void processSubscribeRequest(JsonElement jsonRequest, String ruri, String correlationID, Destination reply2q) throws JasperRequestException {

		if (!jOntology.isRuriKnownForInputPublish(ruri)){
			delegate.removePersistedRequest(persistedRequest);
			logger.error("ruri " + ruri + " is not PUBLISHED by any DTA, cannot SUBSCRIBE to unpublished data, sending back error response for correlationID " + correlationID);
			throw new JasperRequestException(JasperConstants.ResponseCodes.NOTFOUND, "ruri " + ruri + " is not PUBLISHED by any DTA, cannot SUBSCRIBE to unpublished data");
		}
		
		String subscriptionId = null;
		if (jsonRequest.isJsonObject() && jsonRequest.getAsJsonObject().has(JasperConstants.HEADERS_LABEL) && jsonRequest.getAsJsonObject().get(JasperConstants.HEADERS_LABEL).isJsonObject()
				&& jsonRequest.getAsJsonObject().get(JasperConstants.HEADERS_LABEL).getAsJsonObject().has(JasperConstants.SUBSCRIPTION_ID_LABEL)
				&& jsonRequest.getAsJsonObject().get(JasperConstants.HEADERS_LABEL).getAsJsonObject().get(JasperConstants.SUBSCRIPTION_ID_LABEL).isJsonPrimitive()
				&& jsonRequest.getAsJsonObject().get(JasperConstants.HEADERS_LABEL).getAsJsonObject().get(JasperConstants.SUBSCRIPTION_ID_LABEL).getAsJsonPrimitive().isString()) {
			subscriptionId = jsonRequest.getAsJsonObject().get(JasperConstants.HEADERS_LABEL).getAsJsonObject().get(JasperConstants.SUBSCRIPTION_ID_LABEL).getAsJsonPrimitive().getAsString();
		}else{
			logger.error("subscription request does not contain subscription ID, returning bad request response");
			throw new JasperRequestException(JasperConstants.ResponseCodes.BADREQUEST, "subscription request does not contain subscription ID");
		}
		
		int expiry;
		if (jsonRequest.isJsonObject() && jsonRequest.getAsJsonObject().has(JasperConstants.HEADERS_LABEL) && jsonRequest.getAsJsonObject().get(JasperConstants.HEADERS_LABEL).isJsonObject()
				&& jsonRequest.getAsJsonObject().get(JasperConstants.HEADERS_LABEL).getAsJsonObject().has(JasperConstants.EXPIRES_LABEL)
				&& jsonRequest.getAsJsonObject().get(JasperConstants.HEADERS_LABEL).getAsJsonObject().get(JasperConstants.EXPIRES_LABEL).isJsonPrimitive()
				&& jsonRequest.getAsJsonObject().get(JasperConstants.HEADERS_LABEL).getAsJsonObject().get(JasperConstants.EXPIRES_LABEL).getAsJsonPrimitive().isNumber()) {
			expiry = jsonRequest.getAsJsonObject().get(JasperConstants.HEADERS_LABEL).getAsJsonObject().get(JasperConstants.EXPIRES_LABEL).getAsJsonPrimitive().getAsInt();
		} else {
			//IF no expire, then set to -1 and never expire
			expiry = -1;
		}
		
		String rule;
		if (jsonRequest.isJsonObject() && jsonRequest.getAsJsonObject().has(JasperConstants.RULE_LABEL)
				&& jsonRequest.getAsJsonObject().get(JasperConstants.RULE_LABEL).isJsonPrimitive()
				&& jsonRequest.getAsJsonObject().get(JasperConstants.RULE_LABEL).getAsJsonPrimitive().isString()) {
			rule = jsonRequest.getAsJsonObject().get(JasperConstants.RULE_LABEL).getAsJsonPrimitive().getAsString();
		} else {
			rule = null;
		}
		
		String responseType;
		if (jsonRequest.isJsonObject() && jsonRequest.getAsJsonObject().has(JasperConstants.HEADERS_LABEL) && jsonRequest.getAsJsonObject().get(JasperConstants.HEADERS_LABEL).isJsonObject()
				&& jsonRequest.getAsJsonObject().get(JasperConstants.HEADERS_LABEL).getAsJsonObject().has(JasperConstants.RESPONSE_TYPE_LABEL)
				&& jsonRequest.getAsJsonObject().get(JasperConstants.HEADERS_LABEL).getAsJsonObject().get(JasperConstants.RESPONSE_TYPE_LABEL).isJsonPrimitive()
				&& jsonRequest.getAsJsonObject().get(JasperConstants.HEADERS_LABEL).getAsJsonObject().get(JasperConstants.RESPONSE_TYPE_LABEL).getAsJsonPrimitive().isString()) {
			responseType = jsonRequest.getAsJsonObject().get(JasperConstants.HEADERS_LABEL).getAsJsonObject().get(JasperConstants.RESPONSE_TYPE_LABEL).getAsJsonPrimitive().getAsString();
		} else {
			responseType = JasperConstants.DEFAULT_RESPONSE_TYPE;
		}
		
		List<Trigger> triggerList = parseTriggers(rule);
		
		if(expiry == 0){
			delegate.removePersistedSubscriptionRequest(ruri,subscriptionId);
		}else{
			delegate.persistSubscriptionRequest(ruri,subscriptionId,correlationID,responseType,reply2q,triggerList,expiry);
		}
		
	}

	private void processPublishRequest(JsonElement jsonRequest, String typeUri) throws JMSException {
		JsonElement payload = jsonRequest.getAsJsonObject().get(JasperConstants.PARAMETERS_LABEL);
		processPublishPayload(null, payload);
	}

	private void processPublishPayload(String uri, JsonElement data) throws JMSException {

		if(data.isJsonPrimitive() || data.isJsonArray()){
			sendPublishToSubscribers(uri, data);
			return;
		}
		
		if(!data.isJsonObject()){
			logger.warn("Invalid publish payload, not json object, primitive no array, ignoring");
			return;
		}
		
		for(Entry<String, JsonElement> entry:data.getAsJsonObject().entrySet()){
			sendPublishToSubscribers(entry.getKey(),entry.getValue());

			//send to subcribers of superproperty
			Set<String> superPropertyUris = jOntology.getSuperProperties(entry.getKey());
			for(String superRuri:superPropertyUris){
				sendPublishToSubscribers(superRuri, entry.getValue());
			}
			
			//send to subcribers of equivalent property
			Set<String> equivalentPropertiesUris = jOntology.getEquivalentProperties(entry.getKey());
			for(String equivalentUri:equivalentPropertiesUris){
				sendPublishToSubscribers(equivalentUri, entry.getValue());
			}
			
		}
		
		for(Entry<String, JsonElement> entry:data.getAsJsonObject().entrySet()){
			processPublishPayload(entry.getKey(), entry.getValue());
		}
		
		
	}

	private void sendPublishToSubscribers(String ruri, JsonElement data) throws JMSException {
		for(PersistedSubscriptionRequest sub:delegate.getDataSubscriptions(ruri)){
			if(sub.getExpiry() > 0 && System.currentTimeMillis() > (sub.getTimestampMillis() + sub.getExpiry()) ){
				delegate.removePersistedSubscriptionRequest(ruri, sub.getSubscriptionId());
				continue;
			}
				
			JsonElement responsesThatMeetCriteria = extractResponsesThatMeetCriteria(data, sub.getTriggerList());
			if(responsesThatMeetCriteria != null){
				if(sub.getResponseType().equalsIgnoreCase("application/ld+json")){
					JsonElement jsonLDResponse = jsonLDTransformer.parseResponse(responsesThatMeetCriteria);
					sendSubResponse(sub.getCorrelationID(), sub.getReply2q(), jsonLDResponse.toString());
				}
				else{
					sendSubResponse(sub.getCorrelationID(), sub.getReply2q(), responsesThatMeetCriteria.toString());
				}
			}
		}		
	}
	
	private void processPostRequest(JsonElement jsonRequest, String ruri, String correlationID, Destination reply2q, long timestampMillis) throws JasperRequestException, JMSException {
		if (!jOntology.isRuriKnownForInputPost(ruri)){
			delegate.removePersistedRequest(persistedRequest);
			logger.error("ruri " + ruri + " is not known for POST sending back error response for correlationID " + correlationID);
			throw new JasperRequestException(JasperConstants.ResponseCodes.NOTFOUND, ruri + " is not known for POST");
		}
		
		String processing_scheme;
		if (jsonRequest.isJsonObject() && jsonRequest.getAsJsonObject().has(JasperConstants.HEADERS_LABEL) && jsonRequest.getAsJsonObject().get(JasperConstants.HEADERS_LABEL).isJsonObject()
				&& jsonRequest.getAsJsonObject().get(JasperConstants.HEADERS_LABEL).getAsJsonObject().has("processing-scheme")
				&& jsonRequest.getAsJsonObject().get(JasperConstants.HEADERS_LABEL).getAsJsonObject().get("processing-scheme").isJsonPrimitive()
				&& jsonRequest.getAsJsonObject().get(JasperConstants.HEADERS_LABEL).getAsJsonObject().get("processing-scheme").getAsJsonPrimitive().isString()) {
			processing_scheme = jsonRequest.getAsJsonObject().get(JasperConstants.HEADERS_LABEL).getAsJsonObject().get("processing-scheme").getAsJsonPrimitive().getAsString();
		} else {
			processing_scheme = JasperConstants.DEFAULT_PROCESSING_SCHEME;
		}

		DataProcessor dataProcessor = DataProcessorFactory.createDataProcessor(processing_scheme);
		
		ArrayList<String> operations = jOntology.fetchPostOperations(ruri);
		
		JsonObject parameters = getRequestParameters(jsonRequest);
		
		//TODO re-write to do operations in parallel, currently done sequentially.
		for(String operation:operations){
			JsonObject inputObject = getInputObject(jOntology.fetchPostOperationInputObject(operation),parameters);
			if(inputObject == null)
			{
				continue;
			}
			JsonElement response = sendAndWaitforResponse(jOntology.fetchPostDestinationQueue(operation),inputObject.toString());
			if (response == null) {
				//TODO determine if we need return NULL response back to client?
				
				// Status code should be 200.  Note that if status code was not 200, sendAndWaitforResponse would have thrown a jasperException.
				response = jsonParser.parse("{\"http://coralcea.ca/jasper/responseCode\" : 200,\"http://coralcea.ca/jasper/responseReason\" : \"OK\"}");
			}
			dataProcessor.add(response);
		}
		
		JsonElement postResponse = dataProcessor.process();	
		
		if(postResponse != null){
			if(response_type.equalsIgnoreCase("application/ld+json")){
				JsonElement jsonLDResponse = jsonLDTransformer.parseResponse(postResponse);
				sendResponse(correlationID, reply2q, jsonLDResponse.toString());
			}
			else{
				sendResponse(correlationID, reply2q, postResponse.toString());
			}
		}else{
			throw new JasperRequestException(JasperConstants.ResponseCodes.NOTFOUND, ruri + " POST response is null, request failed");
		}
	}

	private void processGetRequest(JsonElement jsonRequest, String ruri, String correlationID, Destination reply2q, long timestampMillis) throws JMSException, JasperRequestException {
		
		if (!jOntology.isRuriKnownForOutputGet(ruri)){
			delegate.removePersistedRequest(persistedRequest);
			logger.error("ruri " + ruri + " is not known sending back error response for correlationID " + correlationID);
			throw new JasperRequestException(JasperConstants.ResponseCodes.NOTFOUND, ruri + " is not known");
		}
		
		String processing_scheme;
		if (jsonRequest.isJsonObject() && jsonRequest.getAsJsonObject().has(JasperConstants.HEADERS_LABEL) && jsonRequest.getAsJsonObject().get(JasperConstants.HEADERS_LABEL).isJsonObject()
				&& jsonRequest.getAsJsonObject().get(JasperConstants.HEADERS_LABEL).getAsJsonObject().has("processing-scheme")
				&& jsonRequest.getAsJsonObject().get(JasperConstants.HEADERS_LABEL).getAsJsonObject().get("processing-scheme").isJsonPrimitive()
				&& jsonRequest.getAsJsonObject().get(JasperConstants.HEADERS_LABEL).getAsJsonObject().get("processing-scheme").getAsJsonPrimitive().isString()) {
			processing_scheme = jsonRequest.getAsJsonObject().get(JasperConstants.HEADERS_LABEL).getAsJsonObject().get("processing-scheme").getAsJsonPrimitive().getAsString();
		} else {
			processing_scheme = JasperConstants.DEFAULT_PROCESSING_SCHEME;
		}

		JsonObject parameters = getRequestParameters(jsonRequest);
		
		String rule;
		if (jsonRequest.isJsonObject() && jsonRequest.getAsJsonObject().has(JasperConstants.RULE_LABEL)
				&& jsonRequest.getAsJsonObject().get(JasperConstants.RULE_LABEL).isJsonPrimitive()
				&& jsonRequest.getAsJsonObject().get(JasperConstants.RULE_LABEL).getAsJsonPrimitive().isString()) {
			rule = jsonRequest.getAsJsonObject().get(JasperConstants.RULE_LABEL).getAsJsonPrimitive().getAsString();
		} else {
			rule = null;
		}
		
		int polling_period;
		if (jsonRequest.isJsonObject() && jsonRequest.getAsJsonObject().has(JasperConstants.HEADERS_LABEL) && jsonRequest.getAsJsonObject().get(JasperConstants.HEADERS_LABEL).isJsonObject()
				&& jsonRequest.getAsJsonObject().get(JasperConstants.HEADERS_LABEL).getAsJsonObject().has(JasperConstants.POLL_PERIOD_LABEL)
				&& jsonRequest.getAsJsonObject().get(JasperConstants.HEADERS_LABEL).getAsJsonObject().get(JasperConstants.POLL_PERIOD_LABEL).isJsonPrimitive()
				&& jsonRequest.getAsJsonObject().get(JasperConstants.HEADERS_LABEL).getAsJsonObject().get(JasperConstants.POLL_PERIOD_LABEL).getAsJsonPrimitive().isNumber()) {
			polling_period = jsonRequest.getAsJsonObject().get(JasperConstants.HEADERS_LABEL).getAsJsonObject().get(JasperConstants.POLL_PERIOD_LABEL).getAsJsonPrimitive().getAsInt();
		} else {
			polling_period = (rule!=null)?delegate.maxPollingInterval:0;
		}
		polling_period = (polling_period > delegate.maxPollingInterval)?delegate.maxPollingInterval:polling_period;
		polling_period = (polling_period < delegate.minPollingInterval)?delegate.minPollingInterval:polling_period;
				
		int expiry;
		if (jsonRequest.isJsonObject() && jsonRequest.getAsJsonObject().has(JasperConstants.HEADERS_LABEL) && jsonRequest.getAsJsonObject().get(JasperConstants.HEADERS_LABEL).isJsonObject()
				&& jsonRequest.getAsJsonObject().get(JasperConstants.HEADERS_LABEL).getAsJsonObject().has(JasperConstants.EXPIRES_LABEL)
				&& jsonRequest.getAsJsonObject().get(JasperConstants.HEADERS_LABEL).getAsJsonObject().get(JasperConstants.EXPIRES_LABEL).isJsonPrimitive()
				&& jsonRequest.getAsJsonObject().get(JasperConstants.HEADERS_LABEL).getAsJsonObject().get(JasperConstants.EXPIRES_LABEL).getAsJsonPrimitive().isNumber()) {
			expiry = jsonRequest.getAsJsonObject().get(JasperConstants.HEADERS_LABEL).getAsJsonObject().get(JasperConstants.EXPIRES_LABEL).getAsJsonPrimitive().getAsInt();
		} else {
			expiry = (rule!=null)?delegate.maxExpiry:0;
		}
		
		expiry = (expiry > delegate.maxExpiry)?delegate.maxExpiry:expiry;
		
		JsonElement response = null;
		
		List<Trigger> triggerList = parseTriggers(rule);
		
		if(expiry == 0){
			response = getResponse(ruri, parameters, processing_scheme);
			
			if (response == null) {
				delegate.removePersistedRequest(persistedRequest);
				logger.error("response from getResponse is null, sending back error response for correlationID " + correlationID);
				if(errorDescription.length() > 0){
					throw new JasperRequestException(JasperConstants.ResponseCodes.NOTFOUND, ruri + " is known, however " + errorDescription.toString());
				}
				else{
					throw new JasperRequestException(JasperConstants.ResponseCodes.NOTFOUND, ruri + " is known, but we could not get a valid response, check parameters");
				}
			}
			
			JsonElement responsesThatMeetCriteria = extractResponsesThatMeetCriteria(response, triggerList);
			if(responsesThatMeetCriteria != null){
				if(response_type.equalsIgnoreCase("application/ld+json")){
					JsonElement jsonLDResponse = jsonLDTransformer.parseResponse(responsesThatMeetCriteria);
					sendResponse(correlationID, reply2q, jsonLDResponse.toString());
				}
				else{
					sendResponse(correlationID, reply2q, responsesThatMeetCriteria.toString());
				}
			}else{
				delegate.removePersistedRequest(persistedRequest);
				logger.error("response from getResponse does not match rule, and expiry = 0, unable to respond with valid response for correlationID " + correlationID);
				throw new JasperRequestException(JasperConstants.ResponseCodes.NOTFOUND, ruri + " cannot be found matching rule : " + rule);
			}
			
		}else{
			do{
				response = extractResponsesThatMeetCriteria(getResponse(ruri, parameters, processing_scheme),triggerList);
				if(response != null || ( System.currentTimeMillis() > (timestampMillis+expiry) ) || errorDescription.length() > 0){
					break;
				}
				try {
					if( (System.currentTimeMillis() + polling_period) < (timestampMillis + expiry) ){
						Thread.sleep(polling_period);
					}else{
						Thread.sleep(timestampMillis + expiry - System.currentTimeMillis()); 
					}
				} catch (InterruptedException e) {
					logger.error("polling sleep interrupted", e);
				}
			}while(true);
			
			if (response == null) {
				delegate.removePersistedRequest(persistedRequest);
				logger.error("responses from polling getResponse is null, sending back error response for correlationID " + correlationID);
				if(errorDescription.length() > 0){
					throw new JasperRequestException(JasperConstants.ResponseCodes.NOTFOUND, ruri + " is known, however " + errorDescription.toString());
				}
				else{
					throw new JasperRequestException(JasperConstants.ResponseCodes.NOTFOUND, ruri + " is known, but we could not get a valid response before request expired");
				}
			}else if(response_type.equalsIgnoreCase("application/ld+json")){
				JsonElement jsonLDResponse = jsonLDTransformer.parseResponse(response);
				sendResponse(correlationID, reply2q, jsonLDResponse.toString());
			}
			else{
				sendResponse(correlationID, reply2q, response.toString());
			}
			
		}
	}
	
	private JsonObject getRequestParameters(JsonElement jsonRequest) {
		JsonObject parameters = null;
		if (jsonRequest.isJsonObject() && jsonRequest.getAsJsonObject().has(JasperConstants.PARAMETERS_LABEL) && jsonRequest.getAsJsonObject().get(JasperConstants.PARAMETERS_LABEL).isJsonObject()) {
			parameters = jsonRequest.getAsJsonObject().get(JasperConstants.PARAMETERS_LABEL).getAsJsonObject();
		}
		return (parameters == null)?new JsonObject():parameters;
	}

	private JsonElement extractResponsesThatMeetCriteria(JsonElement response,List<Trigger> triggerList) {
		if(response == null) return null;
		if(triggerList == null) return response;
		
		if (response.isJsonArray()) {
			JsonArray matchedValues = new JsonArray();
			JsonArray jsonArray = response.getAsJsonArray();
			if (jsonArray.size() == 0)
				return null;
			for (int i = 0; i < triggerList.size(); i++) {
				for (JsonElement item : jsonArray) {
					if (triggerList.get(i).evaluate(item)) {
						matchedValues.add(item);
					}
				}
			}
			if (matchedValues.size() > 0) {
				return matchedValues;
			}
		} else {
			for (Trigger trigger:triggerList) {
				if (trigger.evaluate(response)) {
					return response;
				}
			}
		}
		return null;
	}
	
	private List<Trigger> parseTriggers(String rule) throws JasperRequestException{
		if(rule == null) return null;
		String[] triggers = rule.split("&");
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
		List<Trigger> triggerList = new ArrayList<Trigger>();
		for(int i=0; i<functions.length;i++){
			triggerParms = parms[i].split(",");
			trigger = factory.createTrigger(functions[i], triggerParms);
			if(trigger != null){
				triggerList.add(trigger);
			}
			else{
				logger.error("Invalid notification request received - cannot create rule: " + triggerParms.toString());
				throw new JasperRequestException(JasperConstants.ResponseCodes.BADREQUEST,  "bad rule : " + rule);
			}
		}	
		return triggerList;
	}

	private JsonElement getResponse(String ruri, JsonObject parameters, String processing_scheme) throws JMSException, JasperRequestException {
		DataProcessor dataProcessor = DataProcessorFactory.createDataProcessor(processing_scheme);

        Set<String> setString = jOntology.getEquivalents(ruri);

        for (String uri : jOntology.getEquivalents(ruri)) {
        	JsonElement response = getResponseSingleUri(uri, parameters, processing_scheme);
        	if(response != null) dataProcessor.add(response);
        }

        JsonElement singleResponse = getResponseSingleUri(ruri, parameters, processing_scheme);
        if(singleResponse != null) dataProcessor.add(singleResponse);

        JsonElement result = dataProcessor.process();
        return result;
	}

	private JsonElement getResponseSingleUri(String ruri, JsonObject parameters, String processing_scheme) throws JMSException, JasperRequestException {
		if (!jOntology.isRuriKnownForOutputGet(ruri)) return null;

		DataProcessor dataProcessor = DataProcessorFactory.createDataProcessor(processing_scheme);
		ArrayList<String> operations = jOntology.getProvideOperations(ruri);

		// TODO re-write to do operations in parallel, currently done
		// sequentially.
		for (String operation : operations) {
			JsonObject inputObject = getInputObject(jOntology.getProvideOperationInputObject(operation), parameters);
			if (inputObject == null)
				continue;
			JsonElement response = sendAndWaitforResponse(jOntology.getProvideDestinationQueue(operation), inputObject.toString());
			dataProcessor.add(extractRuriData(ruri, response));
		}

		JsonElement result = dataProcessor.process();
		return result;
	}

	private JsonElement extractRuriData(String ruri, JsonElement response) {

		if (response == null)
			return null;
		
		if (response.isJsonPrimitive()) {
			// if primitive assume it is the data we want.
			return response;
		} else if (response.isJsonObject()) {

			JsonObject responseObject = response.getAsJsonObject();

			// Check 1 level deep
			for (Entry<String, JsonElement> entry : responseObject.entrySet()) {
				if (entry.getKey().equals(ruri))
					return entry.getValue();
			}

			// check n level deep
			for (Entry<String, JsonElement> entry : responseObject.entrySet()) {

				if (entry.getValue().isJsonPrimitive())
					continue;

				JsonElement tmpResponse = extractRuriData(ruri, entry.getValue());
				if (tmpResponse != null)
					return tmpResponse;
			}

			return responseObject;

		} else if (response.isJsonArray()) {

			JsonArray responseArray = response.getAsJsonArray();
			JsonArray array = new JsonArray();

			for (JsonElement item : responseArray) {
				JsonElement tmpItem = extractRuriData(ruri, item);
				if (tmpItem != null)
					array.add(tmpItem);
			}
			return (array.size() > 0) ? array : null;
		}

		return null;
	}

	private JsonElement sendAndWaitforResponse(String provideDestinationQueue, String msgText) throws JMSException, JasperRequestException {	
		Message msg = delegate.createTextMessage(msgText);

		String correlationID = UUID.randomUUID().toString();
		msg.setJMSCorrelationID(correlationID);

		Message response;
		Object lock = new Object();
		synchronized (lock) {
			locks.put(correlationID, lock);
			delegate.sendMessage(provideDestinationQueue, msg);
			int count = 0;
			while (!responses.containsKey(correlationID)) {
				try {
					lock.wait(10000);
				} catch (InterruptedException e) {
					logger.error("Interrupted while waiting for lock notification", e);
				}
				count++;
				if (count >= 6)
					break;
			}
			response = responses.remove(correlationID);
		}

		String responseString = null;
		if (response != null && response.getJMSCorrelationID().equals(correlationID) && response instanceof TextMessage) {
			responseString = ((TextMessage) response).getText();
			if ((response.getIntProperty("code") != 200) && (responseString == null))
			{
				throw new JasperRequestException(JasperConstants.ResponseCodes.NOTFOUND, "null response, response code = " + response.getIntProperty("code"));
			}
		}
		
		//TODO added creation of error repsonses based on response code and possible timeout
		
		JsonElement responseObject = null;
		try {
			if (responseString != null) 
				responseObject = jsonParser.parse(responseString);
		} catch (JsonSyntaxException e) {
			logger.error("response from DTA is not a valid JsonReponse, response string = " + responseString);
		}

		return responseObject;
	}

	private void processInvalidRequest(String correlationID, Destination dstQ, JasperConstants.ResponseCodes respCode, String respMsg) throws JMSException {
		if (logger.isInfoEnabled()) {
			logger.info("processingInvalidRequest, errorMsg = " + respMsg + " for request with correlationID " + correlationID + " from " + dstQ);
		}
		String msgText = delegate.createJasperResponse(respCode, respMsg, null, "application/json", JasperConstants.VERSION_1_0);
		Message message = delegate.createTextMessage(msgText);
		message.setJMSCorrelationID(correlationID);

		delegate.sendMessage(dstQ, message);
	}

	private void sendResponse(String correlationID, Destination dstQ, String response) throws JMSException {
		JasperConstants.ResponseCodes code = JasperConstants.ResponseCodes.OK;
		String jsonResponse = delegate.createJasperResponse(code, "Success", response, "application/json", JasperConstants.VERSION_1_0);
		Message message = delegate.createTextMessage(jsonResponse);
		message.setJMSCorrelationID(correlationID);
		delegate.sendMessage(dstQ, message);
	}

	private void sendSubResponse(String correlationID, Destination dstQ, String response) throws JMSException {
    	Message message = delegate.createTextMessage(response);
        message.setJMSCorrelationID(correlationID);
        delegate.sendMessage(dstQ, message);
	}

	private JsonObject getInputObject(String ruri, JsonObject parameters) throws JMSException, JasperRequestException {

		JsonObject schema = jOntology.createJsonSchema(ruri);

		if (schema.has("required")) {
			if (!isAllRequiredParametersPassed(schema.get("required").getAsJsonArray(), parameters)) {
				// TODO add more info regarding which parameter is missing
				return null;
			}
		}

		JsonObject result = new JsonObject();

		if (!schema.has("type")) {
			logger.warn("invalid schema missing, type.");
			return null;
		}

		String type = schema.getAsJsonPrimitive("type").getAsString();

		if (!"object".equals(type)) {
			logger.info("ruri " + ruri + " is not of type object, all input objects must be of type object. Returning NULL");
			return null;
		}

		if (schema.has("id")) {
			result.add("@type", schema.getAsJsonPrimitive("id"));
		}

		if (!schema.has("properties")) {
			logger.warn("no properites to add, returning result");
			return result;
		} else if (!schema.get("properties").isJsonObject()) {
			logger.warn("invalid schema properites is not of type object, returning null.");
			return null;
		}

		JsonObject properties = schema.get("properties").getAsJsonObject();

		for (Entry<String, JsonElement> entry : properties.entrySet()) {
			if (!parameters.has(entry.getKey())) {
				logger.info("property " + entry.getKey() + " is not passed as parameter, it is not mandatory (we would have failed earlier if it was), ignoring property.");
				continue;
			}

			String propertyName = entry.getKey();
			JsonObject propertySchema = entry.getValue().getAsJsonObject();
			JsonElement property = parameters.get(propertyName);

			if (isValidJsonObject(propertySchema, property, propertyName)) {
				result.add(propertyName, property);
			} else {

				JsonElement subPropertyResult = null;

				if (propertySchema.has("required") && isAllRequiredParametersPassed(propertySchema.get("required").getAsJsonArray(), propertySchema)) {
					if (property.isJsonObject()) {
						subPropertyResult = getInputObject(propertyName, property.getAsJsonObject());
					} else {
						logger.warn("cannot getInputObject of sub property " + propertyName + " and it passed parameters is not a jsonObject");
					}
				} else {
					if (property.isJsonObject()) {

						String subPropertyProcessingScheme = (propertySchema.has("type") && "array".equals(propertySchema.getAsJsonObject().get("type").getAsString())) ? JasperConstants.AGGREGATE_SCHEME
								: JasperConstants.COALESCE_SCHEME;
						if (logger.isInfoEnabled()) {
							logger.info("sub property type : " + (property.getAsJsonObject().get("type")) + " setting processing scheme to : " + subPropertyProcessingScheme);
						}
						subPropertyResult = getResponse(propertyName, property.getAsJsonObject(), subPropertyProcessingScheme);
					} else {
						logger.warn("cannot getInputObject of sub property " + propertyName + " and it passed parameters is not a jsonObject");
					}

				}

				if (subPropertyResult == null && isPropertyRequired(propertyName, schema)) {
					logger.warn("cannot fetch or build property " + propertyName + " and it is required, therefore we cannot build the object, returning null");
					return null;
				} else if (subPropertyResult == null) {
					logger.warn("cannot fetch or build property " + propertyName + " and it is not required, ignoring");
				} else {
					result.add(propertyName, subPropertyResult);
				}
			}

		}

		return result;
	}

	private boolean isPropertyRequired(String propertyName, JsonObject schema) {
		if (!schema.has("required"))
			return true;

		JsonElement required = schema.get("required");

		if (!required.isJsonArray()) {
			logger.warn("property " + propertyName
					+ " schema has required JsonElement that is not a JsonArray, this is an invalid schema, returning that the property is required as default behaviour");
			return true;
		}

		for (JsonElement entry : required.getAsJsonArray()) {
			if (propertyName.equals(entry.getAsString()))
				return true;
		}

		return false;
	}

	private boolean isValidJsonObject(JsonObject propertySchema, JsonElement property, String propertyName) {
		if (!propertySchema.has("type")) {
			logger.warn("invalid schema missing, type.");
			return false;
		}

		String type = propertySchema.getAsJsonPrimitive("type").getAsString();

		switch (type) {
		case "object":
			JsonObject properties = propertySchema.get("properties").getAsJsonObject();
			if (!property.isJsonObject())
				return false;

			if (propertySchema.has("required")) {

				if (!property.isJsonObject()) {
					logger.warn("property is not JsonObject: " + property);
				}

				if (!isAllRequiredParametersPassed(propertySchema.get("required").getAsJsonArray(), property.getAsJsonObject())) {
					// TODO add more info regarding which parameter is missing
					return false;
				}
			}

			for (Entry<String, JsonElement> entry : properties.entrySet()) {
				String subPropertyName = entry.getKey();
				JsonObject subPropertySchema = entry.getValue().getAsJsonObject();
				if (!isValidJsonObject(subPropertySchema, property.getAsJsonObject().get(subPropertyName), subPropertyName)) {
					return false;
				}
			}
			return true;

			// TODO update jOntology to return jsonScheme that matches json
			// schema types, not xml schema
		case "string":
		case "String":
		case "http://www.w3.org/2001/XMLSchema#string":
			if(property.isJsonPrimitive() && property.getAsJsonPrimitive().isString()){
				return true;
			}
			else{
				errorDescription.append("parameter " + propertyName + " is supposed to be of type String");
				return false;
			}
		case "array":
		case "Array":
			// TODO add array validation
			logger.error("array validation isn't currently supported");
			return false;
		case "integer":
		case "Integer":
		case "http://www.w3.org/2001/XMLSchema#integer":
			if(property.isJsonPrimitive() && property.getAsJsonPrimitive().isNumber()){
				return true;
			}
			else{
				errorDescription.append("parameter " + propertyName + " is supposed to be of type integer");
				return false;
			}
		case "boolean":
		case "Boolean":
		case "http://www.w3.org/2001/XMLSchema#boolean":
			if(property.isJsonPrimitive() && property.getAsJsonPrimitive().isBoolean()){
				return true;
			}
			else{
				errorDescription.append("parameter " + propertyName + " is supposed to be of type boolean");
				return false;
			}
		}

		return true;
	}

	private boolean isAllRequiredParametersPassed(JsonArray jsonArray, JsonObject parameters) {
		boolean response = true;
		for (JsonElement entry : jsonArray) {
			if (!(entry.isJsonPrimitive() && entry.getAsJsonPrimitive().isString() && parameters.has(entry.getAsString()))) {
				if (entry.isJsonPrimitive() && entry.getAsJsonPrimitive().isString()) {
					logger.info("required paramter missing : " + entry.getAsString());
				} else {
					logger.info("required paramter not primitive : " + entry);
				}
				response = false;
			}
		}

		return response;
	}

}
