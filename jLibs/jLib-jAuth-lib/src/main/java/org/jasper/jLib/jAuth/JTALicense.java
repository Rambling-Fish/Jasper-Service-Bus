package org.jasper.jLib.jAuth;

import java.io.Serializable;
import java.util.Calendar;

public class JTALicense implements Serializable {

	private static final long serialVersionUID = 3592650466203181338L;

	private String vendor;
	private String appName;
	private String version;
	private String deploymentId;
	private Calendar expiry;
	private String ntpHost;
	private Integer ntpPort;
	
	private byte[] licenseKey;
	
	public JTALicense(String vendor, String appName, String version,
			String deploymentId, Calendar expiry, String ntpHost, Integer ntpPort,
			byte[] licenseKey) {
		super();
		this.vendor = vendor;
		this.appName = appName;
		this.version = version;
		this.deploymentId = deploymentId;
		this.expiry = expiry;
		this.ntpHost = ntpHost;
		this.ntpPort = ntpPort;
		this.licenseKey = licenseKey;
	}

	public String getVendor() {
		return vendor;
	}

	public String getAppName() {
		return appName;
	}

	public String getVersion() {
		return version;
	}

	public String getDeploymentId() {
		return deploymentId;
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
		sb.append(vendor);
		sb.append(":");
		sb.append(appName);
		sb.append(":");
		sb.append(version);
		sb.append(":");
		sb.append(deploymentId);
		if(expiry != null){
			sb.append(":");
			sb.append(expiry.get(Calendar.YEAR));
			sb.append("-");
			sb.append(expiry.get(Calendar.MONTH) +1);
			sb.append("-");
			sb.append(expiry.get(Calendar.DAY_OF_MONTH));
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
