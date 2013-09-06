package org.jasper.core.delegate;

import javax.jms.Connection;
import javax.jms.JMSException;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.jasper.core.JECore;
import org.jasper.core.constants.JasperConstants;
import org.jasper.core.constants.JasperOntologyConstants;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class DelegateFactory{

	private Connection connection;
    private Model model;
    private HazelcastInstance hazelcastInstance;
    private DelegateOntology jOntology;

    public DelegateFactory(boolean distributed, JECore core) throws JMSException{ 	
    	initializeModel();   	
    	
    	Config cfg = new Config();
		hazelcastInstance = Hazelcast.newHazelcastInstance(cfg);
		jOntology = new DelegateOntology(this, model);
		
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost");
        // Create a Connection
        connectionFactory.setUserName(JasperConstants.JASPER_ADMIN_USERNAME);
        connectionFactory.setPassword(JasperConstants.JASPER_ADMIN_PASSWORD);
        connection = connectionFactory.createConnection();
        connection.start(); 
	}
	
	public Delegate createDelegate() throws JMSException{
		return new Delegate(connection, model, jOntology);
	}
	
    private void initializeModel() {
        model = ModelFactory.createDefaultModel();
        for(String prefix:JasperOntologyConstants.PREFIX_MAP.keySet()){
        	model.setNsPrefix(prefix, JasperOntologyConstants.PREFIX_MAP.get(prefix));
        }
    }

	public HazelcastInstance getHazelcastInstance() {
		return hazelcastInstance;
	}
}
