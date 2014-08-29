package org.jasper.core.acl.pep;

import static org.mockito.Mockito.when;
import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.wso2.carbon.authenticator.stub.AuthenticationAdminStub;


public class TestPEP extends TestCase {
	@Mock private AuthenticationAdminStub mockAuthStub;
	private JasperPEP classUnderTest;
	private String action = "GET";
	private String hrResource = "http://coralcea.ca/jasper/hrData";
	private String subject = "dta";
	private String user = "admin";
	private String password = "admin";
	private String authURL = "192.168.10.226";
	
	
	
	/*
	 * This test creates a new JasperPEP class and initializes it 
	 */
	@Test
	public void testPEPInitialization() throws Exception {
		System.out.println("=======================");
		System.out.println("RUNNING JasperPEP TESTS");
		System.out.println("=======================");

		classUnderTest = JasperPEP.getInstance();
		
	}
	
	/*
	 * This testcase tests JasperPEP config file not found 
	 */
	@Test
	public void testPEPConfigError() throws Exception {
		System.setProperty("pdp-property-file", "../zipRoot/jsb-core/pdp.properties");
		classUnderTest = JasperPEP.getInstance();
		System.setProperty("pdp-property-file", "./src/test/resources/pdp.properties");
		classUnderTest.init();
		
	}
	
	/*
	 * This testcase tests JasperPEP authenticating on the PDP server 
	 */
	@Test
	public void testPDPAuthentication() throws Exception {
		classUnderTest = JasperPEP.getInstance();
		when(mockAuthStub.login(user, password, authURL)).thenReturn(true);
		boolean authorize = classUnderTest.authorizeRequest(subject, hrResource, action);
		TestCase.assertEquals(false, authorize);
		
	}
	
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		System.setProperty("delegate-property-file", "../zipRoot/jsb-core/config/delegate.properties");
		System.setProperty("pdp-property-file", "../zipRoot/jsb-core/config/pdp.properties");

	}
	
	@After
	public void tearDown(){
		classUnderTest = null;
	}
}
