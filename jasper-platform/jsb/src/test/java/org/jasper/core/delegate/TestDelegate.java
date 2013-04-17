package org.jasper.core.delegate;

import junit.framework.Assert;
import junit.framework.TestCase;

import java.util.UUID;
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

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;

import org.jasper.jLib.jCommons.message.JasperSyncRequest;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Command;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Type;

import org.junit.Test;

public class TestDelegate  extends TestCase {

	private static final String DELEGATE_GLOBAL_QUEUE = "jms.jasper.delegate.global.queue";
	private static final String TEST_URI = "http://coralcea.com/1.0/testURI";
	private static final String TEST_QUEUE = "jms.jta.testJTA.replyToQueue";
	private static final String JASPER_ADMIN_USERNAME = "jasperAdminUsername";
	private static final String JASPER_ADMIN_PASSWORD = "jasperAdminPassword";
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
	@Test
	public void testDelegateFactoryAndPool() throws Exception {
		// Instantiate the delegate pool
		ExecutorService executorService = Executors.newCachedThreadPool();
		delegateFactory = DelegateFactory.getInstance();
		Delegate[] delegates = new Delegate[3];
		
		for(int i=0;i<delegates.length;i++) {
			delegates[i] = delegateFactory.createDelegate();
			executorService.execute(delegates[i]);
		} 
		
			Assert.assertEquals(delegates.length, 3);
			Assert.assertNotNull(delegateFactory);
			for(int i=0;i<delegates.length;i++) {
				delegates[i].shutdown();
			}
			
			Thread.sleep(3000);
	}
		
	/*
	 * This test simulates adding/removing URIs to the delegate's internal JTA
	 * hash map. It also tries to put a null key which should result in NPE
	 */
	@Test
	public void testURIMap() throws Exception {
		delegateFactory = DelegateFactory.getInstance();
		for(int i = 0; i < 5; i++) {
			delegateFactory.jtaUriMap.put(TEST_URI+i, TEST_QUEUE+i);
		}
		Assert.assertEquals(delegateFactory.jtaUriMap.size(), 5);
		
		delegateFactory.jtaUriMap.remove(TEST_URI+3);
		Assert.assertEquals(delegateFactory.jtaUriMap.size(), 4);
		
		// test NPE exception with null map key
		try {
			delegateFactory.jtaUriMap.put(null, TEST_QUEUE);
		} catch(Exception ex) {
			Assert.assertNotNull(ex);
		}
	}
	
	/*
	 * This test simulates the JasperEngineConnector sending a JTA's URI to the
	 * delegate via an admin message. The delegate's internal JTA map should
	 * have a size of 1 if successful.
	 */
	@Test
	public void testPublishURI() throws Exception {
		setUpConnection();

		JasperAdminMessage goodJam = new JasperAdminMessage(Type.jtaDataManagement, Command.notify, "testJTA", DELEGATE_GLOBAL_QUEUE, TEST_URI);
        
		message = session.createObjectMessage(goodJam);
		producer.send(message);
		Thread.sleep(1000);
		
		Assert.assertEquals(delegateFactory.jtaUriMap.size(), 1);
		
		tearDownConnection();
	}
	
	/*
	 * This test simulates a message sent from jClient to a delegate which is
	 * then sent to the JTA.  The delegate forwards the request to the JTA 
	 * using the JMS replyToQueue in the incoming request.
	 */
	@Test
	public void testClientToJTAMessaging() throws Exception {
		setUpConnection();
		
		// Setup so delegate will forward request back here (JTA)
		Destination jtaQueue = session.createQueue(TEST_QUEUE);
		String clientMsg = "This is a test client message";
		delegateFactory.jtaUriMap.put(TEST_URI, TEST_QUEUE);
		
		// Setup consumer to receive message from delegate (i.e. pretend to be a JTA)
		Session jtaSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	    MessageConsumer jtaConsumer = jtaSession.createConsumer(jtaQueue);
	    Message jClientRequest;
		
		JasperSyncRequest syncReq = new JasperSyncRequest(TEST_URI, clientMsg);
		message = session.createObjectMessage(syncReq);
		String corrId = UUID.randomUUID().toString();
		message.setJMSCorrelationID(corrId);
		message.setJMSReplyTo(jtaQueue);
		
		// Send message to delegate
		producer.send(message);
		
		Thread.sleep(2000);
		
		do{
          	jClientRequest = jtaConsumer.receive(1000);
          }while(jClientRequest == null);
		 if (jClientRequest instanceof ObjectMessage) {
			 ObjectMessage objMessage = (ObjectMessage) jClientRequest;
			 Object obj = objMessage.getObject();
             if(obj instanceof JasperSyncRequest) {
            	 Assert.assertNotNull(obj);
             }
		 }
			
		tearDownConnection();
	}
	
	@Test
	public void testJasperBroker()
    {
        BrokerService service = new BrokerService();
        assertEquals( 1024 * 1024 * 64, service.getSystemUsage().getMemoryUsage().getLimit() );
        assertEquals( 1024L * 1024 * 1024 * 50, service.getSystemUsage().getTempUsage().getLimit() );
        assertEquals( 1024L * 1024 * 1024 * 100, service.getSystemUsage().getStoreUsage().getLimit() );
    }
	
	private void setUpConnection() throws Exception {
		 connectionFactory = new ActiveMQConnectionFactory("vm://localhost");
		 delegateFactory = DelegateFactory.getInstance();
		 delegateFactory.jtaUriMap.clear();

        // Create a Connection
        connectionFactory.setUserName(JASPER_ADMIN_USERNAME);
        connectionFactory.setPassword(JASPER_ADMIN_PASSWORD);
        connection = connectionFactory.createConnection();
        connection.start();
		
		
		executorService = Executors.newCachedThreadPool();
		delegates = new Delegate[2];
		
		for(int i=0;i<delegates.length;i++){
			delegates[i] = delegateFactory.createDelegate();
			executorService.execute(delegates[i]);
		}
       
		session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		globalQueue = session.createQueue(DELEGATE_GLOBAL_QUEUE);
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
