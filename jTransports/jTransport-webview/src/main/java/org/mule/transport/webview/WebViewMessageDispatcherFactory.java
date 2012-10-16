package org.mule.transport.webview;

import org.mule.api.MuleException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transport.MessageDispatcher;
import org.mule.transport.http.HttpClientMessageDispatcher;
import org.mule.transport.http.HttpClientMessageDispatcherFactory;

public class WebViewMessageDispatcherFactory extends HttpClientMessageDispatcherFactory {

    /** {@inheritDoc} */
    public MessageDispatcher create(OutboundEndpoint endpoint) throws MuleException
    {
        return new WebViewMessageDispatcher(endpoint);
    }
	
}
