package org.jasper.jApp.cat010Webview.util;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.log4j.Logger;

import org.jasper.jLib.webview.trax.decoder.WebViewTrax;
import org.jasper.jLib.webview.trax.decoder.WebViewTraxMessage;

public class JasperTraxAggregator {

	private static Map<String, WebViewTraxMessage> traxMap = new ConcurrentHashMap<String, WebViewTraxMessage>();
	private static Map<String, Long> expiresMap = new ConcurrentHashMap<String, Long>();
	private static Logger logger = Logger.getLogger("org.jasper");
	
	private static WebViewTrax getTraxMap(){
		ArrayList<WebViewTraxMessage> list = new ArrayList<WebViewTraxMessage>();
		for(String trackNumber:traxMap.keySet()){
			if((System.currentTimeMillis() - expiresMap.get(trackNumber)) <= 5000){
				list.add(traxMap.get(trackNumber));
			}else{
				traxMap.remove(trackNumber);
				expiresMap.remove(trackNumber);
			}
		}
		WebViewTraxMessage[] empty = {};
		return new WebViewTrax(list.toArray(empty));
	}
	
	public static WebViewTrax getTraxMap(String str) throws Exception{
		return getTraxMap();
	}
	
	public static WebViewTrax putTraxMessage(WebViewTraxMessage traxMessage) {
		try {
			traxMap.put(traxMessage.getTrack_number(), traxMessage);
			expiresMap.put(traxMessage.getTrack_number(), System.currentTimeMillis());
		} catch (NullPointerException npe) {
			logger.error("trax message contains null track_number");
		}
		return getTraxMap();
	}
	
	public static WebViewTrax putTrax(WebViewTrax trax){
		for(WebViewTraxMessage traxMessage:trax.getTrax()){
			putTraxMessage(traxMessage);
		}
		return getTraxMap();
	}
	
}
