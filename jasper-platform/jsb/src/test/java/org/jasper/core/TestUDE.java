package org.jasper.core;

import java.util.Properties;
import java.util.UUID;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

//
//
public class TestUDE extends TestCase {
	Properties props = new Properties();
	private String keystore = "./src/test/java/org/jasper/core/";
	private UDE classUnderTest;
	
	
	/*
	 * This test creates a new UDE class via constructor then 
	 * starts and stops it with clusterEnabled set to false
	 */
	@Test
	public void testClusterDisabled() throws Exception {
		System.out.println("=================");
		System.out.println("RUNNING UDE TESTS");
		System.out.println("=================");
		classUnderTest = new UDE(props);
		classUnderTest.start();
		String deployId = classUnderTest.getDeploymentID();
		String deployAndInst = classUnderTest.getUdeDeploymentAndInstance();
		TestCase.assertEquals(deployId, "junitTest");	
		TestCase.assertEquals(deployAndInst, "junitTest:0");
	}
	
	/*
	 * This test creates a new UDE class via constructor then 
	 * starts and stops it but sets clusterEnabled to true
	 */
	@Test
	public void testClusterEnabled() throws Exception {
	    props.setProperty("jsbClusterEnabled", "true");
	    props.setProperty("persisitence.groupName", UUID.randomUUID().toString());
		classUnderTest = new UDE(props);
		classUnderTest.start();
	}
	
	@Test
	public void testRemoteUDEConnectionDropped(){
		classUnderTest = new UDE(props);
		classUnderTest.start();
		classUnderTest.remoteUdeConnectionDropped("client", "0.0.0.0", "username", "password");
	}
	
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		System.setProperty("delegate-property-file", "../zipRoot/jsb-core/config/delegate.properties");
		props.put("jsb-keystore", keystore);
	    props.setProperty("jsbClusterEnabled", "false");
	}
	
	@After
	public void tearDown(){
		classUnderTest.stop();
	}
}
