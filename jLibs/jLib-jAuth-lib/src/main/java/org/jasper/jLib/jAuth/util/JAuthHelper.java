package org.jasper.jLib.jAuth.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

import org.jasper.jLib.jAuth.JTALicense;
import org.jasper.jLib.jAuth.JSBLicense;

public class JAuthHelper {
	
	private static final String PUBLIC_KEY_FILENAME = "jasper.rsa.public.key";
	private static final String PRIVATE_KEY_FILENAME = "jasper.rsa.private.key";

	public static final String JTA_LICENSE_FILE_SUFFIX = "-jta-license.key";
	public static final String JSB_LICENSE_FILE_SUFFIX = "-jsb-license.key";
	
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
	
	public static void generateKeys(String path) throws Exception{
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
		kpg.initialize(2048);
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
		FileOutputStream fio = new FileOutputStream(path + license.getAppName() + JTA_LICENSE_FILE_SUFFIX);
		ObjectOutputStream oout = new ObjectOutputStream(new BufferedOutputStream(fio));
		try {
			oout.writeObject(license);
	    } catch (Exception e) {
	    	throw new IOException("Unexpected error", e);
		} finally {
		    oout.close();
		}
	}
	
	public static JTALicense loadJTALicenseFromFile(String filename) throws IOException {
		InputStream in = new FileInputStream(filename);
		ObjectInputStream oin = new ObjectInputStream(new BufferedInputStream(in));
		try {
			JTALicense lic = (JTALicense) oin.readObject();
			return lic;
		} catch (Exception e) {
		    throw new RuntimeException("Spurious serialisation error", e);
		} finally {
			oin.close();
		}
	}
	
	public static void saveJSBLicenseToFile(String path, JSBLicense license) throws IOException {
		FileOutputStream fio = new FileOutputStream(path + license.getDeploymentId() + JSB_LICENSE_FILE_SUFFIX);
		ObjectOutputStream oout = new ObjectOutputStream(new BufferedOutputStream(fio));
		try {
			oout.writeObject(license);
	    } catch (Exception e) {
	    	throw new IOException("Unexpected error", e);
		} finally {
		    oout.close();
		}
	}
	
	public static JSBLicense loadJSBLicenseFromFile(String filename) throws IOException {
		InputStream in = new FileInputStream(filename);
		ObjectInputStream oin = new ObjectInputStream(new BufferedInputStream(in));
		try {
			JSBLicense lic = (JSBLicense) oin.readObject();
			return lic;
		} catch (Exception e) {
		    throw new RuntimeException("Spurious serialisation error", e);
		} finally {
			oin.close();
		}
	}
		
	public static PublicKey getPublicKeyFromFile(String path) throws IOException {
		InputStream in = new FileInputStream(path + PUBLIC_KEY_FILENAME);
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

}
