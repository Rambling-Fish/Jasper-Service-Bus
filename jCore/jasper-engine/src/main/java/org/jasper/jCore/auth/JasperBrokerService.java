package org.jasper.jCore.auth;

import org.apache.activemq.broker.BrokerPlugin;
import org.apache.activemq.broker.BrokerService;

public class JasperBrokerService extends BrokerService {

	public JasperBrokerService() {
		super();

		/*
		 * We add the jasper authentication plugin to the processor chain, this allows us
		 * to insert ourselves into the connection establishment flow, if a endpoint doesn't
		 * have a valid app id to match it's username (app name, vendor, version and deployment)
		 */
		BrokerPlugin[] plugins = {(BrokerPlugin)new JasperAuthenticationPlugin()};
		setPlugins(plugins);	
	}
	
}
