package org.mule.transport.jasperengine;

import org.mule.api.MuleContext;
import org.mule.transport.jms.activemq.ActiveMQJmsConnector;

public class JasperEngineConnector extends ActiveMQJmsConnector{
	
	private String vendor;
	private String appName;
	private String version;
	private String deploymentId;
	private String jAppAuthKey;
	
	private String jasperEngineURL;
	
    /* This constant defines the main transport protocol identifier */
    public static final String JASPERENGINE = "jasperengine";
  
    public JasperEngineConnector(MuleContext context){
        super(context);
    }

    //TODO possibly remove
    public String getProtocol(){
        return JASPERENGINE;
    }

    private String generateUsername(){
    	if(vendor == null && appName == null && version == null && deploymentId == null) return null;
    	
    	StringBuffer sb = new StringBuffer();
    	if(vendor != null) sb.append(vendor);
    	sb.append(":");
    	if(appName != null) sb.append(appName);
    	sb.append(":");
    	if(version != null) sb.append(version);
    	sb.append(":");
    	if(deploymentId != null) sb.append(deploymentId);
    	return sb.toString();
    }
    
	public String getVendor() {
		return vendor;
	}

	public void setVendor(String vendor) {
		this.vendor = vendor;
		this.username = generateUsername();
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
		this.username = generateUsername();
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
		this.username = generateUsername();
	}

	public String getDeploymentId() {
		return deploymentId;
	}

	public void setDeploymentId(String deploymentId) {
		this.deploymentId = deploymentId;
		this.username = generateUsername();
	}

	public String getjAppAuthKey() {
		return jAppAuthKey;
	}

	public void setjAppAuthKey(String jAppAuthKey) {
		this.jAppAuthKey = jAppAuthKey;
		this.password = jAppAuthKey;
	}
	
	public String getJasperEngineURL() {
		return jasperEngineURL;
	}

	public void setJasperEngineURL(String jasperEngineURL) {
		this.jasperEngineURL = jasperEngineURL;
		super.setBrokerURL(jasperEngineURL);
	}

	public void setBrokerURL(String url){
        if(jasperEngineURL == null){
        	super.setBrokerURL(url);
        }else{
    		//TODO ADD CUSTOMER LOG THAT PASSWORD IS IGNORED
        	super.setBrokerURL(jasperEngineURL);
        	System.out.println("brokerURL " + url + " is ignored, instead brokerURL set to jasperEngineURL " + jasperEngineURL);
        }
	}
	
    public void setUsername(String username){
    	String generatedUsername = generateUsername();
    	if(generatedUsername == null){
    		super.setUsername(username);
    	}else{
    		//TODO ADD CUSTOMER LOG THAT USERNAME IS IGNORED
    		super.setUsername(generatedUsername);
    		System.out.println("username " + username + " is ignored, instead username set to " + this.username);
    	}
    }

    public void setPassword(String password){
        if(jAppAuthKey == null){
        	super.setPassword(password);
        }else{
    		//TODO ADD CUSTOMER LOG THAT PASSWORD IS IGNORED
        	super.setPassword(jAppAuthKey);
        	System.out.println("password " + password + " is ignored, instead password set to " + this.password);
        }
    }
	
}
