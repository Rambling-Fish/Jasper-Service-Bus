package org.jasper.jCore.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.crypto.Cipher;

import org.jasper.jLib.jAuth.JSBLicense;
import org.jasper.jLib.jAuth.JTALicense;
import org.jasper.jLib.jAuth.UDELicense;
import org.jasper.jLib.jAuth.ClientLicense;
import org.jasper.jLib.jAuth.util.JAuthHelper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class JasperLicenseKeyGenerator {
	
	private static final String LICENSE_FILE_PATH = "licenseKeys/";
	private static final String PRIVATE_KEY_FILENAME = "jasper.rsa.private.key";
	private static final String PUBLIC_KEY_FILENAME = "jasper.rsa.public.key";

	public static byte[] rsaEncrypt(byte[] data,PrivateKey prvKey) throws Exception{
		  Cipher cipher = Cipher.getInstance("RSA");
		  cipher.init(Cipher.ENCRYPT_MODE, prvKey);
		  byte[] cipherData = cipher.doFinal(data);
		  return cipherData;
	}
	
	public static void generateKeys(String path) throws Exception{
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
		kpg.initialize(2048*4);
		KeyPair kp = kpg.genKeyPair();
	
		KeyFactory fact = KeyFactory.getInstance("RSA");
		RSAPublicKeySpec pub = fact.getKeySpec(kp.getPublic(), RSAPublicKeySpec.class);
		RSAPrivateKeySpec priv = fact.getKeySpec(kp.getPrivate(), RSAPrivateKeySpec.class);

		saveRSAKeyToFile(path + PUBLIC_KEY_FILENAME, pub.getModulus(), pub.getPublicExponent());
		saveRSAKeyToFile(path + PRIVATE_KEY_FILENAME, priv.getModulus(), priv.getPrivateExponent());
	}
	
	private static void saveRSAKeyToFile(String fileName, BigInteger mod, BigInteger exp) throws IOException {
		ObjectOutputStream oout = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(fileName)));
		try {
			oout.writeObject(mod);
		    oout.writeObject(exp);
	    } catch (Exception e) {
	    	throw new IOException("Unexpected error", e);
		} finally {
		    oout.close();
		}
	}
	
	public static void saveJTALicenseToFile(String path, JTALicense license) throws IOException {
		FileOutputStream fio = new FileOutputStream(path + license.getVendor() + "_" + license.getAppName() + "_" + license.getVersion() + JAuthHelper.JTA_LICENSE_FILE_SUFFIX);
		ObjectOutputStream oout = new ObjectOutputStream(new BufferedOutputStream(fio));
		try {
			oout.writeObject(license);
	    } catch (Exception e) {
	    	throw new IOException("Unexpected error", e);
		} finally {
		    oout.close();
		}
	}
	
	public static void saveJSBLicenseToFile(String path, JSBLicense license) throws IOException {
		FileOutputStream fio = new FileOutputStream(path + license.getDeploymentId() + "-" + license.getInstanceId() + JAuthHelper.JSB_LICENSE_FILE_SUFFIX);
		ObjectOutputStream oout = new ObjectOutputStream(new BufferedOutputStream(fio));
		try {
			oout.writeObject(license);
	    } catch (Exception e) {
	    	throw new IOException("Unexpected error", e);
		} finally {
		    oout.close();
		}
	}
	
	public static void saveClientLicenseToFile(String path, ClientLicense license) throws IOException {
		FileOutputStream fio = new FileOutputStream(path + license.getVendor() + "_" + license.getAppName() + "_" + license.getVersion() + JAuthHelper.CLIENT_LICENSE_FILE_SUFFIX);
		ObjectOutputStream oout = new ObjectOutputStream(new BufferedOutputStream(fio));
		try {
			oout.writeObject(license);
	    } catch (Exception e) {
	    	throw new IOException("Unexpected error", e);
		} finally {
		    oout.close();
		}
	}
	
	public static void saveUDELicenseToFile(String path, UDELicense license) throws IOException {
		FileOutputStream fio = new FileOutputStream(path + license.getDeploymentId() + "-" + license.getInstanceId() + JAuthHelper.UDE_LICENSE_FILE_SUFFIX);
		ObjectOutputStream oout = new ObjectOutputStream(new BufferedOutputStream(fio));
		try {
			oout.writeObject(license);
	    } catch (Exception e) {
	    	throw new IOException("Unexpected error", e);
		} finally {
		    oout.close();
		}
	}
	
	
	
	public static PrivateKey getPrivateKeyFromFile(String path) throws IOException {
		InputStream in = new FileInputStream(path + PRIVATE_KEY_FILENAME);
		ObjectInputStream oin = new ObjectInputStream(new BufferedInputStream(in));
		try {
			BigInteger m = (BigInteger) oin.readObject();
			BigInteger e = (BigInteger) oin.readObject();
			RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(m, e);
			KeyFactory fact = KeyFactory.getInstance("RSA");
			PrivateKey prvKey = fact.generatePrivate(keySpec);
			return prvKey;
		} catch (Exception e) {
		    throw new RuntimeException("Spurious serialisation error", e);
		} finally {
			oin.close();
		}
	}
	
	public static void main(String[] args) throws Exception {
	    
		//generateKeys("/Users/jorgemorales/Desktop/keys/");
		
		try {
			
			PrivateKey privateKey = getPrivateKeyFromFile("keystore/");
			PublicKey publicKey = JAuthHelper.getPublicKeyFromFile("keystore/");
			
		    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		    String input = "";
		    
		    while (input != null) {
		        System.out.print("Options : \n" +
		        		                   "\tclient  - generate Client licenseFile\n" +
		        		                   "\tvclient  - validate Client licenseFile\n" +
		        		                   "\tude  - generate UDE licenseFile\n" +
		        		                   "\tvude  - validate UDE licenseFile\n" +
		        		                   "\tjta  - generate JTA licenseFile\n" +
		        		                   "\tjtav - validate JTA licenseFile\n" +
		        		                   "\tjsb  - generate JSB licenseFile\n" +
		        		                   "\tjsbv - validate JSB licenseFile\n" +
		        		                   "\ttest - generate testing mule app\n" +
		        		                   "\t:");
		        input = in.readLine();
		        input = input.toLowerCase();
		        if (input.startsWith("jtav")){
		        	System.out.print("Enter JTA name for license key validation = ");
			        input = in.readLine();
			        String filename = LICENSE_FILE_PATH + input + JAuthHelper.JTA_LICENSE_FILE_SUFFIX;
			        if(doesFileExist(filename)){
			        	JTALicense lic = JAuthHelper.loadJTALicenseFromFile(filename);
			        	System.out.println("license info   = " + lic);
			        	System.out.println("decrypted info = " + new String(JAuthHelper.rsaDecrypt(lic.getLicenseKey(), publicKey)));
			        }else{
		        		System.out.println("license file doesn't exist");
		        	}
		        }else if (input.startsWith("client")){
		        	
		        	System.out.println("{\"type\": \"<dta/jsc(Str)>\", \"instance\": \"<instance(int)>\", \"vendor\": \"<vendor(Str)>\", \"appName\": \"<dta/jsc name(Str)>\",\"version\": \"<dta/jsc version(Str)>\", ");
		        	System.out.println("\"numOfPublishers\": <number(int)>, \"numOfConsumers\": <number(int)>, \"adminQueue\": \"<queueName(Str)>\",\"deploymentId\": \"<deploymentName(Str)>\", ");
		        	System.out.println("\"expiry\":{\"year\":<number(int)>,\"month\":<number(int)>,\"dayOfMonth\":<number(int)>,\"hourOfDay\":<number(int)>,\"minute\":<number(int)>,\"second\":<number(int)>, ");
		        	System.out.println("\"ntpHost\":\"<time.nrc.ca/optional>\", \"ntpPort\":\"<number(int)/optional>\" }");
		        	input = in.readLine();
			        if(input == null) break;
			        Gson gson = new Gson();
					ClientLicense lic = gson.fromJson(input, ClientLicense.class);
					if(lic == null) break;
					System.out.println("JSON :" + gson.toJson(lic));
					System.out.println("license info   = " + lic);
					System.out.println("lic size   = " + gson.toJson(lic).getBytes().length);
					lic.setLicenseKey(rsaEncrypt( gson.toJson(lic).getBytes(), privateKey));
			        saveClientLicenseToFile(LICENSE_FILE_PATH, lic);
			        System.out.println("File created : " + LICENSE_FILE_PATH + lic.getVendor() + "_" + lic.getAppName() + "_" + lic.getVersion() + JAuthHelper.CLIENT_LICENSE_FILE_SUFFIX);
			        System.out.println("userName = " + lic.getVendor() + ":" + lic.getAppName() + ":" + lic.getVersion() + ":" + lic.getDeploymentId());
			        System.out.println("password = " + JAuthHelper.bytesToHex(lic.getLicenseKey()));
			        
		        }else if (input.startsWith("vclient")){
		        	
		        	System.out.print("Enter Client's name for license key validation = ");
			        input = in.readLine();
			        String filename = LICENSE_FILE_PATH + input + JAuthHelper.CLIENT_LICENSE_FILE_SUFFIX;
			        if(doesFileExist(filename)){
			        	ClientLicense lic = JAuthHelper.loadClientLicenseFromFile(filename);
			        	System.out.println("license info   = " + lic);
			        	System.out.println("decrypted info = " + new String(JAuthHelper.rsaDecrypt(lic.getLicenseKey(), publicKey)));
			        }else{
		        		System.out.println("license file doesn't exist");
		        	}
			        
		        }else if (input.startsWith("ude")){
		        	
		        	System.out.println("{\"type\":\"ude(Str)\",\"version\":\"<ude version(Str)>\",\"deploymentId\":\"<deploymentName(Str)>\",\"instanceId\":<instance(int)>,\"numOfPublishers\":<number(int)>,\"numOfConsumers\":<number(int)>,");
		        	System.out.println("\"expiry\":{\"year\":<number(int)>,\"month\":<number(int)>,\"dayOfMonth\":<number(int)>,\"hourOfDay\":<number(int)>,\"minute\":<number(int)>,\"second\":<number(int)>}, ");
		        	System.out.println("\"ntpHost\" : \"<hostName/Optional>\", \"ntpPort\": <number(int)/Optional>}");
		        	input = in.readLine();
			        if(input == null) break;
			        
			        //GsonBuilder builder = new GsonBuilder();
			        //builder.excludeFieldsWithoutExposeAnnotation();
			        //Gson gson = builder.create();
			        Gson gson = new Gson();
					UDELicense lic = gson.fromJson(input, UDELicense.class);
					if(lic == null) break;
					System.out.println("JSON :" + gson.toJson(lic));
					System.out.println("license info   = " + lic);
					lic.setLicenseKey(rsaEncrypt( gson.toJson(lic).getBytes(), privateKey));
			        saveUDELicenseToFile(LICENSE_FILE_PATH, lic);
			        System.out.println("File created : " + LICENSE_FILE_PATH + lic.getDeploymentId() + "-" + lic.getInstanceId() + JAuthHelper.UDE_LICENSE_FILE_SUFFIX);
			        //license.getDeploymentId() + "-" + license.getInstanceId() + JAuthHelper.UDE_LICENSE_FILE_SUFFIX
		        }else if (input.startsWith("vude")){
		        	
		        	System.out.print("Enter Client's name for license key validation = ");
			        input = in.readLine();
			        String filename = LICENSE_FILE_PATH + input + JAuthHelper.UDE_LICENSE_FILE_SUFFIX;
			        if(doesFileExist(filename)){
			        	UDELicense lic = JAuthHelper.loadUDELicenseFromFile(filename);
			        	System.out.println("license info   = " + lic);
			        	System.out.println("decrypted info = " + new String(JAuthHelper.rsaDecrypt(lic.getLicenseKey(), publicKey)));
			        }else{
		        		System.out.println("license file doesn't exist");
		        	}
			        
		        }else if(input.startsWith("jta")){
		        	System.out.println("jasper:sampleApp:1.0:jasperLab:2012-12-25-23-59-59:time.nrc.ca");
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
			        if (appInfo.length > 5) ntpHost = appInfo[5];
			        
			        Integer ntpPort = null;
			        if (appInfo.length > 6) ntpPort = new Integer(appInfo[6]);
			        
			        JTALicense lic = new JTALicense(vendor, appName, version, deploymentId, expiry, ntpHost, ntpPort, null);
			        System.out.println("license info   = " + lic);
			        lic.setLicenseKey(rsaEncrypt(lic.toString().getBytes(), privateKey));
			        saveJTALicenseToFile(LICENSE_FILE_PATH, lic);
			        System.out.println("File created : " + LICENSE_FILE_PATH + lic.getAppName() + JAuthHelper.JTA_LICENSE_FILE_SUFFIX);
			        System.out.println("userName = " + lic.getVendor() + ":" + lic.getAppName() + ":" + lic.getVersion() + ":" + lic.getDeploymentId());
			        System.out.println("password = " + JAuthHelper.bytesToHex(lic.getLicenseKey()));
		        }else if (input.startsWith("jsbv")){
		        	System.out.print("Enter the deploymentID and instance for JSB license file validation i.e. jasperLab:0 = ");
			        input = in.readLine();
			        String[] jsbInfo = input.split(":");
			        if(jsbInfo.length < 2) continue;
			        String filename = LICENSE_FILE_PATH + jsbInfo[0] + "-" + jsbInfo[1] + JAuthHelper.JSB_LICENSE_FILE_SUFFIX;
			        if(doesFileExist(filename)){
			        	JSBLicense lic = JAuthHelper.loadJSBLicenseFromFile(filename);
			        	System.out.println("license info   = " + lic);
			        	System.out.println("decrypted info = " + new String(JAuthHelper.rsaDecrypt(lic.getLicenseKey(), publicKey)));
		        	}else{
		        		System.out.println("license file doesn't exist");
		        	}
		        } else if (input.startsWith("jsb")){
		        	System.out.println("jasperLab:0:2012-12-25:time.nrc.ca");
		        	System.out.print("Enter JSB info <deployment_id>:<jsb_instance>:<expiry yyyy-mm-dd>:<ntp_host>:<ntp_port> ; expiry and ntp info optional = \n");
			        input = in.readLine();
			        if(input == null) break;
			        String[] coreInfo = input.split(":");
			        if(coreInfo.length < 1) continue;
			        
			        String deploymentId = coreInfo[0];
			        int instanceId = Integer.parseInt(coreInfo[1]);
			        Calendar expiry = null;
			        if (coreInfo.length > 2){
			        	String[] expiryDate = coreInfo[2].split("-");
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
			        if (coreInfo.length > 3) ntpHost = coreInfo[3];
			        
			        Integer ntpPort = null;
			        if (coreInfo.length > 4) ntpPort = new Integer(coreInfo[4]);
			        
			        JSBLicense lic = new JSBLicense(deploymentId, instanceId, expiry, ntpHost, ntpPort, null);
			        lic.setLicenseKey(rsaEncrypt(lic.toString().getBytes(), privateKey));
			        saveJSBLicenseToFile(LICENSE_FILE_PATH, lic);
			        System.out.println("File created : " + LICENSE_FILE_PATH + lic.getDeploymentId() + JAuthHelper.JSB_LICENSE_FILE_SUFFIX);
		        }else if(input.startsWith("test")){
		        	System.out.print("Number of connections: ");
		        	int num = Integer.parseInt(in.readLine());
		        	
		        	StringBuilder sb = new StringBuilder();
		        	sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + 
		        			  "<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\" xmlns:http=\"http://www.mulesoft.org/schema/mule/http\" xmlns:jms=\"http://www.mulesoft.org/schema/mule/jms\" xmlns:doc=\"http://www.mulesoft.org/schema/mule/documentation\" xmlns:spring=\"http://www.springframework.org/schema/beans\" xmlns:core=\"http://www.mulesoft.org/schema/mule/core\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" version=\"CE-3.3.0\" xsi:schemaLocation=\"http://www.mulesoft.org/schema/mule/http http://www.mulesoft.org/schema/mule/http/current/mule-http.xsd http://www.mulesoft.org/schema/mule/jms http://www.mulesoft.org/schema/mule/jms/current/mule-jms.xsd http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-current.xsd http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd\">\n");
		        	
		        	for(int count = 0;count < num;count++){
		        		String username = "jasper:jtaDemo-sample-" + count + ":1.0:jasperLab";
		        		String[] appInfo = username.split(":");			        
				        String vendor = appInfo[0];
				        String appName = appInfo[1];
				        String version = appInfo[2];
				        String deploymentId = appInfo[3];
		        		
		        		JTALicense lic = new JTALicense(vendor, appName, version, deploymentId, null, null, null, null);
				        lic.setLicenseKey(rsaEncrypt(lic.toString().getBytes(), privateKey));
						String password = JAuthHelper.bytesToHex(lic.getLicenseKey());
						
		        		sb.append("  <jms:activemq-connector name=\"con" + count + "\" specification=\"1.1\" username=\""+ username +"\" password=\"" + password + "\" brokerURL=\"${jasperEngineURL}\" validateConnections=\"true\" maxRedelivery=\"10\" doc:name=\"con" + count + "\" />\n");
		        	}
		        	
		        	sb.append("\n");
		        	
		        	for(int count = 0;count < num;count++){
		        		sb.append("  <flow name=\"jms" + count + "-flow\" doc:name=\"jms" + count + "-flow\"><jms:inbound-endpoint exchange-pattern=\"request-response\" queue=\"jms.jasper.jtaDemo-sample-" + count + ".1.0.jasperLab.admin.queue\" connector-ref=\"con" + count + "\" doc:name=\"jms" + count + "\"/><component class=\"org.jasper.testing.loaclcache.LocalCache\" doc:name=\"Java\"/></flow>\n");
		        	}
		        	
		        	sb.append("</mule>\n");
		        	
		        	System.out.print(sb);
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
