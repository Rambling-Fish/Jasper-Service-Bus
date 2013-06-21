package org.jasper.jsc;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import org.apache.log4j.Logger;



public class ReplyThread extends Thread {
	static Logger log                             = Logger.getLogger(RequestCallable.class.getName());
	QueueConnectionFactory queueConnectionFactory = null;
	QueueConnection        queueConnection        = null;
	QueueSession           queueSession           = null;
	Queue                  queue                  = null;

	public ReplyThread(String queueName) throws Exception {
		try {
			log.debug("ReplyThread: Started initialization");
			
			queueConnectionFactory = SampleUtilities.getQueueConnectionFactory();

			queueConnection = queueConnectionFactory.createQueueConnection("user","password");
			
			queueConnection = queueConnectionFactory.createQueueConnection();
			
			queueSession = queueConnection.createQueueSession(false,
					Session.AUTO_ACKNOWLEDGE);
			
			queue = SampleUtilities.getQueue(queueName, queueSession);
			
			queueConnection.start();
			
			log.debug("ReplyThread: Finished initialization");

		}
		catch (Exception e) {
			System.out.println("Connection problem: " 
					+ e.toString());
			
			if (queueConnection != null) {
				try {
					if (queueSession != null)
						queueSession.close();
					queueConnection.close();
				}
				catch (JMSException ee) {
				}
			}
		}

	}

	/**
	 * Runs the thread.
	 */
	public void run() {
		QueueReceiver queueReceiver = null;
		TextMessage   message       = null;
		Queue         tempQueue     = null;
		QueueSender   replySender   = null;
		TextMessage   reply         = null;
		
		
    String json = "{\n" +
   "    \"name\": \"Pierre\",\n" +
   "    \"surname\": \"rahme\",\n" +
   "    \"BPM\": \"120/80/60\" }";

		
		/*
		 * Start delivery of incoming messages. 
		 * Call receive, which blocks until it obtains a message. Display the message
		 * obtained. Extract the temporary reply queue from the JMSReplyTo field of
		 * the message header. Use the temporary queue to create a sender for the
		 * reply message. Send the reply message.
		 * Finally, close the connection.
		 */
		try {
			log.debug("ReplyThread: In the Run method()");
	    
			// Create a QueueReceiver
			queueReceiver = queueSession.createReceiver(queue);
			
			// Start delivery of incoming messages. Call
			// receive, which blocks until it obtains a message.
			message = (TextMessage) queueReceiver.receive();
			
			log.debug("ReplyThread: Message received: " 
					+ message.getText());
			
			// Extract the temporary reply queue from the JMSReplyTo field of
			// the message header
			tempQueue = (Queue) message.getJMSReplyTo();
			
			log.debug("ReplyThread: tempQueue is: " 
					+ tempQueue.getQueueName());
			
			replySender = queueSession.createSender(tempQueue);
			
			reply = queueSession.createTextMessage();

			reply.setText(json);

			reply.setJMSCorrelationID(message.getJMSMessageID());
			
			log.debug("ReplyThread: Sending reply: " 
					+ reply.getText());
			
			// Send the reply message
			replySender.send(reply);
		}
		catch (JMSException e) {
			System.out.println("ReplyThread: Exception occurred: " + e.toString());
		}
		catch (Exception ee) {
			System.out.println("ReplyThread: Unexpected exception: " + ee.toString());
			ee.printStackTrace();
		}
		finally {
			if (queueSession != null) {
				try {
					// finally close the session.
					queueSession.close();
				}
				catch (JMSException e) {
				}
			}
			if (queueConnection != null) {
				try {
					// This is OK as it run the test on the client creating the connection.
					queueConnection.close();
				}
				catch (JMSException e) {
				}

			}
		}
	}
}
