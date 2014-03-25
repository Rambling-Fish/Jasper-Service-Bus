package org.jasper.core.dataprocessor;

import junit.framework.TestCase;

import org.jasper.core.constants.JasperConstants;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

public class TestDataProcessor extends TestCase {
	
	private JsonObject validInput;
	private JsonObject badInput;
	
	@Before
	public void setUp(){
		JsonParser parser = new JsonParser();
		validInput = parser.parse("{\"version\":\"1.0\",\"method\":\"GET\",\"ruri\":\"http://coralcea.ca/jasper/hrData\",\"headers\":{\"content-type\":\"application/json\"},\"parameters\":{\"http://coralcea.ca/jasper/hrSID\":\"1\"}}").getAsJsonObject();
		badInput = parser.parse("{\"http://coralcea.ca/jasper/environmentalSensor/roomTemperature\":\"25R\",\"http://coralcea.ca/jasper/timeStamp\":\"2013-10-14 02:18:45.0903 EDT\"}").getAsJsonObject();
	}
	

	/*
	 * This tests the a successful adding of input with the AGGREGATE scheme 
	 */
	@Test
	public void testAggregateAddSuccess() throws Exception{ 
		System.out.println("============================");
		System.out.println("RUNNING DATA PROCESSOR TESTS");
		System.out.println("============================");
		
		DataProcessor dp = DataProcessorFactory.createDataProcessor(JasperConstants.AGGREGATE_SCHEME);
		try{
			dp.add(validInput);
		}catch (Exception ex){
			TestCase.fail("Exception thrown in testAggregateAddSuccess()");
		}
	}
	
	/*
	 * This tests the default constructor 
	 */
	@Test
	public void testDefaultConstructor() throws Exception{ 
		DataProcessor dp = DataProcessorFactory.createDataProcessor();
		try{
			dp.add(validInput);
		}catch (Exception ex){
			TestCase.fail("Exception thrown in testDefaultConstructor()");
		}
	}
	
	/*
	 * This tests the a successful adding of input with the COALESCE scheme 
	 */
	@Test
	public void testCoalesceAddSuccess() throws Exception{ 
		DataProcessor dp = DataProcessorFactory.createDataProcessor(JasperConstants.COALESCE_SCHEME);
		try{
			dp.add(validInput);
		}catch (Exception ex){
			TestCase.fail("Exception thrown in testCoalesceAddSuccess()");
		}
	}
	
	/*
	 * This tests the a successful return of aggregated data
	 */
	@Test
	public void testAggregateResponseSuccess() throws Exception{ 
		DataProcessor dp = DataProcessorFactory.createDataProcessor(JasperConstants.AGGREGATE_SCHEME);
		dp.add(validInput);
		dp.add(validInput);
		
		JsonElement response = dp.process();
		TestCase.assertNotNull(response);
		TestCase.assertTrue(response.isJsonArray());
		TestCase.assertEquals(2, response.getAsJsonArray().size());
		
	}
	
	/*
	 * This tests the a successful return of coalesced data 
	 */
	@Test
	public void testCoalesceResponseSuccess() throws Exception{ 
		DataProcessor dp = DataProcessorFactory.createDataProcessor(JasperConstants.COALESCE_SCHEME);
		dp.add(validInput);
		dp.add(validInput);
		dp.add(validInput);
		JsonElement response = dp.process();

		TestCase.assertNotNull(response);
		TestCase.assertTrue(response.isJsonObject());
	}
	
	/*
	 * This tests the adding invalid input with AGGREGATE Scheme 
	 */
	@Test
	public void testAggregateAddError() throws Exception{ 
		DataProcessor dp = DataProcessorFactory.createDataProcessor(JasperConstants.AGGREGATE_SCHEME);
		try{
			dp.add(badInput);
		}catch (Exception ex){
			TestCase.assertNotNull(ex);
		}
	}
	
	/*
	 * This tests the adding invalid input with COALESCE Scheme 
	 */
	@Test
	public void testCoalesceAddError() throws Exception{ 
		DataProcessor dp = DataProcessorFactory.createDataProcessor(JasperConstants.COALESCE_SCHEME);
		try{
			dp.add(badInput);
		}catch (Exception ex){
			TestCase.assertNotNull(ex);
		}
	}
	
	/*
	 * This tests the COALESCE response where not all items match 
	 */
	@Test
	public void testCoalesceResponseNotMatch() throws Exception{ 
		DataProcessor dp = DataProcessorFactory.createDataProcessor(JasperConstants.COALESCE_SCHEME);
		try{
			dp.add(validInput);
			JsonObject tmp = new JsonObject();
			tmp.add("method", new JsonPrimitive("GET"));
			dp.add(tmp);
			JsonElement response = dp.process();
			TestCase.assertEquals(null, response);
		}catch (Exception ex){
			TestCase.fail("Exception caught in testCoalesceResponseNotMatch() " + ex);
		}
	}
	
	/*
	 * This tests an invalid scheme being passed in to the process() method, which should default
	 * to aggregate processor 
	 */
	@Test
	public void testInvalidScheme() throws Exception{ 
		DataProcessor dp = DataProcessorFactory.createDataProcessor("Unknown");
		dp.add(validInput);
		dp.add(validInput);
		
		JsonElement response = dp.process();
		TestCase.assertNotNull(response);
		TestCase.assertTrue(response.isJsonArray());
		TestCase.assertEquals(2, response.getAsJsonArray().size());
	}
}
