package org.jasper.core;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.jms.JMSException;

import org.apache.log4j.Logger;
import org.jasper.core.delegate.Delegate;
import org.jasper.core.delegate.DelegateFactory;

public class UDECore {

	static Logger logger = Logger.getLogger(UDECore.class.getName());
	
	
	private int numDelegates;
	private static int defaultNumDelegates = 5;
	private ExecutorService executorService;
	private DelegateFactory factory;

	private Delegate[] delegates;

	private UDE ude;
	private Properties prop;

	
	public UDECore(UDE ude, Properties prop) {
		this.ude = ude;
		this.prop = prop;
	}

	public void start() throws JMSException {
		try {
			numDelegates = Integer.parseInt(prop.getProperty("numDelegates","5"));
		} catch (NumberFormatException ex) {
			numDelegates = defaultNumDelegates;
			logger.warn("Error in properties file. numDelegates = " + prop.getProperty("numDelegates") + ". Using default value of " + defaultNumDelegates);
		}
		
		// Instantiate the delegate pool
		executorService = Executors.newCachedThreadPool();
		factory = new DelegateFactory(ude);
		delegates = new Delegate[numDelegates];
		
		for(int i=0;i<delegates.length;i++){
			delegates[i]=factory.createDelegate();
			executorService.execute(delegates[i]);
		} 			
	}
	
	private void shutdown(){
		logger.info("received shutdown request, shutting down");
		for(Delegate d:delegates){
			try {
				d.shutdown();
			} catch (JMSException e1) {
				logger.error("jmsconnection caught while shutting down delegates",e1);
			}
		}
		executorService.shutdown();	
	}

	public void stop() {
		shutdown();
	}

}
