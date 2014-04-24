package org.jasper.core.util;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class TestJsonLDTransformer extends TestCase {
	private JsonLDTransformer classUnderTest;
	private JsonParser jParser  = new JsonParser();

	/**
	 * This method tests that a JsonArray that only contains primitives is returned
	 * without any transformation
	 */
	@Test
	public void testPrimitiveArray(){
		JsonArray primitiveArr  = new JsonArray();
		JsonElement prim1 = jParser.parse("one");
		JsonElement prim2 = jParser.parse("two");
		primitiveArr.add(prim1);
		primitiveArr.add(prim2);
		JsonElement parsedResponse = classUnderTest.parseResponse(primitiveArr);
		TestCase.assertSame(primitiveArr, parsedResponse);
	}
	
	/**
	 * This method tests that a JsonArray that contains JsonObjects is returned
	 * in JSON-LD format. Test one expects a context block, test two does expects
	 * an empty context block
	 */
	@Test
	public void testComplexArrays(){
		JsonArray objArr1  = new JsonArray();
		JsonArray objArr2  = new JsonArray();
		Map<String,String> map = new HashMap<String,String>();
		Gson gson = new Gson();
		map.put("http://coralcea.ca/demo/bpm", "52");
		map.put("http://coralcea.ca/demo/timestamp", "2014-04-16");
		map.put("http://coralcea.ca/demo/patient/id", "http://pid01");
		map.put("http://coralcea.ca/demo/sensor/id", "srID02");
		
		JsonElement jsonTree = gson.toJsonTree(map, Map.class);
		objArr1.add(jsonTree);
		map.put("http://coralcea.ca/demo/bpm", "99");
		map.put("http://coralcea.ca/demo/timestamp", "2014-04-17");
		map.put("http://coralcea.ca/demo/patient/id", "http://pid99");
		map.put("http://coralcea.ca/demo/sensor/id", "srID03");
		jsonTree = gson.toJsonTree(map, Map.class);
		objArr1.add(jsonTree);
		
		JsonElement parsedResponse = classUnderTest.parseResponse(objArr1);
		TestCase.assertNotNull(parsedResponse);
		TestCase.assertTrue(parsedResponse.getAsJsonArray().get(0).toString().contains("@context"));
		JsonObject jsonObj = parsedResponse.getAsJsonArray().get(0).getAsJsonObject();
		TestCase.assertTrue(jsonObj.getAsJsonObject().get("@context").toString().length() > 2);
		
		//Test that the response does not have a context since there are no uris that contain "http://"
		map.clear();
		map.put("bpm", "99");
		map.put("timestamp", "2014-04-17");
		map.put("patient_id", "http://pid99");
		map.put("sensor_id", "srID03");
		jsonTree = gson.toJsonTree(map, Map.class);
		objArr2.add(jsonTree);
		
		parsedResponse = classUnderTest.parseResponse(objArr2);
		TestCase.assertNotNull(parsedResponse);
		jsonObj = parsedResponse.getAsJsonArray().get(0).getAsJsonObject();
		TestCase.assertTrue(jsonObj.getAsJsonObject().get("@context").toString().length() == 2);
	}
	
	/**
	 * This method tests that various JsonObjects gets transformed correctly
	 */
	@Test
	public void testJsonObjects(){
		JsonObject jsonObj = new JsonObject();
		jsonObj.addProperty("http://coralcea.ca/demo/wardId", "Wing-5-Floor-3-Ward-4");
		jsonObj.addProperty("http://coralcea.ca/demo/data", "some-data");
		jsonObj.addProperty("http://coralcea.ca/demo/more/data", "more-data");
		jsonObj.addProperty("non-uri", "should not be in context");
		
		JsonElement parsedResponse = classUnderTest.parseResponse(jsonObj);
		TestCase.assertNotNull(parsedResponse);
		TestCase.assertTrue(parsedResponse.getAsJsonObject().get("@context").toString().length() > 2);
		// Check that the one item that did not contain 'http://' does not end up in the context
		TestCase.assertFalse(parsedResponse.getAsJsonObject().get("@context").toString().contains("non-uri"));
		
		// JsonObject with embedded arrays
		JsonArray primitiveArr  = new JsonArray();
		JsonElement prim1 = jParser.parse("one");
		JsonElement prim2 = jParser.parse("two");
		primitiveArr.add(prim1);
		primitiveArr.add(prim2);
		
		JsonArray jsonArr  = new JsonArray();
		Map<String,String> map = new HashMap<String,String>();
		Gson gson = new Gson();
		map.put("http://coralcea.ca/demo/bpm", "52");
		map.put("http://coralcea.ca/demo/timestamp", "2014-04-16");
		map.put("http://coralcea.ca/demo/patient/id", "http://pid01");
		map.put("http://coralcea.ca/demo/sensor/id", "srID02");
		
		JsonElement jsonTree = gson.toJsonTree(map, Map.class);
		jsonArr.add(jsonTree);
		map.put("http://coralcea.ca/demo/bpm", "99");
		map.put("http://coralcea.ca/demo/timestamp", "2014-04-17");
		map.put("http://coralcea.ca/demo/patient/id", "http://pid99");
		map.put("http://coralcea.ca/demo/sensor/id", "srID03");
		jsonTree = gson.toJsonTree(map, Map.class);
		jsonArr.add(jsonTree);
		jsonObj.add("http://coralcea.ca/demo/patient/primitive/array", primitiveArr);
		jsonObj.add("http://coralcea.ca/demo/patient/complex/array", jsonArr);
		
		parsedResponse = classUnderTest.parseResponse(jsonObj);
		TestCase.assertNotNull(parsedResponse);
	}
	
	/**
	 * This method tests that a JsonPrimitive is returned as is
	 */
	@Test
	public void testJsonPrimitive(){
		JsonElement primitive = jParser.parse("primitive");
		JsonElement parsedResponse = classUnderTest.parseResponse(primitive);
		TestCase.assertSame(primitive, parsedResponse);
	}
	
	@Before
	public void setUp() throws Exception {
		
		classUnderTest = new JsonLDTransformer();
	}

	@After
	public void tearDown() throws Exception {
		classUnderTest = null;
	}
	
}
