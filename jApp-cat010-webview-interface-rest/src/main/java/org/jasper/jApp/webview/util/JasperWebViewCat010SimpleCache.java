package org.jasper.jApp.webview.util;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jasper.jLib.cat010.codec.Cat010TargetReport;
import org.jasper.jLib.webview.adaps.decoder.WebViewAdaps;
import org.jasper.jLib.webview.adaps.decoder.WebViewAdapsRunwayInfo;
import org.jasper.jLib.webview.adaps.decoder.WebViewAdapsStats;
import org.jasper.jLib.webview.notam.decoder.WebViewNotam;
import org.jasper.jLib.webview.notam.decoder.WebViewNotamMessage;
import org.jasper.jLib.webview.trax.decoder.WebViewTrax;
import org.jasper.jLib.webview.trax.decoder.WebViewTraxMessage;

public class JasperWebViewCat010SimpleCache {

	private static Map<Integer, Cat010TargetReport> targetReports = new ConcurrentHashMap<Integer, Cat010TargetReport>();
	private static Map<Integer, Long> expiresTRMap = new ConcurrentHashMap<Integer, Long>();
	
	private static Map<String, WebViewTraxMessage> traxMap = new ConcurrentHashMap<String, WebViewTraxMessage>();
	private static Map<String, Long> expiresTraxMap = new ConcurrentHashMap<String, Long>();
	
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
	
	public Object getRestReponse(String msg){
		if(msg.equals("/jasper/webview/1.0/trax")) return trax;
		if(msg.equals("/jasper/webview/1.1/trax")) return getTraxMap();
		if(msg.equals("/jasper/webview/1.0/notam")) return notam;
		if(msg.equals("/jasper/webview/1.0/adaps")) return adaps;
		if(msg.equals("/jasper/cat010/1.0/targetreport")) return getTargetReports();
		return "JASPER ASB --- Resource Not Found.";
	}
	
	public static void putTargetReport(Cat010TargetReport targetReport) {
		targetReports.put(targetReport.getTrackNumber(), targetReport);
		expiresTRMap.put(targetReport.getTrackNumber(), System.currentTimeMillis());
	}
	
	public static Cat010TargetReport[] getTargetReports(){
		ArrayList<Cat010TargetReport> list = new ArrayList<Cat010TargetReport>();
		for(Integer trackNumber:targetReports.keySet()){
			if((System.currentTimeMillis() - expiresTRMap.get(trackNumber)) <= 5000){
				list.add(targetReports.get(trackNumber));
			}else{
				targetReports.remove(trackNumber);
				expiresTRMap.remove(trackNumber);
			}
		}
		Cat010TargetReport[] empty = {};
		return list.toArray(empty);
	}
	
	public static void putTraxMessage(WebViewTraxMessage traxMessage) {
		traxMap.put(traxMessage.getCall_sign_name(), traxMessage);
		expiresTraxMap.put(traxMessage.getCall_sign_name(), System.currentTimeMillis());
	}
	
	public static WebViewTraxMessage[] getTraxMap(){
		ArrayList<WebViewTraxMessage> list = new ArrayList<WebViewTraxMessage>();
		for(String trackNumber:traxMap.keySet()){
			if((System.currentTimeMillis() - expiresTraxMap.get(trackNumber)) <= 5000){
				list.add(traxMap.get(trackNumber));
			}else{
				traxMap.remove(trackNumber);
				expiresTraxMap.remove(trackNumber);
			}
		}
		WebViewTraxMessage[] empty = {};
		return list.toArray(empty);
	}
	
	public void putTrax(WebViewTrax trax){
		JasperWebViewCat010SimpleCache.trax = trax;
		for(WebViewTraxMessage traxMessage:trax.getTrax()){
			putTraxMessage(traxMessage);
		}
	}

	public static void setNotam(WebViewNotam notam) {
		JasperWebViewCat010SimpleCache.notam = notam;
	}

	public static void setAdaps(WebViewAdaps adaps) {
		JasperWebViewCat010SimpleCache.adaps = adaps;
	}
	
}
