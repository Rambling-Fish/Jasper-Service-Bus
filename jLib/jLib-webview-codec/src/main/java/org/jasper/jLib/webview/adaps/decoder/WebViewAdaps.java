package org.jasper.jLib.webview.adaps.decoder;

import java.io.Serializable;

public class WebViewAdaps implements Serializable{
	
	private static final long serialVersionUID = 3248468822816456075L;

	private WebViewAdapsStats stats;
	private WebViewAdapsRunwayInfo[] runways;
	
	public WebViewAdaps(WebViewAdapsStats stats, WebViewAdapsRunwayInfo[] runways) {
		this.stats = stats;
		this.runways = runways;
	}
	public WebViewAdapsStats getStats() {
		return stats;
	}
	public void setStats(WebViewAdapsStats stats) {
		this.stats = stats;
	}
	public WebViewAdapsRunwayInfo[] getRunways() {
		return runways;
	}
	public void setRunways(WebViewAdapsRunwayInfo[] runways) {
		this.runways = runways;
	}
	
}
