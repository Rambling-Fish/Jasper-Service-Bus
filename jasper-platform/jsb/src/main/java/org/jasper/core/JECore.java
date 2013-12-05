package org.jasper.core;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.UnknownHostException;
import java.security.InvalidParameterException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.jms.JMSException;

import org.apache.activemq.broker.BrokerPlugin;
import org.apache.activemq.broker.BrokerRegistry;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.JsbTransportConnector;
import org.apache.activemq.network.NetworkConnector;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.jasper.core.auth.JasperAuthenticationPlugin;
import org.jasper.core.delegate.Delegate;
import org.jasper.core.delegate.DelegateFactory;
import org.jasper.core.persistence.PersistedObject;
import org.jasper.core.persistence.PersistenceFacade;
import org.jasper.core.auth.AuthenticationFacade;
import org.jasper.jLib.jAuth.UDELicense;
import org.jasper.jLib.jAuth.util.JAuthHelper;

public class JECore {
	
	static Logger logger = Logger.getLogger(JECore.class.getName());
	
	private static JECore core;
	
	private BrokerService broker;
	private static UDELicense license;
	private static int numDelegates;
	private static int defaultNumDelegates = 5;	
	private ScheduledExecutorService exec;
	private boolean clusterEnabled;

	private static String brokerTransportIp;
	private static AuthenticationFacade authFacade;
	private static ExecutorService executorService;
	private static DelegateFactory factory;

	private static Delegate[] delegates;

	private JECore(){
		
	}
	
	public static JECore getInstance(){
		if(core == null) core = new JECore();
		return core;
	}
	
	public String getDeploymentID() {
		return (license!=null)?license.getDeploymentId():"licenceKeyNotSet";
	}
     
    public String getUdeDeploymentAndInstance() {
        return (license!=null)?license.getDeploymentId() + ":" + license.getInstanceId():"jasperLab:0";
    }
    
    public boolean isThisMyUdeLicense(String password){
        return JAuthHelper.bytesToHex(license.getLicenseKey()).equals(password);
    }
	
	public UDELicense getUdeLicense(){
		return license;
	}
	

	
	public void setupAudit(){
		exec = Executors.newSingleThreadScheduledExecutor();
		Runnable command = new Runnable() {
			public void run() {
				auditSystem();
			}
		};;;
		
		exec.scheduleAtFixedRate(command , 12, 12, TimeUnit.HOURS);
	}
	
	private void auditSystem(){
		try {
			license = authFacade.loadKeys(System.getProperty("jsb-keystore"));
			if(authFacade.willUdeLicenseKeyExpireInDays(license, 3)){
				logger.error("ude-license key will expire on : " + authFacade.getUdeExpiryDate(license));
			}else if(authFacade.willUdeLicenseKeyExpireInDays(license, 7)){
				logger.warn("ude-license key will expire on : " + authFacade.getUdeExpiryDate(license));
			}else if(authFacade.willUdeLicenseKeyExpireInDays(license, 14)){
				logger.info("ude-license key will expire on : " + authFacade.getUdeExpiryDate(license));
			}else if(authFacade.willUdeLicenseKeyExpireInDays(license, 21)){
				logger.debug("ude-license key will expire on : " + authFacade.getUdeExpiryDate(license));
			}
			if(!authFacade.isValidUdeLicenseKeyExpiry(license))shutdown();
		} catch (IOException e) {
			logger.error("IOException caught when trying to load license key, shutting down",e);
			shutdown();
		} catch (SecurityException e){
			logger.error("SecurityException caught when trying to load license key, shutting down",e);
			shutdown();
		}
	}
	
	public void auditMap(String udeInstance){
		Map<String,PersistedObject> sharedData;
		BlockingQueue<PersistedObject> workQueue;
		sharedData = PersistenceFacade.getInstance().getMap("sharedData");
		workQueue = PersistenceFacade.getInstance().getQueue("tasks");
		PersistedObject statefulData;

		for(String s:sharedData.keySet()){
			statefulData = sharedData.get(s);
			// Only resubmit jobs to queue for UDE that has gone down
			if(statefulData != null && statefulData.getUDEInstance().equals(udeInstance)){
				workQueue.offer(statefulData);
			}
		}
	}


	private void shutdown(){
		logger.info("received shutdown request, shutting down");
		exec.shutdown();
		for(Delegate d:delegates){
			try {
				d.shutdown();
			} catch (JMSException e1) {
				logger.error("jmsconnection caught while shutting down delegates",e1);
			}
		}
		executorService.shutdown();
		PersistenceFacade.getInstance().shutdown();
		
		try {
			broker.stop();
		} catch (Exception e) {
			logger.error("Exception caught during shutdown ", e);
		}
	}
	
	public String getUdeInstance() {
		return license.getDeploymentId() + ":" + license.getInstanceId();
	}


	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
	    
    	Properties prop = new Properties();
    	 
    	try {
            //load a properties file
    		prop.load(new FileInputStream(System.getProperty("jsb-property-file")));
            if(System.getProperty("jsb-log4j-xml") != null) DOMConfigurator.configure(System.getProperty("jsb-log4j-xml"));
    	} catch (IOException ex) {
    		ex.printStackTrace();
    	}
    	
    	JECore core = JECore.getInstance();
    	authFacade = AuthenticationFacade.getInstance();
    	
    	license = authFacade.loadKeys(System.getProperty("jsb-keystore"));
    	
    	if(authFacade.isValidUdeLicenseKey(license)){
    		if(authFacade.isValidUdeLicenseKeyExpiry(license)){
    			
    			core.broker = new JasperBrokerService();
    			String brokerName = core.getUdeLicense().getDeploymentId() + "_" + core.getUdeLicense().getInstanceId();
    			core.broker.setBrokerName(brokerName);
    			BrokerRegistry.getInstance().bind(brokerName, core.broker);
    			BrokerRegistry.getInstance().bind("localhost", core.broker);
    			core.broker.setPersistent(false);
    			
    			try{
    				core.broker.getSystemUsage().getMemoryUsage().setLimit(1024L * 1024 * Long.parseLong(prop.getProperty("memoryLimit","64")));
    				core.broker.getSystemUsage().getStoreUsage().setLimit(1024L * 1024 * Long.parseLong(prop.getProperty("storeLimit","10000")));
    				core.broker.getSystemUsage().getTempUsage().setLimit(1024L * 1024 * Long.parseLong(prop.getProperty("tempLimit","15000")));
    			}catch (NumberFormatException nfe){
    				logger.warn("Error in properties file. One of the following properties not set to valid number: memoryLimit, storeLimit or tempLimit. Setting to 64, 10000 and 15000 respectively");
    				core.broker.getSystemUsage().getMemoryUsage().setLimit(1024L * 1024 * 64);
    				core.broker.getSystemUsage().getStoreUsage().setLimit(1024L * 1024 * 10000);
    				core.broker.getSystemUsage().getTempUsage().setLimit(1024L * 1024 * 15000);
    			}
    			
    			core.broker.setPlugins(new BrokerPlugin[]{new JasperAuthenticationPlugin()});
    			
    			try {
    				numDelegates = Integer.parseInt(prop.getProperty("numDelegates","5"));
    			} catch (NumberFormatException ex) {
    				numDelegates = defaultNumDelegates;
    				logger.warn("Error in properties file. numDelegates = " + prop.getProperty("numDelegates") + ". Using default value of " + defaultNumDelegates);
    			}
    			

				//default value, which will work on both MAC OS and Windows, however for linux machines
				//we need to find the interface with the IPv4 address, we use the default eth0 but this
				//can be overwritten using the property jsbLocalNetworkInterface
    			try{
    				brokerTransportIp = InetAddress.getLocalHost().getHostAddress();
    			}catch(UnknownHostException e){
    				logger.info("unknown host exception caught, expected on linux system.", e);
    			}
    			
				Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
				while (interfaces.hasMoreElements()){
				    NetworkInterface current = interfaces.nextElement();
				    if (!current.isUp() || current.isLoopback() || current.isVirtual()) continue;
				    if(current.getName().equals(prop.getProperty("jsbLocalNetworkInterface", "eth0"))){
				    	Enumeration<InetAddress> addresses = current.getInetAddresses();
    				    while (addresses.hasMoreElements()){
    				        InetAddress current_addr = addresses.nextElement();
    				        if (current_addr.isLoopbackAddress()) continue;
    				        if (current_addr instanceof Inet4Address){
    				        	brokerTransportIp = current_addr.getHostAddress();
    				        	break;
    				        }
    				    }
				    } 
				}
						
    			//clusteredEnable is by default false, only set to false if false 
    			core.clusterEnabled = prop.getProperty("jsbClusterEnabled", "false").equalsIgnoreCase("true");			
    			
    			JsbTransportConnector connector;
    			if(prop.getProperty("jsbLocalURL") != null){
    				if(prop.getProperty("jsbLocalNetworkInterface") == null){
    					logger.error("jsbLocalNetworkInterface must be set when setting jsbLocalURL");
    					throw new InvalidParameterException("jsbLocalNetworkInterface must be set when setting jsbLocalURL");
    				}
    				connector = new JsbTransportConnector(prop.getProperty("jsbLocalURL"));	
    			}else{
    				connector = new JsbTransportConnector("tcp://"+ brokerTransportIp + ":61616??wireFormat.maxInactivityDurationInitalDelay=30000&maximumConnections=1000&wireformat.maxFrameSize=104857600");	
    			}   			
    			
    			if(core.clusterEnabled){
    				NetworkConnector networkConnector = core.broker.addNetworkConnector("multicast://224.1.2.3:6255?group=" + core.getUdeLicense().getDeploymentId());
    				networkConnector.setUserName(core.getUdeLicense().getDeploymentId() + ":" + core.getUdeLicense().getInstanceId());
    				networkConnector.setPassword(JAuthHelper.bytesToHex((core.getUdeLicense().getLicenseKey())));
    				networkConnector.setDecreaseNetworkConsumerPriority(true);
    				connector.setDiscoveryUri(new URI("multicast://224.1.2.3:6255?group=" + core.getUdeLicense().getDeploymentId()));
    				connector.setUpdateClusterClients(true);
    				connector.setUpdateClusterClientsOnRemove(true);
    				connector.setRebalanceClusterClients(true);
    			}
    			    			
    			core.broker.addConnector(connector);
    			core.broker.start();
    			if (!core.broker.waitUntilStarted()) {
    	            throw new Exception(core.broker.getStartException());
    	        }
    			
    			// Instantiate the delegate pool
    			executorService = Executors.newCachedThreadPool();
    			factory = new DelegateFactory(core.clusterEnabled, core);
    			delegates = new Delegate[numDelegates];
    			
    			for(int i=0;i<delegates.length;i++){
    				delegates[i]=factory.createDelegate();
    				executorService.execute(delegates[i]);
    			} 
    			
    			core.setupAudit();
    			Runtime.getRuntime().addShutdownHook(new Thread() {
    	            public void run() {
    	            	JECore.getInstance().shutdown();
    	            }
    	        });
    			
			}else{
    			logger.error("license key expired, UDE not starting"); 
    		}		
    	}else{
			logger.error("invalid license key, UDE not starting"); 
    	}
	}

	public String getBrokerTransportIp() {
		return (brokerTransportIp!=null)?brokerTransportIp:getLocalIP();
	}
	
	private String getLocalIP() {
		String localIP = null;
		try{
			localIP = InetAddress.getLocalHost().getHostAddress();
		}catch(UnknownHostException e){
			logger.info("unknown host exception caught, expected on linux system.", e);
		}
		
		Enumeration<NetworkInterface> interfaces = null;
		try {
			interfaces = NetworkInterface.getNetworkInterfaces();
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		while (interfaces.hasMoreElements()){
		    NetworkInterface current = interfaces.nextElement();
		    try {
				if (!current.isUp() || current.isLoopback() || current.isVirtual()) continue;
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		    if(current.getName().equals("eth0")){
		    	Enumeration<InetAddress> addresses = current.getInetAddresses();
			    while (addresses.hasMoreElements()){
			        InetAddress current_addr = addresses.nextElement();
			        if (current_addr.isLoopbackAddress()) continue;
			        if (current_addr instanceof Inet4Address){
			        	localIP = current_addr.getHostAddress();
			        	break;
			        }
			    }
		    } 
		}
		
		return localIP;
		
	}

	public boolean isClusterEnabled(){
		return clusterEnabled;
	}
	
}
