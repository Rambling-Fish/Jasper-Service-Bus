package org.jasper.engine.idarvideo.cache;

import org.jasper.jLib.idar.video.codec.IDARVideoMessage;
import org.apache.log4j.Logger;

public class JasperIdarVideoSimpleCache {
	private static IDARVideoMessage ivm;
	private static Logger logger = Logger.getLogger("org.jasper");
	
	public Object getRestResponse(String msg){
		if(msg.equals("/jasper/idarvideo/1.0/trackInfo")) {
			try {
				return (String) ivm.getIntelliDARinfo();
				} catch (NullPointerException npe) {
				logger.error("Idar Video Message is null");
				}
		}		
		if(msg.equals("/jasper/idarvideo/1.0/video")) {
			try {
				return ivm;
				} catch (NullPointerException npe) {
				logger.error("Idar Video Message is null");
				}
		}
		return "JASPER ASB --- Resource Not Found.";
	}
	
	public static void putIdarVideoMessage(IDARVideoMessage ivm){
		JasperIdarVideoSimpleCache.ivm = ivm;
	}

}
