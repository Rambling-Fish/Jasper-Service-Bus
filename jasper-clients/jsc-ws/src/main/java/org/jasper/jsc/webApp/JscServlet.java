package org.jasper.jsc.webApp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.log4j.Logger;

@WebServlet("/")
public class JscServlet extends HttpServlet  implements MessageListener  {
	private static final long serialVersionUID = 1L;
	
	private static final String DELEGATE_GLOBAL_QUEUE = "jms.jasper.delegate.global.queue";

	private static final long AUDIT_TIME_IN_MILLISECONDS = 15000;

	static Logger log = Logger.getLogger(JscServlet.class.getName());
	
	private Connection connection = null;
	private Session session = null;
	private Queue globalDelegateQueue;
	private MessageProducer producer;
	private Queue servletQueue;
	private MessageConsumer responseConsumer;
	private Map<String,TimeStampedHttpServletResponse> httpServletResponses;

	private ScheduledExecutorService mapAuditExecutor;

	class TimeStampedHttpServletResponse{
		HttpServletResponse httpServletResponse;
		long timestamp;
		
		TimeStampedHttpServletResponse(HttpServletResponse resp){
			httpServletResponse = resp;
			timestamp = System.currentTimeMillis();
		}
		
		HttpServletResponse getHttpServletResponse(){
			return httpServletResponse;
		}
		
		long getTimestamp(){
			return timestamp;
		}
		
	}

	public JscServlet(){
		super();
    }
    
	public void init(){

    	Properties prop = getProperties();
    	
    	httpServletResponses = new ConcurrentHashMap<String, JscServlet.TimeStampedHttpServletResponse>();
    	
    	String user = prop.getProperty("jsc.username");
    	String password = prop.getProperty("jsc.password");
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
			log.error(" client authentication failed due to an invalid user name or password.", se);
		} catch (JMSException e) {
			log.error("the JMS provider failed to create the queue connection ", e);
		}	
		
		try {
			connection.start();
			session = connection.createSession(false,Session.AUTO_ACKNOWLEDGE);
			globalDelegateQueue = session.createQueue(DELEGATE_GLOBAL_QUEUE);
			
			producer = session.createProducer(globalDelegateQueue);
			producer.setDeliveryMode(DeliveryMode.PERSISTENT);
			producer.setTimeToLive(30000);
			
			servletQueue = session.createQueue("jms.jsc." + System.nanoTime() + ".queue");
	        responseConsumer = session.createConsumer(servletQueue);
	        responseConsumer.setMessageListener(this);
		} catch (JMSException e) {
			log.error("Exception when initilizing servlet and connecting to jasper",e);
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
    
    private Properties getProperties() {
    	Properties prop = new Properties();
		try {
			
			File file = new File(System.getProperty("catalina.base") + "/conf/jsc.properties");
			if(file.exists()){
				FileInputStream fileInputStream = new FileInputStream(file);
				if(log.isInfoEnabled()) log.info("loading properties file from catalina.base/conf");
				prop.load(fileInputStream);
			}else{
				InputStream input = getServletContext().getResourceAsStream("/WEB-INF/conf/jsc.properties");
				if(log.isInfoEnabled()) log.info("loading properties file from WEB-INF/conf");
				prop.load(input);
			}
			
		} catch (IOException e) {
			log.error("error loading jsc properties file.", e);
		}  		
    	return prop;
	}
	
	private void auditMap() {
		synchronized (httpServletResponses) {
			long currentTime = System.currentTimeMillis();

			for(String key:httpServletResponses.keySet()){
				if((httpServletResponses.get(key).getTimestamp() + AUDIT_TIME_IN_MILLISECONDS) > currentTime){
					log.warn("Map audit found response that has timed out, removing response from map and responding with empty set {} for JMSCorrelationID : " + key);
					HttpServletResponse response = httpServletResponses.remove(key).getHttpServletResponse();
					response.setContentType("application/json");
			        response.setCharacterEncoding("UTF-8");
					try {
						response.getWriter().write("{}");
						response.flushBuffer();
					} catch (IOException e) {
						log.error("Exception when trying to write to response",e);
					}
				}
			}
		}
		
	}
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response)  throws ServletException, IOException{
        StringBuffer jasperQuery = new StringBuffer();
        String path = request.getRequestURI().substring(request.getContextPath().length()+1);
        jasperQuery.append(path);
        if(request.getQueryString() != null)
        {
            jasperQuery.append("?");
            jasperQuery.append(request.getQueryString());
        }
       
		try {

	        TextMessage message = session.createTextMessage();

			message.setText(jasperQuery.toString());
			String correlationID = UUID.randomUUID().toString();
			message.setJMSCorrelationID(correlationID);
			message.setJMSReplyTo(servletQueue);
			
			httpServletResponses.put(correlationID, new TimeStampedHttpServletResponse(response));
			producer.send(message);
			
			//wait for response to be sent (including audit timeout error) or max of 2 minutes
			int count = 0;
			while(httpServletResponses.containsKey(correlationID) && count < 1200){
				Thread.sleep(100);
				count++;
			}
			
		} catch (JMSException e) {
			log.error("Exception when trying to send request to jasper",e);
		} catch (InterruptedException e) {
			log.error("Exception when waiting for response",e);
		}
    }

    protected void doPost(HttpServletRequest httpservletrequest, HttpServletResponse httpservletresponse) throws ServletException, IOException{
    }

	public void onMessage(Message msg) {
		try{
			if(!httpServletResponses.containsKey(msg.getJMSCorrelationID())){
				log.warn("jms response message recieved with unknown JMSCorrelationID : " + msg.getJMSCorrelationID() + ", ignoring message. Orginal request may have timed out");
				return;
			}
			HttpServletResponse response = httpServletResponses.remove(msg.getJMSCorrelationID()).getHttpServletResponse();
			response.setContentType("application/json");
	        response.setCharacterEncoding("UTF-8");
	        
		    try {
				if(msg instanceof TextMessage){
					response.getWriter().write(((TextMessage) msg).getText());
					response.flushBuffer();
				}else{
					log.warn("jms response message recieved not TextMessage ignoring response, sending empty set {}.");
					response.getWriter().write("{}");
					response.flushBuffer();
				}
			} catch (IOException e) {
				log.error("Exception when trying to write to response",e);
			}
			
		} catch (JMSException e) {
			log.error("Exception when trying access jms response message",e);
		}		
	}

}