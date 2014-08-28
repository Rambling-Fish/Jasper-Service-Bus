package org.jasper.core.constants;



public class JasperConstants {

	public  static final String DELEGATE_QUEUE_PREFIX      = "jms.jasper.delegate.";
	public  static final String DELEGATE_QUEUE_SUFFIX      = ".queue";
	public  static final String DELEGATE_GLOBAL_QUEUE      = "jms.jasper.delegate.global.queue";
	public  static final String DELEGATE_DEFAULT_NAME      = "jasperDelegate";
	public  static final String JASPER_ADMIN_USERNAME      = "jasperAdminUsername";
	public  static final String JASPER_ADMIN_PASSWORD      = "jasperAdminPassword";
	public  static final String JASPER_ADMIN_TOPIC         = "jms.jasper.admin.messages.topic";
	public  static final String JASPER_ADMIN_QUEUE_PREFIX  = "jms.jasper.admin.messages.queue.";
	public  static final String JTA_QUEUE_PREFIX           = "jms.";
	
	// constants for JSON requests/responses
	public static final String VERSION_LABEL = "version";
	public static final String VERSION_1_0 = "1.0";	
	public static final String METHOD_LABEL = "method";
	public static final String REQUEST_URI_LABEL = "ruri";
	public static final String HEADERS_LABEL = "headers";
	public static final String PARAMETERS_LABEL = "parameters";
	public static final String RULE_LABEL = "rule";
	public static final String PAYLOAD_LABEL = "payload";
	public static final String CODE_LABEL = "code";
	public static final String REASON_LABEL = "reason";
	public static final String DESCRIPTION_LABEL = "description";
	public static final String CONTENT_TYPE_LABEL = "content-type";
	public static final String ENCODING_LABEL = "UTF-8";
	public static final String POLL_PERIOD_LABEL = "poll-period";
	public static final String EXPIRES_LABEL = "expires";
	public static final String DEFAULT_PROCESSING_SCHEME = "aggregate";
	public static final String AGGREGATE_SCHEME = "aggregate";
	public static final String COALESCE_SCHEME = "coalesce";
	public static final String RESPONSE_TYPE_LABEL = "response-type";
	public static final String SUBSCRIPTION_ID_LABEL = "subscription-id";
	public static final String DEFAULT_RESPONSE_TYPE = "application/json";
	
	public static final String GET = "get";
	public static final String POST = "post";
	public static final String PUBLISH = "publish";
	public static final String SUBSCRIBE = "subscribe";
	
	public enum ResponseCodes{
		OK(200, "OK"),
		ACCEPTED(202, "Accepted"),
		BADREQUEST(400, "Bad Request"),
		FORBIDDEN(403,"Forbidden"),
		NOTFOUND(404, "Not Found"),
		TIMEOUT(408, "Request timeout"),
		SERVERERROR(500, "Server Internal Error");
		
		private int code;
		private String description;
		
		private ResponseCodes(int code, String description){
			this.code = code;
			this.description = description;
		}
		
		public void setCode(int code){
			this.code = code;
		}
		
		public String getDescription(){
			return description;
		}
		
		public int getCode(){
			return code;
		}
		
		@Override
		public String toString() {
			return code + ": " + description;
		}
		
	};
	
}
