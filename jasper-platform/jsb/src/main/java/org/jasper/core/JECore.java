package org.jasper.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.security.InvalidParameterException;
import java.security.PublicKey;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.broker.BrokerPlugin;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.JsbTransportConnector;
import org.apache.activemq.network.NetworkConnector;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.jasper.core.auth.JasperAuthenticationPlugin;
import org.jasper.core.delegate.Delegate;
import org.jasper.core.delegate.DelegateFactory;
import org.jasper.jLib.jAuth.JSBLicense;
import org.jasper.jLib.jAuth.JTALicense;
import org.jasper.jLib.jAuth.util.JAuthHelper;

public class JECore {
	
	static Logger logger = Logger.getLogger("org.jasper");
	
	private BrokerService broker;
	private JSBLicense license;
	private PublicKey publicKey;
	
	private ScheduledExecutorService auditExec;
	private ExecutorService delegateService;

	private List<Delegate> delegates = new ArrayList<Delegate>();

	private String brokerTransportIp;

	private DelegateFactory delegateFactory;

	private boolean clusterEnabled;
	
	private static JECore instance;
	
	public static JECore getInstance() {
		if(instance == null){
	    	Properties prop = new Properties();
	        //load a properties file
	   		try {
				prop.load(new FileInputStream(System.getProperty("jsb-property-file")));
		   		if(System.getProperty("jsb-log4j-xml") != null) DOMConfigurator.configure(System.getProperty("jsb-log4j-xml"));
				instance = new JECore(prop);
	   		} catch (Exception e) {
	   			logger.error("Error when initilizing JECore : ",e);
	   		}
		}
		return instance;
	}

	private JECore(Properties prop) throws Exception{
    	loadKeys(System.getProperty("jsb-keystore"));
    	
    	if(isValidLicenseKey()){
    		if(isValidLicenseKeyExpiry()){
    			
    			broker = new JasperBrokerService();
    			broker.setBrokerName(getJSBLicense().getDeploymentId() + "_" + getJSBLicense().getInstanceId());
    			broker.setPersistent(false);
    			
    			setBrokerMemoryUsage(prop);
    			
    			broker.setPlugins(new BrokerPlugin[]{new JasperAuthenticationPlugin()});

				//default value, which will work on both MAC OS and Windows, however for linux machines
				//we need to find the interface with the IPv4 address, we use the default eth0 but this
				//can be overwritten using the property jsbLocalNetworkInterface
				brokerTransportIp = InetAddress.getLocalHost().getHostAddress();
				
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
    			
    			JsbTransportConnector connector;
    			if(prop.getProperty("jsbLocalURL") != null){
    				if(prop.getProperty("jsbLocalNetworkInterface") == null){
    					logger.error("jsbLocalNetworkInterface must be set when setting jsbLocalURL");
    					throw new InvalidParameterException("jsbLocalNetworkInterface must be set when setting jsbLocalURL");
    				}
    				connector = new JsbTransportConnector(prop.getProperty("jsbLocalURL"));	
    			}else{
    				connector = new JsbTransportConnector("tcp://"+ brokerTransportIp + ":61616?maximumConnections=1000&wireformat.maxFrameSize=104857600");	
    			}
    			
    			//clusteredEnable is by default false, only set to false if false 
    			clusterEnabled = prop.getProperty("jsbClusterEnabled", "false").equalsIgnoreCase("true");
    			
    			if(isClusterEnabled()){
    				NetworkConnector networkConnector = broker.addNetworkConnector("multicast://224.1.2.3:6255?group=" + getJSBLicense().getDeploymentId());
    				networkConnector.setUserName(getJSBLicense().getDeploymentId() + ":" + getJSBLicense().getInstanceId());
    				networkConnector.setPassword(JAuthHelper.bytesToHex((getJSBLicense().getLicenseKey())));
    				connector.setDiscoveryUri(new URI("multicast://224.1.2.3:6255?group=" + getJSBLicense().getDeploymentId()));
    				connector.setUpdateClusterClients(true);
    				connector.setUpdateClusterClientsOnRemove(true);
    				connector.setRebalanceClusterClients(true);
    			}
    			
    			broker.addConnector(connector);

			}else{
    			logger.error("license key expired, jsb not starting"); 
    		}		
    	}else{
			logger.error("invalid license key, jsb not starting"); 
    	}
	}
	
	public boolean isClusterEnabled() {
		return clusterEnabled;
	}

	public String getBrokerTransportIp() {
		return brokerTransportIp;
	}

	public void start() throws Exception{
		broker.start();
		
		if (!broker.waitUntilStarted()) {
            throw new Exception(broker.getStartException());
        }
		
		// Instantiate the delegate pool
		delegateService = Executors.newCachedThreadPool();
		delegateFactory = new DelegateFactory(isClusterEnabled(),getDeploymentID(),getDeploymentID() + "_password_2013_jun_20_1030");
		for(int i = 0;i<5;i++){
			delegates.add(delegateFactory.createDelegate());
		}
		
		for(Delegate d:delegates) delegateService.execute(d);
		
		setupAudit();
		
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
            	shutdown();
            }
        });
		
	}
	
	public boolean isSystemDeploymentId(String id) {
		return license.getDeploymentId().equals(id);
	}
	
	public String getDeploymentID() {
		return license.getDeploymentId();
	}
	
	private void loadKeys(String keyStore) throws IOException {
		File licenseKeyFile = getLicenseKeyFile(keyStore);
		if(licenseKeyFile == null){
	    	throw (SecurityException)new SecurityException("Unable to find single JSB license key in: " + keyStore);
		}
		license = JAuthHelper.loadJSBLicenseFromFile(keyStore + licenseKeyFile.getName());
		publicKey = JAuthHelper.getPublicKeyFromFile(keyStore);
	}
		
	private File getLicenseKeyFile(String keystore){
		List<File> results = new ArrayList<File>();
		File[] files = new File(keystore).listFiles();

		for (File file : files) {
		    if (file.isFile()) {
		    	if(file.getName().endsWith(JAuthHelper.JSB_LICENSE_FILE_SUFFIX)){
		    		results.add(file);
		    	}
		    }
		}
		if(results.size() == 1) {
			return results.get(0);
		}else{
			return null;
		}
	}
	
	private boolean isValidLicenseKey() throws Exception {
		return license.toString().equals(new String(JAuthHelper.rsaDecrypt(license.getLicenseKey(), publicKey)));
	}
	
	private JSBLicense getJSBLicense(){
		return license;
	}
	
	public boolean isThisMyJSBLicense(String password){
		return JAuthHelper.bytesToHex(license.getLicenseKey()).equals(password);
	}
	
	public Calendar getExpiry(String licenseKey) {
		return getJTALicense(JAuthHelper.hexToBytes(licenseKey)).getExpiry();
	}
	
	private boolean isValidLicenseKeyExpiry() {
		if(license.getExpiry() == null){
			return true;
		}else{
			Calendar currentTime;
			if(license.getNtpHost() == null){
				currentTime = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
			}else{
				TimeInfo ntpResponse = getNTPTime(license.getNtpHost(), license.getNtpPort());
				currentTime = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
				currentTime.setTime(ntpResponse.getMessage().getTransmitTimeStamp().getDate());
			}
			return currentTime.before(license.getExpiry());
		}
	}
	
	private TimeInfo getNTPTime(String host, Integer port) {
		NTPUDPClient client = new NTPUDPClient();
		client.setDefaultTimeout(10000);
		TimeInfo info = null;
		try {
			client.open();
			try {
				InetAddress hostAddr = InetAddress.getByName(host);
				if(port != null){
					info = client.getTime(hostAddr,port.intValue());
				}else{
					info = client.getTime(hostAddr);
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
		client.close();
		return info;
	}
	
	public void setupAudit(){
		auditExec = Executors.newSingleThreadScheduledExecutor();
		Runnable command = new Runnable() {
			@Override
			public void run() {
				auditSystem();
			}
		};;;
		
		auditExec.scheduleAtFixedRate(command , 12, 12, TimeUnit.HOURS);
	}
	
	private void auditSystem(){
		try {
			loadKeys(System.getProperty("jsb-keystore"));
			if(willLicenseKeyExpireInDays(3)){
				logger.error("jsb-license key will expire on : " + getExpiryDate());
			}else if(willLicenseKeyExpireInDays(7)){
				logger.warn("jsb-license key will expire on : " + getExpiryDate());
			}else if(willLicenseKeyExpireInDays(14)){
				logger.info("jsb-license key will expire on : " + getExpiryDate());
			}else if(willLicenseKeyExpireInDays(21)){
				logger.debug("jsb-license key will expire on : " + getExpiryDate());
			}
			if(!isValidLicenseKeyExpiry())shutdown();
		} catch (IOException e) {
			logger.error("IOException caught when trying to load license key, shutting down",e);
			shutdown();
		} catch (SecurityException e){
			logger.error("SecurityException caught when trying to load license key, shutting down",e);
			shutdown();
		}
	}
	
	private String getExpiryDate() {
		if(license.getExpiry() == null){
			return "";
		}else{
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd:hh:mm:ss zzz");
			return format.format(license.getExpiry().getTime());
		}
	}
	
	public String getExpiryDate(JTALicense license) {
		if(license.getExpiry() == null){
			return "";
		}else{
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd:hh:mm:ss zzz");
			return format.format(license.getExpiry().getTime());
		}
	}

	private boolean willLicenseKeyExpireInDays(int days) {
		if(license.getExpiry() == null){
			return false;
		}else{
			Calendar currentTime;
			if(license.getNtpHost() == null){
				currentTime = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
			}else{
				TimeInfo ntpResponse = getNTPTime(license.getNtpHost(), license.getNtpPort());
				currentTime = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
				currentTime.setTime(ntpResponse.getMessage().getTransmitTimeStamp().getDate());
			}
			currentTime.add(Calendar.DAY_OF_YEAR, days);
			return currentTime.after(license.getExpiry());
		}
				
	}
	
	public boolean willLicenseKeyExpireInDays(JTALicense license, int days) {
		if(license.getExpiry() == null){
			return false;
		}else{
			Calendar currentTime;
			if(license.getNtpHost() == null){
				currentTime = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
			}else{
				TimeInfo ntpResponse = getNTPTime(license.getNtpHost(), license.getNtpPort());
				currentTime = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
				currentTime.setTime(ntpResponse.getMessage().getTransmitTimeStamp().getDate());
			}
			currentTime.add(Calendar.DAY_OF_YEAR, days);
			return currentTime.after(license.getExpiry());
		}		
	}

	private void shutdown(){
		logger.info("received shutdown request, shutting down");
		auditExec.shutdown();
		
		for(Delegate d:delegates) d.shutdown();
		
		delegateService.shutdown();
		delegateFactory.shutdown();
		
		try {
			broker.stop();
		} catch (Exception e) {
			logger.error("Exception caught during shutdown ", e);
		}
	}
	
	public boolean isValidLicenseKey(String userName, String password) {
		return isJSBAuthenticationValid(userName, password) || isJTAAuthenticationValid(userName, password);
	}
	
	public boolean isJSBLicenseKey(String password) {
		return (getJSBLicense(JAuthHelper.hexToBytes(password)) !=null );
	}
	
	public boolean isJSBAuthenticationValid(String userName, String password) {
		JSBLicense lic = getJSBLicense(JAuthHelper.hexToBytes(password));
		return (lic != null) && (userName.equals( lic.getDeploymentId() + ":" + lic.getInstanceId()));
	}
	
	public String getJSBDeploymentAndInstance(String password) {
		JSBLicense lic = getJSBLicense(JAuthHelper.hexToBytes(password));
		if(lic == null) return null;
		return (lic.getDeploymentId() + ":" + lic.getInstanceId());
	}
	
	public String getJSBDeploymentAndInstance() {
		return license.getDeploymentId() + ":" + license.getInstanceId();
	}
	
	public boolean isJTAAuthenticationValid(String userName, String password) {		
		JTALicense lic = getJTALicense(JAuthHelper.hexToBytes(password));
		
		return (lic !=null) && (userName.equals( lic.getVendor() + ":" + lic.getAppName() + ":" +
				                lic.getVersion() + ":" + lic.getDeploymentId())) 
				            && !willLicenseKeyExpireInDays(lic, 0);
	}
	
	private JSBLicense getJSBLicense(byte[] bytes) {
		
		String password;
		try {
			password = new String(JAuthHelper.rsaDecrypt(bytes, publicKey));
			
			/*
			 * Sample valid jsb license string:
			 * jsb:jasperLab:0:2012-12-25:time.nrc.ca:8080
			 */
			String[] jsbInfo = password.split(":");
	        if(jsbInfo.length < 3) return null;
	        if(!jsbInfo[0].equals("jsb")) return null;
	        
	        String deploymentId = jsbInfo[1];
	        int instanceId = Integer.parseInt(jsbInfo[2]);
	        Calendar expiry = null;
	        if (jsbInfo.length > 3){
	        	String[] expiryDate = jsbInfo[3].split("-");
	        	if(expiryDate.length >= 3){
	        		int year = Integer.parseInt(expiryDate[0]);
	        		int month = Integer.parseInt(expiryDate[1])-1;
	        		int day = Integer.parseInt(expiryDate[2]);
	        		int hour = (expiryDate.length>3) ? Integer.parseInt(expiryDate[3]) : 23;
	        		int minute = (expiryDate.length>4) ? Integer.parseInt(expiryDate[4]) : 59;
	        		int second = (expiryDate.length>5) ? Integer.parseInt(expiryDate[5]) : 59;
	        		expiry = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
		        	expiry.set(year,month,day,hour,minute,second);
	        	}	
	        }
	        
	        String ntpHost = null;
	        if (jsbInfo.length > 4) ntpHost = jsbInfo[4];
	        
	        Integer ntpPort = null;
	        if (jsbInfo.length > 5) ntpPort = new Integer(jsbInfo[5]);
	        
	        return new JSBLicense(deploymentId, instanceId, expiry, ntpHost, ntpPort, null);      
		
		} catch (Exception e) {
			logger.error("Exception caught when trying to get JSBLicense Key",e);
			return null;
		}
	}

	public JTALicense getJTALicense(byte[] bytes) {
		
		String password;
		try {
			password = new String(JAuthHelper.rsaDecrypt(bytes, publicKey));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
		/*
		 * Sample valid jta license string:
		 * jta:jasper:sampleApp:1.0:jasperLab:2012-12-25-23-59-59:time.nrc.ca
		 */
		
		String[] jtaInfo = password.split(":");
        if(jtaInfo.length < 5) return null;
        if(!jtaInfo[0].equals("jta")) return null;
        
        String vendor = jtaInfo[1];
        String appName = jtaInfo[2];
        String version = jtaInfo[3];
        String deploymentId = jtaInfo[4];
        Calendar expiry = null;
        if (jtaInfo.length > 5){
        	String[] expiryDate = jtaInfo[5].split("-");
        	if(expiryDate.length >= 3){
        		int year = Integer.parseInt(expiryDate[0]);
        		int month = Integer.parseInt(expiryDate[1])-1;
        		int day = Integer.parseInt(expiryDate[2]);
        		int hour = (expiryDate.length>3) ? Integer.parseInt(expiryDate[3]) : 23;
        		int minute = (expiryDate.length>4) ? Integer.parseInt(expiryDate[4]) : 59;
        		int second = (expiryDate.length>5) ? Integer.parseInt(expiryDate[5]) : 59;
        		expiry = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
	        	expiry.set(year,month,day,hour,minute,second);
        	}	
        }
        
        String ntpHost = null;
        if (jtaInfo.length > 6) ntpHost = jtaInfo[6];
        
        Integer ntpPort = null;
        if (jtaInfo.length > 7) ntpPort = new Integer(jtaInfo[7]);
        
        return new JTALicense(vendor, appName, version, deploymentId, expiry, ntpHost, ntpPort, null);
	}
	
	private void setBrokerMemoryUsage(Properties prop) {
		if(prop.getProperty("memoryLimit") != null){
			broker.getSystemUsage().getMemoryUsage().setLimit(1024L * 1024 * Long.parseLong(prop.getProperty("memoryLimit")));
		}else{
			broker.getSystemUsage().getMemoryUsage().setLimit(1024L * 1024 * 64);
		}
		
		if(prop.getProperty("storeLimit") != null){
			broker.getSystemUsage().getStoreUsage().setLimit(1024L * 1024 * Long.parseLong(prop.getProperty("storeLimit")));
		}else{
			broker.getSystemUsage().getStoreUsage().setLimit(1024L * 1024 * 10000);
		}
		
		if(prop.getProperty("tempLimit") != null){
			broker.getSystemUsage().getTempUsage().setLimit(1024L * 1024 * Long.parseLong(prop.getProperty("tempLimit")));
		}else{
			broker.getSystemUsage().getTempUsage().setLimit(1024L * 1024 * 15000);
		}	
	}

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
    	JECore core = JECore.getInstance();
    	core.start();
	}

}