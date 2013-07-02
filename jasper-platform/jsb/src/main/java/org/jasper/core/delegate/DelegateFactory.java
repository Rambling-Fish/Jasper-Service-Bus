package org.jasper.core.delegate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jms.Connection;
import javax.jms.JMSException;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.jasper.core.constants.JasperConstants;

public class DelegateFactory{

	private static DelegateFactory factory;
	private AtomicInteger count;
	protected Map<String, List<String>> jtaUriMap;
	protected Map<String, List<String>> jtaQueueMap;
	private Connection connection;
	
	private DelegateFactory() throws JMSException{
		count = new AtomicInteger();
		jtaUriMap   = new ConcurrentHashMap<String, List<String>>();
		jtaQueueMap = new ConcurrentHashMap<String, List<String>>();
		
		
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost");
        // Create a Connection
        connectionFactory.setUserName(JasperConstants.JASPER_ADMIN_USERNAME);
        connectionFactory.setPassword(JasperConstants.JASPER_ADMIN_PASSWORD);
        connection = connectionFactory.createConnection();
        connection.start(); 
	}
	
	public static DelegateFactory getInstance() throws JMSException{
		if(factory == null) factory = new DelegateFactory();
		return factory;
	}
	
	public Delegate createDelegate(){
		return createDelegate(JasperConstants.DELEGATE_DEFAULT_NAME);
	}
	
	public Delegate createDelegate(String name){
		return new Delegate(name + "." + count.getAndIncrement(),connection, jtaUriMap, jtaQueueMap);
	}	
}
