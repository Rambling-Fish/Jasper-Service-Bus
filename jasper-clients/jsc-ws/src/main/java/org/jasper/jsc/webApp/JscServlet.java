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
    	StringBuffer jasperQuery = new StringBuffer();
    	String[] tmp;
    	String[] tmp2;
        String path = (request.getRequestURI().length()>request.getContextPath().length())?request.getRequestURI().substring(request.getContextPath().length()+1):"";
        if(uriMapper.containsKey(path)){
        	jasperQuery.append(uriMapper.get(path));
        }
        else{
        	jasperQuery.append(path);
        }
 
        if(request.getQueryString() != null){
            jasperQuery.append("?");
            if(!request.getQueryString().startsWith("query=")){
            	// parse all parameters in incoming query
            	// we ignore sparql queries as there is nothing to parse or
            	// URIs to map
            	String parms[] = request.getQueryString().split("\\?");

            	for(int i=0;i<parms.length;i++){
            		if(!parms[i].startsWith("trigger=")){
            			tmp2 = parms[i].split("&");
            			for(int x=0;x<tmp2.length;x++){
            				tmp = tmp2[x].split("=");
            				if(uriMapper.containsKey(tmp[0])){
            					jasperQuery.append(uriMapper.get(tmp[0]));
            				}
            				else{
            					jasperQuery.append(tmp[0]);
            				}
            				if(tmp.length == 2){
            					jasperQuery.append("=");
            					jasperQuery.append(tmp[1]);
            					if((tmp2.length - 1) > x){
            						jasperQuery.append("&");
            					}
            				}
            			}
            			if((parms.length - 1) > i) jasperQuery.append("?");
            		}
            		else{
            			jasperQuery.append(parms[i]);
            			if((parms.length - 1) > i){
            				jasperQuery.append("?");
            			}
            		}
            	}
            }
            else{
            	jasperQuery.append(request.getQueryString());
            }
        }
       
		response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
		
        String jscResponse = jsc.get(jasperQuery.toString());
        
		if(jscResponse == null){
			response.getWriter().write("{\"error\"=\"null response from jsc for reqeust : " + jasperQuery + "\"}");
			log.warn("null response for from jsc for request : " + jasperQuery);			
		}else{
			response.getWriter().write(jscResponse);
		}
    }

    protected void doPost(HttpServletRequest httpservletrequest, HttpServletResponse httpservletresponse) throws ServletException, IOException{
    	httpservletresponse.getWriter().write("POST NOT SUPPORTED");
    }



}