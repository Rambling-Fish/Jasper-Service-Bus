package org.jasper.core.delegate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.jms.Connection;
import javax.jms.JMSException;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.jasper.core.JECore;
import org.jasper.core.constants.JasperConstants;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDFS;

public class DelegateFactory{

	private Connection connection;
    private Model model;
    
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
		
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost");
        // Create a Connection
        connectionFactory.setUserName(JasperConstants.JASPER_ADMIN_USERNAME);
        connectionFactory.setPassword(JasperConstants.JASPER_ADMIN_PASSWORD);
        connection = connectionFactory.createConnection();
        connection.start(); 
	}
	
	public Delegate createDelegate() throws JMSException{
		return new Delegate(connection, model);
	}
	
    private void initializeModel() {
        model = ModelFactory.createDefaultModel();
         
        model.setNsPrefix("jasper", "http://coralcea.ca/jasper/");
        model.setNsPrefix("patient","http://coralcea.ca/jasper/patient/");
 
        model.setNsPrefix("ms",     "http://coralcea.ca/jasper/medicalSensor/");
        model.setNsPrefix("hr",     "http://coralcea.ca/jasper/medicalSensor/heartRate/");
        model.setNsPrefix("hrData", "http://coralcea.ca/jasper/medicalSensor/heartRate/data/");
        model.setNsPrefix("bp",     "http://coralcea.ca/jasper/medicalSensor/bloodPressure/");
        model.setNsPrefix("bpData", "http://coralcea.ca/jasper/medicalSensor/bloodPressure/data/");
        model.setNsPrefix("",       "http://coralcea.ca/jasper/vocabulary/");
        model.setNsPrefix("jta",    "http://coralcea.ca/jasper/jta/");
         
        model.createResource("http://coralcea.ca/jasper/vocabulary/jta");
        model.createProperty("http://coralcea.ca/jasper/vocabulary/provides");
        model.createProperty("http://coralcea.ca/jasper/vocabulary/param");
        model.createProperty("http://coralcea.ca/jasper/vocabulary/has");
        model.createProperty("http://coralcea.ca/jasper/vocabulary/is");
        model.createProperty("http://coralcea.ca/jasper/vocabulary/subClassOf");
        model.createProperty("http://coralcea.ca/jasper/vocabulary/queue");
         
        model.createResource("http://coralcea.ca/jasper/medicalSensor/data");
        model.createResource("http://coralcea.ca/jasper/timeStamp");
        
        Resource bpSid = model.createResource("http://coralcea.ca/jasper/medicalSensor/bloodPressure/sensorId");
        Resource hrSid = model.createResource("http://coralcea.ca/jasper/medicalSensor/heartRate/sensorId");
        
        bpSid.addProperty(RDFS.label, "Blood Pressure Sensor ID");
        hrSid.addProperty(RDFS.label, "Heart Rate Sensor ID");

        
         
    }
}
