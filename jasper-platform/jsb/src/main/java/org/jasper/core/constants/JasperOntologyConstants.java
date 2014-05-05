package org.jasper.core.constants;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;



public class JasperOntologyConstants {
	
    public static final Map<String, String> PREFIX_MAP;
    static {
        Map<String, String> aMap = new HashMap<String, String>();
        aMap.put(""          , "http://coralcea.ca/jasper/vocabulary/");
        aMap.put("jasper"    , "http://coralcea.ca/jasper/");
        aMap.put("jta"       , "http://coralcea.ca/jasper/vocabulary/jta");
        aMap.put("param"     , "http://coralcea.ca/jasper/vocabulary/param");
        aMap.put("requires"  , "http://coralcea.ca/jasper/vocabulary/requires");
        aMap.put("provides"  , "http://coralcea.ca/jasper/vocabulary/provides");
        aMap.put("rdfs"      , "http://www.w3.org/2000/01/rdf-schema#");        // Vocab 2
        aMap.put("owl"       , "http://www.w3.org/2002/07/owl#");               // Vocab 2
        aMap.put("xsd"       , "http://www.w3.org/2001/XMLSchema#");            // Vocab 2
        aMap.put("rdf"       , "http://www.w3.org/1999/02/22-rdf-syntax-ns#");  // Vocab 2
        aMap.put("dta"       , "http://coralcea.ca/2014/01/dta#");              // Vocab 2
        PREFIX_MAP = Collections.unmodifiableMap(aMap);
    }
    
    public static final String PREFIXES;
    static {
        StringBuffer sb = new StringBuffer();
        for(String key:PREFIX_MAP.keySet()){
        	sb.append("PREFIX ");
        	sb.append(key);
        	sb.append(": <");
        	sb.append(PREFIX_MAP.get(key));
        	sb.append(">\n");
        }
        PREFIXES = sb.toString();
    }
	
//	// prefixLength has to be set to the number of rows in the MAPPREFIXES
//	public static final int PREFIXLENGTH = 11;
//	
//	// Prefixes that are pre-loaded into the model during initialization
//	public static final String MAPPREFIXES = 
//			" ,          http://coralcea.ca/jasper/vocabulary/" + "\n" +
//	        "jta ,       http://coralcea.ca/jasper/vocabulary/jta/" + "\n" +
//			"provides,   http://coralcea.ca/jasper/vocabulary/provides/" + "\n" +
//			"requires,   http://coralcea.ca/jasper/vocabulary/requires/" + "\n" +
//	        "param,      http://coralcea.ca/jasper/vocabulary/param/" + "\n" +
//			"queue,      http://coralcea.ca/jasper/vocabulary/queue/" + "\n" +
//	        "subClass,   http://coralcea.ca/jasper/vocabulary/subClassOf/" + "\n" +
//			"is,         http://coralcea.ca/jasper/vocabulary/is/" + "\n" +
//	        "has,        http://coralcea.ca/jasper/vocabulary/has/" + "\n" +
//			"timestamp,  http://coralcea.ca/jasper/vocabulary/timeStamp/" + "\n" +
//			"jasper,     http://coralcea.ca/jasper/" + "\n" ;
//
//	// prefixes used in Sparql queries JSB ontology model
//	public static final String PREFIXS = 
//			"PREFIX :          <http://coralcea.ca/jasper/vocabulary/>" + "\n" +
//	        "PREFIX jta:       <http://coralcea.ca/jasper/vocabulary/jta/>" + "\n" +
//			"PREFIX provides:  <http://coralcea.ca/jasper/vocabulary/provides/>" + "\n" +
//	        "PREFIX param:     <http://coralcea.ca/jasper/vocabulary/param/>" + "\n" +
//			"PREFIX queue:     <http://coralcea.ca/jasper/vocabulary/queue/>" + "\n" +
//	        "PREFIX subClass:  <http://coralcea.ca/jasper/vocabulary/subClassOf/>" + "\n" +
//			"PREFIX is:        <http://coralcea.ca/jasper/vocabulary/is/>" + "\n" +
//	        "PREFIX has:       <http://coralcea.ca/jasper/vocabulary/has/>" + "\n" +
//	        "PREFIX timestamp: <http://coralcea.ca/jasper/vocabulary/timestamp/>" + "\n" +
//	        "PREFIX requires:  <http://coralcea.ca/jasper/vocabulary/requires/>" + "\n" +
//	 		"PREFIX jasper:    <http://coralcea.ca/jasper/>" + "\n" ;
//	 "PREFIX hr:      <http://coralcea.ca/jasper/medicalSensor/heartRate/>" + "\n" +
//     "PREFIX :        <http://coralcea.ca/jasper/vocabulary/>" + "\n" +
//	 "PREFIX bp:      <http://coralcea.ca/jasper/medicalSensor/bloodPressure/>" + "\n" +
//	 "PREFIX jta:     <http://coralcea.ca/jasper/jta/>" + "\n" +
//	 "PREFIX patient: <http://coralcea.ca/jasper/patient/>" + "\n" +
//	 "PREFIX ms:      <http://coralcea.ca/jasper/medicalSensor/>" + "\n" +
//	 "PREFIX bpData:  <http://coralcea.ca/jasper/medicalSensor/bloodPressure/data/>" + "\n" +
//	 "PREFIX hrData:  <http://coralcea.ca/jasper/medicalSensor/heartRate/data/>" + "\n" +
//	 "PREFIX jasper:  <http://coralcea.ca/jasper/>" + "\n";
	
}
