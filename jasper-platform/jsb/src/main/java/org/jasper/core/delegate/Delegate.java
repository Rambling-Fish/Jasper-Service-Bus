package org.jasper.core.delegate;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.log4j.Logger;
import org.jasper.core.JECore;
import org.jasper.core.constants.JasperConstants;
import org.jasper.core.delegate.handlers.AdminHandler;
import org.jasper.core.delegate.handlers.DataHandler;
import org.jasper.core.delegate.handlers.SparqlHandler;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Type;

import com.hp.hpl.jena.rdf.model.Model;

public class Delegate implements Runnable, MessageListener {

	private boolean isShutdown;
	private Session globalSession;
	private Queue globalQueue;
	private MessageConsumer globalDelegateConsumer;
	private Session jtaSession;
	private MessageProducer producer;
	private Destination delegateQ;
	private MessageConsumer responseConsumer;
    private ExecutorService delegateHandlers;
	
	private Map<String,Message> responseMessages;
	private Map<String,Object> locks;
	private DelegateOntology jOntology;

	
	static Logger logger = Logger.getLogger(Delegate.class.getName());
	static private AtomicInteger count = new AtomicInteger(0);
	
	public Delegate(Connection connection, Model model,DelegateOntology jOntology) throws JMSException {
		this.isShutdown  = false;
		
		this.responseMessages = new ConcurrentHashMap<String, Message>();
		this.locks = new ConcurrentHashMap<String, Object>();
		
        delegateHandlers = Executors.newFixedThreadPool(2);
        this.jOntology = jOntology;

		
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
	
	public void run(){
		processRequests();
	}
		
	public void onMessage(Message msg) {
		try{
			if(logger.isDebugEnabled()){
				logger.debug("Message response received = " + msg);
	  	  	}
			
			if(locks.containsKey(msg.getJMSCorrelationID())){
				responseMessages.put(msg.getJMSCorrelationID(), msg);
				Object lock = locks.remove(msg.getJMSCorrelationID());
				synchronized (lock) {
					lock.notifyAll();
				}
			}else{
				logger.error("response with correlationID = " + msg.getJMSCorrelationID() + " recieved however no record of sending message with this ID, ignoring");
			}
	
		}catch (JMSException jmse){
			logger.error("error occured in onMessage",jmse);
		}
	}
	
	public void sendMessage(Destination destination, Message message) throws JMSException{
		message.setJMSReplyTo(delegateQ);
		producer.send(destination,message);
	}
	
	public void sendMessage(String destination, Message message) throws JMSException{
		this.sendMessage(jtaSession.createQueue(destination),message);
	}
	
	public TextMessage createTextMessage(String txt) throws JMSException{
		return jtaSession.createTextMessage(txt);
	}
	
	public ObjectMessage createObjectMessage(Serializable obj) throws JMSException{
		return jtaSession.createObjectMessage(obj);
	}
	
	public MapMessage createMapMessage(Map<String,?> map) throws JMSException{
		MapMessage mapMsg = jtaSession.createMapMessage();
		for(String key:map.keySet()){
			mapMsg.setObject(key, map.get(key));
		}
		return mapMsg;
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
			            	JasperAdminMessage jam = ((JasperAdminMessage)obj);
			            	if (jam.getType() == Type.jtaDataManagement){
			            		delegateHandlers.submit(new AdminHandler(this, jOntology, jmsRequest, jam, null, null));
			            	}	
			            }
		          } else if(jmsRequest instanceof TextMessage){
		        	  String text = ((TextMessage) jmsRequest).getText();
		        	  if(text != null && text.startsWith("?query=")){
		        		  delegateHandlers.submit(new SparqlHandler(this, jOntology, jmsRequest, null, null));
		        	  }else if(text != null){
		        		  delegateHandlers.submit(new DataHandler(this, jOntology, jmsRequest, locks, responseMessages));
		        	  }else{
			        		logger.error("Incoming text message has null payload - ignoring " + jmsRequest);
			        	}	
			        }else{
			        	logger.warn("JMS Message neither ObjectMessage nor TextMessage, ignoring request : " + jmsRequest);
			        }             
		          
		      }while(!isShutdown);
		     
		  } catch (Exception e) {
		      logger.error("Exception caught while listening for request in delegate : ",e);
		  }
	}
}
