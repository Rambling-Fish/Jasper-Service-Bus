package org.jasper.core.delegate;

import junit.framework.Assert;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerPlugin;
import org.apache.activemq.broker.BrokerService;

import org.jasper.core.constants.JasperConstants;
import org.jasper.core.auth.JasperAuthenticationPlugin;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Command;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Type;

import org.junit.Test;

public class TestDelegate  extends TestCase {

	private static final String TEST_URI = "http://coralcea.com/1.0/testURI";
	private static final String WHITESPACE_URI = "    http://coralcea.com/1.0/testURI   ";
	private static final String TEST_QUEUE = "jms.jta.testJTA.replyToQueue";
	private static final String TEST_JTA_NAME = "TestJTA";
	private static final String EMPTY_JTA_RESPONSE = "{}";
	private Connection connection;
	private DelegateFactory delegateFactory;
	private ActiveMQConnectionFactory connectionFactory;
	private Delegate delegate;
	private Session session;
	private Destination globalQueue;
	private MessageProducer producer;
	private Message message;
	private ExecutorService executorService;
	private Delegate[] delegates = new Delegate[2];
	
	
	/*
	 * This test creates a pool of 2 delegates using the delegate factory
	 */
	@Test
	public void testDelegateFactoryAndPool() throws Exception {
		// Instantiate the delegate pool
		ExecutorService executorService = Executors.newCachedThreadPool();
		delegateFactory = new DelegateFactory();
		Delegate delegateOne = delegateFactory.createDelegate("delegate");
		Delegate delegateTwo = delegateFactory.createDelegate("delegate");
		
		executorService.execute(delegateOne); 
		executorService.execute(delegateTwo); 
		
		Assert.assertNotNull(delegateOne);
		Assert.assertNotNull(delegateTwo);
		Assert.assertNotNull(delegateFactory);
		
		delegateOne.shutdown();
		delegateTwo.shutdown();
		delegateFactory = null;
			
		Thread.sleep(3000);
	}
		
	/*
	 * This test simulates adding/removing URIs to the delegate's internal JTA
	 * hash maps (URI and JTA Name). It also tries to put a null key which
	 * should result in NPE
	 */
	@Test
	public void testDelegateMaps() throws Exception {
		delegateFactory = new DelegateFactory();
		delegate = delegateFactory.createDelegate();
		delegate.getJtaUriMap().clear();

		for(int i = 0; i < 5; i++) {
			List<String> l = new ArrayList<String>();
			l.add(TEST_QUEUE+i);
			delegate.getJtaUriMap().put(TEST_URI+i, l);
			delegate.getJtaQueueMap().put(TEST_JTA_NAME+i, l);
		}
		Assert.assertEquals(delegate.getJtaUriMap().size(), 5);
		Assert.assertEquals(delegate.getJtaQueueMap().size(), 5);
		
		delegate.getJtaUriMap().remove(TEST_URI+3);
		delegate.getJtaQueueMap().remove(TEST_JTA_NAME+0);
		Assert.assertEquals(delegate.getJtaUriMap().size(), 4);
		Assert.assertEquals(delegate.getJtaQueueMap().size(), 4);
		
		// test NPE exception with null map key
		try {
			delegate.getJtaUriMap().put(null, null);
		} catch(Exception ex) {
			Assert.assertNotNull(ex);
		}
		delegateFactory = null;
		delegate = null;
	}
	
	/*
	 * This test simulates adding multiple queues for the same JTA and URI
	 */
	@Test
	public void testJTAMap() throws Exception {
		setUpConnection(1);
		delegates[0].getJtaUriMap().clear();
		delegates[0].getJtaQueueMap().clear();

		JasperAdminMessage jam = new JasperAdminMessage(Type.jtaDataManagement, Command.notify, TEST_QUEUE+"1", TEST_JTA_NAME, TEST_URI);
		JasperAdminMessage jam2 = new JasperAdminMessage(Type.jtaDataManagement, Command.notify, TEST_QUEUE+"2", TEST_JTA_NAME, TEST_URI);
        
		message = session.createObjectMessage(jam);
		producer.send(message);
		Thread.sleep(1000);
		
		message = session.createObjectMessage(jam2);
		producer.send(message);
		Thread.sleep(1000);
		
		Assert.assertEquals(delegates[0].getJtaQueueMap().size(), 1);
		
		tearDown();
		
	}
	
	/*
	 * This test simulates the JasperEngineConnector sending a JTA's URI to the
	 * delegate via a notify admin message.
	 */
	@Test
	public void testPublishURI() throws Exception {
		setUpConnection(1);
		delegates[0].getJtaUriMap().clear();

		JasperAdminMessage jam = new JasperAdminMessage(Type.jtaDataManagement, Command.notify, TEST_QUEUE, JasperConstants.DELEGATE_GLOBAL_QUEUE, TEST_URI);
        
		message = session.createObjectMessage(jam);
		producer.send(message);
		Thread.sleep(1000);
		
		Assert.assertEquals(delegates[0].getJtaUriMap().keySet().isEmpty(), false);
		
		tearDownConnection();

	}
	
	/*
	 * This test checks to see that the core removes all leading and trailing whitespace
	 * in incoming URI before storing in internal map.
	 */
	@Test
	public void testWhitespaceURI() throws Exception {
		setUpConnection(1);
		
		Destination jtaQueue = session.createQueue(TEST_QUEUE);
		delegates[0].getJtaUriMap().clear();

		JasperAdminMessage jam = new JasperAdminMessage(Type.jtaDataManagement, Command.notify, TEST_QUEUE, JasperConstants.DELEGATE_GLOBAL_QUEUE, WHITESPACE_URI);
     
		// send admin message to delegate to store URI. Leading/trailing whitespace should be removed by delegate
		message = session.createObjectMessage(jam);
		producer.send(message);
		Thread.sleep(1000);
		
		//Send message with URI with no leading/trailing whitespace. Should be found by delegate
		Session jtaSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	    MessageConsumer jtaConsumer = jtaSession.createConsumer(jtaQueue);
	    Message jClientRequest;
		
		message = session.createTextMessage(TEST_URI);
		message.setJMSCorrelationID(null);
		message.setJMSReplyTo(jtaQueue);
		
		// Send message to delegate
		producer.send(message);
		
		Thread.sleep(2000);
		
		// simulate jClient timeout
		int maxCount = 10;
	
		do{
          	jClientRequest = jtaConsumer.receive(1000);
          	maxCount--;
          }while(jClientRequest == null && maxCount > 0);

		 if (jClientRequest!= null && jClientRequest instanceof TextMessage) {
			 TextMessage txtMsg = (TextMessage) jClientRequest;
			 Assert.assertNotNull(txtMsg.getText());
		 }

		 // the reply should contain the uri with out spaces
		 if(jClientRequest != null && jClientRequest instanceof ObjectMessage) {
			ObjectMessage objMessage = (ObjectMessage) jClientRequest;
            Object obj = objMessage.getObject();
            String[] uri = (String[])obj;
            
            // URI in response message should have length of 31 which is URI without leading/trailing whitespace
            Assert.assertEquals(31, uri[0].length());		 
		 }
		
		tearDownConnection();

	}
	
	/*
	 * TODO The jClient (JSC) doesn't use correlationID in the request message,
	 * it uses dynamic queues and a messageID, need to change this test to simulate
	 * that behaviour
	 */
	
	/*
	 * This test simulates a message sent from jClient to a delegate which is
	 * then sent to the JTA.  The delegate forwards the request to the JTA 
	 * using the JMS replyToQueue in the incoming request. The JTA then
	 * responds back to the delegate.
	 */
	@Test
	public void testEndToEndMessaging() throws Exception {
		setUpConnection(1);
		
		// Setup so delegate will forward request back here (JTA)
		Destination jtaQueue = session.createQueue(TEST_QUEUE);
		List<String> l = new ArrayList<String>();
		l.add(TEST_QUEUE);
		delegates[0].getJtaUriMap().put(TEST_URI, l);
		
		// Setup consumer to receive message from delegate (i.e. pretend to be a JTA)
		Session jtaSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	    MessageConsumer jtaConsumer = jtaSession.createConsumer(jtaQueue);
	    Message jClientRequest;
	    Message jtaResponse;
		
		message = session.createTextMessage(TEST_URI);
		message.setJMSCorrelationID(null);
		message.setJMSReplyTo(jtaQueue);
		
		// Send message to delegate
		producer.send(message);
		
		Thread.sleep(2000);
		
		// simulate jClient timeout
		int maxCount = 10;
	
		do{
          	jClientRequest = jtaConsumer.receive(1000);
          	maxCount--;
          }while(jClientRequest == null && maxCount > 0);

		 if (jClientRequest!= null && jClientRequest instanceof TextMessage) {
			 TextMessage txtMsg = (TextMessage) jClientRequest;
			 Assert.assertNotNull(txtMsg.getText());
		 }

		 // check that delegate forward message back here
		 if(jClientRequest != null && jClientRequest instanceof ObjectMessage) {
			 ObjectMessage objMsg = (ObjectMessage)jClientRequest;
			 Object obj = objMsg.getObject();
			 Assert.assertNotNull(obj);
		 } 
		
		tearDownConnection();
	}
	
	/*
	 * This test sends an admin message with the publish command to test new
	 * functionality of having delegate request JTA to republish URI for cases
	 * when JSB goes down and comes back up
	 */
	@Test
	public void testNotifyAdminMessage() throws Exception {
		setUpConnection(1);
		delegates[0].getJtaUriMap().clear();

		JasperAdminMessage jam = new JasperAdminMessage(Type.jtaDataManagement, Command.publish, TEST_QUEUE, JasperConstants.DELEGATE_GLOBAL_QUEUE, TEST_URI);
        
		message = session.createObjectMessage(jam);
		producer.send(message);
		Thread.sleep(1000);
		
		tearDownConnection();

	}
	
	/*
	 * This test sends two request to the delegate with the same correlation Id
	 * Exception should be thrown indicating non-unique correlation Id
	 */
	@Test
	public void testDuplicateCorrelationId() throws Exception {
		setUpConnection(1);
		
		// Setup so delegate will forward request back here (JTA)
		Destination jtaQueue = session.createQueue(TEST_QUEUE);
		List<String> l = new ArrayList<String>();
		l.add(TEST_QUEUE);
		delegates[0].getJtaUriMap().put(TEST_URI, l);
		
		message = session.createTextMessage(TEST_URI);
		String corrId = "1234";
		message.setJMSCorrelationID(corrId);
		message.setJMSReplyTo(jtaQueue);
		
		// Send duplicate messages to delegate with same correlationIDs
		try {
			producer.send(message);
			Thread.sleep(1000);
			producer.send(message);
		} catch(Exception ex) {
			Assert.assertNotNull(ex);
		}		
		
	}
	
	/*
	 * This test sends a message with an invalid URI from the client to a 
	 * delegate. The delegate should reject the message since it will not find
	 * the URI in the internal hash map
	 */
	@Test
	public void testMissingURI() throws Exception {
		setUpConnection(1);
		
		// Setup so delegate will forward request back here (JTA)
		Destination jtaQueue = session.createQueue(TEST_QUEUE);

		message = session.createTextMessage(TEST_URI);
		String corrId = "1234";
		message.setJMSCorrelationID(corrId);
		message.setJMSReplyTo(jtaQueue);
		
		producer.send(message);
		Thread.sleep(1000);
		
		tearDownConnection();
	
	}
	
	@Test
	public void testJasperBrokerPlugin() throws Exception {
        BrokerService service = new BrokerService();
        service.setPlugins(new BrokerPlugin[]{new JasperAuthenticationPlugin()});
        assertEquals( 1024 * 1024 * 64, service.getSystemUsage().getMemoryUsage().getLimit() );
        assertEquals( 1024L * 1024 * 1024 * 50, service.getSystemUsage().getTempUsage().getLimit() );
        assertEquals( 1024L * 1024 * 1024 * 100, service.getSystemUsage().getStoreUsage().getLimit() );

    }
	
	/*
	 * This test simulates the Broker sending an admin message to the delegate
	 * once a connection to JTA islost. This should remove the JTA's URI.  
	 */
	@Test
	public void testRemoveURI() throws Exception {
		setUpConnection(1);
		
		// manually add uri to internal hashmap
		List<String> l = new ArrayList<String>();
		l.add(TEST_QUEUE);
		delegates[0].getJtaUriMap().put(TEST_URI, l);
		delegates[0].getJtaQueueMap().put(TEST_JTA_NAME, l);

		JasperAdminMessage jam = new JasperAdminMessage(Type.jtaDataManagement, Command.delete, TEST_JTA_NAME, JasperConstants.DELEGATE_GLOBAL_QUEUE, TEST_URI);
        
		message = session.createObjectMessage(jam);
		producer.send(message);
		Thread.sleep(1000);
		
		tearDownConnection();

	}	
	
	private void setUpConnection(int numDelegates) throws Exception {
		 connectionFactory = new ActiveMQConnectionFactory("vm://localhost");
		 delegateFactory = new DelegateFactory();
		 delegate = delegateFactory.createDelegate();
		 delegate.getJtaUriMap().clear();

        // Create a Connection
        connectionFactory.setUserName(JasperConstants.JASPER_ADMIN_USERNAME);
        connectionFactory.setPassword(JasperConstants.JASPER_ADMIN_PASSWORD);
        connection = connectionFactory.createConnection();
        connection.start();
		
		
		executorService = Executors.newCachedThreadPool();
		delegates = new Delegate[numDelegates];
		
		for(int i=0;i<delegates.length;i++){
			delegates[i] = delegateFactory.createDelegate();
			executorService.execute(delegates[i]);
		}
       
		session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		globalQueue = session.createQueue(JasperConstants.DELEGATE_GLOBAL_QUEUE);
		producer = session.createProducer(globalQueue);
		producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
		producer.setTimeToLive(30000);
	}
	
	private void tearDownConnection() throws Exception {
		// Clean up - no need to close connection as that will be done in JasperBroker
		session.close();
		connection.close();
		for(int i = 0; i< delegates.length; i++) {
			delegates[i].shutdown();
		}
		
		session           = null;
		connection        = null;
		producer          = null;
		globalQueue       = null;
		delegates         = null;
		executorService   = null;
		connectionFactory = null;
		delegateFactory   = null;
		
		Thread.sleep(2000);
		
	}
	
}