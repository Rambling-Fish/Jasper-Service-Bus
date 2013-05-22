package org.jasper.core.delegate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.jms.DeliveryMode;
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

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.atlas.json.JsonString;
import org.apache.jena.atlas.json.JsonValue;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Command;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Type;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

public class DelegateRequest implements Runnable {

	private QueueConnection queueConnection;
	private DelegateOntology jOntology;
	private Message jmsRequest;


	public DelegateRequest(QueueConnection queueConnection, DelegateOntology jOntology, Message jmsRequest) {
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
	  	  	  // Create a Session
	        	QueueSession queueSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
	        	TextMessage txtMsg = (TextMessage) jmsRequest;
	      	    String body = txtMsg.getText();
	      	    String ruri = getRuri(body);
	      	    JsonArray response = getResponse(ruri,body);
	      	  String responseTxt;
	      	    try{
	      	    	responseTxt=(response == null)?"{}":response.toString();
	      	    }catch (Exception e){
	      	    	responseTxt="{}";
	      	    }
	      	    sendResponse(queueSession, txtMsg,responseTxt);
	      	  queueSession.close();		
	        }
		}catch (Exception e){
			e.printStackTrace();
		}
	}
	
	private void handleJasperAdminMessage(JasperAdminMessage jam) {
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
  			
  			JsonObject reponse = JSON.parse(getResponseFromJTA(jta, q,valuePair));
  			JsonValue r = reponse.get(provides);
  			result.add(r);
      	}
      	return result;
	}

	private String getResponseFromJTA(String jta, String q, Map<String, String> valuePair) throws JMSException {
		QueueSession queueSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
		Queue queue = queueSession.createQueue(q);
		QueueRequestor requestor = new QueueRequestor(queueSession, queue);
		MapMessage msg = queueSession.createMapMessage();
		
	    for (String key : valuePair.keySet()) {
	        msg.setObject(key, valuePair.get(key));
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
	
	public static void main(String[] args) throws Exception{
		 
		BrokerService broker = new BrokerService();
		broker.addConnector("tcp://0.0.0.0:61616");
		broker.start();
		
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost");
        // Create a Connection
        QueueConnection qc = connectionFactory.createQueueConnection();
        qc.start(); 		
		DelegateRequest dr = new DelegateRequest(qc, null, null);
		
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
	    String input = "";
	    while (input != null) {
	        System.out.print("Queue leter : ");
	        input = in.readLine();
	        input = input.toLowerCase();
			Map<String, String> valuePair = new HashMap<String, String>();
			valuePair.put("p1", "v1");
			String response = dr.getResponseFromJTA("jta", "jms.jasper.queue."+ input +".queue", valuePair);
			System.out.println(response);
	    }
		
	}
}