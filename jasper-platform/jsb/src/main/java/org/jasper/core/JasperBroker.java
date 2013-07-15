package org.jasper.core;
 
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.Broker;
import org.apache.activemq.broker.BrokerFilter;
import org.apache.activemq.broker.ConnectionContext;
import org.apache.activemq.command.ConnectionInfo;
import org.apache.log4j.Logger;
import org.jasper.core.constants.JasperConstants;
import org.jasper.core.constants.JtaInfo;
import org.jasper.jLib.jAuth.JTALicense;
import org.jasper.jLib.jAuth.util.JAuthHelper;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Command;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Type;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ItemEvent;
import com.hazelcast.core.ItemListener;
 
public class JasperBroker extends BrokerFilter implements ItemListener, EntryListener {
     
    private static final String JASPER_ADMIN_USERNAME = "jasperAdminUsername";
    private static final String JASPER_ADMIN_PASSWORD = "jasperAdminPassword";
     
    public static final String DELEGATE_GLOBAL_QUEUE = "jms.jasper.delegate.global.queue";
     
    private Map<String,JtaInfo> jtaInfoMap;
     
    /*
     * Map will store known connections to prevent multiple JTAs from using same JTA license key
     */
//  private Map<String, ConnectionInfo> jtaConnectionInfoMap;
     
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
     
    static Logger logger = Logger.getLogger(JasperBroker.class.getName());
     
     public JasperBroker(Broker next) {
        super(next);
        core = JECore.getInstance();
         
        if(core.isClusterEnabled()){      
            jtaInfoMap = core.getHazelcastInstance().getMap("jtaInfoMap");
            ((IMap<String, JtaInfo>) jtaInfoMap).addEntryListener (this, true);
        }else{
            jtaInfoMap = new ConcurrentHashMap<String, JtaInfo>();
        }
         
        jsbConnectionInfoMap = new ConcurrentHashMap<String, ConnectionInfo>();
        jtaConnectionContextMap = new ConcurrentHashMap<String, ConnectionContext>();
    }
      
     public void start() throws Exception{
         next.start();
         setupJTALicenseKeyAudit();
     }
     
     public void stop() throws Exception {
         next.stop();
         jtaAuditExec.shutdown();
     }
      
    private void setupJTALicenseKeyAudit(){
        jtaAuditExec = Executors.newSingleThreadScheduledExecutor();
        Runnable command = new Runnable() {
            @Override
            public void run() {
                auditRegisteredJTAs();
            }
        };;;
         
        jtaAuditExec.scheduleAtFixedRate(command , 12, 12, TimeUnit.HOURS);
    }
     
    private void auditRegisteredJTAs(){
        for(String licenseKey:jtaInfoMap.keySet()){
            JTALicense lic = core.getJTALicense(JAuthHelper.hexToBytes(licenseKey));
            if(core.willLicenseKeyExpireInDays(lic, 3)){
                logger.error("JTA : " + jtaInfoMap.get(licenseKey).getJtaName() + " will expire on : " + core.getExpiryDate(lic));
            }else if(core.willLicenseKeyExpireInDays(lic, 7)){
                logger.warn("JTA : " + jtaInfoMap.get(licenseKey).getJtaName() + " will expire on : " + core.getExpiryDate(lic));
            }else if(core.willLicenseKeyExpireInDays(lic, 14)){
                logger.info("JTA : " + jtaInfoMap.get(licenseKey).getJtaName() + " will expire on : " + core.getExpiryDate(lic));
            }else if(core.willLicenseKeyExpireInDays(lic, 21)){
                logger.debug("JTA : " + jtaInfoMap.get(licenseKey).getJtaName() + " will expire on : " + core.getExpiryDate(lic));
            }
            if(!core.isJTAAuthenticationValid(jtaInfoMap.get(licenseKey).getJtaName(), licenseKey)) dropJTAConnection(licenseKey);
        }
    }
      
    private void dropJTAConnection(String licenseKey) {
        ConnectionContext context = jtaConnectionContextMap.remove(licenseKey);
        logger.info("attempting to drop connection for JTA with license key : " + licenseKey);
        try {
            context.getConnection().stop();
        } catch (Exception e) {
            logger.error("Exception caught when trying to drop JTA connection",e);
        }
    }
 
    private void cleanRemoteJtaMap(String jsbInstance) {
    	if(logger.isInfoEnabled()) logger.info("removing all JTA license keys from remote map for jsb : " + jsbInstance);
        JtaInfo jtaInfo;
        for(String key:jtaInfoMap.keySet()){
            jtaInfo = jtaInfoMap.get(key);
            if(jtaInfo.getJsbConnectedTo().equals(jsbInstance)){
            	if(logger.isInfoEnabled()) logger.info("removing : " + jtaInfo.getJtaName() + " from : " + jtaInfo.getJsbConnectedTo());
                jtaInfoMap.remove(key);
                jtaConnectionContextMap.remove(key);
            }
        }
    }
    
    public void addConnection(ConnectionContext context, ConnectionInfo info) throws Exception {  
         
        if(info.getClientIp().startsWith("vm://localhost") || info.getClientIp().startsWith("vm://" + context.getBroker().getBrokerName())
                || info.getClientIp().startsWith("tcp://" + core.getBrokerTransportIp()) && core.isJSBLicenseKey(info.getPassword())){
            super.addConnection(context, info); 
            return;
        }
             
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
                    logger.info("Peer JSB authenticated : " + core.getJSBDeploymentAndInstance(info.getPassword()));
                }
            }else{
                logger.error("Invalid Peer JSB license key for : " + core.getJSBDeploymentAndInstance(info.getPassword()));
                throw (SecurityException)new SecurityException("Invalid Peer JSB license key for : " + core.getJSBDeploymentAndInstance(info.getPassword()));
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
             * Check if JSBLicense is unique
             */
            if(core.isThisMyJSBLicense(info.getPassword())){
                logger.error("Peer JSB using same license key as local license key, failing connection setup");
                throw (SecurityException)new SecurityException("Peer JSB using same license key as local license key, failing connection setup");
            }
             
            /*
             * We check that only one JSB instance id ever registers at one time, if a second JSB using the same
             * instance id attempts to register we throw a security exception
             */
            if(!(jsbConnectionInfoMap.containsKey(info.getPassword()))){
                jsbConnectionInfoMap.put(info.getPassword(), info);
                super.addConnection(context, info);
                logger.warn(getPrintableJtaAndJsbMap());
                if(logger.isInfoEnabled()){
                    logger.info("Peer JSB registered in system : " + core.getJSBDeploymentAndInstance(info.getPassword()));
                }
            }else{
                ConnectionInfo oldJSBInfo = jsbConnectionInfoMap.get(info.getPassword());
                logger.error("Peer JSB not registred in system, JSB instance id must be unique and another peer JSB has registered using same instance id, peer JSB with the following info already registered \n" +
                        "deploymentId:instanceId = " + core.getJSBDeploymentAndInstance(info.getPassword()) + "\n" +
                        "clientId:clientIp = " + oldJSBInfo.getClientId() + ":" + oldJSBInfo.getClientIp());
                 
                throw (SecurityException)new SecurityException("Peer JSB not registred in system, JSB instance id must be unique and another peer JSB has registered using same instance id, peer JSB with the following info already registered \n" +
                        "deploymentId:instanceId = " + core.getJSBDeploymentAndInstance(info.getPassword()) + "\n" +
                        "clientId:clientIp = " + oldJSBInfo.getClientId() + ":" + oldJSBInfo.getClientIp());
            }	
        }else{
            /*
             * During connection setup we validate that the JTA license key provided is valid and
             * matches the stated JTA. JTA info is stored in username and the license key in the password
             */
            if(core.isJTAAuthenticationValid(info.getUserName(), info.getPassword())){
                if(logger.isInfoEnabled()){
                    if(info.getUserName().contains("jsc")) 
                        logger.info("JSC authenticated : " + info.getUserName());
                    else
                        logger.info("JTA authenticated : " + info.getUserName());
                }
            }else if(core.willLicenseKeyExpireInDays(core.getJTALicense(JAuthHelper.hexToBytes(info.getPassword())), 0)){
                logger.error("Valid JTA license key, however it has expired : " + info.getUserName());
                throw (SecurityException)new SecurityException("Invalid JTA license key : " + info.getUserName());
            }else{
                logger.error("Invalid JTA license key : " + info.getUserName());
                throw (SecurityException)new SecurityException("Invalid JTA license key : " + info.getUserName());
            }
             
            /*
             * Check to see if JTA deploymentId matches that of the system.
             */
            if(core.isSystemDeploymentId(info.getUserName().split(":")[3])){
                if(logger.isInfoEnabled()){
                    if(info.getUserName().contains("jsc"))
                        logger.info("JSC deploymentId matches that of the system : " + info.getUserName().split(":")[3]);
                    else
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
            if(!(jtaInfoMap.containsKey(info.getPassword()))){
                
                if(jtaInfoMap instanceof IMap)((IMap)jtaInfoMap).lockMap(30, TimeUnit.SECONDS);
                super.addConnection(context, info); 
            	jtaInfoMap.put(info.getPassword(), new JtaInfo(info.getUserName(), info.getPassword(), core.getJSBDeploymentAndInstance(),info.getClientId(), info.getClientIp()));
                jtaConnectionContextMap.put(info.getPassword(), context);

                if(logger.isInfoEnabled()){
	                if(info.getUserName().contains("jsc")){
	                    logger.info("JSC registered on JSB : " + info.getUserName());
	                }else{
	                    logger.info("JTA registered on JSB : " + info.getUserName());
	                }
                }
                
                String vendor = info.getUserName().split(":")[0];
                String appName = info.getUserName().split(":")[1];
                String version = info.getUserName().split(":")[2];
                String jtaQueueName = JasperConstants.JTA_QUEUE_PREFIX.concat(vendor).concat(".").concat(appName).concat(".").concat(version).concat(JasperConstants.DELEGATE_QUEUE_SUFFIX);
                notifyDelegate(Command.publish, jtaQueueName);
                if(!core.isClusterEnabled())logger.warn(getPrintableJtaMap());
                if(jtaInfoMap instanceof IMap)((IMap)jtaInfoMap).unlockMap();
                               
            }else{
                JtaInfo registeredJtaInfo = jtaInfoMap.get(info.getPassword());
                logger.error("JTA not registred since JTA with same license key registered on " + registeredJtaInfo.getJsbConnectedTo() + ", only one instance of a JTA can be registered with core at a time, JTA with with the following info already registered \n" +
                        "vendor:appName:version:deploymentId = " + registeredJtaInfo.getJtaName() + "\n" +
                        "clientId:clientIp = " + registeredJtaInfo.getClientId() + ":" + registeredJtaInfo.getClientIp());
                throw (SecurityException)new SecurityException("JTA not registred since JTA with same license key registered on " + registeredJtaInfo.getJsbConnectedTo() + ", only one instance of a JTA can be registered with core at a time, JTA with with the following info already registered \n" +
                        "vendor:appName:version:deploymentId = " + registeredJtaInfo.getJtaName() + "\n" +
                        "clientId:clientIp = " + registeredJtaInfo.getClientId() + ":" + registeredJtaInfo.getClientIp());
            }
            
        }
    }
     
    public void removeConnection(ConnectionContext context, ConnectionInfo info, Throwable error)throws Exception{
        if(jtaInfoMap.get(info.getPassword()) != null ){

        	if(logger.isInfoEnabled()){
	        	if(info.getUserName().contains("jsc")){
	                logger.info("JSC de-registered from JSB : " + info.getUserName());
	        	}else{
	                logger.info("JTA de-registered from JSB : " + info.getUserName());
	        	}
        	}
            if(jtaInfoMap instanceof IMap)((IMap)jtaInfoMap).lockMap(30, TimeUnit.SECONDS);
            jtaInfoMap.remove(info.getPassword());
            jtaConnectionContextMap.remove(info.getPassword());
            super.removeConnection(context, info, error);

            if(!core.isClusterEnabled())logger.warn(getPrintableJtaMap());
 
            // TODO right now we are just sending message to delegates so the jta
            // map can be cleaned up. This needs to change to listener pattern
            notifyDelegate(Command.delete, info.getUserName());
            if(jtaInfoMap instanceof IMap)((IMap)jtaInfoMap).unlockMap();
            return;
        }else if(jsbConnectionInfoMap.get(info.getPassword()) != null){
            if(logger.isInfoEnabled()){
                logger.info("Peer JSB deregistered from system : " + info.getUserName());
            }
            if(jtaInfoMap instanceof IMap)((IMap)jtaInfoMap).lockMap(30, TimeUnit.SECONDS);
            jsbConnectionInfoMap.remove(info.getPassword());
            cleanRemoteJtaMap(core.getJSBDeploymentAndInstance(info.getPassword()));
            super.removeConnection(context, info, error);
            logger.warn(getPrintableJtaAndJsbMap());
            if(jtaInfoMap instanceof IMap)((IMap)jtaInfoMap).unlockMap();
            return;
        }
        
        if(logger.isDebugEnabled()){
        	logger.debug("Connection removed that is neither JSB nor JTA, info.getUserName() = " + info.getUserName());
        }
        super.removeConnection(context, info, error);
    }
     
    private String getPrintableJtaMap() {
        if(jtaInfoMap instanceof IMap)((IMap)jtaInfoMap).lockMap(30, TimeUnit.SECONDS);
        JtaInfo jtaInfo;
        StringBuilder sb = new StringBuilder();
 
        sb.append("\n-----  JTA dist map start ----\n");
        for(String key:jtaInfoMap.keySet()){
            jtaInfo = jtaInfoMap.get(key);
            if(jtaInfo.getJtaName().contains("jsc")){
                sb.append("jsc = " + jtaInfo.getJtaName() + " - connected to jsb = " + jtaInfo.getJsbConnectedTo() + "\n");
            }else{
            	sb.append("jta = " + jtaInfo.getJtaName() + " - connected to jsb = " + jtaInfo.getJsbConnectedTo() + "\n");
            }
        }
        if(jtaInfoMap instanceof IMap)((IMap)jtaInfoMap).unlockMap();
        sb.append("-----  JTA dist map  end  ----\n");
        return sb.toString();
    }
 
    private String getPrintableJsbMap() {
        ConnectionInfo jsbInfo;
        StringBuilder sb = new StringBuilder();
        sb.append("\n-----  JSB local map start ----\n");
        for(String key:jsbConnectionInfoMap.keySet()){
            jsbInfo = jsbConnectionInfoMap.get(key);
            sb.append("jsb = " + jsbInfo.getUserName() + " - with IP = " + jsbInfo.getClientIp() + "\n");
        }
        sb.append("-----  JSB local map  end  ----");
        return sb.toString();
    }
    
    private String getPrintableJtaAndJsbMap() {
        StringBuilder sb = new StringBuilder();
        sb.append(getPrintableJsbMap());
        sb.append(getPrintableJtaMap());
        return sb.toString();
    }
    
     
    private void notifyDelegate(Command command, String msgDetails) {
        try {
            // Create a ConnectionFactory
            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost");
 
            // Create a Connection
            connectionFactory.setUserName(JASPER_ADMIN_USERNAME);
            connectionFactory.setPassword(JASPER_ADMIN_PASSWORD);
            Connection connection = connectionFactory.createConnection();
            connection.start();
 
            // Create a Session
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
 
            // Create the destination (Topic or Queue)
            Destination destination = session.createQueue(DELEGATE_GLOBAL_QUEUE);
 
            // Create a MessageProducer from the Session to the Topic or Queue
            MessageProducer producer = session.createProducer(destination);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            producer.setTimeToLive(30000);
 
            JasperAdminMessage jam = new JasperAdminMessage(Type.jtaDataManagement, command, msgDetails, "*",  msgDetails);
 
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

	@Override
	public void entryAdded(EntryEvent arg0) {
        logger.warn(getPrintableJtaMap());	
	}

	@Override
	public void entryEvicted(EntryEvent arg0) {
        logger.warn(getPrintableJtaMap());	
		
	}

	@Override
	public void entryRemoved(EntryEvent arg0) {
        logger.warn(getPrintableJtaMap());	
	}

	@Override
	public void entryUpdated(EntryEvent arg0) {
        logger.warn(getPrintableJtaMap());	
	}

	@Override
	public void itemAdded(ItemEvent arg0) {
        logger.warn(getPrintableJtaMap());	
	}

	@Override
	public void itemRemoved(ItemEvent arg0) {
        logger.warn(getPrintableJtaMap());	
	}
     
     
}