package org.jasper.core.delegate;
 
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
 
import javax.jms.JMSException;
import javax.jms.QueueConnection;
 
import org.apache.activemq.ActiveMQConnectionFactory;
 
import com.hazelcast.config.Config;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
 
public class DelegateFactory{
 
    public static final String DELEGATE_GLOBAL_QUEUE = "jms.jasper.delegate.global.queue";
    public static final Map<String, String> URI_MAPPER;
    static {
        Map<String, String> aMap = new HashMap<String, String>();
        aMap.put("patientId", "http://coralcea.ca/jasper/patient/id");
        aMap.put("hrsId"     , "http://coralcea.ca/jasper/medicalSensor/heartRate/sensorId");
        aMap.put("bpsId"     , "http://coralcea.ca/jasper/medicalSensor/bloodPressure/sensorId");
        aMap.put("msData"   , "http://coralcea.ca/jasper/medicalSensor/data");
        aMap.put("hrData"   , "http://coralcea.ca/jasper/medicalSensor/heartRate/data");
        aMap.put("bpData"   , "http://coralcea.ca/jasper/medicalSensor/bloodPressure/data");
        URI_MAPPER = Collections.unmodifiableMap(aMap);
    }
     
    private static final String DELEGATE_DEFAULT_NAME = "jasperDelegate";
    private static final String JASPER_ADMIN_USERNAME = "jasperAdminUsername";
    private static final String JASPER_ADMIN_PASSWORD = "jasperAdminPassword";
 
    private AtomicInteger count;
    private QueueConnection queueConnection;
     
    private Model model;
     
    private Map<String,List<String>> jtaUriMap;
    private Map<String, List<String>> jtaQueueMap;
	private HazelcastInstance hazelcastInstance;
     
    public DelegateFactory() throws JMSException{
//      initializeModel();
        initializeMaps();
         
        count = new AtomicInteger();
         
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost");
        // Create a Connection
        connectionFactory.setUserName(JASPER_ADMIN_USERNAME);
        connectionFactory.setPassword(JASPER_ADMIN_PASSWORD);
        queueConnection = connectionFactory.createQueueConnection();
        queueConnection.start(); 
    }
     
    private void initializeMaps() {
        Config cfg = new Config();
        GroupConfig groupConfig = new GroupConfig("jasperLab", "jasperLabPasswordJune_05_2013_1510"); //TODO USE DEPLOYMENET ID
        cfg.setGroupConfig(groupConfig);
        hazelcastInstance = Hazelcast.newHazelcastInstance(cfg);
        jtaUriMap = hazelcastInstance.getMap("jtaUriMap");
        jtaQueueMap = hazelcastInstance.getMap("jtaQueueMap");
    }
    
    public void shutdown(){
    	hazelcastInstance.getLifecycleService().shutdown();
    	int count = 0;
		try {
	    	while(hazelcastInstance.getLifecycleService().isRunning()){
				Thread.sleep(500);
	    		count++;
	    		if(count > 20){
	    			hazelcastInstance.getLifecycleService().kill();
	    			break;
	    		}
	    	}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
        model.createResource("http://coralcea.ca/jasper/medicalSensor/bloodPressure/sensorId");
        model.createResource("http://coralcea.ca/jasper/medicalSensor/heartRate/sensorId");
         
    }
     
    public Delegate createDelegate(){
        return createDelegate(DELEGATE_DEFAULT_NAME);
    }
     
    public Delegate createDelegate(String name){
        return new Delegate(name + "." + count.getAndIncrement(),queueConnection, jtaUriMap, jtaQueueMap, model);
    }   
}