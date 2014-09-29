package org.jasper.core.acl.pep;

import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.jasper.core.acl.pep.soap.JasperPEP;
import org.jasper.core.acl.pep.utils.AttributeValueDTO;
import org.jasper.core.acl.pep.utils.XACMLRequestBuilder;
import org.jasper.core.constants.JasperPEPConstants;
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
	private String authURL = "0.0.0.0";
	
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
		String[] environment = new String[2];
		environment[0] = "timeofDay";
		environment[1] = "dayofWeek";
		when(mockAuthStub.login(user, password, authURL)).thenReturn(true);
		boolean authorize = classUnderTest.authorizeRequest(subject, hrResource, action, environment);
		TestCase.assertEquals(false, authorize);
		
	}
	
	/*
	 * This tests the AttributeValueDTO class which is used to
	 * store xacml request attributes
	 */
	@Test
	public void testAttributeValueDTO(){
		AttributeValueDTO myAttribs = new AttributeValueDTO();
		myAttribs.setCategory(JasperPEPConstants.CATEGORY_ENVIRONMENT);
		myAttribs.setValue("12:00");
		String value = myAttribs.getValue();
		String category = myAttribs.getCategory();
		TestCase.assertEquals("12:00", value);
		TestCase.assertEquals(JasperPEPConstants.CATEGORY_ENVIRONMENT, category);
	}
	
	/*
	 * This tests the XACMLRequestBulder class which is
	 * used to build an xacml request and parse an xacml response into
	 * a Map
	 */
	@Test
	public void testXACMLBuilder(){
		String action = "GET";
		String resource = "http://coralcea.ca/jasper/hrData";
		String subject = "DTA-D";
		String[] environment = new String[2];
		environment = new String[2];
		environment[0] = "timeofDay";
		environment[1] = "dayofWeek";
		String request = XACMLRequestBuilder.buildRequest(subject, resource, action, environment);
		TestCase.assertNotNull(request);
		String decision = "<Response xmlns=\"urn:oasis:names:tc:xacml:3.0:core:schema:wd-17\">" +
		"<Result><Decision>Permit</Decision><Status><StatusCode Value=\"urn:oasis:names:tc:xacml:1.0:status:ok\"/>" + 
		"</Status><Attributes Category=\"urn:oasis:names:tc:xacml:3.0:attribute-category:resource\">" + 
		"<Attribute AttributeId=\"urn:oasis:names:tc:xacml:1.0:resource:resource-id\" IncludeInResult=\"true\">" +
        "<AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">http://coralcea.ca/jasper/hrData</AttributeValue></Attribute></Attributes></Result></Response>";
		request = XACMLRequestBuilder.buildRequest(null,  null,  null, null);
		Map<String,String> myMap = new HashMap<String,String>();
		try {
			myMap = XACMLRequestBuilder.parseDecision(decision);
			TestCase.assertEquals(true, myMap.containsValue("Permit"));
		} catch (Exception e) {
			TestCase.fail("Exception during parseDecision");
		}
		
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
