package org.jasper.core.delegate;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.log4j.Logger;
import org.jasper.core.message.JasperSyncRequest;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Command;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Type;

public class Delegate implements Runnable {

	private static final String DELEGATE_QUEUE_PREFIX = "jms.jasper.delegate.";
	private static final String DELEGATE_QUEUE_SUFFIX = ".queue";
	private Map<String,String> jtaUriMap;
	private Map<String, Destination> reqRespMap;
	private String name;
	private Connection connection;
	private boolean isShutdown;
	private ExecutorService singleThreadExecutor;
	private Destination delegateQueue;
	static Logger logger = Logger.getLogger("org.jasper");
	
	
	public Delegate(String name,Connection connection, Map<String,String> map) {
		this.name = name;
		this.connection = connection;
		this.jtaUriMap = map;
		this.reqRespMap = new ConcurrentHashMap<String, Destination>();
		this.isShutdown = false;
		
		singleThreadExecutor = Executors.newSingleThreadExecutor();
		Runnable processResponsesThread = new Runnable() {
			@Override
			public void run() {
				processResponses();
			}
		};;;
		singleThreadExecutor.submit(processResponsesThread);
		
		if(logger.isInfoEnabled()) {
			logger.info("Delegate created : " + name);
		}

	}
	
	public void shutdown(){
		isShutdown = true;
		singleThreadExecutor.shutdown();
	}
	
	@Override
	public void run(){
		processRequests();
	}
	
	private void setDelegateQueue(Destination queue){
		delegateQueue = queue;
	}
	
	private Destination getDelegateQueue() {
		if(delegateQueue == null)
			try {
				this.wait(1000);
				if(delegateQueue != null) System.out.println("Successfully waited for delegateQueue to be created");
			} catch (InterruptedException e) {
				System.out.println("Exception caught while waiting for delegate queue to be initialized.");
				e.printStackTrace();
			}
		return delegateQueue;
	}
	
	public void processResponses(){
		 try {
		      Session delegateSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

		      // Create Queue
		      Destination delegateQueue = delegateSession.createQueue(DELEGATE_QUEUE_PREFIX + name + DELEGATE_QUEUE_SUFFIX);
		      setDelegateQueue(delegateQueue);
		                  
		      // Create a MessageConsumer from the Session to the Queue
		      MessageConsumer delegateConsumer = delegateSession.createConsumer(delegateQueue);


		      // Wait for a message
		      Message jtaResponse;
		      
		      do{
		          do{
		          	jtaResponse = delegateConsumer.receive(1000);
		          }while(jtaResponse == null && !isShutdown);
		          if(isShutdown) break;
		          if (jtaResponse instanceof TextMessage) {
		        	  TextMessage responseMessage = (TextMessage) jtaResponse;	

		              
	            	  String coorelationID = responseMessage.getJMSCorrelationID();
	            	  if(!reqRespMap.containsKey(coorelationID)) throw new Exception("coorealtionID for response not found");
	            	 
	            	  // Create a Session
	                  Session jClientSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	            	  Destination jClientQueueDestination = reqRespMap.remove(coorelationID);
	  		       
	                  // Create a MessageProducer from the Session to the Queue
	                  MessageProducer producer = jClientSession.createProducer(jClientQueueDestination);
	                  producer.setDeliveryMode(DeliveryMode.PERSISTENT);
	                  producer.setTimeToLive(30000);

	                  Message message = jClientSession.createObjectMessage(responseMessage.getText());
	                  message.setJMSCorrelationID(coorelationID);
	      			  producer.send(message);

	                  // Clean up
	                  jClientSession.close();
		          }
		      }while(!isShutdown);
		      
		      delegateConsumer.close();
		      delegateSession.close();
		     
		  } catch (Exception e) {
		      System.out.println("Exception caught while listening for my events: " + e);
		      e.printStackTrace();
		  }
	}
	
	
	public void processRequests() {
		  try {
	  	     	
		      Session globalSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

		      // Create Queue
		      Destination globalQueue = globalSession.createQueue(DelegateFactory.DELEGATE_GLOBAL_QUEUE);
		                  
		      // Create a MessageConsumer from the Session to the Queue
		      MessageConsumer globalDelegateConsumer = globalSession.createConsumer(globalQueue);

		      // Wait for a message
		      Message jClientRequest;
		      
		      do{
		          do{
		          	jClientRequest = globalDelegateConsumer.receive(1000);
		          }while(jClientRequest == null && !isShutdown);
		          if(isShutdown) break;
		          if (jClientRequest instanceof ObjectMessage) {
		        	  ObjectMessage objMessage = (ObjectMessage) jClientRequest;
		              Object obj = objMessage.getObject();
		              if(obj instanceof JasperSyncRequest){
		            	  String coorelationID = objMessage.getJMSCorrelationID();
		            	  Destination jClientQ = objMessage.getJMSReplyTo();
		            	  if(reqRespMap.containsKey(coorelationID)) throw new Exception("Reusing coorealtionID in req, should be unique");
		            	  reqRespMap.put(coorelationID, jClientQ);
		            	  
		            	  JasperSyncRequest req = (JasperSyncRequest)obj;
		            	  
		            	  // Create a Session
		                  Session jtaSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

		                  // Create the destination for JTA queue
		                  Destination jtaQueueDestination = jtaSession.createQueue(jtaUriMap.get(req.getUri()));
		                  
		                  // Create a MessageProducer from the Session to the Queue
		                  MessageProducer producer = jtaSession.createProducer(jtaQueueDestination);
		                  producer.setDeliveryMode(DeliveryMode.PERSISTENT);
		                  producer.setTimeToLive(30000);

		                  Message message = jtaSession.createObjectMessage(req);
		                  message.setJMSCorrelationID(coorelationID);
		                  message.setJMSReplyTo(getDelegateQueue());

 		      			  producer.send(message);

		                  // Clean up
		                  jtaSession.close();
		            	  
		              }
		              else if(obj instanceof JasperAdminMessage) {
		            	  JasperAdminMessage jam = (JasperAdminMessage)obj;
		            	  if(jam.getType() == Type.jtaDataManagement) {
		          			if(jam.getCommand() == Command.notify) {
		          				DelegateFactory factory = DelegateFactory.getInstance();
		          				factory.jtaUriMap.put(jam.getDetails()[0], jam.getSrc());
		          				if(logger.isInfoEnabled()) {
		          					logger.info("Received " + jam.getCommand() + " from " + jam.getSrc() + " with details: " + jam.getDetails()[0]);
		                  		}
		          			}
		          			else if(jam.getCommand() == Command.delete) {
		          				removeURI(jam.getSrc());
		          			}
		          		}
		              }
		          }
		      }while(!isShutdown);
		      
		      globalDelegateConsumer.close();
		      globalSession.close();
		     
		  } catch (Exception e) {
		      System.out.println("Exception caught while listening for my events: " + e);
		      e.printStackTrace();
		  }
	}
	
	private synchronized void removeURI(String username) {
		Iterator<String> it = jtaUriMap.keySet().iterator();
		while(it.hasNext()) {
			String key= (String)it.next(); 
			String value= jtaUriMap.get(key);
			if(value.startsWith(username)) {
				jtaUriMap.remove(key);
				if(logger.isInfoEnabled()){
		    		logger.info("Removed URI " + key + " for JTA " + username);
		    	}
			}
		}
	}
	
	
}
