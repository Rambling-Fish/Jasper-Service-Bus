package org.mule.transport.jasperengine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.jasper.jLib.jAuth.JTALicense;
import org.jasper.jLib.jAuth.util.JAuthHelper;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Command;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Type;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.config.MuleProperties;
import org.mule.transport.jms.activemq.ActiveMQJmsConnector;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

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
	
	private Model model;
	
    /* This constant defines the main transport protocol identifier */
    public static final String JASPERENGINE = "jasperengine";
    private static final String JTA_QUEUE_SUFFIX = ".admin.queue";
    private static final String JTA_QUEUE_PREFIX = "jms.";
    
    public JasperEngineConnector(MuleContext context){
        super(context);
        endpointUriMap = new ConcurrentHashMap<String, String>();
        model = ModelFactory.createDefaultModel();
    }
    
    public void doInitialise(){
    	try {
			license = JAuthHelper.loadJTALicenseFromFile(System.getProperty("jta-keystore") + "/" + vendor + "_" + appName + "_" + version + JAuthHelper.JTA_LICENSE_FILE_SUFFIX);
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
				processAdminRequests();
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
				logger.error("Exception occured while shutting down admin hanlder ", e);
			}
			count--;
    	}
    	if(!adminTask.isDone()) adminTask.cancel(true);
    	adminHandler.shutdown();
    	try {
			adminHandler.awaitTermination(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			logger.error("Exception occured while shutting down admin handler ", e);
		}
    	if(!adminHandler.isTerminated()) adminHandler.shutdownNow();
	}
    
    private void processAdminRequests(){
		while(!isAdminHandlerStopped){
			try {
		  	     	
				Session adminSession = getConnection().createSession(false, Session.AUTO_ACKNOWLEDGE);
			      
			    // Create Queue
			    String queueName = JTA_QUEUE_PREFIX + vendor + "." + appName + "." + version + "." + deploymentId + JTA_QUEUE_SUFFIX;
			    Destination adminQueue = adminSession.createQueue(queueName);
			                  
			    // Create a MessageConsumer from the Session to the Queue
			    MessageConsumer adminConsumer = adminSession.createConsumer(adminQueue);
    			MessageProducer adminProducer = adminSession.createProducer(null);

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
							if(((JasperAdminMessage) obj).getType() == Type.ontologyManagement && ((JasperAdminMessage) obj).getCommand() == Command.get_ontology){
			        			String[][] triples = getOntologyFromFile();
			        			Message response = adminSession.createObjectMessage(triples);
			        			response.setJMSCorrelationID(adminRequest.getJMSCorrelationID());
								adminProducer.send(adminRequest.getJMSReplyTo(), response );
								if(logger.isInfoEnabled()){
									StringBuffer sb = new StringBuffer();
									sb.append("\n\t");
									for(String[] a:triples){
										for(String b:a){
											sb.append(b);
											sb.append(" ");
										}
										sb.append("\n\t");
									};
									logger.info("Sent to " + adminRequest.getJMSReplyTo() + " the following triples :" + sb);
								}
							}else{
			                	logger.warn("Received JasperAdminMessage that isn't supported, ignoring : " + obj);
			                }
		                }else{
		                	logger.warn("Received ObjectMessage that wasn't a JasperAdminMessage, ignoring : " + obj);
		                }
			          }else{
		                	logger.warn("Received JMSMessage that wasn't an ObjectMessage, ignoring : " + adminRequest);
			          }
			          
			      }while(!isAdminHandlerStopped);
			      
			      adminProducer.close();
			      adminConsumer.close();
			      adminSession.close();
			     
			  } catch (Exception e) {
				  logger.error("Exception received while listening for admin message, will continue",e);
			  }
		}
    }
    
    
    private String[][] getOntologyFromFile() {	
    	
		ArrayList<String[]> triples = new ArrayList<String[]>();
    	try {
    		
    		if(logger.isInfoEnabled()){
    			logger.info("loading ttl fileName: " + muleContext.getRegistry().get(MuleProperties.APP_HOME_DIRECTORY_PROPERTY) + "/" + appName + ".ttl");
    		}
    		File file = new File(muleContext.getRegistry().get(MuleProperties.APP_HOME_DIRECTORY_PROPERTY) + "/" + appName + ".ttl");
    		FileInputStream fis = new FileInputStream(file);
    		model.read(fis, null, "TTL");
    		
    		for(StmtIterator statements = model.listStatements(); statements.hasNext(); ) {
    			  Statement statement = statements.next();
    			  triples.add(new String[]{statement.getSubject().toString(),statement.getPredicate().toString(),statement.getObject().toString()});
    		}    		
    	}catch (IOException e){
    		logger.error("exception loading ontology from ttl file",e);
    	}
    	
		return triples.toArray(new String[][]{});    	
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
