package org.jasper.core.delegate.handlers;

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
import org.json.JSONObject;

public class SparqlHandler implements Runnable {

	private Delegate delegate;
	private DelegateOntology jOntology;
	private Message jmsRequest;
	private String ruri;
	private String method;
	private String version;
	private String contentType;
	
	private static Logger logger = Logger.getLogger(SparqlHandler.class.getName());



	public SparqlHandler(Delegate delegate, DelegateOntology jOntology, Message jmsRequest) {
		this.delegate = delegate;
		this.jOntology = jOntology;
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
					processInvalidRequest((TextMessage) jmsRequest, JasperConstants.responseCodes.BADREQUEST, errorMsg);
					return;
				}
				sendResponse((TextMessage)jmsRequest, jOntology.queryModel(queryString, output));
			}
			else{
				errorMsg = "Invalid SPARQL query received";
				processInvalidRequest((TextMessage)jmsRequest, JasperConstants.responseCodes.BADREQUEST, errorMsg);
			}
		}catch (Exception e){
			logger.error("Exception caught processing SPARQL request " ,e);
		}
	}
	
	private String[] parseSparqlRequest(String text) {
		JSONObject parms = new JSONObject();
		JSONObject headers = new JSONObject();
		StringBuilder sb = new StringBuilder();
	
		if(text==null)return null;
		
		try {
			JSONObject jsonObj = new JSONObject(text);
			version = jsonObj.getString(JasperConstants.VERSION_LABEL);
			
			if(jsonObj.has(JasperConstants.PARAMETERS_LABEL)) {
				parms = (JSONObject)jsonObj.getJSONObject(JasperConstants.PARAMETERS_LABEL);
				int len = parms.length();
				for(Object o:parms.keySet()){
					sb.append(parms.get(o.toString()));
					if(len > 1) {
						sb.append("&");
						len--;
					}
				}
			
				if(!sb.toString().contains("SELECT") && (!sb.toString().contains("select"))){
					return null;
				}
			}
			
			if(jsonObj.has(JasperConstants.HEADERS_LABEL)){
				headers = (JSONObject)jsonObj.getJSONObject(JasperConstants.HEADERS_LABEL);
				for(Object o:headers.keySet()){
					switch (o.toString().toLowerCase()) {
					case JasperConstants.CONTENT_TYPE_LABEL :
						contentType = headers.getString(o.toString());
						break;
					}
				}
			}
			
		} catch (JSONException e) {
			logger.error("Exception caught while creating JSONObject " + e);
			return null;
		}
		
		String[] result = null;
		result = sb.toString().split("&");
		
		if(result.length != 2) return null;
		
		try {
			result[0] = URIUtil.decode(result[0]);
			result[1] = URIUtil.decode(result[1]);
		} catch (URIException e) {
			logger.error("Exception when decoding encoded sparql query, returning null " ,e);
			result = null;
		}
		
		result[0] = result[0].replaceFirst("query=", "");
		result[1] = result[1].replaceFirst("output=", "");
		
		return result;
	}
	
	/*
	 * Whenever an error is detected in incoming request rather than letting
	 * the client timeout, the core responds with an empty JSON message.
	 * Currently this occurs if incoming URI cannot be mapped to a JTA or the
	 * incoming correlationID is not unique
	 */
	private void processInvalidRequest(TextMessage msg, JasperConstants.responseCodes responseCode, String responseMsg) throws Exception {
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
        
	private void sendResponse(TextMessage msg, String response) throws JMSException {
		JasperConstants.responseCodes code = JasperConstants.responseCodes.OK;
		String jsonResponse = delegate.createJasperResponse(code, "Success", response, contentType, version);
		Message message = delegate.createTextMessage(jsonResponse);      
		String correlationID = msg.getJMSCorrelationID();
		if(correlationID == null) correlationID = msg.getJMSMessageID();
		message.setJMSCorrelationID(correlationID);
			
		delegate.sendMessage(msg.getJMSReplyTo(), message);
	        
		}
	
}