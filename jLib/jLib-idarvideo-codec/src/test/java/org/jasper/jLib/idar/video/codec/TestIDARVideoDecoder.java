package org.jasper.jLib.idar.video.codec;


import junit.framework.Assert;
import junit.framework.TestCase;

import org.junit.Test;
import java.io.*;
import org.jasper.jLib.idar.video.transformer.*;


public class TestIDARVideoDecoder {
	
	@Test
	/*
	 * This test case sends valid video messages to be decoded to exercise
	 * as much of the code as possible. The decoded msgs should not be
	 * null and no exceptions thrown for the test to pass
	 */
	public void testValidVideoMessages() {
		IDARVideoDecoder videoDecoder = new IDARVideoDecoder();
		String strBytes;
		StringBuffer buff = new StringBuffer();
		
		try { 
			BufferedReader br  = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/IDARTestInput.txt")));
			while((strBytes = br.readLine()) != null) {
				buff.append(strBytes);
			}
			byte[] msg = buff.toString().getBytes();
			
			IDARVideoMessage decodedMsg = videoDecoder.doDecode(msg);
			decodedMsg.getIDARjpeg();
			decodedMsg.getIntelliDARinfo();
			Assert.assertNotNull(decodedMsg); 
		} catch (Exception ex) {
			TestCase.fail("Exception" + ex);
		}
	}
	
	@Test
	/*
	 * This test case sends a null msg to the decoder. The decoded msg must be
	 * null and an exception must not be caught for this test to pass
	 */
	public void testNullMessage() {
		IDARVideoDecoder videoDecoder = new IDARVideoDecoder();
		
		try {
			IDARVideoMessage decodedMsg = videoDecoder.doDecode(null);
			Assert.assertNull(decodedMsg);
			
		} catch (Exception ex) {
			TestCase.fail("Exception was caught: " + ex);
		}
	}
	
	@Test
	/*
	 * This test case exercises the IDAR transformer classes
	 */
	public void testIDARTransformers() {
		ByteArrayToIDARVideoMessage idarTransformer = new ByteArrayToIDARVideoMessage();
		IDARtoJPEG idarJpeg = new IDARtoJPEG();
		IDARtoXML idarXml   = new IDARtoXML();
		String strBytes;
		StringBuffer buff = new StringBuffer();
		
		try { 
			BufferedReader br  = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/IDARTestInput.txt")));
			while((strBytes = br.readLine()) != null) {
				buff.append(strBytes);
			}
			byte[] msg = buff.toString().getBytes();
			Object validObj = idarTransformer.transform(msg);
			Object myObj = idarJpeg.transform(validObj);
			myObj = idarXml.transform(validObj);
			validObj = idarTransformer.transform("this is a test");
			myObj = idarJpeg.transform(validObj);
			myObj = idarXml.transform(validObj);
		} catch (Exception ex) {
			TestCase.fail("Exception: " + ex);
		}
	}
	
	@Test
	/*
	 * This test case exercises the IDARTcpProtocol class
	 */
	public void testIDARTcpProtocol() {
		IDARTcpProtocol tcpProtocol = new IDARTcpProtocol();
		BufferedReader br  = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/IDARTestInput.txt")));
		String strBytes;
		StringBuffer buff = new StringBuffer();
		byte[] msg2 = {(byte)0x55,(byte)0xAA,(byte)0x55,(byte)0xAA,(byte)0xAA,(byte)0x55,(byte)0xAA,(byte)0x55};
		try {
		while((strBytes = br.readLine()) != null) {
			buff.append(strBytes);
		}
		byte[] msg = buff.toString().getBytes();
			InputStream is = new ByteArrayInputStream(msg);
			tcpProtocol.read(is);
			InputStream is2 = new ByteArrayInputStream(msg2);
			tcpProtocol.read(is2);
		} catch (IOException io) {
			TestCase.fail("Exception: " + io);
		}
	}
	
}
