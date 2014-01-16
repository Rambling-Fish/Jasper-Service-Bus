package org.jasper.jsc;

import java.util.Map;

import org.jasper.jsc.constants.RequestConstants;

public class Request {
		
	private final String version = RequestConstants.VERSION_1_0;
	
	private Method method;
	private String ruri;
	private Map<String,String> headers;
	private Map<String,String> parameters;
	private String rule;
	private byte[] payload;
	
	public Request(Method method, String ruri, Map<String, String> headers) {
		this(method, ruri, headers, null, null, null);
	}
	
	public Request(Method method, String ruri, Map<String, String> headers, Map<String, String> parameters) {
		this(method, ruri, headers, parameters, null, null);
	}
	
	public Request(Method method, String ruri, Map<String, String> headers, Map<String, String> parameters, String rule) {
		this(method, ruri, headers, parameters, rule, null);
	}
	
	public Request(Method method, String ruri, Map<String, String> headers,
			Map<String, String> parameters, String rule, byte[] payload) {
		super();
		this.method = method;
		this.ruri = ruri;
		this.headers = headers;
		this.parameters = parameters;
		this.rule = rule;
		this.payload = payload;
	}
	
	public String getVersion() {
		return version;
	}
	public Method getMethod() {
		return method;
	}
	public void setMethod(Method method) {
		this.method = method;
	}
	public String getRuri() {
		return ruri;
	}
	public void setRuri(String ruri) {
		this.ruri = ruri;
	}
	public Map<String, String> getHeaders() {
		return headers;
	}
	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}
	public Map<String, String> getParameters() {
		return parameters;
	}
	public void setParameters(Map<String, String> parameters) {
		this.parameters = parameters;
	}
	public String getRule() {
		return rule;
	}
	public void setRule(String rule) {
		this.rule = rule;
	}
	public byte[] getPayload() {
		return payload;
	}
	public void setPayload(byte[] payload) {
		this.payload = payload;
	}
}
