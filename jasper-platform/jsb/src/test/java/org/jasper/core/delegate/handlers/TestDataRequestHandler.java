package org.jasper.core.delegate.handlers;

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.jasper.core.constants.JasperConstants;
import org.jasper.core.constants.JasperConstants.ResponseCodes;
import org.jasper.core.delegate.Delegate;
import org.jasper.core.delegate.DelegateOntology;
import org.jasper.core.persistence.PersistedDataRequest;
import org.jasper.core.persistence.PersistedSubscriptionRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class TestDataRequestHandler{
	@Mock private Connection mockConnection;
	@Mock private Session mockSession;
	@Mock private DelegateOntology mockOntology;
	@Mock private Queue mockQueue;
	@Mock private Delegate mockDelegate;
	@Mock private MessageConsumer mockConsumer;
	@Mock private MessageProducer mockProducer;
	@Mock private PersistedDataRequest mockPersistReq;
	@Mock private PersistedSubscriptionRequest mockSubscriptionReq;
	@Mock private TextMessage mockTxtMsg;
	@Mock private Destination mockDest;
	private DataRequestHandler classUnderTest;
	private String ruri = "http://coralcea.ca/jasper/hrData";
	private String publishRuri = "http://coralcea.ca/jasper/NurseCall/callNurse";

	private String getRequest = "{\"version\":\"1.0\",\"method\":\"GET\",\"ruri\":\"http://coralcea.ca/jasper/hrData\",\"headers\":{\"content-type\":\"application/json\",\"response-type\":\"application/ld+json\","
			+"\"processing-scheme\":\"aggregate\",\"poll-period\":1,\"expires\":6},\"parameters\":{\"http://coralcea.ca/jasper/hrSID\":\"01\"},"
			+ "\"rule\":\"compareint(http://coralcea.ca/jasper/hrSensor/bpm,gt,90)\"}";
	private String getRequestDefaults = "{\"version\":\"1.0\",\"method\":\"GET\",\"ruri\":\"http://coralcea.ca/jasper/hrData\",\"headers\":{\"content-type\":\"application/json\"}"
			+",\"parameters\":{\"http://coralcea.ca/jasper/hrSID\":\"01\"}}";
	private String subscribeReq = "{\"version\":\"1.0\",\"method\":\"SUBSCRIBE\",\"ruri\":\"http://coralcea.ca/jasper/NurseCall/callNurse\",\"headers\":{\"response-type\":\"application/ld+json\",\"subscription-id\":\"3d69f7b6-ef9c-45c7-b9f8-ee938aff2648\"}}";
	private String subscribeWithRuleReq = "{\"version\":\"1.0\",\"method\":\"SUBSCRIBE\",\"ruri\":\"http://coralcea.ca/jasper/NurseCall/callNurse\","
			+ "\"headers\":{\"subscription-id\":\"3d69f7b6-ef9c-45c7-b9f8-ee938aff2648\"},\"rule\":\"compareint(http://coralcea.ca/jasper/hrSensor/bpm,gt,90)\"}";
	private String unSubscribeReq = "{\"version\":\"1.0\",\"method\":\"SUBSCRIBE\",\"ruri\":\"http://coralcea.ca/jasper/NurseCall/callNurse\",\"headers\":{\"response-type\":\"application/ld+json\",\"expires\":0,\"subscription-id\":\"3d69f7b6-ef9c-45c7-b9f8-ee938aff2648\"}}";
	private String subscribeWithNoID = "{\"version\":\"1.0\",\"method\":\"SUBSCRIBE\",\"ruri\":\"http://coralcea.ca/jasper/NurseCall/callNurse\",\"headers\":{\"response-type\":\"application/ld+json\"}}";
	private String badMethodReq = "{\"version\":\"1.0\",\"method\":\"INVALID\",\"ruri\":\"http://coralcea.ca/jasper/NurseCall/callNurse\",\"headers\":{\"response-type\":\"application/ld+json\",\"expires\":0,\"subscription-id\":\"3d69f7b6-ef9c-45c7-b9f8-ee938aff2648\"}}";
	private String publishReq = "{\"version\":\"1.0\",\"method\":\"PUBLISH\",\"ruri\":\"http://coralcea.ca/jasper/NurseCall/callNurse\",\"parameters\":{\"http://coralcea.ca/jasper/hrSID\":\"01\"}}";
	private String postReq = "{\"version\":\"1.0\",\"method\":\"POST\",\"ruri\":\"http://coralcea.ca/jasper/RoomTempUpdateReq\",\"parameters\":{\"@type\":\"roomTempData\",\"http://coralcea.ca/jasper/roomId\":1,\"http://coralcea.ca/jasper/roomTemperature\":44.7,\"http://coralcea.ca/jasper/timestamp\":\"2014.01.02.11.59.976EDT\"}}";

	
	@Test
	public void testGETRequests() throws Exception{
		System.out.println("==================================");
		System.out.println("RUNNING DATA REQUEST HANDLER TESTS");
		System.out.println("==================================");
		when(mockPersistReq.getRequest()).thenReturn(getRequest);
		when(mockDelegate.getJOntology()).thenReturn(mockOntology);
		when(mockOntology.isRuriKnownForOutputGet(ruri)).thenReturn(true);
		when(mockDelegate.createJasperResponse(JasperConstants.ResponseCodes.NOTFOUND, "http://coralcea.ca/jasper/hrData is known, but we could not get a valid response before request expired",
				null, "application/json", JasperConstants.VERSION_1_0)).thenReturn("error");
		when(mockDelegate.createTextMessage("error")).thenReturn(mockTxtMsg);
		mockDelegate.maxExpiry = 5;
		mockDelegate.maxPollingInterval = 5;
		mockDelegate.minPollingInterval = 2;
		
		classUnderTest = new DataRequestHandler(mockDelegate, mockPersistReq);
		when(mockDelegate.createJasperResponse(JasperConstants.ResponseCodes.NOTFOUND, "http://coralcea.ca/jasper/hrData is known, but we could not get a valid response, check parameters",
				null, "application/json", JasperConstants.VERSION_1_0)).thenReturn("error");
		classUnderTest.run();
		
		when(mockPersistReq.getRequest()).thenReturn(getRequestDefaults);
		classUnderTest.run();
		
		when(mockOntology.isRuriKnownForOutputGet(ruri)).thenReturn(false);
		when(mockDelegate.createJasperResponse(JasperConstants.ResponseCodes.NOTFOUND, "http://coralcea.ca/jasper/hrData is not known", null, "application/json", JasperConstants.VERSION_1_0)).thenReturn("error");

		classUnderTest.run();
	}
	
	@Test
	public void testSUBSCRIBERequests() throws Exception{
		when(mockPersistReq.getRequest()).thenReturn(subscribeReq);
		when(mockDelegate.getJOntology()).thenReturn(mockOntology);
		when(mockOntology.isRuriKnownForInputPublish(publishRuri)).thenReturn(true);
		
		classUnderTest = new DataRequestHandler(mockDelegate, mockPersistReq);
		classUnderTest.run();
		
		when(mockOntology.isRuriKnownForInputPublish(publishRuri)).thenReturn(false);
		when(mockDelegate.createJasperResponse(JasperConstants.ResponseCodes.NOTFOUND, "ruri http://coralcea.ca/jasper/NurseCall/callNurse is not PUBLISHED by any DTA, cannot SUBSCRIBE to unpublished data", null, "application/json", JasperConstants.VERSION_1_0)).thenReturn("error");
		classUnderTest.run();
		
		//Unsubscribe
		when(mockOntology.isRuriKnownForInputPublish(publishRuri)).thenReturn(true);
		when(mockPersistReq.getRequest()).thenReturn(unSubscribeReq);
		classUnderTest.run();
		
		// subscribe with a rule
		when(mockPersistReq.getRequest()).thenReturn(subscribeWithRuleReq);
		when(mockDelegate.getJOntology()).thenReturn(mockOntology);
		when(mockOntology.isRuriKnownForInputPublish(publishRuri)).thenReturn(true);
		classUnderTest.run();
		
		// invalid method
		when(mockPersistReq.getRequest()).thenReturn(badMethodReq);
		when(mockDelegate.createJasperResponse(JasperConstants.ResponseCodes.BADREQUEST, "unsupported method type INVALID", null, "application/json", JasperConstants.VERSION_1_0)).thenReturn("error");
		classUnderTest.run();
		
		// no subscriptionID
		when(mockPersistReq.getRequest()).thenReturn(subscribeWithNoID);
		when(mockDelegate.getJOntology()).thenReturn(mockOntology);
		when(mockOntology.isRuriKnownForInputPublish(publishRuri)).thenReturn(true);
		when(mockDelegate.createJasperResponse(JasperConstants.ResponseCodes.BADREQUEST, "susbcription request does not contain subcription ID", null, "application/json", JasperConstants.VERSION_1_0)).thenReturn("error");
		classUnderTest.run();
	}
	
	@Test
	public void testPOSTRequests() throws Exception{
		JsonParser parser = new JsonParser();
		String ruri = "http://coralcea.ca/jasper/RoomTempUpdateReq";
		String operation = "http://coralcea.ca/jasper/updateRoomTemp";
		String schemaStr = "{\"id\":\"http://coralcea.ca/jasper/RoomTempUpdateReq\",\"type\":\"object\",\"properties\":{\"http://coralcea.ca/jasper/roomId\":{\"type\":\"http://www.w3.org/2001/XMLSchema#string\"},\"http://coralcea.ca/jasper/roomTemperature\":{\"type\":\"http://www.w3.org/2001/XMLSchema#integer\"},\"http://coralcea.ca/jasper/timestamp\":{\"type\":\"http://www.w3.org/2001/XMLSchema#string\"}},\"required\":[\"http://coralcea.ca/jasper/roomId\",\"http://coralcea.ca/jasper/roomTemperature\"]}";
		JsonElement jsonElem = parser.parse(schemaStr);
		JsonObject jsonObj = jsonElem.getAsJsonObject();
		ArrayList<String> ops = new ArrayList<String>();
		ops.add(operation);
		when(mockOntology.isRuriKnownForInputPost(ruri)).thenReturn(true);
		when(mockPersistReq.getRequest()).thenReturn(postReq);
		when(mockDelegate.getJOntology()).thenReturn(mockOntology);
		when(mockOntology.fetchPostOperations(ruri)).thenReturn(ops);
		when(mockOntology.fetchPostOperationInputObject(operation)).thenReturn(ruri);
		when(mockOntology.createJsonSchema(ruri)).thenReturn(jsonObj);
		when(mockDelegate.createJasperResponse(JasperConstants.ResponseCodes.NOTFOUND, "http://coralcea.ca/jasper/RoomTempUpdateReq POST did not return 200 OK from DTAs, request failed", null, "application/json", JasperConstants.VERSION_1_0)).thenReturn("error");
		
		classUnderTest = new DataRequestHandler(mockDelegate, mockPersistReq);
		classUnderTest.run();
		
		when(mockOntology.isRuriKnownForInputPost(ruri)).thenReturn(false);
		when(mockDelegate.createJasperResponse(JasperConstants.ResponseCodes.NOTFOUND, "http://coralcea.ca/jasper/RoomTempUpdateReq is not known for POST", null, "application/json", JasperConstants.VERSION_1_0)).thenReturn("error");
		classUnderTest.run();
	}
	
	@Test
	public void testPUBLISHRequests() throws Exception{
		Set<String> URIs = new HashSet<String>();
		long time = 10000000;
		String ldResponse = "{\"@context\":{\"hrSID\":\"http://coralcea.ca/jasper/hrSID\"},\"hrSID\":\"01\"}";
		String response = "{\"http://coralcea.ca/jasper/hrSID\":\"01\"}";
		Set<PersistedSubscriptionRequest> subs = new HashSet<PersistedSubscriptionRequest>();
		subs.add(mockSubscriptionReq);
		String ruri = "http://coralcea.ca/jasper/NurseCall/callNurse";
		URIs.add("http://coralcea.ca/jasper/msData");
		when(mockPersistReq.getRequest()).thenReturn(publishReq);
		when(mockDelegate.getJOntology()).thenReturn(mockOntology);
		when(mockDelegate.getDataSubscriptions(ruri)).thenReturn(subs);
		when(mockOntology.isRuriKnownForInputPublish(publishRuri)).thenReturn(true);
		when(mockOntology.getSuperProperties(ruri)).thenReturn(URIs);
		when(mockSubscriptionReq.getExpiry()).thenReturn(1);
		when(mockSubscriptionReq.getTimestampMillis()).thenReturn(System.currentTimeMillis() + time);
		when(mockSubscriptionReq.getTriggerList()).thenReturn(null);
		when(mockSubscriptionReq.getResponseType()).thenReturn("application/ld+json");
		when(mockDelegate.createJasperResponse(JasperConstants.ResponseCodes.OK, "Success", ldResponse, "application/json", JasperConstants.VERSION_1_0)).thenReturn("success");
		when(mockDelegate.createTextMessage("success")).thenReturn(mockTxtMsg);
		
		classUnderTest = new DataRequestHandler(mockDelegate, mockPersistReq);
		classUnderTest.run();
		
		when(mockSubscriptionReq.getResponseType()).thenReturn("application/json");
		when(mockDelegate.createJasperResponse(JasperConstants.ResponseCodes.OK, "Success", response, "application/json", JasperConstants.VERSION_1_0)).thenReturn("success");
		classUnderTest.run();
		
		when(mockSubscriptionReq.getTimestampMillis()).thenReturn(System.currentTimeMillis() - time);
		classUnderTest.run();
	}
	
	@Before
	public void setUp() throws Exception{
		MockitoAnnotations.initMocks(this);
		when(mockPersistReq.getCorrelationID()).thenReturn("123");
		when(mockPersistReq.getReply2Q()).thenReturn(mockDest);
		when(mockPersistReq.getTimestampMillis()).thenReturn(System.currentTimeMillis());
		when(mockConnection.createSession(true,Session.SESSION_TRANSACTED)).thenReturn(mockSession);
		when(mockSession.createQueue(JasperConstants.DELEGATE_GLOBAL_QUEUE)).thenReturn(mockQueue);
		when(mockSession.createConsumer(mockQueue)).thenReturn(mockConsumer);
		when(mockConnection.createSession(false, Session.AUTO_ACKNOWLEDGE)).thenReturn(mockSession);
		when(mockSession.createProducer(null)).thenReturn(mockProducer);
		when(mockSession.createConsumer(mockQueue)).thenReturn(mockConsumer);
		when(mockDelegate.createTextMessage("error")).thenReturn(mockTxtMsg);
	}
	
	@After
	public void tearDown(){
		classUnderTest = null;
	}
}
