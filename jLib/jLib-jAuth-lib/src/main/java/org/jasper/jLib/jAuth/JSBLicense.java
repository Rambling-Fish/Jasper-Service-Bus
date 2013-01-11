package org.jasper.jLib.jAuth;

import java.io.Serializable;
import java.util.Calendar;

public class JSBLicense implements Serializable {

	private static final long serialVersionUID = -3149487585283942590L;
	
	private String deploymentId;
	private int instanceId;
	private Calendar expiry;
	private String ntpHost;
	private Integer ntpPort;
	
	private byte[] licenseKey;

	public JSBLicense(String deploymentId, int instanceId, Calendar expiry,
			String ntpHost, Integer ntpPort, byte[] licenseKey) {
		super();
		this.deploymentId = deploymentId;
		this.instanceId = instanceId;
		this.expiry = expiry;
		this.ntpHost = ntpHost;
		this.ntpPort = ntpPort;
		this.licenseKey = licenseKey;
	}

	public String getDeploymentId() {
		return deploymentId;
	}

	public int getInstanceId() {
		return instanceId;
	}

	public Calendar getExpiry() {
		return expiry;
	}

	public String getNtpHost() {
		return ntpHost;
	}

	public Integer getNtpPort() {
		return ntpPort;
	}

	public byte[] getLicenseKey() {
		return licenseKey;
	}
	
	public void setLicenseKey(byte[] licenseKey) {
		this.licenseKey = licenseKey;
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("jsb");
		sb.append(":");
		sb.append(deploymentId);
		sb.append(":");
		sb.append(instanceId);
		
		if(expiry != null){
			sb.append(":");
			sb.append(expiry.get(Calendar.YEAR));
			sb.append("-");
			sb.append(expiry.get(Calendar.MONTH) +1);
			sb.append("-");
			sb.append(expiry.get(Calendar.DAY_OF_MONTH));
			sb.append("-");
			sb.append(expiry.get(Calendar.HOUR_OF_DAY));
			sb.append("-");
			sb.append(expiry.get(Calendar.MINUTE));
			sb.append("-");
			sb.append(expiry.get(Calendar.SECOND));
		}
		
		if(ntpHost != null){
			sb.append(":");
			sb.append(ntpHost);
		}
		
		if(ntpPort != null){
			sb.append(":");
			sb.append(ntpPort.intValue());
		}
		
		return sb.toString();
	}
	
}
