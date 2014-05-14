package org.jasper.core.delegate;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import junit.framework.TestCase;

import org.jasper.core.constants.JasperOntologyConstants;
import org.jasper.core.persistence.PersistenceFacade;
import org.jasper.core.persistence.PersistenceFacadeFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.JsonObject;
import com.hazelcast.core.EntryEvent;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class TestDelegateOntology {
	private static PersistenceFacade cachingSys;
	private static OntModel model;
	private static String ipAddr;
	private static String hazelcastGroup = UUID.randomUUID().toString();
	private static DelegateOntology classUnderTest;
	private String ruri = "http://coralcea.ca/jasper/hrData";
	private String ruri2 = "http://coralcea.ca/jasper/getBpData";
	private String publishUri = "http://coralcea.ca/jasper/sendSms";
	private String operation = "http://coralcea.ca/jasper/getHrData";
	private String inputObject = "http://coralcea.ca/jasper/HrDataReq";
	private static String dtaName = "jasper:dta-heart-rate-monitor-D:1.0:jasperLab";
	private static String dtaName2 = "jasper:dta-sms-terminator:1.0:rayLab";
	private String dest = "dta-heart-rate-monitor-d-gethrdata";
	private String triples = "@prefix :  <http://coralcea.ca/jasper/> .\n@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .\n@prefix hrSensor: <http://coralcea.ca/jasper/hrSensor/> .\n@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .\n@prefix owl:   <http://www.w3.org/2002/07/owl#> .\n@prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n@prefix dta:   <http://coralcea.ca/2014/01/dta#> .\n\n:MsData  a  owl:Class .\n\n:timestamp  a  owl:DatatypeProperty ;\n  rdfs:domain  :HrData ;\n rdfs:range   xsd:string .\n\n:hrSID  a owl:DatatypeProperty ;\n rdfs:domain  :HrDataReq ;\n  rdfs:range   xsd:string .\n\n:HrData  a owl:Class ;\n rdfs:subClassOf  :MsData ;\n  dta:restriction  [ a  owl:Restriction ;\n  owl:onProperty :timestamp\n  ] ;\n dta:restriction  [ a  owl:Restriction ;\n owl:onProperty   hrSensor:bpm\n ] .\n\n:dta-heart-rate-monitor-D\n a dta:DTA ;\n dta:operation  :getHrData .\n\n:hrData  a owl:ObjectProperty ;\n  rdfs:range :HrData ;\n  rdfs:subPropertyOf  :msData .\n\n:msData  a owl:ObjectProperty ;\n rdfs:range  :MsData .\n\n:HrDataReq  a owl:Class ;\n  dta:restriction  [ a owl:Restriction ;\n owl:onProperty   :hrSID;\n ] .\n\nhrSensor:bpm  a owl:DatatypeProperty ;\n  rdfs:domain  :HrData ;\n rdfs:range   xsd:integer .\n\n:getHrData  a dta:Operation ;\n dta:destination  'dta-heart-rate-monitor-d-gethrdata' ;\n dta:input :HrDataReq ;\n dta:kind dta:Get ;\n  dta:output :hrData .\n";	
	private String triples2 = "@prefix : <http://coralcea.ca/jasper/Sms/> .\n@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .\n@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .\n@prefix owl:   <http://www.w3.org/2002/07/owl#> .\n@prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n@prefix jasper: <http://coralcea.ca/jasper/> .\n@prefix dta:   <http://coralcea.ca/2014/01/dta#> .\n\n:SmsPostReq  a     owl:Class ;\n  dta:restriction  [ a  owl:Restriction ;\n   owl:onProperty   :bodySms\n  ] ;\n  dta:restriction  [ a  owl:Restriction ;\n    owl:onProperty   :fromSms\n   ] ;\n  dta:restriction  [ a  owl:Restriction ;\n  owl:onProperty  :logId\n ] ;\n  dta:restriction  [ a  owl:Restriction ;\n  owl:onProperty   :toSms\n  ] .\n\n:dta-sms-terminator  a  dta:DTA ;\n dta:operation  :sendSms .\n\n:sendSms  a  dta:Operation ;\n  dta:destination  'dta-sms-terminator-sendsms' ;\n  dta:input  :SmsPostReq ;\n  dta:kind  dta:Post .\n\n:fromSms  a owl:DatatypeProperty ;\n  rdfs:domain  :SmsPostReq ;\n  rdfs:range   xsd:string .\n\n:logId  a  owl:DatatypeProperty ;\n rdfs:domain  :SmsPostReq ;\n  rdfs:range   xsd:string .\n\n:toSms  a owl:DatatypeProperty ;\n  rdfs:domain  :SmsPostReq ;\n rdfs:range   xsd:string .\n\n:bodySms  a owl:DatatypeProperty ;\n rdfs:domain  :SmsPostReq ;\n rdfs:range   xsd:string .\n";	

	
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
		
		//TODO get non clustered working for all TCs. Currently exception is thrown due to EntryEvent having null source
		// Is it possible to add an event listener to a non multiMap?
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
	}
	
	@Test
	public void testRemove() throws Exception {
		classUnderTest.remove(dtaName);
		classUnderTest.remove(null);
	}
	
	@Test
	public void  testIsRuriKnownForInputPublish(){
		loadModel();
		boolean result = classUnderTest.isRuriKnownForInputPublish(publishUri);
		TestCase.assertEquals(false, result);
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
		
		String result = classUnderTest.getProvideDestinationQueue(null);
		TestCase.assertEquals(null, result);
		result = classUnderTest.getProvideDestinationQueue(operation);
		TestCase.assertEquals(dest, result);
		
		result = classUnderTest.getProvideOperationInputObject(null);
		TestCase.assertEquals(null, result);
		result = classUnderTest.getProvideOperationInputObject(operation);
		TestCase.assertEquals(inputObject, result);
		
		String[] strArr = classUnderTest.getSerializedModels();
		TestCase.assertNotNull(strArr);
		
		Set<String>resultSet = classUnderTest.getSuperProperties(ruri);
		TestCase.assertEquals(1, resultSet.size());
		Iterator<String> it = resultSet.iterator();
		String superRuri = null;
		while(it.hasNext()){
			superRuri = it.next();
		}
		resultSet.clear();
		resultSet = classUnderTest.getEquivalentProperties(superRuri);
		//TODO fix this when Maged/Abe fix code. This should not be null
		TestCase.assertNull(resultSet);
		
		ArrayList<String> arrList = classUnderTest.fetchPostOperations(publishUri);
		TestCase.assertNotNull(arrList);
		arrList = classUnderTest.fetchPostOperations(null);
		TestCase.assertNull(arrList);
		
		String oper = "http://coralcea.ca/jasper/Sms/sendSms";
		String inputObj = classUnderTest.fetchPostOperationInputObject(oper);
		TestCase.assertEquals("http://coralcea.ca/jasper/Sms/SmsPostReq", inputObj);
		inputObj = classUnderTest.fetchPostOperationInputObject(null);
		TestCase.assertNull(inputObj);
		
		String destQ = classUnderTest.fetchPostDestinationQueue(oper);
		TestCase.assertEquals("dta-sms-terminator-sendsms", destQ);
		destQ = classUnderTest.fetchPostDestinationQueue(null);
		TestCase.assertNull(destQ);
			
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
	
	
	private void loadModel(){
		classUnderTest.add(dtaName, triples);
		classUnderTest.add(dtaName2, triples2);
	}
	
	@BeforeClass
	public static void setUpClass() throws Exception {
		model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
		for(String prefix:JasperOntologyConstants.PREFIX_MAP.keySet()){
        	model.setNsPrefix(prefix, JasperOntologyConstants.PREFIX_MAP.get(prefix));
        }
		
		ipAddr = InetAddress.getLocalHost().getHostAddress();
		cachingSys = PersistenceFacadeFactory.getFacade(ipAddr, hazelcastGroup, "testPasswd");
		classUnderTest = new DelegateOntology(cachingSys, model);
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
		classUnderTest.remove(dtaName);
		classUnderTest.remove(dtaName2);
		classUnderTest = null;
		cachingSys.shutdown();
	}
}
