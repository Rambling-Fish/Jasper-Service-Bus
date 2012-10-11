package org.jasper.jTestApp.util;

import org.jasper.jLib.webview.trax.decoder.WebViewTrax;
import org.jasper.jLib.webview.trax.decoder.WebViewTraxMessage;

public class WebViewTraxGenerator {
	
	
	public static WebViewTrax getTrax(){

		long millisSinceGMTMidnight = System.currentTimeMillis() % (24L * 60*60*1000);
		String time_of_day = "" + (((double)millisSinceGMTMidnight) / 1000);
		
		WebViewTraxMessage traxMessage = new WebViewTraxMessage(null, null, null, time_of_day, null, null, null, null, null,
				null, null, false, null, false, false, null, false, null, null, null, null, null, null);
		WebViewTraxMessage[] traxArray = {traxMessage};
		return new WebViewTrax(traxArray);
	}
	
	/*
	 * we ignore the string parameter, as our mule flow will
	 * look for a method with a String parameter, we simply 
	 * call the parameter-less version of this method
	 */
	public static WebViewTrax getTrax(String str){
		return getTrax();
	}

	
}
