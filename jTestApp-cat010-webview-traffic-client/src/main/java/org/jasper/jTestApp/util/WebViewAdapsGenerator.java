package org.jasper.jTestApp.util;

import org.jasper.jLib.webview.adaps.decoder.WebViewAdaps;
import org.jasper.jLib.webview.adaps.decoder.WebViewAdapsRunwayInfo;
import org.jasper.jLib.webview.adaps.decoder.WebViewAdapsStats;

public class WebViewAdapsGenerator {

	private static int count = 0;
	
	public static WebViewAdaps getAdaps(){
		WebViewAdapsRunwayInfo runway = new WebViewAdapsRunwayInfo(null, null, null, null, null, null, null);
		WebViewAdapsRunwayInfo[] runways = {runway};
		
		long millisSinceGMTMidnight = System.currentTimeMillis() % (24L * 60*60*1000);
		String time_of_day = "" + (((double)millisSinceGMTMidnight) / 1000);
		
		WebViewAdapsStats stats = new WebViewAdapsStats("" + count++, time_of_day, null, null, null, null);
		
		return new WebViewAdaps(stats, runways);
	}
	
	/*
	 * we ignore the string parameter, as our mule flow will
	 * look for a method with a String parameter, we simply 
	 * call the parameter-less version of this method
	 */
	public static WebViewAdaps getAdaps(String str){
		return getAdaps();
	}
}
