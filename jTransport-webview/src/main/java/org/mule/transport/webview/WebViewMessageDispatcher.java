package org.mule.transport.webview;

import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.transport.http.HttpClientMessageDispatcher;

public class WebViewMessageDispatcher extends HttpClientMessageDispatcher {

	public WebViewMessageDispatcher(OutboundEndpoint endpoint) {
		super(endpoint);
	}

}
