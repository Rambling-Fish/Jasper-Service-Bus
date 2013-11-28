package org.jasper.jsc;

import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.JMSSecurityException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.log4j.Logger;

public class Jsc implements MessageListener  {

	private static final String DELEGATE_GLOBAL_QUEUE = "jms.jasper.delegate.global.queue";

	private static final long AUDIT_TIME_IN_MILLISECONDS = 15000;

	private static Logger log = Logger.getLogger(Jsc.class.getName());
	
	private Properties prop;
	
	private Connection connection = null;
	private Session session = null;
	private Queue globalDelegateQueue;
	private MessageProducer producer;
	private Queue jscQueue;
	private MessageConsumer responseConsumer;
	private Map<String,Message> responses;
	private Map<String,Object> locks;
	
	private ScheduledExecutorService mapAuditExecutor;
	
	private int jscTimeout = 0;
	
	public Jsc(Properties prop){
		this.prop = prop;
	}
	
	
	public void init() throws JMSException {

    	responses = new ConcurrentHashMap<String, Message>();
    	locks = new ConcurrentHashMap<String, Object>();
    	
    	String user = prop.getProperty("jsc.username");
    	String password = prop.getProperty("jsc.password");
    	String timeout  = prop.getProperty("jsc.timeout", "60000");
    	try{
    		jscTimeout = Integer.parseInt(timeout);
    	} catch (NumberFormatException ex) {
    		jscTimeout = 60000; // set to 60 seconds on error
    	}
 
		try {
			String transportURL = prop.getProperty("jsc.transport");
			ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(transportURL);
			connection = connectionFactory.createConnection(user, password);
			connection.setExceptionListener(new ExceptionListener() {
				public void onException(JMSException arg0) {
					log.error("Exception caught in JSC, ignoring : ", arg0);
				}
			});
			
			if(log.isInfoEnabled()){
				log.info("Queue Connection successfully established with " + prop.getProperty("jsc.transport"));
			}
			
		} catch (JMSSecurityException se) {
			log.error("client authentication failed due to an invalid user name or password.", se);
			throw se;
		} catch (JMSException e) {
			log.error("the JMS provider failed to create the queue connection ", e);
			throw e;
		}	
		
		try {
			connection.start();
			session = connection.createSession(false,Session.AUTO_ACKNOWLEDGE);
			globalDelegateQueue = session.createQueue(DELEGATE_GLOBAL_QUEUE);
			
			producer = session.createProducer(globalDelegateQueue);
			producer.setDeliveryMode(DeliveryMode.PERSISTENT);
			producer.setTimeToLive(30000);
			
			jscQueue = session.createQueue("jms.jsc." + System.nanoTime() + ".queue");
	        responseConsumer = session.createConsumer(jscQueue);
	        responseConsumer.setMessageListener(this);
		} catch (JMSException e) {
			log.error("Exception when connecting to UDE",e);
			throw e;
		}
		
		mapAuditExecutor = Executors.newSingleThreadScheduledExecutor();
		Runnable command = new Runnable() {
			public void run() {
				auditMap();
			}
		};;;
		
		mapAuditExecutor.scheduleAtFixedRate(command , AUDIT_TIME_IN_MILLISECONDS, AUDIT_TIME_IN_MILLISECONDS, TimeUnit.MILLISECONDS);
		
    }
	
    public void destroy(){

    	try {
        	mapAuditExecutor.shutdown();
			mapAuditExecutor.awaitTermination(AUDIT_TIME_IN_MILLISECONDS, TimeUnit.MILLISECONDS);
		} catch (InterruptedException ie) {
			log.error("mapAuditExecutor failed to terminate, forcing shutdown",ie);
		}finally{
			if(!mapAuditExecutor.isShutdown()) mapAuditExecutor.shutdownNow();
		}
    	
		try {
	    	responseConsumer.close();
	    	producer.close();
	    	session.close();
	    	connection.stop();
	    	connection.close();
		} catch (JMSException e) {
			log.error("Exception when destroying servlet and cleaning resources conencted to jasper",e);
		}
		
    }

	private void auditMap() {
		synchronized (responses) {
			long currentTime = System.currentTimeMillis();
			for(String key:responses.keySet()){
				try {
					if((responses.get(key).getJMSTimestamp() + AUDIT_TIME_IN_MILLISECONDS) > currentTime){
						log.warn("Map audit found response that has timed out and weren't forwarded to JSC, removing response from map and droping response for JMSCorrelationID : " + key);
						responses.remove(key);
						locks.remove(key).notifyAll();
					}
				} catch (JMSException e) {
					log.error("Exception caught when getting JMSExpiration",e);
				}
			}
		}
		
	}
	
	public String get(String request){
		try{
			TextMessage message = session.createTextMessage(request);

			String correlationID = UUID.randomUUID().toString();
			message.setJMSCorrelationID(correlationID);
			message.setJMSReplyTo(jscQueue);
			
			Message responseJmsMsg = null;
			Object lock = new Object();
			synchronized (lock) {
				locks.put(correlationID, lock);
				producer.send(message);
			    int count = 0;
			    while(!responses.containsKey(correlationID)){
			    	try {
						lock.wait(jscTimeout);
					} catch (InterruptedException e) {
						log.error("Interrupted while waiting for lock notification",e);
					}
			    	count++;
			    	if(count >= 1)break;
			    }
			    responseJmsMsg = responses.remove(correlationID);
			}
			
			if(responseJmsMsg == null){
				log.warn("jms response from UDE is null for request <" + request + "> with correlationID = " + correlationID + " returning null");
				return null;
			}else if(!(responseJmsMsg instanceof TextMessage)){
				log.warn("jms response from UDE is not instanceof TextMessage, ignoring for request <" + request + "> with correlationID = " + correlationID + " and returning null");
				return null;
			}else{
				if(log.isInfoEnabled()){
					log.info("jms response from UDE for request <" + request + "> with correlationID = " + correlationID + " is : " + ((TextMessage)responseJmsMsg).getText());
				}
				return ((TextMessage)responseJmsMsg).getText();
			}

		}catch (JMSException e){
			log.error("JMSException during get(String request), for request : " + request + " logging and returning null", e);
			return null;
		}
	}
	
	
	public void onMessage(Message msg) {
		try{
			if(msg.getJMSCorrelationID() == null){
				log.warn("jms response message recieved with null JMSCorrelationID, ignoring message.");
				return;
			}

			msg.setJMSTimestamp(System.currentTimeMillis());
			
			if(locks.containsKey(msg.getJMSCorrelationID())){
				responses.put(msg.getJMSCorrelationID(), msg);
				Object lock = locks.remove(msg.getJMSCorrelationID());
				synchronized (lock) {
					lock.notifyAll();
				}
			}else{
				log.error("response with correlationID = " + msg.getJMSCorrelationID() + " recieved however no record of sending message with this ID, ignoring");
			}

		} catch (JMSException e) {
			log.error("Exception when storing response recieved in onMessage",e);
		}		
	}
	
}
