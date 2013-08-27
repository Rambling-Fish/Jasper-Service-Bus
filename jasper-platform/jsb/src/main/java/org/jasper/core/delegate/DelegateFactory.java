package org.jasper.core.delegate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDFS;

public class DelegateFactory{

	private Connection connection;
    private Model model;
    private HazelcastInstance hazelcastInstance;
    private DelegateOntology jOntology;
    
    public static final Map<String, String> URI_MAPPER;
    static {
        Map<String, String> aMap = new HashMap<String, String>();
        aMap.put("patientId", "http://coralcea.ca/jasper/patient/id");
        aMap.put("hrsId"    , "http://coralcea.ca/jasper/medicalSensor/heartRate/sensorId");
        aMap.put("bpsId"    , "http://coralcea.ca/jasper/medicalSensor/bloodPressure/sensorId");
        aMap.put("msData"   , "http://coralcea.ca/jasper/medicalSensor/data");
        aMap.put("hrData"   , "http://coralcea.ca/jasper/medicalSensor/heartRate/data");
        aMap.put("bpData"   , "http://coralcea.ca/jasper/medicalSensor/bloodPressure/data");
        URI_MAPPER = Collections.unmodifiableMap(aMap);
    }

	
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
        Map<String,String> modelPrefixes = new HashMap<String,String>();
        int length = JasperOntologyConstants.PREFIXLENGTH - 1;
        String[] prefixes  = new String[length];
        String key = new String();
        String value = new String();
        prefixes = JasperOntologyConstants.MAPPREFIXES.split("\n"); 

        for(int i=0; i<prefixes.length;i++) {
        	key = prefixes[i].substring(0, prefixes[i].indexOf(","));
        	value = prefixes[i].substring(prefixes[i].indexOf(",")+1, prefixes[i].length());
        	modelPrefixes.put(key.trim(), value.trim());
        	model.createResource(value.trim());
        }
      
        model.setNsPrefixes(modelPrefixes);
         
    }

	public HazelcastInstance getHazelcastInstance() {
		return hazelcastInstance;
	}
}
