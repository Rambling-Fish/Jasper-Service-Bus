package org.jasper.core.constants;


public class JasperPEPConstants {
	
	public static final String PDP_AUTH_ADMIN_URL        = "pdpAuthAdminURL";
	public static final String PDP_SERVER_USER           = "pdpServiceUserName";
	public static final String PDP_SERVER_PASSWORD       = "pdpServicePassword";
	public static final String PDP_SERVICE_URL           = "pdpServiceURL";
	public static final String REUSE_SESSION             = "reuseSession";
	public static final String DEFAULT_POLICY_DECISION   = "isDefaultPolicyAllow";
	public static final String HTTPS_TRANSPORT           = "https";
	public static final String AUTH_SERVICE              = "AuthenticationAdmin"; 
	public static final String ENTITLEMENT_SERVICE       = "EntitlementService";
	public static final String PDP_SERVER_KEYSTORE       = "wso2carbon.jks";
	public static final String PDP_KEYSTORE_PASSWORD     = "wso2carbon";
	public static final String PDP_TRUSTSTORE_TYPE       = "JKS";
	public static final String REQUEST_WRAPPER           = "requestWrapper";
    public static final String REQUIRED_ATTRIBUTE_VALUES = "requiredAttributeValues";
    public static final String SUBJECT_ATTRIBUTE         = "subjectValues";
    public static final String RESOURCE_ATTRIBUTE        = "resourceValues";
    public static final String ACTION_ATTRIBUTE          = "actionValues";
    public static final String RESULT_DENY               = "Deny";
    public static final String RESULT_PERMIT             = "Permit";
    public static final String RESULT_NOTAPPLICABLE      = "NotApplicable";
    public static final String RESULT_INDETERMINATE      = "Indeterminate";
    public static final String TRUST_STORE               = "javax.net.ssl.trustStore";
    public static final String TRUST_STORE_PASSWORD      = "javax.net.ssl.trustStorePassword";
    public static final String TRUST_STORE_TYPE          = "javax.net.ssl.trustStoreType";
    public static final int THRIFT_TIME_OUT              = 30000;
    public static final int DEFAULT_THRIFT_PORT          = 10500;

    // XACML request variables

    public static final String CATEGORY_SUBJECT = "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject";
    public static final String CATEGORY_ACTION = "urn:oasis:names:tc:xacml:3.0:attribute-category:action";
    public static final String CATEGORY_RESOURCE = "urn:oasis:names:tc:xacml:3.0:attribute-category:resource";
    public static final String CATEGORY_ENVIRONMENT = "urn:oasis:names:tc:xacml:3.0:attribute-category:environment";
    public static final String ATTRIBUTE_VALUE = "AttributeValue";
    public static final String  ATTRIBUTE_ID = "AttributeId";
    public static final String  ATTRIBUTE = "Attribute";
    public static final String  ATTRIBUTES = "Attributes";
    public static final String  CATEGORY = "Category";
    public static final String  DATA_TYPE = "DataType";
    public static final String STRING_DATA_TYPE = "http://www.w3.org/2001/XMLSchema#string";
    public static final String SUBJECT_ID = "urn:oasis:names:tc:xacml:1.0:subject:subject-id";
    public static final String RESOURCE_ID = "urn:oasis:names:tc:xacml:1.0:resource:resource-id";
    public static final String ENVIRONMENT_ID = "urn:oasis:names:tc:xacml:1.0:environment:environment-id";
    public static final String ACTION_ID = "urn:oasis:names:tc:xacml:1.0:action:action-id";
    public static final String REQ_RES_CONTEXT = "urn:oasis:names:tc:xacml:3.0:core:schema:wd-17";
    public static final String REQ_SCHEME = "http://www.w3.org/2001/XMLSchema-instance";
    public static final String REQUEST_ELEMENT = "Request";

}