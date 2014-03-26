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

@WebServlet("/")
public class JscServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	static Logger log = Logger.getLogger(JscServlet.class.getName());
	private Map<String, String> uriMapper = new HashMap<String, String>();
	private String rule;
	
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
    	String ruri = (request.getRequestURI().length()>request.getContextPath().length())?request.getRequestURI().substring(request.getContextPath().length()+1):null;
    	if(uriMapper.containsKey(ruri)) ruri = uriMapper.get(ruri); // check if short form is being used and switch to long form URI
		Map<String, String> headers = getHeaders(request);
		Map<String, String> parameters;
		if(ruri.equalsIgnoreCase("sparql")){
			parameters = getSparqlParameters(request);
		}
		else{
			parameters = getParameters(request);
		}
		
		byte[] payload = getPayload(request);
		Request jReq = new Request(Method.GET, ruri, headers, parameters, rule, payload);
    	
		Response jscResponse = jsc.get(jReq);
       
		String contentType;
		if(jscResponse.getHeaders() != null){
			contentType = (jscResponse.getHeaders().containsKey(ResponseHeaders.CONTENT_TYPE))?jscResponse.getHeaders().get(ResponseHeaders.CONTENT_TYPE):"application/json";
		}
		else{
			contentType = "application/json";
		}
		response.setContentType(contentType );
        response.setCharacterEncoding("UTF-8");
		
        if(jscResponse.getCode() >= 200 && jscResponse.getCode() <= 299){
        	String jscJsonResponseString = new String(jscResponse.getPayload());
			response.getWriter().write(jscJsonResponseString);
        }else{
			response.getWriter().write("{\"error\":\"" + jscResponse.getCode() + " " + jscResponse.getReason()  + " -  " + jscResponse.getDescription() + "\"}");
			log.warn("non 2xx response received : " + jscResponse.getCode() + " " + jscResponse.getReason()  + " -  " + jscResponse.getDescription());			
        }
    }

    private Map<String, String> getHeaders(HttpServletRequest request) {
    	Map<String, String> map = new HashMap<String, String>();
    	
    	String contentType = (request.getContentType() != null)?request.getContentType():"application/json";
		map.put(RequestHeaders.CONTENT_TYPE, contentType );
 
		if(request.getQueryString() !=null){
			String[] result = request.getQueryString().split("\\?");
    	
			for(String str:result){
				if(str.startsWith(RequestHeaders.RESPONSE_TYPE)){
					map.put(RequestHeaders.RESPONSE_TYPE, str.replaceFirst(RequestHeaders.RESPONSE_TYPE+"=", ""));
				}else if(str.startsWith(RequestHeaders.EXPIRES)){
					map.put(RequestHeaders.EXPIRES, str.replaceFirst(RequestHeaders.EXPIRES+"=", ""));
				}else if(str.startsWith(RequestHeaders.POLL_PERIOD)){
					map.put(RequestHeaders.POLL_PERIOD, str.replaceFirst(RequestHeaders.POLL_PERIOD+"=", ""));
				}else if(str.startsWith(RequestHeaders.PROCESSING_SCHEME)){
					map.put(RequestHeaders.PROCESSING_SCHEME, str.replaceFirst(RequestHeaders.PROCESSING_SCHEME+"=", ""));
				}
			}
    	}
   
    	return map;
	}
    
    private Map<String, String> getSparqlParameters(HttpServletRequest request) {
    	String params = null;
		StringBuilder sb = new StringBuilder();
		String[] result = request.getQueryString().split("&");
		
    	if(result.length == 1){
    		result[1] = "json";
    	}
    	sb.append(result[0]);
    	sb.append(result[1]);
    	params = sb.toString();
    	
    	if(params == null) return null;
    	
    	Map<String, String> map = new HashMap<String, String>();
    	map.put("parameters", result[0]);
    	map.put("output", result[1]);
    	
    	return map;
    }

	private Map<String, String> getParameters(HttpServletRequest request) {
    	
		String params = null;
		rule          = null;
		StringBuilder sb = new StringBuilder();
    	if(request.getQueryString() !=null){
    		String[] result = request.getQueryString().split("\\?");
    		
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
		return null;
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
		Map<String,String> parameters = new HashMap<String, String>();
		Map<String,String> headers = this.getHeaders(request);
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
		
        if(jscResponse.getCode() >= 200 && jscResponse.getCode() <= 299){
        	String jscJsonResponseString = new String(jscResponse.getPayload());
			response.getWriter().write(jscJsonResponseString);
        }else{
			response.getWriter().write("{\"error\":\"" + jscResponse.getCode() + " " + jscResponse.getReason()  + " -  " + jscResponse.getDescription() + "\"}");
			log.warn("non 2xx response received : " + jscResponse.getCode() + " " + jscResponse.getReason()  + " -  " + jscResponse.getDescription());			
        }
    }



}