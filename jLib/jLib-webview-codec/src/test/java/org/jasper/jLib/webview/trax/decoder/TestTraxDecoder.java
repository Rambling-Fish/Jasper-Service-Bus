package org.jasper.jLib.webview.trax.decoder;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.jasper.jLib.webview.trax.decoder.WebViewTraxMessage.TargetType;
import org.jasper.jLib.webview.trax.decoder.WebViewTraxMessage.TrackInfoSize;
import org.jasper.jLib.webview.trax.decoder.WebViewTraxMessage.TrackInfoType;
import org.jasper.jLib.webview.trax.decoder.WebViewTraxMessage.TrackInfoVmi;
import org.jasper.jLib.webview.trax.transformer.*;

import org.junit.Test;


public class TestTraxDecoder {
	
	@Test
	/*
	 * This testcase sends a valid Trax message to be decoded. The resultant WebViewTrax message
	 * should contain 10 decoded rows for the testcase to be successful
	 */
	public void testValidTraxMessage() {
		WebViewTraxDecoder traxDecoder = new WebViewTraxDecoder();
		String msg = 
			"Content-type: text/plain; charset=ISO-8859-1\n" +
			"\n" +
			"T,0,0,trax|ack('trax')\n" +
			"starttrax(10,74571,0,36,10233,12799,12811,12829,12843,12849,12850,12853)\n" +
			"trax(1,233,10233,74571,ZZZZ,ZZZZ,ZZZZ,ZZZZ,-812,1207,0,False,0,False,False,64,False,0,0,1,-1,0,5,0)\n" +
			"trax(1,,12799,74571,ZZZZ,ZZZZ,ZZZZ,ZZZZ,3092,13719,2713,False,76,False,False,8,False,0,1,3,36,127,5,0)\n" +
			"trax(1,811,12811,74571,ZZZZ,ZZZZ,ZZZZ,ZZZZ,-898,2027,0,False,0,False,False,64,False,0,0,1,-1,0,5,0)\n" +
			"trax(1,,12829,74571,ZZZZ,ZZZZ,ZZZZ,ZZZZ,757,202,2419,False,31,False,False,64,False,0,0,3,307,9,1,0)\n" +
			"trax(1,843,12843,74571,ZZZZ,ZZZZ,ZZZZ,ZZZZ,1878,977,0,False,0,False,False,64,False,0,0,1,57,18,5,0)\n" +
			"trax(1,849,12849,74571,ZZZZ,ZZZZ,ZZZZ,ZZZZ,1424,744,0,False,0,False,False,64,False,0,0,1,-1,0,5,0)\n" +
			"trax(1,,12850,74571,ZZZZ,ZZZZ,ZZZZ,ZZZZ,10200,26661,151,False,59,False,False,8,False,0,1,1,266,169,5,0)\n" +
			"trax(1,853,12853,74571,ZZZZ,ZZZZ,ZZZZ,ZZZZ,1626,815,0,False,0,False,False,64,False,0,0,1,56,21,5,0)\n" +
			"trax(0,973,16973,72614)\n" +
			"trax(0,,13718,58585)\n" +
			"endtrax(8,74571)"; 
		
		WebViewTrax trax = traxDecoder.doDecode(msg);
		Assert.assertEquals(10, trax.getTrax().length);
	}
	
	@Test
	/*
	 * This testcase sends an invalid Trax message to be decoded. The number of rows in the starttrax header
	 * does not match the actual number of trax rows. The resultant WebViewTrax message should contain 0 decoded
	 * rows for the testcase to be successful
	 */
	public void testIncorrectNumberOfRows() {
		WebViewTraxDecoder traxDecoder = new WebViewTraxDecoder();
		String msg = 
			"Content-type: text/plain; charset=ISO-8859-1\n" +
			"\n" +
			"T,0,0,trax|ack('trax')\n" +
			"starttrax(6,74571,0,36,10233,12799,12811,12829,12843,12849,12850,12853)\n" +
			"trax(1,233,10233,74571,ZZZZ,ZZZZ,ZZZZ,ZZZZ,-812,1207,0,False,0,False,False,64,False,0,0,1,-1,0,5,0)\n" +
			"trax(1,,12799,74571,ZZZZ,ZZZZ,ZZZZ,ZZZZ,3092,13719,2713,False,76,False,False,8,False,0,1,3,36,127,5,0)\n" +
			"trax(1,811,12811,74571,ZZZZ,ZZZZ,ZZZZ,ZZZZ,-898,2027,0,False,0,False,False,64,False,0,0,1,-1,0,5,0)\n" +
			"trax(0,973,16973,72614)\n" +
			"trax(0,,13718,58585)\n" +
			"endtrax(8,74571)"; 
		
		WebViewTrax trax = traxDecoder.doDecode(msg);
		Assert.assertEquals(0, trax.getTrax().length);
	}
	
	@Test
	/*
	 * This testcase sends an invalid Trax message to be decoded. The number of rows in the starttrax header
	 * is missing. The resultant WebViewTrax message should contain 0 decoded
	 * rows for the testcase to be successful
	 */
	public void testMissingNumberOfRows() {
		WebViewTraxDecoder traxDecoder = new WebViewTraxDecoder();
		String msg = 
			"Content-type: text/plain; charset=ISO-8859-1\n" +
			"\n" +
			"T,0,0,trax|ack('trax')\n" +
			"starttrax(,74571,0,36,10233,12799,12811,12829,12843,12849,12850,12853)\n" +
			"trax(1,233,10233,74571,ZZZZ,ZZZZ,ZZZZ,ZZZZ,-812,1207,0,False,0,False,False,64,False,0,0,1,-1,0,5,0)\n" +
			"trax(1,,12799,74571,ZZZZ,ZZZZ,ZZZZ,ZZZZ,3092,13719,2713,False,76,False,False,8,False,0,1,3,36,127,5,0)\n" +
			"trax(1,811,12811,74571,ZZZZ,ZZZZ,ZZZZ,ZZZZ,-898,2027,0,False,0,False,False,64,False,0,0,1,-1,0,5,0)\n" +
			"trax(0,973,16973,72614)\n" +
			"trax(0,,13718,58585)\n" +
			"endtrax(8,74571)"; 
		
		WebViewTrax trax = traxDecoder.doDecode(msg);
		Assert.assertEquals(0, trax.getTrax().length);
	}
	
	@Test
	/*
	 * The number of rows in the starttrax header is invalid (non-numeric). The resultant WebViewTrax
	 * message should contain 0 decoded rows for the testcase to be successful
	 */
	public void testNonNumericNumberOfRows() {
		WebViewTraxDecoder traxDecoder = new WebViewTraxDecoder();
		String msg = 
			"Content-type: text/plain; charset=ISO-8859-1\n" +
			"\n" +
			"T,0,0,trax|ack('trax')\n" +
			"starttrax(Garbage,74571,0,36,10233,12799,12811,12829,12843,12849,12850,12853)\n" +
			"trax(1,233,10233,74571,ZZZZ,ZZZZ,ZZZZ,ZZZZ,-812,1207,0,False,0,False,False,64,False,0,0,1,-1,0,5,0)\n" +
			"trax(1,,12799,74571,ZZZZ,ZZZZ,ZZZZ,ZZZZ,3092,13719,2713,False,76,False,False,8,False,0,1,3,36,127,5,0)\n" +
			"trax(1,811,12811,74571,ZZZZ,ZZZZ,ZZZZ,ZZZZ,-898,2027,0,False,0,False,False,64,False,0,0,1,-1,0,5,0)\n" +
			"trax(0,973,16973,72614)\n" +
			"trax(0,,13718,58585)\n" +
			"endtrax(8,74571)"; 
		
		WebViewTrax trax = traxDecoder.doDecode(msg);
		Assert.assertEquals(0, trax.getTrax().length);
	}
	
	@Test
	/*
	 * This testcase sends a message with 2 invalid trax rows to be decoded.
	 * The first and second trax rows are invalid.  The resultant WebViewTrax
	 * message should contain 1 decoded rows for the test to pass
	 */
	public void testInValidTraxRow() {
		WebViewTraxDecoder traxDecoder = new WebViewTraxDecoder();
		String msg = 
			"Content-type: text/plain; charset=ISO-8859-1\n" +
			"\n" +
			"T,0,0,trax|ack('trax')\n" +
			"starttrax(3,74571,0,36,10233,12799,12811,12829,12843,12849,12850,12853)\n" +
			"trax(\n" +
			"trax(1,853,12853,74571,ZZZZ)\n" +
			"trax(0,,13718,58585)\n" +
			"endtrax(8,74571)"; 
		
		WebViewTrax trax = traxDecoder.doDecode(msg);
		Assert.assertEquals(1, trax.getTrax().length);
	}
	
	@Test
	/*
	 * This testcase sends an trax message that does not contain a starttrax row.  The resultant WebViewTrax message
	 * should contain 0 decoded rows for the testcase to be successful
	 */
	public void testMissingStartTrax() {
		WebViewTraxDecoder traxDecoder = new WebViewTraxDecoder();
		String msg = 
			"Content-type: text/plain; charset=ISO-8859-1\n" +
			"\n" +
			"T,0,0,trax|ack('trax')\n" +
			"trax(1,233,10233,74571,ZZZZ,ZZZZ,ZZZZ,ZZZZ,-812,1207,0,False,0,False,False,64,False,0,0,1,-1,0,5,0)\n" +
			"trax(1,,12799,74571,ZZZZ,ZZZZ,ZZZZ,ZZZZ,3092,13719,2713,False,76,False,False,8,False,0,1,3,36,127,5,0)\n" +
			"trax(1,853,12853,74571,ZZZZ,ZZZZ,ZZZZ,ZZZZ,1626,815,0,False,0,False,False,64,False,0,0,1,56,21,5,0)\n" +
			"trax(0,973,16973,72614)\n" +
			"trax(0,,13718,58585)\n" +
			"endtrax(8,74571)"; 
		
		WebViewTrax trax = traxDecoder.doDecode(msg);
		Assert.assertEquals(0, trax.getTrax().length);
	}
	
	@Test
	/*
	 * This testcase sends an trax message that has > 1 starttrax row.  The resultant WebViewTrax message
	 * should contain 0 decoded rows for the testcase to be successful
	 */
	public void testTooManyStartTrax() {
		WebViewTraxDecoder traxDecoder = new WebViewTraxDecoder();
		String msg = 
			"Content-type: text/plain; charset=ISO-8859-1\n" +
			"\n" +
			"T,0,0,trax|ack('trax')\n" +
			"starttrax(1,74571,0,36,10233,12799,12811,12829,12843,12849,12850,12853)\n" +
			"starttrax(1,74571,0,36,10233,12799,12811,12829,12843,12849,12850,12853)\n" +
			"trax(1,233,10233,74571,ZZZZ,ZZZZ,ZZZZ,ZZZZ,-812,1207,0,False,0,False,False,64,False,0,0,1,-1,0,5,0)\n" +
			"endtrax(8,74571)"; 
		
		WebViewTrax trax = traxDecoder.doDecode(msg);
		Assert.assertEquals(0, trax.getTrax().length);
	}
	
	@Test
	/*
	 * This testcase sends an trax message that does not contain and endtrax row.  The resultant WebViewTrax message
	 * should contain 0 decoded rows for the testcase to be successful
	 */
	public void testMissingEndTrax() {
		WebViewTraxDecoder traxDecoder = new WebViewTraxDecoder();
		String msg = 
			"Content-type: text/plain; charset=ISO-8859-1\n" +
			"\n" +
			"T,0,0,trax|ack('trax')\n" +
			"starttrax(3,74571,0,36,10233,12799,12811,12829,12843,12849,12850,12853)\n" +
			"trax(1,233,10233,74571,ZZZZ,ZZZZ,ZZZZ,ZZZZ,-812,1207,0,False,0,False,False,64,False,0,0,1,-1,0,5,0)\n" +
			"trax(1,,12799,74571,ZZZZ,ZZZZ,ZZZZ,ZZZZ,3092,13719,2713,False,76,False,False,8,False,0,1,3,36,127,5,0)\n" +
			"trax(1,811,12811,74571,ZZZZ,ZZZZ,ZZZZ,ZZZZ,-898,2027,0,False,0,False,False,64,False,0,0,1,-1,0,5,0)\n";
		
		WebViewTrax trax = traxDecoder.doDecode(msg);
		Assert.assertEquals(0, trax.getTrax().length);
	}
	
	@Test
	/*
	 * This testcase sends an trax message that has > 1 end row.  The resultant WebViewTrax message
	 * should contain 0 decoded rows for the testcase to be successful
	 */
	public void testTooManyEndTrax() {
		WebViewTraxDecoder traxDecoder = new WebViewTraxDecoder();
		String msg = 
			"Content-type: text/plain; charset=ISO-8859-1\n" +
			"\n" +
			"T,0,0,trax|ack('trax')\n" +
			"starttrax(3,74571,0,36,10233,12799,12811,12829,12843,12849,12850,12853)\n" +
			"trax(1,233,10233,74571,ZZZZ,ZZZZ,ZZZZ,ZZZZ,-812,1207,0,False,0,False,False,64,False,0,0,1,-1,0,5,0)\n" +
			"trax(1,,12799,74571,ZZZZ,ZZZZ,ZZZZ,ZZZZ,3092,13719,2713,False,76,False,False,8,False,0,1,3,36,127,5,0)\n" +
			"trax(1,811,12811,74571,ZZZZ,ZZZZ,ZZZZ,ZZZZ,-898,2027,0,False,0,False,False,64,False,0,0,1,-1,0,5,0)\n" +
			"endtrax(8,74571)\n" + 
			"endtrax(8,74571)"; 
		
		WebViewTrax trax = traxDecoder.doDecode(msg);
		Assert.assertEquals(0, trax.getTrax().length);
	}
	
	@Test
	/*
	 * This testcase sends a valid Trax message to be decoded. The resultant WebViewTrax message
	 * should contain 6 decoded rows for the testcase to be successful since 4 rows contain invalid
	 * values that will be used for enums
	 */
	public void testEnumValueValidation() {
		WebViewTraxDecoder traxDecoder = new WebViewTraxDecoder();
		String msg = 
			"Content-type: text/plain; charset=ISO-8859-1\n" +
			"\n" +
			"T,0,0,trax|ack('trax')\n" +
			"starttrax(10,74571,0,36,10233,12799,12811,12829,12843,12849,12850,12853)\n" +
			"trax(RX,233,10233,74571,ZZZZ,ZZZZ,ZZZZ,ZZZZ,-812,1207,0,False,0,False,False,64,False,0,0,1,-1,0,5,0)\n" + //bad row - RX
			"trax(1,,12799,74571,ZZZZ,ZZZZ,ZZZZ,ZZZZ,3092,13719,2713,False,76,False,False,8,False,99,1,3,36,127,5,0)\n" + //bad row - 99
			"trax(1,811,12811,74571,ZZZZ,ZZZZ,ZZZZ,ZZZZ,-898,2027,0,False,0,False,False,64,False,0,-21,1,-1,0,5,0)\n" + // bad row - -21
			"trax(1,,12829,74571,ZZZZ,ZZZZ,ZZZZ,ZZZZ,757,202,2419,False,31,False,False,64,False,0,0,3,307,9,1,0)\n" +
			"trax(1,843,12843,74571,ZZZZ,ZZZZ,ZZZZ,ZZZZ,1878,977,0,False,0,False,False,64,False,0,0,1,57,18,5,0)\n" +
			"trax(1,849,12849,74571,ZZZZ,ZZZZ,ZZZZ,ZZZZ,1424,744,0,False,0,False,False,64,False,0,0,1,-1,0,5,0)\n" +
			"trax(1,,12850,74571,ZZZZ,ZZZZ,ZZZZ,ZZZZ,10200,26661,151,False,59,False,False,8,False,0,1,1,266,169,5,0)\n" +
			"trax(1,853,12853,74571,ZZZZ,ZZZZ,ZZZZ,ZZZZ,1626,815,0,False,0,False,False,64,False,0,0,,56,21,5,0)\n" + // bad row missing value before'56'
			"trax(0,973,16973,72614)\n" +
			"trax(0,,13718,58585)\n" +
			"endtrax(8,74571)"; 
		
		WebViewTrax trax = traxDecoder.doDecode(msg);
		Assert.assertEquals(6, trax.getTrax().length);
	}
	
	@Test
	/*
	 * This testcase sends null to the decoder.  The resultant WebViewTrax message should contain 0 decoded
	 * rows for the testcase to be successful and there should be no NPE thrown
	 */
	public void testNullMessage() {
		WebViewTraxDecoder traxDecoder = new WebViewTraxDecoder();
		
		WebViewTrax trax = traxDecoder.doDecode(null);
		Assert.assertEquals(0, trax.getTrax().length);
	}
	
	@Test
	/*
	 * This test case invokes the getters on the WebViewTrax and
	 * WebViewTraxMessage classes
	 */
	public void testWebViewTraxMessages() {
		WebViewTraxDecoder traxDecoder = new WebViewTraxDecoder();
		String msg = 
			"Content-type: text/plain; charset=ISO-8859-1\n" +
			"\n" +
			"T,0,0,trax|ack('trax')\n" +
			"starttrax(4,74571,0,36,10233,12799,12811,12829,12843,12849,12850,12853)\n" +
			"trax(1,233,10233,74571,ZZZZ,ZZZZ,ZZZZ,ZZZZ,-812,1207,0,False,0,False,False,64,False,0,0,1,-1,0,5,0)\n" +
			"trax(1,853,12853,74571,ZZZZ,ZZZZ,ZZZZ,ZZZZ,1626,815,0,False,0,False,False,64,False,0,0,1,56,21,5,0)\n" +
			"trax(0,973,16973,72614)\n" +
			"trax(0,,13718,58585)\n" +
			"endtrax(8,74571)"; 
		
		WebViewTrax trax = traxDecoder.doDecode(msg);
		WebViewTraxMessage[] msgs = trax.getTrax();
		TargetType target_type    = TargetType.ghost;
		TargetType tgType         = target_type.valueOf("valid");
		TrackInfoSize info_size   = TrackInfoSize.large;
		TrackInfoSize info_sz     = info_size.valueOf("small");
		TrackInfoType info_type   = TrackInfoType.aircraft;
		TrackInfoType info_type2  = info_type.valueOf("vehicle");
		TrackInfoVmi info_vmi     = TrackInfoVmi.climb;
		TrackInfoVmi vmi          = info_vmi.valueOf("descend");
		String defaultStr         = "ok";
		
		msgs[0].getAc_type();
		msgs[0].setAc_type(defaultStr);
		msgs[0].getTarget_type();
		msgs[0].setTarget_type(target_type);
		msgs[0].getCall_sign_name();
		msgs[0].setCall_sign_name(defaultStr);
		msgs[0].getTrack_number();
		msgs[0].setTrack_number("12345");
		msgs[0].getTime_of_day();
		msgs[0].setTime_of_day("1200");
		msgs[0].getSource_airport();
		msgs[0].setSource_airport("YOW");
		msgs[0].getDestination_airport();
		msgs[0].setDestination_airport("YYZ");
		msgs[0].getStand();
		msgs[0].setStand(defaultStr);
		msgs[0].getCart_coord_x();
		msgs[0].setCart_coord_x("79.88");
		msgs[0].getCart_coord_y();
		msgs[0].setCart_coord_y("-234");
		msgs[0].getMode_3a();
		msgs[0].setMode_3a(defaultStr);
		msgs[0].isMode_3a_valid();
		msgs[0].setMode_3a_valid(true);
		msgs[0].getMode_c_altitude();
		msgs[0].setMode_c_altitude(defaultStr);
		msgs[0].isMode_c_valid();
		msgs[0].setMode_c_valid(false);
		msgs[0].isTrack_status_fusion();
		msgs[0].setTrack_status_fusion(true);
		msgs[0].getTrack_status_rad();
		msgs[0].setTrack_status_rad(defaultStr);
		msgs[0].isTrack_status_quality();
		msgs[0].setTrack_status_quality(false);
		msgs[0].getTrack_info_size();
		msgs[0].setTrack_info_size(info_size);
		msgs[0].getTrack_info_type();
		msgs[0].setTrack_info_type(info_type);
		msgs[0].getTrack_info_vmi();
		msgs[0].setTrack_info_vmi(info_vmi);
		msgs[0].getDirection();
		msgs[0].setDirection(defaultStr);
		msgs[0].getSpeed();
		msgs[0].setSpeed(defaultStr);
		msgs[0].getTrack_info_direction();
		msgs[0].setTrack_info_direction(defaultStr);
		
		trax.setTrax(null);
	}
	
	@Test
	/*
	 * This test case invokes the methods on the StringToWebViewTrax
	 * class
	 */
	public void testWebViewTraxTransformer() {
		StringToWebViewTrax traxTransformer = new StringToWebViewTrax();
		try {
			
			Object myObj = traxTransformer.transform("This is a test");
			Assert.assertNotNull(myObj);
			myObj = traxTransformer.transform(myObj);
		} catch (Exception ex) {
			TestCase.fail("Exception caught");
		}
	}
	
	@Test
	/*
	 * This test case invokes main() which also does some testing. It is to
	 * get a higher test coverage. This test can be deleted if main() is 
	 * deleted from the decoder class in the future.
	 */
	public void testMain() {
		WebViewTraxDecoder traxDecoder = new WebViewTraxDecoder();
		
		try {
			traxDecoder.main(null);
			
		} catch (Exception ex) {
			TestCase.fail("Exception was caught: " + ex);
		}
	}

}
