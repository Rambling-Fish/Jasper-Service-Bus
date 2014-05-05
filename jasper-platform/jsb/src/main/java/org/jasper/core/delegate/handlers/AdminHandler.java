package org.jasper.core.delegate.handlers;

import java.util.Map;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;

import org.apache.activemq.command.ActiveMQTextMessage;
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
	private Map<String, Object> locks;
	private Map<String, Message> responses;
	private JasperAdminMessage jam;
	
	private static Logger logger = Logger.getLogger(AdminHandler.class.getName());

	public AdminHandler(Delegate delegate, Message jmsRequest) {
		this.delegate   = delegate;
		this.jOntology  = delegate.getJOntology();
		this.jmsRequest = jmsRequest;
		this.locks      = delegate.getLocksMap();
		this.responses  = delegate.getResponsesMap();	
	}

	public void run() {
		try{
			jam = (JasperAdminMessage) ((ObjectMessage)jmsRequest).getObject();
			if(jam == null){
				logger.warn("AdminHandler did not receive a JasperAdminMessage, ignoring message : " + jmsRequest);
				return;
			}
			
			Command cmd = jam.getCommand();
			switch (cmd) {
				case jta_disconnect:
					handleDisconnect();
					break;
				case jta_connect:
					handleConnect();
					break;
				case get_ontology:
					handleGetOntology();
					break;
				default:
					logger.error("Invalid Jasper Admin Message received - ignoring " + jam.getCommand());
					break;
			}
		}catch (Exception e){
			logger.error("Exception caught in handler " + e);
		}
	}
	
	private void handleGetOntology() throws JMSException {
		String[] serilaizedModels = jOntology.getSerializedModels();
		Message msg = delegate.createObjectMessage(serilaizedModels);
        msg.setJMSCorrelationID(jmsRequest.getJMSCorrelationID());
	    delegate.sendMessage(jmsRequest.getJMSReplyTo(), msg);		
	}

	private void handleDisconnect(){
		if(jam.getDetails()[0] !=null && jam.getDetails()[0] instanceof String && ((String) jam.getDetails()[0]).length() > 0){
			jOntology.remove((String)jam.getDetails()[0]);
			if(logger.isDebugEnabled()) logger.debug("received disconnect for dta : " + (String)jam.getDetails()[0]);
		}else{
			logger.warn("received invalid disconnect command, details[0] != string, unknown disconnection request");
		}
	}
	
	private void handleConnect() throws JMSException {
		String dtaName = null;
		
		if(jam.getDetails()[0] !=null && jam.getDetails()[0] instanceof String && ((String) jam.getDetails()[0]).length() > 0){
			dtaName = (String)jam.getDetails()[0];
			if(logger.isDebugEnabled()) logger.debug("received connect for dta : " + (String)jam.getDetails()[0]);
		}else{
			logger.warn("received invalid connect command, details[0] != string, unknown connection request, ignoring");
			return;
		}
		
		String dtaAdminQueue = null;
		
		if(jam.getDetails()[1] !=null && jam.getDetails()[1] instanceof String && ((String) jam.getDetails()[1]).length() > 0){
			dtaAdminQueue = (String)jam.getDetails()[1];
		}else{
			logger.warn("received invalid connect command, details[1] != string, unknown connection request, ignoring");
			return;
		}
		
		if(logger.isDebugEnabled()) logger.debug("received connect for dta : " + dtaName + " on adminQ : " + dtaAdminQueue);

		
		
		Message msg = delegate.createObjectMessage(new JasperAdminMessage(Type.ontologyManagement,Command.get_ontology));
        String correlationID = UUID.randomUUID().toString();
        msg.setJMSCorrelationID(correlationID);
        
        Message response;
        Object lock = new Object();
		synchronized (lock) {
			locks.put(correlationID, lock);
		    delegate.sendMessage(dtaAdminQueue, msg);
			if(logger.isDebugEnabled()) logger.debug("sent get_ontology command to : " + dtaAdminQueue);

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
			if(logger.isDebugEnabled()) logger.debug("received response : " + response);
		}

        String triples = null;
        if(response instanceof ActiveMQTextMessage && ((ActiveMQTextMessage)response).getText() instanceof String){
                triples = (String) ((ActiveMQTextMessage)response).getText();
        }else{
                logger.warn("Ignoring invalid response, expected ObjectMessage carrying String but instead received " + response);
                return;
        }

        if(triples != null){
                jOntology.add(dtaName, triples);
        }else{
                logger.warn("triples null, ignoring add request for dta : " + dtaName);
        }
	}
}