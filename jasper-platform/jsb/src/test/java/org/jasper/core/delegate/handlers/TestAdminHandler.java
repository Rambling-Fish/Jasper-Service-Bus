package org.jasper.core.delegate.handlers;

import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;

import junit.framework.TestCase;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.jasper.core.UDE;
import org.jasper.core.constants.JasperConstants;
import org.jasper.core.delegate.Delegate;
import org.jasper.core.delegate.DelegateOntology;
import org.jasper.core.persistence.PersistenceFacade;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Command;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Type;
import org.junit.After;
import org.junit.Before;
//
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.hp.hpl.jena.rdf.model.Model;
//
public class TestAdminHandler extends TestCase {
	
	private AdminHandler classUnderTest;
	@Mock private Delegate mockDelegate;
	@Mock private ObjectMessage mockRequest;
	@Mock Message mockMsg;
	@Mock private Destination mockDest;
	@Mock private DelegateOntology mockJOntology;
	@Mock private UDE mockUDE;
	@Mock private Model mockModel;
	@Mock private Session mockSession;
	@Mock private Queue mockQ;
	@Mock private PersistenceFacade mockCachingSys;
	private Connection connection;
	private Delegate realDelegate;
	private Map<String,Object> locks = new ConcurrentHashMap<String,Object>();
	private Map<String,Message> responses = new ConcurrentHashMap<String,Message>();
	private JasperAdminMessage jam;
	private String ipAddr;
	private String corrID = "123";
	private String jtaName = "testDTA";
	
	@Test
	public void testFofSakeOfTest(){
		
	}
	
	
//	/*
//	 * This tests the run method of the AdminHandler error handling when
//	 * receiving a null Jasper Admin Message
//	 */
//	@Test
//	public void testNullAdminMsg() throws Exception{
//		System.out.println("===========================");
//		System.out.println("RUNNING ADMIN HANDLER TESTS");
//		System.out.println("===========================");
//		
//		classUnderTest.run();	
//	}
//	
//	/*
//	 * This tests the Admin Handler receiving a valid JAM that is a disconnect message
//	 */
//	@Test
//	public void testDisconnectMsg() throws Exception{
//		jam = new JasperAdminMessage(Type.ontologyManagement, Command.jta_disconnect, jtaName);
//		when(mockRequest.getObject()).thenReturn(jam);
//
//		classUnderTest.run();	
//	}
//	
//	/*
//	 * This tests the Admin Handler receiving a valid JAM that is a disconnect message
//	 * but with no details
//	 */
//	@Test
//	public void testDisconnectNoDetails() throws Exception{
//		jam = new JasperAdminMessage(Type.ontologyManagement, Command.jta_disconnect, "");
//		when(mockRequest.getObject()).thenReturn(jam);
//
//		classUnderTest.run();	
//	}
//	
////	/*
////	 * This tests the Admin Handler receiving a valid JAM that is a connect msg
////	 */
////	@Test
////	public void testConnectMsg() throws Exception{
////		jam = new JasperAdminMessage(Type.ontologyManagement, Command.jta_connect, jtaName);
////		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost");
////
////		// Create a Connection
////        connectionFactory.setUserName("Admin");
////        connectionFactory.setPassword("adminPassword");
////        connection = connectionFactory.createConnection();
////        connection.start();
////         
////		when(mockRequest.getObject()).thenReturn(jam);
////		realDelegate = new Delegate(mockUDE);
////		classUnderTest = null;
////		classUnderTest = new AdminHandler(realDelegate, mockJOntology, mockRequest, locks, responses);
////
////		classUnderTest.run();	
////	}
//	
//	/*
//	 * This tests the Admin Handler receiving a valid JAM that is a connect message
//	 * but with no details
//	 */
//	@Test
//	public void testConnectNoDetails() throws Exception{
//		jam = new JasperAdminMessage(Type.ontologyManagement, Command.jta_connect, "");
//		when(mockRequest.getObject()).thenReturn(jam);
//
//		classUnderTest.run();	
//	}
//
////	@Before
////	public void setUp() throws Exception {
////		MockitoAnnotations.initMocks(this);
////		System.setProperty("delegate-property-file", "../zipRoot/jsb-core/config/delegate.properties");
////		ipAddr = InetAddress.getLocalHost().getHostAddress();
////		when(mockRequest.getJMSCorrelationID()).thenReturn(corrID);
////		when(mockRequest.getJMSReplyTo()).thenReturn(mockDest);
////		when(mockUDE.getCachingSys()).thenReturn(mockCachingSys);
////		 
////		classUnderTest = new AdminHandler(mockDelegate, mockJOntology, mockRequest, locks, responses); 
////	}
//
//	@After
//	public void tearDown() throws Exception {
//		classUnderTest = null;
//		}
	
}
