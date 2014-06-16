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
import org.jasper.core.auth.AuthenticationFacade;
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
    private static final String MAX_CONSUMERS         = "maxconsumers";
    private static final String MAX_PRODUCERS         = "maxproducers";
    private static final String ACTIVE_CONSUMERS      = "activeconsumers";
    private static final String ACTIVE_PRODUCERS      = "activeproducers";
     
    public static final String DELEGATE_GLOBAL_QUEUE = "jms.jasper.delegate.global.queue";
     
    private MultiMap<String,String> registeredLicenseKeys;
    private Map<String,Integer> registeredResources;
    private Map<String,String> dtaResources;
    private int clusterMaxProducers = 0;
    private int clusterMaxConsumers = 0;
    private int currentProducers    = 0;
    private int currentConsumers    = 0;
     
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
     * ScheduledExecutorServices for spawning new threads
     */
    private ScheduledExecutorService jtaAuditExec;
	private Connection connection;
	private Session session;
	private MessageProducer producer;
	private Queue globalQ;
	
	private MessageConsumer consumer;
	private Destination adminTopic;
	private AuthenticationFacade licenseKeySys;
	private UDE ude;
     
    static Logger logger = Logger.getLogger(JasperBroker.class.getName());
     
     public JasperBroker(Broker next, UDE ude, PersistenceFacade cachingSys, AuthenticationFacade licenseKeySys) {
        super(next);
        
        this.ude = ude;
        this.licenseKeySys = licenseKeySys;

        jsbConnectionInfoMap = new ConcurrentHashMap<String, ConnectionInfo>();
        jtaConnectionContextMap = new ConcurrentHashMap<String, ConnectionContext>();
        registeredLicenseKeys = cachingSys.getMultiMap("registeredLicenseKeys");
        registeredLicenseKeys.addEntryListener (this, true);
        registeredResources = cachingSys.getMap("registeredResources");
        dtaResources = cachingSys.getMap("dtaResources");
        
        //initialize license key total consumers and producers map
        calculateClusterResourceTotals();
        
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
         
         adminTopic = session.createTopic("jms.jasper." + ude.getDeploymentID() + ".admin.topic");
         consumer = session.createConsumer(adminTopic);
         consumer.setMessageListener(this);
         
         setupClientLicenseKeyAudit();
     }
     
     public void stop() throws Exception {
         try{
        	 jtaAuditExec.shutdown();
        	 consumer.close();
        	 producer.close();
        	 session.close();
        	 connection.close();         
        	 next.stop();   
         }catch (Exception e){
        	 if(!(e instanceof org.apache.activemq.transport.RequestTimedOutIOException)){
        	 // Fix for JASPER-516 to prevent exception each time UDE is stopped
        		 logger.error("Exception caught while shutting down JasperBroker " + e);
        	 }
         }
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
            ClientLicense lic = licenseKeySys.getClientLicense(JAuthHelper.hexToBytes(licenseKey));
            String clientName = jtaConnectionContextMap.get(licenseKey).getUserName();
            String clientType = lic.getType();
            if(licenseKeySys.willClientLicenseKeyExpireInDays(lic, 3)){
                logger.error(clientType + " : " + clientName + " will expire on : " + licenseKeySys.getClientExpiryDate(lic));
            }else if(licenseKeySys.willClientLicenseKeyExpireInDays(lic, 7)){
                logger.warn(clientType + " : " + clientName + " will expire on : " + licenseKeySys.getClientExpiryDate(lic));
            }else if(licenseKeySys.willClientLicenseKeyExpireInDays(lic, 14)){
                logger.info(clientType + " : " + clientName + " will expire on : " + licenseKeySys.getClientExpiryDate(lic));
            }else if(licenseKeySys.willClientLicenseKeyExpireInDays(lic, 21)){
                logger.debug(clientType + " : " + clientName + " will expire on : " + licenseKeySys.getClientExpiryDate(lic));
            }
            if(!licenseKeySys.isClientAuthenticationValid(clientName, licenseKey)) dropClientConnection(licenseKey);
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
    
        if(info.getClientIp().startsWith("vm://localhost") || info.getClientIp().startsWith("vm://" + context.getBroker().getBrokerName())
                || info.getClientIp().startsWith("tcp://" + ude.getBrokerTransportIp()) && licenseKeySys.isUdeLicenseKey(info.getPassword())){
            super.addConnection(context, info);
            return;
        }
        if(!licenseKeySys.isValidLicenseKey(info.getUserName(), info.getPassword())){
            throw (SecurityException)new SecurityException("Invalid license key : " + info.getUserName());
        }
         
        /*
         * If the licenseKey is valid we check to see if the connection being added is for a Client
         * or a Peer UDE and run validation logic depending on which it is, if the validation logic
         * is success 
         */
        if(licenseKeySys.isUdeLicenseKey(info.getPassword())){
        	addUdeConnection(context,info);
        }else{
        	addClientConnection(context,info);
        }
    }
    
    private void addUdeConnection(ConnectionContext context, ConnectionInfo info) throws Exception{
       /*
        * During connection setup we validate that the UDE license key provided is valid and
        * matches the stated UDE. UDE info is stored in username and the license key in the password
        */
       if(licenseKeySys.isUdeAuthenticationValid(info.getUserName(), info.getPassword())){
           if(logger.isInfoEnabled()){
               logger.info("Peer UDE authenticated : " + licenseKeySys.getUdeDeploymentAndInstance(info.getPassword()));
           }
       }else{
           logger.error("Invalid Peer UDE license key for : " + licenseKeySys.getUdeDeploymentAndInstance(info.getPassword()));
           throw (SecurityException)new SecurityException("Invalid Peer UDE license key for : " + licenseKeySys.getUdeDeploymentAndInstance(info.getPassword()));
       }
        
       /*
        * Check to see if UDE deploymentId matches that of the system.
        */
       if(licenseKeySys.isSystemDeploymentId(info.getUserName().split(":")[0])){
           if(logger.isInfoEnabled()){
               logger.info("Peer UDE deploymentId matches that of local UDE : " + info.getUserName().split(":")[0]);
           }
       }else{
           logger.error("Peer UDE deploymentId does not match that of local UDE. Peer UDE deploymentId : " + info.getUserName().split(":")[0] + " and local deploymentId : " + ude.getDeploymentID());
           throw (SecurityException)new SecurityException("Peer UDE deploymentId does not match that of local UDE. Peer UDE deploymentId : " + info.getUserName().split(":")[0] + " and local deploymentId : " + ude.getDeploymentID());
       }
       
       /*
        * Check if UDELicense is unique
        */
       if(ude.isThisMyUdeLicense(info.getPassword())){
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
           calculateClusterResourceTotals();
           broadcastAdminEvent("printMap");

           if(logger.isInfoEnabled()){
               logger.info("Peer UDE registered in system : " + licenseKeySys.getUdeDeploymentAndInstance(info.getPassword()));
           }
       }else{
           ConnectionInfo oldJSBInfo = jsbConnectionInfoMap.get(info.getPassword());
           logger.error("Peer UDE not registered in system, UDE instance id must be unique and another peer UDE has registered using same instance id, peer UDE with the following info already registered \n" +
                   "deploymentId:instanceId = " + licenseKeySys.getUdeDeploymentAndInstance(info.getPassword()) + "\n" +
                   "clientId:clientIp = " + oldJSBInfo.getClientId() + ":" + oldJSBInfo.getClientIp());
            
           throw (SecurityException)new SecurityException("Peer UDE not registred in system, UDE instance id must be unique and another peer UDE has registered using same instance id, peer UDE with the following info already registered \n" +
                   "deploymentId:instanceId = " + licenseKeySys.getUdeDeploymentAndInstance(info.getPassword()) + "\n" +
                   "clientId:clientIp = " + oldJSBInfo.getClientId() + ":" + oldJSBInfo.getClientIp());
       }
    }
    
    private void addClientConnection(ConnectionContext context, ConnectionInfo info) throws Exception{
        
    	/*
         * During connection setup we validate that the Client license key provided is valid and
         * matches the stated Client. Client info is stored in username and the license key in the password
         */
        if(licenseKeySys.isClientAuthenticationValid(info.getUserName(), info.getPassword())){
            if(logger.isInfoEnabled()){
                    logger.info(info.getUserName() + " authenticated");
            }
        }else if(licenseKeySys.willClientLicenseKeyExpireInDays(licenseKeySys.getClientLicense(JAuthHelper.hexToBytes(info.getPassword())), 0)){
            logger.error("Valid license key, however license has expired : " + info.getUserName());
            throw (SecurityException)new SecurityException("Valid license key, however license has expired : " + info.getUserName());
        }else{
            logger.error("Invalid license key : " + info.getUserName());
            throw (SecurityException)new SecurityException("Invalid license key : " + info.getUserName());
        }
         
        /*
         * Check to see if Client deploymentId matches that of the system.
         */
        if(licenseKeySys.isSystemDeploymentId(info.getUserName().split(":")[3])){
            if(logger.isInfoEnabled()){
                logger.info("license key deploymentId matches that of the system : " + info.getUserName().split(":")[3]);
            }
        }else{
            logger.error("deploymentId does not match that of the system. license key deploymentId : " + info.getUserName().split(":")[3] + " and system deploymentId : " + licenseKeySys.getDeploymentId());
            throw (SecurityException)new SecurityException("Client deploymentId does not match that of the system. license key deploymentId : " + info.getUserName().split(":")[3] + " and system deploymentId : " + licenseKeySys.getDeploymentId());
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
        String key = info.getPassword();
        for(int count = 0; count < 5; count++){
        	if(!(registeredLicenseKeys.containsValue(key))){
        		isLicenseKeyAvailable = updateResources(key, true);
        		break;
        	}else{
        		String ude = lookupUde(key);
        		logger.warn("license key is not available it is currently being used by " + ude);
        		Thread.sleep(250);
        	}
        }
        
        if(isLicenseKeyAvailable){
        	synchronized(registeredLicenseKeys){
        		registeredLicenseKeys.put(ude.getUdeDeploymentAndInstance(), key);
        	}
        	jtaConnectionContextMap.put(key, context);        
            super.addConnection(context, info);
            broadcastAdminEvent("printMap");
            
            if(logger.isInfoEnabled()){
                logger.info(info.getUserName() + " registered");
            }
            
            String adminQ = licenseKeySys.getClientAdminQueue(info.getPassword());
            if(adminQ != null){
            	String instanceId = licenseKeySys.getClientInstanceId(key).toString();
            	notifyDelegate(Command.jta_connect, info.getUserName().concat(":").concat(instanceId),adminQ);
            }
            
        }else{
        	String errorMsg = "license key already registered on " + lookupUde(key);
            logger.error(info.getUserName() + " registration failed due to " + errorMsg);
            throw (SecurityException)new SecurityException(errorMsg);
        }
    }

	private String lookupUde(String licenseKey) {
		synchronized(registeredLicenseKeys){
			for(String ude:registeredLicenseKeys.keySet()){
				if(registeredLicenseKeys.get(ude).contains(licenseKey))return ude;
			}
			return null;
		}
	}
	
	private void calculateClusterResourceTotals(){
		
		// Get consumers and producers for this (local) UDE
		int localConsumers = ude.getUdeLicense().getNumOfConsumers();
		int localProducers = ude.getUdeLicense().getNumOfPublishers();
		
		// Add producers and consumers from remote UDEs
		for(String key:jsbConnectionInfoMap.keySet()){
    		int remoteConsumers = licenseKeySys.getUdeNumConsumers(key);
    		int remoteProducers = licenseKeySys.getUdeNumPublishers(key);
    		localConsumers += remoteConsumers;
    		localProducers += remoteProducers;
		}
		
    	if(localConsumers > clusterMaxConsumers){
    		clusterMaxConsumers = localConsumers;
    	}
    	if(localProducers > clusterMaxProducers){
    		clusterMaxProducers = localProducers;
    	}
    	if(localProducers < 0){
    		clusterMaxProducers = -1;
    	}
	
    	synchronized(registeredResources){
    		if(registeredResources.size() > 0){
    			currentConsumers = registeredResources.get(ACTIVE_CONSUMERS);
    			currentProducers = registeredResources.get(ACTIVE_PRODUCERS);
    		}
    		registeredResources.put(MAX_CONSUMERS, clusterMaxConsumers);
    		registeredResources.put(MAX_PRODUCERS, clusterMaxProducers);
    		registeredResources.put(ACTIVE_CONSUMERS, currentConsumers);
        	registeredResources.put(ACTIVE_PRODUCERS, currentProducers);
    	}
	}
	
	private boolean updateResources(String password, boolean addConnection){
		boolean enforceProducerLimit = true;
		int dtaConsumers = licenseKeySys.getClientNumConsumers(password);
		int dtaProducers = licenseKeySys.getClientNumPublishers(password);
		
		if(addConnection){
			if(dtaResources.containsKey(password)) return true; // already registered - this is a fail over
			synchronized(registeredResources){
				currentConsumers = registeredResources.get(ACTIVE_CONSUMERS);
				currentProducers = registeredResources.get(ACTIVE_PRODUCERS);
				clusterMaxConsumers = registeredResources.get(MAX_CONSUMERS);
				clusterMaxProducers = registeredResources.get(MAX_PRODUCERS);
				currentProducers = (currentProducers + dtaProducers);
			
				if(clusterMaxProducers > -1){ // enforce limits if > -1 since -1 indicates unlimited producers
					if(currentProducers <= clusterMaxProducers){
						registeredResources.put(ACTIVE_PRODUCERS, currentProducers);
					}
					else{
						logger.error("Cannot add connection as producer limit for cluster has been reached");
						return false;
					}
				}
				else{ //we update total producers but do not enforce limit
					enforceProducerLimit = false;
				}
			
				currentConsumers = (currentConsumers + dtaConsumers);
				if(currentConsumers <= clusterMaxConsumers){
					registeredResources.put(ACTIVE_CONSUMERS, currentConsumers);
				}
				else{
					logger.error("Cannot add connection as consumer limit for cluster has been reached");
					return false;
				}
				if(!enforceProducerLimit){
					registeredResources.put(ACTIVE_PRODUCERS, currentProducers);
				}
			}
			
			synchronized(dtaResources){
				dtaResources.put(password, "registered");
			}
			
			return true;
		}
		else{ // removing a DTA connection
			if(dtaResources.containsKey(password)){
				synchronized(registeredResources){
					currentConsumers = registeredResources.get(ACTIVE_CONSUMERS);
					currentProducers = registeredResources.get(ACTIVE_PRODUCERS);
					currentConsumers = (currentConsumers - dtaConsumers);
					currentProducers = (currentProducers - dtaProducers);
					registeredResources.put(ACTIVE_PRODUCERS, currentProducers);
					registeredResources.put(ACTIVE_CONSUMERS, currentConsumers);
				}
		
				synchronized(dtaResources){
					dtaResources.remove(password);
				}
			}
			
			return true;
			
		}			
	}

	public void removeConnection(ConnectionContext context, ConnectionInfo info, Throwable error)throws Exception{
    	String key = info.getPassword();

    	if(jtaConnectionContextMap.containsKey(key)){
        	synchronized(registeredLicenseKeys){
        		registeredLicenseKeys.remove(ude.getUdeDeploymentAndInstance(), key);
        	}
    		ConnectionContext jtaConnectionContext = jtaConnectionContextMap.remove(key);
    		updateResources(key, false);

            String adminQ = licenseKeySys.getClientAdminQueue(info.getPassword());
            if(adminQ != null){
            	String instanceId = licenseKeySys.getClientInstanceId(key).toString();
            	notifyDelegate(Command.jta_disconnect, info.getUserName().concat(":").concat(instanceId),adminQ); 
        	}
    		    		
    		if(logger.isInfoEnabled()) logger.info("connection for " + info.getUserName() + " removed, updated local and remote maps");
    		
    	}else if(jsbConnectionInfoMap.containsKey(key) && !((JasperBrokerService)this.getBrokerService()).isStopping()){
    		jsbConnectionInfoMap.remove(key);
        	synchronized(registeredLicenseKeys){
        		registeredLicenseKeys.remove(licenseKeySys.getUdeDeploymentAndInstance(key));
        	}
        	ude.remoteUdeConnectionDropped(info.getClientId(), info.getClientIp(), info.getUserName(), info.getPassword());
    		if(logger.isInfoEnabled()) logger.info("connection for UDE " + info.getUserName() + " removed, updated local and remote maps");    		
    	}
        super.removeConnection(context, info, error);
        broadcastAdminEvent("printMap");
    }
    
     
    private void broadcastAdminEvent(String msg) {
    	try {
			producer.send(adminTopic, session.createTextMessage(msg));
		} catch (JMSException e) {
			logger.error("Exception caught when trying to broadcast admin event");
		}
	}

	private void notifyDelegate(Command command, String jtaName, String adminQ) {
        try {
            JasperAdminMessage jam = new JasperAdminMessage(Type.ontologyManagement, command, jtaName, adminQ);
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
    		sb.append(licenseKeySys.getUdeDeploymentAndInstance(key));
    		sb.append(" ");
    	}
    	sb.append("}");
    	
    	sb.append("\nregistered clients \n{");
    	
    	int count = 0;
    	
    	synchronized(registeredLicenseKeys){
	    	for(String jsb:registeredLicenseKeys.keySet()){
	    		sb.append("\n\t" + jsb + "{ ");
	    		for(String jtaKey:registeredLicenseKeys.get(jsb)){
	    			ClientLicense jtaLic = licenseKeySys.getClientLicense(JAuthHelper.hexToBytes(jtaKey));
	    			sb.append("\n\t\t" + jtaLic.getVendor());
	    			sb.append(": " + jtaLic.getAppName());
	    			sb.append(": " + jtaLic.getinstanceId());
	    			sb.append(": " + jtaLic.getVersion());
	    			sb.append(": " + jtaLic.getDeploymentId());
	    		}
	    		sb.append("\n\t} total system keys = " + registeredLicenseKeys.get(jsb).size());
	    		count += registeredLicenseKeys.get(jsb).size();
	    	}
    	}
    	
    	Integer totalProducers = new Integer(-1);
    	int activeProducers = 0;
    	int activeConsumers = 0;
    	int maxConsumers = 0;
    	String producersString = "Unlimited";
    	
    	sb.append("\nregistered resources");
    	
    	synchronized(registeredResources){
    		activeProducers = registeredResources.get(ACTIVE_PRODUCERS);
    		activeConsumers = registeredResources.get(ACTIVE_CONSUMERS);
    		totalProducers  = registeredResources.get(MAX_PRODUCERS);
    		maxConsumers    = registeredResources.get(MAX_CONSUMERS);
    	}

    	if(totalProducers != -1){
    		producersString = totalProducers.toString();
    	}
    	
    	sb.append("\n\t{");
    	sb.append("\n\tNumber of Producers: " + activeProducers);
    	sb.append("\n\tNumber of Consumers: " + activeConsumers);
    	sb.append("\n\tTotal Producers allowed: " + producersString);
    	sb.append("\n\tTotal Consumers allowed: " + maxConsumers);
    	sb.append("\n\t}");
    	
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
		} catch (Exception e) {
			if(!(e instanceof com.hazelcast.core.HazelcastInstanceNotActiveException)){
				logger.error("jms exception caught when processing onMessage", e);
			}
		}
	}
}
