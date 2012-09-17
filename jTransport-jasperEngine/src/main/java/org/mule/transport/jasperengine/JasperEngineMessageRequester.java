package org.mule.transport.jasperengine;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.transport.jms.JmsMessageRequester;

public class JasperEngineMessageRequester extends JmsMessageRequester {

	public JasperEngineMessageRequester(InboundEndpoint endpoint) {
		super(endpoint);
	}

}
