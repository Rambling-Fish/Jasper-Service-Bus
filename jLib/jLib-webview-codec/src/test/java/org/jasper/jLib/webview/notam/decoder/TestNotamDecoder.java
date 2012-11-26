package org.jasper.jLib.webview.notam.decoder;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.jasper.jLib.webview.notam.transformer.StringToWebViewNotam;
import org.jasper.jLib.webview.trax.transformer.StringToWebViewTrax;
import org.junit.Test;


public class TestNotamDecoder {
	
	@Test
	/*
	 * This testcase sends a valid notam message to be decoded. The resultant WebViewNotam message
	 * should contain 5 decoded rows for the testcase to be successful
	 */
	public void testValidNotamMessage() {
		WebViewNotamDecoder notamDecoder = new WebViewNotamDecoder();
		String msg = 
			"T,0,0,notam|ack('notam')\n" +
			"notam(startnotam)\n" +
			"notam(110688,110688 NOTAMN CYOW OTTAWA/MACDONALD-CARTIER INTL@CYOW AMEND LOC<BC> RWY 25 APCH: PLAN VIEW: NO PT ALT 1500 AUTH FM@TEFLY WPT ONLY)\n" +
			"notam(120073,120073 NOTAMN CYOW OTTAWA/MACDONALD-CARTIER INTL@CYOW WEF 1202090901 AMEND RIVER SEVEN ARR:@THURO: ALT RESTRICTION TO READ:@RWY 25 AT OR BLW 10000 RWY 07 AT OR BLW 16000@RWY 32 AT OR BLW 12000)\n" +
			"notam(120173,120173 NOTAMN CYOW OTTAWA/MACDONALD-CARTIER INTL@CYOW RWY 07/25 SLIPPERY WHEN WET DUE TO RUBBER BUILDUP@1205011315 TIL 1205160400)\n" +
			"notam(120178,120178 NOTAMN CYOW OTTAWA/MACDONALD-CARTIER INTL@CYOW PURSUANT TO SECTION 5.1 OF THE AERONAUTICS ACT; THE AIRSPACE@WITHIN THE AREA BOUNDED BY 453241N 752451W TO 453924N 752709W@TO 454453N 750833W TO 453702N 750101W THEN FOLLOWING ROAD@148 TO POINT OF ORIGIN <CENTRED APRX 25 NM NE AD>; 4000 TO@7000 FT MSL; IS RESTRICTED <FORMATION FLT TRAINING>.@NO PERSON SHALL OPR AN ACFT WITHIN THE AREA DESCRIBED EXC@FOR ACFT AUTH BY ATC.@MAY 11 1700-2200;@MAY 12 1400-2200;@MAY 13 1400-1600.@1205111700 TIL 1205131600)\n" +
			"notam(120179,120179 NOTAMN CYOW OTTAWA/MACDONALD-CARTIER INTL@CYOW RWY 14/32 AVBL 30 MIN PN 613-248-2013; DUE MAINT@1205071315 TIL 1205071730)\n" +
		    "notam(endnotam)";
		
		
		WebViewNotam notam = notamDecoder.doDecode(msg);		
		Assert.assertEquals(5, notam.getNotams().length);
	}
	
	@Test
	/*
	 * This testcase sends a notam message that does not contain a startnotam row.  The resultant WebViewNotam message
	 * should contain 0 decoded rows for the testcase to be successful
	 */
	public void testMissingStartNotam() {
		WebViewNotamDecoder notamDecoder = new WebViewNotamDecoder();
		String msg = 
			"T,0,0,notam|ack('notam')\n" +
			"notam(110688,110688 NOTAMN CYOW OTTAWA/MACDONALD-CARTIER INTL@CYOW AMEND LOC<BC> RWY 25 APCH: PLAN VIEW: NO PT ALT 1500 AUTH FM@TEFLY WPT ONLY)\n" +
			"notam(120073,120073 NOTAMN CYOW OTTAWA/MACDONALD-CARTIER INTL@CYOW WEF 1202090901 AMEND RIVER SEVEN ARR:@THURO: ALT RESTRICTION TO READ:@RWY 25 AT OR BLW 10000 RWY 07 AT OR BLW 16000@RWY 32 AT OR BLW 12000)\n" +
			"notam(120173,120173 NOTAMN CYOW OTTAWA/MACDONALD-CARTIER INTL@CYOW RWY 07/25 SLIPPERY WHEN WET DUE TO RUBBER BUILDUP@1205011315 TIL 1205160400)\n" +
			"notam(120179,120179 NOTAMN CYOW OTTAWA/MACDONALD-CARTIER INTL@CYOW RWY 14/32 AVBL 30 MIN PN 613-248-2013; DUE MAINT@1205071315 TIL 1205071730)\n" +
		    "notam(endnotam)";
		
		WebViewNotam notam = notamDecoder.doDecode(msg);
		Assert.assertEquals(0, notam.getNotams().length);
	}
	
	@Test
	/*
	 * This testcase sends a notam message that does not contain an endnotam row.  The resultant WebViewNotam message
	 * should contain 0 decoded rows for the testcase to be successful
	 */
	public void testMissingEndNotam() {
		WebViewNotamDecoder notamDecoder = new WebViewNotamDecoder();
		String msg = 
			"T,0,0,notam|ack('notam')\n" +
			"notam(startnotam)\n" +
			"notam(110688,110688 NOTAMN CYOW OTTAWA/MACDONALD-CARTIER INTL@CYOW AMEND LOC<BC> RWY 25 APCH: PLAN VIEW: NO PT ALT 1500 AUTH FM@TEFLY WPT ONLY)\n" +
			"notam(120173,120173 NOTAMN CYOW OTTAWA/MACDONALD-CARTIER INTL@CYOW RWY 07/25 SLIPPERY WHEN WET DUE TO RUBBER BUILDUP@1205011315 TIL 1205160400)\n" +
			"notam(120179,120179 NOTAMN CYOW OTTAWA/MACDONALD-CARTIER INTL@CYOW RWY 14/32 AVBL 30 MIN PN 613-248-2013; DUE MAINT@1205071315 TIL 1205071730)\n" ;
		
		WebViewNotam notam = notamDecoder.doDecode(msg);
		Assert.assertEquals(0, notam.getNotams().length);
	}
	
	@Test
	/*
	 * This testcase sends a notam message that does not contain a description
	 * field. The resultant WebViewNotam message should contain 2 decoded rows
	 * for the test to pass as the third row is invalid
	 */
	public void testInvalidLineLength() {
		WebViewNotamDecoder notamDecoder = new WebViewNotamDecoder();
		String msg = 
			"T,0,0,notam|ack('notam')\n" +
			"notam(startnotam)\n" +
			"notam(110688,110688 NOTAMN CYOW OTTAWA/MACDONALD-CARTIER INTL@CYOW AMEND LOC<BC> RWY 25 APCH: PLAN VIEW: NO PT ALT 1500 AUTH FM@TEFLY WPT ONLY)\n" +
			"notam(120173,120173 NOTAMN CYOW OTTAWA/MACDONALD-CARTIER INTL@CYOW RWY 07/25 SLIPPERY WHEN WET DUE TO RUBBER BUILDUP@1205011315 TIL 1205160400)\n" +
			"notam(120179)\n" +
			"notam(endnotam)";
		
		WebViewNotam notam = notamDecoder.doDecode(msg);
		Assert.assertEquals(2, notam.getNotams().length);
	}
	
	@Test
	/*
	 * This testcase sends null to the decoder.  The resultant WebViewNotam message should contain 0 decoded 
	 * rows for the testcase to be successful and there should be no NPE thrown
	 */
	public void testNullMessage() {
		WebViewNotamDecoder notamDecoder = new WebViewNotamDecoder();
		
		WebViewNotam notam = notamDecoder.doDecode(null);
		Assert.assertEquals(0, notam.getNotams().length);
	}
	
	@Test
	/*
	 * This test case invokes the getters on the WebViewNotam and
	 * WebViewNotamMessage classes
	 */
	public void testWebViewNotamMessages() {
			WebViewNotamDecoder notamDecoder = new WebViewNotamDecoder();
			String msg = 
				"T,0,0,notam|ack('notam')\n" +
				"notam(startnotam)\n" +
				"notam(110688,110688 NOTAMN CYOW OTTAWA/MACDONALD-CARTIER INTL@CYOW AMEND LOC<BC> RWY 25 APCH: PLAN VIEW: NO PT ALT 1500 AUTH FM@TEFLY WPT ONLY)\n" +
				"notam(120073,120073 NOTAMN CYOW OTTAWA/MACDONALD-CARTIER INTL@CYOW WEF 1202090901 AMEND RIVER SEVEN ARR:@THURO: ALT RESTRICTION TO READ:@RWY 25 AT OR BLW 10000 RWY 07 AT OR BLW 16000@RWY 32 AT OR BLW 12000)\n" +
				"notam(120173,120173 NOTAMN CYOW OTTAWA/MACDONALD-CARTIER INTL@CYOW RWY 07/25 SLIPPERY WHEN WET DUE TO RUBBER BUILDUP@1205011315 TIL 1205160400)\n" +
				"notam(120178,120178 NOTAMN CYOW OTTAWA/MACDONALD-CARTIER INTL@CYOW PURSUANT TO SECTION 5.1 OF THE AERONAUTICS ACT; THE AIRSPACE@WITHIN THE AREA BOUNDED BY 453241N 752451W TO 453924N 752709W@TO 454453N 750833W TO 453702N 750101W THEN FOLLOWING ROAD@148 TO POINT OF ORIGIN <CENTRED APRX 25 NM NE AD>; 4000 TO@7000 FT MSL; IS RESTRICTED <FORMATION FLT TRAINING>.@NO PERSON SHALL OPR AN ACFT WITHIN THE AREA DESCRIBED EXC@FOR ACFT AUTH BY ATC.@MAY 11 1700-2200;@MAY 12 1400-2200;@MAY 13 1400-1600.@1205111700 TIL 1205131600)\n" +
				"notam(120179,120179 NOTAMN CYOW OTTAWA/MACDONALD-CARTIER INTL@CYOW RWY 14/32 AVBL 30 MIN PN 613-248-2013; DUE MAINT@1205071315 TIL 1205071730)\n" +
			    "notam(endnotam)";
			
			
			WebViewNotam notam = notamDecoder.doDecode(msg);
			Assert.assertEquals(5, notam.getNotams().length);
			WebViewNotamMessage[] msgs = notam.getNotams();
			notam.setNotams(null);
			msgs[0].getNotam_id();
			msgs[0].getDescription();
			msgs[0].setDescription("Notam description");
			msgs[0].setNotam_id("notam_id");
	}
	
	@Test
	/*
	 * This test case invokes the getters on the StringToWebViewNotam
	 * class
	 */
	public void testWebViewNotamTransformer() {
		StringToWebViewNotam notamTransformer = new StringToWebViewNotam();
		try {
			
			Object myObj = notamTransformer.transform("This is a test");
			Assert.assertNotNull(myObj);
			myObj = notamTransformer.transform(myObj);
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
		WebViewNotamDecoder notamDecoder = new WebViewNotamDecoder();
		
		try {
			notamDecoder.main(null);
			
		} catch (Exception ex) {
			TestCase.fail("Exception was caught: " + ex);
		}
	}

}
