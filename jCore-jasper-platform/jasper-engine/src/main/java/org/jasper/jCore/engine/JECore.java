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

	private static String deploymentID;
	private static String deploymentAuthKey;

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
			return appKey.equals(DigestUtils.shaHex(appID + SALT));
		}
	}

	public static boolean isValidDeploymentId(String id) {
		return deploymentID.equals(id);
	}
	
	private static boolean isValidDeploymentId(String id, String authKey) {
		return authKey.equals(DigestUtils.shaHex(id + SALT));
	}
	
	public static String getDeploymentID() {
		return deploymentID;
	}

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
	    
    	Properties prop = new Properties();
    	 
    	try {
            //load a properties file
    		prop.load(new FileInputStream(System.getProperty("jCore-engine-property-file")));
    		DOMConfigurator.configure(System.getProperty("jCore-engine-log4j-xml"));
    		if(logger.isDebugEnabled()) {
    			logger.debug("jasperDeploymentID = " + prop.getProperty("jasperDeploymentID")); 
    			logger.debug("jasperDeploymentAuthKey = " + prop.getProperty("jasperDeploymentAuthKey")); 

    		}
    	} catch (IOException ex) {
    		ex.printStackTrace();
    	}
    	
    	deploymentID = prop.getProperty("jasperDeploymentID");
    	deploymentAuthKey = prop.getProperty("jasperDeploymentAuthKey");
    	
    	
    	if(isValidDeploymentId(deploymentID,deploymentAuthKey)){
			/*
			 * Create new JasperBrokerService to handle JMS messages
			 */
			JasperBrokerService brokerService = new JasperBrokerService();
	
			// configure the broker
			Connector connector = brokerService.addConnector("tcp://"+ prop.getProperty("jasperEngineUrlHost") + ":" + prop.getProperty("jasperEngineUrlPort"));
			
			brokerService.start();
    	}else{
			logger.error("jasperDeploymentID and jasperDeploymentAuthKey don't match, not starting jasper-engine = " + prop.getProperty("jasperDeploymentID") + ":" + prop.getProperty("jasperDeploymentAuthKey")); 
    	}

	}

}
