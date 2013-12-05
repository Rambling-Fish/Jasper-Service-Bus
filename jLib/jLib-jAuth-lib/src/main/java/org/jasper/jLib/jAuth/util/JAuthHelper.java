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

import org.jasper.jLib.jAuth.ClientLicense;
import org.jasper.jLib.jAuth.UDELicense;
public class JAuthHelper {
	
	private static final String PUBLIC_KEY_FILENAME = "jasper.rsa.public.key";

	public static final String CLIENT_LICENSE_FILE_SUFFIX = "-clnt-license.key";
	public static final String UDE_LICENSE_FILE_SUFFIX = "-ude-license.key";
	
	public static byte[] rsaDecrypt(byte[] data,PublicKey pubKey) throws Exception{
		  Cipher cipher = Cipher.getInstance("RSA");
		  cipher.init(Cipher.DECRYPT_MODE, pubKey);
		  byte[] cipherData = cipher.doFinal(data);
		  return cipherData;
	}

	public static ClientLicense loadClientLicenseFromFile(String filename) throws IOException {
		InputStream in = new FileInputStream(filename);
		ObjectInputStream oin = new ObjectInputStream(new BufferedInputStream(in));
		
		try {
			ClientLicense lic = (ClientLicense) oin.readObject();
			return lic;
		} catch (Exception e) {
		    throw new RuntimeException("Spurious serialisation error", e);
		} finally {
			oin.close();
		}
		
	}
	
	public static UDELicense loadUDELicenseFromFile(String filename) throws IOException {
		InputStream in = new FileInputStream(filename);
		ObjectInputStream oin = new ObjectInputStream(new BufferedInputStream(in));
		try {
			UDELicense lic = (UDELicense) oin.readObject();
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
	
	public static String bytesToHex(byte[] bytes) {
	    final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
	    char[] hexChars = new char[bytes.length * 2];
	    int v;
	    for ( int j = 0; j < bytes.length; j++ ) {
	        v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
	
	public static byte[] hexToBytes(String hex) {
	    final String hexArray = "0123456789ABCDEF";
	    byte[] byteArray = new byte[hex.length()/2];
	    char[] hexChars = hex.toUpperCase().toCharArray();
	    int v;
	    for ( int j = 0; j < hex.length(); j += 2 ) {
	        v = hexArray.indexOf(hexChars[j]) & 0x0F;
	        v = v << 4;
	        v = v | (hexArray.indexOf(hexChars[j+1]) & 0x0F);
	        byteArray[j/2] = (byte)v;
	    }
	    return byteArray;
	}
}
