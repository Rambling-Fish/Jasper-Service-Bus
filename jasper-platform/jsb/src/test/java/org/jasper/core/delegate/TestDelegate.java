package org.jasper.core.delegate;

import static org.junit.Assert.assertEquals;

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
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import junit.framework.Assert;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerPlugin;
import org.apache.activemq.broker.BrokerService;
import org.jasper.core.auth.JasperAuthenticationPlugin;
import org.jasper.core.constants.JasperConstants;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Command;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Type;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestDelegate {

	private static final String TEST_URI = "http://coralcea.com/1.0/testURI";
	private static final String WHITESPACE_URI = "    http://coralcea.com/1.0/testURI   ";
	private static final String TEST_QUEUE = "jms.jta.testJTA.replyToQueue";
	private static final String TEST_JTA_NAME = "TestJTA";

	private Connection connection;
	private Session session;
	private MessageProducer producer;
	private DelegateFactory delegateFactory;
	private Delegate delegate;
	private ExecutorService delegateService;
    private List<Delegate> delegates;

	@Before
	public void setUp()throws Exception{
		delegateService = Executors.newCachedThreadPool();
		delegateFactory = new DelegateFactory();
		delegates = new ArrayList<Delegate>();
		delegate = delegateFactory.createDelegate();
		delegates.add(delegate);
		for(Delegate d:delegates) delegateService.execute(d);
		
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost");
        // Create a Connection
        connectionFactory.setUserName(JasperConstants.JASPER_ADMIN_USERNAME);
        connectionFactory.setPassword(JasperConstants.JASPER_ADMIN_PASSWORD);
		
		connection = connectionFactory.createConnection();
		connection.start();
		session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		Queue globalQueue = session.createQueue(JasperConstants.DELEGATE_GLOBAL_QUEUE);
		producer = session.createProducer(globalQueue);
		producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
		producer.setTimeToLive(30000);
	}
	
	@After
	public void tearDown()throws Exception{
    	for(Delegate d:delegates) d.shutdown();
		delegateService.shutdown();
		delegateFactory.shutdown();
		producer.close();
		session.close();
		connection.close();
	}
		
	/*
	 * This test simulates adding/removing URIs to the delegate's internal JTA
	 * hash maps (URI and JTA Name). It also tries to put a null key which
	 * should result in NPE
	 */
	@Test
	public void testDelegateMaps() throws Exception {
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

	}
	
	/*
	 * This test simulates adding multiple queues for the same JTA and URI
	 */
	@Test
	public void testJTAMap() throws Exception {
		delegates.get(0).getJtaUriMap().clear();
		delegates.get(0).getJtaQueueMap().clear();

		JasperAdminMessage jam = new JasperAdminMessage(Type.jtaDataManagement, Command.notify, TEST_QUEUE+"1", TEST_JTA_NAME, TEST_URI);
		JasperAdminMessage jam2 = new JasperAdminMessage(Type.jtaDataManagement, Command.notify, TEST_QUEUE+"2", TEST_JTA_NAME, TEST_URI);
        
		Message message = session.createObjectMessage(jam);
		producer.send(message);
		Thread.sleep(1000);
		
		message = session.createObjectMessage(jam2);
		producer.send(message);
		Thread.sleep(1000);
		
		Assert.assertEquals(delegates.get(0).getJtaQueueMap().size(), 1);		
	}
	
	/*
	 * This test simulates the JasperEngineConnector sending a JTA's URI to the
	 * delegate via a notify admin message.
	 */
	@Test
	public void testPublishURI() throws Exception {
		delegates.get(0).getJtaUriMap().clear();

		JasperAdminMessage jam = new JasperAdminMessage(Type.jtaDataManagement, Command.notify, TEST_QUEUE, JasperConstants.DELEGATE_GLOBAL_QUEUE, TEST_URI);
        
		Message message = session.createObjectMessage(jam);
		producer.send(message);
		Thread.sleep(1000);
		
		Assert.assertEquals(delegates.get(0).getJtaUriMap().keySet().isEmpty(), false);
		
	}
	
	/*
	 * This test checks to see that the core removes all leading and trailing whitespace
	 * in incoming URI before storing in internal map.
	 */
	@Test
	public void testWhitespaceURI() throws Exception {
		
		Destination jtaQueue = session.createQueue(TEST_QUEUE);
		delegates.get(0).getJtaUriMap().clear();

		JasperAdminMessage jam = new JasperAdminMessage(Type.jtaDataManagement, Command.notify, TEST_QUEUE, JasperConstants.DELEGATE_GLOBAL_QUEUE, WHITESPACE_URI);
     
		// send admin message to delegate to store URI. Leading/trailing whitespace should be removed by delegate
		Message message = session.createObjectMessage(jam);
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
		
		// Setup so delegate will forward request back here (JTA)
		Destination jtaQueue = session.createQueue(TEST_QUEUE);
		List<String> l = new ArrayList<String>();
		l.add(TEST_QUEUE);
		delegates.get(0).getJtaUriMap().put(TEST_URI, l);
		
		// Setup consumer to receive message from delegate (i.e. pretend to be a JTA)
		Session jtaSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	    MessageConsumer jtaConsumer = jtaSession.createConsumer(jtaQueue);
	    Message jClientRequest;
	    Message jtaResponse;
		
	    Message message = session.createTextMessage(TEST_URI);
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
		
	}
	
	/*
	 * This test sends an admin message with the publish command to test new
	 * functionality of having delegate request JTA to republish URI for cases
	 * when JSB goes down and comes back up
	 */
	@Test
	public void testNotifyAdminMessage() throws Exception {
		delegates.get(0).getJtaUriMap().clear();

		JasperAdminMessage jam = new JasperAdminMessage(Type.jtaDataManagement, Command.publish, TEST_QUEUE, JasperConstants.DELEGATE_GLOBAL_QUEUE, TEST_URI);
        
		Message message = session.createObjectMessage(jam);
		producer.send(message);
	}
	
	/*
	 * This test sends two request to the delegate with the same correlation Id
	 * Exception should be thrown indicating non-unique correlation Id
	 */
	@Test
	public void testDuplicateCorrelationId() throws Exception {
		
		// Setup so delegate will forward request back here (JTA)
		Destination jtaQueue = session.createQueue(TEST_QUEUE);
		List<String> l = new ArrayList<String>();
		l.add(TEST_QUEUE);
		delegates.get(0).getJtaUriMap().put(TEST_URI, l);
		
		Message message = session.createTextMessage(TEST_URI);
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
		
		// Setup so delegate will forward request back here (JTA)
		Destination jtaQueue = session.createQueue(TEST_QUEUE);

		Message message = session.createTextMessage(TEST_URI);
		String corrId = "1234";
		message.setJMSCorrelationID(corrId);
		message.setJMSReplyTo(jtaQueue);
		
		producer.send(message);			
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
		
		// manually add uri to internal hashmap
		List<String> l = new ArrayList<String>();
		l.add(TEST_QUEUE);
		delegates.get(0).getJtaUriMap().put(TEST_URI, l);
		delegates.get(0).getJtaQueueMap().put(TEST_JTA_NAME, l);

		JasperAdminMessage jam = new JasperAdminMessage(Type.jtaDataManagement, Command.delete, TEST_JTA_NAME, JasperConstants.DELEGATE_GLOBAL_QUEUE, TEST_URI);
        
		Message message = session.createObjectMessage(jam);
		producer.send(message);		

	}	
}