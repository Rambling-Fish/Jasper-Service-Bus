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
import org.jasper.core.persistence.PersistenceFacade;
import org.junit.After;
import org.junit.Before;
//
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
//
public class TestDataHandler extends TestCase {
	
	private DataHandler classUnderTest;
	@Mock private UDE mockUDE;
	@Mock private Delegate mockDelegate;
	@Mock private TextMessage mockRequest;
	@Mock private TextMessage mockResp;
	@Mock private Destination mockDest;
	private PersistenceFacade cachingSys;
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
	private String hrDataRequest = "{\"version\":\"1.0\",\"method\":\"GET\",\"ruri\":\"http://coralcea.ca/jasper/hrData\",\"headers\":{\"content-type\":\"application/json\"},\"parameters\":{\"http://coralcea.ca/jasper/hrSID\":\"1\",\"http://coralcea.ca/jasper/msID\":\"001\"}}";
    private String hrDataNotification = "{\"version\":\"1.0\",\"method\":\"GET\",\"ruri\":\"http://coralcea.ca/jasper/hrData\",\"headers\":{\"content-type\":\"application/json\",\"expires\":\"10\",\"output\":\"json\"},\"parameters\":{},\"rule\":\"compareint(http://coralcea.ca/jasper/medicalSensor/heartRate/data/bpm,gt,90)\"}";
    private String hrDataNotificationNoExpires = "{\"version\":\"1.0\",\"method\":\"GET\",\"ruri\":\"http://coralcea.ca/jasper/hrData\",\"headers\":{\"content-type\":\"application/json\"},\"parameters\":{},\"rule\":\"compareint(http://coralcea.ca/jasper/medicalSensor/heartRate/data/bpm,gt,90)\"}";
    private String hrDataNotificationMinPolling = "{\"version\":\"1.0\",\"method\":\"GET\",\"ruri\":\"http://coralcea.ca/jasper/hrData\",\"headers\":{\"content-type\":\"application/json\",\"expires\":\"10\",\"poll-period\":\"1\"},\"parameters\":{},\"rule\":\"compareint(http://coralcea.ca/jasper/medicalSensor/heartRate/data/bpm,gt,90)\"}";
    private String hrDataNotificationMaxPolling = "{\"version\":\"1.0\",\"method\":\"GET\",\"ruri\":\"http://coralcea.ca/jasper/hrData\",\"headers\":{\"content-type\":\"application/json\",\"expires\":\"5\",\"poll-period\":\"15\"},\"parameters\":{},\"rule\":\"compareint(http://coralcea.ca/jasper/medicalSensor/heartRate/data/bpm,gt,90)\"}";
    private String hrDataNotificationBadInts = "{\"version\":\"1.0\",\"method\":\"GET\",\"ruri\":\"http://coralcea.ca/jasper/hrData\",\"headers\":{\"content-type\":\"application/json\",\"expires\":\"1x\",\"output\":\"json\",\"poll-period\":\"Y\"},\"parameters\":{},\"rule\":\"compareint(http://coralcea.ca/jasper/medicalSensor/heartRate/data/bpm,gt,90)\"}";
    private String hrDataNotificationInvalidTrigger = "{\"version\":\"1.0\",\"method\":\"GET\",\"ruri\":\"http://coralcea.ca/jasper/hrData\",\"headers\":{\"content-type\":\"application/json\",\"expires\":\"10\",\"output\":\"json\"},\"parameters\":{},\"rule\":\"comparelong(http://coralcea.ca/jasper/medicalSensor/heartRate/data/bpm,gt,90)\"}";
    private String noRURIReqeuest = "{\"version\":\"1.0\",\"method\":\"GET\",\"headers\":{\"content-type\":\"application/json\"},\"parameters\":{\"http://coralcea.ca/jasper/hrSID\":\"1\"}}";
	private String nullRURIReqeuest = "{\"version\":\"1.0\",\"method\":\"GET\",\"ruri\":\"\",\"headers\":{\"content-type\":\"application/json\"},\"parameters\":{\"http://coralcea.ca/jasper/hrSID\":\"1\"}}";
	private String unsupportedMethodReqeuest = "{\"version\":\"1.0\",\"method\":\"DELETE\",\"ruri\":\"http://coralcea.ca/jasper/hrData\",\"headers\":{\"content-type\":\"application/json\"},\"parameters\":{\"http://coralcea.ca/jasper/hrSID\":\"1\"}}";
	private String hazelcastGroup = UUID.randomUUID().toString();
	
	/*
	 * placeholder until we delete this class since DataHandler
	 * is no longer used
	 */
	@Test
	public void testNothing() throws Exception{
		
	}
	
	/*
	 * This tests the Data Handler error handling when receiving
	 * a null request (i.e. no text in text message)
	 */
//	@Test
//	public void testNullRequest() throws Exception{
//		System.out.println("==========================");
//		System.out.println("RUNNING DATA HANDLER TESTS");
//		System.out.println("==========================");
//		when(mockDelegate.createJasperResponse(JasperConstants.ResponseCodes.BADREQUEST, errorTxt, null, null, null)).thenReturn(errorResp);
//		when(mockDelegate.createTextMessage(errorResp)).thenReturn(mockResp);
//		
//		classUnderTest.run();	
//	}
	
	/*
	 * This tests the Data Handler receiving a valid request
	 */
//	@Test
//	public void testValidRequest() throws Exception{
//		when(mockRequest.getText()).thenReturn(hrDataRequest);
//		when(mockRequest.getJMSReplyTo()).thenReturn(null);
//
//		classUnderTest.run();
//	}
	
	/*
	 * This tests the Data Handler receiving a valid notification
	 */
//	@Test
//	public void testValidNotification() throws Exception{
//		when(mockRequest.getText()).thenReturn(hrDataNotification);
//		when(mockRequest.getJMSReplyTo()).thenReturn(null);
//		mockDelegate.defaultOutput = "json";
//
//		classUnderTest.run();	
//	}
	
	/*
	 * This tests the Data Handler receiving a valid notification
	 * but with no expires parameter
	 */
//	@Test
//	public void testNotificationNoExpires() throws Exception{
//		when(mockRequest.getText()).thenReturn(hrDataNotificationNoExpires);
//		when(mockRequest.getJMSReplyTo()).thenReturn(null);
//		mockDelegate.defaultOutput = "json";
//		mockDelegate.maxExpiry = 60000;
//		mockDelegate.maxPollingInterval = 6000;
//		mockDelegate.minPollingInterval = 2000;
//
//		classUnderTest.run();	
//	}
	
	/*
	 * This tests the Data Handler receiving a valid notification
	 * but with a poll period that is less than the minimum
	 */
//	@Test
//	public void testNotificationMinPolling() throws Exception{
//		when(mockRequest.getText()).thenReturn(hrDataNotificationMinPolling);
//		when(mockRequest.getJMSReplyTo()).thenReturn(null);
//		mockDelegate.defaultOutput = "json";
//		mockDelegate.maxExpiry = 60000;
//		mockDelegate.maxPollingInterval = 6000;
//		mockDelegate.minPollingInterval = 2000;
//
//		classUnderTest.run();	
//	}
	
	/*
	 * This tests the Data Handler receiving a valid notification
	 * but with a poll period that is greater than maximum
	 */
//	@Test
//	public void testNotificationMaxPolling() throws Exception{
//		when(mockRequest.getText()).thenReturn(hrDataNotificationMaxPolling);
//		when(mockRequest.getJMSReplyTo()).thenReturn(null);
//		mockDelegate.defaultOutput = "json";
//		mockDelegate.maxExpiry = 60000;
//		mockDelegate.maxPollingInterval = 6000;
//		mockDelegate.minPollingInterval = 2000;
//
//		classUnderTest.run();	
//	}
	
	/*
	 * This tests the Data Handler receiving a valid notification
	 * but with non numbers in the poll period and expires parameters
	 */
//	@Test
//	public void testNotificationBadInts() throws Exception{
//		when(mockRequest.getText()).thenReturn(hrDataNotificationBadInts);
//		when(mockRequest.getJMSReplyTo()).thenReturn(null);
//		mockDelegate.defaultOutput = "json";
//		mockDelegate.maxExpiry = 60000;
//		mockDelegate.maxPollingInterval = 6000;
//		mockDelegate.minPollingInterval = 2000;
//
//		classUnderTest.run();	
//	}
	
	/*
	 * This tests the Data Handler receiving an invalid notification
	 * with an unknown trigger name
	 */
//	@Test
//	public void testNotificationInvalidTrigger() throws Exception{
//		when(mockRequest.getText()).thenReturn(hrDataNotificationInvalidTrigger);
//		when(mockRequest.getJMSReplyTo()).thenReturn(null);
//		mockDelegate.defaultOutput = "json";
//		mockDelegate.maxExpiry = 60000;
//		mockDelegate.maxPollingInterval = 6000;
//		mockDelegate.minPollingInterval = 2000;
//
//		classUnderTest.run();	
//	}
	
	/*
	 * This tests the Data Handler receiving a request without an RURI parameter
	 */
//	@Test
//	public void testNoRURI() throws Exception{
//		when(mockRequest.getText()).thenReturn(noRURIReqeuest);
//		when(mockRequest.getJMSReplyTo()).thenReturn(mockDest);
//		when(mockDelegate.createJasperResponse(JasperConstants.ResponseCodes.BADREQUEST, errorTxt2, null, null, null)).thenReturn(errorResp);
//		when(mockDelegate.createTextMessage(errorResp)).thenReturn(mockResp);
//
//		classUnderTest.run();
//	}
	
	/*
	 * This tests the Data Handler receiving a request with an empty RURI
	 */
//	@Test
//	public void testEmptyRURI() throws Exception{
//		when(mockRequest.getText()).thenReturn(nullRURIReqeuest);
//		when(mockRequest.getJMSReplyTo()).thenReturn(mockDest);
//		when(mockRequest.getJMSCorrelationID()).thenReturn(null);
//		when(mockRequest.getJMSMessageID()).thenReturn(corrID);
//		when(mockDelegate.createJasperResponse(JasperConstants.ResponseCodes.BADREQUEST, errorTxt3, null, contentType, version)).thenReturn(errorResp);
//		when(mockDelegate.createTextMessage(errorResp)).thenReturn(mockResp);
//
//		classUnderTest.run();
//	}
	
	/*
	 * This tests the Data Handler receiving a request with an 
	 * unsupported method
	 */
//	@Test
//	public void testUnsupportedMethod() throws Exception{
//		when(mockRequest.getText()).thenReturn(unsupportedMethodReqeuest);
//		when(mockRequest.getJMSReplyTo()).thenReturn(mockDest);
//		when(mockRequest.getJMSCorrelationID()).thenReturn(null);
//		when(mockRequest.getJMSMessageID()).thenReturn(corrID);
//		when(mockDelegate.createJasperResponse(JasperConstants.ResponseCodes.BADREQUEST, errorTxt4, null, contentType, version)).thenReturn(errorResp);
//		when(mockDelegate.createTextMessage(errorResp)).thenReturn(mockResp);
//
//		classUnderTest.run();
//	}

//	@Before
//	public void setUp() throws Exception {
//		MockitoAnnotations.initMocks(this);
//		System.setProperty("delegate-property-file", "../zipRoot/jsb-core/config/delegate.properties");
//		ipAddr = InetAddress.getLocalHost().getHostAddress();
//		cachingSys   = new PersistenceFacadeImpl(ipAddr, hazelcastGroup, "testPassword");
//		when(mockRequest.getJMSCorrelationID()).thenReturn(corrID);
//		when(mockRequest.getJMSReplyTo()).thenReturn(mockDest);
//		when(mockUDE.getCachingSys()).thenReturn(cachingSys);
//		when(mockUDE.getUdeDeploymentAndInstance()).thenReturn(deploymentAndInstance);
//		 
//		classUnderTest = new DataHandler(mockUDE, mockDelegate, mockRequest); 
//	}

//	@After
//	public void tearDown() throws Exception {
//		classUnderTest = null;
//		cachingSys.shutdown();
//		}
	
}
