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
	 * Map will store known connections to prevent multiple jApps from using same jApp authKey
	 * combinations
	 */
	private static Map<String, ConnectionInfo> jAppList = new ConcurrentHashMap<String, ConnectionInfo>();
	static Logger logger = Logger.getLogger("org.jasper");
	
     public JasperBroker(Broker next) {
        super(next);  
    }

    public void addConnection(ConnectionContext context, ConnectionInfo info) throws Exception {       
    	/*
    	 * During connection setup we validate that the jAppID and jAppAuthKey stored in the username and password
    	 * values respectfully, match a generated set, if not we throw a Security Exception which will terminate
    	 * the connection, otherwise we addConnection and process down the chain of brokers.
    	 */
    	if(JECore.isAuthenticationValid(info.getUserName(), info.getPassword())){
    		if(logger.isInfoEnabled()){
    			logger.info("jApp authenticated : " + info.getUserName() + ":" + info.getPassword());
    		}
    	}else{
    		logger.error("Invalid jApp name and id combination : " + info.getUserName() + ":" + info.getPassword());
	    	throw (SecurityException)new SecurityException("Invalid jApp name and id combination : " + info.getUserName() + ":" + info.getPassword());
    	}
    	
    	/*
    	 * Check to see if jApp deploymentId matches that of the system.
    	 */
    	if(JECore.isValidDeploymentId(info.getUserName().split(":")[3])){
    		if(logger.isInfoEnabled()){
    			logger.info("jApp deploymentId matches that of the system : " + info.getUserName().split(":")[3]);
    		}
    	}else{
    		logger.error("jApp deploymentId does not match that of the system. jApp deploymentId : " + info.getUserName().split(":")[3] + " and system deploymentId : " + JECore.getDeploymentID());
	    	throw (SecurityException)new SecurityException("jApp deploymentId does not match that of the system. jApp deploymentId : " + info.getUserName().split(":")[3] + " and system deploymentId : " + JECore.getDeploymentID());
    	}
    	
    	/*
    	 * We check that only one jApp per authKey is ever registered at one time, if a second jApp attempts
    	 * to register we throw a security exception
    	 */
    	if(!(jAppList.containsKey(info.getPassword()))){
    		jAppList.put(info.getPassword(), info);
    		if(logger.isInfoEnabled()){
    			logger.info("jApp registered in system : " + info.getUserName() + ":" + info.getPassword());
    		}
    	}else{
    		ConnectionInfo oldAppInfo = jAppList.get(info.getPassword());
    		logger.error("jApp not registred in system, only one instance of a jApp can be registered with Jasper Core at a time, jApp with with the following info already registered \n" +
                    "vendor:appName:version:deploymentId:jAppAuthKey = " + oldAppInfo.getUserName() + ":" + oldAppInfo.getPassword() + "\n" +
                    "clientId:clientIp = " + oldAppInfo.getClientId() + ":" + oldAppInfo.getClientIp());
    		throw (SecurityException)new SecurityException("jApp not registred in system, only one instance of a jApp can be registered with Jasper Core at a time, jApp with with the following info already registered \n" +
    				                                        "vendor:appName:version:deploymentId:jAppAuthKey = " + oldAppInfo.getUserName() + ":" + oldAppInfo.getPassword() + "\n" +
    				                                        "clientId:clientIp = " + oldAppInfo.getClientId() + ":" + oldAppInfo.getClientIp());
    	}
		super.addConnection(context, info);
    }
    
    public void removeConnection(ConnectionContext context, ConnectionInfo info, Throwable error)throws Exception{
    	if(logger.isInfoEnabled()){
    		logger.info("jApp deregistered in system : " + info.getUserName() + ":" + info.getPassword());
    	}
    	jAppList.remove(info.getPassword());
    	super.removeConnection(context, info, null);
    }
   
}