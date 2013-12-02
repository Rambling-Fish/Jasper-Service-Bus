package org.jasper.core.delegate.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.log4j.Logger;
import org.jasper.core.JECore;
import org.jasper.core.delegate.Delegate;
import org.jasper.core.delegate.DelegateOntology;
import org.jasper.core.notification.triggers.Trigger;
import org.jasper.core.notification.triggers.TriggerFactory;
import org.jasper.core.persistence.PersistedObject;
import org.jasper.core.persistence.PersistenceFacade;

public class DataHandler implements Runnable {

	private Delegate delegate;
	private Message jmsRequest;
	private String notification;
	private String output;
	private String jtaParms;
	private int expiry;
	private int polling;
	private List<Trigger> triggerList;
	private static final int MILLISECONDS = 1000;
	private PersistedObject statefulData;
	private Map<String,PersistedObject> sharedData;
	private String key;
	private BlockingQueue<PersistedObject> workQueue;
	private Session delegateSession;
	
	private static Logger logger = Logger.getLogger(DataHandler.class.getName());



	public DataHandler(Delegate delegate, Message jmsRequest, Session session) {
		this.delegate = delegate;
		this.jmsRequest = jmsRequest;
		this.delegateSession = session;
	}

	public void run() {
		try{
			sharedData = PersistenceFacade.getInstance().getMap("sharedData");
			workQueue  = PersistenceFacade.getInstance().getQueue("tasks");
			processRequest( (TextMessage) jmsRequest);
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
        removeSharedData(key);
	}
	
	private void processRequest(TextMessage txtMsg) throws Exception {
  	    String request = txtMsg.getText();
  	    key = txtMsg.getJMSCorrelationID();

  	    if(request == null || request.length() == 0){
  	    	processInvalidRequest(txtMsg, "Invalid request received, request is null or empty string");
  	    	return;
  	    }
  	    
  	    String ruri = getRuri(request);
  	    if(ruri == null){
  	    	processInvalidRequest(txtMsg, "Invalid request received, request does not contain a URI");
  	    	return;
  	    }
  	    
  	    parseRequest(request);
  	    
  	    // create object that contains stateful data
  	    statefulData = new PersistedObject(key, txtMsg.getJMSCorrelationID(), request, ruri, txtMsg.getJMSReplyTo(),
  	    		false, JECore.getInstance().getUdeDeploymentAndInstance());
  	    
  	    logger.debug("*****\nstatefulData created on " + statefulData.getUDEInstance());
  	    
  	    if(triggerList != null){
  	    	statefulData.setTriggers(triggerList);
  	    	statefulData.setNotification(notification);
  	    	statefulData.setIsNotificationRequest(true);
  	    	statefulData.setJtaParms(jtaParms);
  	    }

  	    sharedData.put(key, statefulData);
  	    workQueue.offer(statefulData);
  	    delegateSession.commit();
    	
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
	
	/*
	 * Parses out the different trigger types from the inbound notification string
	 * Will create and link a trigger for each function in the inbound request
	 */
	private void parseTrigger(String notification){
		String[] triggers = notification.split("&");
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
		for(int i=0; i<functions.length;i++){
			triggerParms = parms[i].split(",");
			trigger = factory.createTrigger(functions[i], expiry, polling, triggerParms);
			if(trigger != null){
				trigger.setNotificationExpiry();
				triggerList.add(trigger);
			}
			else{
				logger.error("Invalid notification request received - cannot create trigger: " + triggerParms.toString());
			}
		}		
	}
	
	private void removeSharedData(String key){
		sharedData.remove(key);
	}
	
	private void parseRequest(String text) {
		String[] result = null;
		notification = null;
		expiry = -1;
		polling = -1;
		
		result = text.split("\\?");
		
		if(result.length < 1) return;
		
		try {
			for(int i=1;i<result.length;i++){
				result[i] = URIUtil.decode(result[i]);
				if(result[i].toLowerCase().contains("trigger=")){
					notification = result[i].replaceFirst("trigger=", "");
					triggerList = new ArrayList<Trigger>();
				}
				else if(result[i].toLowerCase().contains("output=")){
					output = result[i].toLowerCase().replaceFirst("output=", "");
				}
				else if(result[i].toLowerCase().contains("expiry=")){
					String tmp = result[i].toLowerCase().replaceFirst("expiry=",  "");
					try{
						expiry = Integer.parseInt(tmp);
						expiry = (expiry * MILLISECONDS); // convert from seconds to milliseconds
					}catch (NumberFormatException ex) {
						// give one shot at getting data on error since we do
						// know what to set expiry to
						expiry = 0;
					}
				}
				else if(result[i].toLowerCase().contains("polling=")){
					String tmpPoll = result[i].toLowerCase().replaceFirst("polling=",  "");
					try{
						polling = Integer.parseInt(tmpPoll);
						polling = (polling * MILLISECONDS); // convert from seconds to milliseconds
						
					}catch (NumberFormatException ex) {
		    			polling = delegate.maxPollingInterval;
					}
				}
				else{
					jtaParms = result[i];
				}
			}
			
			if(notification != null){
				if(expiry == -1) expiry = delegate.maxExpiry; // if not supplied set to max
				if(expiry > delegate.maxExpiry) expiry = delegate.maxExpiry;
				if(polling == -1) polling = delegate.maxPollingInterval; // if not supplied set to max
				if(polling < delegate.minPollingInterval) polling = delegate.minPollingInterval;
				if(polling > delegate.maxPollingInterval) polling = delegate.maxPollingInterval;
				if(polling > expiry) polling = (int) expiry;
				if(output == null) output =  delegate.defaultOutput;
				
				parseTrigger(notification);
			}
			
		} catch (URIException e) {
			logger.error("Exception when decoding encoded notification request " ,e);
		}
	}

}