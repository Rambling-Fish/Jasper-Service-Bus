package org.jasper.jsc;

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
	Boolean ExceptionHappened = false;

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
	 *            the body the JMS message to be sent and consumed by JSB.
	 * @param remoteQueueName
	 *            name of the remote queue
	 */

	public RequestCallable(String remoteQueueName, String query) {

		requestUrl = query;
		try {

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

		} catch (Exception e) {
			log.error("Connection problem: " + e.toString());

			if (queueConnection != null) {
				try {
					queueConnection.close();
				} catch (JMSException ee) {
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
			log.info("RequestCallable: Sending message: " + message.getText());

			// block until the broker service the request and sends back a reply
			reply = (TextMessage) queueRequestor.request(message);

			// queueRequestor.request returns null if it times out, so I have to
			// check for it.
			if (reply != null) {

				// Extract and display the reply message
				// log.info("RequestCallable: Reply received: " + reply.getText());

				replyID = new String(reply.getJMSCorrelationID());

				// Read the JMSCorrelationID of the reply message
				// and confirm that it matches the JMSMessageID of
				// the message that was sent.
				if (replyID.equals(message.getJMSMessageID())) {
					log.info("RequestCallable: OK: Reply matches sent message "
							+ replyID);

					// reply passes our checks, let us consume it.
					return reply;
				} else {
					log.error("RequestCallable: ERROR: Reply does not match sent message "
							+ replyID);
				}
			} else {
				log.error("RequestCallable: reply received from Jasper is null");	
			}
		} catch (JMSException e) {
			log.error("RequestCallable: Exception occurred: " + e.toString());
			e.printStackTrace();
		} catch (Exception e) {
			log.error("RequestCallable: Unexpected exception: " + e.toString());
			e.printStackTrace();
		} finally {
			// clean up nicely
			log.info("RequestCallable: shutting down");
			shutdown();
		}
		return reply;
	}

	public void shutdown() {

		try {
			log.info("RequestCallable: shutdown the queueRequestor: ");
			queueRequestor.close();
		} catch (JMSException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

}
