package org.jasper.jsc.webApp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jms.JMSException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.jasper.jsc.Jsc;
import org.jasper.jsc.Listener;
import org.jasper.jsc.Method;
import org.jasper.jsc.Request;
import org.jasper.jsc.Response;
import org.jasper.jsc.constants.RequestHeaders;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@WebServlet("/")
public class JscServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	static Logger log = Logger.getLogger(JscServlet.class.getName());
	
	private Jsc jsc;
	private Gson gson;

	private LocalStatistics stats;
	
	Listener callNurseListener = null;
	Listener cancelCallNurseListener = null;
	Listener emergencyListener = null;
	Listener cancelEmergencyListener = null;
	Listener tempUpdateListener = null;
	Listener doorUpdateListener = null;

	private Map<String,Object> locks;
	
	long ts1sendRest2NcDta = 0;
	long ts2recvPubfromUde = 0;
	long ts3sendPosttoUde = 0;
	long ts4recv200okfromUde = 0;
	long tsTotal = 0;

	boolean receivedSmsPost200ok = false;
	
	private class LocalStatistics{
		private boolean isConnected;
		private AtomicInteger numberOfRequests;
		
		public LocalStatistics(boolean isConnected) {
			super();
			this.isConnected = isConnected;
			numberOfRequests = new AtomicInteger();
		}
		public AtomicInteger getNumberOfRequests() {
			return numberOfRequests;
		}
		public void setNumberOfRequests(AtomicInteger numberOfRequests) {
			this.numberOfRequests = numberOfRequests;
		}
		public boolean isConnected() {
			return isConnected;
		}
		public void setConnected(boolean isConnected) {
			this.isConnected = isConnected;
		}
		public void incrementNumRequest(){
			numberOfRequests.incrementAndGet();
		}
	}
	
	public JscServlet(){
		super();
		gson = new Gson();
		stats = new LocalStatistics(false);
    }
    
	public void init(){
		jsc = new Jsc(getProperties());
		locks = new ConcurrentHashMap<String, Object>();
		try {
			jsc.init();
			setupCallNurseSubscription();
			setupCancelCallNurseSubscription();
			setupEmergencySubscription();
			setupCancelEmergencySubscription();
			setupBldgMgmtTempUpdateSubscription();
			setupBldgMgmtDoorUpdateSubscription();
		} 
		catch (JMSException e) {
			log.error("Exception occurred during initialization " + e);
		}
		stats.setConnected(true);
    }
	
    private void setupCallNurseSubscription() {
    	
    	Map<String, String> headers = new HashMap<String, String>();
    	headers.put(RequestHeaders.RESPONSE_TYPE, "application/json");
		Request request = new Request(Method.SUBSCRIBE, "http://coralcea.ca/jasper/NurseCall/callNurse", headers);
		callNurseListener = new Listener() {
			
			public void processMessage(Response response) {
				/*
				 * increment count
				 * send back SMS POST with location
				 */
				recordT2();
				
				byte[] bytearray = response.getPayload();
				String decoded = null;
				
				try {
					decoded = new String(response.getPayload(), "UTF-8");
				} 
				catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				
		    	String logId = parseLocation(decoded);

		    	// send 
		    	Map<String, String> headers = new HashMap<String, String>();
		    	Map<String, String> parameters = new HashMap<String, String>();
		    	headers.put(RequestHeaders.RESPONSE_TYPE, "application/json");
		    	parameters.put("http://coralcea.ca/jasper/Sms/toSms", "to");
		    	parameters.put("http://coralcea.ca/jasper/Sms/fromSms", "from");
		    	parameters.put("http://coralcea.ca/jasper/Sms/bodySms", "callNurse");
		    	parameters.put("http://coralcea.ca/jasper/Sms/logId", logId);

		    	Request request = new Request(Method.POST, "http://coralcea.ca/jasper/Sms/SmsPostReq", headers, parameters);

		    	recordT3();
		    	Response sms_response = jsc.post(request);
		    	recordT4();
		    	removePostResponseLock(logId);
		    	
				stats.incrementNumRequest();
			}
		};

		jsc.registerListener(callNurseListener , request);
	}

    private void setupCancelCallNurseSubscription() {
    	
    	Map<String, String> headers = new HashMap<String, String>();
    	headers.put(RequestHeaders.RESPONSE_TYPE, "application/json");
		Request request = new Request(Method.SUBSCRIBE, "http://coralcea.ca/jasper/NurseCall/cancelCallNurse", headers);
		cancelCallNurseListener = new Listener() {
			
			public void processMessage(Response response) {
				/*
				 * increment count
				 * send back SMS POST with location
				 */
				recordT2();
								
				byte[] bytearray = response.getPayload();
				String decoded = null;
				
				try {
					decoded = new String(response.getPayload(), "UTF-8");
				} 
				catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				
		    	String logId = parseLocation(decoded);

		    	// send 
		    	Map<String, String> headers = new HashMap<String, String>();
		    	Map<String, String> parameters = new HashMap<String, String>();
		    	headers.put(RequestHeaders.RESPONSE_TYPE, "application/json");
		    	parameters.put("http://coralcea.ca/jasper/Sms/toSms", "to");
		    	parameters.put("http://coralcea.ca/jasper/Sms/fromSms", "from");
		    	parameters.put("http://coralcea.ca/jasper/Sms/bodySms", "cancelCallNurse");
		    	parameters.put("http://coralcea.ca/jasper/Sms/logId", logId);

		    	Request request = new Request(Method.POST, "http://coralcea.ca/jasper/Sms/SmsPostReq", headers, parameters);

		    	recordT3();
		    	Response sms_response = jsc.post(request);
		    	recordT4();
		    	removePostResponseLock(logId);
				
				stats.incrementNumRequest();
			}
		};
		
		jsc.registerListener(cancelCallNurseListener , request);
	}

    private void setupEmergencySubscription() {
    	
    	Map<String, String> headers = new HashMap<String, String>();
    	headers.put(RequestHeaders.RESPONSE_TYPE, "application/json");
		Request request = new Request(Method.SUBSCRIBE, "http://coralcea.ca/jasper/NurseCall/emergency", headers);
		emergencyListener = new Listener() {
			
			public void processMessage(Response response) {
				/*
				 * increment count
				 * send back SMS POST with location
				 */
				recordT2();
								
				byte[] bytearray = response.getPayload();
				String decoded = null;
				
				try {
					decoded = new String(response.getPayload(), "UTF-8");
				}
				catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				
		    	String logId = parseLocation(decoded);

		    	// send 
		    	Map<String, String> headers = new HashMap<String, String>();
		    	Map<String, String> parameters = new HashMap<String, String>();
		    	headers.put(RequestHeaders.RESPONSE_TYPE, "application/json");
		    	parameters.put("http://coralcea.ca/jasper/Sms/toSms", "to");
		    	parameters.put("http://coralcea.ca/jasper/Sms/fromSms", "from");
		    	parameters.put("http://coralcea.ca/jasper/Sms/bodySms", "emergency");
		    	parameters.put("http://coralcea.ca/jasper/Sms/logId", logId);

		    	Request request = new Request(Method.POST, "http://coralcea.ca/jasper/Sms/SmsPostReq", headers, parameters);
				recordT3();
		    	Response sms_response = jsc.post(request);
		    	recordT4();
		    	removePostResponseLock(logId);
				
				stats.incrementNumRequest();
			}
		};

		jsc.registerListener(emergencyListener , request);
	}

    private void setupCancelEmergencySubscription() {
    	
    	Map<String, String> headers = new HashMap<String, String>();
    	headers.put(RequestHeaders.RESPONSE_TYPE, "application/json");
		Request request = new Request(Method.SUBSCRIBE, "http://coralcea.ca/jasper/NurseCall/cancelEmergency", headers);
		cancelEmergencyListener = new Listener() {
			
			public void processMessage(Response response) {
				/*
				 * increment count
				 * send back SMS POST with location
				 */
				recordT2();
				
				byte[] bytearray = response.getPayload();
				String decoded = null;
				
				try {
					decoded = new String(response.getPayload(), "UTF-8");
				}
				catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				
		    	String logId = parseLocation(decoded);

		    	// send 
		    	Map<String, String> headers = new HashMap<String, String>();
		    	Map<String, String> parameters = new HashMap<String, String>();
		    	headers.put(RequestHeaders.RESPONSE_TYPE, "application/json");
		    	parameters.put("http://coralcea.ca/jasper/Sms/toSms", "to");
		    	parameters.put("http://coralcea.ca/jasper/Sms/fromSms", "from");
		    	parameters.put("http://coralcea.ca/jasper/Sms/bodySms", "cancelEmergency");
		    	parameters.put("http://coralcea.ca/jasper/Sms/logId", logId);

		    	Request request = new Request(Method.POST, "http://coralcea.ca/jasper/Sms/SmsPostReq", headers, parameters);
		    	recordT3();
		    	Response sms_response = jsc.post(request);
		    	recordT4();
		    	removePostResponseLock(logId);
		    	
				stats.incrementNumRequest();
			}
		};
		
		jsc.registerListener(cancelEmergencyListener , request);
	}

    private String parseLocation(String decoded)
    {
    	JsonParser parser = new JsonParser();
    	JsonObject jObj = (JsonObject)parser.parse(decoded);

		JsonElement location = jObj.get("http://coralcea.ca/jasper/NurseCall/location");

		return location.getAsString();
    }

    private void setupBldgMgmtTempUpdateSubscription() {
    	
    	Map<String, String> headers = new HashMap<String, String>();
    	headers.put(RequestHeaders.RESPONSE_TYPE, "application/json");
		Request request = new Request(Method.SUBSCRIBE, "http://coralcea.ca/jasper/BuildingMgmt/roomTempUpdate", headers);
		tempUpdateListener = new Listener() {
			
			public void processMessage(Response response) {
				/*
				 * increment count
				 * send back SMS POST with roomId
				 */
				recordT2();
				
				byte[] bytearray = response.getPayload();
				String decoded = null;
				
				try {
					decoded = new String(response.getPayload(), "UTF-8");
				}
				catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				
		    	String logId = parseRoomId(decoded);

		    	// send 
		    	Map<String, String> headers = new HashMap<String, String>();
		    	Map<String, String> parameters = new HashMap<String, String>();
		    	headers.put(RequestHeaders.RESPONSE_TYPE, "application/json");
		    	parameters.put("http://coralcea.ca/jasper/Sms/toSms", "to");
		    	parameters.put("http://coralcea.ca/jasper/Sms/fromSms", "from");
		    	parameters.put("http://coralcea.ca/jasper/Sms/bodySms", "roomTempUpdate");
		    	parameters.put("http://coralcea.ca/jasper/Sms/logId", logId);

		    	Request request = new Request(Method.POST, "http://coralcea.ca/jasper/Sms/SmsPostReq", headers, parameters);
		    	recordT3();
		    	Response sms_response = jsc.post(request);
		    	recordT4();
		    	removePostResponseLock(logId);
		    	
				stats.incrementNumRequest();
			}
		};
		jsc.registerListener(tempUpdateListener , request);
    	
	}

    private String parseRoomId(String decoded)
    {
    	JsonParser parser = new JsonParser();
    	JsonObject jObj = (JsonObject)parser.parse(decoded);

		JsonElement roomId = jObj.get("http://coralcea.ca/jasper/BuildingMgmt/roomID");

		return roomId.getAsString();
    }

    private void setupBldgMgmtDoorUpdateSubscription() {
    	
    	Map<String, String> headers = new HashMap<String, String>();
    	headers.put(RequestHeaders.RESPONSE_TYPE, "application/json");
		Request request = new Request(Method.SUBSCRIBE, "http://coralcea.ca/jasper/BuildingMgmt/doorStateChange", headers);
		doorUpdateListener = new Listener() {
			
			public void processMessage(Response response) {
				/*
				 * increment count
				 * send back SMS POST with doorId
				 */
				recordT2();
		    	
				byte[] bytearray = response.getPayload();
				String decoded = null;
				
				try {
					decoded = new String(response.getPayload(), "UTF-8");
				}
				catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				
		    	String logId = parseDoorId(decoded);

		    	// send 
		    	Map<String, String> headers = new HashMap<String, String>();
		    	Map<String, String> parameters = new HashMap<String, String>();
		    	headers.put(RequestHeaders.RESPONSE_TYPE, "application/json");
		    	parameters.put("http://coralcea.ca/jasper/Sms/toSms", "to");
		    	parameters.put("http://coralcea.ca/jasper/Sms/fromSms", "from");
		    	parameters.put("http://coralcea.ca/jasper/Sms/bodySms", "doorStateChange");
		    	parameters.put("http://coralcea.ca/jasper/Sms/logId", logId);

		    	Request request = new Request(Method.POST, "http://coralcea.ca/jasper/Sms/SmsPostReq", headers, parameters);
		    	recordT3();
		    	Response sms_response = jsc.post(request);
		    	recordT4();
		    	removePostResponseLock(logId);
		    	
				stats.incrementNumRequest();
			}
		};
		jsc.registerListener(doorUpdateListener , request);
    	
	}

    private String parseDoorId(String decoded)
    {
    	JsonParser parser = new JsonParser();
    	JsonObject jObj = (JsonObject)parser.parse(decoded);

		JsonElement doorId = jObj.get("http://coralcea.ca/jasper/BuildingMgmt/doorID");

		return doorId.getAsString();
    }

    private void unSubscribeCallNurse() {
    	jsc.deregisterListener(callNurseListener);
	}

    private void unSubscribeCancelCallNurse() {
    	jsc.deregisterListener(cancelCallNurseListener);
	}

    private void unSubscribeEmergency() {
    	jsc.deregisterListener(emergencyListener);
	}

    private void unSubscribeCancelEmergency() {
    	jsc.deregisterListener(cancelEmergencyListener);
	}

    private void unSubscribeRoomTempUpdate() {
    	jsc.deregisterListener(tempUpdateListener);
	}

    private void unSubscribeDoorStateChange() {
    	jsc.deregisterListener(doorUpdateListener);
	}

	public void destroy() {
		if(log.isInfoEnabled()) log.info("jsc-hrv-tw-testhead destroy");
		
    	unSubscribeCallNurse();
    	unSubscribeCancelCallNurse();
    	unSubscribeEmergency();
    	unSubscribeCancelEmergency();
    	unSubscribeRoomTempUpdate();
    	unSubscribeDoorStateChange();

    	jsc.destroy();

    	stats.setConnected(false);
    }
    
	private Properties getProperties() {
		if(log.isInfoEnabled()) log.info("loading jsc-hrv-tw-testhead properties...");
    	Properties prop = new Properties();
		try {
			File file = new File(System.getProperty("catalina.base") + "/conf/jsc-hrv-tw-testhead.properties");
			if(file.exists()){
				FileInputStream fileInputStream = new FileInputStream(file);
				if(log.isInfoEnabled()) log.info("loading properties file from catalina.base/conf");
				prop.load(fileInputStream);
			}else{
				InputStream input = getServletContext().getResourceAsStream("/WEB-INF/conf/jsc-hrv-tw-testhead.properties");
				if(log.isInfoEnabled()) log.info("loading properties file from WEB-INF/conf");
				prop.load(input);
			}
			
		} catch (IOException e) {
			log.error("error loading jsc-hrv-tw-testhead properties file.", e);
		}
		return prop;
	}
    
    private void recordT1()
    {
		ts1sendRest2NcDta = System.currentTimeMillis();
		log.info("JSC_HRV: TIMECHECK send REST HTTP NC/BM ........ t1[" + ts1sendRest2NcDta + "]");
    }
    
    private void recordT2()
    {
    	ts2recvPubfromUde = System.currentTimeMillis();
    	log.info("JSC_HRV: TIMECHECK async publish received ...... t2[" + ts2recvPubfromUde + "] " + (ts2recvPubfromUde - ts1sendRest2NcDta));
    }
    
    private void recordT3()
    {
    	ts3sendPosttoUde = System.currentTimeMillis();
    	log.info("JSC_HRV: TIMECHECK sms post request sent ....... t3[" + ts3sendPosttoUde + "] " + (ts3sendPosttoUde - ts1sendRest2NcDta));
    }
    
    private void recordT4()
    {
    	ts4recv200okfromUde = System.currentTimeMillis();
    	tsTotal = ts4recv200okfromUde - ts1sendRest2NcDta;
    	
    	if (tsTotal > 50)
    		log.info("JSC_HRV: TIMECHECK sms post 200ok received ..... t4[" + ts4recv200okfromUde + "] --- " + tsTotal);
    	else
    		log.info("JSC_HRV: TIMECHECK sms post 200ok received ..... t4[" + ts4recv200okfromUde + "] " + tsTotal);
    }
    
    private void removePostResponseLock(String logId)
    {
		if(locks.containsKey(logId)){
			Object lock = locks.remove(logId);
			synchronized (lock) {
				lock.notifyAll();
			}
		}
		else {
			log.error("lock id not found : " + logId );
		}
    }
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response)  throws ServletException, IOException{

		// send HTTP rest call to NurseCall/BuildingManagement DTA
   	
		String urlString= null;
		String idString = null;
		
		String requestPath = request.getPathInfo();
		
		if (requestPath.equalsIgnoreCase("/callNurse"))
		{
			idString = new String("cn-" + stats.getNumberOfRequests());
			urlString = new String("http://127.0.0.1:8082/rnc/callNurse?http://coralcea.ca/jasper/NurseCall/location=" + idString);
		}
		else if (requestPath.equalsIgnoreCase("/cancelCallNurse"))
		{	
			idString = new String("ccn-" + stats.getNumberOfRequests());
			urlString = new String("http://127.0.0.1:8082/rnc/cancelCallNurse?http://coralcea.ca/jasper/NurseCall/location=" + idString);
		}
		else if (requestPath.equalsIgnoreCase("/emergency"))
		{	
			idString = new String("e-" + stats.getNumberOfRequests());
			urlString = new String("http://127.0.0.1:8082/rnc/emergency?http://coralcea.ca/jasper/NurseCall/location=" + idString);
		}
		else if (requestPath.equalsIgnoreCase("/cancelEmergency"))
		{	
			idString = new String("ce-" + stats.getNumberOfRequests());
			urlString = new String("http://127.0.0.1:8082/rnc/cancelEmergency?http://coralcea.ca/jasper/NurseCall/location=" + idString);
		}
		else if (requestPath.equalsIgnoreCase("/roomTempUpdate"))
		{	
			idString = new String("rtu-" + stats.getNumberOfRequests());
			urlString = new String("http://127.0.0.1:8083/rbm/roomTempUpdate?http://coralcea.ca/jasper/BuildingMgmt/roomID=" + idString + "&http://coralcea.ca/jasper/BuildingMgmt/temperature=19");
		}
		else if (requestPath.equalsIgnoreCase("/doorStateChange"))
		{	
			idString = new String("dsc-" + stats.getNumberOfRequests());
			urlString = new String("http://127.0.0.1:8083/rbm/doorStateChange?http://coralcea.ca/jasper/BuildingMgmt/doorID=" + idString + "&http://coralcea.ca/jasper/BuildingMgmt/doorState=open");
		}
		else if (requestPath.equalsIgnoreCase("/stats"))
		{	
			response.setContentType("application/json");
			response.setCharacterEncoding("UTF-8");
			response.getWriter().write(gson.toJson(stats));
			return;
		}
		
		if (urlString == null)
		{
			log.error("invalid http path");
			response.setStatus(400);
			return;
		}
		
		URL url = null;
		try {
			url = new URL(urlString);
		} catch (MalformedURLException e1) {
			log.error("MalformedURLException on new url");
			e1.printStackTrace();
			response.setStatus(400);
			return;
		}

		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection)url.openConnection();
		} catch (IOException e1) {
			log.error("IOException on url.openConnection()");
			e1.printStackTrace();
			response.setStatus(500);
			return;
		}

		try {
			conn.setRequestMethod("GET");
		} catch (ProtocolException e1) {
			log.error("ProtocolException on conn.setRequestMethod");
			e1.printStackTrace();
			response.setStatus(500);
			return;
		}

		conn.setRequestProperty("User-Agent", "Mozilla/5.0");
		conn.setRequestProperty("Keep-Alive", "true");
		conn.setRequestProperty("Connection", "true");
		
		// record timestamp #1 just prior to sending the TEST HTTP request to the NC (or BM) DTA
		// and reset the 200ok flag  
		recordT1();
		receivedSmsPost200ok = false;
		
		// send the HTTP request
		try {
			conn.connect();
		} catch (IOException e1) {
			log.error("IOException on conn.connect.  Are the NC/BM DTAs deployed?");
			e1.printStackTrace();
			response.setStatus(500);
			return;
		}

		try {
			conn.getResponseCode();
		} catch (IOException e1) {
			e1.printStackTrace();
			log.error("IOException on conn.getResponseCode");
			response.setStatus(500);
			return;
		}

		// need to wait for sms post response before returning 200 OK to caller
		// Lock and wait for 200ok flag to be set (or timeout and return 504 timeout to caller)
		
		Object lock = new Object();
		synchronized (lock) {
			locks.put(idString, lock);
			int count = 0;
		    while (receivedSmsPost200ok == false) {
		    	try {
					lock.wait(10000);  // timeout is 10 sec
				} catch (InterruptedException e) {
					log.error("Interrupted while waiting for lock notification",e);
				}
		    	count++;
		    	if(count >= 1) break;
		    }
		}

		if (receivedSmsPost200ok == false)
		{
			response.setStatus(504);
			log.info("JSC_HRV: TIMECHECK timed out waiting for SMS POST 200 OK");
		}
		else
		{
			long ts200ok = System.currentTimeMillis();
			log.info("JSC_HRV: TIMECHECK 200 OK ...................... tE[" + ts200ok + "] " + (ts200ok - ts1sendRest2NcDta));
		}
			
		response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
    }

}