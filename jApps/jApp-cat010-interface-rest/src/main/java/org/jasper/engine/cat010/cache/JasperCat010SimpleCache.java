package org.jasper.engine.cat010.cache;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jasper.jLib.cat010.codec.Cat010TargetReport;

public class JasperCat010SimpleCache {

	private static Map<Integer, Cat010TargetReport> reports = new ConcurrentHashMap<Integer, Cat010TargetReport>();
	private static Map<Integer, Long> expires = new ConcurrentHashMap<Integer, Long>();
	
	public Object getTargetReport(String msg){
		if(msg.equals("/jasper/cat010/1.0/targetreport")) return getTargetReports();
		return "JASPER ASB --- Resource Not Found.";
	}
	
	public static void addTargetReport(Cat010TargetReport targetReport) {
		reports.put(targetReport.getTrackNumber(), targetReport);
		expires.put(targetReport.getTrackNumber(), System.currentTimeMillis());
	}
	
	public static Cat010TargetReport[] getTargetReports(){
		ArrayList<Cat010TargetReport> list = new ArrayList<Cat010TargetReport>();
		for(Integer trackNumber:reports.keySet()){
			if((System.currentTimeMillis() - expires.get(trackNumber)) <= 5000){
				list.add(reports.get(trackNumber));
			}else{
				reports.remove(trackNumber);
				expires.remove(trackNumber);
			}
		}
		Cat010TargetReport[] empty = {};
		return list.toArray(empty);
	}
}