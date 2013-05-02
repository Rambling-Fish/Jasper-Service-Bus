package org.mule.transport.jasperengine;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jasper.jLib.jCommons.admin.JasperAdminMessage;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Type;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Command;
import org.jasper.jLib.jAuth.JTALicense;
import org.jasper.jLib.jAuth.util.JAuthHelper;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.config.MuleProperties;
import org.mule.transport.jms.activemq.ActiveMQJmsConnector;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;

public class JasperEngineConnector extends ActiveMQJmsConnector{
	
	private String vendor;
	private String appName;
	private String version;
	private String deploymentId;
	private String uri;
	private String globalQueue;
	private JTALicense license;
	private String jasperEngineURL;
	private Map<String,String> endpointUriMap;
	
    /* This constant defines the main transport protocol identifier */
    public static final String JASPERENGINE = "jasperengine";
  
    public JasperEngineConnector(MuleContext context){
        super(context);
        endpointUriMap = new ConcurrentHashMap<String, String>();
    }

    //TODO possibly remove
    public String getProtocol(){
        return JASPERENGINE;
    }
    
    public void doInitialise(){
    	try {
			license = JAuthHelper.loadJTALicenseFromFile(muleContext.getRegistry().get(MuleProperties.APP_HOME_DIRECTORY_PROPERTY) + "/" + appName + JAuthHelper.JTA_LICENSE_FILE_SUFFIX);
			deploymentId = license.getDeploymentId();
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
    
    public void connect() {
    	try {
    		super.connect();
    		
    		if(endpointUriMap.size() > 0) {
    			publishURI();
    		}
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    	}
    	
    }
    
    /**
     * Synchronous JTAs need to notify the jasper core of the URI(s) that it
     * is providing. An admin message is sent to the core on the delegate
     * global queue that contains URI value and the JTAs replyToQueue that
     * the core will use to send requests for data to the JTA based on URI
     */
    private void publishURI() {
    	try {
    		String dest = getGlobalQueue();
    		
    		// Get the already established connection
    		Connection connection = getConnection();

    		// Create a Session
    		Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

    		// Create the destination (delegate global queue)
    		Destination destination = session.createQueue(dest);
        
    		// Create a MessageProducer from the Session to the Topic or Queue
    		MessageProducer producer = session.createProducer(destination);
    		producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
	
    		// JasperAdmin message is created for every inbound endpoint with a
    		// URI defined. The JTA's URI and replyToQueue are registered with
    		// the core
    		Iterator<String> it = endpointUriMap.keySet().iterator();
    		while(it.hasNext()) {
    			String uri = (String)it.next();
    			String queue = endpointUriMap.get(uri);
    			String jtaName = (vendor + ":" + appName + ":" + version + ":" + deploymentId);
    			JasperAdminMessage jam = new JasperAdminMessage(Type.jtaDataManagement, Command.notify, queue, jtaName,  uri);
    			Message message = session.createObjectMessage(jam);
    			producer.send(message);
    		}

    		// Clean up - no need to close connection as that will be done in JasperBroker
    		session.close();
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    	}
    	
    }
    
    public void registerInboundEndpointUri(String endpoint, String uri){
    	endpointUriMap.put(uri, endpoint);
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

	public String getGlobalQueue() {
		return globalQueue;
	}
	
	public void setGlobalQueue(String queue) {
		this.globalQueue = queue;
	}
	
}
