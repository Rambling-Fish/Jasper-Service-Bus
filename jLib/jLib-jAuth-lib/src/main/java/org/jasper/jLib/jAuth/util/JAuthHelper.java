package org.jasper.jLib.jAuth.util;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;

import javax.crypto.Cipher;

import org.jasper.jLib.jAuth.JSBLicense;
import org.jasper.jLib.jAuth.JTALicense;

public class JAuthHelper {
	
	private static final String PUBLIC_KEY_FILENAME = "jasper.rsa.public.key";

	public static final String JTA_LICENSE_FILE_SUFFIX = "-jta-license.key";
	public static final String JSB_LICENSE_FILE_SUFFIX = "-jsb-license.key";
	
	public static byte[] rsaDecrypt(byte[] data,PublicKey pubKey) throws Exception{
		  Cipher cipher = Cipher.getInstance("RSA");
		  cipher.init(Cipher.DECRYPT_MODE, pubKey);
		  byte[] cipherData = cipher.doFinal(data);
		  return cipherData;
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

}
