package org.jasper.jLib.webview.trax.decoder;

import java.io.Serializable;

public class WebViewTrax implements Serializable {
	
	private static final long serialVersionUID = 2796073050264580818L;

	private WebViewTraxMessage[] trax;

	public WebViewTrax(WebViewTraxMessage[] trax) {
		this.trax = trax;
	}

	public WebViewTraxMessage[] getTrax() {
		return trax;
	}

	public void setTrax(WebViewTraxMessage[] trax) {
		this.trax = trax;
	}

}
