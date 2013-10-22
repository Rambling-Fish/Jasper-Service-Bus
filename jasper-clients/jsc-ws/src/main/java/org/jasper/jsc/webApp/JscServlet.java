package org.jasper.jsc.webApp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
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
	private Map<String,Message> responses;
	private Map<String,Object> locks;
	public Map<String, String> uriMapper = new HashMap<String, String>();

	private ScheduledExecutorService mapAuditExecutor;

	public JscServlet(){
		super();
    }
    
	public void init(){
    	Properties prop = getProperties();
    	loadOntologyMapper();

    	responses = new ConcurrentHashMap<String, Message>();
    	locks = new ConcurrentHashMap<String, Object>();
    	
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
    
    private void loadOntologyMapper() {
    	BufferedReader br = null;
    	String line;
    	String[] parsedLine;
		try {
    	File file = new File(System.getProperty("catalina.base") + "/conf/jsc.ontology.properties");
			if(file.exists()){
				FileReader inputFile = new FileReader(file);
				if(log.isInfoEnabled()) log.info("loading properties file from catalina.base/conf");
				br = new BufferedReader(inputFile);

			    // Read file line by line and print on the console
			    while ((line = br.readLine()) != null)   {
			    	parsedLine = line.split("=");
			    	uriMapper.put(parsedLine[0], parsedLine[1]);
			    }
			    //Close the buffer reader
			    br.close();
			}else{
				InputStream input = getServletContext().getResourceAsStream("/WEB-INF/conf/jsc.ontology.properties");
				if(input != null){
					br = new BufferedReader(new InputStreamReader(input));
					if(log.isInfoEnabled()) log.info("loading properties file from WEB-INF/conf");
					while ((line = br.readLine()) != null)   {
						parsedLine = line.split("=");
						uriMapper.put(parsedLine[0], parsedLine[1]);
					}
					//Close the buffer reader
					br.close();
				}
			}
			
		} catch (IOException e) {
			log.warn("error loading ontology properties file - continuing without", e);
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
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response)  throws ServletException, IOException{
    	StringBuffer jasperQuery = new StringBuffer();
    	String[] tmp;
        String path = (request.getRequestURI().length()>request.getContextPath().length())?request.getRequestURI().substring(request.getContextPath().length()+1):"";
        if(uriMapper.containsKey(path)){
        	jasperQuery.append(uriMapper.get(path));
        }
        else{
        	jasperQuery.append(path);
        }
 
        if(request.getQueryString() != null){
            jasperQuery.append("?");
            if(!request.getQueryString().startsWith("query=")){
            	// parse all parameters in incoming query
            	String parms[] = request.getQueryString().split("\\?");

            	for(int i=0;i<parms.length;i++){
            		if(!parms[i].startsWith("trigger=")){
            			tmp = parms[i].split("=");
            			if(uriMapper.containsKey(tmp[0])){
            				jasperQuery.append(uriMapper.get(tmp[0]));
            				if(tmp.length == 2){
            					jasperQuery.append("=");
            					jasperQuery.append(tmp[1]);
            				}
            				if(tmp.length >= i) jasperQuery.append("?");
            			}
            			else{
            				jasperQuery.append(tmp[0]);
            				if(tmp.length == 2){
            					jasperQuery.append("=");
            					jasperQuery.append(tmp[1]);
            				}
            				if(tmp.length >= i) jasperQuery.append("?");
            			}
            		}
            		else{
            			jasperQuery.append(parms[i]);
            			if(parms.length >= i) jasperQuery.append("?");
            		}
            	}
            }
            else{
            	jasperQuery.append(request.getQueryString());
            }
        }
       
		try {

	        TextMessage message = session.createTextMessage();

			message.setText(jasperQuery.toString());
			String correlationID = UUID.randomUUID().toString();
			message.setJMSCorrelationID(correlationID);
			message.setJMSReplyTo(servletQueue);
			
			Message responseJmsMsg = null;
			Object lock = new Object();
			synchronized (lock) {
				locks.put(correlationID, lock);
				producer.send(message);
			    int count = 0;
			    while(!responses.containsKey(correlationID)){
			    	try {
						lock.wait(10000);
					} catch (InterruptedException e) {
						log.error("Interrupted while waiting for lock notification",e);
					}
			    	count++;
			    	if(count >= 6)break;
			    }
			    responseJmsMsg = responses.remove(correlationID);
			}
			
			response.setContentType("application/json");
	        response.setCharacterEncoding("UTF-8");
			
			if(responseJmsMsg == null){
				response.getWriter().write("{\"error\"=\"jscTimedOutWaitingForJsbResponse\"}");
				log.warn("No respone for JMSCorrelationID : " + correlationID + " sending {\"error\"=\"jscTimedOutWaitingForJsbResponse\"} back to client");			
			}else if (responseJmsMsg instanceof TextMessage){
				response.getWriter().write(((TextMessage) responseJmsMsg).getText());
			}else{
				response.getWriter().write("{\"error\"=\"responseWasNotJmsTextMessage\"}");
				log.warn("Response was not a TextMessage for JMSCorrelationID : " + correlationID + " sending {} back to client");
			}
			
		} catch (JMSException e) {
			log.error("Exception when trying to send request to jasper",e);
		}
    }

    protected void doPost(HttpServletRequest httpservletrequest, HttpServletResponse httpservletresponse) throws ServletException, IOException{
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