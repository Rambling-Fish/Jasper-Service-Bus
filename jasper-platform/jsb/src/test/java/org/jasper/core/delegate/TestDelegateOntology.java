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
import org.mockito.MockitoAnnotations;

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
	private String dtaName = "jasper:dta-heart-rate-monitor-D:1.0:rayLab";
	private String dest = "dta-heart-rate-monitor-d-gethrdata";
	private String triples	= "@prefix :  <http://coralcea.ca/jasper/> .\n@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .\n@prefix hrSensor: <http://coralcea.ca/jasper/hrSensor/> .\n@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .\n@prefix owl:   <http://www.w3.org/2002/07/owl#> .\n@prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n@prefix dta:   <http://coralcea.ca/2014/01/dta#> .\n\n:MsData  a  owl:Class .\n\n:timestamp  a  owl:DatatypeProperty ;\n  rdfs:domain  :HrData ;\n rdfs:range   xsd:string .\n\n:hrSID  a owl:DatatypeProperty ;\n rdfs:domain  :HrDataReq ;\n  rdfs:range   xsd:string .\n\n:HrData  a owl:Class ;\n rdfs:subClassOf  :MsData ;\n  dta:restriction  [ a  owl:Restriction ;\n  owl:onProperty :timestamp\n  ] ;\n dta:restriction  [ a  owl:Restriction ;\n owl:onProperty   hrSensor:bpm\n ] .\n\n:dta-heart-rate-monitor-D\n a dta:DTA ;\n dta:operation  :getHrData .\n\n:hrData  a owl:ObjectProperty ;\n  rdfs:range :HrData ;\n  rdfs:subPropertyOf  :msData .\n\n:msData  a owl:ObjectProperty ;\n rdfs:range  :MsData .\n\n:HrDataReq  a owl:Class ;\n  dta:restriction  [ a owl:Restriction ;\n owl:onProperty   :hrSID;\n ] .\n\nhrSensor:bpm  a owl:DatatypeProperty ;\n  rdfs:domain  :HrData ;\n rdfs:range   xsd:integer .\n\n:getHrData  a dta:Operation ;\n dta:destination  'dta-heart-rate-monitor-d-gethrdata' ;\n dta:input :HrDataReq ;\n dta:kind dta:Get ;\n  dta:output :hrData .\n";	

	@Test
	public void testIsRuriKnownForInputGet() throws Exception {
		System.out.println("===============================");
		System.out.println("RUNNING DELEGATE ONTOLOGY TESTS");
		System.out.println("===============================");
		classUnderTest.isRuriKnownForInputGet(ruri);
		boolean result = classUnderTest.isRuriKnownForInputGet(null);
		TestCase.assertEquals(false, result);
	}	
	
	@Test
	public void testIsRuriKnownForInputPost() throws Exception {
		classUnderTest.isRuriKnownForInputPost(ruri);
		boolean result = classUnderTest.isRuriKnownForInputPost(null);
		TestCase.assertEquals(false, result);
	}	
	
	@Test
	public void testIsRuriKnownForOutputGet() throws Exception {
		classUnderTest.isRuriKnownForOutputGet(ruri);
		boolean result = classUnderTest.isRuriKnownForOutputGet(null);
		TestCase.assertEquals(false, result);
	}
	
	@Test
	public void testIsRuriSubPropertyOf() throws Exception {
		boolean result = classUnderTest.isRuriSubPropteryOf(ruri, null);
		TestCase.assertEquals(false,  result);
		result = classUnderTest.isRuriSubPropteryOf(ruri, ruri);
		TestCase.assertEquals(true, result);
		result = classUnderTest.isRuriSubPropteryOf(ruri, ruri2);
		TestCase.assertEquals(false, result);
	}
	
	@Test
	public void testIsEncapsulatedRuriKnownForOutput() throws Exception {
		boolean result = classUnderTest.isEncapsulatedRuriKnownForOutput(ruri);
		TestCase.assertEquals(false,  result);
		result = classUnderTest.isEncapsulatedRuriKnownForOutput(null);
		TestCase.assertEquals(false, result);
	}
	
	@Test
	public void testRemove() throws Exception {
		classUnderTest.remove(ruri);
		classUnderTest.remove(null);
	}
	
	@Test
	public void testGetProvideOperations() throws Exception {
		ArrayList<String> result = classUnderTest.getProvideOperations(null);
		TestCase.assertEquals(null, result);
		
		loadModel();
		Thread.sleep(50);
		result = classUnderTest.getProvideOperations(ruri);
		TestCase.assertEquals(1, result.size());
		classUnderTest.remove(dtaName);
	}
	
	@Test
	public void testGetProvideOperationsEncapsulated() throws Exception {
		ArrayList<String> result = classUnderTest.getProvideOperationsEncapsulated(null);
		TestCase.assertEquals(null, result);

		result = classUnderTest.getProvideOperationsEncapsulated(ruri);
		TestCase.assertEquals(0, result.size());
	}
	
	@Test
	public void testGetProvideDestinationQueue() throws Exception {
		String result = classUnderTest.getProvideDestinationQueue(null);
		TestCase.assertEquals(null, result);
		
		loadModel();
		Thread.sleep(50);
		result = classUnderTest.getProvideDestinationQueue(operation);
		TestCase.assertEquals(dest, result);
		classUnderTest.remove(dtaName);
	}
	
	@Test
	public void testGetProvideOperationInputObject() throws Exception {
		String result = classUnderTest.getProvideOperationInputObject(null);
		TestCase.assertEquals(null, result);
		
		loadModel();
		Thread.sleep(50);
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
	
	private void loadModel(){
		classUnderTest.add(dtaName, triples);
	}
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
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
		classUnderTest = null;
		cachingSys.shutdown();
	}
}
