package org.jasper.jCore.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.jasper.jLib.jAuth.JSBLicense;
import org.jasper.jLib.jAuth.JTALicense;
import org.jasper.jLib.jAuth.util.JAuthHelper;

public class JAppLicenseGenerator {
	
	private static final String LICENSE_FILE_PATH = "licenseKeys/";
	
	
	public static void main(String[] args) throws Exception {
	    
		try {
			
			PrivateKey privateKey = JAuthHelper.getPrivateKeyFromFile("keystore/");
			PublicKey publicKey = JAuthHelper.getPublicKeyFromFile("keystore/");
			
		    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		    String input = "";
		    while (input != null) {
		        System.out.print("Options : \n" +
		        		                   "\tga - generate JTA lisenceFile\n" +
		        		                   "\tva - validate JTA lisenceFile\n" +
		        		                   "\tgc - generate JSB lisenceFile\n" +
		        		                   "\tvc - validate JSB lisenceFile\n" +
		        		                   "\t:");
		        input = in.readLine();
		        input = input.toLowerCase();
		        if(input.startsWith("ga")){
		        	System.out.println("jasper:sampleApp:1.0:jasperLab:2012-12-25");
		        	System.out.print("Enter JTA info <vendor>:<app_name>:<version>:<deployment_id>:<expiry yyyy-mm-dd>:<ntp_host>:<ntp_port> ; expiry and ntp info optional = \n");
			        input = in.readLine();
			        if(input == null) break;
			        String[] appInfo = input.split(":");
			        if(appInfo.length < 4) continue;
			        
			        String vendor = appInfo[0];
			        String appName = appInfo[1];
			        String version = appInfo[2];
			        String deploymentId = appInfo[3];
			        Calendar expiry = null;
			        if (appInfo.length > 4){
			        	String[] expiryDate = appInfo[4].split("-");
			        	if(expiryDate.length == 3){
			        		expiry = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
				        	expiry.set(Integer.parseInt(expiryDate[0]),Integer.parseInt(expiryDate[1])-1,Integer.parseInt(expiryDate[2]),23,59,59);
			        	}	
			        }
			        
			        String ntpHost = null;
			        if (appInfo.length > 5) ntpHost = appInfo[5];
			        
			        Integer ntpPort = null;
			        if (appInfo.length > 6) ntpPort = new Integer(appInfo[6]);
			        
			        JTALicense lic = new JTALicense(vendor, appName, version, deploymentId, expiry, ntpHost, ntpPort, null);
			        lic.setLicenseKey(JAuthHelper.rsaEncrypt(lic.toString().getBytes(), privateKey));
			        JAuthHelper.saveJTALicenseToFile(LICENSE_FILE_PATH, lic);
			        System.out.println("File created : " + LICENSE_FILE_PATH + lic.getAppName() + JAuthHelper.JTA_LICENSE_FILE_SUFFIX);
		        }else if (input.startsWith("va")){
		        	System.out.print("Enter JTA name for lisence key validation = ");
			        input = in.readLine();
			        String filename = LICENSE_FILE_PATH + input + JAuthHelper.JTA_LICENSE_FILE_SUFFIX;
			        if(doesFileExist(filename)){
			        	JTALicense lic = JAuthHelper.loadJTALicenseFromFile(filename);
			        	System.out.println("lisence info   = " + lic);
			        	System.out.println("decrypted info = " + new String(JAuthHelper.rsaDecrypt(lic.getLicenseKey(), publicKey)));
			        }else{
		        		System.out.println("lisence file doesn't exist");
		        	}
		        }else if (input.startsWith("gc")){
		        	System.out.println("jasperLab:2012-12-25");
		        	System.out.print("Enter JSB info <deployment_id>:<expiry yyyy-mm-dd>:<ntp_host>:<ntp_port> ; expiry and ntp info optional = \n");
			        input = in.readLine();
			        if(input == null) break;
			        String[] coreInfo = input.split(":");
			        if(coreInfo.length < 1) continue;
			        
			        String deploymentId = coreInfo[0];
			        Calendar expiry = null;
			        if (coreInfo.length > 1){
			        	String[] expiryDate = coreInfo[1].split("-");
			        	if(expiryDate.length == 3){
			        		expiry = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
				        	expiry.set(Integer.parseInt(expiryDate[0]),Integer.parseInt(expiryDate[1])-1,Integer.parseInt(expiryDate[2]),23,59,59);
			        	}	
			        }
			        
			        String ntpHost = null;
			        if (coreInfo.length > 2) ntpHost = coreInfo[2];
			        
			        Integer ntpPort = null;
			        if (coreInfo.length > 3) ntpPort = new Integer(coreInfo[3]);
			        
			        JSBLicense lic = new JSBLicense(deploymentId, expiry, ntpHost, ntpPort, null);
			        lic.setLicenseKey(JAuthHelper.rsaEncrypt(lic.toString().getBytes(), privateKey));
			        JAuthHelper.saveJSBLicenseToFile(LICENSE_FILE_PATH, lic);
			        System.out.println("File created : " + LICENSE_FILE_PATH + lic.getDeploymentId() + JAuthHelper.JSB_LICENSE_FILE_SUFFIX);
		        }else if (input.startsWith("vc")){
		        	System.out.print("Enter the deploymentID for JSB lisence file validation = ");
			        input = in.readLine();
			        String filename = LICENSE_FILE_PATH + input + JAuthHelper.JSB_LICENSE_FILE_SUFFIX;
			        if(doesFileExist(filename)){
			        	JSBLicense lic = JAuthHelper.loadJSBLicenseFromFile(filename);
			        	System.out.println("lisence info   = " + lic);
			        	System.out.println("decrypted info = " + new String(JAuthHelper.rsaDecrypt(lic.getLicenseKey(), publicKey)));
		        	}else{
		        		System.out.println("lisence file doesn't exist");
		        	}
		        }
		    }
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static boolean doesFileExist(String filename) {
		return (new File(filename)).exists();
	}

}
