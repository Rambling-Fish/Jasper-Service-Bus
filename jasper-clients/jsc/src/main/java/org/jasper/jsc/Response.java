package org.jasper.jsc;

import java.util.Map;

import org.jasper.jsc.constants.ResponseConstants;

public class Response {

	private final String version = ResponseConstants.VERSION_1_0;

	private int code;
	private String reason;
	private String description;
	private Map<String,String> headers;
	private byte[] payload;
	
	public Response(int code, String reason, byte[] payload) {
		this(code, reason, null, null, payload);
	}
	
	public Response(int code, String reason, String description,
			Map<String, String> headers, byte[] payload) {
		super();
		this.code = code;
		this.reason = reason;
		this.description = description;
		this.headers = headers;
		this.payload = payload;
	}
	
	public String getVersion() {
		return version;
	}
	public int getCode() {
		return code;
	}
	public void setCode(int code) {
		this.code = code;
	}
	public String getReason() {
		return reason;
	}
	public void setReason(String reason) {
		this.reason = reason;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public Map<String, String> getHeaders() {
		return headers;
	}
	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}
	public byte[] getPayload() {
		return payload;
	}
	public void setPayload(byte[] payload) {
		this.payload = payload;
	}	
}
