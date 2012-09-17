package org.jasper.jLib.webview.notam.decoder;

import java.io.Serializable;

public class WebViewNotamMessage implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5326324209054263134L;
	
	String notam_id;
	String description;
	
	public WebViewNotamMessage(String notam_id, String description) {
		this.notam_id = notam_id;
		this.description = description;
	}
	
	public String getNotam_id() {
		return notam_id;
	}
	public void setNotam_id(String notam_id) {
		this.notam_id = notam_id;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

}
