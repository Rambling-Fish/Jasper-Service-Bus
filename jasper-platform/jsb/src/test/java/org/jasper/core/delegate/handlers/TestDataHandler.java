package org.jasper.core.delegate.handlers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.InetAddress;

import javax.jms.Destination;
import javax.jms.TextMessage;

import junit.framework.TestCase;

import org.jasper.core.UDE;
import org.jasper.core.constants.JasperConstants;
import org.jasper.core.delegate.Delegate;
import org.jasper.core.persistence.PersistenceFacade;
import org.junit.After;
import org.junit.Before;
//
import org.junit.Test;
//
public class TestDataHandler extends TestCase {
	
	private DataHandler classUnderTest;
	private UDE mockUDE;
	private PersistenceFacade cachingSys;
	private Delegate mockDelegate;
	private TextMessage mockRequest;
	private TextMessage mockResp;
	private Destination mockDest;
	private String ipAddr;
	private String corrID = "1234";
	private String errorResp = "{400: Bad Request, msg : Invalid request received - request is null or empty string, version : 2.1}";
	private String errorTxt = "Invalid request received - request is null or empty string";
	private String errorTxt2 = "Invalid / Malformed JSON object received";
	private String errorTxt3 = "Invalid request received - request does not contain a URI";
	private String errorTxt4 = "Invalid request type: DELETE";
	private String contentType = "application/json";
	private String version = "1.0";
	private String deploymentAndInstance = "UDE:0";
	private String hrDataReqeuest = "{\"version\":\"1.0\",\"method\":\"GET\",\"ruri\":\"http://coralcea.ca/jasper/hrData\",\"headers\":{\"content-type\":\"application/json\"},\"parameters\":{\"http://coralcea.ca/jasper/hrSID\":\"1\"}}";
    private String hrDataNotification = "{\"version\":\"1.0\",\"method\":\"GET\",\"ruri\":\"http://coralcea.ca/jasper/hrData\",\"headers\":{\"content-type\":\"application/json\",\"expires\":\"10\"},\"parameters\":{},\"rule\":\"compareint(http://coralcea.ca/jasper/medicalSensor/heartRate/data/bpm,gt,90)\"}";
	private String noRURIReqeuest = "{\"version\":\"1.0\",\"method\":\"GET\",\"headers\":{\"content-type\":\"application/json\"},\"parameters\":{\"http://coralcea.ca/jasper/hrSID\":\"1\"}}";
	private String nullRURIReqeuest = "{\"version\":\"1.0\",\"method\":\"GET\",\"ruri\":\"\",\"headers\":{\"content-type\":\"application/json\"},\"parameters\":{\"http://coralcea.ca/jasper/hrSID\":\"1\"}}";
	private String unsupportedMethodReqeuest = "{\"version\":\"1.0\",\"method\":\"DELETE\",\"ruri\":\"http://coralcea.ca/jasper/hrData\",\"headers\":{\"content-type\":\"application/json\"},\"parameters\":{\"http://coralcea.ca/jasper/hrSID\":\"1\"}}";

	
	/*
	 * This tests the Data Handler error handling when receiving
	 * a null request
	 */
	@Test
	public void testNullRequest() throws Exception{
		System.out.println("==========================");
		System.out.println("RUNNING DATA HANDLER TESTS");
		System.out.println("==========================");
		when(mockDelegate.createJasperResponse(JasperConstants.responseCodes.BADREQUEST, errorTxt, null, null, null)).thenReturn(errorResp);
		when(mockDelegate.createTextMessage(errorResp)).thenReturn(mockResp);
		
		classUnderTest.run();	
	}
	
	/*
	 * This tests the Data Handler receiving a valid request
	 */
	@Test
	public void testValidRequest() throws Exception{
		when(mockRequest.getText()).thenReturn(hrDataReqeuest);
		when(mockRequest.getJMSReplyTo()).thenReturn(null);

		classUnderTest.run();
	}
	
	/*
	 * This tests the Data Handler receiving a valid notification
	 */
	@Test
	public void testValidNotification() throws Exception{
		when(mockRequest.getText()).thenReturn(hrDataNotification);
		when(mockRequest.getJMSReplyTo()).thenReturn(null);

		classUnderTest.run();	
	}
	
	/*
	 * This tests the Data Handler receiving a request without an RURI parameter
	 */
	@Test
	public void testNoRURI() throws Exception{
		when(mockRequest.getText()).thenReturn(noRURIReqeuest);
		when(mockRequest.getJMSReplyTo()).thenReturn(mockDest);
		when(mockDelegate.createJasperResponse(JasperConstants.responseCodes.BADREQUEST, errorTxt2, null, null, null)).thenReturn(errorResp);
		when(mockDelegate.createTextMessage(errorResp)).thenReturn(mockResp);

		classUnderTest.run();
	}
	
	/*
	 * This tests the Data Handler receiving a request with an empty RURI
	 */
	@Test
	public void testEmptyRURI() throws Exception{
		when(mockRequest.getText()).thenReturn(nullRURIReqeuest);
		when(mockRequest.getJMSReplyTo()).thenReturn(mockDest);
		when(mockRequest.getJMSCorrelationID()).thenReturn(null);
		when(mockRequest.getJMSMessageID()).thenReturn(corrID);
		when(mockDelegate.createJasperResponse(JasperConstants.responseCodes.BADREQUEST, errorTxt3, null, contentType, version)).thenReturn(errorResp);
		when(mockDelegate.createTextMessage(errorResp)).thenReturn(mockResp);

		classUnderTest.run();
	}
	
	/*
	 * This tests the Data Handler receiving a request with an 
	 * unsupported method
	 */
	@Test
	public void testUnsupportedMethod() throws Exception{
		when(mockRequest.getText()).thenReturn(unsupportedMethodReqeuest);
		when(mockRequest.getJMSReplyTo()).thenReturn(mockDest);
		when(mockRequest.getJMSCorrelationID()).thenReturn(null);
		when(mockRequest.getJMSMessageID()).thenReturn(corrID);
		when(mockDelegate.createJasperResponse(JasperConstants.responseCodes.BADREQUEST, errorTxt4, null, contentType, version)).thenReturn(errorResp);
		when(mockDelegate.createTextMessage(errorResp)).thenReturn(mockResp);

		classUnderTest.run();
	}

	@Before
	public void setUp() throws Exception {
		 System.setProperty("delegate-property-file", "../zipRoot/jsb-core/config/delegate.properties");
		 ipAddr = InetAddress.getLocalHost().getHostAddress();
		 mockDelegate = mock(Delegate.class);
		 mockUDE      = mock(UDE.class);
		 mockRequest  = mock(TextMessage.class);
		 mockResp     = mock(TextMessage.class);
		 mockDest     = mock(Destination.class);
		 cachingSys   = new PersistenceFacade(ipAddr, "testGroup", "testPassword");
		 when(mockRequest.getJMSCorrelationID()).thenReturn(corrID);
		 when(mockRequest.getJMSReplyTo()).thenReturn(mockDest);
		 when(mockUDE.getCachingSys()).thenReturn(cachingSys);
		 when(mockUDE.getUdeDeploymentAndInstance()).thenReturn(deploymentAndInstance);
		 
		 classUnderTest = new DataHandler(mockUDE, mockDelegate, mockRequest);
		 
	}

	@After
	public void tearDown() throws Exception {
		classUnderTest = null;
		cachingSys.shutdown();
		}
	
}
