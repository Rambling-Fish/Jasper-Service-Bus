package org.jasper.jTestApp.util;

import org.jasper.jLib.cat010.codec.Cat010PeriodicStatusMessage;
import org.jasper.jLib.cat010.codec.Cat010TargetReport;
import org.jasper.jLib.webview.adaps.decoder.WebViewAdaps;
import org.jasper.jLib.webview.notam.decoder.WebViewNotam;
import org.jasper.jLib.webview.trax.decoder.WebViewTrax;

public class PutInSimpleCache {
	
	public static void putByteArray(byte[] byteArray){
		SimpleCacheAndStats.putByteArray(byteArray);
	}
	
	public static void putPeriodicStatusMessage(Cat010PeriodicStatusMessage psm){
		SimpleCacheAndStats.putPeriodicStatusMessage(psm);
	}
	
	public static void putTargetReport(Cat010TargetReport tr){
		SimpleCacheAndStats.putTargetReport(tr);
	}
	
	public static void putTrax(WebViewTrax trax){
		SimpleCacheAndStats.putTrax(trax);
	}
	
	public static void putNotam(WebViewNotam notam){
		SimpleCacheAndStats.putNotam(notam);
	}
	
	public static void putAdaps(WebViewAdaps adaps){
		SimpleCacheAndStats.putAdaps(adaps);
	}
	
}
