package org.jasper.core.delegate;

import junit.framework.Assert;
import junit.framework.TestCase;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.Broker;
import org.apache.activemq.broker.BrokerFilter;
import org.apache.activemq.broker.BrokerPlugin;
import org.apache.activemq.broker.BrokerService;
import org.jasper.core.JECore;
import org.jasper.core.JasperBrokerService;
import org.jasper.core.auth.JasperAuthenticationPlugin;
import org.jasper.core.constants.JasperConstants;
import org.jasper.core.constants.JtaInfo;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Command;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Type;
//
import org.junit.Test;
//
public class TestDelegate  extends TestCase {

	private static final String ADMIN_QUEUE    = "jms.TestJTA.admin.queue";
	private static final String TEST_JTA_NAME  = "TestJTA";
	private static final String SPARQL_QUERY3  = "?query=PREFIX%20:%20%3Chttp://coralcea.ca/jasper/vocabulary/%3E%20PREFIX%20jta:%20%3Chttp://coralcea.ca/jasper/vocabulary/jta/%3E%20PREFIX%20jasper:%20%3Chttp://coralcea.ca/jasper/%3E%20SELECT%20?jta%20?jtaProvidedData%20?params%20WHERE%20{%20{%20?jta%20:is%20:jta%20.%20?jta%20:provides%20?jtaProvidedData%20.%20}%20UNION%20{%20?jta%20:is%20:jta%20.%20?jta%20:param%20?params%20.%20}%20}&output=";
	private static final String SPARQL_QUERY2  = "?query=PREFIX%20:%20%3Chttp://coralcea.ca/jasper/vocabulary/%3E%20PREFIX%20jta:%20%3Chttp://coralcea.ca/jasper/vocabulary/jta/%3E%20PREFIX%20jasper:%20%3Chttp://coralcea.ca/jasper/%3E%20SELECT%20?jta%20?jtaProvidedData%20?params%20WHERE%20{%20{%20?jta%20:is%20:jta%20.%20?jta%20:provides%20?jtaProvidedData%20.%20}%20UNION%20{%20?jta%20:is%20:jta%20.%20?jta%20:param%20?params%20.%20}%20}&output=jj";
	private static final String SPARQL_QUERY   = "?query=PREFIX%20:%20%3Chttp://coralcea.ca/jasper/vocabulary/%3E%20PREFIX%20jta:%20%3Chttp://coralcea.ca/jasper/vocabulary/jta/%3E%20PREFIX%20jasper:%20%3Chttp://coralcea.ca/jasper/%3E%20SELECT%20?jta%20?jtaProvidedData%20?params%20WHERE%20{%20{%20?jta%20:is%20:jta%20.%20?jta%20:provides%20?jtaProvidedData%20.%20}%20UNION%20{%20?jta%20:is%20:jta%20.%20?jta%20:param%20?params%20.%20}%20}&output=json";
	private static final String DATA_QUERY     = "http://coralcea.ca/jasper/vocabulary/hrData?http://coralcea.ca/jasper/vocabulary/hrSRId=12";
	private static final String BAD_DATA_QUERY = "http://coralcea.ca/jasper/vocabulary/invalidURI";
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

	}
	
	/*
	 * This test simulates a valid sparql query coming in from the JSC.
	 */
	@Test
	public void testSparqlHandlerSuccess() throws Exception {
		setUpConnection(2);
		
		message = session.createTextMessage(SPARQL_QUERY);
		producer.send(message);
		
	}
	
	/*
	 * This test simulates an invalid sparql query coming in from the JSC.
	 */
	@Test
	public void testSparqlHandlerFail() throws Exception {
		setUpConnection(2);
		
		message = session.createTextMessage("?query=not a valid query");
		producer.send(message);
		
		message = session.createTextMessage(SPARQL_QUERY2);
		producer.send(message);
		
		message = session.createTextMessage(SPARQL_QUERY3);
		producer.send(message);

	}
	
	/*
	 * This tests the DataHandler class.
	 */
	@Test
	public void testDataHandler() throws Exception {
		setUpConnection(2);
		
		message = session.createTextMessage(DATA_QUERY);
		producer.send(message);
		
		Thread.sleep(2000);
		tearDownConnection();
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
		tearDownConnection();
	}

	
	/*
	 * This test exercises JTAInfo class.
	 */
	@Test
	public void testJTAInfo() throws Exception {
		JtaInfo info = new JtaInfo(TEST_JTA_NAME,"00093837","jsb0","myClientId","0.0.0.0");
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
		setUpConnection(1);
		message = delegates[0].createTextMessage(null);
		producer.send(message);
		
		Map<String, Serializable> map = new HashMap<String,Serializable>();
		map.put("key", "value");
		MapMessage mapMsg = delegates[0].createMapMessage(map);
		Assert.assertNotNull(mapMsg);
		
//		Thread.sleep(2000);
		tearDownConnection();
	}
	
	/*
	 * This tests AdminHandler error paths
	 */
	@Test
	public void testAdminHandlerError() throws Exception {
		setUpConnection(2);

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
		tearDownConnection();
	}
	
	/*
	 * This tests DataHandler error paths
	 */
	@Test
	public void testDataHandlerError() throws Exception {
		setUpConnection(2);

		message = session.createTextMessage(BAD_DATA_QUERY);
		producer.send(message);
		
		Thread.sleep(1000);
		tearDownConnection();
	}
	
	/*
	 * This test sends an invalid response to the delegate in response to a get_ontology 
	 * request.
	 */
	@Test
	public void testJTAConnectInvalidResponse() throws Exception {
		setUpConnection(2);

		JasperAdminMessage jam = new JasperAdminMessage(Type.ontologyManagement, Command.jta_connect, TEST_JTA_NAME);
        
		message = session.createObjectMessage(jam);
		Destination adminQueue = session.createQueue(ADMIN_QUEUE);
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
	    	    
		tearDownConnection();

	}

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
		triples.add(new String[]{"http://coralcea.ca/jasper/vocabulary/jtaA","http://coralcea.ca/jasper/vocabulary/is","http://coralcea.ca/jasper/vocabulary/jta"});
		triples.add(new String[]{"http://coralcea.ca/jasper/vocabulary/jtaA","http://coralcea.ca/jasper/vocabulary/provides","http://coralcea.ca/jasper/vocabulary/hrData"});
		triples.add(new String[]{"http://coralcea.ca/jasper/vocabulary/jtaA","http://coralcea.ca/jasper/vocabulary/param","http://coralcea.ca/jasper/vocabulary/hrSRId"});
		triples.add(new String[]{"http://coralcea.ca/jasper/vocabulary/jtaA","http://coralcea.ca/jasper/vocabulary/requires","http://coralcea.ca/jasper/vocabulary/patientId"});
		triples.add(new String[]{"http://coralcea.ca/jasper/vocabulary/jtaA","http://coralcea.ca/jasper/vocabulary/queue","http://coralcea.ca/jasper/vocabulary/jms.TestJTA.admin.queue"});
		triples.add(new String[]{"http://coralcea.ca/jasper/vocabulary/jtaA","http://coralcea.ca/jasper/vocabulary/requires"}); // invalid row on purpose!
		
		return triples.toArray(new String[][]{});
		
	}
	
}
