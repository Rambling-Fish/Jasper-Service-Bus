package org.jasper.core.delegate.handlers;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;

import org.apache.log4j.Logger;
import org.jasper.core.delegate.Delegate;
import org.jasper.core.persistence.PersistedDataReqeust;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Type;

public class GlobalQHandler implements Runnable {

	static Logger logger = Logger.getLogger(GlobalQHandler.class.getName());
	
	private Delegate delegate;
	private Message	jmsRequest;

	public GlobalQHandler(Delegate delegate, Message jmsRequest) {
		super();
		this.delegate = delegate;
		this.jmsRequest = jmsRequest;
	}


	@Override
	public void run() {
		
		try{		
			if(jmsRequest instanceof ObjectMessage && ((ObjectMessage)jmsRequest).getObject() instanceof JasperAdminMessage && ((JasperAdminMessage)((ObjectMessage)jmsRequest).getObject()).getType() == Type.ontologyManagement){
				AdminHandler adminHandler = new AdminHandler(delegate, jmsRequest);
				adminHandler.run();
//				jmsRequest.acknowledge();
			}else if (jmsRequest instanceof TextMessage && ((TextMessage) jmsRequest).getText().contains("query") ){
				SparqlHandler sparqlHandler = new SparqlHandler(delegate, jmsRequest);
				sparqlHandler.run();
//				jmsRequest.acknowledge();
			}else if (jmsRequest instanceof TextMessage	&& ((TextMessage) jmsRequest).getText() !=null){
				PersistedDataReqeust pData = null;
				try {
					pData  = delegate.persistDataRequest((TextMessage)jmsRequest);
				} catch (Exception e) {
					logger.error("unable to persist data request",e);
				}
//				jmsRequest.acknowledge();
				DataRequestHandler dataRequestHandler = new DataRequestHandler(delegate,pData);
				dataRequestHandler.run();
			} else {
				logger.warn("JMS Message neither ObjectMessage nor TextMessage, ignoring request : " + jmsRequest);
			}
		}catch (JMSException jmse){
			logger.error("error occured in processDelegateQMsg", jmse);
		}		
	}
}
