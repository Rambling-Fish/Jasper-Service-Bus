package org.mule.transport.jasperengine;

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

public class JasperEngineConnector extends ActiveMQJmsConnector{
	
	private String vendor;
	private String appName;
	private String version;
	private String deploymentId;
	private JTALicense license;
	private String jasperEngineURL;
	private Map<String,String> endpointUriMap;
	protected boolean isAdminHandlerStopped;
	private ExecutorService adminHandler;
	private Future adminTask;
	
    /* This constant defines the main transport protocol identifier */
    public static final String JASPERENGINE = "jasperengine";
    private static final String DELEGATE_GLOBAL_QUEUE = "jms.jasper.delegate.global.queue";
    private static final String JTA_QUEUE_SUFFIX = ".queue";
    private static final String JTA_QUEUE_PREFIX = "jms.";
  
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
    
    public void connect() throws Exception{
    	if(!willLicenseKeyExpireInDays(license, 0)){
    			try {
    				super.connect();
    		
    				if(endpointUriMap.size() > 0) {
    					initializeAdminHandler();
    				}	
    			}
    			catch (Exception e) {
    				e.printStackTrace();
    			}
    	}
    	else{
    		throw new DefaultMuleException("Invalid JTA license key");
    	}
    	
    }
    
    public void disconnect(){
    	if(adminHandler != null){
    		stopAdminHandler();
    	}
    }
    
    /**
     * Synchronous JTAs need to notify the jasper core of the URI(s) that it
     * is providing. An admin message is sent to the core on the delegate
     * global queue that contains URI value and the JTAs replyToQueue that
     * the core will use to send requests for data to the JTA based on URI
     */
    private void initializeAdminHandler() {
    	adminHandler = Executors.newSingleThreadExecutor();
    	isAdminHandlerStopped = false;
    	
    	Runnable task = new Runnable() {

			@Override
			public void run() {
				try {
					
					while(!isAdminHandlerStopped){
						try {
					  	     	
							Session adminSession = getConnection().createSession(false, Session.AUTO_ACKNOWLEDGE);
						      
						    // Create Queue
						    String queueName = JTA_QUEUE_PREFIX.concat(getVendor()).concat(".").concat(getAppName()).concat(".").concat(getVersion()).concat(JTA_QUEUE_SUFFIX);
						    Destination adminQueue = adminSession.createQueue(queueName);
						                  
						    // Create a MessageConsumer from the Session to the Queue
						    MessageConsumer adminConsumer = adminSession.createConsumer(adminQueue);

						    // Wait for a message
						    Message adminRequest;
						      
						    do{
						    	do{
						    		adminRequest = adminConsumer.receive(3000);
						    	}while(adminRequest == null && !isAdminHandlerStopped);
						    	if(isAdminHandlerStopped) break;
						          
						        if (adminRequest instanceof ObjectMessage) {
						        	ObjectMessage objMessage = (ObjectMessage) adminRequest;
						        	Object obj = objMessage.getObject();
						        	if(obj instanceof JasperAdminMessage){
						        		if(((JasperAdminMessage) obj).getType() == Type.jtaDataManagement && ((JasperAdminMessage) obj).getCommand() == Command.notify)
						        			publishURI();
						                }
						          }
						          
						      }while(!isAdminHandlerStopped);
						      
						      adminConsumer.close();
						      adminSession.close();
						     
						  } catch (Exception e) {
							  logger.error("Exception received while listening for admin message",e);
						      isAdminHandlerStopped = true;
						  }
					}
					
		    	}catch (Exception e) {
		    		e.printStackTrace();
		    	}
			}
		};;;
		adminTask = adminHandler.submit(task);
    }

    private void stopAdminHandler() {
    	isAdminHandlerStopped = true;
    	
    	int count = 10;
    	while(!adminTask.isDone() && count > 0){
    		try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			count--;
    	}
    	if(!adminTask.isDone()) adminTask.cancel(true);
    	adminHandler.shutdown();
    	try {
			adminHandler.awaitTermination(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	if(!adminHandler.isTerminated()) adminHandler.shutdownNow();
	} 	
    
    
    private void publishURI() {
    	try {
    		
    		// Get the already established connection
    		Connection connection = getConnection();

    		// Create a Session
    		Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

    		// Create the destination (delegate global queue)
    		Destination destination = session.createQueue(DELEGATE_GLOBAL_QUEUE);
        
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
    
    private boolean willLicenseKeyExpireInDays(JTALicense license, int days) {
		if(license.getExpiry() == null){
			return false;
		}else{
			Calendar currentTime;
			currentTime = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
			currentTime.add(Calendar.DAY_OF_YEAR, days);
			return currentTime.after(license.getExpiry());
		}		
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

	public String getJasperEngineURL() {
		return jasperEngineURL;
	}

	public void setJasperEngineURL(String jasperEngineURL) {
		this.jasperEngineURL = jasperEngineURL;
	}
	
}
