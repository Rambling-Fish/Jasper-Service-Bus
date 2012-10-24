package org.mule.transport.jasperengine;

import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.transport.Connector;
import org.mule.transport.jms.XaTransactedJmsMessageReceiver;

public class XaTransactedJasperEngineMessageReceiver extends XaTransactedJmsMessageReceiver {

	public XaTransactedJasperEngineMessageReceiver(Connector connector,	FlowConstruct flowConstruct, InboundEndpoint endpoint)
			throws CreateException {
		super(connector, flowConstruct, endpoint);
	}

}
