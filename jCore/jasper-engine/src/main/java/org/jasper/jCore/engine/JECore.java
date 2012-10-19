package org.jasper.jCore.engine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import org.apache.activemq.broker.Connector;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.jasper.jCore.auth.JasperBrokerService;
import org.jasper.jLib.jAuth.JSBLicense;
import org.jasper.jLib.jAuth.util.JAuthHelper;

public class JECore {
	
	static Logger logger = Logger.getLogger("org.jasper");


	
	private static JSBLicense license;
	private static PublicKey publicKey;

	
	public static boolean isSystemDeploymentId(String id) {
		return license.getDeploymentId().equals(id);
	}
	
	public static String getDeploymentID() {
		return license.getDeploymentId();
	}
	
	private static void loadKeys(String keyStore) throws IOException {
		File licenseKeyFile = getLicenseKeyFile(keyStore);
		license = JAuthHelper.loadJSBLicenseFromFile(keyStore + licenseKeyFile.getName());
		publicKey = JAuthHelper.getPublicKeyFromFile(keyStore);
	}
		
	private static File getLicenseKeyFile(String keystore){
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
		
	
	private static boolean isValidLicenseKey() throws Exception {
		return license.toString().equals(new String(JAuthHelper.rsaDecrypt(license.getLicenseKey(), publicKey)));
	}
	
	private static boolean isValidLicenseKeyExpiry() {
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
	
	private static TimeInfo getNTPTime(String host, Integer port) {
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

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
	    
    	Properties prop = new Properties();
    	 
    	try {
            //load a properties file
    		prop.load(new FileInputStream(System.getProperty("jCore-engine-property-file")));
    		DOMConfigurator.configure(System.getProperty("jCore-engine-log4j-xml"));
    	} catch (IOException ex) {
    		ex.printStackTrace();
    	}
    	 	
    	loadKeys(System.getProperty("jCore-keystore"));
    	
    	if(isValidLicenseKey()){
    		if(isValidLicenseKeyExpiry()){
    			logger.error("SYSTEM STARTING");
//    			JasperBrokerService brokerService = new JasperBrokerService();
//    			Connector connector = brokerService.addConnector("tcp://"+ prop.getProperty("jasperEngineUrlHost") + ":" + prop.getProperty("jasperEngineUrlPort"));	
//    			brokerService.start();
			}else{
    			logger.error("license key expired, jasper engine not starting"); 
    		}		
    	}else{
			logger.error("invalid license key, jasper engine not starting"); 
    	}
	}

}
