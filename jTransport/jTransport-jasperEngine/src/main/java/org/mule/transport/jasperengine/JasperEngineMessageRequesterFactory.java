package org.mule.transport.jasperengine;

import org.mule.api.MuleException;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.transport.MessageRequester;
import org.mule.transport.jms.JmsMessageRequester;
import org.mule.transport.jms.JmsMessageRequesterFactory;

public class JasperEngineMessageRequesterFactory extends JmsMessageRequesterFactory {
	
    public MessageRequester create(InboundEndpoint endpoint) throws MuleException{
        return new JasperEngineMessageRequester(endpoint);
    }

}
