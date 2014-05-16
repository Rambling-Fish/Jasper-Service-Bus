package org.jasper.jsc.webApp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.jms.JMSException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.jasper.jsc.Jsc;
import org.jasper.jsc.Method;
import org.jasper.jsc.Request;
import org.jasper.jsc.Response;
import org.jasper.jsc.constants.RequestHeaders;
import org.jasper.jsc.constants.ResponseHeaders;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

@WebServlet("/")
public class JscServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	static Logger log = Logger.getLogger(JscServlet.class.getName());
	private Map<String, String> uriMapper = new HashMap<String, String>();
	private String rule;
	private String ruri;
	private static int MILLISECONDS = 1000;
	
	private Jsc jsc;

	public JscServlet(){
		super();
    }
    
	public void init(){
		jsc = new Jsc(getProperties());
    	loadOntologyMapper();
		try {
			jsc.init();
		} catch (JMSException e) {
			log.error("Exception occurred during initialization " + e);
		}
    }
	
    public void destroy(){
    	jsc.destroy();
    }
    
    private Properties getProperties() {
    	Properties prop = new Properties();
		try {
			File file = new File(System.getProperty("catalina.base") + "/conf/jsc.properties");
			if(file.exists()){
				FileInputStream fileInputStream = new FileInputStream(file);
				if(log.isInfoEnabled()) log.info("loading properties file from catalina.base/conf");
				prop.load(fileInputStream);
			}else{
				InputStream input = getServletContext().getResourceAsStream("/WEB-INF/conf/jsc.properties");
				if(log.isInfoEnabled()) log.info("loading properties file from WEB-INF/conf");
				prop.load(input);
			}
			
		} catch (IOException e) {
			log.error("error loading jsc properties file.", e);
		}
		return prop;
	}
    
    private void loadOntologyMapper() {
    	BufferedReader br = null;
    	String line;
    	String[] parsedLine;
		try {
    	File file = new File(System.getProperty("catalina.base") + "/conf/jsc.ontology.properties");
			if(file.exists()){
				FileReader inputFile = new FileReader(file);
				if(log.isInfoEnabled()) log.info("loading properties file from catalina.base/conf");
				br = new BufferedReader(inputFile);

			    // Read file line by line and print on the console
			    while ((line = br.readLine()) != null)   {
			    	parsedLine = line.split("=");
			    	uriMapper.put(parsedLine[0], parsedLine[1]);
			    }
			    //Close the buffer reader
			    br.close();
			}else{
				InputStream input = getServletContext().getResourceAsStream("/WEB-INF/conf/jsc.ontology.properties");
				if(input != null){
					br = new BufferedReader(new InputStreamReader(input));
					if(log.isInfoEnabled()) log.info("loading properties file from WEB-INF/conf");
					while ((line = br.readLine()) != null)   {
						parsedLine = line.split("=");
						uriMapper.put(parsedLine[0], parsedLine[1]);
					}
					//Close the buffer reader
					br.close();
				}
			}
			
		} catch (IOException e) {
			log.warn("error loading ontology properties file - continuing without", e);
		} 
   	
	}
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response)  throws ServletException, IOException{
    	ruri = (request.getRequestURI().length()>request.getContextPath().length())?request.getRequestURI().substring(request.getContextPath().length()+1):null;
    	ruri = URLDecoder.decode(ruri, "UTF-8");
    	if(uriMapper.containsKey(ruri)) ruri = uriMapper.get(ruri); // check if short form is being used and switch to long form URI
		JsonObject headers = getHeaders(request);
		Map<String, String> parameters;
		if(ruri.equalsIgnoreCase("sparql")){
			parameters = null;
		}
		else{
			parameters = getParameters(request);
		}
		
		byte[] payload = getPayload(request);
		
		Request jReq = new Request(Method.GET, ruri, headers, parameters, rule, payload);
    	
		Response jscResponse = jsc.get(jReq);
       
		if(jscResponse == null){
	        response.setStatus(408);
			response.getWriter().write("{\"error\":\"408 Request Timeout\"}");
			log.warn("null response from jsc.get(request) assuming timeout");
			return;
		}
		
		String contentType;
		if(jscResponse.getHeaders() != null){
			contentType = (jscResponse.getHeaders().containsKey(ResponseHeaders.CONTENT_TYPE))?jscResponse.getHeaders().get(ResponseHeaders.CONTENT_TYPE):"application/json";
		}
		else{
			contentType = "application/json";
		}
		response.setContentType(contentType );
        response.setCharacterEncoding("UTF-8");
        response.setStatus(jscResponse.getCode());
		
        if(jscResponse.getCode() >= 200 && jscResponse.getCode() <= 299){
        	String jscJsonResponseString = new String(jscResponse.getPayload());
        	if(log.isInfoEnabled()){
        		log.info("successful response, writing response to writer : " + jscJsonResponseString);
        	}
			response.getWriter().write(jscJsonResponseString);
        }else{
			response.getWriter().write("{\"error\":\"" + jscResponse.getCode() + " " + jscResponse.getReason()  + " -  " + jscResponse.getDescription() + "\"}");
			log.warn("non 2xx response received : " + jscResponse.getCode() + " " + jscResponse.getReason()  + " -  " + jscResponse.getDescription());			
        }
    }

    private JsonObject getHeaders(HttpServletRequest request) {

    	JsonObject headers = new JsonObject();
    	
    	String contentType = (request.getContentType() != null)?request.getContentType():"application/json";
    	headers.add(RequestHeaders.CONTENT_TYPE, new JsonPrimitive(contentType) );
 
		if(request.getQueryString() !=null){
			String[] result = request.getQueryString().split("\\?");
    	
			for(String str:result){
				if(str.startsWith(RequestHeaders.RESPONSE_TYPE)){
					headers.add(RequestHeaders.RESPONSE_TYPE, new JsonPrimitive(str.replaceFirst(RequestHeaders.RESPONSE_TYPE+"=", "")));
				}else if(str.startsWith(RequestHeaders.EXPIRES)){
					try{
						int expires = (Integer.parseInt(str.replaceFirst(RequestHeaders.EXPIRES+"=", "")) * MILLISECONDS);
						headers.add(RequestHeaders.EXPIRES, new JsonPrimitive(expires));
					}catch (NumberFormatException e){
						log.error("unanable to parse expires, expires set to : " + (str.replaceFirst(RequestHeaders.EXPIRES+"=", "")),e);
					}
				}else if(str.startsWith(RequestHeaders.POLL_PERIOD)){
					try{
						int pollPeriod = (Integer.parseInt(str.replaceFirst(RequestHeaders.POLL_PERIOD+"=", "")) * MILLISECONDS);
						headers.add(RequestHeaders.POLL_PERIOD, new JsonPrimitive(pollPeriod));
					}catch (NumberFormatException e){
						log.error("unanable to parse poll period, expires set to : " + (str.replaceFirst(RequestHeaders.POLL_PERIOD+"=", "")),e);
					}
				}else if(str.startsWith(RequestHeaders.PROCESSING_SCHEME)){
					headers.add(RequestHeaders.PROCESSING_SCHEME, new JsonPrimitive(str.replaceFirst(RequestHeaders.PROCESSING_SCHEME+"=", "")));
				}
			}
    	}
   
    	return headers;
	}

	private Map<String, String> getParameters(HttpServletRequest request) throws UnsupportedEncodingException {
    	
		String params = null;
		rule          = null;
		StringBuilder sb = new StringBuilder();
    	if(request.getQueryString() !=null){
    		String reqeustString = URLDecoder.decode(request.getQueryString(), "UTF-8");
    		String[] result = reqeustString.split("\\?");
    		
    		for(String str:result){
    			if(str.startsWith(RequestHeaders.RESPONSE_TYPE)  ||
    			    str.startsWith(RequestHeaders.EXPIRES)       ||
    			    str.startsWith(RequestHeaders.POLL_PERIOD))   
    			{
    				continue;
    			}else if(str.startsWith(RequestHeaders.RULE)){
    				rule = str.replaceFirst(RequestHeaders.RULE+"=", "");
    			}
    			else{
    				String[] results = str.split("&");
    				for(int i=0; i<results.length; i++){
    					String[] arr = results[i].split("=");
    					// Convert to long form URI if short form was passed in
    					if(uriMapper.containsKey(arr[0])){
    						sb.append(uriMapper.get(arr[0]));
    					}
    					else{
    						sb.append(arr[0]);
    					}
    						sb.append("=");
    						sb.append(arr[1]);
    						sb.append("&");
    				}
    			}
    		}

    		if(sb.length() > 0){
    			if ((sb.length() - 1) == (sb.lastIndexOf("&"))){
    				sb.deleteCharAt(sb.lastIndexOf("&"));
    			}
    		}
			
    		params = sb.toString();
    	}
    	
    	if(params == null) return null;
    	
    	Map<String, String> map = new HashMap<String, String>();
		String[] keyValuePairs = params.split("&");
		String[] keyValue;
		for(String s:keyValuePairs){
			keyValue = s.split("=");
			if(keyValue.length == 2 ) map.put(keyValue[0], keyValue[1]);
		}

    	return map;
	}

	//TODO UPDATE PAYLOAD
	private byte[] getPayload(HttpServletRequest request) {
		if(ruri.equalsIgnoreCase("sparql")){
			String[] result = request.getQueryString().split("&");
			StringBuilder sb = new StringBuilder();
			sb.append(result[0]);
			if(result.length == 2){
				sb.append(result[1]);
			}
			else{
				sb.append("output=json");
			}
			return sb.toString().getBytes();
		}
		return null;
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
		Map<String,String> parameters = new HashMap<String, String>();
		JsonObject headers = this.getHeaders(request);
		String contentType = request.getHeader(RequestHeaders.CONTENT_TYPE);
		String ruri = (request.getRequestURI().length()>request.getContextPath().length())?request.getRequestURI().substring(request.getContextPath().length()+1):null;
	
	    try {
	        BufferedReader reader = request.getReader();
	        String line = reader.readLine();
	        //TODO what do we do if content-type not application/json?
	        if(line != null && contentType.equalsIgnoreCase("application/json")){
	        	JsonElement jelement = new JsonParser().parse(line);
	    		JsonObject jsonObj = jelement.getAsJsonObject();
	    		if(jsonObj.has(RequestHeaders.PARAMETERS_LABEL)){
	    			JsonElement elem = jsonObj.get(RequestHeaders.PARAMETERS_LABEL);
	    			if(elem.isJsonObject()){
	    				JsonObject parms = jsonObj.getAsJsonObject(RequestHeaders.PARAMETERS_LABEL);
	    				for (Entry<String, JsonElement> key_val: parms.entrySet()) {
	    					parameters.put(key_val.getKey(), key_val.getValue().getAsString());
	    				}
	    			}
	    			else if(elem.isJsonArray()){
	    				JsonArray parmsArray = jsonObj.getAsJsonArray(RequestHeaders.PARAMETERS_LABEL);
	    				parameters.put("parmsArray", parmsArray.toString());
	    			}
	    		}
	    		else {
	    			log.error("missing PARAMETERS_LABEL");
	    		}
	        }
	        reader.close();
	    } catch(IOException e) {
	        log.error("doPost() couldn't get the post data body ", e); 
	    }
		
		Request jReq = new Request(Method.POST, ruri, headers, parameters);
		Response jscResponse = jsc.post(jReq);
		contentType = (jscResponse.getHeaders().containsKey(ResponseHeaders.CONTENT_TYPE))?jscResponse.getHeaders().get(ResponseHeaders.CONTENT_TYPE):"application/json";
		response.setContentType(contentType );
        response.setCharacterEncoding("UTF-8");
		response.setStatus(jscResponse.getCode());
		
        if(jscResponse.getCode() >= 200 && jscResponse.getCode() <= 299){
        	String jscJsonResponseString = new String(jscResponse.getPayload());
			response.getWriter().write(jscJsonResponseString);
        }else{
			response.getWriter().write("{\"error\":\"" + jscResponse.getCode() + " " + jscResponse.getReason()  + " -  " + jscResponse.getDescription() + "\"}");
			log.warn("non 2xx response received : " + jscResponse.getCode() + " " + jscResponse.getReason()  + " -  " + jscResponse.getDescription());			
        }
    }



}