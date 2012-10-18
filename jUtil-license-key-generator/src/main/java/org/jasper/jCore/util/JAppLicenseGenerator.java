package org.jasper.jCore.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
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
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.crypto.Cipher;

import org.apache.commons.codec.digest.DigestUtils;
import org.jasper.jLib.jAuth.JAppLicense;
import org.jasper.jLib.jAuth.JCoreLicense;

public class JAppLicenseGenerator {
	
	private static final String PATH_SEPARATOR = System.getProperty("file.separator");
	private static final String PUBLIC_KEY_FILENAME = "keystore" + PATH_SEPARATOR + "jasper.rsa.public.key";
	private static final String PRIVATE_KEY_FILENAME = "keystore" + PATH_SEPARATOR + "jasper.rsa.private.key";
	private static final String LICENSE_FILE_PREFIX = "licenseKeys" + PATH_SEPARATOR;
	private static final String LICENSE_FILE_SUFFIX = "-license.key";

	public static byte[] rsaEncrypt(byte[] data,PrivateKey prvKey) throws Exception{
		  Cipher cipher = Cipher.getInstance("RSA");
		  cipher.init(Cipher.ENCRYPT_MODE, prvKey);
		  byte[] cipherData = cipher.doFinal(data);
		  return cipherData;
	}
	
	public static byte[] rsaDecrypt(byte[] data,PublicKey pubKey) throws Exception{
		  Cipher cipher = Cipher.getInstance("RSA");
		  cipher.init(Cipher.DECRYPT_MODE, pubKey);
		  byte[] cipherData = cipher.doFinal(data);
		  return cipherData;
	}
	
	public static void generateKeys() throws Exception{
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
		kpg.initialize(2048);
		KeyPair kp = kpg.genKeyPair();
	
		KeyFactory fact = KeyFactory.getInstance("RSA");
		RSAPublicKeySpec pub = fact.getKeySpec(kp.getPublic(), RSAPublicKeySpec.class);
		RSAPrivateKeySpec priv = fact.getKeySpec(kp.getPrivate(), RSAPrivateKeySpec.class);

		saveRSAKeyToFile(PUBLIC_KEY_FILENAME, pub.getModulus(), pub.getPublicExponent());
		saveRSAKeyToFile(PRIVATE_KEY_FILENAME, priv.getModulus(), priv.getPrivateExponent());
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
	
	private static void saveJAppLicenseToFile(JAppLicense license) throws IOException {
		FileOutputStream fio = new FileOutputStream(LICENSE_FILE_PREFIX + license.getAppName() + LICENSE_FILE_SUFFIX);
		ObjectOutputStream oout = new ObjectOutputStream(new BufferedOutputStream(fio));
		try {
			oout.writeObject(license);
	    } catch (Exception e) {
	    	throw new IOException("Unexpected error", e);
		} finally {
		    oout.close();
		}
	}
	
	private static JAppLicense loadJAppLicenseFromFile(String filename) throws IOException {
		InputStream in = new FileInputStream(filename);
		ObjectInputStream oin = new ObjectInputStream(new BufferedInputStream(in));
		try {
			JAppLicense lic = (JAppLicense) oin.readObject();
			return lic;
		} catch (Exception e) {
		    throw new RuntimeException("Spurious serialisation error", e);
		} finally {
			oin.close();
		}
	}
	
	private static void saveJCoreLicenseToFile(JCoreLicense license) throws IOException {
		FileOutputStream fio = new FileOutputStream(LICENSE_FILE_PREFIX + license.getDeploymentId() + LICENSE_FILE_SUFFIX);
		ObjectOutputStream oout = new ObjectOutputStream(new BufferedOutputStream(fio));
		try {
			oout.writeObject(license);
	    } catch (Exception e) {
	    	throw new IOException("Unexpected error", e);
		} finally {
		    oout.close();
		}
	}
	
	private static JCoreLicense loadJCoreLicenseFromFile(String filename) throws IOException {
		InputStream in = new FileInputStream(filename);
		ObjectInputStream oin = new ObjectInputStream(new BufferedInputStream(in));
		try {
			JCoreLicense lic = (JCoreLicense) oin.readObject();
			return lic;
		} catch (Exception e) {
		    throw new RuntimeException("Spurious serialisation error", e);
		} finally {
			oin.close();
		}
	}
		
	public static PublicKey getPublicKeyFromFile() throws IOException {
		InputStream in = new FileInputStream(PUBLIC_KEY_FILENAME);
		ObjectInputStream oin = new ObjectInputStream(new BufferedInputStream(in));
		try {
			BigInteger m = (BigInteger) oin.readObject();
			BigInteger e = (BigInteger) oin.readObject();
			RSAPublicKeySpec keySpec = new RSAPublicKeySpec(m, e);
			KeyFactory fact = KeyFactory.getInstance("RSA");
			PublicKey pubKey = fact.generatePublic(keySpec);
			return pubKey;
		} catch (Exception e) {
		    throw new RuntimeException("Spurious serialisation error", e);
		} finally {
			oin.close();
		}
	}
	
	public static PrivateKey getPrivateKeyFromFile() throws IOException {
		InputStream in = new FileInputStream(PRIVATE_KEY_FILENAME);
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
	    
		try {
			
			PrivateKey privateKey = getPrivateKeyFromFile();
			PublicKey publicKey = getPublicKeyFromFile();
			
		    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		    String input = "";
		    while (input != null) {
		        System.out.print("Options : \n" +
		        		                   "\tga - generate jApp lisenceFile\n" +
		        		                   "\tva - validate jApp lisenceFile\n" +
		        		                   "\tgc - generate jCore lisenceFile\n" +
		        		                   "\tvc - validate jCore lisenceFile\n" +
		        		                   "\t:");
		        input = in.readLine();
		        input = input.toLowerCase();
		        if(input.startsWith("ga")){
		        	System.out.println("jasper:sampleApp:1.0:jasperLab:2012-12-25");
		        	System.out.print("Enter jApp info <vendor>:<app_name>:<version>:<deployment_id>:<expiry yyyy-mm-dd>:<ntp_host>:<ntp_port> ; expiry and ntp info optional = \n");
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
			        
			        JAppLicense lic = new JAppLicense(vendor, appName, version, deploymentId, expiry, ntpHost, ntpPort, null);
			        lic.setLicenseKey(rsaEncrypt(lic.toString().getBytes(), privateKey));
			        saveJAppLicenseToFile(lic);
			        System.out.println("File created : " + LICENSE_FILE_PREFIX + lic.getAppName() + LICENSE_FILE_SUFFIX);
		        }else if (input.startsWith("va")){
		        	System.out.print("Enter name of jApp for jApp lisence file validation = ");
			        input = in.readLine();
			        String filename = LICENSE_FILE_PREFIX + input + LICENSE_FILE_SUFFIX;
			        if(doesFileExist(filename)){
			        	JAppLicense lic = loadJAppLicenseFromFile(filename);
			        	System.out.println("lisence info   = " + lic);
			        	System.out.println("decrypted info = " + new String(rsaDecrypt(lic.getLicenseKey(), publicKey)));
			        }else{
		        		System.out.println("lisence file doesn't exist");
		        	}
		        }else if (input.startsWith("gc")){
		        	System.out.println("jasperLab:2012-12-25");
		        	System.out.print("Enter jCore info <deployment_id>:<expiry yyyy-mm-dd>:<ntp_host>:<ntp_port> ; expiry and ntp info optional = \n");
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
			        
			        JCoreLicense lic = new JCoreLicense(deploymentId, expiry, ntpHost, ntpPort, null);
			        lic.setLicenseKey(rsaEncrypt(lic.toString().getBytes(), privateKey));
			        saveJCoreLicenseToFile(lic);
			        System.out.println("File created : " + LICENSE_FILE_PREFIX + lic.getDeploymentId() + LICENSE_FILE_SUFFIX);
		        }else if (input.startsWith("vc")){
		        	System.out.print("Enter the deploymentID for jCore lisence file validation = ");
			        input = in.readLine();
			        String filename = LICENSE_FILE_PREFIX + input + LICENSE_FILE_SUFFIX;
			        if(doesFileExist(filename)){
			        	JCoreLicense lic = loadJCoreLicenseFromFile(filename);
			        	System.out.println("lisence info   = " + lic);
			        	System.out.println("decrypted info = " + new String(rsaDecrypt(lic.getLicenseKey(), publicKey)));
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
