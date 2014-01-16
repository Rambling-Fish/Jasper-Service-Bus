package org.jasper.core.delegate.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import javax.jms.Message;
import javax.jms.TextMessage;

import org.apache.log4j.Logger;
import org.jasper.core.UDE;
import org.jasper.core.constants.JasperConstants;
import org.jasper.core.delegate.Delegate;
import org.jasper.core.notification.triggers.Trigger;
import org.jasper.core.notification.triggers.TriggerFactory;
import org.jasper.core.persistence.PersistedObject;
import org.json.JSONException;
import org.json.JSONObject;

public class DataHandler implements Runnable {

	private Delegate delegate;
	private Message jmsRequest;
	private String notification;
	private String output;
	private String dtaParms;
	private String ruri;
	private String method;
	private String version;
	private String contentType;
	private int expires;
	private int pollPeriod;
	private List<Trigger> triggerList;
	private static final int MILLISECONDS = 1000;
	private PersistedObject statefulData;
	private Map<String,PersistedObject> sharedData;
	private String key;
	private BlockingQueue<PersistedObject> workQueue;
	private UDE ude;
	private String errorTxt;
	
	private static Logger logger = Logger.getLogger(DataHandler.class.getName());



	public DataHandler(UDE ude, Delegate delegate, Message jmsRequest) {
		this.ude = ude;
		this.delegate = delegate;
		this.jmsRequest = jmsRequest;
	}

	public void run() {
		try{
			sharedData = ude.getCachingSys().getMap("sharedData");
			workQueue  = ude.getCachingSys().getQueue("tasks");
			processRequest( (TextMessage) jmsRequest);
		}catch (Exception e){
			logger.error("Exception caught in handler " + e);
		}
	}
	
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
        removeSharedData(key);
	}

	private void processRequest(TextMessage txtMsg) throws Exception {
  	    String request = txtMsg.getText();
  	    boolean requestOK = false;
  	    key = txtMsg.getJMSCorrelationID();   

  	    if(request == null || request.length() == 0){
  	    	processInvalidRequest(txtMsg, JasperConstants.responseCodes.BADREQUEST, "Invalid request received - request is null or empty string");
  	    	return;
  	    }
  	    
//  	    JSONObject myOBJ = zbuildObjectForTextONLY(); // TODO cleanup - delete this line when Abe delivers
  	    
  	    requestOK =  parseJasperRequest(request); // TODO cleanup - uncomment when Abe delivers and delete next line
//  	    requestOK = parseJasperRequest(myOBJ.toString());
  	    if (!requestOK){
  	    	processInvalidRequest(txtMsg, JasperConstants.responseCodes.BADREQUEST, errorTxt);
  	    	return;
  	    }
  	    
  	    if(ruri == null){
  	    	processInvalidRequest(txtMsg, JasperConstants.responseCodes.BADREQUEST, "Invalid request received - request does not contain a URI");
  	    	return;
  	    }
  	    
  	    // create object that contains stateful data
  	    statefulData = new PersistedObject(key, txtMsg.getJMSCorrelationID(), request, ruri, dtaParms,
  			  txtMsg.getJMSReplyTo(), false, ude.getUdeDeploymentAndInstance(), output, version, contentType);
  	   
  	    if(triggerList != null){
  	    	statefulData.setTriggers(triggerList);
  	    	statefulData.setNotification(notification);
  	    	statefulData.setIsNotificationRequest(true);
  	    }

  	    sharedData.put(key, statefulData);
  	    workQueue.offer(statefulData);
    	
	}
	
	// TODO cleanup - remove when Abe delivers
	private JSONObject zbuildObjectForTextONLY(){
		String str1 = "{\"method\":\"GET\",\"version\":\"1.0\",\"ruri\":\"http://coralcea.ca/jasper/hrData\"}";
//		String str1 = "{\"method\":\"GET\",\"version\":\"1.0\",\"ruri\":\"http://coralcea.ca/jasper/bpData\"}";
		String str2 = "{\"compareint\":\"(http://coralcea.ca/jasper/environmentalSensor/roomTemperature,gt,15)\"}";
  	    JSONObject myOBJ = new JSONObject(str1);
  	    Map<String,String> m1 = new HashMap<String,String>();
  	    m1.put("http://coralcea.ca/jasper/hrSID", "1");
//  	    m1.put("http://coralcea.ca/jasper/bpSID", "bp1");
  	    JSONObject parms = new JSONObject(m1);
  	    m1.clear();
  	    m1.put("expires" , "15");
  	    m1.put("poll-period", "1");
  	    m1.put("content-type", "application/json");
  	    JSONObject headers = new JSONObject(m1);

  	    myOBJ.put(JasperConstants.PARAMETERS_LABEL, parms);
  	    myOBJ.put(JasperConstants.HEADERS_LABEL, headers);
//  	    myOBJ.put(JasperConstants.RULES, new JSONObject(str2));
  	    
  	    return myOBJ;
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
			trigger = factory.createTrigger(functions[i], expires, pollPeriod, triggerParms);
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
	
	private boolean parseJasperRequest(String req) {
		boolean validMsg = false;
		expires = -1;
		pollPeriod = -1;
		JSONObject parms = new JSONObject();
		JSONObject headers = new JSONObject();
		StringBuilder sb = new StringBuilder();
		
		try {
			JSONObject jsonObj = new JSONObject(req);
			// parse out mandatory parameters
			ruri = jsonObj.getString(JasperConstants.REQUEST_URI_LABEL);
			version = jsonObj.getString(JasperConstants.VERSION_LABEL);
			method = jsonObj.getString(JasperConstants.METHOD_LABEL);
			
			if(ruri != null && version != null && method != null) {
				validMsg = true;
			}		

			if((!method.equalsIgnoreCase("get")) && (!method.equalsIgnoreCase("post"))){
				validMsg = false;
				errorTxt = ("Invalid request type: " + method);
			}
			
			if(jsonObj.has(JasperConstants.PARAMETERS_LABEL)) {
				parms = (JSONObject)jsonObj.getJSONObject(JasperConstants.PARAMETERS_LABEL);
				int len = parms.length();
				for(Object o:parms.keySet()){
					sb.append(o.toString());
					sb.append("=");
					sb.append(parms.get(o.toString()));
					if(len > 1) {
						sb.append("&");
						len--;
					}
				}
			
				dtaParms = sb.toString();
			}
			
			if(jsonObj.has(JasperConstants.HEADERS_LABEL)){
				headers = (JSONObject)jsonObj.getJSONObject(JasperConstants.HEADERS_LABEL);
				for(Object o:headers.keySet()){
					switch (o.toString().toLowerCase()) {
					case JasperConstants.POLL_PERIOD_LABEL :
						try{
							pollPeriod = headers.getInt(o.toString());
							pollPeriod = (pollPeriod * MILLISECONDS); // convert to milliseconds
						}catch(JSONException ex){
							pollPeriod = delegate.maxPollingInterval;
						}
						break;
					case JasperConstants.EXPIRES_LABEL :
						try{
							expires = headers.getInt(o.toString());
							expires = (expires * MILLISECONDS); // convert to milliseconds
						} catch(JSONException ex){
							expires = delegate.maxExpiry;
						}
						break;
					case JasperConstants.CONTENT_TYPE_LABEL :
						contentType = headers.getString(o.toString());
						break;
					}
				}
			}
			
			if(jsonObj.has(JasperConstants.RULE_LABEL)){
				notification = jsonObj.getString(JasperConstants.RULE_LABEL);
				triggerList = new ArrayList<Trigger>();
			}
			
			if(notification != null){
				if(expires == -1) expires = delegate.maxExpiry; // if not supplied set to max
				if(expires > delegate.maxExpiry) expires = delegate.maxExpiry;
				if(pollPeriod == -1) pollPeriod = delegate.maxPollingInterval; // if not supplied set to max
				if(pollPeriod < delegate.minPollingInterval) pollPeriod = delegate.minPollingInterval;
				if(pollPeriod > delegate.maxPollingInterval) pollPeriod = delegate.maxPollingInterval;
				if(pollPeriod > expires) pollPeriod = (int) expires;
				if(output == null) output =  delegate.defaultOutput;
				
				parseTrigger(notification);
			}
			
		} catch (JSONException e) {
			logger.error("Exception caught while creating JSONObject " + e);
			validMsg = false;
			errorTxt = "Invalid / Malformed JSON object received";
		}
		
		return validMsg;
		
	}
	
}
