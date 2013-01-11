package org.jasper.jCore.auth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.Broker;
import org.apache.activemq.broker.BrokerFilter;
import org.apache.activemq.broker.ConnectionContext;
import org.apache.activemq.command.ConnectionInfo;
import org.apache.log4j.Logger;
import org.jasper.jCore.admin.JasperAdminMessage;
import org.jasper.jCore.admin.JasperAdminMessage.Command;
import org.jasper.jCore.admin.JasperAdminMessage.Type;
import org.jasper.jCore.engine.JECore;

public class JasperBroker extends BrokerFilter {
    
	/*
	 * Map will store known connections to prevent multiple JTAs from using same JTA license key
	 */
	private static Map<String, ConnectionInfo> jtaConnectionInfoMap = new ConcurrentHashMap<String, ConnectionInfo>();
	
	/*
	 * Map will store JTAs that have been registered into the cluster remotely.
	 */
	private static Map<String,String> remoteJtaRegistrationMap = new ConcurrentHashMap<String, String>();

	/*
	 * Maps will store known connections and contexts to prevent multiple JSBs from using same JSB license key
	 */
	private static Map<String, ConnectionInfo> jsbConnectionInfoMap = new ConcurrentHashMap<String, ConnectionInfo>();
	private static Map<String, ConnectionContext> jtaConnectionContextMap = new ConcurrentHashMap<String, ConnectionContext>();

	/*
	 * access to the JECore to determine deployment id and access to utilities
	 */
	private JECore core;

	/*
	 * boolean to stop processing peer notificaitons
	 */
	private boolean processPeerNotificaitons;
	
	/*
	 * to executors used to create new threads to listen to different admin JMS events
	 */
	private ScheduledExecutorService exec1;
	private ScheduledExecutorService exec2;

	
	static Logger logger = Logger.getLogger("org.jasper");
	
     public JasperBroker(Broker next) {
        super(next);
        core = JECore.getInstance();
        processPeerNotificaitons = true;
    }
     
     public void start() throws Exception{
         next.start();
     }

     public void brokerServiceStarted() {
         startListeningForPeerNotificaitons();
         next.brokerServiceStarted();
     }
     
     public void stop() throws Exception {
    	 stopPeerNotificaitons();
         next.stop();
     }
     
    private void startListeningForPeerNotificaitons(){
		exec1 = Executors.newSingleThreadScheduledExecutor();
		exec2 = Executors.newSingleThreadScheduledExecutor();
		Runnable runListenForBroadcastEvents = new Runnable() {
			@Override
			public void run() {
				listenForBroadcastEvents();
			}
		};;;
		Runnable runListenForMyEvents = new Runnable() {
			@Override
			public void run() {
				listenForMyEvents();
			}
		};;;
		exec1.schedule(runListenForBroadcastEvents, 0, TimeUnit.SECONDS);
		exec2.schedule(runListenForMyEvents, 0, TimeUnit.SECONDS);
    }
    
    private void stopPeerNotificaitons(){
    	processPeerNotificaitons = false;
    	exec1.shutdown();
    	exec2.shutdown();
    }
    
    private void notifyPeers(Command command, String msgDetails) {
        try {
            // Create a ConnectionFactory
            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost");

            // Create a Connection
            connectionFactory.setUserName("jasperAdmin");         //TODO factor into constant
            connectionFactory.setPassword("japerAdminPassword");  //TODO factor into constant
            Connection connection = connectionFactory.createConnection();
            connection.start();

            // Create a Session
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // Create the destination (Topic or Queue)
            Destination destination = session.createTopic("jms.jasper.admin.messages.topic"); //TODO factor into constant

            // Create a MessageProducer from the Session to the Topic or Queue
            MessageProducer producer = session.createProducer(destination);
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

            JasperAdminMessage jam = new JasperAdminMessage(Type.jsbClusterManagement, command, core.getJSBInstance(), "*",  msgDetails);

            Message message = session.createObjectMessage(jam);
			producer.send(message);

            // Clean up
            session.close();
            connection.close();
        }
        catch (Exception e) {
            logger.error("Exception caught while notifiging peers: ", e);
        }
	}
    
    private void notifyPeer(Command command, String dst, String msgDetails) {
        try {

            // Create a ConnectionFactory
            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost");

            // Create a Connection
            connectionFactory.setUserName("jasperAdmin");  		 //TODO factor into constant
            connectionFactory.setPassword("japerAdminPassword"); //TODO factor into constant
            Connection connection = connectionFactory.createConnection();
            connection.start();

            // Create a Session
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // Create the destination (Topic or Queue)
            Destination destination = session.createQueue("jms.jasper.admin.messages.queue." + dst); //TODO factor into constant

            // Create a MessageProducer from the Session to the Topic or Queue
            MessageProducer producer = session.createProducer(destination);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            producer.setTimeToLive(5000);

            JasperAdminMessage jam = new JasperAdminMessage(Type.jsbClusterManagement, command, core.getJSBInstance(), dst,  msgDetails);

            Message message = session.createObjectMessage(jam);
			producer.send(message);

            // Clean up
            session.close();
            connection.close();
        }
        catch (Exception e) {
            logger.error("Exception caught while notifiging peers: ", e);
        }
	}
    
    private void listenForBroadcastEvents(){
        try {
        	     	
            // Create a ConnectionFactory
            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost");

            // Create a Connection
            connectionFactory.setUserName("jasperAdmin");			//TODO factor into constant
            connectionFactory.setPassword("japerAdminPassword");	//TODO factor into constant
            Connection connection = connectionFactory.createConnection();
            connection.start();

            // Create a Session
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // Create the destination (Topic or Queue)
            Destination destination = session.createTopic("jms.jasper.admin.messages.topic"); //TODO factor into constant

            // Create a MessageConsumer from the Session to the Topic or Queue
            MessageConsumer consumer = session.createConsumer(destination);

            // Wait for a message
            Message message;
            
            do{
	            do{
	            	message = consumer.receive(1000);
	            }while(message ==null && processPeerNotificaitons);
	
	            
	            if (message instanceof ObjectMessage) {
	            	ObjectMessage objMessage = (ObjectMessage) message;
	                Object obj = objMessage.getObject();
	                if(obj instanceof JasperAdminMessage){
	                	processJasperAdminMessages((JasperAdminMessage) obj);
	                }
	            }
            }while(processPeerNotificaitons);
            
            consumer.close();
            session.close();
            connection.close();
        } catch (Exception e) {
            logger.error("Exception caught while listening for broadcast events: ", e);
        }
    }
    
    private void listenForMyEvents(){
        try {
        	     	
            // Create a ConnectionFactory
            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost");

            // Create a Connection
            connectionFactory.setUserName("jasperAdmin");			//TODO factor into constant
            connectionFactory.setPassword("japerAdminPassword");	//TODO factor into constant
            Connection connection = connectionFactory.createConnection();
            connection.start();

            // Create a Session
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // Create the destination (Topic or Queue)
            Destination destination = session.createQueue("jms.jasper.admin.messages.queue." + core.getJSBInstance());	//TODO factor into constant
                        
            // Create a MessageConsumer from the Session to the Topic or Queue
            MessageConsumer consumer = session.createConsumer(destination);

            // Wait for a message
            Message message;
            
            do{
	            do{
	            	message = consumer.receive(1000);
	            }while(message == null && processPeerNotificaitons);

	            if (message instanceof ObjectMessage) {
	            	ObjectMessage objMessage = (ObjectMessage) message;
	                Object obj = objMessage.getObject();
	                if(obj instanceof JasperAdminMessage){
	                	processJasperAdminMessages((JasperAdminMessage) obj);
	                }
	            }
            }while(processPeerNotificaitons);
            
            consumer.close();
            session.close();
            connection.close();
        } catch (Exception e) {
            logger.error("Exception caught while listening for my events: ", e);
        }
    }
    
	private void processJasperAdminMessages(JasperAdminMessage jam) {
		if(jam.getType() == Type.jsbClusterManagement){
			if(jam.getSrc().equals(core.getJSBInstance())) return;
        	if(jam.getCommand() == Command.add){
        		remoteJtaRegistrationMap.put(jam.getDetails(), jam.getSrc());
    		}else if(jam.getCommand() == Command.update){
    			remoteJtaRegistrationMap.put(jam.getDetails(), jam.getSrc());
    			if(jtaConnectionInfoMap.containsKey(jam.getDetails())) dropJTAConnection(jam.getDetails());
    		}else if(jam.getCommand() == Command.delete){
    			remoteJtaRegistrationMap.remove(jam.getDetails());
    		}
    	}		
	}

	private void dropJTAConnection(String licenseKey) {
		ConnectionContext context = jtaConnectionContextMap .remove(licenseKey);
		jtaConnectionInfoMap.remove(licenseKey);
		try {
				context.getConnection().stop();
		} catch (Exception e) {
			logger.error("Exception caught when trying to drop JTA connection",e);
		}
	}

	private void cleanRemoteJtaMap(String jsbInstance) {
		for(String password:remoteJtaRegistrationMap.keySet()){
			if(remoteJtaRegistrationMap.get(password).equals(jsbInstance)) remoteJtaRegistrationMap.remove(password);
		}
	}

	private void updatePeer(String dst){
		for(String password:jtaConnectionInfoMap.keySet()){
			ConnectionInfo info = jtaConnectionInfoMap.get(password);
			notifyPeer(Command.update, dst, info.getPassword());
		}
	}
   
    public void addConnection(ConnectionContext context, ConnectionInfo info) throws Exception {
    	 
    	if(info.getUserName().equals("jasperAdmin") && info.getPassword().equals("japerAdminPassword") && info.getClientIp().startsWith("vm://localhost")){
    		super.addConnection(context, info);	
    		return;
    	}
    	
    	JECore core = JECore.getInstance();
    	
    	if(!core.isValidLicenseKey(info.getUserName(), info.getPassword())){
    		throw (SecurityException)new SecurityException("Invalid license key : " + info.getUserName());
		}
    	
    	/*
    	 * If the licenseKey is valid we check to see if the connection being added is for a JTA
    	 * or a Peer JSB and run validation logic depending on which it is, if the validation logic
    	 * is success 
    	 */
    	if(core.isJSBLicenseKey(info.getPassword())){	
        	/*
        	 * During connection setup we validate that the JSB lisence key provided is valid and
        	 * matches the stated JSB. JSB info is stored in username and the license key in the password
        	 */
        	if(core.isJSBAuthenticationValid(info.getUserName(), info.getPassword())){
        		if(logger.isInfoEnabled()){
        			logger.info("Peer JSB authenticated : " + core.getJSBInstance(info.getPassword()));
        		}
        	}else{
        		logger.error("Invalid Peer JSB license key for : " + core.getJSBInstance(info.getPassword()));
    	    	throw (SecurityException)new SecurityException("Invalid Peer JSB license key for : " + core.getJSBInstance(info.getPassword()));
        	}
        	
        	/*
        	 * Check to see if JSB deploymentId matches that of the system.
        	 */
        	if(core.isSystemDeploymentId(info.getUserName().split(":")[0])){
        		if(logger.isInfoEnabled()){
        			logger.info("Peer JSB deploymentId matches that of local JSB : " + info.getUserName().split(":")[0]);
        		}
        	}else{
        		logger.error("Peer JSB deploymentId does not match that of local JSB. Peer JSB deploymentId : " + info.getUserName().split(":")[0] + " and local deploymentId : " + core.getDeploymentID());
    	    	throw (SecurityException)new SecurityException("Peer JSB deploymentId does not match that of local JSB. Peer JSB deploymentId : " + info.getUserName().split(":")[0] + " and local deploymentId : " + core.getDeploymentID());
        	}
        	
        	/*
        	 * We check that only one JSB instance id ever registers at one time, if a second JSB using the same
        	 * instance id attempts to register we throw a security exception
        	 */
        	if(!(jsbConnectionInfoMap.containsKey(info.getPassword()))){
        		jsbConnectionInfoMap.put(info.getPassword(), info);

        		if(logger.isInfoEnabled()){
        			logger.info("Peer JSB registered in system : " + core.getJSBInstance(info.getPassword()));
        		}
        	}else{
        		ConnectionInfo oldJSBInfo = jsbConnectionInfoMap.get(info.getPassword());
        		logger.error("Peer JSB not registred in system, JSB instance id must be unique and another peer JSB has registered using same instance id, peer JSB with the following info already registered \n" +
                        "deploymentId:instanceId = " + core.getJSBInstance(info.getPassword()) + "\n" +
                        "clientId:clientIp = " + oldJSBInfo.getClientId() + ":" + oldJSBInfo.getClientIp());
        		
        		throw (SecurityException)new SecurityException("Peer JSB not registred in system, JSB instance id must be unique and another peer JSB has registered using same instance id, peer JSB with the following info already registered \n" +
                        "deploymentId:instanceId = " + core.getJSBInstance(info.getPassword()) + "\n" +
                        "clientId:clientIp = " + oldJSBInfo.getClientId() + ":" + oldJSBInfo.getClientIp());
        	}
    		super.addConnection(context, info);
    		updatePeer(core.getJSBInstance(info.getPassword()));
    	}else{
        	/*
        	 * During connection setup we validate that the JTA license key provided is valid and
        	 * matches the stated JTA. JTA info is stored in username and the license key in the password
        	 */
        	if(core.isJTAAuthenticationValid(info.getUserName(), info.getPassword())){
        		if(logger.isInfoEnabled()){
        			logger.info("JTA authenticated : " + info.getUserName());
        		}
        	}else{
        		logger.error("Invalid JTA license key : " + info.getUserName());
    	    	throw (SecurityException)new SecurityException("Invalid JTA license key : " + info.getUserName());
        	}
        	
        	/*
        	 * Check to see if JTA deploymentId matches that of the system.
        	 */
        	if(core.isSystemDeploymentId(info.getUserName().split(":")[3])){
        		if(logger.isInfoEnabled()){
        			logger.info("JTA deploymentId matches that of the system : " + info.getUserName().split(":")[3]);
        		}
        	}else{
        		logger.error("JTA deploymentId does not match that of the system. JTA deploymentId : " + info.getUserName().split(":")[3] + " and system deploymentId : " + JECore.getInstance().getDeploymentID());
    	    	throw (SecurityException)new SecurityException("JTA deploymentId does not match that of the system. JTA deploymentId : " + info.getUserName().split(":")[3] + " and system deploymentId : " + JECore.getInstance().getDeploymentID());
        	}
        	
        	/*
        	 * We check that only one JTA per license key is ever registered at one time, both locally and
        	 * remotely, if a second JTA attempts to register we throw a security exception
        	 */
        	if(!(jtaConnectionInfoMap.containsKey(info.getPassword())) && !(remoteJtaRegistrationMap.containsKey(info.getPassword()))){
        		jtaConnectionInfoMap.put(info.getPassword(), info);
        		jtaConnectionContextMap.put(info.getPassword(),context);
        		notifyPeers(Command.add, info.getPassword());
        		
        		if(logger.isInfoEnabled()){
        			logger.info("JTA registered in system : " + info.getUserName());
        		}
        	}else{
        		if(jtaConnectionInfoMap.containsKey(info.getPassword())){
	        		ConnectionInfo oldAppInfo = jtaConnectionInfoMap.get(info.getPassword());
	        		logger.error("JTA not registred in system, only one instance of a JTA can be registered with JSB core at a time, JTA with with the following info already registered \n" +
	                        "vendor:appName:version:deploymentId = " + oldAppInfo.getUserName() + "\n" +
	                        "clientId:clientIp = " + oldAppInfo.getClientId() + ":" + oldAppInfo.getClientIp());
	        		throw (SecurityException)new SecurityException("JTA not registred in system, only one instance of a JTA can be registered with JSB core at a time, JTA with with the following info already registered \n" +
	                        "vendor:appName:version:deploymentId = " + oldAppInfo.getUserName() + "\n" +
	                        "clientId:clientIp = " + oldAppInfo.getClientId() + ":" + oldAppInfo.getClientIp());
        		}else{
        			logger.error("JTA not registred in system, JTA with same license key already registered on JSB : " + remoteJtaRegistrationMap.get(info.getPassword()));
	        		throw (SecurityException)new SecurityException("JTA not registred in system, JTA with same license key already registered on JSB : " + remoteJtaRegistrationMap.get(info.getPassword()));        		}
        	}
    		super.addConnection(context, info);	
    	}
    }
    
	public void removeConnection(ConnectionContext context, ConnectionInfo info, Throwable error)throws Exception{
    	if(jtaConnectionInfoMap.get(info.getPassword()) != null ){
	    	if(logger.isInfoEnabled()){
	    		logger.info("JTA deregistered from system : " + info.getUserName());
	    	}
	    	jtaConnectionInfoMap.remove(info.getPassword());
    		notifyPeers(Command.delete,info.getPassword());
    	}else if(jsbConnectionInfoMap.get(info.getPassword()) != null){
    		if(logger.isInfoEnabled()){
	    		logger.info("Peer JSB deregistered from system : " + info.getUserName());
	    	}
	    	jsbConnectionInfoMap.remove(info.getPassword());
	    	cleanRemoteJtaMap(core.getJSBInstance(info.getPassword()));
    	}
    	super.removeConnection(context, info, null);
    }
	
}