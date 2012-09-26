package org.jasper.jCore.engine;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.activemq.broker.Connector;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.jasper.jCore.auth.JasperBrokerService;

public class JECore {
	
	/*
	 * VERY IMPOARTANT STRING DO NOT CHANGE
	 * this is our hard coded SALT which we use to both generate and validate
	 * app id's, this salt should not be shared outside of Coral CEA
	 */
	private static final String SALT = "aAN:TKcacqi]@yO0xm?d";
	static Logger logger = Logger.getLogger("org.jasper");

	//TODO add deployment ID info and check deployment ID against registering j-Apps
	
	/**
	 * @param appID is the app id, in the format : <vendor>:<app_name>:<version>:<deployment_id>
	 * @param appKey is the generated app ID which matches the app username
	 * @return true if the appID corresponds to the appKey
	 */
	public static boolean isAuthenticationValid(String appID, String appKey){
		if(appID == null || appKey == null){
			return false;
		}else{
			String[] appDetails = appID.split(":");
			if(appDetails.length != 4){
				logger.error("appID incorrectly formatted");
				return false;
			}
			System.out.println("\napp vendor        = " + appDetails[0]);
			System.out.println("app name          = " + appDetails[1]);
			System.out.println("app version       = " + appDetails[2]);
			System.out.println("app deployment id = " + appDetails[3]);
			System.out.println("app key           = " + appKey);
			return appKey.equals(DigestUtils.shaHex(appID + SALT));
		}
	}
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
	    
    	Properties prop = new Properties();
    	 
    	try {
    		Properties sysProps = System.getProperties();
               //load a properties file
    		prop.load(new FileInputStream(System.getProperty("jCore-engine-property-file")));
    		DOMConfigurator.configure(System.getProperty("jCore-engine-log4j-xml"));
    		if(logger.isDebugEnabled()) {
    			logger.debug("jasperDeploymentID = " + prop.getProperty("jasperDeploymentID")); 
    		}
    	} catch (IOException ex) {
    		ex.printStackTrace();
    	}
		
		/*
		 * Create new JasperBrokerService to handle JMS messages
		 */
		JasperBrokerService brokerService = new JasperBrokerService();

		// configure the broker
		Connector connector = brokerService.addConnector("tcp://localhost:61616");
		
		brokerService.start();	

	}

}
