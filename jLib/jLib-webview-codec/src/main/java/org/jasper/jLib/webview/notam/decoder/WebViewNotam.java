package org.jasper.jLib.webview.notam.decoder;

import java.io.Serializable;

public class WebViewNotam implements Serializable{
	

	private static final long serialVersionUID = -6162371562151325522L;

	private WebViewNotamMessage[] notams;

	public WebViewNotamMessage[] getNotams() {
		return notams;
	}

	public void setNotams(WebViewNotamMessage[] notams) {
		this.notams = notams;
	}

	public WebViewNotam(WebViewNotamMessage[] notams) {
		super();
		this.notams = notams;
	}
	
}
