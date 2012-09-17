package org.mule.transport.webview;

import org.mule.api.MuleException;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.transport.MessageRequester;
import org.mule.transport.http.HttpClientMessageRequesterFactory;

public class WebViewMessageRequesterFactory extends	HttpClientMessageRequesterFactory {

    /** {@inheritDoc} */
    public MessageRequester create(InboundEndpoint endpoint) throws MuleException
    {
        return new WebViewMessageRequester(endpoint);
    }
	
}
