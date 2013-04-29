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

	public static final JClientProvider INSTANCE = new JClientProvider();
	public static final ExecutorService execute = Executors
			.newCachedThreadPool();
	private static QueueConnectionFactory queueConnectionFactory;
	private static QueueConnection queueConnection = null;
	RequestCallable RC;

	/**
	 * 
	 * The entry point into the api interface which all users must call first
	 * prior to using any other methods on this class.
	 * <p>
	 * A static method that returns an instance of the class. Internally, it
	 * sets up the remoteQueue and creates the main connection to the Jasper
	 * engine
	 * 
	 */
	public synchronized static JClientProvider getInstance() {
		try {
			if (queueConnectionFactory == null) {
				// create a queue connection
				queueConnectionFactory = SampleUtilities
						.getQueueConnectionFactory();
			}

			if (queueConnection == null) {
				String user = PropertiesUtil.getProperty("jclient.username");
				String password = PropertiesUtil
						.getProperty("jclient.password");

				queueConnection = queueConnectionFactory.createQueueConnection(
						user, password);

				queueConnection.start();
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return INSTANCE;
	}

	
	/**
	 * 
	 * The entry point into the api interface which all users must call first
	 * prior to using any other methods on this class.
	 * <p>
	 * A static method that returns an instance of the class. Internally, it
	 * sets up the remoteQueue and creates the main connection to the Jasper
	 * engine
	 * 
	 */
	public synchronized static JClientProvider getTestInstance() {
		try {
			if (queueConnectionFactory == null) {
				// create a queue connection
				queueConnectionFactory = SampleUtilities
						.getQueueConnectionFactory();
			}

			if (queueConnection == null) {
				
				queueConnection = queueConnectionFactory.createQueueConnection("user","password");

				queueConnection.start();
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return INSTANCE;
	}
	
	

	/**
	 * 
	 * Tears down the connection to the Jasper Server and releases all local
	 * resources.
	 * 
	 */

	public static void shutdown() {

		log.info("Shutting down Executor."); 

		execute.shutdown();
		try {
			if (!execute.awaitTermination(100, TimeUnit.MICROSECONDS)) {
	            log.error("Executor did not terminate in the specified time."); 
	            List<Runnable> droppedTasks = execute.shutdownNow(); 
	            log.error("Executor is now being abruptly shut down. " + droppedTasks.size() + " tasks will not be executed."); 
			}
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		log.debug("Exiting normally...");

		log.info("Shutting down JCLientProvider."); 
		if (queueConnection != null) {
			try {
				log.info("Shutting down QueueConnection."); 

				queueConnection.stop();
				queueConnection.close();
			} catch (JMSException e) {
			}
		}
	}

	/**
	 * Sets the Query to be sent to the Jasper Server and sends it while
	 * blocking until it gets a reply.
	 * <p>
	 * Internally, it handles session management to the Jasper Server.
	 * <p>
	 * If the QueryUri doesn't fit the correct format of a URI param, the method
	 * throws an exception.
	 * 
	 * @param string
	 *            QueryUri a String representing the ask of the client to the
	 *            server. The convention is to use a URI form.
	 * @return string a String encoded in Json format if the query finds the
	 *         data to handle the response. Otherwise, a String representing an
	 *         empty Json is returned.
	 */

	// This method need to validate that the queryUrl is a well formed URI
	public String FetchDataFromJasper(String QueryUri, String...queueName)
			throws UnsupportedEncodingException {
		boolean isQueryValid = false;
		boolean exceptionHappened = false;
		try {
			isQueryValid = SampleUtilities.validateQueryString(QueryUri);
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}

		if (!isQueryValid) {
			return JSONObject.quote("{ }");
		}

		RequestCallable RC;
		if (queueName != null)
		{
			System.out.println("queueName is : " + queueName);
			System.out.println("queueName[0] is : " + queueName[0]);

			RC = new RequestCallable(queueName[0],
					QueryUri);
		} else {
			RC = new RequestCallable(DELEGATE_GLOBAL_QUEUE,
					QueryUri);
			System.out.println("queueName is : " + DELEGATE_GLOBAL_QUEUE);
		}

		Future<TextMessage> future = execute.submit(RC);

		String text = JSONObject.quote("{ }");

		try {
			TextMessage bigJMSmessage = future.get(20, TimeUnit.SECONDS);

			text = bigJMSmessage.getText();

			log.info("Expecting a JSON String  :  " + text);
		} catch (TimeoutException e) {
			log.error("Timed out waiting for server check thread."
					+ "We'll try to interrupt it."
					+ "Sending  back an empty JSON");

			// return empty JSON body;
			log.error(e.getCause() + " is what caused the exception");
			exceptionHappened = true;
			// close the session.
			RC.shutdown();
			
			return JSONObject.quote("{ }");
		} catch (InterruptedException e) {
			exceptionHappened = true;
			
			log.error(e.getCause() + " is what caused the exception");
			e.printStackTrace();
		} catch (ExecutionException e) {
			exceptionHappened = true;
			
			log.error(e.getCause() + " is what caused the exception");
			e.printStackTrace();
		} catch (JMSException e) {
			exceptionHappened = true;

			log.error(e.getCause() + " is what caused the exception");
			e.printStackTrace();
		} finally {
			
			if (exceptionHappened){
				// close the session.
				RC.shutdown();
				// return empty JSON body;
				return JSONObject.quote("{ }");
			}
			
	    }

		return text;
	}

	public synchronized static QueueConnection getQueueConnection() {

		try {
			if (queueConnectionFactory == null) {
				// create a queue connection
				queueConnectionFactory = SampleUtilities
						.getQueueConnectionFactory();
			}

			if (queueConnection == null) {
				queueConnection = queueConnectionFactory.createQueueConnection(
						"user", "password");
				queueConnection.start();
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return queueConnection;

	}

}
