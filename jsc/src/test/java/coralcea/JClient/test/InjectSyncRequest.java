/**
 * 
 */
package coralcea.JClient.test;

import coralcea.JClient.*;
import static org.junit.Assert.*;
import java.awt.Desktop;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ThreadPoolExecutor;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author pierrerahme
 * 
 */
public class InjectSyncRequest {
	static  Logger log              = Logger.getLogger(InjectSyncRequest.class.getName());
	static  final  int threadCount  = 10;
	private final  static  String QueueName = "JClientQueue";
	private final  static  String brokerURL = "tcp://localhost:61616";

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		JClientProvider.startBroker(brokerURL);
		JClientProvider.getInstance();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		JClientProvider.shutdown();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		for (int i = 0; i <= threadCount; i++) {
			ReplyThread reply = new ReplyThread(QueueName);
			reply.start();
		}
		Thread monitor = new Thread(new MonitorExecutor(
				(ThreadPoolExecutor) JClientProvider.execute));
		monitor.setDaemon(true);
		monitor.start();

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
		JClientProvider JCP = JClientProvider.getInstance();

		String whatdoIget;
		try {
			whatdoIget = JCP.FetchDataFromJasper("/CoralCEA/HeartRate?patient=Pierre&patient=PierreAgain");
			log.info(whatdoIget);
		}
		catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	//	assertEquals("sent and receive the correct object from the server",
	//			"HR data", whatdoIget);

		String url = "http://localhost:8161/admin/queues.jsp";
		URI myUri = URI.create(url);
		try {
			Desktop.getDesktop().browse(myUri);
		}
		catch (IOException e) {
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
		JClientProvider JCP = JClientProvider.getInstance();

		for (int i = 1; i <= threadCount; i++) {
			String whatdoIget;
			try {
				whatdoIget = JCP.FetchDataFromJasper("/CoralCEA/HeartRate?patient=zimmerman&bed=fancybed");
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

}
