package org.jasper.core.delegate;

import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;
import javax.jms.Queue;
import javax.jms.Session;

import junit.framework.TestCase;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.jasper.core.UDE;
import org.jasper.core.constants.JasperConstants;
import org.jasper.core.constants.JtaInfo;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Command;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Type;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class TestDelegate extends TestCase {
	@Mock private UDE mockUDE;
	@Mock private Connection mockConnection;
	@Mock private DelegateOntology mockJOntology;
	@Mock private Session mockSession;
	@Mock private Queue mockQueue;
	@Mock private MessageConsumer mockConsumer;
	@Mock private MessageProducer mockProducer;
	private Model model;
	private String ipAddr;
	private Delegate classUnderTest;
	private static final String JASPER_ADMIN_USERNAME = "jasperAdminUsername";
	private static final String JASPER_ADMIN_PASSWORD = "jasperAdminPassword";

	
	/*
	 * This test constructor of the Delegate class and the shutdown method
	 */
	@Test
	public void testDelegateConstructor() throws Exception {
		System.out.println("======================");
		System.out.println("RUNNING DELEGATE TESTS");
		System.out.println("======================");
		
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost");
		Connection connection;

        // Create a Connection
        connectionFactory.setUserName(JASPER_ADMIN_USERNAME);
        connectionFactory.setPassword(JASPER_ADMIN_PASSWORD);
        connection = connectionFactory.createConnection();

		classUnderTest = new Delegate(mockUDE, connection, model, mockJOntology);
		classUnderTest.shutdown();	
	}
	
	/*
	 * This tests the Delegate processing of an Object message
	 */
	@Test
	public void testDelegateOnMessage() throws Exception {
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost");
		Connection connection;

        // Create a Connection
        connectionFactory.setUserName(JASPER_ADMIN_USERNAME);
        connectionFactory.setPassword(JASPER_ADMIN_PASSWORD);
        connection = connectionFactory.createConnection();

		classUnderTest = new Delegate(mockUDE, connection, model, mockJOntology);
		Message msg = classUnderTest.createObjectMessage(new JasperAdminMessage(Type.ontologyManagement,Command.get_ontology));
		msg.setJMSCorrelationID("123");
		classUnderTest.onMessage(msg);
		classUnderTest.shutdown();
	}	
	
	/*
	 * This tests the Delegate sendMessage() methods
	 */
	@Test
	public void testDelegateSendMessage() throws Exception {
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost");
		Destination globalQueue;
		Session session;
		Connection connection;

        // Create a Connection
        connectionFactory.setUserName(JASPER_ADMIN_USERNAME);
        connectionFactory.setPassword(JASPER_ADMIN_PASSWORD);
        connection = connectionFactory.createConnection();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        globalQueue = session.createQueue(JasperConstants.DELEGATE_GLOBAL_QUEUE);

		classUnderTest = new Delegate(mockUDE, connection, model, mockJOntology);
		Message msg = classUnderTest.createObjectMessage(new JasperAdminMessage(Type.ontologyManagement,Command.get_ontology));
		msg.setJMSCorrelationID("123");
		classUnderTest.sendMessage(globalQueue, msg);
		classUnderTest.sendMessage("testQ", msg);
		classUnderTest.shutdown();
	}	
	
	/*
	 * This tests the Delegate createTextMessage and createObjectMessage 
	 * and createMapMessage methods
	 */
	@Test
	public void testDelegateCreateMessages() throws Exception {
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost");
		Connection connection;

        // Create a Connection
        connectionFactory.setUserName(JASPER_ADMIN_USERNAME);
        connectionFactory.setPassword(JASPER_ADMIN_PASSWORD);
        connection = connectionFactory.createConnection();

		classUnderTest = new Delegate(mockUDE, connection, model, mockJOntology);
		ObjectMessage objMsg = classUnderTest.createObjectMessage(JASPER_ADMIN_PASSWORD);
		TestCase.assertNotNull(objMsg);
		
		TextMessage txtMsg = classUnderTest.createTextMessage("text");
		TestCase.assertNotNull(txtMsg);
		
		Map<String, Serializable> map = new HashMap<String,Serializable>();
		map.put("key", "value");
		MapMessage mapMsg = classUnderTest.createMapMessage(map);
		TestCase.assertNotNull(mapMsg);
		
		classUnderTest.shutdown();
	}	
	
	/*
	 * This tests the Delegate createJasperResponse method
	 */
	@Test
	public void testDelegateCreateJasperResponse() throws Exception {
		JasperConstants.responseCodes code = JasperConstants.responseCodes.OK;
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost");
		Connection connection;

        // Create a Connection
        connectionFactory.setUserName(JASPER_ADMIN_USERNAME);
        connectionFactory.setPassword(JASPER_ADMIN_PASSWORD);
        connection = connectionFactory.createConnection();

		classUnderTest = new Delegate(mockUDE, connection, model, mockJOntology);

		String result = classUnderTest.createJasperResponse(code, "respMsg", "response", "application/json", "1.0");
		String result2 = classUnderTest.createJasperResponse(code, "respMsg", "response", null, null);
		TestCase.assertNotNull(result);
		TestCase.assertNotNull(result2);
		classUnderTest.shutdown();
	}	
		
	/*
	 * This test exercises JTAInfo class.
	 */
	@Test
	public void testJTAInfo() throws Exception {
		System.out.println("==================");
		System.out.println("RUNNING MISC TESTS");
		System.out.println("==================");
		JtaInfo info = new JtaInfo("TEST_DTA","00093837","jsb0","myClientId","0.0.0.0");
		info.getClientId();
		info.getClientIp();
		info.getJsbConnectedTo();
		info.getJtaName();
		info.getLicenseKey();
		info = null;
	}
	
	/*
	 * This test exercises JasperConstants response codes.
	 */
	@Test
	public void testResponseCodes() throws Exception {
		JasperConstants.responseCodes code = JasperConstants.responseCodes.ACCEPTED;
		code.setCode(200);
		int myCode = code.getCode();
		TestCase.assertEquals(myCode, 200);
		code.getDescription();
		code.toString();
	}

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		model          = ModelFactory.createDefaultModel();
		ipAddr         = InetAddress.getLocalHost().getHostAddress();
		System.setProperty("delegate-property-file", "../zipRoot/jsb-core/config/delegate.properties");
		when(mockConnection.createSession(true,Session.SESSION_TRANSACTED)).thenReturn(mockSession);
		when(mockSession.createQueue(JasperConstants.DELEGATE_GLOBAL_QUEUE)).thenReturn(mockQueue);
		when(mockSession.createConsumer(mockQueue)).thenReturn(mockConsumer);
		when(mockConnection.createSession(false, Session.AUTO_ACKNOWLEDGE)).thenReturn(mockSession);
		when(mockSession.createProducer(null)).thenReturn(mockProducer);
		when(mockUDE.getBrokerTransportIp()).thenReturn(ipAddr);
		when(mockSession.createQueue("jms.delegate." + mockUDE.getBrokerTransportIp() + "." + 0 + ".queue")).thenReturn(mockQueue);
		when(mockSession.createConsumer(mockQueue)).thenReturn(mockConsumer); 
	}

	@After
	public void tearDown() throws Exception {
		// Clean up - no need to close connection as that will be done in JasperBroker
		classUnderTest = null;
	}
}
