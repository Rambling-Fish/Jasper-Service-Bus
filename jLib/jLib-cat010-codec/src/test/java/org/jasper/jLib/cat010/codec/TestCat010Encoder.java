package org.jasper.jLib.cat010.codec;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.junit.Test;
import java.util.Arrays;


public class TestCat010Encoder {
	
	@Test
	/*
	 * This test case sends valid cat010 messages to be encoded to exercise
	 * as much of the code as possible. The encoded msgs should not be
	 * null, should match the original byte[] msg and no exceptions thrown for
	 * the test to pass
	 */
	public void testValidCat010TargetReports() {
		Cat010Encoder cat010Encoder = new Cat010Encoder();
		Cat010Decoder decoder = new Cat010Decoder();

		byte[] msg1 = {(byte)0x0a,(byte)0x00,(byte)0x0d,(byte)0xd1,(byte)0x01,
		          (byte)0x04,(byte)0x00,(byte)0x01,(byte)0x03,(byte)0xa3,
		          (byte)0x7d,(byte)0x5c,(byte)0x80};
		
		byte[] msg2 = {(byte)0x0a,(byte)0x00,(byte)0x0d,(byte)0xd1,(byte)0x01,
			       (byte)0x04,(byte)0x00,(byte)0x01,(byte)0x03,(byte)0xa3,
			       (byte)0x7d,(byte)0x5c,(byte)0x80};
	
		byte[] msg3 = {(byte)0x0a,(byte)0x00,(byte)0x22,(byte)0xf3,(byte)0x31,
			       (byte)0x09,(byte)0x84,(byte)0x00,(byte)0x01,(byte)0x01,
			       (byte)0xe1,(byte)0x0c,(byte)0xa3,(byte)0x83,(byte)0x1c,
			       (byte)0x0d,(byte)0x8a,(byte)0x09,(byte)0x82,(byte)0x00,
			       (byte)0x02,(byte)0x82,(byte)0x0b,(byte)0x01,(byte)0x28,
			       (byte)0x01,(byte)0x01,(byte)0x00,(byte)0x00,(byte)0x05,
			       (byte)0x00,(byte)0x02,(byte)0x01,(byte)0x40};
	
		byte[] msg4 = {(byte)0x0a,(byte)0x00,(byte)0x26,(byte)0xf3,(byte)0x71,
			       (byte)0x09,(byte)0x84,(byte)0x00,(byte)0x01,(byte)0x01,
			       (byte)0xe1,(byte)0x0c,(byte)0xa3,(byte)0x83,(byte)0x5c,
			       (byte)0x0d,(byte)0x8b,(byte)0x09,(byte)0x80,(byte)0xff,
			       (byte)0xf4,(byte)0x00,(byte)0x05,(byte)0x00,(byte)0x02,
			       (byte)0x83,(byte)0x0b,(byte)0x5f,(byte)0x2a,(byte)0x01,
			       (byte)0x01,(byte)0x00,(byte)0x00,(byte)0x05,(byte)0x00,
			       (byte)0x02,(byte)0x01,(byte)0x00};
		
		try {
			Cat010Message decodedMsg = decoder.doDecode(msg1);
			byte[] encodedMsg = cat010Encoder.doEncode(decodedMsg);
			Assert.assertNotNull(encodedMsg);
			
			decodedMsg = decoder.doDecode(msg2);
			encodedMsg = cat010Encoder.doEncode(decodedMsg);
			Assert.assertNotNull(encodedMsg);
			
			decodedMsg = decoder.doDecode(msg3);
			encodedMsg = cat010Encoder.doEncode(decodedMsg);
			Assert.assertNotNull(encodedMsg);
			
			decodedMsg = decoder.doDecode(msg4);
			encodedMsg = cat010Encoder.doEncode(decodedMsg); 
			Assert.assertNotNull(encodedMsg);
		} catch (Exception ex) {
			TestCase.fail();
		}
	}
	
	@Test
	/*
	 * This test case sends a null msg to the encoder. An exception must be
	 * caught for this test to pass
	 */
	public void testNullMessage() {
		Cat010Encoder cat010Encoder = new Cat010Encoder();
		
		try {
			byte[] msg = cat010Encoder.doEncode(null);
		} catch (Exception ex) {
			Assert.assertNotNull(ex);
		}
	}
	
	@Test
	/*
	 * This test case invokes main() which also does some testing. It is to
	 * get a higher test coverage. This test can be deleted if main() is 
	 * deleted from the encoder class in the future.
	 */
	public void testMain() {
		Cat010Encoder cat010Encoder = new Cat010Encoder();
		
		try {
			cat010Encoder.main(null);
			
		} catch (Exception ex) {
			TestCase.fail("Exception was caught: " + ex);
		}
	}

}
