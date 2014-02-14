package org.jasper.core;

import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.util.Properties;

import junit.framework.TestCase;

import org.jasper.core.persistence.PersistenceFacade;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

//
//
public class TestUDECore extends TestCase {
	Properties props = new Properties();
	@Mock private UDE mockUDE;
	private UDECore classUnderTest;
	private PersistenceFacade cachingSys;
	
	
	
	@Test
	public void testCleanStartAndStop() throws Exception {
		System.out.println("======================");
		System.out.println("RUNNING UDE CORE TESTS");
		System.out.println("======================");
		classUnderTest.start();
		classUnderTest.stop();
	}
	
	@Test
	public void testNumberFormatException() throws Exception{
		props.put("numDelegates","T");
		
		classUnderTest.start();
		classUnderTest.stop();
	}
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		String ipAddr = InetAddress.getLocalHost().getHostAddress();
		cachingSys = new PersistenceFacade(ipAddr, "testGroup", "testPassword");
	    props.put("numDelegates","5");
		when(mockUDE.getCachingSys()).thenReturn(cachingSys);
		
		classUnderTest = new UDECore(mockUDE, props);
	}

	@After
	public void tearDown() throws Exception {
		cachingSys.shutdown();
		classUnderTest = null;
		
	}

}
