package org.jasper.core.acl.pep;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

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
import org.wso2.carbon.authenticator.stub.AuthenticationAdminStub;
import org.wso2.carbon.identity.entitlement.stub.EntitlementServiceStub;

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
		if(! reuseSession) {
			isAuthenticated = false;
		}
		try {
			if(! isAuthenticated) {
				authenticate();
			}
			
			if(isAuthenticated){
				String decision = getDecision(subject, resource, action);
				if(decision == null){
					logger.error("XACML response is null - ensure that the PDP is running");
					return false;
				}
				return (decision.contains("Permit") || decision.contains("NotApplicable"));
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
	
	private String getDecision(String subject, String resource, String action) throws Exception{
		String decision;
		String serviceURL = pdpServiceURL + JasperPEPConstants.ENTITLEMENT_SERVICE;
		EntitlementServiceStub stub = new EntitlementServiceStub(configCtx, serviceURL);
		ServiceClient client = stub._getServiceClient();
		Options option = client.getOptions();
		option.setManageSession(true);
		option.setProperty(HTTPConstants.COOKIE_STRING, authCookie);
		option.setTransportOut(transportOut);
				
		try {
			decision = stub.getDecisionByAttributes(subject, resource, action, null);
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