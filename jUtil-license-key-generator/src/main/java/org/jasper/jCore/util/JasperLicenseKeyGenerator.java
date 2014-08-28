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

import javax.crypto.Cipher;

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
	
	public static void saveClientLicenseToFile(String path, ClientLicense license) throws IOException {
		FileOutputStream fio = new FileOutputStream(path + license.getVendor() + "_" + license.getAppName() + "_" + license.getVersion() + JAuthHelper.CLIENT_LICENSE_FILE_SUFFIX);
		ObjectOutputStream oout = new ObjectOutputStream(new BufferedOutputStream(fio));
		try {
			if (license.getType().equals("jsc")){
				StringBuilder sbl = new StringBuilder();
				sbl.append("jsc.username = "+ license.getVendor() + ":" + license.getAppName() + ":" + license.getVersion() + ":" + license.getDeploymentId());
				sbl.append("\n");
				sbl.append("jsc.password =" + JAuthHelper.bytesToHex(license.getLicenseKey()));
				sbl.append("\n");
				sbl.append("jsc.transport=failover://(tcp://0.0.0.0:61616)?timeout=3000");
				sbl.append("\n");
				sbl.append("jsc.timeout=6000");
				
				oout.writeObject(sbl.toString().getBytes());
				
			} else oout.writeObject(license);
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
		        		                   "\ttest - generate testing mule app\n" +
		        		                   "\t:");
		        input = in.readLine();
		        input = input.toLowerCase();
		        if (input.startsWith("client")){
		        	
		        	System.out.println("{\"type\": \"<dta/jsc(Str)>\", \"instance\": \"<instance(int)>\", \"vendor\": \"<vendor(Str)>\", \"appName\": \"<dta/jsc name(Str)>\",\"version\": \"<dta/jsc version(Str)>\", ");
		        	System.out.println("\"numOfPublishers\": <number(int)>, \"numOfConsumers\": <number(int)>, \"adminQueue\": \"<queueName(Str)>\",\"deploymentId\": \"<deploymentName(Str)>\", ");
		        	System.out.println("\"expiry\":{\"year\":<number(int)>,\"month\":<number(int)>,\"dayOfMonth\":<number(int)>,\"hourOfDay\":<number(int)>,\"minute\":<number(int)>,\"second\":<number(int)>, ");
		        	System.out.println("\"ntpHost\":\"<time.nrc.ca/optional>\", \"ntpPort\":\"<number(int)/optional>\" }");
		        	input = in.readLine();
			        if(input == null) break;
			        Gson gson = new GsonBuilder().setVersion(2.1).create();
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
		        	System.out.println("Format: [vendor_AppName_Version(jasper_demo-EMR-C_1.0)]");
		        	System.out.print("Enter Client's name for license key validation = ");
			        input = in.readLine();
			        String filename = LICENSE_FILE_PATH + input + JAuthHelper.CLIENT_LICENSE_FILE_SUFFIX;
			        System.out.println("Getting: " + filename);
			        if(doesFileExist(filename)){
			        	ClientLicense lic = JAuthHelper.loadClientLicenseFromFile(filename);
			        	System.out.println("license info   = " + lic);
			        	System.out.println("decrypted info = " + new String(JAuthHelper.rsaDecrypt(lic.getLicenseKey(), publicKey)));
			        	if (lic.toString().equals(new String(JAuthHelper.rsaDecrypt(lic.getLicenseKey(), publicKey)))) {
			        		System.out.println("______________________");
			        		System.out.println("         VALID!");
			        		System.out.println("______________________");
			        	}
			        	else {
			        		System.out.println("______________________");
			        		System.out.println("        NOT VALID");
			        		System.out.println("______________________");
			        	}
			        }else{
		        		System.out.println("license file doesn't exist");
		        	}
			        
		        }else if (input.startsWith("ude")){
		        	
		        	System.out.println("{\"type\":\"ude(Str)\",\"version\":\"<ude version(Str)>\",\"deploymentId\":\"<deploymentName(Str)>\",\"instanceId\":<instance(int)>,\"numOfPublishers\":<number(int)>,\"numOfConsumers\":<number(int)>,");
		        	System.out.println("\"aclEnabled\":{<boolean(true/false)>,\"expiry\":{\"year\":<number(int)>,\"month\":<number(int)>,\"dayOfMonth\":<number(int)>,\"hourOfDay\":<number(int)>,\"minute\":<number(int)>,\"second\":<number(int)>}, ");
		        	System.out.println("\"ntpHost\" : \"<hostName/Optional>\", \"ntpPort\": <number(int)/Optional>}");
		        	input = in.readLine();
			        if(input == null) break;
			        
			        Gson gson = new GsonBuilder().setVersion(2.1).create();
					UDELicense lic = gson.fromJson(input, UDELicense.class);
					if(lic == null) break;
					System.out.println("JSON :" + gson.toJson(lic));
					System.out.println("license info   = " + lic);
					lic.setLicenseKey(rsaEncrypt( gson.toJson(lic).getBytes(), privateKey));
			        saveUDELicenseToFile(LICENSE_FILE_PATH, lic);
			        System.out.println("File created : " + LICENSE_FILE_PATH + lic.getDeploymentId() + "-" + lic.getInstanceId() + JAuthHelper.UDE_LICENSE_FILE_SUFFIX);
			        
		        }else if (input.startsWith("vude")){
		        	System.out.println("Format: [DeploymentID-Instance(jasperLab-0)]");
		        	System.out.print("Enter UDE's name for license key validation = ");
			        input = in.readLine();
			        String filename = LICENSE_FILE_PATH + input + JAuthHelper.UDE_LICENSE_FILE_SUFFIX;
			        if(doesFileExist(filename)){
			        	UDELicense lic = JAuthHelper.loadUDELicenseFromFile(filename);
			        	System.out.println("license info   = " + lic);
			        	System.out.println("decrypted info = " + new String(JAuthHelper.rsaDecrypt(lic.getLicenseKey(), publicKey)));
			        	if (lic.toString().equals(new String(JAuthHelper.rsaDecrypt(lic.getLicenseKey(), publicKey)))){
			        		System.out.println("______________________");
			        		System.out.println("         VALID!");
			        		System.out.println("______________________");
			        	}
			        	else {
			        		System.out.println("______________________");
			        		System.out.println("        NOT VALID");
			        		System.out.println("______________________");
			        	}
			        }else{
		        		System.out.println("license file doesn't exist");
		        	}
			        
		        } else if(input.startsWith("test")){
		        	System.out.print("Number of connections: ");
		        	int num = Integer.parseInt(in.readLine());
		        	
		        	StringBuilder sb = new StringBuilder();
		        	sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + 
		        			  "<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\" xmlns:http=\"http://www.mulesoft.org/schema/mule/http\" xmlns:jms=\"http://www.mulesoft.org/schema/mule/jms\" xmlns:doc=\"http://www.mulesoft.org/schema/mule/documentation\" xmlns:spring=\"http://www.springframework.org/schema/beans\" xmlns:core=\"http://www.mulesoft.org/schema/mule/core\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" version=\"CE-3.3.0\" xsi:schemaLocation=\"http://www.mulesoft.org/schema/mule/http http://www.mulesoft.org/schema/mule/http/current/mule-http.xsd http://www.mulesoft.org/schema/mule/jms http://www.mulesoft.org/schema/mule/jms/current/mule-jms.xsd http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-current.xsd http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd\">\n");
		        	
		        	for(int count = 0;count < num;count++){
		        		String username = "jasper:jtaDemo-sample-" + count + ":1.0:jasperLab";
		        		String licString = "{\"type\": \"dta\", \"instance\": 0, \"vendor\": \"jasper\", \"appName\": \"dtaDemo-sample-"+ count + "\", \"version\": \"1.0\", \"numOfPublishers\": " + num + ", \"numOfConsumers\": 1, \"adminQueue\": \"jms.jasper.jtaDemo-sample-" + count +".1.0.jasperLab.admin.queue\", \"deploymentId\": \"jasperLab\"}";
		        		Gson gson = new GsonBuilder().setVersion(2.1).create();
						ClientLicense lic = gson.fromJson(licString, ClientLicense.class);
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
