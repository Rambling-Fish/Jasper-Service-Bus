package org.jasper.core.constants;

import java.io.Serializable;

public class JtaInfo implements Serializable {
	private static final long serialVersionUID = -4016625558650948052L;
	private String jtaName;
	private String licenseKey;
	private String jsbConnectedTo;
	private String clientId;
	private String clientIp;
	 
	public JtaInfo(String jtaName, String licenseKey, String jsb, String clientId, String clientIp){
		this.jtaName = jtaName;
		this.licenseKey = licenseKey;
		this.jsbConnectedTo = jsb;
		this.clientId = clientId;
		this.clientIp = clientIp;
	}
	

	public String getJtaName() {
		return jtaName;
	}

	public String getJsbConnectedTo() {
		return jsbConnectedTo;
	}

	public String getLicenseKey() {
		return licenseKey;
	}

	public String getClientId() {
		return clientId;
	}
	
	public String getClientIp() {
		return clientIp;
	}
}
