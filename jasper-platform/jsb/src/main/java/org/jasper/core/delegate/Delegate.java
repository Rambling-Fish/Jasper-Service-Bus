package org.jasper.core.delegate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.log4j.Logger;
import org.jasper.core.JECore;
import org.jasper.core.constants.JasperConstants;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Command;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Type;

public class Delegate implements Runnable, MessageListener {

	private Map<String,List<String>> jtaUriMap;
	private Map<String,List<String>> jtaQueueMap;
	private Map<String, Destination> reqRespMap;
	private Map<String, String> msgIdMap;
	private Connection connection;
	private boolean isShutdown;
	private Session globalSession;
	private Queue globalQueue;
	private MessageConsumer globalDelegateConsumer;
	private Session jtaSession;
	private MessageProducer producer;
	private Destination delegateQ;
	private MessageConsumer responseConsumer;
	
	static Logger logger = Logger.getLogger("org.jasper");
	static private AtomicInteger count = new AtomicInteger(0);
	
	public Delegate(Connection connection, Map<String,List<String>> uriMap, Map<String,List<String>> queueMap) throws JMSException {
		this.connection  = connection;
		this.jtaUriMap   = uriMap;
		this.jtaQueueMap = queueMap;
		this.reqRespMap  = new ConcurrentHashMap<String, Destination>();
		this.msgIdMap    = new ConcurrentHashMap<String, String>();
		this.isShutdown  = false;
		
		globalSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	    globalQueue = globalSession.createQueue(JasperConstants.DELEGATE_GLOBAL_QUEUE);
	    globalDelegateConsumer = globalSession.createConsumer(globalQueue);
	    
        jtaSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        producer = jtaSession.createProducer(null);
        producer.setDeliveryMode(DeliveryMode.PERSISTENT);
        producer.setTimeToLive(30000);
        
        delegateQ = jtaSession.createQueue("jms.delegate." + JECore.getInstance().getBrokerTransportIp() + "." + count.getAndIncrement() + ".queue");
        responseConsumer = jtaSession.createConsumer(delegateQ);
        responseConsumer.setMessageListener(this);
	}
	
	public void shutdown() throws JMSException{
		isShutdown = true;
		producer.close();
		responseConsumer.close();
        jtaSession.close();
        globalDelegateConsumer.close();
	    globalSession.close();
	}
	
	@Override
	public void run(){
		processRequests();
	}
		
	@Override
	public void onMessage(Message msg) {
		try{
			if(logger.isDebugEnabled()){
				logger.debug("Message response received = " + msg);
	  	  	}
		
			if (msg instanceof TextMessage) {
				TextMessage responseMessage = (TextMessage) msg;	
				if(logger.isDebugEnabled()){
					logger.debug("TextMessage response received = " + responseMessage);
				}
	          
				String correlationID = responseMessage.getJMSCorrelationID();
	
				if(!reqRespMap.containsKey(correlationID)) {
					logger.error("correlationID " + correlationID + " for response not found, ignoring");
					return;
				}
	    	  
				// Create a Session
				Destination jClientQueueDestination = reqRespMap.remove(correlationID);
	       
				Message message = jtaSession.createTextMessage(responseMessage.getText());
				if(msgIdMap.containsKey(correlationID)) correlationID = msgIdMap.remove(correlationID);
				message.setJMSCorrelationID(correlationID);
				producer.send(jClientQueueDestination,message);
			}
		}catch (JMSException jmse){
			logger.error("error occured in onMessage",jmse);
		}
	}
	
	/*
	 * Whenever an error is detected in incoming request rather than letting
	 * the client timeout, the core responds with an empty JSON message.
	 * Currently this occurs if incoming URI cannot be mapped to a JTA or the
	 * incoming correlationID is not unique
	 */
	public void processInvalidRequest(TextMessage msg) throws Exception {
		// Create a Session
        Session jClientSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
  	  	Destination jClientQueueDestination = msg.getJMSReplyTo();
       
        // Create a MessageProducer from the Session to the Queue
        MessageProducer producer = jClientSession.createProducer(jClientQueueDestination);
        producer.setDeliveryMode(DeliveryMode.PERSISTENT);
        producer.setTimeToLive(30000);

        // For now we always send back empty JSON for all error scenarios
        Message message = jClientSession.createTextMessage("{}");
        message.setJMSCorrelationID(msg.getJMSMessageID());
        producer.send(message);

        // Clean up
        jClientSession.close();
	}
	
	public void processRequests() {
		  try {

		      // Wait for a message
		      Message jmsRequest;
		      	      
		      do{
		          do{
		          	jmsRequest = globalDelegateConsumer.receive(500);
		          }while(jmsRequest == null && !isShutdown);
		          if(isShutdown) break;
		          if (jmsRequest instanceof ObjectMessage) {
		        	  ObjectMessage objMessage = (ObjectMessage) jmsRequest;
		              Object obj = objMessage.getObject();
		              if(obj instanceof JasperAdminMessage) {
		            	  JasperAdminMessage jam = (JasperAdminMessage)obj;
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
		              else {
		            	  logger.error("Invalid message received: " + jmsRequest.toString());
		              }
		          }else if(jmsRequest instanceof TextMessage){
		        	  TextMessage txtMsg = (TextMessage) jmsRequest;

		        	  // Remove leading and trailing spaces in incoming URI
		        	  String uri = txtMsg.getText().trim();      	  
		        	  
		        	  if(!jtaUriMap.containsKey(uri)){
			        	  logger.error("URI " + uri + " in incoming request not found");
			        	  processInvalidRequest(txtMsg);
		        	  }else{
			        	  String correlationID = txtMsg.getJMSCorrelationID();
		            	  Destination jClientQ = txtMsg.getJMSReplyTo();
			        	  if(logger.isInfoEnabled()){
			        		  logger.info("request getJMSCorrelationID = " + txtMsg.getJMSCorrelationID());
			        		  logger.info("request getJMSReplyTo       = " + txtMsg.getJMSReplyTo());
			        	  }

			        	  if(correlationID == null){
			        		  if(logger.isDebugEnabled()){
			        			  logger.debug("jmsCorrelationID is null, assuming jmsReplyTo queue is unique and therefore using jmsReplyTo queue as correlation id : " + jClientQ.toString());
			        		  }
			        		  correlationID = jClientQ.toString();
				        	  msgIdMap.put(correlationID, txtMsg.getJMSMessageID());
			        	  }
			        	  
		            	  if(reqRespMap.containsKey(correlationID)) {
		            		  logger.error("CorrelationID " + correlationID + " in incoming message not unique");
		            		  processInvalidRequest(txtMsg);
		            	  }
		            	  
		            	  reqRespMap.put(correlationID, jClientQ);
		            	  
		            	  String[] req = new String[]{uri};
		  	              
		                  Message message = jtaSession.createObjectMessage(req);
		                  message.setJMSCorrelationID(correlationID);
		                  message.setJMSReplyTo(delegateQ);
		
		      			  producer.send(jtaSession.createQueue(jtaUriMap.get(uri).get(0)), message);

		        	  }
		          }
		      }while(!isShutdown);
		     
		  } catch (Exception e) {
		      logger.error("Exception caught while listening for request in delegate : ",e);
		  }
	}
	
	private synchronized void updateURIMaps(JasperAdminMessage jam) throws Exception{
		List<String> uriQueueList = new ArrayList<String>();
		List<String> jtaQueueList = new ArrayList<String>();
		String uri = jam.getDetails()[0].trim();
		String jtaName = jam.getDst();
		
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
			// Create a ConnectionFactory
			ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost");

			// Create a Connection
			connectionFactory.setUserName(JasperConstants.JASPER_ADMIN_USERNAME);
			connectionFactory.setPassword(JasperConstants.JASPER_ADMIN_PASSWORD);
			Connection connection = connectionFactory.createConnection();
			connection.start();

			// Create a Session
			Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

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
			connection.close();
		}
		catch (Exception e) {
			logger.error("Exception caught while notifying peers: ", e);
		}
	}
}
