package org.jasper.jCore.engine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.PublicKey;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.broker.Connector;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.jasper.jCore.auth.JasperBrokerService;
import org.jasper.jLib.jAuth.JSBLicense;
import org.jasper.jLib.jAuth.JTALicense;
import org.jasper.jLib.jAuth.util.JAuthHelper;

public class JECore {
	
	static Logger logger = Logger.getLogger("org.jasper");

	private static JECore core;
	
	private JSBLicense license;
	private PublicKey publicKey;
	
	private JECore(){
		
	}
	
	public static JECore getInstance(){
		if(core == null) core = new JECore();
		return core;
	}
	
	public boolean isSystemDeploymentId(String id) {
		return license.getDeploymentId().equals(id);
	}
	
	public String getDeploymentID() {
		return license.getDeploymentId();
	}
	
	private void loadKeys(String keyStore) throws IOException {
		File licenseKeyFile = getLicenseKeyFile(keyStore);
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
			
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd:hh:mm:ss zzz");
			
			System.out.println("current time = " + format.format(currentTime.getTime()));
			System.out.println("expiry time  = " + format.format(license.getExpiry().getTime()));
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
		ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
		Runnable command = new Runnable() {
			@Override
			public void run() {
				auditSystem();
			}
		};;;
		
		exec.scheduleAtFixedRate(command , 15, 15, TimeUnit.SECONDS);
	}
	
	private void auditSystem(){
		try {
			loadKeys(System.getProperty("jsb-keystore"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("audit callaed and isValidLicenseKeyExpiry = " + isValidLicenseKeyExpiry());
	}
	
	public boolean isJTAAuthenticationValid(String userName, String password) {
		System.out.println("username = " + userName);
		
		JTALicense lic = getJTALicense(JAuthHelper.hexToBytes(password));
		
		if(userName.equals( lic.getVendor() + ":" + lic.getAppName() + ":" +
                lic.getVersion() + ":" + lic.getDeploymentId())){
			System.out.println("valid licenseKey : " + lic);
		}else{
			System.out.println("invalid licenseKey" + lic);
		}
		
		return userName.equals( lic.getVendor() + ":" + lic.getAppName() + ":" +
				                lic.getVersion() + ":" + lic.getDeploymentId());
	}

	private JTALicense getJTALicense(byte[] bytes) {
		
		String password;
		try {
			password = new String(JAuthHelper.rsaDecrypt(bytes, publicKey));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		System.out.println("decrypted string = " + password);
		
		String[] jtaInfo = password.split(":");
        if(jtaInfo.length < 4) return null;
        
        String vendor = jtaInfo[0];
        String appName = jtaInfo[1];
        String version = jtaInfo[2];
        String deploymentId = jtaInfo[3];
        Calendar expiry = null;
        if (jtaInfo.length > 4){
        	String[] expiryDate = jtaInfo[4].split("-");
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
        if (jtaInfo.length > 5) ntpHost = jtaInfo[5];
        
        Integer ntpPort = null;
        if (jtaInfo.length > 6) ntpPort = new Integer(jtaInfo[6]);
        
        return new JTALicense(vendor, appName, version, deploymentId, expiry, ntpHost, ntpPort, null);
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
    	 	
    	core.loadKeys(System.getProperty("jsb-keystore"));
    	
    	if(core.isValidLicenseKey()){
    		if(core.isValidLicenseKeyExpiry()){
    			logger.error("SYSTEM STARTING");
    			JasperBrokerService brokerService = new JasperBrokerService();
    			Connector connector = brokerService.addConnector("tcp://"+ prop.getProperty("jsbUrlHost") + ":" + prop.getProperty("jsbUrlPort"));	
    			brokerService.start();
			}else{
    			logger.error("license key expired, jsb not starting"); 
    		}		
    	}else{
			logger.error("invalid license key, jsb not starting"); 
    	}
		core.setupAudit();
	}
}
