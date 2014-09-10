package org.jasper.core.acl.pep;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.transport.TransportSender;
import org.apache.axis2.transport.http.CommonsHTTPTransportSender;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.log4j.Logger;
import org.jasper.core.acl.pep.utils.AttributeValueDTO;
import org.jasper.core.acl.pep.utils.XACMLRequestBuilder;
import org.jasper.core.constants.JasperPEPConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.wso2.carbon.authenticator.stub.AuthenticationAdminStub;
import org.wso2.carbon.identity.entitlement.stub.EntitlementServiceStub;
import org.xml.sax.InputSource;

public class JasperPEP {
	static Logger logger = Logger.getLogger(JasperPEP.class.getName());
	private static JasperPEP pep;
	private String authCookie;

	private String pdpServiceURL;
	private ConfigurationContext configCtx;
	private String userName;
	private String password;
	private String authAdminURL;
	private TransportOutDescription transportOut;
	private Properties props = new Properties();
	private TransportSender sender;
	private boolean reuseSession;
	private boolean isAuthenticated = false;
	private boolean defaultPolicyDecision = false;

	private JasperPEP() {
		
	}

	public void init() {
		try {
			props.load(new FileInputStream(System.getProperty("pdp-property-file")));
		} catch (IOException ex) {
			logger.error("Unable to load PDP properties", ex);
		}

		transportOut = new TransportOutDescription(JasperPEPConstants.HTTPS_TRANSPORT);
		password = props.getProperty(JasperPEPConstants.PDP_SERVER_PASSWORD);
		userName = props.getProperty(JasperPEPConstants.PDP_SERVER_USER);
		pdpServiceURL = props.getProperty(JasperPEPConstants.PDP_SERVICE_URL);
		authAdminURL = props.getProperty(JasperPEPConstants.PDP_AUTH_ADMIN_URL);
		reuseSession = props.getProperty(JasperPEPConstants.REUSE_SESSION, "true").equalsIgnoreCase("true");
		defaultPolicyDecision = props.getProperty(JasperPEPConstants.DEFAULT_POLICY_DECISION, "false").equalsIgnoreCase("true");
		
		if (pdpServiceURL != null) {
			pdpServiceURL = pdpServiceURL.trim();
			if (!pdpServiceURL.endsWith("/")) {
				pdpServiceURL += "/";
			}
		}
		
		//set keystore to enable SSL communications with PDP server
		System.setProperty("javax.net.ssl.trustStore", System.getProperty("jsb-keystore") + JasperPEPConstants.PDP_SERVER_KEYSTORE);
		System.setProperty("javax.net.ssl.trustStorePassword", JasperPEPConstants.PDP_KEYSTORE_PASSWORD);
		System.setProperty("javax.net.ssl.trustStoreType", JasperPEPConstants.PDP_TRUSTSTORE_TYPE);

		try {
			configCtx = ConfigurationContextFactory.createEmptyConfigurationContext();
			sender = new CommonsHTTPTransportSender();
			sender.init(configCtx, transportOut);
			transportOut.setSender(sender);
		} catch (AxisFault af) {
			logger.error("Error creating PDP configuration context " + af);
		}

	}

	public static JasperPEP getInstance() {
		if (pep == null) {
			pep = new JasperPEP();
			pep.init();
		}
		return pep;
	}

	public boolean authorizeRequest(String subject, String resource, String action) {
		Map<String,String> result = new HashMap<String,String>();
		if(! reuseSession) {
			isAuthenticated = false;
		}
		try {
			if(! isAuthenticated) {
				authenticate();
			}
		
			if(isAuthenticated){
				String xacmlRequest = buildRequest(subject, resource, action);
				if(xacmlRequest == null){
					logger.error("Error creating XACML request");
					return false;
				}
				if(logger.isDebugEnabled()) logger.debug("XACML request = " + xacmlRequest);
				String decision = getDecision(xacmlRequest);
				if(decision == null){
					logger.error("XACML response is null - ensure that the PDP is running");
					return false;
				}
				
				result = parseDecision(decision);
				if(result.containsValue(JasperPEPConstants.RESULT_DENY)){
					return false;
				}
				else if(result.containsValue(JasperPEPConstants.RESULT_NOTAPPLICABLE)){
					return defaultPolicyDecision;
				}
				else if(result.containsValue(JasperPEPConstants.RESULT_INDETERMINATE)){
					return false;
				}
				else if(result.containsValue(JasperPEPConstants.RESULT_PERMIT)){
					return true;
				}
			}
			else{
				logger.error("Could not authenticate user on PDP - all requests will be denied until issue is resolved");
				return false;
			}
		} catch (Exception e) {
			logger.error("Error logging into PDP for authentication ", e);
		}

		return false;

	}

	/**
	 * authenticates with WSO2 Identity Server using authentication admin
	 * service This must be done for each request
	 * 
	 * @return true/false
	 */
	private boolean authenticate() {
		String serviceURL = null;
		ServiceClient client = null;
		Options option = null;
		AuthenticationAdminStub authAdminStub = null;
		serviceURL = pdpServiceURL + JasperPEPConstants.AUTH_SERVICE;
		
		try{
			authAdminStub = new AuthenticationAdminStub(configCtx, serviceURL);
			client = authAdminStub._getServiceClient();
			option = client.getOptions();
			option.setManageSession(true);
			option.setTransportOut(transportOut);

			isAuthenticated = authAdminStub.login(userName, password, authAdminURL);
			authCookie = (String) authAdminStub._getServiceClient().getServiceContext().getProperty(HTTPConstants.COOKIE_STRING);
			option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, authCookie);
		} catch(Exception e){
			logger.error("Exception occured while logging into PDP - ensure that PDP is running", e);
			return false;
		}

		return isAuthenticated;
	}
	
	private String buildRequest(String subject, String resource, String action) {
		Set<AttributeValueDTO> valueDTOs = new HashSet<AttributeValueDTO>();
		XACMLRequestBuilder reqBuilder = new XACMLRequestBuilder();
		Set<AttributeValueDTO> subjectAttributes = reqBuilder.getSubjectAttributeValues(subject);
		Set<AttributeValueDTO> actionAttributes = reqBuilder.getActionAttributeValues(action);
        Set<AttributeValueDTO> resourceAttributes = reqBuilder.getResourceAttributeValues(resource);
        
        valueDTOs.addAll(resourceAttributes);
        valueDTOs.addAll(subjectAttributes);
        valueDTOs.addAll(actionAttributes);
        
        try{
        	Document document = reqBuilder.createRequestElement(valueDTOs);
        	String request = reqBuilder.getStringFromDocument(document);
        	return request;
        }catch(Exception e){
        	logger.error("Exception occurred while creating XACML request", e);
        	return null;
        }

	}
    
    private Map<String,String> parseDecision(String decision) throws Exception{
    	Map<String, String> result = new HashMap<String, String>();
    	DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    	Document doc = dBuilder.parse( new InputSource( new StringReader( decision ) ) );
    	doc.getDocumentElement().normalize();
    	
    	NodeList decList = doc.getElementsByTagName("Decision");
    	NodeList attList = doc.getElementsByTagName("AttributeValue");

    	for (int temp = 0; temp < decList.getLength(); temp++) {
    		Node dNode = decList.item(temp);
    		Node aNode = attList.item(temp);
    		result.put(aNode.getFirstChild().getTextContent(), dNode.getFirstChild().getTextContent());
    	}
    	
    	return result;
    }
    	
	private String getDecision(String xacmlRequest) throws Exception{
		String decision;
		String serviceURL = pdpServiceURL + JasperPEPConstants.ENTITLEMENT_SERVICE;
		EntitlementServiceStub stub = new EntitlementServiceStub(configCtx, serviceURL);
		ServiceClient client = stub._getServiceClient();
		Options option = client.getOptions();
		option.setManageSession(true);
		option.setProperty(HTTPConstants.COOKIE_STRING, authCookie);
		option.setTransportOut(transportOut);
				
		try {
			decision = stub.getDecision(xacmlRequest);
			if(logger.isDebugEnabled()) logger.debug("PDP Decision = " + decision);
			return decision;
		} catch (Exception e) {
			logger.error("Error sending XACML authorization request, ensure that PDP is running ", e);
			isAuthenticated = false;
			return null;		
		} finally{
			stub.cleanup();
		}
	}

}