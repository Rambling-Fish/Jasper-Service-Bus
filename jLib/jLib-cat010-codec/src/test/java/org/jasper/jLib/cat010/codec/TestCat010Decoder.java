package org.jasper.jLib.cat010.codec;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.jasper.jLib.cat010.transformer.ByteArrayOrCat010WithOriginalByteArrayToCat010Message;
import org.jasper.jLib.cat010.transformer.ByteArrayToCat010MessageWithOriginalByteArray;
import org.jasper.jLib.cat010.transformer.Cat010TargetReportToWebViewTrax;
import org.jasper.jLib.cat010.transformer.Cat010WithOriginalByteArrayToByteArray;
import org.junit.Test;


public class TestCat010Decoder {
	
	@Test
	/*
	 * This test case sends valid cat010 messages to be decoded to exercise
	 * as much of the code as possible. The decoded msgs should not be
	 * null and no exceptions thrown for the test to pass
	 */
	public void testValidCat010Messages() {
		Cat010Decoder cat010Decoder = new Cat010Decoder();
		
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
			       (byte)0x82,(byte)0x0b,(byte)0x5f,(byte)0x2a,(byte)0x01,
			       (byte)0x01,(byte)0x00,(byte)0x00,(byte)0x05,(byte)0x00,
			       (byte)0x02,(byte)0x01,(byte)0x00};
		byte[] msg5 = {(byte)0x0a,(byte)0x00,(byte)0x26,(byte)0xf3,(byte)0x71,
			       (byte)0x09,(byte)0x84,(byte)0x00,(byte)0x01,(byte)0x01,
			       (byte)0xe1,(byte)0x0a,(byte)0xa3,(byte)0x83,(byte)0x5c,
			       (byte)0x0d,(byte)0x8b,(byte)0x09,(byte)0x80,(byte)0xff,
			       (byte)0xf4,(byte)0x00,(byte)0x05,(byte)0x00,(byte)0x02,
			       (byte)0x83,(byte)0x0b,(byte)0x5f,(byte)0x2a,(byte)0x01,
			       (byte)0x01,(byte)0x00,(byte)0x00,(byte)0x05,(byte)0x00,
			       (byte)0x02,(byte)0x01,(byte)0x00};
		
		try { 
			Cat010Message decodedMsg = cat010Decoder.doDecode(msg1); 
			Assert.assertNotNull(decodedMsg);
			decodedMsg = null;
			decodedMsg = cat010Decoder.doDecode(msg2);
			Assert.assertNotNull(decodedMsg);
			decodedMsg = null;
			decodedMsg = cat010Decoder.doDecode(msg3);
			Assert.assertNotNull(decodedMsg);
			decodedMsg = null;
			decodedMsg = cat010Decoder.doDecode(msg4);
			Assert.assertNotNull(decodedMsg);
			decodedMsg = null;
			decodedMsg = cat010Decoder.doDecode(msg5);
			Assert.assertNotNull(decodedMsg);
		} catch (Exception ex) {
			TestCase.fail("Exception" + ex);
		}
	}
	
	@Test
	/*
	 * This test case sends an invalid message type in byte[0] - any value
	 * that is not 0x0a is not valid. An exception must be caught for the test
	 * to pass
	 */
	public void testInvalidCat010MsgType() {
		Cat010Decoder cat010Decoder = new Cat010Decoder();
		byte[] msg = {(byte)0x2a,(byte)0x00,(byte)0x0d,(byte)0xd1,(byte)0x01,
		          (byte)0x04,(byte)0x00,(byte)0x01,(byte)0x03,(byte)0xa3,
		          (byte)0x7d,(byte)0x5c,(byte)0x80};
		
		try {
			Cat010Message decodedMsg = cat010Decoder.doDecode(msg);
			TestCase.fail("Msg should not have been decoded");
		} catch (Exception ex) {
			Assert.assertNotNull(ex);
		}
	}
	
	@Test
	/*
	 * This test case sends a message that is too short. An exception must
	 * be caught for the test to pass
	 */
	public void testInvalidMsgLength() {
		Cat010Decoder cat010Decoder = new Cat010Decoder();
		byte[] msg = {(byte)0x0a,(byte)0x00,(byte)0x0d,(byte)0xd1,(byte)0x01,
		          (byte)0x04,(byte)0x00,(byte)0x01,(byte)0x03,(byte)0xa3};
		
		try {
			Cat010Message decodedMsg = cat010Decoder.doDecode(msg);
			TestCase.fail("Msg should not have been decoded");
		} catch (Exception ex) {
			Assert.assertNotNull(ex);
		}
	}
	
	@Test
	/*
	 * This test case sends an invalid FSPEC in the message. An exception must
	 * be caught for the test to pass
	 */
	public void testInvalidFSPec() {
		Cat010Decoder cat010Decoder = new Cat010Decoder();
		byte[] msg = {(byte)0x0a,(byte)0x00,(byte)0x0d,(byte)0xd1,(byte)0x01,
		          (byte)0x01,(byte)0x11,(byte)0x01,(byte)0x03,(byte)0xa3,
		          (byte)0x7d,(byte)0x5c,(byte)0x80};
		
		try {
			Cat010Message decodedMsg = cat010Decoder.doDecode(msg);
			TestCase.fail("Msg should not have been decoded");
		} catch (Exception ex) {
			Assert.assertNotNull(ex);
		}
	}
	
	@Test
	/*
	 * This test case sends an unsupported FSPEC value in the message. An
	 * exception must be caught for the test to pass
	 */
	public void testUnsupportedFSPec() {
		Cat010Decoder cat010Decoder = new Cat010Decoder();
		byte[] msg = {(byte)0x0a,(byte)0x00,(byte)0x0d,(byte)0xd1,(byte)0x01,
		          (byte)0x0b,(byte)0x00,(byte)0x01,(byte)0x03,(byte)0xa3,
		          (byte)0x7d,(byte)0x5c,(byte)0x80};
		
		try {
			Cat010Message decodedMsg = cat010Decoder.doDecode(msg);
			TestCase.fail("Msg should not have been decoded");
		} catch (Exception ex) {
			Assert.assertNotNull(ex);
		}
	}
	
	@Test
	/*
	 * This test case sends an unsupported status msg type - Start of Update
	 * Cycle Message.  Valid values are 1 and 3 only. This test sends a value
	 * of 0x02. An exception must be caught for the test to pass
	 */
	public void testUnsupportedUpdateCycleEvent() {
		Cat010Decoder cat010Decoder = new Cat010Decoder();
		byte[] msg = {(byte)0x0a,(byte)0x00,(byte)0x22,(byte)0xf3,(byte)0x31,
			       (byte)0x09,(byte)0x84,(byte)0x00,(byte)0x01,(byte)0x02,
			       (byte)0xe1,(byte)0x0c,(byte)0xa3,(byte)0x83,(byte)0x1c,
			       (byte)0x0d,(byte)0x8a,(byte)0x09,(byte)0x82,(byte)0x00,
			       (byte)0x02,(byte)0x82,(byte)0x0b,(byte)0x01,(byte)0x28,
			       (byte)0x01,(byte)0x01,(byte)0x00,(byte)0x00,(byte)0x05,
			       (byte)0x00,(byte)0x02,(byte)0x01,(byte)0x40};
		
		try {
			Cat010Message decodedMsg = cat010Decoder.doDecode(msg);
			TestCase.fail("Msg should not have been decoded");
		} catch (Exception ex) {
			Assert.assertNotNull(ex);
		}
	}
	
	@Test
	/*
	 * This test case sends an unsupported status msg type - Event-triggered
	 * Status Message.  Valid values are 1 and 3 only. This test sends a value
	 * of 0x04. An exception must be caught for the test to pass
	 */
	public void testUnsupportedEventStatusMessage() {
		Cat010Decoder cat010Decoder = new Cat010Decoder();
		byte[] msg = {(byte)0x0a,(byte)0x00,(byte)0x22,(byte)0xf3,(byte)0x31,
			       (byte)0x09,(byte)0x84,(byte)0x00,(byte)0x01,(byte)0x04,
			       (byte)0xe1,(byte)0x0c,(byte)0xa3,(byte)0x83,(byte)0x1c,
			       (byte)0x0d,(byte)0x8a,(byte)0x09,(byte)0x82,(byte)0x00,
			       (byte)0x02,(byte)0x82,(byte)0x0b,(byte)0x01,(byte)0x28,
			       (byte)0x01,(byte)0x01,(byte)0x00,(byte)0x00,(byte)0x05,
			       (byte)0x00,(byte)0x02,(byte)0x01,(byte)0x40};
		
		try {
			Cat010Message decodedMsg = cat010Decoder.doDecode(msg);
			TestCase.fail("Msg should not have been decoded");
		} catch (Exception ex) {
			Assert.assertNotNull(ex);
		}
	}
	
	@Test
	/*
	 * This test case sends an unknown status msg type.  Valid values are
	 * 1 and 3 only. This test sends a value of 7. An exception must be caught for
	 * the test to pass
	 */
	public void testUnknownEvent() {
		Cat010Decoder cat010Decoder = new Cat010Decoder();
		byte[] msg = {(byte)0x0a,(byte)0x00,(byte)0x22,(byte)0xf3,(byte)0x31,
			       (byte)0x09,(byte)0x84,(byte)0x00,(byte)0x01,(byte)0x07,
			       (byte)0xe1,(byte)0x0c,(byte)0xa3,(byte)0x83,(byte)0x1c,
			       (byte)0x0d,(byte)0x8a,(byte)0x09,(byte)0x82,(byte)0x00,
			       (byte)0x02,(byte)0x82,(byte)0x0b,(byte)0x01,(byte)0x28,
			       (byte)0x01,(byte)0x01,(byte)0x00,(byte)0x00,(byte)0x05,
			       (byte)0x00,(byte)0x02,(byte)0x01,(byte)0x40};
		
		try {
			Cat010Message decodedMsg = cat010Decoder.doDecode(msg);
			TestCase.fail();
		} catch (Exception ex) {
			Assert.assertNotNull(ex);
		}
	}
	
	@Test
	/*
	 * This test case sends a null msg to the decoder. The decoded msg must be
	 * null and an exception must not be caught for this test to pass
	 */
	public void testNullMessage() {
		Cat010Decoder cat010Decoder = new Cat010Decoder();
		
		try {
			Cat010Message decodedMsg = cat010Decoder.doDecode(null);
			Assert.assertNull(decodedMsg);
			
		} catch (Exception ex) {
			TestCase.fail("Exception was caught: " + ex);
		}
	}
	
	@Test
	/*
	 * This test case exercises the cat010 transformer classes
	 */
	public void testCat010Transformers() {
		Cat010Decoder cat010Decoder = new Cat010Decoder();
		byte[] msg = {(byte)0x0a,(byte)0x00,(byte)0x0d,(byte)0xd1,(byte)0x01,
		          (byte)0x04,(byte)0x00,(byte)0x01,(byte)0x03,(byte)0xa3,
		          (byte)0x7d,(byte)0x5c,(byte)0x80};
		
		ByteArrayOrCat010WithOriginalByteArrayToCat010Message cat010Transformer1 = new ByteArrayOrCat010WithOriginalByteArrayToCat010Message();
		ByteArrayToCat010MessageWithOriginalByteArray cat010Transformer2 = new ByteArrayToCat010MessageWithOriginalByteArray();
		Cat010TargetReportToWebViewTrax cat010Transformer3 = new Cat010TargetReportToWebViewTrax();
		Cat010WithOriginalByteArrayToByteArray cat010Transformer4 = new Cat010WithOriginalByteArrayToByteArray();
		Cat010TargetReport targetRpt = new Cat010TargetReport();
		Double coord = 1.5;
		
		try {
			// testing ByteArrayOrCat010WithOriginalByteArrayToCat010Message
			Cat010Message decodedMsg = cat010Decoder.doDecode(msg);
			Cat010MessageWithOriginalByteArray src = new Cat010MessageWithOriginalByteArray(decodedMsg, msg);
			src.getMsg();
			src.getOringalByteArray();
			Assert.assertNotNull(decodedMsg);
			cat010Transformer1.transform(src);
			cat010Transformer1.transform(msg);
			cat010Transformer1.transform("test");
			
			// testing ByteArrayToCat010MessageWithOriginalByteArray
			cat010Transformer2.transform(msg);
			cat010Transformer2.transform("test");
			
			// testing Cat010TargetReportToWebViewTrax
			targetRpt.setCalTrackVelocityInCartesianCoordX(coord);
			targetRpt.setPositionInCartesianCoordX(1234);
			targetRpt.setPositionInCartesianCoordY(345);
			targetRpt.setTrackNumber(123);
			targetRpt.setTimeOfDay(coord);
			cat010Transformer3.transform(targetRpt);
			targetRpt.setCalTrackVelocityInCartesianCoordX(coord);
			targetRpt.setCalTrackVelocityInCartesianCoordY(coord);
			cat010Transformer3.transform(targetRpt);
			cat010Transformer3.transform("test");
			
			// testing Cat010WithOriginalByteArrayToByteArray
			cat010Transformer4.transform(src);
			cat010Transformer4.transform("test");
			
			// testing Cat010TargetReportToWebViewTrax to invoke a null ptr exception
			targetRpt = null;
			cat010Transformer3.transform(targetRpt);
		} catch (Exception ex) {
			Assert.assertNotNull(ex);
		}
	}
	
	@Test
	/*
	 * This test case invokes main() which also does some testing. It is to
	 * get a higher test coverage. This test can be deleted if main() is 
	 * deleted from the decoder class in the future.
	 */
	public void testMain() {
		Cat010Decoder cat010Decoder = new Cat010Decoder();
		
		try {
			cat010Decoder.main(null);
			
		} catch (Exception ex) {
			TestCase.fail("Exception was caught: " + ex);
		}
	}

}
