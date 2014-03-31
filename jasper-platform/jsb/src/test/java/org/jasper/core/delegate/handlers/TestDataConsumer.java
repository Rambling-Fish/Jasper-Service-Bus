package org.jasper.core.delegate.handlers;

import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

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
import org.jasper.core.notification.triggers.CompareInt;
import org.jasper.core.notification.triggers.Trigger;
import org.jasper.core.notification.triggers.TriggerFactory;
import org.jasper.core.persistence.PersistedObject;
import org.jasper.core.persistence.PersistenceFacade;
import org.junit.After;
import org.junit.Before;
//
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
//
public class TestDataConsumer extends TestCase {
	@Mock private UDE mockUDE;
	@Mock private Delegate mockDelegate;
	@Mock private TextMessage mockRequest;
	@Mock private TextMessage mockResp;
	@Mock private Destination mockDest;
	@Mock private DelegateOntology mockOntology;
	private DataConsumer classUnderTest;
	private PersistenceFacade cachingSys;
	private OntModel model;
	private String ipAddr;
	private String ruri = "http://coralcea.ca/jasper/hrData";
	private String corrID = "1234";
	private String errorResp = "{408: Request timeout, msg : Notification has expired, version : 1.0}";
	private String errorTxt = "http://coralcea.ca/jasper/hrData is not supported";
	private String errorTxt2 = "compareint(http://coralcea.ca/jasper/medicalSensor/heartRate/data/bpm,gt,90) has expired";
	private String contentType = "application/json";
	private String version = "1.0";
	private String output = "json";
	private String deploymentAndInstance = "UDE:0";
	private String hrDataRequest = "{\"version\":\"1.0\",\"method\":\"GET\",\"ruri\":\"http://coralcea.ca/jasper/hrData\",\"headers\":{\"content-type\":\"application/json\"},\"parameters\":{\"http://coralcea.ca/jasper/hrSID\":\"1\"}}";
	private String hrDataRequest2 = "{\"version\":\"1.0\",\"method\":\"GET\",\"ruri\":\"http://coralcea.ca/jasper/hrData\",\"headers\":{\"content-type\":\"application/json\",\"processing-scheme\":\"aggregate\"},\"parameters\":{\"http://coralcea.ca/jasper/hrSID\":\"1\"}}";
	private String hrDataNotification = "{\"version\":\"1.0\",\"method\":\"GET\",\"ruri\":\"http://coralcea.ca/jasper/hrData\",\"headers\":{\"content-type\":\"application/json\",\"expires\":\"10\"},\"parameters\":{},\"rule\":\"compareint(http://coralcea.ca/jasper/medicalSensor/heartRate/data/bpm,gt,90)\"}";
	private String hrSubscribeRequest = "{\"version\":\"1.0\",\"method\":\"SUBSCRIBE\",\"ruri\":\"http://coralcea.ca/jasper/hrData\",\"headers\":{\"content-type\":\"application/json\",\"subscription-id\":\"1234\"},\"parameters\":{\"http://coralcea.ca/jasper/hrSID\":\"1\"}}";
	private String hrPostRequest = "{\"version\":\"1.0\",\"method\":\"POST\",\"ruri\":\"http://coralcea.ca/jasper/hrData\",\"headers\":{\"content-type\":\"application/json\"},\"parameters\":{\"@type\":\"roomTempData\", \"http://coralcea.ca/jasper/roomId\":\"1\"}}";
	private String hrPostArray = "{\"version\":\"1.0\",\"method\":\"POST\",\"ruri\":\"http://coralcea.ca/jasper/hrData\",\"headers\":{\"content-type\":\"application/json\"},\"parameters\":{\"parmsArray\":[{\"@type\":\"roomTempData\",\"http://coralcea.ca/jasper/environmentalSensor/roomTemperature\":\"33.5\",\"http://coralcea.ca/jasper/roomId\":\"2\",\"@type\":\"roomTempData\",\"http://coralcea.ca/jasper/environmentalSensor/roomTemperature\":\"98.6\",\"http://coralcea.ca/jasper/roomId\":\"1\"}]}";
	private Map<String,Object> locks = new ConcurrentHashMap<String,Object>();
	private Map<String,Message> responses = new ConcurrentHashMap<String,Message>();
	private JsonArray jsonArray = new JsonArray();
	private Map<String,String> paramsMap = new HashMap<String,String>();
	private String hazelcastGroup = UUID.randomUUID().toString();
	private BlockingQueue<PersistedObject> workQueue;
	private Map<String,PersistedObject> sharedData;
	private PersistedObject pObj;
	private List<Trigger> triggers;
	private ArrayList<String> operations = new ArrayList<String>();
//	private JsonElement jelem;
	private String hrDataInputObject = "{\"@type\":\"http://coralcea.ca/jasper/HrDataReq\",\"http://coralcea.ca/jasper/hrSID\":\"1\"}";
	private String schemaStr = "{\"id\":\"http://coralcea.ca/jasper/HrDataReq\",\"type\":\"object\",\"properties\":{\"http://coralcea.ca/jasper/hrSID\":{\"type\":\"http://www.w3.org/2001/XMLSchema#string\"}},\"required\":[\"http://coralcea.ca/jasper/hrSID\"]}";
	private JsonParser jsonParser = new JsonParser();
	private JsonElement schema;
	
	/*
	 * This tests the Data Consumer receiving a valid 
	 * request but no DTA is registered to return data
	 */
	@Test
	public void testValidRequest() throws Exception{
		System.out.println("===========================");
		System.out.println("RUNNING DATA CONSUMER TESTS");
		System.out.println("===========================");
		operations.add("http://coralcea.ca/jasper/getHrData");
		schema = jsonParser.parse(schemaStr);
		when(mockRequest.getText()).thenReturn(hrDataInputObject);
		when(mockRequest.getJMSReplyTo()).thenReturn(null);
		when(mockDelegate.createJasperResponse(JasperConstants.responseCodes.NOTFOUND, errorTxt, null, contentType, version)).thenReturn(errorResp);
		when(mockDelegate.createTextMessage(errorResp)).thenReturn(mockResp);
		when(mockOntology.getProvideOperations(ruri)).thenReturn(operations);
		when(mockOntology.getProvideOperationInputObject(operations.get(0))).thenReturn(hrDataInputObject);
		when(mockOntology.createJsonSchema(hrDataInputObject)).thenReturn((JsonObject) schema);
		when(mockDelegate.createTextMessage(hrDataInputObject)).thenReturn(mockRequest);
		
		pObj = new PersistedObject(corrID, corrID, hrDataRequest, ruri, null, null, false, "UDE:0", output, version, contentType, "GET", 20);
		sharedData.put(corrID, pObj);
		workQueue.offer(pObj);

		// **** IMPORTANT DataConsumer.shutdown() must be called first on all test
		// cases otherwise the class will wait on the distribute queue forever and
		// the TC will never finish.  Calling shutdown() before run() allows one
		// iteration before cleanup
		classUnderTest.shutdown();
		classUnderTest.run();
	}
	/*
	 * This tests the Data Consumer receiving valid 
	 * requests but no DTA is registered to return data
	 */
	@Test
	public void testValidRequests() throws Exception{
		when(mockOntology.isRuriKnownForOutputGet(ruri)).thenReturn(true);
		
		pObj = new PersistedObject(corrID, corrID, hrDataRequest, ruri, null, null, false, "UDE:0", output, version, contentType, "GET", 20);
		pObj.setCorrelationID(null);;
		sharedData.put(corrID, pObj);
		workQueue.offer(pObj);
		classUnderTest.shutdown();
		classUnderTest.run();
		
		pObj = new PersistedObject(corrID, corrID, hrDataRequest2, ruri, null, null, false, "UDE:0", output, version, contentType, "GET", 20);
		sharedData.put(corrID, pObj);
		workQueue.offer(pObj);
		classUnderTest.shutdown();
		classUnderTest.run();
		
	}
	
	/*
	 * This tests the Data Consumer receiving subscribe requests
	 */
	@Test
	public void testSubscribe() throws Exception{
		when(mockRequest.getText()).thenReturn(hrSubscribeRequest);
		when(mockRequest.getJMSReplyTo()).thenReturn(null);
		when(mockDelegate.createJasperResponse(JasperConstants.responseCodes.NOTFOUND, errorTxt, null, contentType, version)).thenReturn(errorResp);
		when(mockDelegate.createTextMessage(errorResp)).thenReturn(mockResp);
		
		pObj = new PersistedObject(corrID, corrID, hrSubscribeRequest, ruri, null, null, false, "UDE:0", output, version, contentType, "SUBSCRIBE", 20);
		pObj.setSubscriptionId(corrID);
		sharedData.put(corrID, pObj);
		workQueue.offer(pObj);

		// **** IMPORTANT DataConsumer.shutdown() must be called first on all test
		// cases otherwise the class will wait on the distribute queue forever and
		// the TC will never finish.  Calling shutdown() before run() allows one
		// iteration before cleanup
		classUnderTest.shutdown();
		classUnderTest.run();
		
		//Unsubscribe
		pObj = new PersistedObject(corrID, corrID, hrSubscribeRequest, ruri, null, null, false, "UDE:0", output, version, contentType, "SUBSCRIBE", 0);
		pObj.setSubscriptionId(corrID);
		sharedData.put(corrID, pObj);
		workQueue.offer(pObj);
		classUnderTest.shutdown();
		classUnderTest.run();
	}
	
	/*
	 * This tests the Data Consumer receiving POST requests
	 */
	@Test
	public void testPublish() throws Exception{
		when(mockRequest.getText()).thenReturn(hrSubscribeRequest);
		when(mockRequest.getJMSReplyTo()).thenReturn(null);
		when(mockDelegate.createJasperResponse(JasperConstants.responseCodes.NOTFOUND, errorTxt, null, contentType, version)).thenReturn(errorResp);
		when(mockDelegate.createTextMessage(errorResp)).thenReturn(mockResp);
		
		//subscribe first
		pObj = new PersistedObject(corrID, corrID, hrSubscribeRequest, ruri, null, null, false, "UDE:0", output, version, contentType, "SUBSCRIBE", 20);
		pObj.setSubscriptionId(corrID);
		sharedData.put(corrID, pObj);
		workQueue.offer(pObj);
		
		// **** IMPORTANT DataConsumer.shutdown() must be called first on all test
		// cases otherwise the class will wait on the distribute queue forever and
		// the TC will never finish.  Calling shutdown() before run() allows one
		// iteration before cleanup
		classUnderTest.shutdown();
		classUnderTest.run();
		
		// Now send POST
		pObj = new PersistedObject(corrID, corrID, hrPostRequest, ruri, null, null, false, "UDE:0", output, version, contentType, "PUBLISH", 20);
		sharedData.put(corrID, pObj);
		workQueue.offer(pObj);

		classUnderTest.shutdown();
		classUnderTest.run();
		
		pObj = new PersistedObject(corrID, corrID, hrPostArray, ruri, null, null, false, "UDE:0", output, version, contentType, "PUBLISH", 20);
		sharedData.put(corrID, pObj);
		workQueue.offer(pObj);

		classUnderTest.shutdown();
		classUnderTest.run();
		
	}
	
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
		
		TriggerFactory factory = new TriggerFactory();
		Trigger trigger = new CompareInt(6, 2, ruri, "ge", "20");
		trigger.setNotificationExpiry();
		triggers = new ArrayList<Trigger>();
		triggers.add(trigger);

		pObj = new PersistedObject(corrID, corrID, hrDataNotification, ruri, null, null, true, "UDE:0", output, version, contentType, "GET", 6);
		pObj.setTriggers(triggers);
		sharedData.put(corrID, pObj);
		workQueue.offer(pObj);
		classUnderTest.shutdown();
		classUnderTest.run();
	}

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		ipAddr        = InetAddress.getLocalHost().getHostAddress();
		model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
		
		for(String prefix:JasperOntologyConstants.PREFIX_MAP.keySet()){
			model.setNsPrefix(prefix, JasperOntologyConstants.PREFIX_MAP.get(prefix));
		}
		cachingSys    = new PersistenceFacade(ipAddr, hazelcastGroup, "testPassword");
		workQueue  = cachingSys.getQueue("tasks");
		sharedData = (Map<String, PersistedObject>) cachingSys.getMap("sharedData");
		when(mockRequest.getJMSCorrelationID()).thenReturn(corrID);
		when(mockRequest.getJMSReplyTo()).thenReturn(mockDest);
		when(mockUDE.getCachingSys()).thenReturn(cachingSys);
		when(mockUDE.getUdeDeploymentAndInstance()).thenReturn(deploymentAndInstance);
		when(mockOntology.isRuriKnownForOutputGet(ruri)).thenReturn(true);
		 
		classUnderTest = new DataConsumer(mockUDE, mockDelegate, mockOntology, locks, responses); 
	}

	@After
	public void tearDown() throws Exception {
		classUnderTest.shutdown();
		cachingSys.shutdown();
		classUnderTest = null;
	}
	
}
