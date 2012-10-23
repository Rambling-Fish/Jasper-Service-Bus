package org.jasper.jTestApp.util;

import org.jasper.jLib.webview.notam.decoder.WebViewNotam;
import org.jasper.jLib.webview.notam.decoder.WebViewNotamMessage;

public class WebViewNotamGenerator {

	private static int count = 0;
	
	public static WebViewNotam getNotam(){
		
		long millisSinceGMTMidnight = System.currentTimeMillis() % (24L * 60*60*1000);
		String time_of_day = "" + (((double)millisSinceGMTMidnight) / 1000);
		
		WebViewNotamMessage notamMessage = new WebViewNotamMessage("" + count++, "time_of_day = " + time_of_day);
		WebViewNotamMessage[] notamsArray = {notamMessage };
		return new WebViewNotam(notamsArray);
	}
	
	/*
	 * we ignore the string parameter, as our mule flow will
	 * look for a method with a String parameter, we simply 
	 * call the parameter-less version of this method
	 */
	public static WebViewNotam getNotam(String str){
		return getNotam();
	}
}
