package org.jasper.jsc.webApp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
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

@WebServlet("/")
public class JscServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	static Logger log = Logger.getLogger(JscServlet.class.getName());
	private Map<String, String> uriMapper = new HashMap<String, String>();
	
	private Jsc jsc;

	public JscServlet(){
		super();
		jsc = new Jsc(getProperties());
    }
    
	public void init(){
    	loadOntologyMapper();
		try {
			jsc.init();
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		Map<String, String> headers = getHeaders(request);
		Map<String, String> parameters = getParameters(request);
		String rule = getRule(request);
		byte[] payload = getPayload(request);
		Request jReq = new Request(Method.GET, ruri, headers, parameters, rule, payload);
    	
		Response jscResponse = jsc.get(jReq);
        String jscJsonResponseString = new String(jscResponse.getPayload());
       
		String contentType = (jscResponse.getHeaders().containsKey(ResponseHeaders.CONTENT_TYPE))?jscResponse.getHeaders().get(ResponseHeaders.CONTENT_TYPE):"application/json";
		response.setContentType(contentType );
        response.setCharacterEncoding("UTF-8");
		
        if(jscResponse.getCode() >= 200 && jscResponse.getCode() <= 299){
			response.getWriter().write(jscJsonResponseString);
        }else{
			response.getWriter().write("{\"error\":\"" + jscResponse.getCode() + " " + jscResponse.getReason()  + " -  " + jscResponse.getDescription() + "\"}");
			log.warn("non 2xx response recieved : " + jscResponse.getCode() + " " + jscResponse.getReason()  + " -  " + jscResponse.getDescription());			
        }
    }

    private Map<String, String> getHeaders(HttpServletRequest request) {
    	Map<String, String> map = new HashMap<String, String>();
    	
    	String contentType = (request.getContentType() != null)?request.getContentType():"application/json";
		map.put(RequestHeaders.CONTENT_TYPE, contentType );
    	
    	String[] result = request.getQueryString().split("\\?");
    	
    	for(String str:result){
    		if(str.startsWith("output=")){
				map.put(RequestHeaders.RESPONSE_TYPE, str.replaceFirst("output=", ""));
			}else if(str.startsWith(RequestHeaders.RESPONSE_TYPE)){
				map.put(RequestHeaders.RESPONSE_TYPE, str.replaceFirst(RequestHeaders.RESPONSE_TYPE, ""));
			}else if(str.startsWith("expiry=")){
				map.put(RequestHeaders.EXPIRES, str.replaceFirst("expiry=", ""));
			}else if(str.startsWith(RequestHeaders.EXPIRES)){
				map.put(RequestHeaders.EXPIRES, str.replaceFirst(RequestHeaders.EXPIRES, ""));
			}else if(str.startsWith("polling=")){
				map.put(RequestHeaders.POOL_PERIOD, str.replaceFirst("polling=", ""));
			}else if(str.startsWith(RequestHeaders.POOL_PERIOD)){
				map.put(RequestHeaders.POOL_PERIOD, str.replaceFirst(RequestHeaders.POOL_PERIOD, ""));
			}
    	}
    	
    	return map;
	}

	private Map<String, String> getParameters(HttpServletRequest request) {
    	
    	String[] result = request.getQueryString().split("\\?");
    	String params = null;
    	for(String str:result){
    		if(str.startsWith("output=")       || str.startsWith(RequestHeaders.RESPONSE_TYPE) ||
    			    str.startsWith("expiry=")  || str.startsWith(RequestHeaders.EXPIRES)       ||
    			    str.startsWith("polling=") || str.startsWith(RequestHeaders.POOL_PERIOD)   ||
    			    str.startsWith("trigger=") || str.startsWith("rule=") )
    		{
    			continue;
		    }else{
    			params = str;
		    }
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

	private String getRule(HttpServletRequest request) {
		String[] result = request.getQueryString().split("\\?");
    	String rule = null;
    	for(String str:result){
    		if(str.startsWith("trigger=")){
    			rule = str.replaceFirst("trigger=", "");
    		}else if (str.startsWith("rule=") )    		{
    			rule = str.replaceFirst("rule=", "");
		    }
		}
    	
    	return rule;
	}

	//TODO UPDATE PAYLOAD
	private byte[] getPayload(HttpServletRequest request) {
		return null;
	}
	
	protected void doPost(HttpServletRequest httpservletrequest, HttpServletResponse httpservletresponse) throws ServletException, IOException{
    	httpservletresponse.getWriter().write("POST NOT SUPPORTED");
    }



}