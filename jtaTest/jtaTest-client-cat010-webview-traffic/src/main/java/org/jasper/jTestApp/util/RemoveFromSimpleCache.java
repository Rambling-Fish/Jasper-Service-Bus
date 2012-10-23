package org.jasper.jTestApp.util;

import org.jasper.jLib.cat010.codec.Cat010PeriodicStatusMessage;
import org.jasper.jLib.cat010.codec.Cat010TargetReport;
import org.jasper.jLib.webview.adaps.decoder.WebViewAdaps;
import org.jasper.jLib.webview.notam.decoder.WebViewNotam;
import org.jasper.jLib.webview.trax.decoder.WebViewTrax;

public class RemoveFromSimpleCache {
	
	public static void removeByteArray(byte[] byteArray){
		SimpleCacheAndStats.removeByteArray(byteArray);
	}
	
	public static void removePeriodicStatusMessage(Cat010PeriodicStatusMessage psm){
		SimpleCacheAndStats.removePeriodicStatusMessage(psm);
	}
	
	public static void removeTargetReport(Cat010TargetReport tr){
		SimpleCacheAndStats.removeTargetReport(tr);
	}
	
	public static void removeTrax(WebViewTrax trax){
		SimpleCacheAndStats.removeTrax(trax);
	}
	
	public static void removeNotam(WebViewNotam notam){
		SimpleCacheAndStats.removeNotam(notam);
	}
	
	public static void removeAdaps(WebViewAdaps adaps){
		SimpleCacheAndStats.removeAdaps(adaps);
	}
	
}
