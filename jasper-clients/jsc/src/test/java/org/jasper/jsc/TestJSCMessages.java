package org.jasper.jsc;

import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;

import junit.framework.TestCase;

import org.jasper.jsc.constants.RequestConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class TestJSCMessages {

	private String ruri = "http://coralcea.ca/jasper/msData";
	private Map<String,String> paramsMap = new HashMap<String,String>();
	private Map<String,String> headersMap = new HashMap<String,String>();
	private JsonObject headerObj;
	private JsonObject paramsObj;
	private JsonParser parser = new JsonParser();
	private byte[] payload = new byte[1];
	
	
	@Before
	public void setUp() throws Exception {	
		
	}

	@After
	public void tearDown() throws Exception {
		
	}
	

	@Test
	public void testRequest() throws JMSException {
		Request request;
		String headerStr = "{\"headers\":{\"content-type\":\"application/json\"}}";
		String paramsStr = "{\"parameters\":{\"http://coralcea.ca/jasper/hrSID\":\"01\"}}";
		String rule = "compareint(ruri,gt,10)";
		headerObj = parser.parse(headerStr).getAsJsonObject();
		paramsObj = parser.parse(paramsStr).getAsJsonObject();
		
		// test all constructors()
		request = new Request(Method.GET, ruri, headerObj);
		TestCase.assertNotNull(request);
		paramsMap.put("http://coralcea.ca/jasper/hrSID", "01");
		
		request = null;
		headersMap.put("expires", "10");
		request = new Request(Method.GET, ruri, headersMap, paramsMap);
		
		request = null;
		request = new Request(Method.SUBSCRIBE, ruri, headerObj, paramsMap); 
		TestCase.assertNotNull(request);
		request = null;
		paramsMap = null;
		request = new Request(Method.SUBSCRIBE, ruri, headerObj, paramsMap); 
		TestCase.assertNotNull(request);
		
		request = null;
		request = new Request(Method.POST, ruri, headersMap, paramsMap, rule);
		TestCase.assertNotNull(request);
		
		request = null;
		request = new Request(Method.POST, ruri, headersMap, paramsMap, rule, payload);
		TestCase.assertNotNull(request);
		
		request = null;
		request = new Request(Method.GET, ruri, headersMap); 
		TestCase.assertNotNull(request);
		
		request = null;
		request = new Request(Method.GET, ruri, headerObj, paramsMap, rule, payload);
		TestCase.assertNotNull(request);
		
		request = null;
		request = new Request(Method.POST, ruri, headersMap, paramsObj);
		TestCase.assertNotNull(request);
		
		request = null;
		request = new Request(Method.GET, ruri, headerObj, paramsObj);
		TestCase.assertNotNull(request);
		
		request = null;
		request = new Request(Method.GET, ruri, headersMap, paramsObj, rule);
		TestCase.assertNotNull(request);  
		
		request = null;
		request = new Request(Method.SUBSCRIBE, ruri, headerObj, paramsObj, rule);
		TestCase.assertNotNull(request);   
		
		// test getters and setters
		request.setRuri("test");
		TestCase.assertEquals("test", request.getRuri());
		TestCase.assertEquals(RequestConstants.VERSION_1_0, request.getVersion());
		request.setMethod(Method.DELETE);
		TestCase.assertEquals(Method.DELETE, request.getMethod());
		request.setHeaders(headerObj);
		TestCase.assertNotNull(request.getHeaders());
		request.setRule("rule");
		TestCase.assertEquals("rule", request.getRule());
		request.setPayload(new byte[1]);
		TestCase.assertNotNull(request.getPayload());
		paramsMap = new HashMap<String,String>();
		paramsMap.put("http://coralcea.ca/jasper/hrSID", "01");
		request.parseAndSetHeaders(paramsMap);
		request.setParameters(headerObj);
		TestCase.assertNotNull(request.getParameters());
		request.getParametersAsMap();
		Map<String,String> map = request.getHeadersAsMap();
		TestCase.assertNotNull(map);
		request.parseAndSetParameters(paramsMap);
	}
	
	@Test
	public void testResponse() throws JMSException {
		Response response;
		int code = 200;
		String reason = "OK";
		String description = "Success";
		byte[] arr = new byte[]{(byte)0x10};
		
		// test constructors
		response = new Response(code, reason, payload);
		TestCase.assertNotNull(response);
		
		response = null;
		headersMap.put("expires", "10");
		response = new Response(code, reason, description, headersMap, payload);
		TestCase.assertNotNull(response);
		
		// test getters and setters
		TestCase.assertNotNull(response.getVersion());
		TestCase.assertEquals(200, response.getCode());
		response.setCode(404);
		TestCase.assertEquals(404, response.getCode());
		TestCase.assertEquals("OK", response.getReason());
		TestCase.assertEquals("Success", response.getDescription());
		TestCase.assertNotNull(response.getHeaders());
		TestCase.assertNotNull(response.getPayload());
		response.setReason("Error");
		response.setDescription("Failure");
		response.setPayload(arr);
		response.setHeaders(headersMap);
		TestCase.assertEquals("Error", response.getReason());
		TestCase.assertEquals("Failure", response.getDescription());
		
	}

}
