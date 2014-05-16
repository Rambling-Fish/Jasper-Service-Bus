package org.jasper.core.delegate.handlers;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.log4j.Logger;
import org.jasper.core.constants.JasperConstants;
import org.jasper.core.delegate.Delegate;
import org.jasper.core.delegate.DelegateOntology;
import org.json.JSONException;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class SparqlHandler implements Runnable {

	private Delegate delegate;
	private DelegateOntology jOntology;
	private Message jmsRequest;
	private String version;
	private String contentType;
	
	private static Logger logger = Logger.getLogger(SparqlHandler.class.getName());



	public SparqlHandler(Delegate delegate, Message jmsRequest) {
		this.delegate = delegate;
		this.jOntology = delegate.getJOntology();
		this.jmsRequest = jmsRequest;
		
	}

	public void run() {
		try{
			String[] parsedSparqlRequest = parseSparqlRequest(((TextMessage)jmsRequest).getText());
			String errorMsg;
			if(parsedSparqlRequest != null){
        		String queryString = parsedSparqlRequest[0];
				String output;
				output = (parsedSparqlRequest.length > 1) ? parsedSparqlRequest[1]:"json";
				if(output.isEmpty()){
					output = "json";
				}
				// We only support json or xml as output formats
				if(!output.equalsIgnoreCase("json") && (!output.equalsIgnoreCase("xml"))){
					errorMsg="Output must be set to json or xml";
					processInvalidRequest((TextMessage) jmsRequest, JasperConstants.ResponseCodes.BADREQUEST, errorMsg);
					return;
				}
				sendResponse((TextMessage)jmsRequest, jOntology.queryModel(queryString, output));
			}
			else{
				errorMsg = "Invalid SPARQL query received";
				processInvalidRequest((TextMessage)jmsRequest, JasperConstants.ResponseCodes.BADREQUEST, errorMsg);
			}
		}catch (Exception e){
			logger.error("Exception caught processing SPARQL request " ,e);
		}
	}
	
	private String[] parseSparqlRequest(String text) {
		String[] result = null;
	
		if(text==null)return null;
		
		try {
			JsonElement jelement = new JsonParser().parse(text);
		    JsonObject  jsonObj = jelement.getAsJsonObject();
			version = jsonObj.get(JasperConstants.VERSION_LABEL).getAsString();
			
			if(jsonObj.has(JasperConstants.PAYLOAD_LABEL)) {
				JsonArray jsonArr = (JsonArray) jsonObj.get(JasperConstants.PAYLOAD_LABEL);
				String query = new String(getByteArray(jsonArr));
				result = query.split("output=");
				try {
					result[0] = URIUtil.decode(result[0]);
					result[0] = result[0].replaceFirst("query=", "");
					if(!result[0].contains("SELECT") && (!result[0].contains("select"))){
						return null;
					}
				} catch (URIException e) {
					logger.error("Exception when decoding encoded sparql query, returning null " ,e);
					return null;
				}
				if(result.length == 2){
					result[1] = result[1].replaceFirst("output=", "");
				}
			}
			
			Map<String, String> headers = getMap(jsonObj.get(JasperConstants.HEADERS_LABEL).getAsJsonObject());
			for(String s:headers.keySet()){
				switch (s.toLowerCase()) {
				case JasperConstants.CONTENT_TYPE_LABEL :
					contentType = headers.get(s);
					break;
				}
			}
			
		} catch (JSONException e) {
			logger.error("Exception caught while creating JsonObject " + e);
			return null;
		}
		
		return result;
	}
	
	/*
	 * Whenever an error is detected in incoming request rather than letting
	 * the client timeout, the core responds with an empty JSON message.
	 * Currently this occurs if incoming URI cannot be mapped to a JTA or the
	 * incoming correlationID is not unique
	 */
	private void processInvalidRequest(TextMessage msg, JasperConstants.ResponseCodes responseCode, String responseMsg) throws Exception {
		if(logger.isInfoEnabled()){
			logger.info("processingInvalidRequest, errorMsg = " + responseMsg + " for request " + msg.getText() + " from " + msg.getJMSReplyTo());
		}
		String response = delegate.createJasperResponse(responseCode, responseMsg, null, contentType, version);
        Message message = delegate.createTextMessage(response);

        
        if(msg.getJMSCorrelationID() == null){
            message.setJMSCorrelationID(msg.getJMSMessageID());
  	  	}else{
            message.setJMSCorrelationID(msg.getJMSCorrelationID());
  	  	}
        
        delegate.sendMessage(msg.getJMSReplyTo(),message);
	}
	
	private byte[] getByteArray(JsonArray array) {
		byte[] result = new byte[array.size()];
		for(int index = 0 ; index < array.size() ;  index++){
			result[index] = array.get(index).getAsByte();
		}
		return result;
	}
	
	private Map<String, String> getMap(JsonObject json) {
		Map<String, String> map = new HashMap<String, String>();
		for(Entry<String, JsonElement> entry:json.entrySet()){
			map.put(entry.getKey(),entry.getValue().getAsString());
		}
		return map;
	}
        
	private void sendResponse(TextMessage msg, String response) throws JMSException {
		JasperConstants.ResponseCodes code = JasperConstants.ResponseCodes.OK;
		String jsonResponse = delegate.createJasperResponse(code, "Success", response, contentType, version);
		Message message = delegate.createTextMessage(jsonResponse);      
		String correlationID = msg.getJMSCorrelationID();
		if(correlationID == null) correlationID = msg.getJMSMessageID();
		message.setJMSCorrelationID(correlationID);
			
		delegate.sendMessage(msg.getJMSReplyTo(), message);
	        
		}
	
}
