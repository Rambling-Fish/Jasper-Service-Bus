package org.jasper.jsc;

import java.util.Properties;

import javax.jms.JMSException;
import javax.jms.TextMessage;

import junit.framework.TestCase;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.when;

import com.google.gson.JsonObject;

public class TestJSC {

	@Mock private TextMessage mockTxtMsg;
	private static Properties props = new Properties();
	private static String ruri = "http://coralcea.ca/jasper/msData";
	private static JsonObject jsonObj;
	private static Jsc classUnderTest;
	
	
	@Test
	public void testGet() throws JMSException {
		System.out.println("=================");
		System.out.println("RUNNING JSC TESTS");
		System.out.println("=================");
		jsonObj = new JsonObject();
		Request req = new Request(Method.POST, ruri, jsonObj);
		Response resp = classUnderTest.get(req);
		TestCase.assertEquals(400, resp.getCode());
		req.setMethod(Method.GET);
		req.setRuri("");
		resp = classUnderTest.get(req);
		TestCase.assertEquals(400, resp.getCode());
		
		req.setRuri(ruri);
		resp = classUnderTest.get(req);
		TestCase.assertNull(resp);
	}
	
	
	@Test
	public void testPost() throws JMSException {
		jsonObj = new JsonObject();
		Request req = new Request(Method.GET, ruri, jsonObj);
		Response resp = classUnderTest.post(req);
		TestCase.assertEquals(400, resp.getCode());
		
		req.setMethod(Method.POST);
		resp = classUnderTest.post(req);
	}
	
	@Test
	public void testListeners() throws JMSException {
		jsonObj = new JsonObject();
		class TestListener implements Listener{

			public void processMessage(Response response) {
				
			}
			
			public TestListener(){
				
			}
			
		}
		
		Listener myListener = new TestListener();
		boolean result;
		Request req = new Request(Method.GET, ruri, jsonObj);
		result = classUnderTest.registerListener(myListener, req);
		TestCase.assertEquals(true, result);
		
		// register same listener
		result = classUnderTest.registerListener(myListener, req);
		TestCase.assertEquals(false, result);
		
		//deregister listener
		result = classUnderTest.deregisterListener(myListener);
		TestCase.assertEquals(true, result);
	}
	
	@Test
	public void testOnMessage() throws Exception{
		MockitoAnnotations.initMocks(this);
		byte[] payload = new byte[]{(byte)0xe0, 0x4f, (byte)0xd0,0x20};
		when(mockTxtMsg.getJMSCorrelationID()).thenReturn("123");
		when(mockTxtMsg.getText()).thenReturn("{\"code\":200,\"reason\":\"OK\",\"description\":\"Success\",\"version\":\"1.0\",\"headers\":{\"content-type\":\"application/json\"},\"payload\":[123,10,32,32,34,104,101,97]}");
		mockTxtMsg.setText("test");
		classUnderTest.onMessageForAsyncResponse(mockTxtMsg);
		
		when(mockTxtMsg.getJMSCorrelationID()).thenReturn(null);
		classUnderTest.onMessageForAsyncResponse(mockTxtMsg);
		classUnderTest.onMessageForSyncResponse(mockTxtMsg);
		
		when(mockTxtMsg.getJMSCorrelationID()).thenReturn("123");
		classUnderTest.onMessageForSyncResponse(mockTxtMsg);
		
	}
	
	@Test
	public void testMisc() throws Exception{
		Properties newProps = new Properties();
		newProps.setProperty("jsc.username", "testUser");
		newProps.setProperty("jsc.password", "testPasswd");
		newProps.setProperty("jsc.timeout", "2xe");
		newProps.setProperty("jsc.transport", "vm://localhost?broker.persistent=false");
		Jsc jsc = new Jsc(newProps);
		jsc.init();
		jsc.destroy();
	}
	
	@BeforeClass
	public static void setUpClass() throws Exception {
		props.setProperty("jsc.username", "testUser");
		props.setProperty("jsc.password", "testPasswd");
		props.setProperty("jsc.timeout", "2");
		props.setProperty("jsc.poll-period", "200h");
		props.setProperty("jsc.transport", "vm://localhost?broker.persistent=false");
		classUnderTest = new Jsc(props);
		classUnderTest.init();
	}
	
	@AfterClass
	public static void tearDownClass() throws Exception {
		classUnderTest.destroy();
		classUnderTest = null;
	}

}
