/**
 * 
 */
package coralcea.JClient.test;

import static org.junit.Assert.assertEquals;

import java.io.UnsupportedEncodingException;

import org.apache.activemq.broker.BrokerService;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import coralcea.JClient.JClientProvider;
import coralcea.JClient.ReplyThread;

import org.codehaus.jettison.json.JSONObject;



/**
 * @author pierrerahme
 * 
 */
public class InjectSyncRequest {
	static Logger log = Logger.getLogger(InjectSyncRequest.class.getName());
	static final int threadCount = 2;
	private final String QueueName = "JClientQueue";
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
		 * for (int i = 0; i <= threadCount+1; i++) {
			ReplyThread reply = new ReplyThread(QueueName);
			reply.start();
		}

		Thread monitor = new Thread(new MonitorExecutor(
				(ThreadPoolExecutor) JClientProvider.execute));
		monitor.setDaemon(true);
		monitor.start();
*/
	}
	private static void startBroker() {
		broker = new BrokerService();
		broker.setUseJmx(true);
		broker.setBrokerName("Broker_for_stubbing_activemq");
		try {
			broker.addConnector("tcp://0.0.0.0:61616");
			broker.start();
		}
		catch (Exception e) {
			System.out.println("in startBroker - exception is: " + e);
			e.printStackTrace();
		}
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
//		JClientProvider.shutdown();
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
				reply = new ReplyThread(QueueName);
				reply.start();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
					
		String whatdoIget = null;
		try {
			whatdoIget = JCP.FetchDataFromJasper("/CoralCEA/HeartRate?patient=Pierre&patient=PierreAgain", QueueName);
			log.info(whatdoIget);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
				reply = new ReplyThread(QueueName);
				reply.start();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		for (int i = 0; i <= threadCount; i++) {
			String whatdoIget;
			try {
				whatdoIget = JCP.FetchDataFromJasper("/CoralCEA/HeartRate?patient=zimmerman&bed=fancybed",QueueName);
				log.info(whatdoIget);
			}
			catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			assertEquals("sent and receive the correct object from the server",
					"HR data", "HR data");
		}

	}
	
	@Test
	public final void testTimeOutCallables() {

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
				whatdoIget = JCP.FetchDataFromJasper("/CoralCEA/HeartRate?patient=zimmerman&bed=fancybed","badQueueName");
				log.info(whatdoIget);
			}
			catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			assertEquals("sent and receive the correct object from the server",
					whatdoIget, expected);
		}

	

}
