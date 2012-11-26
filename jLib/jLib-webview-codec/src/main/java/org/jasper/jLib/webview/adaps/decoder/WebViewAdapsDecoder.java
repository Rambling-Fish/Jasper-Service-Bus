package org.jasper.jLib.webview.adaps.decoder;

import java.util.ArrayList;

public class WebViewAdapsDecoder {

	/*
	 * Sample adaps response
	 *
     * Content-type: text/plain; charset=ISO-8859-1
     * 
     * T,0,0,adaps|ack('adaps')
     * adaps(stats,fir;UL,last_update_time;2012-05-11 21:54:55,gusting;G18,windspeed;11,adapsaltimeter; 2994 ,winddirection;330)
     * adaps(runway,32,el:0,cl:0,baz:?,riu:*,vl:0,al:0,tl:0,rvra:60+,elev:?,sl:0,gp: ,rwy:32,az:?,loc: )
     * adaps(runway,07,el:0,dme: ,cl:0,baz:?,riu:*,vl:0,al:0,tl:0,rvra:60+,elev:?,sl:0,gp: ,rwy:07,az:?,loc: )
     * adaps(runway,25,el:0,cl:0,baz:?,riu: ,vl:5,al:0,tl:0,elev:?,sl:0,rwy:25,az:?)
     * adaps(runway,14,el:0,cl:0,baz:?,riu: ,vl:0,al:0,tl:0,elev:?,sl:0,rwy:14,az:?)
     * adaps(endstats
	 */

	public static WebViewAdaps doDecode(String msg){
		
		WebViewAdapsStats stats = getAdapsStats(msg);
		
		ArrayList<WebViewAdapsRunwayInfo> adaps = new ArrayList<WebViewAdapsRunwayInfo>();
		
		if(isValidAdapsResponse(msg)){
			ArrayList<String> rows = getAdapsRunwayRows(msg);
			for(String row:rows){
				WebViewAdapsRunwayInfo decodedRow = decodeRow(row);
				if (decodedRow != null) adaps.add(decodedRow);
			}
		}
		
		return new WebViewAdaps(stats, adaps.toArray(new WebViewAdapsRunwayInfo[0]));
	}
	
	private static WebViewAdapsStats getAdapsStats(String msg){
		if(msg == null) return null;
		
		String[] rows = msg.split("\n");
		String statsLine = null;
		for(String row:rows){
			if(row.startsWith("adaps(stats") && row.endsWith(")")){
				statsLine = row.substring(12,(row.length() - 1));
				break;
			}
		}
		if( statsLine == null || statsLine.isEmpty()) return null;
		
		String[] stats = statsLine.split(",");
		String fir = "";
		String last_update_time = "";
		String gusting = "";
		String windspeed = "";
		String adaps_altimeter = "";
		String wind_direction = "";
		
		for(String stat:stats){
			String[] keyValuePair = stat.split(";");
			if(keyValuePair.length == 2) {
				if ("fir".equals(keyValuePair[0])){
					fir = keyValuePair[1].trim();
				}else if ("last_update_time".equals(keyValuePair[0])){
					last_update_time = keyValuePair[1].trim();
				}else if ("gusting".equals(keyValuePair[0])){
					gusting = keyValuePair[1].trim();
				}else if ("windspeed".equals(keyValuePair[0])){
					windspeed = keyValuePair[1].trim();
				}else if ("adapsaltimeter".equals(keyValuePair[0])){
					adaps_altimeter = keyValuePair[1].trim();
				}else if ("winddirection".equals(keyValuePair[0])){
					wind_direction = keyValuePair[1].trim();
				}
			}
			
		}
		
		return new WebViewAdapsStats(fir, last_update_time, gusting, windspeed, adaps_altimeter, wind_direction);
	}
	
	private static WebViewAdapsRunwayInfo decodeRow(String row) {
		String[] line = row.substring(13,(row.length() - 1)).split(",");
		String runway = "";
		String rvr_a = "";
		String rvr_b = "";
		String edge_lights = "";
		String loc = "";
		String gp = "";
		String dme = "";
		
		for(String info:line){
			String[] keyValuePair = info.split(":");
			if(keyValuePair.length == 2) {
				if ("rwy".equals(keyValuePair[0])){
					runway = keyValuePair[1];
				}else if ("rvra".equals(keyValuePair[0])){
					rvr_a = keyValuePair[1];
				}else if ("rvrb".equals(keyValuePair[0])){
					rvr_b = keyValuePair[1];
				}else if ("el".equals(keyValuePair[0])){
					edge_lights = keyValuePair[1];
				}else if ("loc".equals(keyValuePair[0])){
					loc = "OK";
				}else if ("gp".equals(keyValuePair[0])){
					gp = "OK";
				}else if ("dme".equals(keyValuePair[0])){
					dme = "OK";
				}
			}
		}
		
		return new WebViewAdapsRunwayInfo(runway, rvr_a, rvr_b, edge_lights, loc, gp, dme);
	}

	private static ArrayList<String> getAdapsRunwayRows(String msg) {
		String[] rows = msg.split("\n");
		ArrayList<String> adapsRows = new ArrayList<String>(); 
		for(String row : rows){
			if(row.startsWith("adaps(runway,") && row.endsWith(")")){
					adapsRows.add(row);
			}
		}
		return adapsRows;
	}

	private static boolean isValidAdapsResponse(String msg) {
		if(msg == null)
			return false;
		else
		 return true;
	}

	public static void main(String arg[]){
		String record = 
			"Content-type: text/plain; charset=ISO-8859-1\n" +
			"\n" +
			"T,0,0,adaps|ack('adaps')\n" +
			"adaps(stats,fir;UL,last_update_time;2012-05-11 21:54:55,gusting;G18,windspeed;11,adapsaltimeter; 2994 ,winddirection;330)\n" +
			"adaps(runway,32,el:0,cl:0,baz:?,riu:*,vl:0,al:0,tl:0,rvra:60+,elev:?,sl:0,gp: ,rwy:32,az:?,loc: )\n" +
			"adaps(runway,07,el:0,dme: ,cl:0,baz:?,riu:*,vl:0,al:0,tl:0,rvra:60+,elev:?,sl:0,gp: ,rwy:07,az:?,loc: )\n" +
			"adaps(runway,25,el:0,cl:0,baz:?,riu: ,vl:5,al:0,tl:0,elev:?,sl:0,rwy:25,az:?)\n" +
			"adaps(runway,14,el:0,cl:0,baz:?,riu: ,vl:0,al:0,tl:0,elev:?,sl:0,rwy:14,az:?)\n" +
			"adaps(endstats)";

		WebViewAdaps adaps = doDecode(record);		
	}
		
}
