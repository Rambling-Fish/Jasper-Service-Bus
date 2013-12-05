package org.jasper.jLib.jAuth;

import java.io.Serializable;
import java.util.Calendar;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.Since;

public class ClientLicense implements Serializable {
	
	private static final long serialVersionUID = 7151809391636292573L;
	
	// To provide version-ing of the properties use the following 
	// Annotations 
	// @Since(2.1) or @Until(2.0)
	// @Expose controls which properties can be 
	// serialized (Java to JSON) and/or deserialized (JSON to JAVA)
	// Sample annotation @Expose(serialize = false, deserialize = false)
	
	@Expose private String type;
	@Expose private Integer instanceId;
	@Expose private String vendor;
	@Expose private String appName;
	@Expose private String version;
	@Expose private Integer numOfPublishers;
	@Expose private Integer numOfConsumers;
	@Expose private String adminQueue;
	@Expose private String deploymentId;
	@Expose private Calendar expiry;
	@Expose private String ntpHost;
	@Expose private Integer ntpPort;
	
	private byte[] licenseKey;
	
	public ClientLicense(String type, Integer instanceId, String vendor, String appName, String version,
			Integer numOfPublishers, Integer numOfConsumers, String adminQueue, String deploymentId, 
			Calendar expiry, String ntpHost, Integer ntpPort,
			byte[] licenseKey) {
		super();
		this.type = type;
		this.instanceId = instanceId;
		this.vendor = vendor;
		this.appName = appName;
		this.version = version;
		this.numOfPublishers = numOfPublishers;
		this.numOfConsumers = numOfConsumers;
		this.adminQueue = adminQueue;
		this.deploymentId = deploymentId;
		this.expiry = expiry;
		this.ntpHost = ntpHost;
		this.ntpPort = ntpPort;
		this.licenseKey = licenseKey;
	}
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Integer getinstanceId() {
		return instanceId;
	}

	public void setinstanceId(Integer instanceId) {
		this.instanceId = instanceId;
	}

	public Integer getNumOfPublishers() {
		return numOfPublishers;
	}

	public void setNumOfPublishers(Integer numOfPublishers) {
		this.numOfPublishers = numOfPublishers;
	}

	public Integer getNumOfConsumers() {
		return numOfConsumers;
	}

	public void setNumOfConsumers(Integer numOfConsumers) {
		this.numOfConsumers = numOfConsumers;
	}

	public String getAdminQueue() {
		return adminQueue;
	}

	public void setAdminQueue(String adminQueue) {
		this.adminQueue = adminQueue;
	}

	public String getVendor() {
		return vendor;
	}

	public void setVendor(String vendor) {
		this.vendor = vendor;
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getDeploymentId() {
		return deploymentId;
	}

	public void setDeploymentId(String deploymentId) {
		this.deploymentId = deploymentId;
	}

	public Calendar getExpiry() {
		return expiry;
	}

	public void setExpiry(Calendar expiry) {
		this.expiry = expiry;
	}

	public String getNtpHost() {
		return ntpHost;
	}

	public void setNtpHost(String ntpHost) {
		this.ntpHost = ntpHost;
	}

	public Integer getNtpPort() {
		return ntpPort;
	}

	public void setNtpPort(Integer ntpPort) {
		this.ntpPort = ntpPort;
	}

	public byte[] getLicenseKey() {
		return licenseKey;
	}

	public void setLicenseKey(byte[] licenseKey) {
		this.licenseKey = licenseKey;
	}
	
	public String toString(){
		GsonBuilder builder = new GsonBuilder();
        builder.excludeFieldsWithoutExposeAnnotation();
        Gson gson = builder.create();
		return gson.toJson(this);
	}
}
