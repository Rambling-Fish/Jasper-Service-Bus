package org.jasper.core;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

public class UDEMain {
	
	static Logger logger = Logger.getLogger(UDEMain.class.getName());
	private static UDE ude;		
	
	private static Properties getPropeties(){
    	Properties prop = new Properties();
   	 
    	try {
         	prop.load(new FileInputStream(System.getProperty("jsb-property-file")));
    	} catch (IOException ex) {
    		logger.error("unable to load properties",ex);
    	}
    	
        DOMConfigurator.configure(System.getProperty("jsb-log4j-xml"));
    	
        //Add system properties
        prop.putAll(System.getProperties());
        
    	return prop;
	}
	
	public static void main(String[] args) throws Exception {
		
		ude = new UDE(getPropeties());
		ude.start();
    	
		Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
            	ude.stop();
            }
        });
    	
	}
}
