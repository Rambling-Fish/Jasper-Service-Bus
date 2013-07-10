package org.jasper.core.delegate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jms.Connection;
import javax.jms.JMSException;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.jasper.core.JECore;
import org.jasper.core.constants.JasperConstants;

import com.hazelcast.core.HazelcastInstance;

public class DelegateFactory{

	protected Map<String, List<String>> jtaUriMap;
	protected Map<String, List<String>> jtaQueueMap;
	private Connection connection;
	private HazelcastInstance hazelcastInstance;
	
    public DelegateFactory(boolean distributed, JECore core) throws JMSException{
		
		if(distributed && core != null){
	        hazelcastInstance = core.getHazelcastInstance();
	        jtaUriMap = hazelcastInstance.getMap("jtaUriMap");
	        jtaQueueMap = hazelcastInstance.getMap("jtaQueueMap");
    	}else{
    		jtaUriMap = new ConcurrentHashMap<String, List<String>>();
    		jtaQueueMap = new ConcurrentHashMap<String, List<String>>();
    	}         
		
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost");
        // Create a Connection
        connectionFactory.setUserName(JasperConstants.JASPER_ADMIN_USERNAME);
        connectionFactory.setPassword(JasperConstants.JASPER_ADMIN_PASSWORD);
        connection = connectionFactory.createConnection();
        connection.start(); 
	}
	
	public Delegate createDelegate() throws JMSException{
		return new Delegate(connection, jtaUriMap, jtaQueueMap);
	}
}
