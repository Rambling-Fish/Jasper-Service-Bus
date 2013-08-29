package org.jasper.core.delegate.handlers;

import java.util.Map;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;

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

	public AdminHandler(Delegate delegate, DelegateOntology jOntology, Message jmsRequest, Map<String,Object> locks, Map<String,Message> responses) {
		this.delegate   = delegate;
		this.jOntology  = jOntology;
		this.jmsRequest = jmsRequest;
		this.locks      = locks;
		this.responses  = responses;	
	}

	public void run() {
		try{
			handleAdminMessage();
		}catch (Exception e){
			logger.error("Exception caught in handler " + e);
		}
	}
	
	private void handleAdminMessage() throws Exception {
		if(jmsRequest instanceof ObjectMessage && ((ObjectMessage)jmsRequest).getObject() instanceof JasperAdminMessage){
			jam = (JasperAdminMessage) ((ObjectMessage)jmsRequest).getObject();
		}
		
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
			default:
				logger.error("Invalid Jasper Admin Message received - ignoring " + jam.getCommand());
				break;
		}
		
	}
	
	private void handleDisconnect(){
		if(jam.getDetails()[0] !=null && jam.getDetails()[0] instanceof String && ((String) jam.getDetails()[0]).length() > 0){
			jOntology.remove((String)jam.getDetails()[0]);
			if(logger.isDebugEnabled()) logger.debug("received disconnect for jta : " + (String)jam.getDetails()[0]);
		}else{
			logger.warn("received invalid disconnect command, details[0] != string, unknown disconnection request");
		}
	}
	
	private void handleConnect() throws JMSException {
		String jtaId = null;
		
		if(jam.getDetails()[0] !=null && jam.getDetails()[0] instanceof String && ((String) jam.getDetails()[0]).length() > 0){
			jtaId = (String)jam.getDetails()[0];
			if(logger.isDebugEnabled()) logger.debug("received connect for jta : " + (String)jam.getDetails()[0]);
		}else{
			logger.warn("received invalid connect command, details[0] != string, unknown connection request, ignoring");
			return;
		}
		
		String jtaAdminQueue = "jms." + jtaId.replace(":", ".") + ".admin.queue";
		
		Message msg = delegate.createObjectMessage(new JasperAdminMessage(Type.ontologyManagement,Command.get_ontology));
        String correlationID = UUID.randomUUID().toString();
        msg.setJMSCorrelationID(correlationID);
        
        Message response;
        Object lock = new Object();
		synchronized (lock) {
			locks.put(correlationID, lock);
		    delegate.sendMessage(jtaAdminQueue, msg);
			if(logger.isDebugEnabled()) logger.debug("sent get_ontology command to : " + jtaAdminQueue);

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

		String[][] triples = null;
		if(response instanceof ObjectMessage && ((ObjectMessage)response).getObject() instanceof String[][]){
			triples = (String[][]) ((ObjectMessage)response).getObject();
		}else{
			logger.warn("Ignoring invalid response, expected ObjectMessage carrying String[][] but instead received " + response);
			return;
		}
		
		for(String[] triple:triples){
			if(triple.length != 3){
				logger.error("String[] received that is not a triple, ignoring");
				continue;
			}
			jOntology.add(jtaId, triple);
			if(logger.isDebugEnabled()) logger.debug("jta : " + jtaId + " - adding triple : " + triple[0] + " " + triple[1] + " " + triple[2]);
		}
		
	}
}