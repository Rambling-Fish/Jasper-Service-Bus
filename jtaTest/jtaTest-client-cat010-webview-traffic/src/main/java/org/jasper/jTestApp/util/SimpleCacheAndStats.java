package org.jasper.jTestApp.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jasper.jLib.cat010.codec.Cat010PeriodicStatusMessage;
import org.jasper.jLib.cat010.codec.Cat010TargetReport;
import org.jasper.jLib.webview.adaps.decoder.WebViewAdaps;
import org.jasper.jLib.webview.notam.decoder.WebViewNotam;
import org.jasper.jLib.webview.trax.decoder.WebViewTrax;

public class SimpleCacheAndStats {

	private static long totalObjectCountPUT = 0;
	private static long totalObjectCountREMOVE = 0;
	private static long noObjectRemoved = 0;
	private static long objectAlreadyInMap = 0;

	
	private static Map<String,Long> byteArrayMap = new ConcurrentHashMap<String, Long>();
	private static Map<Double,Long> periodicStatusMessageMap = new ConcurrentHashMap<Double, Long>();
	private static Map<Double,Long> targetReportMap = new ConcurrentHashMap<Double, Long>();

	private static Map<String,Long> traxMap = new ConcurrentHashMap<String, Long>();
	private static Map<String,Long> notamMap = new ConcurrentHashMap<String, Long>();
	private static Map<String,Long> adapsMap = new ConcurrentHashMap<String, Long>();
	
	public static String getStats(String str) throws Exception{
		StringBuilder sb = new StringBuilder();
		sb.append("total object count OUT = ");
		sb.append(totalObjectCountPUT);
		
		sb.append("<br>");
		sb.append("total object count IN = ");
		sb.append(totalObjectCountREMOVE);
		
		sb.append("<br>");
		sb.append("delta out/in  = ");
		sb.append(totalObjectCountPUT - totalObjectCountREMOVE);
		
		sb.append("<br>");
		sb.append("number of errors, noObjectRemoved from map  = ");
		sb.append(noObjectRemoved);
		
		sb.append("<br>");
		sb.append("number of errors, objectAlreadyInMap  = ");
		sb.append(objectAlreadyInMap);
		
		sb.append("<br>");
		sb.append("number in byteArrayList  = ");
		sb.append(byteArrayMap.size());
		if(byteArrayMap.size() > 0 && byteArrayMap.size() < 15){
			sb.append("</dd>");
			for(String byteArrayID:byteArrayMap.keySet()){
				sb.append("<br>byteArrayID = ");
				sb.append(byteArrayID);
				sb.append("  --  ");
				
				sb.append("timestop = ");
				sb.append(byteArrayMap.get(byteArrayID));
			}
			sb.append("</dd>");
		}
		
		sb.append("<br>");
		sb.append("number in periodicStatusMessageMap  = ");
		sb.append(periodicStatusMessageMap.size());
		if(periodicStatusMessageMap.size() > 0 && periodicStatusMessageMap.size() < 15){
			sb.append("<dd>");
			for(Double periodicStatusMessageID:periodicStatusMessageMap.keySet()){
				sb.append("<br>periodicStatusMessageID = ");
				sb.append(periodicStatusMessageID);
				sb.append("  --  ");
				
				sb.append("timestop = ");
				sb.append(periodicStatusMessageMap.get(periodicStatusMessageID));
			}
			sb.append("</dd>");
		}
		
		sb.append("<br>");
		sb.append("number in targetReportMap  = ");
		sb.append(targetReportMap.size());
		if(targetReportMap.size() > 0 && targetReportMap.size() < 15){
			sb.append("<dd>");
			for(Double targetReportID:targetReportMap.keySet()){
				sb.append("<br>targetReportID = ");
				sb.append(targetReportID);
				sb.append("  --  ");
				
				sb.append("timestop = ");
				sb.append(targetReportMap.get(targetReportID));
			}
			sb.append("</dd>");
		}
		
		sb.append("<br>");
		sb.append("number in traxMap  = ");
		sb.append(traxMap.size());
		if(traxMap.size() > 0 && traxMap.size() < 15){
			sb.append("<dd>");
			for(String traxID:traxMap.keySet()){
				sb.append("<br>traxID = ");
				sb.append(traxID);
				sb.append("  --  ");
				
				sb.append("timestop = ");
				sb.append(traxMap.get(traxID));
			}
			sb.append("</dd>");
		}
		
		sb.append("<br>");
		sb.append("number in notamMap  = ");
		sb.append(notamMap.size());
		if(notamMap.size() > 0 && notamMap.size() < 15){
			sb.append("<dd>");
			for(String notamID:notamMap.keySet()){
				sb.append("<br>notamID = ");
				sb.append(notamID);
				sb.append("  --  ");
				
				sb.append("timestop = ");
				sb.append(notamMap.get(notamID));
			}
			sb.append("</dd>");
		}
		
		
		sb.append("<br>");
		sb.append("number in adapsMap  = ");
		sb.append(adapsMap.size());
		if(adapsMap.size() > 0 && adapsMap.size() < 15){
			sb.append("<dd>");
			for(String adapsID:adapsMap.keySet()){
				sb.append("<br>adapsID = ");
				sb.append(adapsID);
				sb.append("  --  ");
				
				sb.append("timestop = ");
				sb.append(adapsMap.get(adapsID));
			}
			sb.append("</dd>");
		}
		return sb.toString();
	}
	
	private static String bytesToHex(byte[] bytes) {
	    final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
	    char[] hexChars = new char[bytes.length * 2];
	    int v;
	    for ( int j = 0; j < bytes.length; j++ ) {
	        v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
	
	public static void putByteArray(byte[] byteArray){
		if(byteArrayMap.containsKey(bytesToHex(byteArray))) objectAlreadyInMap++;
		byteArrayMap.put(bytesToHex(byteArray),System.currentTimeMillis());
		totalObjectCountPUT++;
	}
	
	public static void putPeriodicStatusMessage(Cat010PeriodicStatusMessage psm){
		if(periodicStatusMessageMap.containsKey(psm.getTimeOfDay())) objectAlreadyInMap++;
		periodicStatusMessageMap.put(psm.getTimeOfDay(), System.currentTimeMillis());
		totalObjectCountPUT++;
	}
	
	public static void putTargetReport(Cat010TargetReport tr){
		if(targetReportMap.containsKey(tr.getTimeOfDay())) objectAlreadyInMap++;
		targetReportMap.put(tr.getTimeOfDay(), System.currentTimeMillis());
		totalObjectCountPUT++;
	}
	
	public static void putTrax(WebViewTrax trax){
		if(traxMap.containsKey(trax.getTrax()[0].getCall_sign_name())) objectAlreadyInMap++;
		traxMap.put(trax.getTrax()[0].getCall_sign_name(), System.currentTimeMillis());
		totalObjectCountPUT++;
	}
	
	public static void putNotam(WebViewNotam notam){
		if(notamMap.containsKey(notam.getNotams()[0].getNotam_id())) objectAlreadyInMap++;
		notamMap.put(notam.getNotams()[0].getNotam_id(), System.currentTimeMillis());
		totalObjectCountPUT++;
	}
	
	public static void putAdaps(WebViewAdaps adaps){
		if(adapsMap.containsKey(adaps.getStats().getFir())) objectAlreadyInMap++;
		adapsMap.put(adaps.getStats().getFir(),System.currentTimeMillis());
		totalObjectCountPUT++;
	}
	
	public static void removeByteArray(byte[] byteArray){
		Long timeStamp = byteArrayMap.remove(bytesToHex(byteArray));
		if (timeStamp == null) noObjectRemoved++;
		totalObjectCountREMOVE++;
	}
	
	public static void removePeriodicStatusMessage(Cat010PeriodicStatusMessage psm){
		Long timeStamp = periodicStatusMessageMap.remove(psm.getTimeOfDay());
		if (timeStamp == null) noObjectRemoved++;
		totalObjectCountREMOVE++;
	}
	
	public static void removeTargetReport(Cat010TargetReport tr){
		Long timeStamp = targetReportMap.remove(tr.getTimeOfDay());
		if (timeStamp == null) noObjectRemoved++;
		totalObjectCountREMOVE++;
	}
	
	public static void removeTrax(WebViewTrax trax){
		Long timeStamp = traxMap.remove(trax.getTrax()[0].getCall_sign_name());
		if (timeStamp == null) noObjectRemoved++;
		totalObjectCountREMOVE++;
	}
	
	public static void removeNotam(WebViewNotam notam){
		Long timeStamp = notamMap.remove(notam.getNotams()[0].getNotam_id());
		if (timeStamp == null) noObjectRemoved++;
		totalObjectCountREMOVE++;
	}
	
	public static void removeAdaps(WebViewAdaps adaps){
		Long timeStamp = adapsMap.remove(adaps.getStats().getFir());
		if (timeStamp == null) noObjectRemoved++;
		totalObjectCountREMOVE++;
	}
	
}
