package org.jasper.core.delegate;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.UUID;

import junit.framework.TestCase;

import org.jasper.core.constants.JasperOntologyConstants;
import org.jasper.core.persistence.PersistenceFacade;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonObject;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class TestDelegateOntology extends TestCase {
	private PersistenceFacade cachingSys;
	private OntModel model;
	private String ipAddr;
	private String hazelcastGroup = UUID.randomUUID().toString();
	private DelegateOntology classUnderTest;
	private String ruri = "http://coralcea.ca/jasper/hrData";
	private String ruri2 = "http://coralcea.ca/jasper/getBpData";
	private String operation = "http://coralcea.ca/jasper/getHrData";
	private String inputObject = "http://coralcea.ca/jasper/HrDataReq";
	private String dtaName = "jasper:dta-heart-rate-monitor-D:1.0:jasperLab";
	private String dest = "dta-heart-rate-monitor-d-gethrdata";
	private String triples	= "@prefix :  <http://coralcea.ca/jasper/> .\n@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .\n@prefix hrSensor: <http://coralcea.ca/jasper/hrSensor/> .\n@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .\n@prefix owl:   <http://www.w3.org/2002/07/owl#> .\n@prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n@prefix dta:   <http://coralcea.ca/2014/01/dta#> .\n\n:MsData  a  owl:Class .\n\n:timestamp  a  owl:DatatypeProperty ;\n  rdfs:domain  :HrData ;\n rdfs:range   xsd:string .\n\n:hrSID  a owl:DatatypeProperty ;\n rdfs:domain  :HrDataReq ;\n  rdfs:range   xsd:string .\n\n:HrData  a owl:Class ;\n rdfs:subClassOf  :MsData ;\n  dta:restriction  [ a  owl:Restriction ;\n  owl:onProperty :timestamp\n  ] ;\n dta:restriction  [ a  owl:Restriction ;\n owl:onProperty   hrSensor:bpm\n ] .\n\n:dta-heart-rate-monitor-D\n a dta:DTA ;\n dta:operation  :getHrData .\n\n:hrData  a owl:ObjectProperty ;\n  rdfs:range :HrData ;\n  rdfs:subPropertyOf  :msData .\n\n:msData  a owl:ObjectProperty ;\n rdfs:range  :MsData .\n\n:HrDataReq  a owl:Class ;\n  dta:restriction  [ a owl:Restriction ;\n owl:onProperty   :hrSID;\n ] .\n\nhrSensor:bpm  a owl:DatatypeProperty ;\n  rdfs:domain  :HrData ;\n rdfs:range   xsd:integer .\n\n:getHrData  a dta:Operation ;\n dta:destination  'dta-heart-rate-monitor-d-gethrdata' ;\n dta:input :HrDataReq ;\n dta:kind dta:Get ;\n  dta:output :hrData .\n";	

	/**
	 * This method tests all the public boolean methods of the DelegateOntology class
	 * Decided to test all methods in a single test case to save time having to
	 * start/stop hazelcast between each test.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAllBooleanMethods() throws Exception {
		System.out.println("===============================");
		System.out.println("RUNNING DELEGATE ONTOLOGY TESTS");
		System.out.println("===============================");
		
		classUnderTest.isRuriKnownForInputGet(ruri);
		boolean result = classUnderTest.isRuriKnownForInputGet(null);
		TestCase.assertEquals(false, result);
		
		classUnderTest.isRuriKnownForInputPost(ruri);
		result = classUnderTest.isRuriKnownForInputPost(null);
		TestCase.assertEquals(false, result);
		
		classUnderTest.isRuriKnownForOutputGet(ruri);
		result = classUnderTest.isRuriKnownForOutputGet(null);
		TestCase.assertEquals(false, result);
		
		result = classUnderTest.isRuriSubPropteryOf(ruri, null);
		TestCase.assertEquals(false,  result);
		result = classUnderTest.isRuriSubPropteryOf(ruri, ruri);
		TestCase.assertEquals(true, result);
		result = classUnderTest.isRuriSubPropteryOf(ruri, ruri2);
		TestCase.assertEquals(false, result);
		
		result = classUnderTest.isEncapsulatedRuriKnownForOutput(ruri);
		TestCase.assertEquals(false,  result);
		result = classUnderTest.isEncapsulatedRuriKnownForOutput(null);
		TestCase.assertEquals(false, result);
	}
	
	@Test
	public void testRemove() throws Exception {
		classUnderTest.remove(dtaName);
		classUnderTest.remove(null);
	}
	
	/**
	 * This method tests all the public GET methods of the DelegateOntology class
	 * Decided to test all methods in a single test case to save time having to
	 * start/stop hazelcast between each test.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAllGetOperations() throws Exception {
		ArrayList<String> results = classUnderTest.getProvideOperations(null);
		TestCase.assertEquals(null, results);
		
		loadModel();
		Thread.sleep(50);
		results = classUnderTest.getProvideOperations(ruri);
		TestCase.assertEquals(1, results.size());
		
		results = classUnderTest.getProvideOperationsEncapsulated(null);
		TestCase.assertEquals(null, results);
		results = classUnderTest.getProvideOperationsEncapsulated(ruri);
		TestCase.assertEquals(0, results.size());
		
		String result = classUnderTest.getProvideDestinationQueue(null);
		TestCase.assertEquals(null, result);
		result = classUnderTest.getProvideDestinationQueue(operation);
		TestCase.assertEquals(dest, result);
		
		result = classUnderTest.getProvideOperationInputObject(null);
		TestCase.assertEquals(null, result);
		result = classUnderTest.getProvideOperationInputObject(operation);
		TestCase.assertEquals(inputObject, result);
	}
	
	@Test
	public void testCreateJsonSchema() throws Exception {
		JsonObject result = classUnderTest.createJsonSchema(null);
		TestCase.assertEquals(null, result);
		
		loadModel();
		Thread.sleep(50);
		result = classUnderTest.createJsonSchema(inputObject);
	}
	
	@Test
	public void testAllModelMethods() throws Exception {
		classUnderTest.getModel();
		
		loadModel();
		Thread.sleep(50);
		String queryString = "PREFIX : <http://coralcea.ca/jasper/vocabulary/>\nPREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\nPREFIX jta: <http://coralcea.ca/jasper/vocabulary/jta>\nPREFIX param: <http://coralcea.ca/jasper/vocabulary/param>\nPREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\nPREFIX owl: <http://www.w3.org/2002/07/owl#>\nPREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\nPREFIX provides: <http://coralcea.ca/jasper/vocabulary/provides>\nPREFIX jasper: <http://coralcea.ca/jasper/>\nPREFIX dta: <http://coralcea.ca/2014/01/dta#>\nPREFIX requires: <http://coralcea.ca/jasper/vocabulary/requires>\nSELECT ?operation  WHERE \n   {{\n      ?dta              a                dta:DTA        .\n      ?dta              dta:operation    ?operation     .\n      ?operation        dta:kind         dta:Get        .\n      ?operation        dta:output/rdfs:subPropertyOf*       <http://coralcea.ca/jasper/hrData>  .\n       }       UNION       {      ?dta           a                dta:DTA        .\n      ?dta           dta:operation    ?operation     .\n      ?operation     dta:kind         dta:Get        .\n      ?operation     dta:output/rdfs:subPropertyOf*       ?superRuri     .\n      ?superRuri     rdfs:range       ?superType     .\n      <http://coralcea.ca/jasper/hrData> rdfs:domain      ?superType     .\n   }}";
		String result = classUnderTest.queryModel(queryString, "json");
		TestCase.assertNotNull(result);
		result = classUnderTest.queryModel(queryString, "xml");
		TestCase.assertNotNull(result);
	}
	
	@Test
	private void loadModel(){
		classUnderTest.add(dtaName, triples);
	}
	
	@Before
	public void setUp() throws Exception {
		model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
		for(String prefix:JasperOntologyConstants.PREFIX_MAP.keySet()){
        	model.setNsPrefix(prefix, JasperOntologyConstants.PREFIX_MAP.get(prefix));
        }
		ipAddr = InetAddress.getLocalHost().getHostAddress();
		cachingSys    = new PersistenceFacade(ipAddr, hazelcastGroup, "testPassword");
		classUnderTest = new DelegateOntology(cachingSys, model);
	}

	@After
	public void tearDown() throws Exception {
		classUnderTest.remove(dtaName);
		classUnderTest = null;
		cachingSys.shutdown();
	}
}
