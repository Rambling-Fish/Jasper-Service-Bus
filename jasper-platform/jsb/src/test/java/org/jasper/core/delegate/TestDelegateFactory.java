package org.jasper.core.delegate;

import static org.mockito.Mockito.when;

import java.net.InetAddress;

import junit.framework.TestCase;

import org.jasper.core.UDE;
import org.jasper.core.persistence.PersistenceFacade;
import org.junit.After;
import org.junit.Before;
//
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
//
public class TestDelegateFactory extends TestCase {
	@Mock private UDE mockUDE;
	private PersistenceFacade cachingSys;
	private Delegate delegate;
	private DelegateFactory classUnderTest;

	
	/*
	 * This tests the Delegate Factory constructor as well as
	 * creating a Delegate
	 */
	@Test
	public void testFactory() throws Exception{
		System.out.println("==============================");
		System.out.println("RUNNING DELEGATE FACTORY TESTS");
		System.out.println("==============================");
	
		
		classUnderTest = new DelegateFactory(mockUDE);
		delegate = classUnderTest.createDelegate();
	}

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		System.setProperty("delegate-property-file", "../zipRoot/jsb-core/config/delegate.properties");
		String ipAddr = InetAddress.getLocalHost().getHostAddress();
		cachingSys   = new PersistenceFacade(ipAddr, "testGroup", "testPassword");
		when(mockUDE.getCachingSys()).thenReturn(cachingSys);
	}

	@After
	public void tearDown() throws Exception {
		classUnderTest = null;
		delegate = null;
		cachingSys.shutdown();
		}
	
}
