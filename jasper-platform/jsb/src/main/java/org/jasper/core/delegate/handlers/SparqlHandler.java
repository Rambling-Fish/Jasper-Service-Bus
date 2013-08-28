package org.jasper.core.delegate.handlers;

import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.log4j.Logger;
import org.jasper.core.delegate.Delegate;
import org.jasper.core.delegate.DelegateOntology;

public class SparqlHandler implements Runnable {

	private Delegate delegate;
	private DelegateOntology jOntology;
	private Message jmsRequest;
//	private Map<String, Object> locks;
//	private Map<String, Message> responses;
	
	private static Logger logger = Logger.getLogger(SparqlHandler.class.getName());



	public SparqlHandler(Delegate delegate, DelegateOntology jOntology, Message jmsRequest, Map<String,Object> locks, Map<String,Message> responses) {
		this.delegate = delegate;
		this.jOntology = jOntology;
		this.jmsRequest = jmsRequest;
//		this.locks = locks;
//		this.responses = responses;
		
	}

	public void run() {
		try{
			String[] parsedSparqlRequest = parseSparqlRequest(((TextMessage)jmsRequest).getText());
			if(parsedSparqlRequest != null){
        		String queryString = parsedSparqlRequest[0];
				String output;
				output = (parsedSparqlRequest.length > 1) ? parsedSparqlRequest[1]:"TTL"; // TODO change TTL to json or xml
				sendResponse((TextMessage)jmsRequest, jOntology.queryModel(queryString, output));	
			}
			else{
				String errorMsg = "Invalid SPARQL query received";
				processInvalidRequest((TextMessage) jmsRequest, errorMsg);
			}
		}catch (Exception e){
			logger.error("Exception caught processing SParQL request " ,e);
		}
	}
	
	private String[] parseSparqlRequest(String text) {
		if(text==null)return null;
		String[] result = null;
		if(!text.startsWith("?query=")){
			return null;
		}
		
		text = text.substring("?query=".length(), text.length());
		result = text.split("&");
		
		if(result.length != 2) return null;
		
		try {
			result[0] = URIUtil.decode(result[0]);
			result[1] = URIUtil.decode(result[1]);
		} catch (URIException e) {
			logger.error("Exception when decoding encoded sparql query, returning null " ,e);
			result = null;
		}
		
		result[1] = result[1].replaceFirst("output=", "");
		
		return result;
	}
	
	/*
	 * Whenever an error is detected in incoming request rather than letting
	 * the client timeout, the core responds with an empty JSON message.
	 * Currently this occurs if incoming URI cannot be mapped to a JTA or the
	 * incoming correlationID is not unique
	 */
	private void processInvalidRequest(Message msg, String errorMsg) throws Exception {

        // For now we always send back empty JSON for all error scenarios
		String msgText = "{".concat(errorMsg).concat("}");
        Message message = delegate.createTextMessage(msgText);
        if(msg.getJMSCorrelationID() == null){
            message.setJMSCorrelationID(msg.getJMSMessageID());
  	  	}else{
            message.setJMSCorrelationID(msg.getJMSCorrelationID());
  	  	}
     
        delegate.sendMessage(msg.getJMSReplyTo(), message);

	}

	private void sendResponse(TextMessage jmsRequestMsg,String response) throws JMSException {
        
        Message message = delegate.createTextMessage(response);
        
        String correlationID = jmsRequestMsg.getJMSCorrelationID();
        if(correlationID == null) correlationID = jmsRequestMsg.getJMSMessageID();
        message.setJMSCorrelationID(correlationID);
		
        delegate.sendMessage(jmsRequestMsg.getJMSReplyTo(), message);
	}

}