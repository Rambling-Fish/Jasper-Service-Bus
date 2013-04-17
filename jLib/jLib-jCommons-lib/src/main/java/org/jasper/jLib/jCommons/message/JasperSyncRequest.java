package org.jasper.jLib.jCommons.message;

import java.io.Serializable;

public class JasperSyncRequest implements Serializable{

	 private static final long serialVersionUID = -3658912135474278727L;
	
	private String uri;
	private String[] params;
	public JasperSyncRequest(String uri, String... params) {
		super();
		this.uri = uri;
		this.params = params;
	}

	public String getUri() {
		return uri;
	}
	public void setUri(String uri) {
		this.uri = uri;
	}
	public String[] getParams() {
		return params;
	}
	public void setParams(String[] params) {
		this.params = params;
	}
	
}
