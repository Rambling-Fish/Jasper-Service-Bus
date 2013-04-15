package org.jasper.core.delegate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jms.Connection;
import javax.jms.JMSException;

import org.apache.activemq.ActiveMQConnectionFactory;

public class DelegateFactory{

	public static final String DELEGATE_GLOBAL_QUEUE = "jms.jasper.delegate.global.queue";

	private static final String DELEGATE_DEFAULT_NAME = "jasperDelegate";
	private static final String JASPER_ADMIN_USERNAME = "jasperAdminUsername";
	private static final String JASPER_ADMIN_PASSWORD = "jasperAdminPassword";
	private static DelegateFactory factory;
	private AtomicInteger count;
	protected Map<String, String> jtaUriMap;
	private Connection connection;
	
	private DelegateFactory() throws JMSException{
		count = new AtomicInteger();
		jtaUriMap = new ConcurrentHashMap<String, String>();
		
		
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost");
        // Create a Connection
        connectionFactory.setUserName(JASPER_ADMIN_USERNAME);
        connectionFactory.setPassword(JASPER_ADMIN_PASSWORD);
        connection = connectionFactory.createConnection();
        connection.start(); 
	}
	
	public static DelegateFactory getInstance() throws JMSException{
		if(factory == null) factory = new DelegateFactory();
		return factory;
	}
	
	public Delegate createDelegate(){
		return createDelegate(DELEGATE_DEFAULT_NAME);
	}
	
	public Delegate createDelegate(String name){
		return new Delegate(name + "." + count.getAndIncrement(),connection, jtaUriMap);
	}	
}
