package org.jasper.core.auth;

import org.apache.activemq.broker.Broker;
import org.apache.activemq.broker.BrokerPlugin;
import org.jasper.core.JasperBroker;

public class JasperAuthenticationPlugin implements BrokerPlugin {	
        
        public Broker installPlugin(Broker broker) throws Exception {            
             return new JasperBroker(broker);
        }	

}