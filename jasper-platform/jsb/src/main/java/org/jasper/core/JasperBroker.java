package org.jasper.core;
 
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.Broker;
import org.apache.activemq.broker.BrokerFilter;
import org.apache.activemq.broker.ConnectionContext;
import org.apache.activemq.command.ConnectionInfo;
import org.apache.log4j.Logger;
import org.jasper.core.persistence.PersistenceFacade;
import org.jasper.jLib.jAuth.ClientLicense;
import org.jasper.jLib.jAuth.util.JAuthHelper;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Command;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Type;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.MultiMap;
 
public class JasperBroker extends BrokerFilter implements EntryListener, javax.jms.MessageListener {
     
    private static final String JASPER_ADMIN_USERNAME = "jasperAdminUsername";
    private static final String JASPER_ADMIN_PASSWORD = "jasperAdminPassword";
     
    public static final String DELEGATE_GLOBAL_QUEUE = "jms.jasper.delegate.global.queue";
     
    private MultiMap<String,String> registeredLicenseKeys;
     
    /*
     * Map will store know connection context information so that we can manipulate the connections, we currently use
     * the context to retrieve the connection and stop JTAs
     */
    private Map<String, ConnectionContext> jtaConnectionContextMap;
     
    /*
     * Map will store known connections of JSBs in cluster to prevent multiple JSBs from using same JSB license key
     */
    private Map<String, ConnectionInfo> jsbConnectionInfoMap;
 
    /*
     * access to the JECore to determine deployment id and access to utilities
     */
    private JECore core;
 
    /*
     * ScheduledExecutorServices for spawning new threads
     */
    private ScheduledExecutorService jtaAuditExec;
	private Connection connection;
	private Session session;
	private MessageProducer producer;
	private Queue globalQ;
	
	private MessageConsumer consumer;
	private Destination adminTopic;
     
    static Logger logger = Logger.getLogger(JasperBroker.class.getName());
     
     public JasperBroker(Broker next) {
        super(next);
        core = JECore.getInstance();
         
        jsbConnectionInfoMap = new ConcurrentHashMap<String, ConnectionInfo>();
        jtaConnectionContextMap = new ConcurrentHashMap<String, ConnectionContext>();
        registeredLicenseKeys = PersistenceFacade.getInstance().getMultiMap("registeredLicenseKeys");
        registeredLicenseKeys.addEntryListener (this, true);
        
        
        
    }
      
     public void start() throws Exception{
         next.start();
         
         // Create a ConnectionFactory
         ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost");

         // Create a Connection
         connectionFactory.setUserName(JASPER_ADMIN_USERNAME);
         connectionFactory.setPassword(JASPER_ADMIN_PASSWORD);
         connection = connectionFactory.createConnection();
         connection.start();

         // Create a Session
         session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

         // Create the destination (Topic or Queue)
         globalQ = session.createQueue(DELEGATE_GLOBAL_QUEUE);

         // Create a MessageProducer from the Session to the Topic or Queue
         producer = session.createProducer(null);
         producer.setDeliveryMode(DeliveryMode.PERSISTENT);
         producer.setTimeToLive(30000);
         
         adminTopic = session.createTopic("jms.jasper." + core.getDeploymentID() + ".admin.topic");
         consumer = session.createConsumer(adminTopic);
         consumer.setMessageListener(this);
         
         setupClientLicenseKeyAudit();
     }
     
     public void stop() throws Exception {
         jtaAuditExec.shutdown();
         consumer.close();
         producer.close();
         session.close();
         connection.close();         
         next.stop();         
     }
      
    private void setupClientLicenseKeyAudit(){
        jtaAuditExec = Executors.newSingleThreadScheduledExecutor();
        Runnable command = new Runnable() {
            public void run() {
                auditRegisteredClients();
            }
        };;;
         
        jtaAuditExec.scheduleAtFixedRate(command , 12, 12, TimeUnit.HOURS);
    }
     
    private void auditRegisteredClients(){
        for(String licenseKey:jtaConnectionContextMap.keySet()){
            ClientLicense lic = core.getClientLicense(JAuthHelper.hexToBytes(licenseKey));
            String clientName = jtaConnectionContextMap.get(licenseKey).getUserName();
            String clientType = lic.getType();
            if(core.willClientLicenseKeyExpireInDays(lic, 3)){
                logger.error(clientType + " : " + clientName + " will expire on : " + core.getExpiryDate(lic));
            }else if(core.willClientLicenseKeyExpireInDays(lic, 7)){
                logger.warn(clientType + " : " + clientName + " will expire on : " + core.getExpiryDate(lic));
            }else if(core.willClientLicenseKeyExpireInDays(lic, 14)){
                logger.info(clientType + " : " + clientName + " will expire on : " + core.getExpiryDate(lic));
            }else if(core.willClientLicenseKeyExpireInDays(lic, 21)){
                logger.debug(clientType + " : " + clientName + " will expire on : " + core.getExpiryDate(lic));
            }
            if(!core.isClientAuthenticationValid(clientName, licenseKey)) dropClientConnection(licenseKey);
        }
    }
      
    private void dropClientConnection(String licenseKey) {
        ConnectionContext context = jtaConnectionContextMap.remove(licenseKey);
        logger.info("attempting to drop connection for Client with license key : " + licenseKey);
        try {
            context.getConnection().stop();
        } catch (Exception e) {
            logger.error("Exception caught when trying to drop Client connection",e);
        }
    }
    
    public void addConnection(ConnectionContext context, ConnectionInfo info) throws Exception {  
        
    	logger.info("=========addConnection Event=========\n");
    	logger.info("Connection INFO" + info); 
        if(info.getClientIp().startsWith("vm://localhost") || info.getClientIp().startsWith("vm://" + context.getBroker().getBrokerName())
                || info.getClientIp().startsWith("tcp://" + core.getBrokerTransportIp()) && core.isUdeLicenseKey(info.getPassword())){
            super.addConnection(context, info);
            logger.info("=========LocalConnection=========\n");
            return;
        }
        logger.info("=========External Connection Event=========");     
        if(!core.isValidLicenseKey(info.getUserName(), info.getPassword())){
            throw (SecurityException)new SecurityException("Invalid license key : " + info.getUserName());
        }
         
        /*
         * If the licenseKey is valid we check to see if the connection being added is for a Client
         * or a Peer UDE and run validation logic depending on which it is, if the validation logic
         * is success 
         */
        if(core.isUdeLicenseKey(info.getPassword())){
        	logger.info("=========Adding UDE=========\n");
        	addUdeConnection(context,info);
        }else{
        	logger.info("=========Adding Client=========\n");
        	addClientConnection(context,info);
        }
    }
    
    private void addUdeConnection(ConnectionContext context, ConnectionInfo info) throws Exception{
    	logger.info("=========Remote UDE=========\n");
    	logger.info("INFO " + info + context+"\n");
       /*
        * During connection setup we validate that the UDE license key provided is valid and
        * matches the stated UDE. UDE info is stored in username and the license key in the password
        */
       if(core.isUdeAuthenticationValid(info.getUserName(), info.getPassword())){
           if(logger.isInfoEnabled()){
               logger.info("Peer UDE authenticated : " + core.getUdeDeploymentAndInstance(info.getPassword()));
           }
       }else{
           logger.error("Invalid Peer UDE license key for : " + core.getUdeDeploymentAndInstance(info.getPassword()));
           throw (SecurityException)new SecurityException("Invalid Peer UDE license key for : " + core.getUdeDeploymentAndInstance(info.getPassword()));
       }
        
       /*
        * Check to see if UDE deploymentId matches that of the system.
        */
       if(core.isSystemDeploymentId(info.getUserName().split(":")[0])){
           if(logger.isInfoEnabled()){
               logger.info("Peer UDE deploymentId matches that of local UDE : " + info.getUserName().split(":")[0]);
           }
       }else{
           logger.error("Peer UDE deploymentId does not match that of local JSB. Peer JSB deploymentId : " + info.getUserName().split(":")[0] + " and local deploymentId : " + core.getDeploymentID());
           throw (SecurityException)new SecurityException("Peer UDE deploymentId does not match that of local JSB. Peer JSB deploymentId : " + info.getUserName().split(":")[0] + " and local deploymentId : " + core.getDeploymentID());
       }
       
       /*
        * Check if UDELicense is unique
        */
       if(core.isThisMyUdeLicense(info.getPassword())){
           logger.error("Peer UDE using same license key as local license key, failing connection setup");
           throw (SecurityException)new SecurityException("Peer UDE using same license key as local license key, failing connection setup");
       }
        
       /*
        * We check that only one UDE instance id ever registers at one time, if a second UDE using the same
        * instance id attempts to register we throw a security exception
        */
       if(!(jsbConnectionInfoMap.containsKey(info.getPassword()))){
           jsbConnectionInfoMap.put(info.getPassword(), info);
           super.addConnection(context, info);
           broadcastAdminEvent("printMap");

           if(logger.isInfoEnabled()){
               logger.info("Peer UDE registered in system : " + core.getUdeDeploymentAndInstance(info.getPassword()));
           }
       }else{
           ConnectionInfo oldJSBInfo = jsbConnectionInfoMap.get(info.getPassword());
           logger.error("Peer UDE not registred in system, UDE instance id must be unique and another peer UDE has registered using same instance id, peer UDE with the following info already registered \n" +
                   "deploymentId:instanceId = " + core.getUdeDeploymentAndInstance(info.getPassword()) + "\n" +
                   "clientId:clientIp = " + oldJSBInfo.getClientId() + ":" + oldJSBInfo.getClientIp());
            
           throw (SecurityException)new SecurityException("Peer UDE not registred in system, UDE instance id must be unique and another peer UDE has registered using same instance id, peer UDE with the following info already registered \n" +
                   "deploymentId:instanceId = " + core.getUdeDeploymentAndInstance(info.getPassword()) + "\n" +
                   "clientId:clientIp = " + oldJSBInfo.getClientId() + ":" + oldJSBInfo.getClientIp());
       }
    }
    
    private void addClientConnection(ConnectionContext context, ConnectionInfo info) throws Exception{
        
    	logger.info("=========Remote Client=========\n");
    	logger.info("INFO " + info + context+"\n");
    	/*
         * During connection setup we validate that the Client license key provided is valid and
         * matches the stated Client. Client info is stored in username and the license key in the password
         */
        if(core.isClientAuthenticationValid(info.getUserName(), info.getPassword())){
            if(logger.isInfoEnabled()){
                    logger.info(info.getUserName() + " authenticated");
            }
        }else if(core.willClientLicenseKeyExpireInDays(core.getClientLicense(JAuthHelper.hexToBytes(info.getPassword())), 0)){
            logger.error("Valid license key, however license has expired : " + info.getUserName());
            throw (SecurityException)new SecurityException("Valid license key, however license has expired : " + info.getUserName());
        }else{
            logger.error("Invalid license key : " + info.getUserName());
            throw (SecurityException)new SecurityException("Invalid license key : " + info.getUserName());
        }
         
        /*
         * Check to see if Client deploymentId matches that of the system.
         */
        if(core.isSystemDeploymentId(info.getUserName().split(":")[3])){
            if(logger.isInfoEnabled()){
                logger.info("license key deploymentId matches that of the system : " + info.getUserName().split(":")[3]);
            }
        }else{
            logger.error("deploymentId does not match that of the system. license key deploymentId : " + info.getUserName().split(":")[3] + " and system deploymentId : " + JECore.getInstance().getDeploymentID());
            throw (SecurityException)new SecurityException("Client deploymentId does not match that of the system. license key deploymentId : " + info.getUserName().split(":")[3] + " and system deploymentId : " + JECore.getInstance().getDeploymentID());
        }
        
        /*
         *Check if license key is already used
         */
        /*
         * check if license key is available, if not it may be due to a race condition
         * in which chase we wait 250 ms and retry, if all attempts fail we conclude
         * the license key is already in use and we fail the registration.
         */
        boolean isLicenseKeyAvailable = false;
        for(int count = 0; count < 5; count++){
        	if(!(registeredLicenseKeys.containsValue(info.getPassword()))){
        		isLicenseKeyAvailable = true;
        		break;
        	}else{
        		String jsb = lookupJsb(info.getPassword());
        		logger.warn("license key is not available it is currently being used by " + jsb);
        		Thread.sleep(250);
        	}
        }
        
        if(isLicenseKeyAvailable){
        	registeredLicenseKeys.put(core.getUdeDeploymentAndInstance(), info.getPassword());
            jtaConnectionContextMap.put(info.getPassword(), context);

            super.addConnection(context, info);
            broadcastAdminEvent("printMap");
            
            if(logger.isInfoEnabled()){
                logger.info(info.getUserName() + " registered") ;
            }
            //TODO Get Type for JSC
            if(!info.getUserName().contains("jsc")) notifyDelegate(Command.jta_connect, info.getUserName());
        }else{
        	String errorMsg = "license key already registered on " + lookupJsb(info.getPassword());
            logger.error(info.getUserName() + " registration failed due to " + errorMsg);
            throw (SecurityException)new SecurityException(errorMsg);
        }
    }

	private String lookupJsb(String licenseKey) {
    	for(String jsb:registeredLicenseKeys.keySet()){
    		if(registeredLicenseKeys.get(jsb).contains(licenseKey))return jsb;
    	}
    	return null;
	}

	public void removeConnection(ConnectionContext context, ConnectionInfo info, Throwable error)throws Exception{

    	String key = info.getPassword();
    	if(jtaConnectionContextMap.containsKey(key)){
    		registeredLicenseKeys.remove(core.getUdeDeploymentAndInstance(), key);
    		ConnectionContext jtaConnectionContext = jtaConnectionContextMap.remove(key);
    		
            if(jtaConnectionContext !=null && !info.getUserName().contains("jsc")) notifyDelegate(Command.jta_disconnect, info.getUserName());     
    		
    		if(logger.isInfoEnabled()) logger.info("connection for " + info.getUserName() + " removed, updated local and remote maps");
    		
    	}else if(jsbConnectionInfoMap.containsKey(key) && !((JasperBrokerService)this.getBrokerService()).isStopping()){
    		jsbConnectionInfoMap.remove(key);    		
        	registeredLicenseKeys.remove(core.getUdeDeploymentAndInstance(key));
        	core.auditMap(core.getUdeDeploymentAndInstance(key));
    		if(logger.isInfoEnabled()) logger.info("connection for UDE " + info.getUserName() + " removed, updated local and remote maps");    		
    	}
        super.removeConnection(context, info, error);
        broadcastAdminEvent("printMap");
    }
    
     
    private void broadcastAdminEvent(String msg) {
    	try {
			producer.send(adminTopic, session.createTextMessage(msg));
		} catch (JMSException e) {
			logger.error("exception caught when trying to broadcast admin event");
		}
	}

	private void notifyDelegate(Command command, String jtaName) {
        try {
            JasperAdminMessage jam = new JasperAdminMessage(Type.ontologyManagement, command, jtaName);
            Message message = session.createObjectMessage(jam);
            producer.send(globalQ, message);
        }
        catch (Exception e) {
            logger.error("Exception caught while notifying peers: ", e);
        }
    }
    
    private String getPrintableRegisteredLicenseKeysMap(){
    	StringBuilder sb = new StringBuilder();
    	sb.append("\n----------------------------------------");

    	sb.append("\njsb peers { ");
    	for(String key:jsbConnectionInfoMap.keySet()){
    		sb.append(core.getUdeDeploymentAndInstance(key));
    		sb.append(" ");
    	}
    	sb.append("}");
    	
    	sb.append("\nregistered clients \n{");
    	
    	int count = 0;
    	
    	for(String jsb:registeredLicenseKeys.keySet()){
    		sb.append("\n\t" + jsb + "{ ");
    		for(String jtaKey:registeredLicenseKeys.get(jsb)){
    			ClientLicense jtaLic = core.getClientLicense(JAuthHelper.hexToBytes(jtaKey));
    			sb.append("\n\t\t" + jtaLic.getVendor());
    			sb.append(": " + jtaLic.getAppName());
    			sb.append(": " + jtaLic.getVersion());
    			sb.append(": " + jtaLic.getDeploymentId());
    		}
    		sb.append("\n\t} total system keys = " + registeredLicenseKeys.get(jsb).size());
    		count += registeredLicenseKeys.get(jsb).size();
    	}
    	sb.append("\n} total cluster keys = " + count);
    	sb.append("\n----------------------------------------");
    	return sb.toString();
    }

	public void entryAdded(EntryEvent event) {
        logger.warn("entryAdded by " + event.getMember() + " value = " + event.getValue().toString().substring(0,15));	
	}

	public void entryEvicted(EntryEvent event) {
        logger.warn("entryEvicted by " + event.getMember() + " value = " + event.getValue().toString().substring(0,15));	
	}

	public void entryRemoved(EntryEvent event) {
        logger.warn("entryRemoved by " + event.getMember() + " value = " + event.getValue().toString().substring(0,15));	
	}

	public void entryUpdated(EntryEvent event) {
        logger.warn("entryUpdated by " + event.getMember() + " value = " + event.getValue().toString().substring(0,15));	
	}

	public void onMessage(Message msg) {
		try {
			if(msg instanceof TextMessage && "printMap".equals(((TextMessage)msg).getText()) ){
		        logger.warn(getPrintableRegisteredLicenseKeysMap());	
			}
		} catch (JMSException e) {
			logger.error("jms exception caught when processing onMessage", e);
		}
	}
}
