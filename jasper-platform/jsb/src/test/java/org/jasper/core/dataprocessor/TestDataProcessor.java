package org.jasper.core.dataprocessor;

import junit.framework.TestCase;

import org.jasper.core.constants.JasperConstants;
import org.junit.Test;

public class TestDataProcessor extends TestCase {
	
	private String validInput = "{\"version\":\"1.0\",\"method\":\"GET\",\"ruri\":\"http://coralcea.ca/jasper/hrData\",\"headers\":{\"content-type\":\"application/json\"},\"parameters\":{\"http://coralcea.ca/jasper/hrSID\":\"1\"}}";
	private String badInput = "{ http://coralcea.ca/jasper/environmentalSensor/roomTemperature : 25R ,\n" +
		    "http://coralcea.ca/jasper/timeStamp : 2013-10-14 02:18:45.0903 EDT }";

	/*
	 * This tests the a successful adding of input with the AGGREGATE scheme 
	 */
	@Test
	public void testAggregateAddSuccess() throws Exception{ 
		System.out.println("============================");
		System.out.println("RUNNING DATA PROCESSOR TESTS");
		System.out.println("============================");
		
		DataProcessor dp = new DataProcessor(JasperConstants.AGGREGATE_SCHEME);
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
		DataProcessor dp = new DataProcessor();
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
		DataProcessor dp = new DataProcessor(JasperConstants.COALESCE_SCHEME);
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
		DataProcessor dp = new DataProcessor(JasperConstants.AGGREGATE_SCHEME);
		dp.add(validInput);
		dp.add(validInput);
		
		String response = dp.process();
		TestCase.assertNotNull(response);
		// Should have two items in response
		TestCase.assertEquals(349, response.length());
	}
	
	/*
	 * This tests the a successful return of coalesced data 
	 */
	@Test
	public void testCoalesceResponseSuccess() throws Exception{ 
		DataProcessor dp = new DataProcessor(JasperConstants.COALESCE_SCHEME);
		dp.add(validInput);
		dp.add(validInput);
		dp.add(validInput);
		String response = dp.process();

		TestCase.assertNotNull(response);
		// Should only have 1 item in response
		TestCase.assertEquals(173, response.length());
	}
	
	/*
	 * This tests the adding invalid input with AGGREGATE Scheme 
	 */
	@Test
	public void testAggregateAddError() throws Exception{ 
		DataProcessor dp = new DataProcessor(JasperConstants.AGGREGATE_SCHEME);
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
		DataProcessor dp = new DataProcessor(JasperConstants.COALESCE_SCHEME);
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
		DataProcessor dp = new DataProcessor(JasperConstants.COALESCE_SCHEME);
		try{
			dp.add(validInput);
			dp.add("{\"method\":\"GET\"}");
			String response = dp.process();
			TestCase.assertEquals(null, response);
		}catch (Exception ex){
			TestCase.fail("Exception caught in testCoalesceResponseNotMatch() " + ex);
		}
	}
	
	/*
	 * This tests an invalid scheme being passed in to the process() method 
	 */
	@Test
	public void testInvalidScheme() throws Exception{ 
		DataProcessor dp = new DataProcessor("Unknown");
		dp.add(validInput);
		String response = dp.process();
		TestCase.assertEquals(null, response);
	}
	
	/*
	 * This tests a non-JSON input 
	 */
	@Test
	public void testNonJsonInput() throws Exception{ 
		DataProcessor dp = new DataProcessor(JasperConstants.AGGREGATE_SCHEME);
		DataProcessor dp2 = new DataProcessor(JasperConstants.COALESCE_SCHEME);
		dp.add("hello");
		dp.add("<xml/>");
		dp2.add("hello");
		dp2.add("<xml/>");
	}
}
