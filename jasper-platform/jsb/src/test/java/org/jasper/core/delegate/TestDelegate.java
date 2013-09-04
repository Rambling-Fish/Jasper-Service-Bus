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
import org.jasper.core.JECore;
import org.jasper.core.auth.JasperAuthenticationPlugin;
import org.jasper.core.constants.JasperConstants;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Command;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Type;
//
import org.junit.Test;
//
public class TestDelegate  extends TestCase {
//
	private static final String TEST_URI = "http://coralcea.com/1.0/testURI";
	private static final String WHITESPACE_URI = "    http://coralcea.com/1.0/testURI   ";
	private static final String ADMIN_QUEUE = "jms.TestJTA.admin.queue";
	private static final String TEST_JTA_NAME = "TestJTA";
	private static final String EMPTY_JTA_RESPONSE = "{}";
	private static final String SPARQL_QUERY = "?query=PREFIX%20:%20%3Chttp://coralcea.ca/jasper/vocabulary/%3E%20PREFIX%20jta:%20%3Chttp://coralcea.ca/jasper/vocabulary/jta/%3E%20PREFIX%20jasper:%20%3Chttp://coralcea.ca/jasper/%3E%20SELECT%20?jta%20?jtaProvidedData%20?params%20WHERE%20{%20{%20?jta%20:is%20:jta%20.%20?jta%20:provides%20?jtaProvidedData%20.%20}%20UNION%20{%20?jta%20:is%20:jta%20.%20?jta%20:param%20?params%20.%20}%20}&output=json";
	private Connection connection;
	private DelegateFactory delegateFactory;
	private ActiveMQConnectionFactory connectionFactory;
	private Session session;
	private Destination globalQueue;
	private MessageProducer producer;
	private Message message;
	private ExecutorService executorService;
	private Delegate[] delegates = new Delegate[2];
	
	/*
	 * This test creates a pool of 3 delegates using the delegate factory
	 */
//	@Test
//	public void testDelegateFactoryAndPool() throws Exception {
//		// Instantiate the delegate pool
//		ExecutorService executorService = Executors.newCachedThreadPool();
//		delegateFactory = new DelegateFactory(false, null);
//		Delegate[] delegates = new Delegate[3];
//		
//		for(int i=0;i<delegates.length;i++) {
//			delegates[i] = delegateFactory.createDelegate();
//			executorService.execute(delegates[i]);
//		} 
//		
//			Assert.assertEquals(delegates.length, 3);
//			Assert.assertNotNull(delegateFactory);
//			for(int i=0;i<delegates.length;i++) {
//				delegates[i].shutdown();
//			}
//			
//			Thread.sleep(3000);
//	}
	
	/*
	 * This test simulates adding/removing URIs to the delegate's internal JTA
	 * hash maps (URI and JTA Name). It also tries to put a null key which
	 * should result in NPE
	 */
//	@Test
//	public void testDelegateMaps() throws Exception {
//		delegateFactory = new DelegateFactory(false, null);
//		delegateFactory.jtaUriMap.clear();
//		for(int i = 0; i < 5; i++) {
//			List<String> l = new ArrayList<String>();
//			l.add(TEST_QUEUE+i);
//			delegateFactory.jtaUriMap.put(TEST_URI+i, l);
//			delegateFactory.jtaQueueMap.put(TEST_JTA_NAME+i, l);
//		}
//		Assert.assertEquals(delegateFactory.jtaUriMap.size(), 5);
//		Assert.assertEquals(delegateFactory.jtaQueueMap.size(), 5);
//		
//		delegateFactory.jtaUriMap.remove(TEST_URI+3);
//		delegateFactory.jtaQueueMap.remove(TEST_JTA_NAME+0);
//		Assert.assertEquals(delegateFactory.jtaUriMap.size(), 4);
//		Assert.assertEquals(delegateFactory.jtaQueueMap.size(), 4);
//		
//		// test NPE exception with null map key
//		try {
//			delegateFactory.jtaUriMap.put(null, null);
//		} catch(Exception ex) {
//			Assert.assertNotNull(ex);
//		}
//	}

	/*
	 * This test simulates adding multiple queues for the same JTA and URI
	 */
//	@Test
//	public void testJTAMap() throws Exception {
//		setUpConnection(2);
//		DelegateFactory factory = new DelegateFactory(false, null);
//
//		JasperAdminMessage jam = new JasperAdminMessage(Type.jtaDataManagement, Command.notify, TEST_QUEUE+"1", TEST_JTA_NAME, TEST_URI);
//		JasperAdminMessage jam2 = new JasperAdminMessage(Type.jtaDataManagement, Command.notify, TEST_QUEUE+"2", TEST_JTA_NAME, TEST_URI);
//        
//		message = session.createObjectMessage(jam);
//		producer.send(message);
//		Thread.sleep(1000);
//		
//		message = session.createObjectMessage(jam2);
//		producer.send(message);
//		Thread.sleep(1000);
//		
//		Assert.assertEquals(delegateFactory.jtaQueueMap.size(), 1);
//		
//	}
	
	/*
	 * This test simulates the broker sending a connect message to the delegate.
	 * The admin handler then sends a JAM message to tell the JTA to publish it's
	 * ontology. This test case receives the get_ontology JAM message and creates
	 * an object message composed of triples and sends it back to the delegate.
	 */
	@Test
	public void testJTAConnect() throws Exception {
		setUpConnection(2);

		JasperAdminMessage jam = new JasperAdminMessage(Type.ontologyManagement, Command.jta_connect, TEST_JTA_NAME);
        
		message = session.createObjectMessage(jam);
		Destination adminQueue = session.createQueue(ADMIN_QUEUE);
		MessageConsumer adminConsumer = session.createConsumer(adminQueue);
		MessageProducer adminProducer = session.createProducer(null);
		producer.send(message);
		
		// Wait for a message
	    Message adminRequest;
	    
	    do{
    		adminRequest = adminConsumer.receive(3000);
    	}while(adminRequest == null);
	    
	    if (adminRequest instanceof ObjectMessage) {
        	ObjectMessage objMessage = (ObjectMessage) adminRequest;
        	Object obj = objMessage.getObject();
        	if(obj instanceof JasperAdminMessage){
				if(((JasperAdminMessage) obj).getType() == Type.ontologyManagement && ((JasperAdminMessage) obj).getCommand() == Command.get_ontology){
        			String[][] triples = loadOntology();
        			Message response = session.createObjectMessage(triples);
        			response.setJMSCorrelationID(adminRequest.getJMSCorrelationID());
					adminProducer.send(adminRequest.getJMSReplyTo(), response );
				}
        	}
	    }
		tearDownConnection();

	}
	
	/*
	 * This test simulates a valid sparql query coming in from the JSC.
	 */
	@Test
	public void testSparqlHandlerSuccess() throws Exception {
		setUpConnection(2);
		
		message = session.createTextMessage(SPARQL_QUERY);
		producer.send(message);
		
		Thread.sleep(2000);
		tearDown();
	}
	
	/*
	 * This test simulates an invalid sparql query coming in from the JSC.
	 */
	@Test
	public void testSparqlHandlerFail() throws Exception {
		setUpConnection(2);
		
		message = session.createTextMessage("?query=not a valid query");
		producer.send(message);
		
		Thread.sleep(2000);
		tearDown();
	}
	
	/*
	 * This test simulates the broker sending a disconnect message to the delegate.
	 * The admin handler then removes the statements of this JTA from the model.
	 */
	@Test
	public void testJTADisconnect() throws Exception {
		setUpConnection(2);
		
		JasperAdminMessage jam = new JasperAdminMessage(Type.ontologyManagement, Command.jta_disconnect, TEST_JTA_NAME);
		message = session.createObjectMessage(jam);
		producer.send(message);
		
		Thread.sleep(2000);
		tearDown();
	}
	
//	
//	/*
//	 * This test checks to see that the core removes all leading and trailing whitespace
//	 * in incoming URI before storing in internal map.
//	 */
//	@Test
//	public void testWhitespaceURI() throws Exception {
//		setUpConnection(1);
//		
//		DelegateFactory factory = new DelegateFactory(false, null);
//		Destination jtaQueue = session.createQueue(TEST_QUEUE);
//		factory.jtaUriMap.clear();
//
//		JasperAdminMessage jam = new JasperAdminMessage(Type.jtaDataManagement, Command.notify, TEST_QUEUE, JasperConstants.DELEGATE_GLOBAL_QUEUE, WHITESPACE_URI);
//     
//		// send admin message to delegate to store URI. Leading/trailing whitespace should be removed by delegate
//		message = session.createObjectMessage(jam);
//		producer.send(message);
//		Thread.sleep(1000);
//		
//		//Send message with URI with no leading/trailing whitespace. Should be found by delegate
//		Session jtaSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
//	    MessageConsumer jtaConsumer = jtaSession.createConsumer(jtaQueue);
//	    Message jClientRequest;
//	    Message jtaResponse;
//		
//		message = session.createTextMessage(TEST_URI);
//		message.setJMSCorrelationID(null);
//		message.setJMSReplyTo(jtaQueue);
//		
//		// Send message to delegate
//		producer.send(message);
//		
//		Thread.sleep(2000);
//		
//		// simulate jClient timeout
//		int maxCount = 10;
//	
//		do{
//          	jClientRequest = jtaConsumer.receive(1000);
//          	maxCount--;
//          }while(jClientRequest == null && maxCount > 0);
//
//		 if (jClientRequest!= null && jClientRequest instanceof TextMessage) {
//			 TextMessage txtMsg = (TextMessage) jClientRequest;
//			 Assert.assertNotNull(txtMsg.getText());
//		 }
//
//		 // reply back to delegate with empty response from JTA if valid reply
//		 if(jClientRequest != null && jClientRequest instanceof ObjectMessage) {
//			ObjectMessage objMessage = (ObjectMessage) jClientRequest;
//            Object obj = objMessage.getObject();
//            String[] uri = (String[])obj;
//            
//            // URI in response message should have length of 31 which is URI without leading/trailing whitespace
//            Assert.assertEquals(31, uri[0].length());		 
//		 }
//		
//		tearDownConnection();
//
//	}

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
//	@Test
//	public void testEndToEndMessaging() throws Exception {
//		setUpConnection(2);
//		
//		// Setup so delegate will forward request back here (JTA)
//		Destination jtaQueue = session.createQueue(ADMIN_QUEUE);
//		List<String> l = new ArrayList<String>();
//		l.add(ADMIN_QUEUE);
//		
//		// Setup consumer to receive message from delegate (i.e. pretend to be a JTA)
//		Session jtaSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
//	    MessageConsumer jtaConsumer = jtaSession.createConsumer(jtaQueue);
//	    Message jClientRequest;
//	    Message jtaResponse;
//		
//		message = session.createTextMessage(TEST_URI);
//		message.setJMSCorrelationID(null);
//		message.setJMSReplyTo(jtaQueue);
//		
//		// Send message to delegate
//		producer.send(message);
//		
//		Thread.sleep(2000);
//		
//		// simulate jClient timeout
//		int maxCount = 10;
//	
//		do{
//          	jClientRequest = jtaConsumer.receive(1000);
//          	maxCount--;
//          }while(jClientRequest == null && maxCount > 0);
//
//		 if (jClientRequest!= null && jClientRequest instanceof TextMessage) {
//			 TextMessage txtMsg = (TextMessage) jClientRequest;
//			 Assert.assertNotNull(txtMsg.getText());
//		 }
//
//		 // reply back to delegate with empty response from JTA if valid reply
//		 if(jClientRequest != null && jClientRequest instanceof ObjectMessage) {
//			 session  = null;
//			 producer = null;
//		 
//			 Destination delegateUniqueQueue = jClientRequest.getJMSReplyTo();
//			 session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
//			 producer = session.createProducer(delegateUniqueQueue);
//		 
//			 jtaResponse = session.createTextMessage(EMPTY_JTA_RESPONSE);
//			 jtaResponse.setJMSDestination(jClientRequest.getJMSReplyTo());
//			 jtaResponse.setJMSCorrelationID(jClientRequest.getJMSCorrelationID());
//
//			 producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
//			 producer.setTimeToLive(30000);
//			 session.createQueue(delegateUniqueQueue.toString());
//			 producer.send(jtaResponse);
//			 Thread.sleep(1000);
//		 } 
//			
//		tearDownConnection();
//	}
//	
//	/*
//	 * This test sends an admin message with the publish command to test new
//	 * functionality of having delegate request JTA to republish URI for cases
//	 * when JSB goes down and comes back up
//	 */
//	@Test
//	public void testNotifyAdminMessage() throws Exception {
//		setUpConnection(2);
//		
//		DelegateFactory factory = new DelegateFactory(false, null);
//		factory.jtaUriMap.clear();
//
//		JasperAdminMessage jam = new JasperAdminMessage(Type.jtaDataManagement, Command.publish, TEST_QUEUE, JasperConstants.DELEGATE_GLOBAL_QUEUE, TEST_URI);
//        
//		message = session.createObjectMessage(jam);
//		producer.send(message);
//		Thread.sleep(1000);
//		tearDownConnection();
//
//	}
//	
//	/*
//	 * This test sends two request to the delegate with the same correlation Id
//	 * Exception should be thrown indicating non-unique correlation Id
//	 */
//	@Test
//	public void testDuplicateCorrelationId() throws Exception {
//		setUpConnection(1);
//		
//		// Setup so delegate will forward request back here (JTA)
//		Destination jtaQueue = session.createQueue(TEST_QUEUE);
//		List<String> l = new ArrayList<String>();
//		l.add(TEST_QUEUE);
//		delegateFactory.jtaUriMap.put(TEST_URI, l);
//		
//		message = session.createTextMessage(TEST_URI);
//		String corrId = "1234";
//		message.setJMSCorrelationID(corrId);
//		message.setJMSReplyTo(jtaQueue);
//		
//		// Send duplicate messages to delegate with same correlationIDs
//		try {
//			producer.send(message);
//			Thread.sleep(1000);
//			producer.send(message);
//		} catch(Exception ex) {
//			Assert.assertNotNull(ex);
//		}		
//		
//	}
//	
//	/*
//	 * This test sends a message with an invalid URI from the client to a 
//	 * delegate. The delegate should reject the message since it will not find
//	 * the URI in the internal hash map
//	 */
//	@Test
//	public void testMissingURI() throws Exception {
//		setUpConnection(1);
//		
//		// Setup so delegate will forward request back here (JTA)
//		Destination jtaQueue = session.createQueue(TEST_QUEUE);
//
//		message = session.createTextMessage(TEST_URI);
//		String corrId = "1234";
//		message.setJMSCorrelationID(corrId);
//		message.setJMSReplyTo(jtaQueue);
//		
//		producer.send(message);
//		Thread.sleep(1000);
//		
//		tearDownConnection();
//	
//	}
//	
//	@Test
//	public void testJasperBrokerPlugin() throws Exception {
//        BrokerService service = new BrokerService();
//        service.setPlugins(new BrokerPlugin[]{new JasperAuthenticationPlugin()});
//        assertEquals( 1024 * 1024 * 64, service.getSystemUsage().getMemoryUsage().getLimit() );
//        assertEquals( 1024L * 1024 * 1024 * 50, service.getSystemUsage().getTempUsage().getLimit() );
//        assertEquals( 1024L * 1024 * 1024 * 100, service.getSystemUsage().getStoreUsage().getLimit() );
//
//    }
//	
//	/*
//	 * This test simulates the Broker sending an admin message to the delegate
//	 * once a connection to JTA islost. This should remove the JTA's URI.  
//	 */
//	@Test
//	public void testRemoveURI() throws Exception {
//		setUpConnection(2);
//		
//		// manually add uri to internal hashmap
//		delegateFactory = new DelegateFactory(false, null);
//		List<String> l = new ArrayList<String>();
//		l.add(TEST_QUEUE);
//		delegateFactory.jtaUriMap.put(TEST_URI, l);
//		delegateFactory.jtaQueueMap.put(TEST_JTA_NAME, l);
//
//		JasperAdminMessage jam = new JasperAdminMessage(Type.jtaDataManagement, Command.delete, TEST_JTA_NAME, JasperConstants.DELEGATE_GLOBAL_QUEUE, TEST_URI);
//        
//		message = session.createObjectMessage(jam);
//		producer.send(message);
//		Thread.sleep(1000);
//		
//		tearDownConnection();
//		
//	}	

	private void setUpConnection(int numDelegates) throws Exception {
		 connectionFactory = new ActiveMQConnectionFactory("vm://localhost");
		 delegateFactory = new DelegateFactory(false, null);

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
	
	private String[][] loadOntology(){
		ArrayList<String[]> triples = new ArrayList<String[]>();
		triples.add(new String[]{"jtaA","is","jta"});
		triples.add(new String[]{"jtaA","provides","hrData"});
		triples.add(new String[]{"jtaA","param","hrSRId"});
		triples.add(new String[]{"jtaA","requires","patientId"});
		
		return triples.toArray(new String[][]{});
		
	}
	
}
