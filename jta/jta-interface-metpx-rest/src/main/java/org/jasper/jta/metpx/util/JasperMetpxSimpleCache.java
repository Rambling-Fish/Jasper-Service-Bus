package org.jasper.jta.metpx.util;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.jasper.jLib.metpx.decoder.Bulletin;

public class JasperMetpxSimpleCache {
	private static Map<String, Bulletin> reports = new ConcurrentHashMap<String, Bulletin>();
	private static Map<String, Long> expires = new ConcurrentHashMap<String, Long>();
	private static Bulletin bulletin = new Bulletin();
	private static Logger logger = Logger.getLogger("org.jasper");

	public Object getBulletinList(String msg){
		if(msg.equals("/jasper/metpx/1.0/board")) {
			if (bulletin.getHeader() != null) {
				return reports;
			} else {
				return "Waiting for bulletins...";
			}
		}
		if(msg.equals("/jasper/metpx/1.0/current")) {
			if (bulletin.getHeader() != null) {
				return bulletin;
			} else {
				return "Waiting for a bulletin...";
			}
			
		}
		
		return "JASPER ASB --- Resource Not Found.";
	}
	
	public static void addBulletins(Bulletin bulletin){
		try {
			reports.put(bulletin.getHeader() + " " + System.currentTimeMillis(), bulletin);
			expires.put(bulletin.getHeader() + " " + System.currentTimeMillis(), System.currentTimeMillis());
			JasperMetpxSimpleCache.bulletin = bulletin;
		} catch (NullPointerException npe) {
			logger.error("Bulletin Message is null");
		}
		
	}
	
	public static Bulletin[] getBulletinBoard(){
		ArrayList<Bulletin> list = new ArrayList<Bulletin>();
		for(String header:reports.keySet()){
			if((System.currentTimeMillis() - expires.get(header)) <= 5000){
				list.add(reports.get(header));
			}else{
				reports.remove(header);
				expires.remove(header);
			}
		}
		Bulletin[] empty = {};
		return list.toArray(empty);
	}
	
	
}
