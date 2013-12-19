package org.jasper.core.auth;

import org.apache.activemq.broker.Broker;
import org.apache.activemq.broker.BrokerPlugin;
import org.jasper.core.JasperBroker;
import org.jasper.core.UDE;
import org.jasper.core.persistence.PersistenceFacade;

public class JasperAuthenticationPlugin implements BrokerPlugin {	
        
        private PersistenceFacade cachingSys;
		private AuthenticationFacade licenseKeySys;
		private UDE ude;

		public JasperAuthenticationPlugin(UDE ude, PersistenceFacade cachingSys, AuthenticationFacade licenseKeySys) {
        	this.ude = ude;
			this.cachingSys = cachingSys;
        	this.licenseKeySys = licenseKeySys;
        }

		public Broker installPlugin(Broker broker) throws Exception {            
             return new JasperBroker(broker, ude, cachingSys,licenseKeySys);
        }	

}