package org.jasper.core.delegate.handlers;

import java.util.ArrayList;
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
import org.jasper.core.persistence.PersistedObject;
import org.jasper.core.persistence.PersistenceFacade;
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
public class TestDataHandler  extends TestCase {

//	private static final String TEST_JTA_ADMIN_QUEUE = "jms.TestJTA.admin.queue";
//	private static final String TEST_JTA_NAME = "TestJTA";
//	private static final String COMPAREINT_NOTIFICATION = "http://coralcea.ca/jasper/roomTempData?output=xml?trigger=compareint(http://coralcea.ca/jasper/environmentalSensor/roomTemperature,gt,15)?expiry=20000?polling=5";
//	private static final String BASIC_NOTIFICATION = "http://coralcea.ca/jasper/roomTempData?trigger=compareint(http://coralcea.ca/jasper/environmentalSensor/roomTemperature,gt,15)?expiry=200?polling=1";
//	private static final String BAD_NOTIFICATION = "?trigger=compareint(http://coralcea.ca/jasper/environmentalSensor/roomTemperature,gt,15)?expiry=20";
//	private static final String NOT_MET_NOTIFICATION = "http://coralcea.ca/jasper/roomTempData?trigger=compareint(http://coralcea.ca/jasper/environmentalSensor/roomTemperature,gt,40)?expiry=1?polling=5";
//	private static final String WILL_EXPIRE_NOTIFICATION = "http://coralcea.ca/jasper/roomTempData?trigger=compareint(http://coralcea.ca/jasper/environmentalSensor/roomTemperature,gt,40)?expiry=0?polling=2";
//	
//	private Connection connection;
//	private DelegateFactory delegateFactory;
//	private ActiveMQConnectionFactory connectionFactory;
//	private Session session;
//	private Destination globalQueue;
//	private MessageProducer producer;
//	private Message message;
//	private Message adminRequest;
//	private MessageConsumer adminConsumer;
//	private MessageProducer adminProducer;
//	private ExecutorService executorService;
//	private Delegate delegate;
//	private JECore core;
//	private boolean dtaRegistered = false;
//	private HazelcastInstance hz;
//	private String tmp = "{ http://coralcea.ca/jasper/environmentalSensor/roomTemperature : 25 ,\n" +
//		    "http://coralcea.ca/jasper/timeStamp : 2013-10-14 02:18:45.0903 EDT }";
//	
//
//	/*
//	 * This tests the Data Handler for a successful end to end
//	 * notification request where the criteria is not met and
//	 * request expires
//	 */
//	@Test
//	public void testNotificationCriteriaNotMet() throws Exception {
//		System.out.println("==========================");
//		System.out.println("RUNNING DATA HANDLER TESTS");
//		System.out.println("==========================");
//		registerDTA();
//	    
//		// Send a notification request
//	    Integer id = (int) System.currentTimeMillis();
//		message = session.createTextMessage(WILL_EXPIRE_NOTIFICATION);
//		message.setJMSCorrelationID(id.toString());
//		message.setJMSReplyTo(globalQueue);
//		producer.send(message);
//		
//		int count = 0;
//		
//		// receive notification request and reply to it
//		// with room temperature data
//		do{
//			adminRequest = adminConsumer.receive(3000);
//	    	count++;
//	    	if(count >= 3) break;
//	    }while(adminRequest == null);
//		    
//		// send response to notification request
//        if(adminRequest != null){
//        	JsonArray array = new JsonArray();
//			array.add(tmp);
//		    JsonObject jasonObj = new JsonObject();
//			jasonObj.put("http://coralcea.ca/jasper/environmentalSensor/roomTemperature", 25);
//			jasonObj.put("timestamp", "09042013:7:00");
//		    
//		    Message response = session.createTextMessage(jasonObj.toString());
//			response.setJMSCorrelationID(adminRequest.getJMSCorrelationID());
//			adminProducer.send(adminRequest.getJMSReplyTo(), response );
//        }
//       
//		Thread.sleep(1000);		
//	}
//	
//	/*
//	 * This tests the Data Handler for a successful end to end
//	 * notification request where the criteria is met and data
//	 * is returned
//	 */
//	@Test
//	public void testNotificationCriteriaMet() throws Exception {
//		registerDTA();
//	    
//		// Send a notification request
//	    Integer id = (int) System.currentTimeMillis();
//		message = session.createTextMessage(COMPAREINT_NOTIFICATION);
//		message.setJMSCorrelationID(id.toString());
//		message.setJMSReplyTo(globalQueue);
//		producer.send(message);
//		
//		int count = 0;
//		
//		// receive notification request and reply to it
//		// with room temperature data
//		do{
//			adminRequest = adminConsumer.receive(3000);
//	    	count++;
//	    	if(count >= 3) break;
//	    }while(adminRequest == null);
//		    
//		// send response to notification request
//        if(adminRequest != null){
//        	JsonArray array = new JsonArray();
//			array.add(tmp);
//		    JsonObject jasonObj = new JsonObject();
//			jasonObj.put("http://coralcea.ca/jasper/environmentalSensor/roomTemperature", 25);
//			jasonObj.put("timestamp", "09042013:7:00");
//		    
//		    Message response = session.createTextMessage(jasonObj.toString());
//			response.setJMSCorrelationID(adminRequest.getJMSCorrelationID());
//			adminProducer.send(adminRequest.getJMSReplyTo(), response );
//        }
//        
//		Thread.sleep(1000);
//	}
//	
//
//	
//	/*
//	 * This tests the Data Handler error paths for
//	 * notification requests
//	 */
//	@Test
//	public void testNotificationErrors() throws Exception {
//
//		// Send invalid notification request (no RURI)
//		message = session.createTextMessage(BAD_NOTIFICATION);
//		message.setJMSReplyTo(globalQueue);
//		producer.send(message);
//		Thread.sleep(1000);
//		
//		// Send invalid notification request (empty trigger)
//		message = session.createTextMessage("http://coralcea.ca/jasper/roomTempData?trigger=");
//		message.setJMSReplyTo(globalQueue);
//		producer.send(message);
//		
//		message = session.createTextMessage(BASIC_NOTIFICATION+"?expiry=200?polling=-1");
//		message.setJMSReplyTo(globalQueue);
//		producer.send(message);
//		
//		message = session.createTextMessage(BASIC_NOTIFICATION+"?expiry=2?polling=300?output=");
//		message.setJMSReplyTo(globalQueue);
//		producer.send(message);
//		
//		message = session.createTextMessage(BASIC_NOTIFICATION+"?expiry=ab?polling=cd?output=");
//		message.setJMSReplyTo(globalQueue);
//		producer.send(message);
//
//		
//		// Send empty text message
//		message = session.createTextMessage(null);
//		producer.send(message);
//		Thread.sleep(1000);
//		
//	}
//	
//	/*
//	 * This tests the Data Handler where notification request
//	 * criteria is not met and it expires
//	 */
//	@Test
//	public void testNotificationExpiryWithResponse() throws Exception {
//		registerDTA();
//		// Send notification request where criteria not met
//		Integer id = (int) System.currentTimeMillis();
//		message = session.createTextMessage(NOT_MET_NOTIFICATION);
//		message.setJMSCorrelationID(id.toString());
//		message.setJMSReplyTo(globalQueue);
//		producer.send(message);
//		
//		 Message adminRequest;
//		 int count = 0;
//		
//	    do{
//    		adminRequest = adminConsumer.receive(3000);
//    		count++;
//    		if(count >= 3) break;
//    	}while(adminRequest == null);
//	    
//	    if(adminRequest != null){
//	    	JsonArray array = new JsonArray();
//	    	array.add(tmp);
//	    	JsonObject jasonObj = new JsonObject();
//	    	jasonObj.put("http://coralcea.ca/jasper/environmentalSensor/roomTemperature", 25);
//	    	jasonObj.put("timestamp", "09042013:7:00");
//	    
//	    	Message response = session.createTextMessage(jasonObj.toString());
//	    	response.setJMSCorrelationID(adminRequest.getJMSCorrelationID());
//	    	adminProducer.send(adminRequest.getJMSReplyTo(), response );
//	    }
//	  
//		Thread.sleep(1000);
//	}
//	
//	/*
//	 * This tests the PersistedObject class which is used to store
//	 * all stateful data for Data Handler high availability
//	 */
//	@Test
//	public void testPersistedObject() throws Exception {
//		PersistedObject pObj = new PersistedObject();
//		pObj.setOutput("json");
//		pObj.setReplyTo(null);
//		pObj.setCorrelationID("123");
//		pObj.setRequest("request");
//		pObj.setKey("key");
//		pObj.setRURI("ruri");
//		pObj.getNotification();
//		pObj.setUDEInstance("UDE:1");
//		
//		TestCase.assertNotNull(pObj);
//		TestCase.assertEquals("json", pObj.getOutput());
//		TestCase.assertEquals("123", pObj.getCorrelationID());
//		TestCase.assertEquals("request", pObj.getRequest());
//		TestCase.assertEquals("key", pObj.getKey());
//		TestCase.assertEquals("ruri", pObj.getRURI());
//		TestCase.assertEquals("UDE:1", pObj.getUDEInstance());
//		
//		Object myObj = PersistenceFacade.getInstance().getSharedMemoryInstance();
//		TestCase.assertNotNull(myObj);
//	}
//
//	@Before
//	public void setUp() throws Exception {
//		 System.setProperty("delegate-property-file", "../zipRoot/jsb-core/config/delegate.properties");
//		 connectionFactory = new ActiveMQConnectionFactory("vm://localhost");
//		 
//		 core = JECore.getInstance();
//		 
//		 Config cfg = new Config();
//		 GroupConfig groupConfig = new GroupConfig("testNotificationJunitTestingSuite", "testNotificationJunitTestingSuite_" + System.currentTimeMillis());
//		 cfg.setGroupConfig(groupConfig);
//		 hz = Hazelcast.newHazelcastInstance(cfg);
//		 
//		 delegateFactory = new DelegateFactory(false, core);
//
//        // Create a Connection
//        connectionFactory.setUserName(JasperConstants.JASPER_ADMIN_USERNAME);
//        connectionFactory.setPassword(JasperConstants.JASPER_ADMIN_PASSWORD);
//        connection = connectionFactory.createConnection();
//        connection.start();
//		
//		
//		executorService = Executors.newCachedThreadPool();
//		
//		delegate = delegateFactory.createDelegate();
//		executorService.execute(delegate);
//       
//		session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
//		globalQueue = session.createQueue(JasperConstants.DELEGATE_GLOBAL_QUEUE);
//		producer = session.createProducer(globalQueue);
//		producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
//		producer.setTimeToLive(30000);
//	}
//
//	@After
//	public void tearDown() throws Exception {
//		// Send jta disconnect message
//		if(dtaRegistered){
//			JasperAdminMessage jam2 = new JasperAdminMessage(Type.ontologyManagement, Command.jta_disconnect, TEST_JTA_NAME);
//			message = session.createObjectMessage(jam2);
//			producer.send(message);
//			dtaRegistered = false;
//		}
//		
//		// Clean up - no need to close connection as that will be done in JasperBroker	
//		session.close();
//		connection.close();
//
//		delegate.shutdown();
//		hz.getLifecycleService().shutdown();
//		Thread.sleep(500);
//		session           = null;
//		connection        = null;
//		producer          = null;
//		globalQueue       = null;
//		delegate          = null;
//		executorService   = null;
//		connectionFactory = null;
//		delegateFactory   = null;
//		core              = null;
//		
//		Thread.sleep(2000);
//		
//	}
//	
//	private String[][] loadOntology(){
//		ArrayList<String[]> triples = new ArrayList<String[]>();
//	
//		triples.add(new String[]{"http://coralcea.ca/jasper/vocabulary/jtaA","http://coralcea.ca/jasper/vocabulary/is","http://coralcea.ca/jasper/vocabulary/jta"});
//		triples.add(new String[]{"http://coralcea.ca/jasper/vocabulary/jtaA","http://coralcea.ca/jasper/vocabulary/provides","http://coralcea.ca/jasper/roomTempData"});
//		triples.add(new String[]{"http://coralcea.ca/jasper/vocabulary/jtaA","http://coralcea.ca/jasper/vocabulary/queue",TEST_JTA_ADMIN_QUEUE});
//		
//		return triples.toArray(new String[][]{});
//		
//	}
//	
//	private void registerDTA() throws Exception{
//		JasperAdminMessage jam = new JasperAdminMessage(Type.ontologyManagement, Command.jta_connect, TEST_JTA_NAME);
//        
//		// Send admin message to DTA to publish ontology
//		message = session.createObjectMessage(jam);
//		Destination adminQueue = session.createQueue(TEST_JTA_ADMIN_QUEUE);
//		adminConsumer = session.createConsumer(adminQueue);
//		adminProducer = session.createProducer(null);
//		producer.send(message);
//		
//		// Wait admin message and send triples in reply message
//	    Message adminRequest;
//	    int count = 0;
//	    
//	    do{
//    		adminRequest = adminConsumer.receive(3000);
//    		count++;
//    		if(count >= 3) break;
//    	}while(adminRequest == null);
//	    
//	    if (adminRequest instanceof ObjectMessage) {
//        	ObjectMessage objMessage = (ObjectMessage) adminRequest;
//        	Object obj = objMessage.getObject();
//        	if(obj instanceof JasperAdminMessage){
//				if(((JasperAdminMessage) obj).getType() == Type.ontologyManagement && ((JasperAdminMessage) obj).getCommand() == Command.get_ontology){
//        			String[][] triples = loadOntology();
//        			Message response = session.createObjectMessage(triples);
//        			response.setJMSCorrelationID(adminRequest.getJMSCorrelationID());
//					adminProducer.send(adminRequest.getJMSReplyTo(), response );
//					dtaRegistered = true;
//				}
//        	}
//	    }
//	}
	
}
