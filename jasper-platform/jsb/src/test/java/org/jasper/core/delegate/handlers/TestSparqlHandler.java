package org.jasper.core.delegate.handlers;

import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.util.UUID;

import javax.jms.Destination;
import javax.jms.TextMessage;

import junit.framework.TestCase;

import org.jasper.core.UDE;
import org.jasper.core.constants.JasperConstants;
import org.jasper.core.delegate.Delegate;
import org.jasper.core.delegate.DelegateOntology;
import org.jasper.core.persistence.PersistenceFacadeImpl;
import org.junit.After;
import org.junit.Before;
//
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
//
public class TestSparqlHandler extends TestCase {
	
	private SparqlHandler classUnderTest;
	@Mock private UDE mockUDE;
	@Mock private Delegate mockDelegate;
	@Mock private TextMessage mockRequest;
	@Mock private TextMessage mockResp;
	@Mock private Destination mockDest;
	@Mock private DelegateOntology mockOntology;
	private String corrID = "1234";
	private String errorResp = "{400: Bad Request, msg : Invalid request received - request is null or empty string, version : 2.1}";
	private String validResp = "{200: OK, msg : Valid query, version : 2.1}";
	private String deploymentAndInstance = "UDE:0";
	private String msgText  = "{\"version\":\"1.0\",\"method\":\"GET\",\"ruri\":\"sparql\",\"headers\":{\"content-type\":\"application/json\"},\"parameters\":{\"parameters\":\"query\u003dPREFIX%20:%20%3Chttp://coralcea.ca/jasper/vocabulary/%3E%20PREFIX%20jta:%20%3Chttp://coralcea.ca/jasper/vocabulary/jta/%3E%20PREFIX%20jasper:%20%3Chttp://coralcea.ca/jasper/%3E%20SELECT%20?jta%20?jtaProvidedData%20?params%20WHERE%20{%20{%20?jta%20:is%20:jta%20.%20?jta%20:provides%20?jtaProvidedData%20.%20}%20UNION%20{%20?jta%20:is%20:jta%20.%20?jta%20:param%20?params%20.%20}%20}\",\"output\":\"json\"}}";
	private String msgText2 = "{\"version\":\"1.0\",\"method\":\"GET\",\"ruri\":\"sparql\",\"headers\":{\"content-type\":\"application/json\"},\"parameters\":{\"parameters\":\"query\u003dPREFIX%20:%20%3C%3E%20SELECT%20queryString}\",\"output\":\"unknown\"}}";

	@Test
	public void testFofSakeOfTest(){
		
	}
	
//	/*
//	 * This tests the Sparql Handler error handling
//	 */
//	@Test
//	public void testNullQuery() throws Exception{
//		System.out.println("============================");
//		System.out.println("RUNNING SPARQL HANDLER TESTS");
//		System.out.println("============================");
//		when(mockDelegate.createJasperResponse(JasperConstants.ResponseCodes.BADREQUEST, "Invalid SPARQL query received", null, null, null)).thenReturn(errorResp);
//		when(mockDelegate.createTextMessage(errorResp)).thenReturn(mockResp);
//		
//		
//		classUnderTest.run();	
//	}
//	
//	/*
//	 * This tests the Sparql Handler error handling
//	 */
//	@Test
//	public void testInvalidOutput() throws Exception{
//		when(mockDelegate.createJasperResponse(JasperConstants.ResponseCodes.BADREQUEST, "Invalid SPARQL query received", null, null, null)).thenReturn(errorResp);
//		when(mockDelegate.createTextMessage(errorResp)).thenReturn(mockResp);
//		when(mockRequest.getText()).thenReturn(msgText2);
//		
//		
//		classUnderTest.run();	
//	}
//	
//	/*
//	 * This tests the Sparql Handler processing a valid query
//	 */
//	@Test
//	public void testValidQuery() throws Exception{
//		when(mockDelegate.createJasperResponse(JasperConstants.ResponseCodes.OK, "Success", null, "application/json", "1.0")).thenReturn(validResp);
//		when(mockDelegate.createTextMessage(validResp)).thenReturn(mockResp);
//		when(mockRequest.getText()).thenReturn(msgText);
//		
//		
//		classUnderTest.run();	
//	}
//	
//	/*
//	 * This tests the Sparql Handler processing a valid query
//	 */
//	@Test
//	public void testValidQueryNoCorrID() throws Exception{
//		when(mockDelegate.createJasperResponse(JasperConstants.ResponseCodes.OK, "Success", null, "application/json", "1.0")).thenReturn(validResp);
//		when(mockDelegate.createTextMessage(validResp)).thenReturn(mockResp);
//		when(mockRequest.getText()).thenReturn(msgText);
//		when(mockRequest.getJMSMessageID()).thenReturn(corrID);
//		when(mockRequest.getJMSCorrelationID()).thenReturn(null);
//		
//		
//		classUnderTest.run();	
//	}
//	
//
////	@Before
////	public void setUp() throws Exception {
////		MockitoAnnotations.initMocks(this);
////		System.setProperty("delegate-property-file", "../zipRoot/jsb-core/config/delegate.properties");
////		when(mockRequest.getJMSCorrelationID()).thenReturn(corrID);
////		when(mockRequest.getJMSReplyTo()).thenReturn(mockDest);
////		when(mockUDE.getUdeDeploymentAndInstance()).thenReturn(deploymentAndInstance);
////		 
////		classUnderTest = new SparqlHandler(mockDelegate, mockOntology, mockRequest); 
////	}
//
//	@After
//	public void tearDown() throws Exception {
//		classUnderTest = null;
//		}
	
}
