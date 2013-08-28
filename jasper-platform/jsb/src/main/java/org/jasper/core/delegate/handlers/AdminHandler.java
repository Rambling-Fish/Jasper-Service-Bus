package org.jasper.core.delegate.handlers;

import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Message;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.log4j.Logger;
import org.jasper.core.delegate.Delegate;
import org.jasper.core.delegate.DelegateOntology;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Command;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Type;

public class AdminHandler implements Runnable {

	private Delegate delegate;
	private DelegateOntology jOntology;
	private Message jmsRequest;
	private JasperAdminMessage jam;
//	private Map<String, Object> locks;
//	private Map<String, Message> responses;
	
	private static Logger logger = Logger.getLogger(AdminHandler.class.getName());



	public AdminHandler(Delegate delegate, DelegateOntology jOntology, Message jmsRequest, JasperAdminMessage jam, Map<String,Object> locks, Map<String,Message> responses) {
		this.delegate   = delegate;
		this.jOntology  = jOntology;
		this.jmsRequest = jmsRequest;
		this.jam        = jam;
//		this.locks      = locks;
//		this.responses  = responses;
		
	}

	public void run() {
		try{
			handleJasperAdminMessage(jam);
		}catch (Exception e){
			logger.error("Exception caught in handler " + e);
		}
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
     
        delegate.sendMessage(msg.getJMSReplyTo(),message);

	}

	private void handleJasperAdminMessage(JasperAdminMessage jam) throws Exception {
		// TODO change to switch statement
		Command cmd = jam.getCommand();
		switch (cmd) {
		case jta_disconnect:  handleDeleteRequest(jam);
        break;
		case jta_connect: handleConnectRequest(jam);
		break;
		case get_ontology: handlePublishResponse(jam);
		break;
		default: logger.error("Invalid Jasper Admin Message received - ignoring " + jam.getCommand());
		break;
		}
		
	}
	
	private void handlePublishResponse(JasperAdminMessage jam){
		Map<String, String[]> map = new HashMap<String, String[]>();
		map = jam.getMap();

		if(map != null) {
			if(isValidMap(map)){
				for(String key:map.keySet()){
					String[] row = map.get(key);
					try {
						row[0] = URIUtil.decode(row[0]);
						row[1] = URIUtil.decode(row[1]);
						row[2] = URIUtil.decode(row[2]);
						row[3] = URIUtil.decode(row[3]);
					} catch (URIException e) {
						logger.error("Exception when decoding incoming notify request, ignoring " ,e);
						return;
						
					}
					String jtaName = row[0];
					String[] statement = new String[]{row[1],row[2],row[3]};
					jOntology.add(jtaName, statement);
				}
			}
			else {
				try{
					processInvalidRequest(jmsRequest, "Invalid ontology statement from JTA");
				}catch (Exception e){
					logger.error("Error while process ontology statements");
				}
			}
		}
		else{
			try{
				processInvalidRequest(jmsRequest, "Notify request contains no data");
			}catch (Exception e){
				logger.error("Exception while processing notify request " + e);
			}
		}
	}
	
	private void handleDeleteRequest(JasperAdminMessage jam){
		if(jam.getDetails()[0] !=null && jam.getDetails()[0].length() > 0){
			jOntology.remove(jam.getDetails()[0]);
		}
		else{
			try{
				this.processInvalidRequest(jmsRequest, "Delete request contains no data");
			}catch (Exception e){
				logger.error("Exception while processing delete request " + e);
			}
		}
	}

	// Send message to JTA to republish its ontology if it has one after JSB
	// connection re-established
	private void handleConnectRequest(JasperAdminMessage msg) {
		String jtaQueueName = msg.getDetails()[0];
		JasperAdminMessage jam = new JasperAdminMessage(Type.ontologyManagement, Command.get_ontology, "*",  "*", jtaQueueName);
		try{
			sendResponse(jmsRequest, jam);
		}catch(JMSException e) {
			logger.error("Exception sending notify message to delegate: " + e);
		}
		
		String[][] arrayOfArrays = {{"","",""},{"","",""}};
		for(String[] singleArray:arrayOfArrays){
			if(singleArray.length == 3) logger.error("BAD");
			System.out.println(singleArray[0]);
			System.out.println(singleArray[1]);
			System.out.println(singleArray[2]);
		}
		
	}
	
	private boolean isValidMap(Map<String, String[]> map) {
		for(String key:map.keySet()){
			String[] row = map.get(key);
			if(row.length != 4){
				return false;
			}
		}
		return true;
	}

	private void sendResponse(Message jmsRequestMsg,JasperAdminMessage jam) throws JMSException {
        Message message = delegate.createObjectMessage(jam);
        String correlationID = jmsRequestMsg.getJMSCorrelationID();
        if(correlationID == null) correlationID = jmsRequestMsg.getJMSMessageID();
        message.setJMSCorrelationID(correlationID);
		
//        delegate.sendMessage(jmsRequestMsg.getJMSDestination(), message);
        delegate.sendMessage(jam.getDetails()[0], message);
	}
	
}