package org.jasper.core.delegate;

import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.util.Collection;
import java.util.UUID;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import junit.framework.TestCase;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.jasper.core.UDE;
import org.jasper.core.constants.JasperConstants;
import org.jasper.core.constants.JtaInfo;
import org.jasper.core.persistence.PersistedDataRequest;
import org.jasper.core.persistence.PersistedSubscriptionRequest;
import org.jasper.core.persistence.PersistenceFacade;
import org.jasper.core.persistence.PersistenceFacadeFactory;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Command;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Type;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class TestDelegate{// extends TestCase {
	@Mock private static UDE mockUDE;
	@Mock private static Connection mockConnection;
	@Mock private static DelegateOntology mockJOntology;
	@Mock private static Session mockSession;
	@Mock private static Queue mockQueue;
	@Mock private static MessageConsumer mockConsumer;
	@Mock private static MessageProducer mockProducer;
	private static Destination dest;
	private static Model model;
	private static String ipAddr;
	private static PersistenceFacade cachingSys;
	private static Delegate classUnderTest;
	private static final String JASPER_ADMIN_USERNAME = "jasperAdminUsername";
	private static final String JASPER_ADMIN_PASSWORD = "jasperAdminPassword";
	
	/*
	 * This test constructor of the Delegate class various get methods
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
        
		Session sess = classUnderTest.getGlobalSession();
		classUnderTest.getLocksMap();
		classUnderTest.getJOntology();
		classUnderTest.getResponsesMap();
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

		Message msg = classUnderTest.createObjectMessage(new JasperAdminMessage(Type.ontologyManagement,Command.get_ontology));
		msg.setJMSCorrelationID("123");
		classUnderTest.processDelegateQMsg(msg);
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

		Message msg = classUnderTest.createObjectMessage(new JasperAdminMessage(Type.ontologyManagement,Command.get_ontology));
		msg.setJMSCorrelationID("123");
		classUnderTest.sendMessage(globalQueue, msg);
		classUnderTest.sendMessage("testQ", msg);
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

		ObjectMessage objMsg = classUnderTest.createObjectMessage(JASPER_ADMIN_PASSWORD);
		TestCase.assertNotNull(objMsg);
		
		TextMessage txtMsg = classUnderTest.createTextMessage("text");
		TestCase.assertNotNull(txtMsg);
	}	
	
	/*
	 * This tests the Delegate createJasperResponse method
	 */
	@Test
	public void testDelegateCreateJasperResponse() throws Exception {
		JasperConstants.ResponseCodes code = JasperConstants.ResponseCodes.OK;
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost");
		Connection connection;

        // Create a Connection
        connectionFactory.setUserName(JASPER_ADMIN_USERNAME);
        connectionFactory.setPassword(JASPER_ADMIN_PASSWORD);
        connection = connectionFactory.createConnection();

		String result = classUnderTest.createJasperResponse(code, "respMsg", "response", "application/json", "1.0");
		String result2 = classUnderTest.createJasperResponse(code, "respMsg", "response", null, null);
		TestCase.assertNotNull(result);
		TestCase.assertNotNull(result2);
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
		JasperConstants.ResponseCodes code = JasperConstants.ResponseCodes.ACCEPTED;
		code.setCode(200);
		int myCode = code.getCode();
		TestCase.assertEquals(myCode, 200);
		code.getDescription();
		code.toString();
	}
	
	@Test
	public void testPersistedRequests() throws Exception{
		when(mockUDE.getUdeInstance()).thenReturn("ude:0");
		PersistedDataRequest request = new PersistedDataRequest("123", null, null, System.currentTimeMillis());
		classUnderTest.removePersistedRequest(request);
		
		TextMessage txtMsg = classUnderTest.createTextMessage("test");
		txtMsg.setJMSCorrelationID("123");
		txtMsg.setJMSReplyTo(dest);
		PersistedDataRequest retrievedReq = classUnderTest.persistDataRequest(txtMsg);
		TestCase.assertNotNull(retrievedReq);
		
		classUnderTest.connectionToRemoteUdeLost("ude:0");
		
		classUnderTest.persistSubscriptionRequest("http://coralcea/ca/jasper/hrSID", "12345", "123", "application/ld+json", dest, null, -1);
		classUnderTest.persistSubscriptionRequest("http://coralcea/ca/jasper/hrSID", "12345", "123", "application/ld+json", dest, null, -1);
		Collection<PersistedSubscriptionRequest> requests = classUnderTest.getDataSubscriptions("http://coralcea/ca/jasper/hrSID");
		TestCase.assertNotNull(requests);
		classUnderTest.removePersistedSubscriptionRequest("http://coralcea/ca/jasper/hrSID", "12345");
	}
	
	@BeforeClass
	public static void setUpClass() throws Exception {
		model          = ModelFactory.createDefaultModel();
		ipAddr         = InetAddress.getLocalHost().getHostAddress();
		cachingSys = PersistenceFacadeFactory.getFacade(ipAddr, UUID.randomUUID().toString(), "testPasswd");
		System.setProperty("delegate-property-file", "../zipRoot/jsb-core/config/delegate.properties");
	}
	
	@Before
	public void setUp() throws Exception{
		MockitoAnnotations.initMocks(this);
		when(mockUDE.getCachingSys()).thenReturn(cachingSys);
		when(mockConnection.createSession(true,Session.SESSION_TRANSACTED)).thenReturn(mockSession);
		when(mockSession.createQueue(JasperConstants.DELEGATE_GLOBAL_QUEUE)).thenReturn(mockQueue);
		when(mockSession.createConsumer(mockQueue)).thenReturn(mockConsumer);
		when(mockConnection.createSession(false, Session.AUTO_ACKNOWLEDGE)).thenReturn(mockSession);
		when(mockSession.createProducer(null)).thenReturn(mockProducer);
		when(mockUDE.getBrokerTransportIp()).thenReturn(ipAddr);
		when(mockSession.createQueue("jms.delegate." + mockUDE.getBrokerTransportIp() + "." + 0 + ".queue")).thenReturn(mockQueue);
		when(mockSession.createConsumer(mockQueue)).thenReturn(mockConsumer);
		classUnderTest = new Delegate(mockUDE);
		classUnderTest.start();
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
		cachingSys.shutdown();
		classUnderTest.shutdown();
		classUnderTest = null;
	}
}
