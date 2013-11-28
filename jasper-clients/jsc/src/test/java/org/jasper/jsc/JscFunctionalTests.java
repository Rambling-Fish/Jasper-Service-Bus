package org.jasper.jsc;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class JscFunctionalTests {

	private Jsc jscUnderTest;
	private BrokerService broker;
	private MessageConsumer delegateConsumerSim;
	private MessageProducer delegateProducerSim;
	private Session session;
	private Connection connection;
	
	
	
	@Before
	public void setUp() throws Exception {	
		
		broker = new BrokerService();
		broker.setPersistent(false);
		broker.addConnector("tcp://0.0.0.0:61616");
		broker.start();
		assert(broker.waitUntilStarted());
		
		Properties localBrokerProps = new Properties();
		localBrokerProps.put("jsc.username", "username");
		localBrokerProps.put("jsc.password", "password");
		localBrokerProps.put("jsc.transport", "tcp://0.0.0.0:61616");
		
		jscUnderTest = new Jsc(localBrokerProps);
		jscUnderTest.init();
		
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://0.0.0.0:61616");
		connection = connectionFactory.createConnection();
		connection.start();
		
		session = connection.createSession(false,Session.AUTO_ACKNOWLEDGE);
		Queue globalDelegateQueue = session.createQueue("jms.jasper.delegate.global.queue");
		
		delegateConsumerSim = session.createConsumer(globalDelegateQueue);
		
		delegateProducerSim = session.createProducer(null);
		delegateProducerSim.setDeliveryMode(DeliveryMode.PERSISTENT);
		delegateProducerSim.setTimeToLive(30000);
		
	}

	@After
	public void tearDown() throws Exception {
		jscUnderTest.destroy();
		
		delegateConsumerSim.close();
		delegateProducerSim.close();
		session.close();
		connection.close();
		
		broker.stop();
		broker.waitUntilStopped();
	}
	
	private Properties loadTestProperties() {
    	Properties prop = new Properties();
		try {
			File file = new File(System.getProperty("user.dir") +  "/src/test/resources/jsc.properties");
			FileInputStream fileInputStream = new FileInputStream(file);
			prop.load(fileInputStream);			
		} catch (IOException e) {
			e.printStackTrace();
		}  		
    	return prop;
	}

	@Test
	public void testSuccessfulRequest() throws JMSException {
	
		delegateConsumerSim.setMessageListener( new MessageListener() {			
			public void onMessage(Message msg) {
				
				System.out.println("delegateSim recv msg");

				
				assert(msg instanceof TextMessage);
				
				String txt = null;
				
				try {
					txt = ((TextMessage)msg).getText();
				} catch (JMSException e) {
					e.printStackTrace();
					fail("JMSException caught in onMessage when getting the TextMessage payload");
				}
				
				assertNotNull(txt);
				
				try {
					assertNotNull(msg.getJMSCorrelationID());
				} catch (JMSException e1) {
					e1.printStackTrace();
					fail("JMSException caught in onMessage when getting the JMSCorrelationID");
				}
				
				try {
					assertNotNull(msg.getJMSReplyTo());
				} catch (JMSException e1) {
					e1.printStackTrace();
					fail("JMSException caught in onMessage when getting the JMSReplyTo");
				}
				
				try {
					Message response = session.createTextMessage("{\"response\" : \"sucess\"}");
					response.setJMSCorrelationID(msg.getJMSCorrelationID());
					delegateProducerSim.send(msg.getJMSReplyTo(), response );
					System.out.println("delegateSim sent resp");
				} catch (JMSException e) {
					e.printStackTrace();
					fail("JMSException caught in onMessage when sending response");
				}
			}
		});
		
		System.out.println("jsc-ws sim send msg");
		String response = jscUnderTest.get("http://jasper.com/testRequest");
		System.out.println("jsc-ws sim recv resp");

		assertEquals(response, "{\"response\" : \"sucess\"}");		
	}

}