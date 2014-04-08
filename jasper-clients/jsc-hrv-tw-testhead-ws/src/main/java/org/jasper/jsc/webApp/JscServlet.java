package org.jasper.jsc.webApp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
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
import org.jasper.jsc.constants.ResponseHeaders;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
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
//		jsc = new Jsc(getProperties());
//		try {
//			jsc.init();
			setupNCSubscription();
//		} catch (JMSException e) {
//			log.error("Exception occurred during initialization " + e);
//		}
		stats.setConnected(true);
    }
	
    private void setupNCSubscription() {
		// TODO Auto-generated method stub
    	
    	Map<String, String> headers = new HashMap<String, String>();
    	headers.put(RequestHeaders.RESPONSE_TYPE, "application/json");
		Request request = new Request(Method.SUBSCRIBE, "http://NC", headers);
		Listener listener = new Listener() {
			
			public void processMessage(Response response) {
				// TODO Auto-generated method stub
				/*
				 * increment count
				 * send back SMS POST with bedNumber
				 */
				
			}
		};
		jsc.registerListener(listener , request);
    	
	}
    
    private void unSubscribeNC() {
		// TODO Auto-generated method stub
		
	}

	public void destroy(){
    	jsc.destroy();
    	unSubscribeNC();
    }
    
	private Properties getProperties() {
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
			log.error("error loading jsc properties file.", e);
		}
		return prop;
	}
    
 
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response)  throws ServletException, IOException{
		response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
		response.getWriter().write(gson.toJson(stats));
    }



}