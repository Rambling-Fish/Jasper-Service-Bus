package org.jasper.jsc;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.jasper.jsc.constants.RequestConstants;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Request {
		
	private final String version = RequestConstants.VERSION_1_0;
	
	private Method method;
	private String ruri;
	private Map<String,String> headers;
	private JsonObject parameters;
	private String rule;
	private byte[] payload;
	
	public Request(Method method, String ruri, Map<String, String> headers) {
		super();
		this.method = method;
		this.ruri = ruri;
		this.headers = headers;
		this.parameters = null;
		this.rule = null;
		this.payload = null;
	}
	
	public Request(Method method, String ruri, Map<String, String> headers, Map<String, String> parameters) {
		this(method, ruri, headers, parseParameters(parameters), null, null);
	}
	
	public Request(Method method, String ruri, Map<String, String> headers, Map<String, String> parameters, String rule) {
		this(method, ruri, headers, parseParameters(parameters), rule, null);
	}
	
	public Request(Method method, String ruri, Map<String, String> headers,	Map<String, String> parameters, String rule, byte[] payload) {
		this(method, ruri, headers, parseParameters(parameters), rule, payload);
	}
	
	public Request(Method method, String ruri, Map<String, String> headers, JsonObject parameters) {
		this(method, ruri, headers, parameters, null, null);
	}
	
	public Request(Method method, String ruri, Map<String, String> headers, JsonObject parameters, String rule) {
		this(method, ruri, headers, parameters, rule, null);
	}
	
	public Request(Method method, String ruri, Map<String, String> headers,	JsonObject parameters, String rule, byte[] payload) {
		super();
		this.method = method;
		this.ruri = ruri;
		this.headers = headers;
		this.parameters = parameters;
		this.rule = rule;
		this.payload = payload;
	}
	
	private static JsonObject parseParameters(Map<String, String> params) {
		JsonObject result = new JsonObject();
		JsonParser jParser = new JsonParser();
		
		if(params == null) return result;
		
		for(Entry<String, String> entry:params.entrySet()){
			result.add(entry.getKey(), jParser.parse(entry.getValue()));
		}
		
		return result;
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
	public JsonObject getParameters() {
		return parameters;
	}
	public void setParameters(JsonObject parameters) {
		this.parameters = parameters;
	}
	public Map<String, String> getParametersAsMap() {
		 Map<String, String> result = new HashMap<String, String>();
		
		 for(Entry<String, JsonElement> entry:parameters.entrySet()){
			 result.put(entry.getKey(), entry.getValue().toString());
		 }
		 
		return result;
	}
	public void parseAndSetParameters(Map<String, String> parameters) {
		this.parameters = parseParameters(parameters);
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
