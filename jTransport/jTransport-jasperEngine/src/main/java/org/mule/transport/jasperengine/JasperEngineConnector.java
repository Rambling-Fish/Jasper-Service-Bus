package org.mule.transport.jasperengine;

import java.io.IOException;

import org.jasper.jLib.jAuth.JTALicense;
import org.jasper.jLib.jAuth.util.JAuthHelper;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.config.MuleProperties;
import org.mule.transport.jms.activemq.ActiveMQJmsConnector;

public class JasperEngineConnector extends ActiveMQJmsConnector{
	
	private String vendor;
	private String appName;
	private String version;
	private String deploymentId;
	private JTALicense license;
	
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
    
    public void doInitialise(){
    	try {
			license = JAuthHelper.loadJTALicenseFromFile(muleContext.getRegistry().get(MuleProperties.APP_HOME_DIRECTORY_PROPERTY) + "/" + appName + JAuthHelper.JTA_LICENSE_FILE_SUFFIX);
			if(!validJTALicense()){
				throw new DefaultMuleException("Invalid JTA license key");
			}else{				
				setPassword(JAuthHelper.bytesToHex((license.getLicenseKey())));
				setBrokerURL(jasperEngineURL);
				setUsername(vendor + ":" + appName + ":" + version + ":" + deploymentId);
				super.doInitialise();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (MuleException e) {
			e.printStackTrace();
		}
    }
    
    private boolean validJTALicense() {
    	return ( (license.getVendor().equals(vendor)) && 
    			 (license.getAppName().equals(appName)) &&
    			 (license.getVersion().equals(version)) &&
    			 (license.getDeploymentId().equals(deploymentId)) );
	}
  
	public String getVendor() {
		return vendor;
	}

	public void setVendor(String vendor) {
		this.vendor = vendor;
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getDeploymentId() {
		return deploymentId;
	}

	public void setDeploymentId(String deploymentId) {
		this.deploymentId = deploymentId;
	}

	public String getJasperEngineURL() {
		return jasperEngineURL;
	}

	public void setJasperEngineURL(String jasperEngineURL) {
		this.jasperEngineURL = jasperEngineURL;
	}
}
