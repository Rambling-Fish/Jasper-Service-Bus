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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

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
	private Map<String, Response> responses;
	
	long ts1sendRest2NcDta = 0;
	long ts2recvPubfromUde = 0;
	long ts3sendPosttoUde = 0;
	long ts4recvPostResponsefromUde = 0;
	long tsTotal = 0;
	
	String dtaServerIp = null;
	
	private class LocalStatistics{
		private boolean isConnected;
		private AtomicInteger numberOfRequests;
		private AtomicInteger numberOfResponses200ok;
		
		public LocalStatistics(boolean isConnected) {
			super();
			this.isConnected = isConnected;
			numberOfRequests = new AtomicInteger();
			numberOfResponses200ok = new AtomicInteger();
		}
		public int getNumberOfRequestsAndIncrement() {
			return numberOfRequests.getAndIncrement();
		}
		public void setNumberOfRequests(AtomicInteger numberOfRequests) {
			this.numberOfRequests = numberOfRequests;
		}
		public AtomicInteger getNumberOfResponses200ok() {
			return numberOfResponses200ok;
		}
		public void setNumberOfResponses200ok(AtomicInteger numberOfResponses200ok) {
			this.numberOfRequests = numberOfResponses200ok;
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
		public void incrementNumResponses200ok(){
			numberOfResponses200ok.incrementAndGet();
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
		responses = new ConcurrentHashMap<String, Response>();
		try {
			jsc.init();
			
			// example DTA Server IP entry in jsc-hrv-tw-testhead.properties for case where DTA Server is remote:   
			//
			// 1. remote DTA Server:
			// dta.serverip=192.168.1.115
			//
			// 2. Local DTA Server (in this scenario the entry can be omitted.  It will default to 0.0.0.0)
			// dta.serverip=0.0.0.0
			
			dtaServerIp = getProperties().getProperty("dta.serverip","0.0.0.0");

			callNurseListener        = setupSubscription("http://coralcea.ca/jasper/NurseCall/callNurse");
			cancelCallNurseListener  = setupSubscription("http://coralcea.ca/jasper/NurseCall/cancelCallNurse");
			emergencyListener        = setupSubscription("http://coralcea.ca/jasper/NurseCall/emergency");
			cancelEmergencyListener  = setupSubscription("http://coralcea.ca/jasper/NurseCall/cancelEmergency");
			tempUpdateListener       = setupSubscription("http://coralcea.ca/jasper/BuildingMgmt/roomTempUpdate");
			doorUpdateListener       = setupSubscription("http://coralcea.ca/jasper/BuildingMgmt/doorStateChange");

		} 
		catch (JMSException e) {
			log.error("Exception occurred during initialization " + e);
		}
		stats.setConnected(true);
    }
	
	 private Listener setupSubscription(String ruri) {
		 	    	
		 JsonObject headers = new JsonObject();
		 headers.add(RequestHeaders.RESPONSE_TYPE, new JsonPrimitive("application/ld+json"));
		 
		 Request request = new Request(Method.SUBSCRIBE, ruri, headers);
		 Listener listener = new Listener() {
				
				public void processMessage(Response response) {
					
					recordT2();

					String decoded = null;
					
					try {
						decoded = new String(response.getPayload(), "UTF-8");
					} 
					catch (UnsupportedEncodingException e) {
						log.error("UnsupportedEncodingException, unable to parse payload",e);
					}
			    	if(log.isDebugEnabled())log.debug("decoded string " + decoded);
			    	String logId = getLogId(decoded);
			    	if(log.isDebugEnabled())log.debug("logId string " + logId);


			    	// send 
			    	JsonObject headers = new JsonObject();
			    	JsonObject parameters = new JsonObject();
			    	headers.add(RequestHeaders.RESPONSE_TYPE, new JsonPrimitive("application/ld+json"));
			    	parameters.add("http://coralcea.ca/jasper/Sms/toSms", new JsonPrimitive("to"));
			    	parameters.add("http://coralcea.ca/jasper/Sms/fromSms", new JsonPrimitive("from"));
			    	parameters.add("http://coralcea.ca/jasper/Sms/bodySms", new JsonPrimitive("smsBodyText"));
			    	parameters.add("http://coralcea.ca/jasper/Sms/logId", new JsonPrimitive(logId));

			    	if(log.isDebugEnabled())log.debug("building Post Request for " + logId);
			    	Request request = new Request(Method.POST, "http://coralcea.ca/jasper/Sms/SmsPostReq", headers, parameters);
			    	if(log.isDebugEnabled())log.debug("Post Request built " + request);
			    	
			    	recordT3();
			    	if(log.isDebugEnabled())log.debug("about to send post request " + request);
			    	Response sms_response = jsc.post(request);
			    	recordT4();
			    	
					if (locks.containsKey(logId)) {
						responses.put(logId, sms_response);
						Object lock = locks.remove(logId);
						synchronized (lock) {
							lock.notifyAll();
						}
					} else {
						log.error("response with IdString = " + logId
								+ " received however no record of sending message with this ID, ignoring");
					}
				}
			};

			jsc.registerListener(listener , request);
			return listener;
		}

    private String getLogId(String decoded)
    {
    	JsonParser parser = new JsonParser();
    	JsonObject jObj = (JsonObject)parser.parse(decoded);

    	if(jObj.has("http://coralcea.ca/jasper/NurseCall/location")){
    		return jObj.get("http://coralcea.ca/jasper/NurseCall/location").getAsString();
    	}else if(jObj.has("http://coralcea.ca/jasper/BuildingMgmt/roomID")){
    		return jObj.get("http://coralcea.ca/jasper/BuildingMgmt/roomID").getAsString();
    	}else if(jObj.has("http://coralcea.ca/jasper/BuildingMgmt/doorID")){
    		return jObj.get("http://coralcea.ca/jasper/BuildingMgmt/doorID").getAsString();
    	}else{
    		return null;
    	}
    }

	public void destroy() {
		if(log.isInfoEnabled()) log.info("jsc-hrv-tw-testhead destroy");
		
    	jsc.deregisterListener(callNurseListener);
    	jsc.deregisterListener(cancelCallNurseListener);
    	jsc.deregisterListener(emergencyListener);
    	jsc.deregisterListener(cancelEmergencyListener);
    	jsc.deregisterListener(tempUpdateListener);
    	jsc.deregisterListener(doorUpdateListener);

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
    
    private void recordT1() {
    	if(!log.isInfoEnabled()) return;
		ts1sendRest2NcDta = System.currentTimeMillis();
		log.info("JSC_HRV: TIMECHECK send REST HTTP NC/BM ........ t1[" + ts1sendRest2NcDta + "]");
    }
    
    private void recordT2()
    {
    	if(!log.isInfoEnabled()) return;
    	ts2recvPubfromUde = System.currentTimeMillis();
    	log.info("JSC_HRV: TIMECHECK async publish received ...... t2[" + ts2recvPubfromUde + "] " + (ts2recvPubfromUde - ts1sendRest2NcDta));
    }
    
    private void recordT3()
    {
    	if(!log.isInfoEnabled()) return;
    	ts3sendPosttoUde = System.currentTimeMillis();
    	log.info("JSC_HRV: TIMECHECK sms post request sent ....... t3[" + ts3sendPosttoUde + "] " + (ts3sendPosttoUde - ts1sendRest2NcDta));
    }
    
    private void recordT4()
    {
    	if(!log.isInfoEnabled()) return;
    	ts4recvPostResponsefromUde = System.currentTimeMillis();
    	tsTotal = ts4recvPostResponsefromUde - ts1sendRest2NcDta;
    	
    	if (tsTotal > 50)
    		log.info("JSC_HRV: TIMECHECK sms post response received .. t4[" + ts4recvPostResponsefromUde + "] --- " + tsTotal);
    	else
    		log.info("JSC_HRV: TIMECHECK sms post response received .. t4[" + ts4recvPostResponsefromUde + "] " + tsTotal);
    }
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response)  throws ServletException, IOException{

    	Response sms_response = null;

		// send HTTP rest call to NurseCall/BuildingManagement DTA
		String urlString= null;
		int requestNum = stats.getNumberOfRequestsAndIncrement();
		String idString = null;
		
		String requestPath = request.getPathInfo();
		
		if (requestPath.equalsIgnoreCase("/callNurse"))
		{
			idString = new String("cn-" + requestNum);
			urlString = new String("http://" + dtaServerIp + ":8082/rnc/callNurse?http://coralcea.ca/jasper/NurseCall/location=" + idString);
		}
		else if (requestPath.equalsIgnoreCase("/cancelCallNurse"))
		{	
			idString = new String("ccn-" + requestNum);
			urlString = new String("http://" + dtaServerIp + ":8082/rnc/cancelCallNurse?http://coralcea.ca/jasper/NurseCall/location=" + idString);
		}
		else if (requestPath.equalsIgnoreCase("/emergency"))
		{	
			idString = new String("e-" + requestNum);
			urlString = new String("http://" + dtaServerIp + ":8082/rnc/emergency?http://coralcea.ca/jasper/NurseCall/location=" + idString);
		}
		else if (requestPath.equalsIgnoreCase("/cancelEmergency"))
		{	
			idString = new String("ce-" + requestNum);
			urlString = new String("http://" + dtaServerIp + ":8082/rnc/cancelEmergency?http://coralcea.ca/jasper/NurseCall/location=" + idString);
		}
		else if (requestPath.equalsIgnoreCase("/roomTempUpdate"))
		{	
			idString = new String("rtu-" + requestNum);
			urlString = new String("http://" + dtaServerIp + ":8083/rbm/roomTempUpdate?http://coralcea.ca/jasper/BuildingMgmt/roomID=" + idString + "&http://coralcea.ca/jasper/BuildingMgmt/temperature=19");
		}
		else if (requestPath.equalsIgnoreCase("/doorStateChange"))
		{	
			idString = new String("dsc-" + requestNum);
			urlString = new String("http://" + dtaServerIp + ":8083/rbm/doorStateChange?http://coralcea.ca/jasper/BuildingMgmt/doorID=" + idString + "&http://coralcea.ca/jasper/BuildingMgmt/doorState=open");
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
		if(log.isInfoEnabled())log.info("urlString= " + urlString);
		
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

		conn.setRequestProperty("Keep-Alive", "true");
		conn.setRequestProperty("Connection", "true");
		
		// record timestamp #1 just prior to sending the TEST HTTP request to the NC (or BM) DTA
		recordT1();
		
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
		// Lock and wait for post received flag to be set (or timeout and return 504 timeout to caller)
		
		//log.error("sent publish for ID = " + idString);
		
		Object lock = new Object();
		synchronized (lock) {
			locks.put(idString, lock);
			int count = 0;
		    while (!responses.containsKey(idString)) {
		    	try {
					lock.wait(10000);  // timeout is 10 sec
				} catch (InterruptedException e) {
					log.error("Interrupted while waiting for lock notification",e);
				}
		    	count++;
		    	if(count >= 1) break;
		    }
		}

		sms_response = responses.remove(idString);
		
		if (sms_response == null)
		{
			response.setStatus(504);
			log.error("SMS Post response == null for " + idString);
			response.setContentType("text/plain");
			response.setCharacterEncoding("UTF-8");
			response.getWriter().write("504 - SMS Post resposne was null");
			if(log.isInfoEnabled()) log.info("JSC_HRV: TIMECHECK timed out waiting for SMS POST Response");
		}
		else
		{
			if(log.isInfoEnabled()) {
				long tsPostResponse = System.currentTimeMillis();
				log.info("JSC_HRV: TIMECHECK CODE=" + sms_response.getCode() + " .................... tE[" + tsPostResponse + "] " + (tsPostResponse - ts1sendRest2NcDta));
			}
			
			response.setStatus(sms_response.getCode());
			response.setContentType("text/plain");
			response.setCharacterEncoding("UTF-8");
			response.getWriter().write(sms_response.getCode() + " - sms response code");
			
			if (sms_response.getCode() == 200) {
				stats.incrementNumResponses200ok();
			}
			else {
				log.error("statusCode= " + sms_response.getCode() + " for " + idString);
			}
		}
			
    }

}