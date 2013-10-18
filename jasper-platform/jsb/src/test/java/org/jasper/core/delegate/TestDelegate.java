package org.jasper.core.delegate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.jena.atlas.json.JsonObject;
import org.jasper.core.JECore;
import org.jasper.core.constants.JasperConstants;
import org.jasper.core.constants.JtaInfo;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Command;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Type;
import org.junit.After;
import org.junit.Before;
//
import org.junit.Test;
import org.mule.transport.jasperengine.JasperEngineConnector;

import com.hazelcast.config.Config;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
//
public class TestDelegate  extends TestCase {

	private static final String TEST_JTA_A_ADMIN_QUEUE = "jms.TestJTA-A.admin.queue";
	private static final String TEST_JTA_B_ADMIN_QUEUE = "jms.TestJTA-B.admin.queue";
	private static final String TEST_JTA_A_NAME = "TestJTA-A";
	private static final String TEST_JTA_B_NAME = "TestJTA-B";
	private static final String SPARQL_QUERY3   = "?query=PREFIX%20:%20%3Chttp://coralcea.ca/jasper/vocabulary/%3E%20PREFIX%20jta:%20%3Chttp://coralcea.ca/jasper/vocabulary/jta/%3E%20PREFIX%20jasper:%20%3Chttp://coralcea.ca/jasper/%3E%20SELECT%20?jta%20?jtaProvidedData%20?params%20WHERE%20{%20{%20?jta%20:is%20:jta%20.%20?jta%20:provides%20?jtaProvidedData%20.%20}%20UNION%20{%20?jta%20:is%20:jta%20.%20?jta%20:param%20?params%20.%20}%20}&output=";
	private static final String SPARQL_QUERY2   = "?query=PREFIX%20:%20%3Chttp://coralcea.ca/jasper/vocabulary/%3E%20PREFIX%20jta:%20%3Chttp://coralcea.ca/jasper/vocabulary/jta/%3E%20PREFIX%20jasper:%20%3Chttp://coralcea.ca/jasper/%3E%20SELECT%20?jta%20?jtaProvidedData%20?params%20WHERE%20{%20{%20?jta%20:is%20:jta%20.%20?jta%20:provides%20?jtaProvidedData%20.%20}%20UNION%20{%20?jta%20:is%20:jta%20.%20?jta%20:param%20?params%20.%20}%20}&output=jj";
	private static final String SPARQL_QUERY    = "?query=PREFIX%20:%20%3Chttp://coralcea.ca/jasper/vocabulary/%3E%20PREFIX%20jta:%20%3Chttp://coralcea.ca/jasper/vocabulary/jta/%3E%20PREFIX%20jasper:%20%3Chttp://coralcea.ca/jasper/%3E%20SELECT%20?jta%20?jtaProvidedData%20?params%20WHERE%20{%20{%20?jta%20:is%20:jta%20.%20?jta%20:provides%20?jtaProvidedData%20.%20}%20UNION%20{%20?jta%20:is%20:jta%20.%20?jta%20:param%20?params%20.%20}%20}&output=json";
	private static final String GOOD_DATA_QUERY = "http://coralcea.ca/jasper/vocabulary/hrData?http://coralcea.ca/jasper/vocabulary/hrSRId=12";
	private static final String INDIRECT_DATA_QUERY = "http://coralcea.ca/jasper/vocabulary/hrData?http://coralcea.ca/jasper/vocabulary/patientId=0012";
	private static final String BAD_DATA_QUERY  = "http://coralcea.ca/jasper/vocabulary/invalidURI";
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
	 * This test sends an invalid response to the delegate in response to a get_ontology 
	 * request.
	 */
	@Test
	public void testJTAConnectInvalidResponse() throws Exception {
		JasperAdminMessage jam = new JasperAdminMessage(Type.ontologyManagement, Command.jta_connect, TEST_JTA_A_NAME);
        
		message = session.createObjectMessage(jam);
		Destination adminQueue = session.createQueue(TEST_JTA_A_ADMIN_QUEUE);
		MessageConsumer adminConsumer = session.createConsumer(adminQueue);
		MessageProducer adminProducer = session.createProducer(null);
		producer.send(message);
		
		// Wait for a message
	    Message adminRequest;
	    int count = 0;
	    
	    do{
    		adminRequest = adminConsumer.receive(3000);
    		count++;
    		if(count >= 2) break;
    	}while(adminRequest == null);
	    
	    if (adminRequest instanceof ObjectMessage) {
        	ObjectMessage objMessage = (ObjectMessage) adminRequest;
        	Object obj = objMessage.getObject();
        	if(obj instanceof JasperAdminMessage){
				if(((JasperAdminMessage) obj).getType() == Type.ontologyManagement && ((JasperAdminMessage) obj).getCommand() == Command.get_ontology){
        			Message response = session.createObjectMessage("INVALID");
        			response.setJMSCorrelationID(adminRequest.getJMSCorrelationID());
					adminProducer.send(adminRequest.getJMSReplyTo(), response );
				}
        	}
	    }
	}

	
	/*
	 * This test simulates the broker sending a connect message to the delegate.
	 * The admin handler then sends a JAM message to tell the JTA to publish it's
	 * ontology. This test case receives the get_ontology JAM message and creates
	 * an object message composed of triples and sends it back to the delegate.
	 * Since the model is transient with the TC it also sends valid and invalid
	 * sparql queries and sends a data request (ie as the JSC) and formats and
	 * sends a reply (as a JTA) to test end to end data handler processing
	 */
	@Test
	public void testJTAConnect() throws Exception {
		JasperAdminMessage jam = new JasperAdminMessage(Type.ontologyManagement, Command.jta_connect, TEST_JTA_A_NAME);
        
		message = session.createObjectMessage(jam);
		Destination adminQueue = session.createQueue(TEST_JTA_A_ADMIN_QUEUE);
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
        			String[][] triples = loadOntology(1);
        			Message response = session.createObjectMessage(triples);
        			response.setJMSCorrelationID(adminRequest.getJMSCorrelationID());
					adminProducer.send(adminRequest.getJMSReplyTo(), response );
				}
        	}
	    }
	    
	    // Successful SPARQL Query to in-memory Jena model
	    message = session.createTextMessage(SPARQL_QUERY);
		producer.send(message);
		
		//Fail Sparql queries
		message = session.createTextMessage("?query=not a valid query");
		producer.send(message);
		
		message = session.createTextMessage(SPARQL_QUERY2);
		producer.send(message);
		
		message = session.createTextMessage(SPARQL_QUERY3);
		producer.send(message);
		
		// Send data request
		message = session.createTextMessage(GOOD_DATA_QUERY);
		producer.send(message);
		
		count = 0;
  
		    do{
	    		adminRequest = adminConsumer.receive(3000);
	    		count++;
	    		if(count >= 3) break;
	    	}while(adminRequest == null);
		    
        if(adminRequest != null){    
		    JsonObject jasonObj = new JsonObject();
			jasonObj.put("HR", "76");
			jasonObj.put("timestamp", "09042013:7:00");
		    
		    Message response = session.createTextMessage(jasonObj.toString());
			response.setJMSCorrelationID(adminRequest.getJMSCorrelationID());
			adminProducer.send(adminRequest.getJMSReplyTo(), response );
        }
		    
		Thread.sleep(1000);
		
		// Send data request but change correlationId in response
		message = session.createTextMessage(GOOD_DATA_QUERY);
		producer.send(message);
				
		count = 0;
		  
			do{
				adminRequest = adminConsumer.receive(3000);
			    count++;
			   	if(count >= 3) break;
			    	}while(adminRequest == null);
				    
		    if(adminRequest != null){    
		    	JsonObject jasonObj = new JsonObject();
		    	jasonObj.put("HR", "76");
		    	jasonObj.put("timestamp", "09042013:7:00");
				    
		    	Message response = session.createTextMessage(jasonObj.toString());
		    	response.setJMSCorrelationID("55");
		    	adminProducer.send(adminRequest.getJMSReplyTo(), response );
		    }
				    
		    Thread.sleep(1000);
		
		// Send jta disconnect message
		JasperAdminMessage jam2 = new JasperAdminMessage(Type.ontologyManagement, Command.jta_disconnect, this.TEST_JTA_A_NAME);
		message = session.createObjectMessage(jam2);
		producer.send(message);
		
		Thread.sleep(2000);
	}
		
	/*
	 * This test exercises JTAInfo class.
	 */
	@Test
	public void testJTAInfo() throws Exception {
		JtaInfo info = new JtaInfo(TEST_JTA_A_NAME,"00093837","jsb0","myClientId","0.0.0.0");
		info.getClientId();
		info.getClientIp();
		info.getJsbConnectedTo();
		info.getJtaName();
		info.getLicenseKey();
		info = null;
		
	}
	
	/*
	 * This test exercises Delegate class.
	 */
	@Test
	public void testDelegate() throws Exception {
		message = delegates[0].createTextMessage(null);
		producer.send(message);
		
		Map<String, Serializable> map = new HashMap<String,Serializable>();
		map.put("key", "value");
		MapMessage mapMsg = delegates[0].createMapMessage(map);
		Assert.assertNotNull(mapMsg);
	}
	
	/*
	 * This tests AdminHandler error paths
	 */
	@Test
	public void testAdminHandlerError() throws Exception {
		JasperAdminMessage jam = new JasperAdminMessage(Type.ontologyManagement, Command.jta_connect, (Serializable)null);
		message = session.createObjectMessage(jam);
		producer.send(message);
		
		JasperAdminMessage jam2 = new JasperAdminMessage(Type.ontologyManagement, Command.jta_connect, "");    
		message = session.createObjectMessage(jam2);
		producer.send(message);
		
		JasperAdminMessage jam3 = new JasperAdminMessage(Type.ontologyManagement, Command.jta_disconnect, "");
		message = session.createObjectMessage(jam3);
		producer.send(message);
		
		Thread.sleep(1000);
	}
	
	/*
	 * This tests DataHandler error paths
	 */
	@Test
	public void testDataHandlerError() throws Exception {
		message = session.createTextMessage(BAD_DATA_QUERY);
		producer.send(message);
		
		message = session.createTextMessage(BAD_DATA_QUERY);
		message.setJMSCorrelationID("12345");
		producer.send(message);
		
		message = session.createMapMessage();
		producer.send(message);
		
		Thread.sleep(1000);
	}
	
	/*
	 * This test simulates having 2 JTAs connected to the core. It allows for
	 * testing some of the success paths in the DataHandler namely when the
	 * param is a JsonArray
	 */
	@Test
	public void testMultipleJTAs() throws Exception {
		// Register first test JTA
		JasperAdminMessage jam = new JasperAdminMessage(Type.ontologyManagement, Command.jta_connect, TEST_JTA_A_NAME);
		message = session.createObjectMessage(jam);
		Destination adminQueue = session.createQueue(TEST_JTA_A_ADMIN_QUEUE);
		MessageConsumer adminConsumer = session.createConsumer(adminQueue);
		MessageProducer adminProducer = session.createProducer(null);
		producer.send(message);
		
		// Wait for a get ontology message for JTA-A
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
        			String[][] triples = loadOntology(1);
        			Message response = session.createObjectMessage(triples);
        			response.setJMSCorrelationID(adminRequest.getJMSCorrelationID());
					adminProducer.send(adminRequest.getJMSReplyTo(), response );
				}
        	}
	    }
	    
	    // Register 2nd test JTA
	    jam = new JasperAdminMessage(Type.ontologyManagement, Command.jta_connect, TEST_JTA_B_NAME);
        
		message = session.createObjectMessage(jam);
		Destination adminQueue2 = session.createQueue(TEST_JTA_B_ADMIN_QUEUE);
		MessageConsumer adminConsumer2 = session.createConsumer(adminQueue2);
		producer.send(message);
		
		// Wait for a get ontology message for JTA-B
	    count = 0;
	    
	    do{
    		adminRequest = adminConsumer2.receive(3000);
    		count++;
    		if(count >= 3) break;
    	}while(adminRequest == null);
	    
	    if (adminRequest instanceof ObjectMessage) {
        	ObjectMessage objMessage = (ObjectMessage) adminRequest;
        	Object obj = objMessage.getObject();
        	if(obj instanceof JasperAdminMessage){
				if(((JasperAdminMessage) obj).getType() == Type.ontologyManagement && ((JasperAdminMessage) obj).getCommand() == Command.get_ontology){
        			String[][] triples = loadOntology(2);
        			Message response = session.createObjectMessage(triples);
        			response.setJMSCorrelationID(adminRequest.getJMSCorrelationID());
					adminProducer.send(adminRequest.getJMSReplyTo(), response );
				}
        	}
	    }
		
		// Send indirect data request. That is a request for HRData but
	    // provides PatientId as a param. HR JTA-A requires heart rate
	    // sensorId so this forces more introspection of the model to discover
	    // JTA that requires PatientId and that provides HR SensorId which
	    // in this TC is JTA-B
		message = session.createTextMessage(INDIRECT_DATA_QUERY);
		producer.send(message);
		
		// wait for message from core
		count = 0;
		  
		do{
			adminRequest = adminConsumer2.receive(3000);
		    count++;
		   	if(count >= 3) break;
		    	}while(adminRequest == null);
			    
	    if(adminRequest != null){    
	    	JsonObject jasonObj = new JsonObject();
	    	jasonObj.put("http://coralcea.ca/jasper/vocabulary/hrSRId", "1234");
	    	Message response = session.createTextMessage(jasonObj.toString());
	    	adminProducer.send(adminRequest.getJMSReplyTo(), response );
	    }
	    
		Thread.sleep(1000);
	}

	@Before
	public void setUp() throws Exception {
		System.setProperty("delegate-property-file", "../zipRoot/jsb-core/config/delegate.properties");
		 connectionFactory = new ActiveMQConnectionFactory("vm://localhost");
		 
		 JECore core = JECore.getInstance();
		 
		 Config cfg = new Config();
		 GroupConfig groupConfig = new GroupConfig("testDelegateJunitTestingSuite", "testDelegateJunitTestingSuite_" + System.currentTimeMillis());
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
		delegates = new Delegate[2];
		
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

	@After
	public void tearDown() throws Exception {
		// Clean up - no need to close connection as that will be done in JasperBroker
		session.close();
		connection.close();

		for(int i = 0; i< delegates.length; i++) {
			delegates[i].shutdown();
		}
		delegateFactory.getHazelcastInstance().getLifecycleService().shutdown();
		Thread.sleep(500);
		if(delegateFactory.getHazelcastInstance().getLifecycleService().isRunning()){
			delegateFactory.getHazelcastInstance().getLifecycleService().shutdown();
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
	
	private String[][] loadOntology(int ontology){
		ArrayList<String[]> triples = new ArrayList<String[]>();
		if(ontology == 1){
			triples.add(new String[]{"http://coralcea.ca/jasper/vocabulary/jtaA","http://coralcea.ca/jasper/vocabulary/is","http://coralcea.ca/jasper/vocabulary/jta"});
			triples.add(new String[]{"http://coralcea.ca/jasper/vocabulary/jtaA","http://coralcea.ca/jasper/vocabulary/provides","http://coralcea.ca/jasper/vocabulary/hrData"});
			triples.add(new String[]{"http://coralcea.ca/jasper/vocabulary/jtaA","http://coralcea.ca/jasper/vocabulary/param","http://coralcea.ca/jasper/vocabulary/hrSRId"});
			triples.add(new String[]{"http://coralcea.ca/jasper/vocabulary/jtaA","http://coralcea.ca/jasper/vocabulary/queue",TEST_JTA_A_ADMIN_QUEUE});
			triples.add(new String[]{"http://coralcea.ca/jasper/vocabulary/jtaA","http://coralcea.ca/jasper/vocabulary/requires"}); // invalid row on purpose!
		}
		else{
			triples.add(new String[]{"http://coralcea.ca/jasper/vocabulary/jtaB","http://coralcea.ca/jasper/vocabulary/is","http://coralcea.ca/jasper/vocabulary/jta"});
			triples.add(new String[]{"http://coralcea.ca/jasper/vocabulary/jtaB","http://coralcea.ca/jasper/vocabulary/provides","http://coralcea.ca/jasper/vocabulary/hrSRId"});
			triples.add(new String[]{"http://coralcea.ca/jasper/vocabulary/jtaB","http://coralcea.ca/jasper/vocabulary/param","http://coralcea.ca/jasper/vocabulary/patientId"});
			triples.add(new String[]{"http://coralcea.ca/jasper/vocabulary/jtaB","http://coralcea.ca/jasper/vocabulary/queue",TEST_JTA_B_ADMIN_QUEUE});
		}
		
		return triples.toArray(new String[][]{});
		
	}
	
}
