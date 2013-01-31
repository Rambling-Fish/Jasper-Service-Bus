package org.jasper.jtaDemo.cat010Webview.util;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.log4j.Logger;

import org.jasper.jLib.cat010.codec.Cat010TargetReport;
import org.jasper.jLib.cat010.codec.Cat010TargetReportDescriptor.TOT;
import org.jasper.jLib.webview.trax.decoder.WebViewTrax;
import org.jasper.jLib.webview.trax.decoder.WebViewTraxMessage;
import org.jasper.jLib.webview.trax.decoder.WebViewTraxMessage.TargetType;
import org.jasper.jLib.webview.trax.decoder.WebViewTraxMessage.TrackInfoSize;
import org.jasper.jLib.webview.trax.decoder.WebViewTraxMessage.TrackInfoType;
import org.jasper.jLib.webview.trax.decoder.WebViewTraxMessage.TrackInfoVmi;

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
	
	private static int getInt(String str){
		int value = 0;
		try{
			value = Integer.parseInt(str);
		}catch (NumberFormatException e) {
			value = 0;
		}
		return value;
	}
	
	public static SimpleTarget[] getTraxMap(String str) throws Exception{
		WebViewTrax traxMap = getTraxMap();
		SimpleTarget[] targets = new SimpleTarget[traxMap.getTrax().length];
		int count = 0;
		for(WebViewTraxMessage trax : traxMap.getTrax()){
			int x = 400 + (getInt(trax.getCart_coord_x()) / 10);
			int y = 400 + (getInt(trax.getCart_coord_y()) / 10);
			int x1 = (trax.getTrack_number().length() > 1) ? x-7 : x-3;
			int y1 = y+4;
			targets[count] = new SimpleTarget(trax.getTrack_number(), x, y, x1, y1,trax.getTrack_info_type().toString());
			count++;
		}
		return targets;
	}
	
	private static WebViewTraxMessage convertTRtoWVTM(Cat010TargetReport tr){
		
		String defaultStringString = "ZZZZ";
		String defaultIntString    = "0";
		boolean defaultBoolean     = false;
		String defaultDirectionString = "-1";
		int cart_coord_x_offset = 2680; //TODO make configurable
		int cart_coord_y_offset = 2001; //TODO make configurable
		
		TargetType target_type       = TargetType.valid;
		String call_sign_name        = tr.getTrackNumber().toString();
		String track_num             = tr.getTrackNumber().toString();
		String time_of_day           = tr.getTimeOfDay().toString();
		String source_airport        = defaultStringString;
		String destination_airport   = defaultStringString;
		String ac_type               = defaultStringString;
		String stand                 = defaultStringString;
		String cart_coord_x          = "";
		if(tr.getPositionInCartesianCoordX() !=null){
			cart_coord_x = "" + (tr.getPositionInCartesianCoordX().intValue() - cart_coord_x_offset);
		}
		String cart_coord_y          = "";
		if(tr.getPositionInCartesianCoordY() !=null){
			cart_coord_y = "" + (tr.getPositionInCartesianCoordY().intValue() - cart_coord_y_offset);
		}
		String mode_3a               = defaultIntString;
		boolean mode_3a_valid        = defaultBoolean;
		String mode_c_altitude       = defaultIntString;
		boolean mode_c_valid         = defaultBoolean;
		boolean track_status_fusion  = defaultBoolean;
		String track_status_rad      = defaultIntString;
		boolean track_status_quality = defaultBoolean;
		TrackInfoSize track_info_size = TrackInfoSize.unknown;
		
		TrackInfoType track_info_type = null;
		if(tr.getTargetReportDescriptor().getTot() == TOT.aircraft){
			track_info_type = TrackInfoType.aircraft;
		}else if(tr.getTargetReportDescriptor().getTot() == TOT.ground_vehicle){
			track_info_type = TrackInfoType.vehicle;
		}else{
			track_info_type = TrackInfoType.defaultValue;
		}
			
		TrackInfoVmi track_info_vmi   = TrackInfoVmi.defaultValue;
		String direction              = defaultDirectionString;
		/*
		 * So if you have vX = 1.5, vY=1.25
		 * s = sqrt(1.5^2 + 1.25^2)
		 * speed = s * 1.94384
		 * speed = 3.795
		 */
		String speed = "";
		if(tr.getCalTrackVelocityInCartesianCoordX() == null || tr.getCalTrackVelocityInCartesianCoordY() == null){
			speed = "unknown";
		}else{
			double speedInMetersPerSec = Math.sqrt( (Math.pow(tr.getCalTrackVelocityInCartesianCoordX(), 2)) +
				                                (Math.pow(tr.getCalTrackVelocityInCartesianCoordY(), 2)));
			double speedInKnots = speedInMetersPerSec * 1.94384;
			speed = "" + speedInKnots;
		}
		String track_info_direction   = defaultIntString;
//		String unknown                = line[23];
		
		return new WebViewTraxMessage(target_type, call_sign_name, track_num, time_of_day, source_airport, destination_airport,
				ac_type, stand, cart_coord_x, cart_coord_y, mode_3a, mode_3a_valid, mode_c_altitude, mode_c_valid, track_status_fusion,
				track_status_rad, track_status_quality, track_info_size, track_info_type, track_info_vmi, direction, speed, track_info_direction);
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
	
	public static SimpleTarget[] putTargetReport(Cat010TargetReport targetReport) throws Exception{
		WebViewTraxMessage traxMessage = convertTRtoWVTM(targetReport);
		try {
			traxMap.put(traxMessage.getTrack_number(), traxMessage);
			expiresMap.put(traxMessage.getTrack_number(), System.currentTimeMillis());
		} catch (NullPointerException npe) {
			logger.error("trax message contains null track_number");
		}
		return getTraxMap("");	}
	
}
