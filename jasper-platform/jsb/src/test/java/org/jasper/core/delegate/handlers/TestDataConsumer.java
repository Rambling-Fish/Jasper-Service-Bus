package org.jasper.core.delegate.handlers;

import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.TextMessage;

import junit.framework.TestCase;

import org.apache.jena.atlas.json.JsonArray;
import org.jasper.core.UDE;
import org.jasper.core.constants.JasperConstants;
import org.jasper.core.constants.JasperOntologyConstants;
import org.jasper.core.delegate.Delegate;
import org.jasper.core.delegate.DelegateOntology;
import org.jasper.core.persistence.PersistenceFacade;
import org.junit.After;
import org.junit.Before;
//
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
//
public class TestDataConsumer extends TestCase {
	@Mock private UDE mockUDE;
	@Mock private Delegate mockDelegate;
	@Mock private TextMessage mockRequest;
	@Mock private TextMessage mockResp;
	@Mock private Destination mockDest;
	private DataConsumer classUnderTest;
	private DataHandler dataHandler;
	private PersistenceFacade cachingSys;
	private Model model;
	private DelegateOntology jOntology;
	private String ipAddr;
	private String ruri = "http://coralcea.ca/jasper/hrData";
	private String corrID = "1234";
	private String errorResp = "{408: Request timeout, msg : Notification has expired, version : 1.0}";
	private String errorTxt = "http://coralcea.ca/jasper/hrData is not supported";
	private String errorTxt2 = "compareint(http://coralcea.ca/jasper/medicalSensor/heartRate/data/bpm,gt,90) has expired";
	private String contentType = "application/json";
	private String version = "1.0";
	private String deploymentAndInstance = "UDE:0";
	private String hrDataReqeuest = "{\"version\":\"1.0\",\"method\":\"GET\",\"ruri\":\"http://coralcea.ca/jasper/hrData\",\"headers\":{\"content-type\":\"application/json\"},\"parameters\":{\"http://coralcea.ca/jasper/hrSID\":\"1\"}}";
    private String hrDataNotification = "{\"version\":\"1.0\",\"method\":\"GET\",\"ruri\":\"http://coralcea.ca/jasper/hrData\",\"headers\":{\"content-type\":\"application/json\",\"expires\":\"10\"},\"parameters\":{},\"rule\":\"compareint(http://coralcea.ca/jasper/medicalSensor/heartRate/data/bpm,gt,90)\"}";
	private Map<String,Object> locks = new ConcurrentHashMap<String,Object>();
	private Map<String,Message> responses = new ConcurrentHashMap<String,Message>();
	private JsonArray jsonArray = new JsonArray();
	private Map<String,String> paramsMap = new HashMap<String,String>();
	private String hazelcastGroup = UUID.randomUUID().toString();
	
	/*
	 * This tests the Data Consumer receiving a valid request
	 * but no DTA is registered to return data
	 */
	@Test
	public void testValidRequestNoData() throws Exception{
		System.out.println("===========================");
		System.out.println("RUNNING DATA CONSUMER TESTS");
		System.out.println("===========================");
		when(mockRequest.getText()).thenReturn(hrDataReqeuest);
		when(mockRequest.getJMSReplyTo()).thenReturn(null);
		when(mockDelegate.createJasperResponse(JasperConstants.responseCodes.NOTFOUND, errorTxt, null, contentType, version)).thenReturn(errorResp);
		when(mockDelegate.createTextMessage(errorResp)).thenReturn(mockResp);

		dataHandler.run();
		// **** IMPORTANT DataConsumer.shutdown() must be called first on all test
		// cases otherwise the class will wait on the distribute queue forever and
		// the TC will never finish.  Calling shutdown() before run() allows one
		// iteration before cleanup
		classUnderTest.shutdown();
		classUnderTest.run();
		
	}
	
	/*
	 * This tests the Data Consumer receiving a valid request
	 * and DTA returns data
//	 */
//	@Test
//	public void testValidRequestWithData() throws Exception{
//		JsonObject jObj = new JsonObject();
//		jObj.put("http://coralcea.ca/jasper/hrSID", "1");
//		String jObjStr = jObj.toString();
//		String[][] triples = createTriples();
//		loadOntologyModel(triples);
//		when(mockRequest.getText()).thenReturn(hrDataReqeuest);
//		when(mockRequest.getJMSReplyTo()).thenReturn(null);
//		when(mockDelegate.createJasperResponse(JasperConstants.responseCodes.NOTFOUND, errorTxt, null, contentType, version)).thenReturn(errorResp);
//		when(mockDelegate.createTextMessage(jObjStr)).thenReturn(mockRequest);
//
//		dataHandler.run();
//		classUnderTest.shutdown();
//		classUnderTest.run();
//		
//	}
	
	/*
	 * This tests the Data Consumer receiving a valid notification
	 */
	@Test
	public void testNotificationTimeout() throws Exception{
		mockDelegate.maxExpiry = 60000;
		mockDelegate.maxPollingInterval = 5000;
		mockDelegate.defaultOutput = "json";
		when(mockRequest.getText()).thenReturn(hrDataNotification);
		when(mockRequest.getJMSReplyTo()).thenReturn(null);
		when(mockDelegate.createJasperResponse(JasperConstants.responseCodes.TIMEOUT, errorTxt2, null, contentType, version)).thenReturn(errorResp);
		when(mockDelegate.createTextMessage(errorResp)).thenReturn(mockResp);

		dataHandler.run();
		classUnderTest.shutdown();
		classUnderTest.run();
	}
	
//	private String[][] createTriples(){
//		ArrayList<String[]> triples = new ArrayList<String[]>();
//		triples.add(new String[]{"http://coralcea.ca/jasper/jtaD", "http://coralcea.ca/jasper/vocabulary/queue", "jms.jasper.demo-heart-rate-monitor-D.1.0.jasperLab.0.queue"});
//		triples.add(new String[]{"http://coralcea.ca/jasper/jtaD", "http://coralcea.ca/jasper/vocabulary/provides", "http://coralcea.ca/jasper/hrData"});
//		triples.add(new String[]{"http://coralcea.ca/jasper/jtaD", "http://coralcea.ca/jasper/vocabulary/param", "http://coralcea.ca/jasper/hrSID"});
//		triples.add(new String[]{"http://coralcea.ca/jasper/jtaD", "http://coralcea.ca/jasper/vocabulary/is", "http://coralcea.ca/jasper/vocabulary/jta"});
//		triples.add(new String[]{"http://coralcea.ca/jasper/hrData", "http://coralcea.ca/jasper/vocabulary/subClassOf", "http://coralcea.ca/jasper/msData"});
//		triples.add(new String[]{"http://coralcea.ca/jasper/hrData", "http://coralcea.ca/jasper/vocabulary/has", "http://coralcea.ca/jasper/hrDataBpm"});
//		triples.add(new String[]{"http://coralcea.ca/jasper/hrData", "http://coralcea.ca/jasper/vocabulary/has", "http://coralcea.ca/jasper/vocabulary/timeStamp"});
//		
//		return triples.toArray(new String[][]{});
//	}
//	
//	private void loadOntologyModel(String[][] triples){
//		String jtaId = "jasper:demo-heart-rate-monitor-D:1.0:testLab";
//		for(String[] triple:triples){
//			jOntology.add(jtaId, triple);
//		}
//	}

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		ipAddr        = InetAddress.getLocalHost().getHostAddress();
		model = ModelFactory.createDefaultModel();
		for(String prefix:JasperOntologyConstants.PREFIX_MAP.keySet()){
			model.setNsPrefix(prefix, JasperOntologyConstants.PREFIX_MAP.get(prefix));
		}
		cachingSys    = new PersistenceFacade(ipAddr, hazelcastGroup, "testPassword");
		jOntology = new DelegateOntology(cachingSys, model);
		when(mockRequest.getJMSCorrelationID()).thenReturn(corrID);
		when(mockRequest.getJMSReplyTo()).thenReturn(mockDest);
		when(mockUDE.getCachingSys()).thenReturn(cachingSys);
		when(mockUDE.getUdeDeploymentAndInstance()).thenReturn(deploymentAndInstance);
		 
		classUnderTest = new DataConsumer(mockUDE, mockDelegate, jOntology, locks, responses);
		dataHandler    = new DataHandler(mockUDE, mockDelegate, mockRequest); 
	}

	@After
	public void tearDown() throws Exception {
		classUnderTest.shutdown();
		cachingSys.shutdown();
		classUnderTest = null;
        dataHandler = null;
		}
	
}
