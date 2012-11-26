package org.jasper.jta.webview.util;

import org.jasper.jLib.webview.adaps.decoder.WebViewAdaps;
import org.jasper.jLib.webview.adaps.decoder.WebViewAdapsRunwayInfo;
import org.jasper.jLib.webview.adaps.decoder.WebViewAdapsStats;
import org.jasper.jLib.webview.notam.decoder.WebViewNotam;
import org.jasper.jLib.webview.notam.decoder.WebViewNotamMessage;
import org.jasper.jLib.webview.trax.decoder.WebViewTrax;
import org.jasper.jLib.webview.trax.decoder.WebViewTraxMessage;

public class JasperWebViewSimpleCache {

	private static WebViewTrax trax;
	private static WebViewNotam notam;
	private static WebViewAdaps adaps;
	
	static{
		WebViewTraxMessage traxMessage = new WebViewTraxMessage(null, null, null, null, null, null, null, null, null,
				null, null, false, null, false, false, null, false, null, null, null, null, null, null);
		WebViewTraxMessage[] traxArray = {traxMessage};
		trax = new WebViewTrax(traxArray);
		
		WebViewNotamMessage notamMessage = new WebViewNotamMessage(null, null);
		WebViewNotamMessage[] notamsArray = {notamMessage };
		notam = new WebViewNotam(notamsArray);
		
		
		WebViewAdapsRunwayInfo runway = new WebViewAdapsRunwayInfo(null, null, null, null, null, null, null);
		WebViewAdapsRunwayInfo[] runways = {runway};
		WebViewAdapsStats stats = new WebViewAdapsStats(null, null, null, null, null, null);
		
		adaps = new WebViewAdaps(stats, runways);
	}
	
	public Object getTrax(String msg){
		if(msg.equals("/jasper/webview/1.0/trax")) return trax;
		if(msg.equals("/jasper/webview/1.0/notam")) return notam;
		if(msg.equals("/jasper/webview/1.0/adaps")) return adaps;
		return "JASPER ASB --- Resource Not Found.";
	}
	
	public void setTrax(WebViewTrax trax){
		JasperWebViewSimpleCache.trax = trax;
	}

	public static void setNotam(WebViewNotam notam) {
		JasperWebViewSimpleCache.notam = notam;
	}

	public static void setAdaps(WebViewAdaps adaps) {
		JasperWebViewSimpleCache.adaps = adaps;
	}
	
}
