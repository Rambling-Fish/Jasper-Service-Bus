package org.jasper.core.notification;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.MessageProducer;
import javax.jms.Session;

import junit.framework.TestCase;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.jena.atlas.json.JsonArray;
import org.jasper.core.JECore;
import org.jasper.core.constants.JasperConstants;
import org.jasper.core.delegate.Delegate;
import org.jasper.core.delegate.DelegateFactory;
import org.jasper.core.notification.triggers.Trigger;
import org.jasper.core.notification.triggers.TriggerFactory;
import org.jasper.core.notification.util.JsonResponseParser;
import org.junit.After;
import org.junit.Before;
//
import org.junit.Test;

import com.hazelcast.config.Config;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
//
public class TestNotification  extends TestCase {
//
//	private static final String RURI = "http://coralcea.ca/jasper/environmentalSensor/roomTemperature";
//	private static final String COMPARE_INT = "compareint";
//	private static final String RANGE = "range";
//	
//	private static final int EXPIRY = 10;
//	private static final int POLLING = 5;
//	private Connection connection;
//	private DelegateFactory delegateFactory;
//	private ActiveMQConnectionFactory connectionFactory;
//	private Session session;
//	private Destination globalQueue;
//	private MessageProducer producer;
//	private ExecutorService executorService;
//	private Delegate delegate;
//	private JECore core;
//	private HazelcastInstance hz;
//	private String tmp = "{ http://coralcea.ca/jasper/environmentalSensor/roomTemperature : 25 ,\n" +
//		    "http://coralcea.ca/jasper/timeStamp : 2013-10-14 02:18:45.0903 EDT }";
//
//	/*
//	 * This tests the utility class JsonResponeParser 
//	 * request.
//	 */
//	@Test
//	public void testJsonParser() { 
//		System.out.println("==========================");
//		System.out.println("RUNNING NOTIFICATION TESTS");
//		System.out.println("==========================");
//		String tmp2 = "{ http://coralcea.ca/jasper/environmentalSensor/roomTemperature : 25R ,\n" +
//			    "http://coralcea.ca/jasper/timeStamp : 2013-10-14 02:18:45.0903 EDT }"; 
//		JsonArray response = new JsonArray();
//		JsonResponseParser parser = new JsonResponseParser();
//		
//		// test passing in empty array
//		List<Integer> list = parser.parse(response, RURI);
//		
//		// test valid array and a bad room temperature (NaN)
//		response.add(tmp);
//		response.add(tmp2);
//		
//		list = parser.parse(response, RURI);
//		TestCase.assertNotNull(list);
//		
//	}
//	
//	/*
//	 * This tests the TriggerFactory class
//	 */
//	@Test
//	public void testTriggerFactory() {
//		TriggerFactory factory = new TriggerFactory();
//
//		Trigger trig1 = factory.createTrigger(COMPARE_INT, EXPIRY, POLLING, RURI, "gt", "20");
//		Trigger trig2 = factory.createTrigger(RANGE, EXPIRY, POLLING, RURI, "10", "25");
//		Trigger trig3 = factory.createTrigger("wrong", EXPIRY, POLLING, RURI, "gt", "45");
//		Trigger trig4 = factory.createTrigger(COMPARE_INT, EXPIRY, POLLING, RURI);
//		Trigger trig5 = factory.createTrigger(RANGE, EXPIRY, POLLING, RURI,"20","ne", "30");
//
//		TestCase.assertNotNull(trig1);
//		TestCase.assertNotNull(trig2);
//		TestCase.assertNull(trig3);
//		TestCase.assertNull(trig4);
//		TestCase.assertNull(trig5);
//		
//		Trigger trigger = new Trigger();
//		trigger.evaluate(null);
//		trigger.getNotificationExpiry();
//		trigger.setPolling(2000);
//		trigger.getPolling();
//		trigger.setExpiry(10000);
//		trigger.getExpiry();
//		TestCase.assertNotNull(trigger);
//	
//	}
//	
//	/*
//	 * This tests the Count Trigger class
//	 */
//	@Test
//	public void testCompareIntTrigger() {
//		TriggerFactory factory = new TriggerFactory();
//		Trigger gtCompareInt  = factory.createTrigger(COMPARE_INT, 20, POLLING, RURI, "gt", "20");
//		gtCompareInt.setNotificationExpiry();
//		while(true){
//			try{
//				if(!gtCompareInt.isNotificationExpired()){
//					System.out.println(1);
//					Thread.sleep(100);
//				}
//				else break;
//			} catch (Exception ex){
//				System.out.println("Exception occurred during TestNotification.testcompareIntTrigger");
//			}
//		}
//		Trigger gtCompareInt2 = factory.createTrigger(COMPARE_INT, 20, POLLING, RURI, "gt", "25");
//		Trigger eqCompareInt  = factory.createTrigger(COMPARE_INT, EXPIRY, POLLING, RURI, "eq", "25");
//		Trigger eqCompareInt2 = factory.createTrigger(COMPARE_INT, EXPIRY, POLLING, RURI, "eq", "5");
//		Trigger ltCompareInt  = factory.createTrigger(COMPARE_INT, EXPIRY, POLLING, RURI, "lt", "38");
//		Trigger ltCompareInt2 = factory.createTrigger(COMPARE_INT, EXPIRY, POLLING, RURI, "lt", "25");
//		Trigger neCompareInt  = factory.createTrigger(COMPARE_INT, EXPIRY, POLLING, RURI, "ne", "26");
//		Trigger neCompareInt2 = factory.createTrigger(COMPARE_INT, EXPIRY, POLLING, RURI, "ne", "25");
//		Trigger geCompareInt  = factory.createTrigger(COMPARE_INT, EXPIRY, POLLING, RURI, "ge", "22");
//		Trigger geCompareInt2 = factory.createTrigger(COMPARE_INT, EXPIRY, POLLING, RURI, "ge", "27");
//		Trigger leCompareInt  = factory.createTrigger(COMPARE_INT, EXPIRY, POLLING, RURI, "le", "25");
//		Trigger leCompareInt2 = factory.createTrigger(COMPARE_INT, EXPIRY, POLLING, RURI, "le", "24");
//		Trigger badCompareInt = factory.createTrigger(COMPARE_INT, EXPIRY, POLLING, RURI, "bad", "25");
//		Trigger left          = factory.createTrigger(COMPARE_INT, EXPIRY, POLLING, "23Q", "lt", "40");
//		Trigger right         = factory.createTrigger(COMPARE_INT, EXPIRY, POLLING, RURI, "lt", "http://jasper.com");
//
//		JsonArray response = new JsonArray();
//		response.add(tmp);
//		
//		// test all the operands
//		TestCase.assertFalse(gtCompareInt2.evaluate(response));
//		TestCase.assertTrue(gtCompareInt.evaluate(response));
//		TestCase.assertTrue(eqCompareInt.evaluate(response));
//		TestCase.assertFalse(eqCompareInt2.evaluate(response));
//		TestCase.assertTrue(ltCompareInt.evaluate(response));
//		TestCase.assertFalse(ltCompareInt2.evaluate(response));
//		TestCase.assertTrue(neCompareInt.evaluate(response));
//		TestCase.assertFalse(neCompareInt2.evaluate(response));
//		TestCase.assertTrue(geCompareInt.evaluate(response));
//		TestCase.assertFalse(geCompareInt2.evaluate(response));
//		TestCase.assertTrue(leCompareInt.evaluate(response));
//		TestCase.assertFalse(leCompareInt2.evaluate(response));
//		TestCase.assertFalse(badCompareInt.evaluate(response));
//		left.evaluate(response);
//		
//		//Test empty response passed into trigger
//		response.clear();
//		TestCase.assertFalse(gtCompareInt.evaluate(response));
//
//	}
//	
//	/*
//	 * This tests the Range Trigger class
//	 */
//	@Test
//	public void testRangeTrigger() {
//		JsonArray response = new JsonArray();
//		response.add(tmp);
//		TriggerFactory factory = new TriggerFactory();
//		Trigger range1  = factory.createTrigger(RANGE, EXPIRY, POLLING, RURI, "20", "30");
//		Trigger range2  = factory.createTrigger(RANGE, EXPIRY, POLLING, RURI, "26", "36");
//		Trigger range3  = factory.createTrigger(RANGE, EXPIRY, POLLING, "30T", "25", "36");
//		
//		TestCase.assertTrue(range1.evaluate(response));
//		TestCase.assertFalse(range2.evaluate(response));
//		
//		response.clear();
//		range3.evaluate(response);
//		tmp = "helloWorld";
//		response.add(tmp);
//		TestCase.assertFalse(range1.evaluate(response));
//		
//	}
//	
//	@Before
//	public void setUp() throws Exception {
//		 System.setProperty("delegate-property-file", "../zipRoot/jsb-core/config/delegate.properties");
//		 connectionFactory = new ActiveMQConnectionFactory("vm://localhost");
//		 
//		 core = JECore.getInstance();
//		 
//		 Config cfg = new Config();
//		 GroupConfig groupConfig = new GroupConfig("testNotificationJunitTestingSuite", "testNotificationJunitTestingSuite_" + System.currentTimeMillis());
//		 cfg.setGroupConfig(groupConfig);
//		 hz = Hazelcast.newHazelcastInstance(cfg);
//		 
//		 delegateFactory = new DelegateFactory(false, core);
//
//        // Create a Connection
//        connectionFactory.setUserName(JasperConstants.JASPER_ADMIN_USERNAME);
//        connectionFactory.setPassword(JasperConstants.JASPER_ADMIN_PASSWORD);
//        connection = connectionFactory.createConnection();
//        connection.start();
//		
//		
//		executorService = Executors.newCachedThreadPool();
//		
//		delegate = delegateFactory.createDelegate();
//		executorService.execute(delegate);
//       
//		session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
//		globalQueue = session.createQueue(JasperConstants.DELEGATE_GLOBAL_QUEUE);
//		producer = session.createProducer(globalQueue);
//		producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
//		producer.setTimeToLive(30000);
//	}
//
//	@After
//	public void tearDown() throws Exception {
//		// Clean up - no need to close connection as that will be done in JasperBroker
//		session.close();
//		connection.close();
//
//		delegate.shutdown();
//		hz.getLifecycleService().shutdown();
//		Thread.sleep(500);
//		session           = null;
//		connection        = null;
//		producer          = null;
//		globalQueue       = null;
//		delegate         = null;
//		executorService   = null;
//		connectionFactory = null;
//		delegateFactory   = null;
//		core              = null;
//		
//		Thread.sleep(2000);
//		
//	}
}
