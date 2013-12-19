package org.jasper.core.delegate;

import javax.jms.Connection;
import javax.jms.JMSException;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.jasper.core.UDE;
import org.jasper.core.constants.JasperConstants;
import org.jasper.core.constants.JasperOntologyConstants;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class DelegateFactory{

	private Connection connection;
    private Model model;
    private DelegateOntology jOntology;
	private UDE ude;

    public DelegateFactory(UDE ude) throws JMSException{
    	this.ude = ude;
    	initializeModel();   	
    	
		jOntology = new DelegateOntology(ude.getCachingSys(), model);
		
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost");
        // Create a Connection
        connectionFactory.setUserName(JasperConstants.JASPER_ADMIN_USERNAME);
        connectionFactory.setPassword(JasperConstants.JASPER_ADMIN_PASSWORD);
        connection = connectionFactory.createConnection();
        connection.start(); 
	}
	
	public Delegate createDelegate() throws JMSException{
		return new Delegate(ude, connection, model, jOntology);
	}
	
    private void initializeModel() {
        model = ModelFactory.createDefaultModel();
        for(String prefix:JasperOntologyConstants.PREFIX_MAP.keySet()){
        	model.setNsPrefix(prefix, JasperOntologyConstants.PREFIX_MAP.get(prefix));
        }
    }

}
