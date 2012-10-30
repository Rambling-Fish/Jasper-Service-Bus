package org.jasper.jLib.webview.adaps.decoder;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.jasper.jLib.webview.adaps.transformer.StringToWebViewAdaps;
import org.jasper.jLib.webview.notam.transformer.StringToWebViewNotam;
import org.junit.Test;


public class TestAdapsDecoder {
	
	@Test
	/*
	 * This testcase sends a valid notam message to be decoded. The resultant WebViewNotam message
	 * should contain 5 decoded rows for the test to pass
	 */
	public void testValidAdapsMessage() {
		WebViewAdapsDecoder adapsDecoder = new WebViewAdapsDecoder();
		String msg = 
			"Content-type: text/plain; charset=ISO-8859-1\n" +
			"\n" +
			"T,0,0,adaps|ack('adaps')\n" +
			"adaps(stats,fir;UL,last_update_time;2012-05-11 21:54:55,gusting;G18,windspeed;11,adapsaltimeter; 2994 ,winddirection;330)\n" +
			"adaps(runway,32,el:0,cl:0,baz:?,riu:*,vl:0,al:0,tl:0,rvra:60+,elev:?,sl:0,gp: ,rwy:32,az:?,loc: )\n" +
			"adaps(runway,07,el:0,dme: ,cl:0,baz:?,riu:*,vl:0,al:0,tl:0,rvra:60+,rvrb:69+,elev:?,sl:0,gp: ,rwy:07,az:?,loc: )\n" +
			"adaps(runway,25,el:0,cl:0,baz:?,riu: ,vl:5,al:0,tl:0,elev:?,sl:0,rwy:25,az:?)\n" +
			"adaps(runway,14,el:0,cl:0,baz:?,riu: ,vl:0,al:0,tl:0,elev:?,sl:0,rwy:14,az:?)\n" +
			"adaps(endstats)";
		
		WebViewAdaps adaps = adapsDecoder.doDecode(msg);
		Assert.assertNotNull(adaps.getStats());
		Assert.assertEquals(4, adaps.getRunways().length);
	}
	
	@Test
	/*
	 * This test case sends a notam msg with an invalid stats line. The resultant WebViewNotam message
	 * should contain 5 decoded rows for the test case to pass
	 */
	public void testInvalidStatsLine() {
		WebViewAdapsDecoder adapsDecoder = new WebViewAdapsDecoder();
		String msg = 
			"Content-type: text/plain; charset=ISO-8859-1\n" +
			"\n" +
			"T,0,0,adaps|ack('adaps')\n" +
			"adaps(stats,)\n" +
			"adaps(runway,32,el:0,cl:0,baz:?,riu:*,vl:0,al:0,tl:0,rvra:60+,elev:?,sl:0,gp: ,rwy:32,az:?,loc: )\n" +
			"adaps(runway,07,el:0,dme: ,cl:0,baz:?,riu:*,vl:0,al:0,tl:0,rvra:60+,elev:?,sl:0,gp: ,rwy:07,az:?,loc: )\n" +
			"adaps(runway,25,el:0,cl:0,baz:?,riu: ,vl:5,al:0,tl:0,elev:?,sl:0,rwy:25,az:?)\n" +
			"adaps(endstats)";
		
		WebViewAdaps adaps = adapsDecoder.doDecode(msg);
		Assert.assertNull(adaps.getStats());
		Assert.assertEquals(3, adaps.getRunways().length);
	
	}
	
	@Test
	/*
	 * This test case sends a notam msg with an invalid runway line - first
	 * runway contains "rvra:," no value for rvra field. The resultant
	 * WebViewNotam message should contain 3 decoded rows for the test case to pass
	 */
	public void testInvalidRunwayLine() {
		WebViewAdapsDecoder adapsDecoder = new WebViewAdapsDecoder();
		String msg = 
			"Content-type: text/plain; charset=ISO-8859-1\n" +
			"\n" +
			"T,0,0,adaps|ack('adaps')\n" +
			"adaps(stats,fir;UL,last_update_time;2012-05-11 21:54:55,gusting;G18,windspeed;11,adapsaltimeter; 2994 ,winddirection;330)\n" +
			"adaps(runway,32,el:0,cl:0,baz:?,riu:*,vl:0,al:0,tl:0,rvra:,elev:?,sl:0,gp: ,rwy:32,az:?,loc: )\n" +
			"adaps(runway,07,el:0,dme: ,cl:0,baz:?,riu:*,vl:0,al:0,tl:0,rvra:60+,elev:?,sl:0,gp: ,rwy:07,az:?,loc: )\n" +
			"adaps(runway,25,el:0,cl:0,baz:?,riu: ,vl:5,al:0,tl:0,elev:?,sl:0,rwy:25,az:?)\n" +
			"adaps(endstats)";
		
		WebViewAdaps adaps = adapsDecoder.doDecode(msg);
		Assert.assertNotNull(adaps.getStats());
		Assert.assertEquals(3, adaps.getRunways().length);
	}
	
	@Test
	/*
	 * This testcase sends null to the decoder.  The resultant WebViewNotam message should contain 0 decoded 
	 * rows for the testcase to be successful and there should be no NPE thrown
	 */
	public void testNullMessage() {
		WebViewAdapsDecoder adapsDecoder = new WebViewAdapsDecoder();
		WebViewAdaps adaps = adapsDecoder.doDecode(null);
		
		Assert.assertNull(adaps.getStats());
		Assert.assertEquals(0, adaps.getRunways().length);
	}
	
	@Test
	/*
	 * This test case invokes the getters on the WebViewAdaps,
	 * WebViewAdapsMessage, WebViewAdapsRunwayInfo, and WebViewAdapsStats
	 * classes
	 */
	public void testWebViewAdapsMessages() {
		WebViewAdapsDecoder adapsDecoder = new WebViewAdapsDecoder();
		String defaultStr = "test";
		String msg = 
			"Content-type: text/plain; charset=ISO-8859-1\n" +
			"\n" +
			"T,0,0,adaps|ack('adaps')\n" +
			"adaps(stats,fir;UL,last_update_time;2012-05-11 21:54:55,gusting;G18,windspeed;11,adapsaltimeter; 2994 ,winddirection;330)\n" +
			"adaps(runway,32,el:0,cl:0,baz:?,riu:*,vl:0,al:0,tl:0,rvra:60+,elev:?,sl:0,gp: ,rwy:32,az:?,loc: )\n" +
			"adaps(endstats)";
		
		WebViewAdaps adaps = adapsDecoder.doDecode(msg);
		Assert.assertNotNull(adaps.getStats());
		Assert.assertEquals(1, adaps.getRunways().length);
		WebViewAdapsStats stats = adaps.getStats();
		stats.getFir();
		stats.setFir(defaultStr);
		stats.getGusting();
		stats.setGusting(defaultStr);
		stats.getAdaps_altimeter();
		stats.setAdaps_altimeter(defaultStr);
		stats.getLast_update_time();
		stats.setLast_update_time(defaultStr);
		stats.getWind_direction();
		stats.setWind_direction(defaultStr);
		stats.getWindspeed();
		stats.setWindspeed(defaultStr);
		WebViewAdapsRunwayInfo[] runways = adaps.getRunways();
		runways[0].getDme();
		runways[0].setDme(defaultStr);
		runways[0].getEdge_lights();
		runways[0].setEdge_lights(defaultStr);
		runways[0].getGp();
		runways[0].setGp(defaultStr);
		runways[0].getLoc();
		runways[0].setLoc(defaultStr);
		runways[0].getRunway();
		runways[0].setRunway(defaultStr);
		runways[0].getRvr_a();
		runways[0].setRvr_a(defaultStr);
		runways[0].getRvr_b();
		runways[0].setRvr_b(defaultStr);
		adaps.setStats(null);
		adaps.setRunways(null);
	}
	
	@Test
	/*
	 * This test case invokes the getters on the StringToWebViewAdaps
	 * class
	 */
	public void testWebViewAdapsTransformer() {
		StringToWebViewAdaps adapsTransformer = new StringToWebViewAdaps();
		try {
			Object myObj = adapsTransformer.transform("This is a test");
			Assert.assertNotNull(myObj);
			myObj = adapsTransformer.transform(myObj);
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
		WebViewAdapsDecoder adapsDecoder = new WebViewAdapsDecoder();
		
		try {
			adapsDecoder.main(null);	
		} catch (Exception ex) {
			TestCase.fail("Exception was caught: " + ex);
		}
	}

}
