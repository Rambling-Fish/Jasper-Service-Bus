package org.jasper.core.notification;

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

import junit.framework.TestCase;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.jasper.core.JECore;
import org.jasper.core.constants.JasperConstants;
import org.jasper.core.delegate.Delegate;
import org.jasper.core.delegate.DelegateFactory;
import org.jasper.core.notification.triggers.Trigger;
import org.jasper.core.notification.triggers.TriggerFactory;
import org.jasper.core.notification.util.JsonResponseParser;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Command;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Type;
import org.junit.After;
import org.junit.Before;
//
import org.junit.Test;

import com.hazelcast.config.Config;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
//
public class TestNotification  extends TestCase {

	private static final String TEST_JTA_ADMIN_QUEUE = "jms.TestJTA.admin.queue";
	private static final String TEST_JTA_NAME = "TestJTA";
	private static final String RURI = "http://coralcea.ca/jasper/environmentalSensor/temperature/";
	private static final String COUNT_NOTIFICATION = "http://coralcea.ca/jasper/roomTempData?trigger=count(http://coralcea.ca/jasper/environmentalSensor/temperature/temp,gt,15)?expiry=20?polling=5";
	private static final String BAD_NOTIFICATION = "?trigger=count(http://coralcea.ca/jasper/environmentalSensor/temperature/temp,gt,15)?expiry=20";
	private static final String NOT_MET_NOTIFICATION = "http://coralcea.ca/jasper/roomTempData?trigger=count(http://coralcea.ca/jasper/environmentalSensor/temperature/temp,gt,40)?expiry=5?polling=5";
	private Connection connection;
	private DelegateFactory delegateFactory;
	private ActiveMQConnectionFactory connectionFactory;
	private Session session;
	private Destination globalQueue;
	private MessageProducer producer;
	private Message message;
	private ExecutorService executorService;
	private Delegate delegate;
	private JECore core;
	private String tmp = "{ http://coralcea.ca/jasper/environmentalSensor/temperature/temp : 25 ,\n" +
		    "http://coralcea.ca/jasper/timeStamp : 2013-10-14 02:18:45.0903 EDT }";

	/*
	 * This tests the utility class JsonResponeParser 
	 * request.
	 */
	@Test
	public void testJsonParser() { 
		String tmp2 = "{ http://coralcea.ca/jasper/environmentalSensor/temperature/temp : 25R ,\n" +
			    "http://coralcea.ca/jasper/timeStamp : 2013-10-14 02:18:45.0903 EDT }"; 
		JsonArray response = new JsonArray();
		JsonResponseParser parser = new JsonResponseParser();
		
		// test passing in empty array
		List<Integer> list = parser.parse(response, RURI);
		
		// test valid array and a bad room temperature (NaN)
		response.add(tmp);
		response.add(tmp2);
		
		list = parser.parse(response, RURI);
		TestCase.assertNotNull(list);
		
	}
	
	/*
	 * This tests the TriggerFactory class
	 */
	@Test
	public void testTriggerFactory() {
		TriggerFactory factory = new TriggerFactory();

		Trigger trig1 = factory.createTrigger("count", RURI, "gt", "20");
		Trigger trig2 = factory.createTrigger("range", RURI, "10", "25");
		Trigger trig3 = factory.createTrigger("wrong", RURI, "gt", "45");
		Trigger trig4 = factory.createTrigger("count", RURI);
		Trigger trig5 = factory.createTrigger("range", RURI,"20","ne", "30");

		TestCase.assertNotNull(trig1);
		TestCase.assertNotNull(trig2);
		TestCase.assertNull(trig3);
		TestCase.assertNull(trig4);
		TestCase.assertNull(trig5);
	
	}
	
	/*
	 * This tests the Count Trigger class
	 */
	@Test
	public void testCountTrigger() {
		TriggerFactory factory = new TriggerFactory();
		Trigger gtCount  = factory.createTrigger("count", RURI, "gt", "20");
		Trigger eqCount  = factory.createTrigger("count", RURI, "eq", "25");
		Trigger ltCount  = factory.createTrigger("count", RURI, "lt", "38");
		Trigger neCount  = factory.createTrigger("count", RURI, "ne", "26");
		Trigger geCount  = factory.createTrigger("count", RURI, "ge", "22");
		Trigger leCount  = factory.createTrigger("count", RURI, "le", "25");
		Trigger badCount = factory.createTrigger("count", RURI, "bad", "25");
		Trigger left     = factory.createTrigger("count", "23Q", "lt", "40");

		JsonArray response = new JsonArray();
		response.add(tmp);
		
		// test all the operands
		TestCase.assertTrue(gtCount.evaluate(response));
		TestCase.assertTrue(eqCount.evaluate(response));
		TestCase.assertTrue(ltCount.evaluate(response));
		TestCase.assertTrue(neCount.evaluate(response));
		TestCase.assertTrue(geCount.evaluate(response));
		TestCase.assertTrue(leCount.evaluate(response));
		TestCase.assertFalse(badCount.evaluate(response));
		left.evaluate(response);
		
		//Test empty response passed into trigger
		response.clear();
		TestCase.assertFalse(gtCount.evaluate(response));

	}
	
	/*
	 * This tests the Range Trigger class
	 */
	@Test
	public void testRangeTrigger() {
		JsonArray response = new JsonArray();
		response.add(tmp);
		TriggerFactory factory = new TriggerFactory();
		Trigger range1  = factory.createTrigger("range", RURI, "20", "30");
		Trigger range2  = factory.createTrigger("range", "30T", "25", "36");
		
		TestCase.assertTrue(range1.evaluate(response));
		
		response.clear();
		range2.evaluate(response);
		
		tmp = "helloWorld";
		response.add(tmp);
		TestCase.assertFalse(range1.evaluate(response));
		
	}
	
	/*
	 * This tests the Data Handler success and error paths for
	 * notification requests
	 */
	@Test
	public void testDataHandlerForNotifications() throws Exception {
		JasperAdminMessage jam = new JasperAdminMessage(Type.ontologyManagement, Command.jta_connect, TEST_JTA_NAME);
        
		message = session.createObjectMessage(jam);
		Destination adminQueue = session.createQueue(TEST_JTA_ADMIN_QUEUE);
		MessageConsumer adminConsumer = session.createConsumer(adminQueue);
		MessageProducer adminProducer = session.createProducer(null);
		producer.send(message);
		
		// Wait for a message
	    Message adminRequest;
	    int count = 0;
	    
	    do{
    		adminRequest = adminConsumer.receive(3000);
    		count++;
    		if(count >= 3) break;
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
	    
		// Send notification request
		message = session.createTextMessage(COUNT_NOTIFICATION);
		producer.send(message);
		
		count = 0;
  
		do{
			adminRequest = adminConsumer.receive(3000);
	    	count++;
	    	if(count >= 3) break;
	    }while(adminRequest == null);
		    
		// send response to notification request
        if(adminRequest != null){
        	JsonArray array = new JsonArray();
			array.add(tmp);
		    JsonObject jasonObj = new JsonObject();
			jasonObj.put("http://coralcea.ca/jasper/environmentalSensor/temperature/temp", 25);
			jasonObj.put("timestamp", "09042013:7:00");
		    
		    Message response = session.createTextMessage(jasonObj.toString());
			response.setJMSCorrelationID(adminRequest.getJMSCorrelationID());
			adminProducer.send(adminRequest.getJMSReplyTo(), response );
        }
		    
		Thread.sleep(1000);
		
		// Send invalid notification request (no RURI)
		message = session.createTextMessage(BAD_NOTIFICATION);
		producer.send(message);
		Thread.sleep(1000);
		
		// Send empty text messsage
		message = session.createTextMessage(null);
		producer.send(message);
		Thread.sleep(1000);
		
		// Send notification request where criteria not met
		message = session.createTextMessage(NOT_MET_NOTIFICATION);
		producer.send(message);
		
		count = 0;
		  
	    do{
    		adminRequest = adminConsumer.receive(3000);
    		count++;
    		if(count >= 3) break;
    	}while(adminRequest == null);
	    
	    if(adminRequest != null){
	    	JsonArray array = new JsonArray();
	    	array.add(tmp);
	    	JsonObject jasonObj = new JsonObject();
	    	jasonObj.put("http://coralcea.ca/jasper/environmentalSensor/temperature/temp", 25);
	    	jasonObj.put("timestamp", "09042013:7:00");
	    
	    	Message response = session.createTextMessage(jasonObj.toString());
	    	response.setJMSCorrelationID(adminRequest.getJMSCorrelationID());
	    	adminProducer.send(adminRequest.getJMSReplyTo(), response );
	    }
    
		Thread.sleep(1000);
		
		// Send jta disconnect message
		JasperAdminMessage jam2 = new JasperAdminMessage(Type.ontologyManagement, Command.jta_disconnect, TEST_JTA_NAME);
		message = session.createObjectMessage(jam2);
		producer.send(message);
		
		// Send notification request after DTA has de-registered
		message = session.createTextMessage(COUNT_NOTIFICATION);
		producer.send(message);
		
		Thread.sleep(2000);
	}

	@Before
	public void setUp() throws Exception {
		 connectionFactory = new ActiveMQConnectionFactory("vm://localhost");
		 
		 core = JECore.getInstance();
		 
		 Config cfg = new Config();
		 GroupConfig groupConfig = new GroupConfig("testNotificationJunitTestingSuite", "testNotificationJunitTestingSuite_" + System.currentTimeMillis());
		 cfg.setGroupConfig(groupConfig);
		 HazelcastInstance hz = Hazelcast.newHazelcastInstance(cfg);
		 core.setHazelcastInstance(hz);
		 
		 delegateFactory = new DelegateFactory(false, core);

        // Create a Connection
        connectionFactory.setUserName(JasperConstants.JASPER_ADMIN_USERNAME);
        connectionFactory.setPassword(JasperConstants.JASPER_ADMIN_PASSWORD);
        connection = connectionFactory.createConnection();
        connection.start();
		
		
		executorService = Executors.newCachedThreadPool();
		
		delegate = delegateFactory.createDelegate();
		executorService.execute(delegate);
       
		session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		globalQueue = session.createQueue(JasperConstants.DELEGATE_GLOBAL_QUEUE);
		producer = session.createProducer(globalQueue);
		producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
		producer.setTimeToLive(30000);
	}

	@After
	public void tearDown() throws Exception {
		// Clean up - no need to close connection as that will be done in JasperBroker
		session.close();
		connection.close();

		delegate.shutdown();
		delegateFactory.getHazelcastInstance().getLifecycleService().shutdown();
		Thread.sleep(500);
		if(delegateFactory.getHazelcastInstance().getLifecycleService().isRunning()){
			delegateFactory.getHazelcastInstance().getLifecycleService().shutdown();
		}
		session           = null;
		connection        = null;
		producer          = null;
		globalQueue       = null;
		delegate         = null;
		executorService   = null;
		connectionFactory = null;
		delegateFactory   = null;
		core              = null;
		
		Thread.sleep(2000);
		
	}
	
	private String[][] loadOntology(){
		ArrayList<String[]> triples = new ArrayList<String[]>();
	
		triples.add(new String[]{"http://coralcea.ca/jasper/vocabulary/jtaA","http://coralcea.ca/jasper/vocabulary/is","http://coralcea.ca/jasper/vocabulary/jta"});
		triples.add(new String[]{"http://coralcea.ca/jasper/vocabulary/jtaA","http://coralcea.ca/jasper/vocabulary/provides","http://coralcea.ca/jasper/roomTempData"});
		triples.add(new String[]{"http://coralcea.ca/jasper/vocabulary/jtaA","http://coralcea.ca/jasper/vocabulary/queue",TEST_JTA_ADMIN_QUEUE});
		
		return triples.toArray(new String[][]{});
		
	}
	
}
