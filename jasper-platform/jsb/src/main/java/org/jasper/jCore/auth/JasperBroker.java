package org.jasper.jCore.auth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.activemq.broker.Broker;
import org.apache.activemq.broker.BrokerFilter;
import org.apache.activemq.broker.ConnectionContext;
import org.apache.activemq.command.ConnectionInfo;
import org.apache.log4j.Logger;
import org.jasper.jCore.engine.JECore;

public class JasperBroker extends BrokerFilter {
    
	/*
	 * Map will store known connections to prevent multiple JTAs from using same JTA license key
	 */
	private static Map<String, ConnectionInfo> jtaList = new ConcurrentHashMap<String, ConnectionInfo>();
	static Logger logger = Logger.getLogger("org.jasper");
	
     public JasperBroker(Broker next) {
        super(next);  
    }

    public void addConnection(ConnectionContext context, ConnectionInfo info) throws Exception {  
    	/*
    	 * During connection setup we validate that the JTA lisence key provided is valide and
    	 * matches the stated JTA. JTA info is stored in username and the license key in the password
    	 */
    	if(JECore.getInstance().isJTAAuthenticationValid(info.getUserName(), info.getPassword())){
    		if(logger.isInfoEnabled()){
    			logger.info("JTA authenticated : " + info.getUserName());
    		}
    	}else{
    		logger.error("Invalid JTA license key : " + info.getUserName());
	    	throw (SecurityException)new SecurityException("Invalid JTA license key : " + info.getUserName());
    	}
    	
    	/*
    	 * Check to see if JTA deploymentId matches that of the system.
    	 */
    	if(JECore.getInstance().isSystemDeploymentId(info.getUserName().split(":")[3])){
    		if(logger.isInfoEnabled()){
    			logger.info("JTA deploymentId matches that of the system : " + info.getUserName().split(":")[3]);
    		}
    	}else{
    		logger.error("JTA deploymentId does not match that of the system. JTA deploymentId : " + info.getUserName().split(":")[3] + " and system deploymentId : " + JECore.getInstance().getDeploymentID());
	    	throw (SecurityException)new SecurityException("JTA deploymentId does not match that of the system. JTA deploymentId : " + info.getUserName().split(":")[3] + " and system deploymentId : " + JECore.getInstance().getDeploymentID());
    	}
    	
    	/*
    	 * We check that only one JTA per license key is ever registered at one time, if a second JTA attempts
    	 * to register we throw a security exception
    	 */
    	if(!(jtaList.containsKey(info.getPassword()))){
    		jtaList.put(info.getPassword(), info);
    		if(logger.isInfoEnabled()){
    			logger.info("JTA registered in system : " + info.getUserName());
    		}
    	}else{
    		ConnectionInfo oldAppInfo = jtaList.get(info.getPassword());
    		logger.error("JTA not registred in system, only one instance of a JTA can be registered with JSB core at a time, JTA with with the following info already registered \n" +
                    "vendor:appName:version:deploymentId = " + oldAppInfo.getUserName() + "\n" +
                    "clientId:clientIp = " + oldAppInfo.getClientId() + ":" + oldAppInfo.getClientIp());
    		throw (SecurityException)new SecurityException("JTA not registred in system, only one instance of a JTA can be registered with JSB core at a time, JTA with with the following info already registered \n" +
                    "vendor:appName:version:deploymentId = " + oldAppInfo.getUserName() + "\n" +
                    "clientId:clientIp = " + oldAppInfo.getClientId() + ":" + oldAppInfo.getClientIp());
    	}
		super.addConnection(context, info);
    }
    
    public void removeConnection(ConnectionContext context, ConnectionInfo info, Throwable error)throws Exception{
    	if(logger.isInfoEnabled()){
    		logger.info("JTA deregistered in system : " + info.getUserName());
    	}
    	jtaList.remove(info.getPassword());
    	super.removeConnection(context, info, null);
    }
   
}