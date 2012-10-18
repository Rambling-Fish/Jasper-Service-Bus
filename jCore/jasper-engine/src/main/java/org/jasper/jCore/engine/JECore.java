package org.jasper.jCore.engine;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.crypto.Cipher;

import org.apache.activemq.broker.Connector;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.jasper.jCore.auth.JasperBrokerService;
import org.jasper.jLib.jAuth.JCoreLicense;

public class JECore {
	
	static Logger logger = Logger.getLogger("org.jasper");

	private static final String PUBLIC_KEY_FILENAME = "jasper.rsa.public.key";
	private static final String LICENSE_FILE_SUFFIX = "-license.key";

	
	private static JCoreLicense license;
	private static PublicKey publicKey;

	
	public static boolean isSystemDeploymentId(String id) {
		return license.getDeploymentId().equals(id);
	}
	
	public static String getDeploymentID() {
		return license.getDeploymentId();
	}
	
	private static void loadKeys(String keyStore) throws IOException {
		loadPublicKeyFromFile(keyStore + PUBLIC_KEY_FILENAME);
		loadJCoreLicenseFromFile(keyStore);
	}
	
	private static void loadJCoreLicenseFromFile(String keyStore) throws IOException {
		InputStream in = new FileInputStream(getLicenseKeyFile(keyStore));
		ObjectInputStream oin = new ObjectInputStream(new BufferedInputStream(in));
		try {
			license = (JCoreLicense) oin.readObject();
		} catch (Exception e) {
		    throw new RuntimeException("Spurious serialisation error", e);
		} finally {
			oin.close();
		}
	}
	
	private static byte[] rsaDecrypt(byte[] data,PublicKey pubKey) throws Exception{
		  Cipher cipher = Cipher.getInstance("RSA");
		  cipher.init(Cipher.DECRYPT_MODE, pubKey);
		  byte[] cipherData = cipher.doFinal(data);
		  return cipherData;
	}
	
	private static File getLicenseKeyFile(String keystore){
		List<File> results = new ArrayList<File>();
		File[] files = new File(keystore).listFiles();

		for (File file : files) {
		    if (file.isFile()) {
		    	if(file.getName().endsWith("LICENSE_FILE_SUFFIX")){
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
		
	private static void loadPublicKeyFromFile(String filename) throws IOException {
		InputStream in = new FileInputStream(filename);
		ObjectInputStream oin = new ObjectInputStream(new BufferedInputStream(in));
		try {
			BigInteger m = (BigInteger) oin.readObject();
			BigInteger e = (BigInteger) oin.readObject();
			RSAPublicKeySpec keySpec = new RSAPublicKeySpec(m, e);
			KeyFactory fact = KeyFactory.getInstance("RSA");
			publicKey = fact.generatePublic(keySpec);
		} catch (Exception e) {
		    throw new RuntimeException("Spurious serialisation error", e);
		} finally {
			oin.close();
		}
	}
	
	
	private static boolean isValidLicenseKey() {
		// TODO Auto-generated method stub
		return false;
	}
	
	private static boolean isValidLicenseKeyExpiry() {
		// TODO Auto-generated method stub
		return false;
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
    		if(logger.isDebugEnabled()) {
    			logger.debug("jasperDeploymentID = " + prop.getProperty("jasperDeploymentID")); 
    			logger.debug("jasperDeploymentAuthKey = " + prop.getProperty("jasperDeploymentAuthKey")); 

    		}
    	} catch (IOException ex) {
    		ex.printStackTrace();
    	}
    	 	
    	loadKeys(prop.getProperty("jCore-keystore"));
    	
    	if(isValidLicenseKey() && isValidLicenseKeyExpiry()){
			JasperBrokerService brokerService = new JasperBrokerService();
			Connector connector = brokerService.addConnector("tcp://"+ prop.getProperty("jasperEngineUrlHost") + ":" + prop.getProperty("jasperEngineUrlPort"));	
			brokerService.start();
    	}else if (!isValidLicenseKey()){
			logger.error("invalid license key, jasper engine not starting"); 
    	}else if (!isValidLicenseKeyExpiry()){
			logger.error("license key expired, jasper engine not starting"); 
    	}

	}

}
