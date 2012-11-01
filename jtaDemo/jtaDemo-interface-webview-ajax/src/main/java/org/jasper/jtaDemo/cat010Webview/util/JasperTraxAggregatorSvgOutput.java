package org.jasper.jtaDemo.cat010Webview.util;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.log4j.Logger;

import org.jasper.jLib.webview.trax.decoder.WebViewTrax;
import org.jasper.jLib.webview.trax.decoder.WebViewTraxMessage;

public class JasperTraxAggregatorSvgOutput {

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
	
	public static SimpleTarget[] getTraxMap(String str) throws Exception{
		WebViewTrax traxMap = getTraxMap();
		SimpleTarget[] targets = new SimpleTarget[traxMap.getTrax().length];
		int count = 0;
		for(WebViewTraxMessage trax : traxMap.getTrax()){
			int x = 400 + (Integer.parseInt(trax.getCart_coord_x()) / 10);
			int y = 400 + (Integer.parseInt(trax.getCart_coord_y()) / 10);
			int x1 = (trax.getTrack_number().length() > 1) ? x-8 : x-4;
			int y1 = y+5;
			targets[count] = new SimpleTarget(trax.getTrack_number(), x, y, x1, y1);
			count++;
		}
		return targets;
	}
	
	public static SimpleTarget[] putTraxMessage(WebViewTraxMessage traxMessage) throws Exception {
		try {
			traxMap.put(traxMessage.getTrack_number(), traxMessage);
			expiresMap.put(traxMessage.getTrack_number(), System.currentTimeMillis());
		} catch (NullPointerException npe) {
			logger.error("trax message contains null track_number");
		}
		return getTraxMap("");
	}
	
	public static SimpleTarget[] putTrax(WebViewTrax trax) throws Exception{
		for(WebViewTraxMessage traxMessage:trax.getTrax()){
			putTraxMessage(traxMessage);
		}
		return getTraxMap("");
	}
	
}
