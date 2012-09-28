package org.jasper.engine.idarvideo.cache;

import org.jasper.jLib.idar.video.codec.IDARVideoMessage;

public class JasperIdarVideoSimpleCache {
	private static IDARVideoMessage ivm;
	
	public Object getRestReponse(String msg){
		if(msg.equals("/jasper/idarvideo/1.0/trackInfo")) return ivm.getIntelliDARinfo();;
		//if(msg.equals("/jasper/idarvideo/1.0/video")) return ivm.getIDARjpeg();
		return "JASPER ASB --- Resource Not Found.";
	}
	
	public static void putIdarVideoMessage(IDARVideoMessage ivm){
		JasperIdarVideoSimpleCache.ivm = ivm;
	}

}
