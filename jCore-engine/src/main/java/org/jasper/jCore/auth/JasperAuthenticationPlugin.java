package org.jasper.jCore.auth;

import org.apache.activemq.broker.Broker;
import org.apache.activemq.broker.BrokerPlugin;

public class JasperAuthenticationPlugin implements BrokerPlugin {	
        
        public Broker installPlugin(Broker broker) throws Exception {            
             return new JasperBroker(broker);
        }	

}