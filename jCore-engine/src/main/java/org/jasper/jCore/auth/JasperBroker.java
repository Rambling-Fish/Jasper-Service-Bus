package org.jasper.jCore.auth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.activemq.broker.Broker;
import org.apache.activemq.broker.BrokerFilter;
import org.apache.activemq.broker.ConnectionContext;
import org.apache.activemq.command.ConnectionInfo;
import org.jasper.jCore.engine.JECore;

public class JasperBroker extends BrokerFilter {
    
	/*
	 * Map will store known connections to prevent multiple jApps from using same jApp authKey
	 * combinations
	 */
//	private static Map<String, String> jAppList = new ConcurrentHashMap<String, String>();
	
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
    		System.out.println("jApp authenticated : " + info.getUserName() + ":" + info.getPassword());
    	}else{
    		System.out.println("Invalid jApp name and id combination : " + info.getUserName() + ":" + info.getPassword());
	    	throw (SecurityException)new SecurityException("Invalid jApp name and id combination : " + info.getUserName() + ":" + info.getPassword());
    	}
    	
//    	//TODO store connection, client IDs and/or IPs of registered jApps so that we can better log why a registration is failing.
//    	
//    	if(!jAppList.containsKey(info.getPassword())){
//    		jAppList.put(info.getPassword(), info.getUserName());
//    		System.out.println("j-App registered in system : " + info.getUserName() + ":" + info.getPassword());
//    	}else{
//    		throw (SecurityException)new SecurityException("Only one instance of a jApp can be registered with Jasper Core, jApp with with name and id combination already registered : " + info.getUserName() + ":" + info.getPassword());
//    	}
		super.addConnection(context, info);
    }
    
    public void removeConnection(ConnectionContext context, ConnectionInfo info, Throwable error)throws Exception{
		System.out.println("\njApp deregistered in system : " + info.getUserName() + ":" + info.getPassword());
//    	jAppList.remove(info.getPassword());
    	super.removeConnection(context, info, null);
    }
    
    

}