package org.jasper.core.delegate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueRequestor;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.atlas.json.JsonString;
import org.apache.jena.atlas.json.JsonValue;
import org.apache.log4j.Logger;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Command;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Type;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

public class DelegateRequest implements Runnable {

	private Delegate delegate;
	private QueueConnection queueConnection;
	private DelegateOntology jOntology;
	private Message jmsRequest;
	
	private static Logger logger = Logger.getLogger("org.jasper");



	public DelegateRequest(Delegate delegate, QueueConnection queueConnection, DelegateOntology jOntology, Message jmsRequest) {
		this.delegate = delegate;
		this.queueConnection = queueConnection;
		this.jOntology = jOntology;
		this.jmsRequest = jmsRequest;
		
	}

	@Override
	public void run() {
		try{
			if (jmsRequest instanceof ObjectMessage) {
				ObjectMessage objMessage = (ObjectMessage) jmsRequest;
	            Object obj = objMessage.getObject();
	            if(obj instanceof JasperAdminMessage) {
	          	  handleJasperAdminMessage((JasperAdminMessage)obj);
	        	}
	        }else if(jmsRequest instanceof TextMessage){
	        	if(isOntologyRequest( (TextMessage) jmsRequest)){
	        		handleOntologyRequest( (TextMessage) jmsRequest);
	        	}else{
	        		handleSynchronousRequest( (TextMessage) jmsRequest);
	        	}	
	        }else{
	        	logger.warn("JMS Message neither ObjectMessage nor TextMessage, ignoring request : " + jmsRequest);
	        }
		}catch (Exception e){
			e.printStackTrace();
		}
	}
	
	private boolean isOntologyRequest(TextMessage txtMsg) {
			//TODO currently don't support ontology, only for demo.
			return false;
	}
	
	/*
	 * Whenever an error is detected in incoming request rather than letting
	 * the client timeout, the core responds with an empty JSON message.
	 * Currently this occurs if incoming URI cannot be mapped to a JTA or the
	 * incoming correlationID is not unique
	 */
	private void processInvalidRequest(TextMessage msg) throws Exception {
		// Create a Session
        Session jClientSession = queueConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
  	  	Destination jClientQueueDestination = msg.getJMSReplyTo();
       
        // Create a MessageProducer from the Session to the Queue
        MessageProducer producer = jClientSession.createProducer(jClientQueueDestination);
        producer.setDeliveryMode(DeliveryMode.PERSISTENT);
        producer.setTimeToLive(30000);

        // For now we always send back empty JSON for all error scenarios
        Message message = jClientSession.createTextMessage("{}");
        if(msg.getJMSCorrelationID() == null){
            message.setJMSCorrelationID(msg.getJMSMessageID());
  	  	}else{
            message.setJMSCorrelationID(msg.getJMSCorrelationID());
  	  	}
        
        producer.send(message);

        // Clean up
        jClientSession.close();
	}
	
	private void handleSynchronousRequest(TextMessage txtMsg) throws Exception {

		String uri = txtMsg.getText();
		Map<String, List<String>> jtaUriMap = delegate.getJtaUriMap();
		
	  	if(!jtaUriMap.containsKey(uri)){
	      	  logger.error("URI " + uri + " in incoming request not found");
	      	  processInvalidRequest(txtMsg);
	  	  }else{
	      	  String correlationID = txtMsg.getJMSCorrelationID();
        	  if(correlationID == null){
        		  if(logger.isInfoEnabled()){
        			  logger.info("jmsCorrelationID is null, setting jmsCorrelationID to jmsMessageID : " + txtMsg.getJMSMessageID());
        		  }
        		  correlationID = txtMsg.getJMSMessageID();
        	  }
	      	  if(logger.isInfoEnabled()){
	      		  logger.info("request getJMSCorrelationID = " + txtMsg.getJMSCorrelationID());
	      		  logger.info("request getJMSReplyTo       = " + txtMsg.getJMSReplyTo());
	      	  }
  
	      	  String[] req = new String[]{uri};
	      	  
              // TODO need to get multiple responses and coordinate replies
              // for when multiple queues per URI
	      	  String response = getResponseFromQueue(jtaUriMap.get(uri).get(0), req);
	
	      	  Session jClientSession = queueConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	      	  Destination jClientQueueDestination = txtMsg.getJMSReplyTo();
	       
	      	  // Create a MessageProducer from the Session to the Queue
	      	  MessageProducer producer = jClientSession.createProducer(jClientQueueDestination);
	      	  producer.setDeliveryMode(DeliveryMode.PERSISTENT);
	      	  producer.setTimeToLive(30000);
	      	  Message message = jClientSession.createTextMessage(response);
	      	  message.setJMSCorrelationID(correlationID);
			  producer.send(message);

			  // Clean up
			  jClientSession.close();
	  	  }
	
	}
	
	private void handleOntologyRequest(TextMessage txtMsg) throws Exception {
    	// Create a Session
    	QueueSession queueSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
  	    String request = txtMsg.getText();
  	    String ruri = getRuri(request);
  	    JsonArray response = getResponse(ruri,request);
  	    String responseTxt;
  	    try{
  	    	responseTxt=(response == null)?"{}":response.toString();
  	    }catch (Exception e){
  	    	responseTxt="{}";
  	    }
  	    sendResponse(queueSession, txtMsg,responseTxt);
  	    queueSession.close();			
	}

	private void handleJasperAdminMessage(JasperAdminMessage jam) throws Exception {
		if(isUriUpdate(jam)){
			handleJasperUriUpdateAdminMessage(jam);
		}else if(isOntologyUpdate(jam)){
			handleJasperOntologyAdminMessage(jam);
		}else{
			logger.warn("JasperAdminMessage neither Uri update nor Ontology update, ignorning message : " + jam);
		}
	}

	private void handleJasperUriUpdateAdminMessage(JasperAdminMessage jam) throws Exception {
		if(jam.getType() == Type.jtaDataManagement) {
  			if(jam.getCommand() == Command.notify) {
  				updateURIMaps(jam);
  			}
  			else if(jam.getCommand() == Command.publish) {
  				notifyJTA(jam);
  			}
  			else if(jam.getCommand() == Command.delete) {
  				cleanURIMaps(jam.getSrc());
  			}
  		}
	}

	private synchronized void updateURIMaps(JasperAdminMessage jam) throws Exception{
		List<String> uriQueueList = new ArrayList<String>();
		List<String> jtaQueueList = new ArrayList<String>();
		String uri = jam.getDetails()[0];
		String jtaName = jam.getDst();
		
		Map<String, List<String>> jtaUriMap = delegate.getJtaUriMap();
		Map<String, List<String>> jtaQueueMap = delegate.getJtaQueueMap();
		
		// Add URI if it does not exist in map
		if(!jtaUriMap.containsKey(uri)) {
			uriQueueList.add(jam.getSrc());
		}
		else {
			// URI (key) exists we want to add to the queue list
			uriQueueList.clear();
			uriQueueList = jtaUriMap.get(uri);
			uriQueueList.add(jam.getSrc());
		}
		
		jtaUriMap.put(uri, uriQueueList);
		
		// If JTAName exists then add to existing queue list
		if(jtaQueueMap.containsKey(jtaName)) {
			jtaQueueList.clear();
			jtaQueueList = jtaQueueMap.get(jtaName);
			jtaQueueList.add(jam.getSrc());
		}
		else {
			jtaQueueList.add(jam.getSrc());
		}
		
		jtaQueueMap.put(jtaName, jtaQueueList);
		
		if(logger.isInfoEnabled()) {
			logger.info("Received " + jam.getCommand() + " from " + jam.getSrc() + " with details: " + uri);
  		}
	}

	private synchronized void cleanURIMaps(String username) throws Exception {
		
		Map<String, List<String>> jtaUriMap = delegate.getJtaUriMap();
		Map<String, List<String>> jtaQueueMap = delegate.getJtaQueueMap();

		Iterator<String> uriIT = jtaUriMap.keySet().iterator();
		List<String> queueList = new ArrayList<String>();
		
		queueList = jtaQueueMap.get(username);
		
		if(queueList != null) {
			while(uriIT.hasNext()) {
				String key= (String)uriIT.next(); 
				List<String> values = jtaUriMap.get(key);
				for(int i = 0; i < queueList.size(); i++) {
					String tmp = queueList.get(i);
					if(values.contains(tmp)) {
						values.remove(tmp);
					}
				}
			
				if(values.isEmpty()) {
					jtaUriMap.remove(key);
					if(logger.isInfoEnabled()){
						logger.info("Removed URI " + key + " for JTA " + username);
					}
				}
			}
		
			jtaQueueMap.remove(username);
		}
	}

	// Send message to JTA to republish its URI if it has one after
	// JSB connection re-established (for single JSB deployment scenario only)
	private void notifyJTA(JasperAdminMessage msg) {
		String jtaQueueName = msg.getDetails()[0];
		
		try {

			// Create a Session
			Session session = queueConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);

			// Create the destination (Topic or Queue)
			Destination destination = session.createQueue(jtaQueueName);

			// Create a MessageProducer from the Session to the Topic or Queue
			MessageProducer producer = session.createProducer(destination);
			producer.setDeliveryMode(DeliveryMode.PERSISTENT);
			producer.setTimeToLive(30000);

			JasperAdminMessage jam = new JasperAdminMessage(Type.jtaDataManagement, Command.notify, "*",  "*", jtaQueueName);

			Message message = session.createObjectMessage(jam);
			producer.send(message);

			// Clean up
			session.close();
		}
		catch (Exception e) {
			logger.error("Exception caught while notifying peers: ", e);
		}
	}

	private boolean isUriUpdate(JasperAdminMessage jam) {
		// TODO Auto-generated method stub
		return true;
	}

	private boolean isOntologyUpdate(JasperAdminMessage jam) {
		// TODO Auto-generated method stub
		return false;
	}

	private void handleJasperOntologyAdminMessage(JasperAdminMessage jam) {
		String uri = jam.getDetails()[0];
		if(uri == null) return;
		
		Model model = jOntology.getModel();;
		Resource jta = model.getResource("http://coralcea.ca/jasper/vocabulary/jta");
		Resource msData    = model.getResource("http://coralcea.ca/jasper/medicalSensor/data");
		Resource bpSID     = model.getResource("http://coralcea.ca/jasper/medicalSensor/bloodPressure/sensorId");
		Resource hrSID     = model.getResource("http://coralcea.ca/jasper/medicalSensor/heartRate/sensorId");
		Property provides = model.getProperty("http://coralcea.ca/jasper/vocabulary/provides");
		Property param = model.getProperty("http://coralcea.ca/jasper/vocabulary/param");
		Property has = model.getProperty("http://coralcea.ca/jasper/vocabulary/has");
		Property is = model.getProperty("http://coralcea.ca/jasper/vocabulary/is");
		Property subClassOf = model.getProperty("http://coralcea.ca/jasper/vocabulary/subClassOf");
		Property queue = model.getProperty("http://coralcea.ca/jasper/vocabulary/queue");
		
		if(jam.getType() == Type.jtaDataManagement && jam.getCommand() == Command.notify){
			if(uri.equals("hrJTA")){
				Resource hrData    = model.createResource("http://coralcea.ca/jasper/medicalSensor/heartRate/data");
				Resource hrDataBpm = model.createResource("http://coralcea.ca/jasper/medicalSensor/heartRate/data/bpm");
				Resource timeStamp = model.getResource("http://coralcea.ca/jasper/timeStamp");
				Resource jtaA      = model.createResource("http://coralcea.ca/jasper/jtaA");
				jtaA.addProperty(is, jta);
				jtaA.addProperty(provides, hrData);
				jtaA.addProperty(param, hrSID);
				jtaA.addLiteral(queue, model.createLiteral(jam.getSrc()));
				hrData.addProperty(subClassOf,msData);
				hrData.addProperty(has, hrDataBpm);
				hrData.addProperty(has, timeStamp);
			}else if(uri.equals("bpJTA")){
				Resource bpData    = model.createResource("http://coralcea.ca/jasper/medicalSensor/bloodPressure/data");
				Resource bpDataDia = model.createResource("http://coralcea.ca/jasper/medicalSensor/bloodPressure/data/diastolic");
				Resource bpDataSys = model.createResource("http://coralcea.ca/jasper/medicalSensor/bloodPressure/data/systolic");
				Resource timeStamp = model.getResource("http://coralcea.ca/jasper/timeStamp");
				Resource jtaB      = model.createResource("http://coralcea.ca/jasper/jtaB");
				jtaB.addProperty(is, jta);
				jtaB.addProperty(provides, bpData);
				jtaB.addProperty(param, bpSID);
				jtaB.addLiteral(queue, model.createLiteral(jam.getSrc()));
				bpData.addProperty(subClassOf,msData);
				bpData.addProperty(has, bpDataDia);
				bpData.addProperty(has, bpDataSys);
				bpData.addProperty(has, timeStamp);		
			}else if(uri.equals("emrJTA")){
				Resource patient   = model.createResource("http://coralcea.ca/jasper/patient");
				Resource patientID = model.createResource("http://coralcea.ca/jasper/patient/id");
				Resource ward      = model.createResource("http://coralcea.ca/jasper/ward");
				Resource bed       = model.createResource("http://coralcea.ca/jasper/bed");
				Resource jtaC      = model.createResource("http://coralcea.ca/jasper/jtaC");
				jtaC.addProperty(is, jta);
				jtaC.addProperty(provides, bed);
				jtaC.addProperty(provides, ward);
				jtaC.addProperty(provides, hrSID);
				jtaC.addProperty(provides, bpSID);
				jtaC.addProperty(param, patientID);
				jtaC.addLiteral(queue, model.createLiteral(jam.getSrc()));
				patient.addProperty(has, ward);
				patient.addProperty(has, patientID);
				patient.addProperty(has, bed);
			}
		}
	}

	private void sendResponse(QueueSession queueSession,TextMessage jmsRequestMsg,String response) throws JMSException {
		MessageProducer producer = queueSession.createProducer(jmsRequestMsg.getJMSReplyTo());
        producer.setDeliveryMode(DeliveryMode.PERSISTENT);
        producer.setTimeToLive(30000);
        
        Message message = queueSession.createTextMessage(response);
        
        String correlationID = jmsRequestMsg.getJMSCorrelationID();
        if(correlationID == null) correlationID = jmsRequestMsg.getJMSMessageID();
        message.setJMSCorrelationID(correlationID);
		
        producer.send(message);
        producer.close();
	}

	private JsonArray getResponse(String ruri, String body) throws JMSException {
		JsonArray result = new JsonArray();
		Map<String,String> params = getParams(body);
        JsonArray queuesAndParams = jOntology.getQandParams(ruri, params);
        if(queuesAndParams == null)return null;
      	for(JsonValue j:queuesAndParams){
      		String jta      = ((JsonString)j.getAsObject().get("jta").getAsString()).value();
  			String q        = ((JsonString)j.getAsObject().get("queue").getAsString()).value();
  			String provides = ((JsonString)j.getAsObject().get("provides").getAsString()).value();
  			JsonObject p    = (JsonObject) j.getAsObject().get("params");
  			Object tmp = null;
  			Map<String,String> valuePair = new HashMap<String, String>();
  			for(String key:p.keys()){
  				tmp = p.get(key);
  				if(tmp instanceof JsonString){
  					valuePair.put(key, ((JsonString)tmp).value());
  				}else if(tmp instanceof JsonArray){
  		  			JsonArray response = getResponse(key, body);
  		  			for(JsonValue index:response){
  	  		  			if(index instanceof JsonObject || index instanceof JsonString){
	  	  		  			JsonValue value = (index instanceof JsonObject)?((JsonObject)index).get(key):index;
	  	  		  			if(value.isString()){
	  	  		  				valuePair.put(key, ((JsonString)value).value());
	  	  		  			}else{
	  	  		  				System.out.println("ERROR --------");
	  	  		  			}
  	  		  			}else{
  	  		  				System.out.println("ERROR --------");
  	  		  			}
  		  			}
  				}
  			}
  			
  			if(valuePair.isEmpty()){
  				//TODO change is empty to is all parms availalbe
  				continue;
  			}
  			
  			JsonObject reponse = JSON.parse(getResponseFromQueue(q,valuePair));
  			JsonValue r = reponse.get(provides);
  			result.add(r);
      	}
      	return result;
	}

	private String getResponseFromQueue(String q, Object obj) throws JMSException {
		QueueSession queueSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
		Queue queue = queueSession.createQueue(q);
		QueueRequestor requestor = new QueueRequestor(queueSession, queue);
		Message msg;
		
		if(obj instanceof Map){
			msg = queueSession.createMapMessage();
			Map valuePair = (Map)obj;
		    for (Object key : valuePair.keySet()) {
		    	if(key instanceof String){
		    		((MapMessage)msg).setObject((String)key, valuePair.get(key));
		    	}else{
		    		logger.warn("Creating MapMessage however key not String, ignoring key and value pair : " + key);
		    	}
		    }
		}else{
			msg = queueSession.createObjectMessage();
			if(obj instanceof Serializable){
				((ObjectMessage)msg).setObject((Serializable)obj);
			}else{
				logger.warn("Creating objectMessage however obj not Serializable, not setting obj : " + obj);
			}
		}
		
        String correlationID = UUID.randomUUID().toString();
        msg.setJMSCorrelationID(correlationID);
	    Message response = requestor.request(msg);	

		String responseString = null;
		if(response != null && response.getJMSCorrelationID().equals(correlationID) && response instanceof TextMessage){
			responseString = ((TextMessage)response).getText();
		}
		return responseString;
	}

	/*
	 * assumes the request will be in the format of ruri?param1=val1
	 * and that the ruri can be be abbreviated. If the abbreviated or
	 * long form ruri is in the uriMapper we return the long
	 * form otherwise we return null
	 */
	private String getRuri(String request) {
		if(request == null)return null;
		String ruri = request.split("\\?")[0];
		if(ruri.isEmpty()) return null;
		if(DelegateFactory.URI_MAPPER.containsValue(ruri)) return ruri;
		return (DelegateFactory.URI_MAPPER.containsKey(ruri))?DelegateFactory.URI_MAPPER.get(ruri):null;
	}
	
	/*
	 * assumes the request will be in the format of ruri?param1=val1&parm2=val2
	 * if there are no params, than we return an empty map, if there's an error,
	 * we return null. All params must be in the abbreviated form in the uriMapper
	 */
	private Map<String, String> getParams(String request) {
		if(request == null)return null;
		String[] splitRequest = request.split("\\?");
		if(splitRequest.length == 1){
			return new HashMap<String, String>();
		}else if(splitRequest.length == 2){
			return getMapFromValuePairString(splitRequest[1]);
		}else{
			return null;
		}		
	}

	private Map<String, String> getMapFromValuePairString(String str) {
		Map<String, String> result = new HashMap<String, String>();
		String[] keyValuePairs = str.split("&");
		String[] keyValue;
		for(String s:keyValuePairs){
			keyValue = s.split("=");
			if(keyValue.length == 2 && DelegateFactory.URI_MAPPER.containsKey(keyValue[0])) result.put(DelegateFactory.URI_MAPPER.get(keyValue[0]), keyValue[1]);
		}
		return result;
	}

}