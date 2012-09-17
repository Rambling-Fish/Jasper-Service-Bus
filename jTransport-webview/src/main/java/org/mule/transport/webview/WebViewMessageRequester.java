package org.mule.transport.webview;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.transport.http.HttpClientMessageRequester;

public class WebViewMessageRequester extends HttpClientMessageRequester {

	public WebViewMessageRequester(InboundEndpoint endpoint) {
		super(endpoint);
	}

}
