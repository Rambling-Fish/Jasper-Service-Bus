package org.jasper.core.notification;

import java.util.List;

import junit.framework.TestCase;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import org.jasper.core.notification.triggers.Trigger;
import org.jasper.core.notification.triggers.TriggerFactory;
import org.jasper.core.notification.util.JsonResponseParser;
import org.junit.Test;

public class TestTriggers extends TestCase {

	private static final String RURI = "http://coralcea.ca/jasper/environmentalSensor/roomTemperature";
	private static final String COMPARE_INT = "compareint";
	private static final String RANGE = "range";
	
	private static final int EXPIRY = 10;
	private static final int POLLING = 5;
	private String tmp = "{ http://coralcea.ca/jasper/environmentalSensor/roomTemperature : 25 ,\n" +
		    "http://coralcea.ca/jasper/timeStamp : 2013-10-14 02:18:45.0903 EDT }";

	/*
	 * This tests the utility class JsonResponeParser 
	 * request.
	 */
	@Test
	public void testJsonParser() { 
		System.out.println("=====================");
		System.out.println("RUNNING TRIGGER TESTS");
		System.out.println("=====================");

		JsonObject tmp1 = new JsonObject();
		tmp1.addProperty("http://coralcea.ca/jasper/environmentalSensor/roomTemperature", 25);
		tmp1.addProperty("http://coralcea.ca/jasper/timeStamp", "2013-10-14 02:18:45.0903 EDT");

		JsonObject tmp2 = new JsonObject();
		tmp2.addProperty("http://coralcea.ca/jasper/environmentalSensor/roomTemperature", "25R");
		tmp2.addProperty("http://coralcea.ca/jasper/timeStamp", "2013-10-14 02:18:45.0903 EDT");

		JsonArray response = new JsonArray();
		JsonResponseParser parser = new JsonResponseParser();
		
		// test passing in empty array
		List<Float> list = parser.parse(response, RURI);
		TestCase.assertEquals(null, list);
		
		// test valid array and a bad room temperature (NaN)
		response.add(tmp1);
		response.add(tmp2);
		list = parser.parse(response, RURI);
		TestCase.assertNotNull(list);
		
		//test with a JsonObject
		list = parser.parse(tmp1, RURI);
		TestCase.assertNotNull(list);
		
	}
	
	/*
	 * This tests the TriggerFactory class
	 */
	@Test
	public void testTriggerFactory() {
		TriggerFactory factory = new TriggerFactory();

		Trigger trig1 = factory.createTrigger(COMPARE_INT, EXPIRY, POLLING, RURI, "gt", "20");
		Trigger trig2 = factory.createTrigger(RANGE, EXPIRY, POLLING, RURI, "10", "25");
		Trigger trig3 = factory.createTrigger("wrong", EXPIRY, POLLING, RURI, "gt", "45");
		Trigger trig4 = factory.createTrigger(COMPARE_INT, EXPIRY, POLLING, RURI);
		Trigger trig5 = factory.createTrigger(RANGE, EXPIRY, POLLING, RURI,"20","ne", "30");

		TestCase.assertNotNull(trig1);
		TestCase.assertNotNull(trig2);
		TestCase.assertNull(trig3);
		TestCase.assertNull(trig4);
		TestCase.assertNull(trig5);
	
		Trigger trigger = new Trigger();
		trigger.evaluate(null);
		trigger.getNotificationExpiry();
		trigger.setPolling(2000);
		trigger.getPolling();
		trigger.setExpiry(10000);
		trigger.getExpiry();
		TestCase.assertNotNull(trigger);
	
	}
	
	/*
	 * This tests the Count Trigger class
	 */
	@Test
	public void testCompareIntTrigger() {
		JsonObject tmp1 = new JsonObject();
		tmp1.addProperty("http://coralcea.ca/jasper/environmentalSensor/roomTemperature", 25);
		tmp1.addProperty("http://coralcea.ca/jasper/timeStamp", "2013-10-14 02:18:45.0903 EDT");

		TriggerFactory factory = new TriggerFactory();
		Trigger gtCompareInt  = factory.createTrigger(COMPARE_INT, 20, POLLING, RURI, "gt", "20");
		gtCompareInt.setNotificationExpiry();
		while(true){
			try{
				if(!gtCompareInt.isNotificationExpired()){
					Thread.sleep(100);
				}
				else break;
			} catch (Exception ex){
				System.out.println("Exception occurred during TestNotification.testcompareIntTrigger");
			}
		}
		Trigger gtCompareInt2 = factory.createTrigger(COMPARE_INT, 20, POLLING, RURI, "gt", "25");
		Trigger eqCompareInt  = factory.createTrigger(COMPARE_INT, EXPIRY, POLLING, RURI, "eq", "25");
		Trigger eqCompareInt2 = factory.createTrigger(COMPARE_INT, EXPIRY, POLLING, RURI, "eq", "5");
		Trigger ltCompareInt  = factory.createTrigger(COMPARE_INT, EXPIRY, POLLING, RURI, "lt", "38");
		Trigger ltCompareInt2 = factory.createTrigger(COMPARE_INT, EXPIRY, POLLING, RURI, "lt", "25");
		Trigger neCompareInt  = factory.createTrigger(COMPARE_INT, EXPIRY, POLLING, RURI, "ne", "26");
		Trigger neCompareInt2 = factory.createTrigger(COMPARE_INT, EXPIRY, POLLING, RURI, "ne", "25");
		Trigger geCompareInt  = factory.createTrigger(COMPARE_INT, EXPIRY, POLLING, RURI, "ge", "22");
		Trigger geCompareInt2 = factory.createTrigger(COMPARE_INT, EXPIRY, POLLING, RURI, "ge", "27");
		Trigger leCompareInt  = factory.createTrigger(COMPARE_INT, EXPIRY, POLLING, RURI, "le", "25");
		Trigger leCompareInt2 = factory.createTrigger(COMPARE_INT, EXPIRY, POLLING, RURI, "le", "24");
		Trigger badCompareInt = factory.createTrigger(COMPARE_INT, EXPIRY, POLLING, RURI, "bad", "25");
		Trigger right         = factory.createTrigger(COMPARE_INT, EXPIRY, POLLING, RURI, "lt", "http://jasper.com");

		JsonArray response = new JsonArray();
		response.add(tmp1);
		
		// test all the operands
		TestCase.assertFalse(gtCompareInt2.evaluate(response));
		TestCase.assertTrue(gtCompareInt.evaluate(response));
		TestCase.assertTrue(eqCompareInt.evaluate(response));
		TestCase.assertFalse(eqCompareInt2.evaluate(response));
		TestCase.assertTrue(ltCompareInt.evaluate(response));
		TestCase.assertFalse(ltCompareInt2.evaluate(response));
		TestCase.assertTrue(neCompareInt.evaluate(response));
		TestCase.assertFalse(neCompareInt2.evaluate(response));
		TestCase.assertTrue(geCompareInt.evaluate(response));
		TestCase.assertFalse(geCompareInt2.evaluate(response));
		TestCase.assertTrue(leCompareInt.evaluate(response));
		TestCase.assertFalse(leCompareInt2.evaluate(response));
		TestCase.assertFalse(badCompareInt.evaluate(response));
		
		//Test empty response passed into trigger
		JsonArray responseEmpty = new JsonArray();
		TestCase.assertFalse(gtCompareInt.evaluate(responseEmpty));

	}
	
	/*
	 * This tests the Range Trigger class
	 */
	@Test
	public void testRangeTrigger() {
		JsonObject tmp1 = new JsonObject();
		tmp1.addProperty("http://coralcea.ca/jasper/environmentalSensor/roomTemperature", 25);
		tmp1.addProperty("http://coralcea.ca/jasper/timeStamp", "2013-10-14 02:18:45.0903 EDT");

		JsonArray response = new JsonArray();
		response.add(tmp1);
		TriggerFactory factory = new TriggerFactory();
		Trigger range1  = factory.createTrigger(RANGE, EXPIRY, POLLING, RURI, "20", "30");
		Trigger range2  = factory.createTrigger(RANGE, EXPIRY, POLLING, RURI, "26", "36");
		Trigger range3  = factory.createTrigger(RANGE, EXPIRY, POLLING, "30T", "25", "36");
		
		TestCase.assertTrue(range1.evaluate(response));
		TestCase.assertFalse(range2.evaluate(response));
		
		JsonArray response2 = new JsonArray();
		range3.evaluate(response2);
		JsonPrimitive jPrim = new JsonPrimitive("helloWorld");
		response2.add(jPrim);
		TestCase.assertFalse(range1.evaluate(response2));
	}
}
