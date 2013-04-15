package org.jasper.core.delegate;

import junit.framework.Assert;
import junit.framework.TestCase;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.net.URI;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.BrokerFactory;

import org.apache.log4j.Logger;
import org.jasper.core.message.JasperSyncRequest;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Command;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Type;

import org.junit.Test;

public class TestDelegate  extends TestCase {

	private static final String DELEGATE_GLOBAL_QUEUE = "jms.jasper.delegate.global.queue";
	private static final String TEST_URI = "testURI";
	private static final String TEST_QUEUE = "jms.jta.testJTA.replyToQueue";
	private String name;
	private Connection connection;
	private boolean isShutdown;
	private ExecutorService singleThreadExecutor;
	private DelegateFactory factory;
	protected BrokerService broker;
	
	
	protected void setUp() throws Exception {
        super.setUp();
        broker = createBroker();
//        policyMap.setDefaultEntry(getDefaultPolicy());
//        broker.setDestinationPolicy(policyMap);
        broker.start();
    }
	
	@Test
	/*
	 * This testcase creates a pool of 3 delegates using the delegate factory
	 */
	public void testDelegateFactoryAndPool() throws Exception {
		// Instantiate the delegate pool
		ExecutorService executorService = Executors.newCachedThreadPool();
		factory = DelegateFactory.getInstance();
		Delegate[] delegates = new Delegate[3];
		
		for(int i=0;i<delegates.length;i++) {
			delegates[i]=factory.createDelegate();
			executorService.execute(delegates[i]);
		} 
		
			Assert.assertEquals(delegates.length, 3);
			Assert.assertNotNull(factory);
			for(int i=0;i<delegates.length;i++) {
				delegates[i].shutdown();
			}
			
			Thread.sleep(3000);
	}
		
	@Test
	/*
	 * This test simulates sending a JTA URI to the delegate
	 */
	public void testPublishURI() throws Exception {
		factory = DelegateFactory.getInstance();
		for(int i = 0;i < 6; i++) {
			factory.jtaUriMap.put(TEST_URI+i, TEST_QUEUE+i);
		}
		
	}
	
	@Test
	/*
	 * This test simulates removing a JTA URI from the delegate
	 * when JTA connection is lost
	 */
	public void testRemoveURI() throws Exception {
		factory = DelegateFactory.getInstance();
		int length = factory.jtaUriMap.size();
		factory.jtaUriMap.remove(TEST_URI+3);
		Assert.assertTrue("URI map entry deleted", factory.jtaUriMap.size() < length);
		
	}
	
	@Test
	public void testJasperBroker()
    {
        BrokerService service = new BrokerService();
        assertEquals( 1024 * 1024 * 64, service.getSystemUsage().getMemoryUsage().getLimit() );
        assertEquals( 1024L * 1024 * 1024 * 50, service.getSystemUsage().getTempUsage().getLimit() );
        assertEquals( 1024L * 1024 * 1024 * 100, service.getSystemUsage().getStoreUsage().getLimit() );
    }
	
	 protected BrokerService createBroker() throws Exception {
	        BrokerService broker = BrokerFactory.createBroker(new URI("broker:()/localhost?persistent=false"));
	        return broker;
	    }
	 
	 protected void tearDown() throws Exception {
	        broker.stop();
	        broker.waitUntilStopped();
	        broker = null;
	        super.tearDown();
	    }

	
	
}
