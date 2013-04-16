package coralcea.JClient;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.TextMessage;
import org.codehaus.jettison.json.JSONObject;
import org.apache.activemq.broker.BrokerService;
import org.apache.log4j.Logger;

public class JClientProvider {

	static Logger log = Logger.getLogger(JClientProvider.class.getName());

	private static final String DELEGATE_GLOBAL_QUEUE = "jms.jasper.delegate.global.queue";
	
	public  static final JClientProvider INSTANCE = new JClientProvider();
	public  static final ExecutorService execute = Executors.newCachedThreadPool();
	private static QueueConnectionFactory queueConnectionFactory = null;
	private static QueueConnection queueConnection = null;
	RequestCallable RC;
	private static BrokerService broker;

	public synchronized static JClientProvider getInstance() {
		/*
		 * try { startBroker(); } catch (Exception e) { // TODO Auto-generated catch
		 * block e.printStackTrace(); }
		 */
		try {
			if (queueConnectionFactory == null) {
			// create a queue connection
				queueConnectionFactory = SampleUtilities.getQueueConnectionFactory();
			}

			if (queueConnection == null) {
//				queueConnection = queueConnectionFactory.createQueueConnection("user",
//						"password");
				String user = PropertiesUtil.getProperty("jclient.username");
				String password = PropertiesUtil.getProperty("jclient.password");


				queueConnection = queueConnectionFactory.createQueueConnection(user,password);

				queueConnection.start();
			}

		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return INSTANCE;
	}

	public static void startBroker(String brokerURL) {
		broker = new BrokerService();
		broker.setUseJmx(true);
		broker.setBrokerName("Broker_for_stubbing_activemq");
		try {
			broker.addConnector(brokerURL);
			broker.start();
		}
		catch (Exception e) {
			System.out.println("in startBroker - exception is: " + e);
			e.printStackTrace();
		}

	}

	public static void shutdown() {
		
		if (queueConnection != null) {
			try {
				queueConnection.stop();
				queueConnection.close();
			}
			catch (JMSException e) {
			}
		}

		execute.shutdown();
		
		for (int i = 0; i < 10; i++) {
			if (execute.isShutdown())
				break;
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (!execute.isShutdown())
			execute.shutdownNow();

	}

	// This method need to validate that the queryUrl is a well formed URI
	public String FetchDataFromJasper(String QueryUri)
			throws UnsupportedEncodingException {
    boolean isQueryValid = false;
    try {
    	isQueryValid = SampleUtilities.validateQueryString(QueryUri);
		}
		catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
    
    if (!isQueryValid)
    {
    	return JSONObject.quote("{ }");
    }
    
		RequestCallable RC = new RequestCallable(DELEGATE_GLOBAL_QUEUE, QueryUri);

		Future<TextMessage> future = execute.submit(RC);
		
		String text = JSONObject.quote("{ }");
		
		try {
			TextMessage bigJMSmessage = future.get(20, TimeUnit.SECONDS);
			
			text = bigJMSmessage.getText();
			
			log.info("Excepting a JSON String  :  " + text);
		}
		catch (TimeoutException e) {
			log.error("Timed out waiting for server check thread."
					+ "We'll try to interrupt it."
					+ "Sending  back an empty JSON");
			
			future.cancel(true);
			
			// return empty JSON body;
			log.error(JSONObject.quote("{ }"));
			
			// close the session.
			RC.shutdown();
			return JSONObject.quote("{ }");
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
		catch (ExecutionException e) {
			e.printStackTrace();
		}
		catch (JMSException e) {
			e.printStackTrace();
		}
		
		return text;
	}

	// will I need to synchronize this code given it's running ExecutorService as
	// this wraps the Queues and Thread pools in one class
	public synchronized static QueueConnection getQueueConnection() {

		try {
			if (queueConnectionFactory == null) {
			// create a queue connection
				queueConnectionFactory = SampleUtilities.getQueueConnectionFactory();
			}

			if (queueConnection == null) {
				queueConnection = queueConnectionFactory.createQueueConnection("user",
						"password");
				queueConnection.start();
			}

		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return queueConnection;

	}

}
