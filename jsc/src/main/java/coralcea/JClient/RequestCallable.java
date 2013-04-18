package coralcea.JClient;

import java.util.concurrent.Callable;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueRequestor;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.log4j.Logger;

/*
 * Create a QueueRequestor. Create a text message and set its text to the
 * query content of the ThoughtWire incoming messages.
 * Start delivery of incoming messages. Send the text message as the 
 * argument to the request method, which returns the reply message. 
 * 
 * Read the JMSCorrelationID of the reply message and confirm that it matches 
 * the JMSMessageID of the message that was sent. 
 * Finally, close the session.
 */

public class RequestCallable implements Callable<TextMessage> {
	static Logger log = Logger.getLogger(RequestCallable.class.getName());
	QueueConnectionFactory queueConnectionFactory = null;
	QueueConnection queueConnection = null;
	QueueSession queueSession = null;
	Queue queue = null;
	QueueRequestor queueRequestor = null;
	TextMessage message = null;
	TextMessage reply = null;
	String replyID = null;
	String requestUrl;


	/**
	 * Sets up the remoteQueue and creates a queue session to the Jasper engine.
	 * Sets the Query to be sent to the Jasper Server.
	 * 
	 * Called by a JClientProvider on behalf of a subscriber.
	 * 
	 * <p>
	 * If controlQueue doesn't exist, the method throws an exception.
	 * 
	 * @param query
	 *          the body the JMS message to be sent and consumed by JSB.
	 * @param remoteQueueName
	 *          name of the remote queue
	 */

	public RequestCallable(String remoteQueueName, String query) {
		
		requestUrl = query;
		try {
			// queueConnection =
			// queueConnectionFactory.createQueueConnection("user","password");
			// queueConnection =
			// queueConnectionFactory.createQueueConnection("jasper:jsc:1.0:jasperLab","7D39BD7B74C4C74C7A01D746C3AA88BB62CD69F313AAACFFC8EEF421BAB301F36E01F584E19EF1005073372953F571C7A6C950A039F57BD5FAC40BDE1A1AAE0E87779489BB02978438C467C29524C38A79E2F5324E958FF41B3A4E46548E247EBBF03709298D7876FB33B5776B17415B69F93E076FFFD694E16CB40BAA1AED71FC8FF4ABEEC5940BC1570D64E7D9AA20E1574947266A1A77CBAD7D846DB771C4607012893651BCAE43CB4199F5B3452F97740B29762FAE531EEEF3B28B07238D00C4F3613B01155F977EED9CB1A840987CFB1EC4ED8FE041D92BF7BF74FBFDFD4448AADC74684A59722C1D4C8DA6C7CC1D441803811C31EF17F5B3B4A5658EA9");

			log.info("RequestCallable: Started Initialization");
			queueConnection = JClientProvider.getQueueConnection();
			
		  // create a queue session
			queueSession = queueConnection.createQueueSession(false,
					Session.AUTO_ACKNOWLEDGE);

			// The queue the API attempts to connect to should be at the very
			// least validated. Should have that either hard coded in advance or
			// discovered early on. The later need capabilities on the server.
			queue = SampleUtilities.getQueue(remoteQueueName, queueSession);
			
			log.info("RequestCallable: Finished Initialization");

		}
		catch (Exception e) {
			log.error("Connection problem: " 
						+ e.toString());
			
			if (queueConnection != null) {
				try {
					queueConnection.close();
				}
				catch (JMSException ee) {
					log.error("JMSException: Connection problem: " 
							+ ee.toString());
				}
			}
		}

	}

	/**
	 * Send a message to remoteQueue. Called by a subscriber to send a query to
	 * the Jasper Server.
	 * <p>
	 * If controlQueue doesn't exist, the method throws an exception.
	 * 
	 * @param prefix
	 *          prefix (publisher or subscriber) to be displayed
	 * @param controlQueueName
	 *          name of control queue
	 */

	@Override
	public TextMessage call() throws Exception {

		try {
			
		    // create a queue requester
			queueRequestor = new QueueRequestor(queueSession, queue);
			
			// create a text msg on the session
			// the msg represents the query.
			message = queueSession.createTextMessage();
			
			// set the text message inside the JMS message.
			message.setText(requestUrl);
			log.info("RequestCallable: Sending message: "
					+ message.getText());
			
			// block until the broker service the request and sends back a reply
			reply = (TextMessage) queueRequestor.request(message);
			
			// Extract and display the reply message
			log.info("RequestCallable: Reply received: " 
					+ reply.getText());
		
			replyID = new String(reply.getJMSCorrelationID());
			
			// Read the JMSCorrelationID of the reply message 
			// and confirm that it matches the JMSMessageID of 
			// the message that was sent. 
			if (replyID.equals(message.getJMSMessageID())) {
				log.info("RequestCallable: OK: Reply matches sent message "
						+ replyID);
				
				// reply passes our checks, let us consume it.
				return reply;
			}
			else {
				log.error("RequestCallable: ERROR: Reply does not match sent message "
						+ replyID);
			}
		}
		catch (JMSException e) {
			log.error("RequestCallable: Exception occurred: " 
						+ e.toString());
			e.printStackTrace();
		}
		catch (Exception ee) {
			log.error("RequestCallable: Unexpected exception: "
					+ ee.toString());
			ee.printStackTrace();
		}
		finally {
			// clean up nicely
			shutdown();
		}
		return reply;
	}


	public void shutdown() {
		if (queueConnection != null) {
			if (queueSession != null) {

				try {
					queueSession.close();
					// the queueConnection.close() will be called from the JClientProvider once the later is shutdown()
				}
				catch (JMSException e) {
				}
			}
		}
	}

}
