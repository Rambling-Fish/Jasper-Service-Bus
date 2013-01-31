package org.jasper.jtaDemo.util;

import org.jasper.jLib.webview.trax.decoder.WebViewTrax;
import org.jasper.jLib.webview.trax.decoder.WebViewTraxMessage;
import org.jasper.jLib.webview.trax.decoder.WebViewTraxMessage.TargetType;
import org.jasper.jLib.webview.trax.decoder.WebViewTraxMessage.TrackInfoType;

public class WebViewTraxGenerator {
	
	private static Double[] ySign = {1.0,1.0,1.0,1.0};
	private static Double[] x = {0.0,0.0,0.0,0.0};
	private static Double[] r = {2000.0,2500.0,3750.0,3500.0};
	private static Double[] increment = {10.0,30.0,-15.0,20.0};
	
	private static int count = 0;	
	public static WebViewTrax getTrax(){
		
		Double y = (ySign[count]) * Math.sqrt(r[count]*r[count] - x[count]*x[count]);
		
		if( (x[count]*x[count]) > (r[count]*r[count]*0.95)){
			x[count] += (increment[count]*0.25);
		}else if( (x[count]*x[count]) > (r[count]*r[count]*0.9)){
			x[count] += (increment[count]*0.5);
		}else{
			x[count] += increment[count];
		}
		
		if (x[count] >= r[count] || x[count] <= (-1)*r[count]){
			increment[count] *= -1.0;
			ySign[count] *= -1.0;
		}

		long millisSinceGMTMidnight = System.currentTimeMillis() % (24L * 60*60*1000);
		String time_of_day = "" + (((double)millisSinceGMTMidnight) / 1000);
		WebViewTraxMessage traxMessage = new WebViewTraxMessage(TargetType.valid, "" + (count+4), "" + (count+4), time_of_day, null, null, null, null, "" + x[count].intValue(), "" + y.intValue(),
				null, false, null, false, false, null, false, null, TrackInfoType.aircraft, null, null, null, null);

		WebViewTraxMessage[] traxArray = {traxMessage};
		
		count++;
		count %= 4;
		
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
