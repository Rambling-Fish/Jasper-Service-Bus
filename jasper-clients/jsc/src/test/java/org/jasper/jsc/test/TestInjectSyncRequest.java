/**
 * 
 */
package org.jasper.jsc.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.UnsupportedEncodingException;

import org.apache.activemq.broker.BrokerService;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.jasper.jsc.JClientProvider;
import org.jasper.jsc.ReplyThread;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


//import org.codehaus.jettison.json.JSONObject;

import com.jayway.jsonpath.JsonPath;

/**
 * @author pierrerahme
 * 
 */
public class TestInjectSyncRequest {
	static Logger log = Logger.getLogger(TestInjectSyncRequest.class.getName());
	static final int threadCount = 2;
	private final String QUEUE_NAME = "JClientQueue";
	private final String DELEGATE_GLOBAL_QUEUE = "jms.jasper.delegate.global.queue";

	private static BrokerService broker;
	public static JClientProvider JCP;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		startBroker();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		broker.stop();
		JClientProvider.shutdown();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		JCP = JClientProvider.getTestInstance();

		/*
		 * for (int i = 0; i <= threadCount+1; i++) { ReplyThread reply = new
		 * ReplyThread(QueueName); reply.start(); }
		 * 
		 * Thread monitor = new Thread(new MonitorExecutor( (ThreadPoolExecutor)
		 * JClientProvider.execute)); monitor.setDaemon(true); monitor.start();
		 */
	}

	private static void startBroker() {
		broker = new BrokerService();
		broker.setUseJmx(true);
		broker.setPersistent(false);
		broker.setBrokerName("Broker_for_stubbing_activemq");
		try {
			broker.addConnector("tcp://0.0.0.0:61616");
			broker.start();
		} catch (Exception e) {
			System.out.println("in startBroker - exception is: " + e);
			e.printStackTrace();
		}
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		// JClientProvider.shutdown();
	}

	/**
	 * Test method for
	 * {@link com.pierre.thread.manager.RequestCallable#RequestCallable(java.lang.String)}
	 * .
	 */

	@Test
	public final void testRequestCallable() {

		ReplyThread reply;
		try {
			reply = new ReplyThread(QUEUE_NAME);
			reply.start();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		String whatdoIget = null;
		try {
			whatdoIget = JCP.FetchDataFromJasper(
					"coralcea.ca.jasper.MedicalSensorData.HeartRate",
					QUEUE_NAME);
			log.info(whatdoIget);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		assertNotNull(whatdoIget);
	}

	@Test
	public final void testRequestCallableDefaultQueue() {

		ReplyThread reply;
		try {
			reply = new ReplyThread(DELEGATE_GLOBAL_QUEUE);
			reply.start();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		String whatdoIget = null;
		try {
			whatdoIget = JCP.FetchDataFromJasper(
					"coralcea.ca.jasper.MedicalSensorData.HeartRate",DELEGATE_GLOBAL_QUEUE);
			log.info(whatdoIget);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		assertNotNull(whatdoIget);
	}

	/**
	 * Test method for
	 * {@link com.pierre.thread.manager.RequestCallable#RequestCallable(java.lang.String)}
	 * .
	 */

	@Test
	public final void testRequestCallables() {

		for (int i = 0; i <= threadCount; i++) {
			ReplyThread reply;
			try {
				reply = new ReplyThread(QUEUE_NAME);
				reply.start();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		for (int i = 0; i <= threadCount; i++) {
			String whatdoIget = null;
			try {
				whatdoIget = JCP.FetchDataFromJasper(
						"coralcea.ca.jasper.MedicalSensorData.HeartRate",
						QUEUE_NAME);
				log.info(whatdoIget);
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// assertEquals("sent and receive the correct object from the server",
			// "HR data", "HR data");
			assertNotNull(whatdoIget);
		}

	}

	@Test
	public final void testTimeOutCallablesWithBadQueue() {

		ReplyThread reply;
		try {
			reply = new ReplyThread(QUEUE_NAME);
			reply.start();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		String whatdoIget = null;
		String expected = "{ }";
		expected = JSONObject.quote(expected);
		System.out.println("expected is " + expected);

		try {
			whatdoIget = JCP.FetchDataFromJasper(
					"coralcea.ca.jasper.MedicalSensorData.HeartRate",
					"badQueueName");
			log.info(whatdoIget);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		assertEquals("sent and receive the correct object from the server",
				whatdoIget, expected);
	}

	/*
	 * 
	@Test
	public final void testTimeOutCallablesWithBadURI() {

		ReplyThread reply;
		try {
			reply = new ReplyThread(QueueName);
			reply.start();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		String whatdoIget = null;
		String expected = "{ }";
		expected = JSONObject.quote(expected);
		System.out.println("expected is " + expected);

		try {
			whatdoIget = JCP.FetchDataFromJasper(
					"/CoralCEA/HeartRate?patient=zimmerman&bed=fancybed",
					QueueName);
			log.info(whatdoIget);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		assertEquals("sent and receive the correct object from the server",
				whatdoIget, expected);
	}
*/

	@Test
	public void testRequestCallablesProgrammatic() {

		ReplyThread reply;
		try {
			reply = new ReplyThread(QUEUE_NAME);
			reply.start();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		String whatdoIget = null;
		try {
			whatdoIget = JCP.FetchDataFromJasper(
					"/CoralCEA/HeartRate?patient=Pierre&patient=PierreAgain",
					QUEUE_NAME);
			log.info(whatdoIget);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		assertEquals("Pierre", JsonPath.read(whatdoIget, "$.name"));
		assertEquals("rahme", JsonPath.read(whatdoIget, "$.surname"));
		assertEquals("120/80/60", JsonPath.read(whatdoIget, "$.BPM"));
	}

}
