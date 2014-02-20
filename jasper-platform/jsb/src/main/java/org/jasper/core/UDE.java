package org.jasper.core;

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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.broker.BrokerPlugin;
import org.apache.activemq.broker.BrokerRegistry;
import org.apache.activemq.broker.JsbTransportConnector;
import org.apache.activemq.network.NetworkConnector;
import org.apache.log4j.Logger;
import org.jasper.core.auth.AuthenticationFacade;
import org.jasper.core.auth.JasperAuthenticationPlugin;
import org.jasper.core.persistence.PersistedObject;
import org.jasper.core.persistence.PersistenceFacade;
import org.jasper.core.persistence.PersistenceFacadeFactory;
import org.jasper.jLib.jAuth.UDELicense;
import org.jasper.jLib.jAuth.util.JAuthHelper;

public class UDE {

	static Logger logger = Logger.getLogger(UDE.class.getName());

	private Properties prop;
	
	private JasperBrokerService broker;
	private AuthenticationFacade licenseKeySys;
	private PersistenceFacade cachingSys;
	
	private UDECore udeCore;
	private UDELicense license;
	

	private ScheduledExecutorService exec;
	private boolean clusterEnabled;
	
	private String brokerTransportIp;

	
	public UDE(Properties prop){
		this.prop = prop;
		
		//initializing license key system
    	licenseKeySys = AuthenticationFacade.getInstance();
		try {
			license = licenseKeySys.loadKeys(prop.getProperty("jsb-keystore"));
		} catch (IOException e) {
			logger.error("unalble to load license key",e);
		}
		
		//initializing and starting caching layer
		String localIP = prop.getProperty("persisitence.localIp", getBrokerTransportIp());
		String groupName = prop.getProperty("persisitence.groupName", license.getDeploymentId());
		String groupPassword = prop.getProperty("persisitence.groupPassword", license.getDeploymentId() + "_password_dec_18_2013_1108");
		this.cachingSys = PersistenceFacadeFactory.getFacade(localIP, groupName, groupPassword);
		
		this.udeCore = new UDECore(this,prop);
	}
	
	public void start(){
    	try {
	    	if(licenseKeySys.isValidUdeLicenseKey(license)){
	    		if(licenseKeySys.isValidUdeLicenseKeyExpiry(license)){
	    			
	    			startBroker();
	    			udeCore.start();
	    			setupAudit();
	    	
				}else{
	    			logger.error("license key expired, UDE not starting"); 
	    		}		
	    	}else{
				logger.error("invalid license key, UDE not starting"); 
	    	}
		} catch (Exception e) {
			logger.error("exception caught when starting UDE, shutting down : ",e);
			stop();
		}
	}

	private void startBroker() throws Exception {
		
		broker = new JasperBrokerService();
		String brokerName = license.getDeploymentId() + "_" + license.getInstanceId();
		broker.setBrokerName(brokerName);
		BrokerRegistry.getInstance().bind(brokerName, broker);
		BrokerRegistry.getInstance().bind("localhost", broker);
		broker.setPersistent(false);
		
		try{
			broker.getSystemUsage().getMemoryUsage().setLimit(1024L * 1024 * Long.parseLong(prop.getProperty("memoryLimit","64")));
			broker.getSystemUsage().getStoreUsage().setLimit(1024L * 1024 * Long.parseLong(prop.getProperty("storeLimit","10000")));
			broker.getSystemUsage().getTempUsage().setLimit(1024L * 1024 * Long.parseLong(prop.getProperty("tempLimit","15000")));
		}catch (NumberFormatException nfe){
			logger.warn("Error in properties file. One of the following properties not set to valid number: memoryLimit, storeLimit or tempLimit. Setting to 64, 10000 and 15000 respectively");
			broker.getSystemUsage().getMemoryUsage().setLimit(1024L * 1024 * 64);
			broker.getSystemUsage().getStoreUsage().setLimit(1024L * 1024 * 10000);
			broker.getSystemUsage().getTempUsage().setLimit(1024L * 1024 * 15000);
		}
		
		broker.setPlugins(new BrokerPlugin[]{new JasperAuthenticationPlugin(this, cachingSys, licenseKeySys)});
				
		//clusteredEnable is by default false, only set to false if false 
		clusterEnabled = prop.getProperty("jsbClusterEnabled", "false").equalsIgnoreCase("true");			
		
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
		
		if(isClusterEnabled()){
			NetworkConnector networkConnector = broker.addNetworkConnector("multicast://224.1.2.3:6255?group=" + license.getDeploymentId());
			networkConnector.setUserName(license.getDeploymentId() + ":" + license.getInstanceId());
			networkConnector.setPassword(JAuthHelper.bytesToHex((license.getLicenseKey())));
			networkConnector.setDecreaseNetworkConsumerPriority(true);
			connector.setDiscoveryUri(new URI("multicast://224.1.2.3:6255?group=" + license.getDeploymentId()));
			connector.setUpdateClusterClients(true);
			connector.setUpdateClusterClientsOnRemove(true);
			connector.setRebalanceClusterClients(true);
		}
		    			
		broker.addConnector(connector);
		broker.start();
		if (!broker.waitUntilStarted()) {
            throw new Exception(broker.getStartException());
        }
		
	}

	public void stop(){
		
		stopAudit();
		
		udeCore.stop();

		try {
			broker.stop();
			broker.waitUntilStopped();
		} catch (Exception e) {
			logger.error("unable to stop broker ",e);
		}
		
		cachingSys.shutdown();
	}
	
	public PersistenceFacade getCachingSys() {
		return cachingSys;
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
	
	private void stopAudit() {
		exec.shutdown();
		
		try {
			exec.awaitTermination(250, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			logger.error("exception while awaiting termination of the audit exectutor", e);
		}
		
		if(!exec.isShutdown()){
			exec.shutdownNow();
		}
	}
	
	private void setupAudit(){
		exec = Executors.newSingleThreadScheduledExecutor();
		Runnable command = new Runnable() {
			public void run() {
				auditSystem();
			}
		};
		
		exec.scheduleAtFixedRate(command , 12, 12, TimeUnit.HOURS);
	}
	
	private void auditSystem(){
		try {
			//reload license key
			license = licenseKeySys.loadKeys(prop.getProperty("jsb-keystore"));
			if(licenseKeySys.willUdeLicenseKeyExpireInDays(license, 3)){
				logger.error("ude-license key will expire on : " + licenseKeySys.getUdeExpiryDate(license));
			}else if(licenseKeySys.willUdeLicenseKeyExpireInDays(license, 7)){
				logger.warn("ude-license key will expire on : " + licenseKeySys.getUdeExpiryDate(license));
			}else if(licenseKeySys.willUdeLicenseKeyExpireInDays(license, 14)){
				logger.info("ude-license key will expire on : " + licenseKeySys.getUdeExpiryDate(license));
			}else if(licenseKeySys.willUdeLicenseKeyExpireInDays(license, 21)){
				logger.debug("ude-license key will expire on : " + licenseKeySys.getUdeExpiryDate(license));
			}
			if(!licenseKeySys.isValidUdeLicenseKeyExpiry(license))stop();
		} catch (IOException e) {
			logger.error("IOException caught when trying to load license key, shutting down",e);
			stop();
		} catch (SecurityException e){
			logger.error("SecurityException caught when trying to load license key, shutting down",e);
			stop();
		}
	}
	
	public void auditMap(String udeInstance){
		Map<String,PersistedObject> sharedData;
		BlockingQueue<PersistedObject> workQueue;
		sharedData = (Map<String, PersistedObject>) cachingSys.getMap("sharedData");
		workQueue = cachingSys.getQueue("tasks");
		PersistedObject statefulData;

		for(String s:sharedData.keySet()){
			statefulData = sharedData.get(s);
			// Only resubmit jobs to queue for UDE that has gone down
			if(statefulData != null && statefulData.getUDEInstance().equals(udeInstance)){
				workQueue.offer(statefulData);
			}
		}
	}
	
	public String getUdeInstance() {
		return license.getDeploymentId() + ":" + license.getInstanceId();
	}
	
	public String getBrokerTransportIp() {
		if(brokerTransportIp == null){
			//default value, which will work on both MAC OS and Windows, however for linux machines
			//we need to find the interface with the IPv4 address, we use the default eth0 but this
			//can be overwritten using the property jsbLocalNetworkInterface
			try{
				brokerTransportIp = InetAddress.getLocalHost().getHostAddress();
			}catch(UnknownHostException e){
				logger.warn("unknown host exception caught, expected on linux system.", e);
			}
			
			try{
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
			} catch (SocketException e) {
				logger.warn("SocketException caught when trying tp parse interfaces", e);
			}
		}
		
		return brokerTransportIp;
	}

	public boolean isClusterEnabled(){
		return clusterEnabled;
	}

}
