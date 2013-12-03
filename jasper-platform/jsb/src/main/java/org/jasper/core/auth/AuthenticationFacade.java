package org.jasper.core.auth;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.PublicKey;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.apache.log4j.Logger;
import org.jasper.jLib.jAuth.ClientLicense;
import org.jasper.jLib.jAuth.UDELicense;
import org.jasper.jLib.jAuth.util.JAuthHelper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

public class AuthenticationFacade {
	
	static Logger logger = Logger.getLogger(AuthenticationFacade.class.getName());
	private PublicKey publicKey;	
	private static AuthenticationFacade authFacade;
	private static Gson gson;
	private String deploymentId;
	
	private AuthenticationFacade(){
		GsonBuilder builder = new GsonBuilder();
        builder.excludeFieldsWithoutExposeAnnotation();
        gson = builder.create();
	}
	
	public static AuthenticationFacade getInstance(){
		if(authFacade == null){
			authFacade = new AuthenticationFacade();
		}
		return authFacade;
	}
	
	private String getDeploymentId(){
		return deploymentId;
	}
	
	public UDELicense loadKeys(String keyStore) throws IOException {
		File licenseKeyFile = getLicenseKeyFile(keyStore);
		if(licenseKeyFile == null){
	    	throw (SecurityException)new SecurityException("Unable to find single UDE license key in: " + keyStore);
		}		
		publicKey = JAuthHelper.getPublicKeyFromFile(keyStore);
		UDELicense udeLic = JAuthHelper.loadUDELicenseFromFile(keyStore + licenseKeyFile.getName());
		if (isUdeLicenseKey(udeLic)){
			// Set deploymentID
			deploymentId = udeLic.getDeploymentId();
			return udeLic;
		}else return null; 		
		
	}
		
	private File getLicenseKeyFile(String keystore){
		List<File> results = new ArrayList<File>();
		File[] files = new File(keystore).listFiles();

		for (File file : files) {
		    if (file.isFile()) {
		    	if(file.getName().endsWith(JAuthHelper.UDE_LICENSE_FILE_SUFFIX)){
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
	
    public String getUdeDeploymentAndInstance(String password) {
        UDELicense lic = getUdeLicense(JAuthHelper.hexToBytes(password));
        if(lic == null) return null;
        return (lic.getDeploymentId() + ":" + lic.getInstanceId());
    }
	
	public boolean isSystemDeploymentId(String id) {
		return authFacade.getDeploymentId().equals(id);
	}
	
	
	public Calendar getExpiry(String licenseKey) {
		return getClientLicense(JAuthHelper.hexToBytes(licenseKey)).getExpiry();
	}
	
	public boolean isValidUdeLicenseKey(UDELicense license) throws Exception {
		return license.toString().equals(new String(JAuthHelper.rsaDecrypt(license.getLicenseKey(), publicKey)));
	}
	
	public boolean isValidUdeLicenseKeyExpiry(UDELicense license) {
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
	
	public String getUdeExpiryDate(UDELicense license) {
		if(license.getExpiry() == null){
			return "";
		}else{
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd:hh:mm:ss zzz");
			return format.format(license.getExpiry().getTime());
		}
	}
	
	public String getClientExpiryDate(ClientLicense license) {
		if(license.getExpiry() == null){
			return "";
		}else{
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd:hh:mm:ss zzz");
			return format.format(license.getExpiry().getTime());
		}
	}

	public boolean willUdeLicenseKeyExpireInDays(UDELicense license, int days) {
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
	
	public boolean willClientLicenseKeyExpireInDays(ClientLicense license, int days) {
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
	
	public boolean isValidLicenseKey(String userName, String password) {
		return isUdeAuthenticationValid(userName, password) || isClientAuthenticationValid(userName, password);
	}
	
	public boolean isUdeLicenseKey(String password) {
		UDELicense ulic = getUdeLicense(JAuthHelper.hexToBytes(password));
		logger.info("=========Checking if is a UDE on not Null=========\n");
		logger.info("Is this a UDE License? \n");
		logger.info("License: " + ulic);
		if (ulic.getType().toLowerCase().equals("ude")) {
			logger.info("It's a UDE License\n");
			return true;
		}
		else {
			logger.info("It's a CLIENT License \n");
			return false;
		}
	}
	
	public boolean isUdeLicenseKey(UDELicense lic) {
		if (lic.getType().toLowerCase().equals("ude")) {
			logger.info("It's a UDE License\n");
			return true;
		} else return false;
	}
	
	public boolean isUdeAuthenticationValid(String userName, String password) {
		UDELicense lic = getUdeLicense(JAuthHelper.hexToBytes(password));
		logger.info("========= isUdeAuthenticationValid =========\n");
		logger.info("Lic: " + lic + "\n UserName("+ userName + ") = " + lic.getDeploymentId() + ":" + lic.getInstanceId());
		return (lic != null) && (userName.equals( lic.getDeploymentId() + ":" + lic.getInstanceId()));
	}
	
	public String getUdeInstance(String password) {
		UDELicense lic = getUdeLicense(JAuthHelper.hexToBytes(password));
		if(lic == null) return null;
		return (lic.getDeploymentId() + ":" + lic.getInstanceId());
	}
	
	public boolean isClientAuthenticationValid(String userName, String password) {		
		ClientLicense lic = getClientLicense(JAuthHelper.hexToBytes(password));
		logger.info("========= isClientAuthenticationValid =========\n");
		logger.info("Lic: " + lic + "\n UserName("+ userName + ") = " + lic.getVendor() + ":" + lic.getAppName() + ":" + lic.getVersion() + ":" + lic.getDeploymentId());
		return (lic !=null) && (userName.equals( lic.getVendor() + ":" + lic.getAppName() + ":" +
				                lic.getVersion() + ":" + lic.getDeploymentId())) 
				            && !willClientLicenseKeyExpireInDays(lic, 0);
	}
	
	public UDELicense getUdeLicense(byte[] bytes) throws JsonSyntaxException {
		
		String password;
		try {
			password = new String(JAuthHelper.rsaDecrypt(bytes, publicKey));
			UDELicense lic = gson.fromJson(password, UDELicense.class);
			return lic;
		} catch (Exception e) {
			logger.error("Exception caught when trying to get UDE License Key",e);
			return null;
		}
	}

	public ClientLicense getClientLicense(byte[] bytes) throws JsonSyntaxException {
		
		String password;
		try {
			password = new String(JAuthHelper.rsaDecrypt(bytes, publicKey));
			ClientLicense lic = gson.fromJson(password, ClientLicense.class);			
			return lic;
		} catch (Exception e) {
			logger.error("Exception caught when trying to get Client License Key",e);
			return null;
		}

	}


}
