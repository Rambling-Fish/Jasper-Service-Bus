package org.jasper.jLib.jAuth;

import java.io.Serializable;
import java.util.Calendar;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.Since;

public class UDELicense implements Serializable {
	
	private static final long serialVersionUID = -7333626604830091492L;
	
	// To provide version-ing of the properties use the following 
	// Annotations 
	// @Since(2.1) or @Until(2.0)
	// @Expose controls which properties can be 
	// serialized (Java to JSON) and/or deserialized (JSON to JAVA)
	// Sample annotation @Expose(serialize = false, deserialize = false)
		
	@Expose private String type;
	@Expose private String version;
	@Expose private String deploymentId;
	@Expose private int instanceId;
	@Expose private Integer numOfPublishers;
	@Expose private Integer numOfConsumers;
	@Expose private Calendar expiry;
	@Expose private String ntpHost;
	@Expose private Integer ntpPort;
	
	private byte[] licenseKey;
	
	public UDELicense(String type, String version, String deploymentId, int instanceId, 
			Integer numOfPublishers, Integer numOfConsumers, Calendar expiry,
			String ntpHost, Integer ntpPort, byte[] licenseKey) {
		super();
		this.type = type;
		this.version = version;
		this.deploymentId = deploymentId;
		this.instanceId = instanceId;
		this.numOfPublishers = numOfPublishers;
		this.numOfConsumers = numOfConsumers;
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

	public int getInstanceId() {
		return instanceId;
	}

	public void setInstanceId(int instanceId) {
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
		//Gson gson = new Gson();
		GsonBuilder builder = new GsonBuilder();
        builder.excludeFieldsWithoutExposeAnnotation();
        Gson gson = builder.create();
		return gson.toJson(this);
	}

}
