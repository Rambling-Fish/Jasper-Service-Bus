package org.mule.transport.webview;

import org.mule.api.MuleContext;
import org.mule.transport.http.HttpMuleMessageFactory;

public class WebViewMuleMessageFactory extends HttpMuleMessageFactory {

	public WebViewMuleMessageFactory(MuleContext context) {
		super(context);
	}

}
