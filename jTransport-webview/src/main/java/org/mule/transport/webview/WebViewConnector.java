package org.mule.transport.webview;

import java.util.Properties;

import org.apache.commons.httpclient.HttpClient;
import org.mule.api.MuleContext;
import org.mule.api.config.MuleProperties;
import org.mule.transport.http.HttpPollingConnector;

public class WebViewConnector extends HttpPollingConnector {

	/*
	 * Originally developed using
	 * WebView2 1.9.1508.17
	 * Location: CYOW
	 * 
	 * Re-tested and re-factored with
	 * WebView2 1.9.1738.25
	 * Location: CYOW
	 *
	 * TODO Need to properly handle app shutdown, i.e. we should logout of WebView,
	 * we currently don't, this hasn't caused any issues to date as WebView's audits will
	 * log us out, and the app is meant to be long lived, with previous up times greater
	 * than a month
	 * 
	 */
	
	private volatile HttpClient client;
	
	private long webViewPollingFrequencyTrax;
	private long webViewPollingFrequencyVideo;
	private long webViewPollingFrequencyNotam;
	private long webViewPollingFrequencyAdaps;
	private long webViewPollingFrequencyApm;

	public WebViewConnector(MuleContext context) {
		super(context);
        serviceOverrides = new Properties();
        //Override which MessageReceiver we use and which MessageDispatcherFactory we use
        serviceOverrides.setProperty(MuleProperties.CONNECTOR_MESSAGE_RECEIVER_CLASS, WebViewMessageReceiver.class.getName());
        serviceOverrides.setProperty(MuleProperties.CONNECTOR_DISPATCHER_FACTORY, WebViewMessageDispatcherFactory.class.getName());
    }
	
	/*
	 * (non-Javadoc)
	 * @see org.mule.transport.http.HttpConnector#doClientConnect()
	 *
	 * we override so that we can locally store the HttpClient, this allows us to get access
	 * to the cookies, which we need to overwrite to satisfy how WebView handles cookies
	 * 
	 * TODO investigate whether we can use the inherited method from the HttpConnector class
	 * getClientConnectionManager() instead of this one to get access to the HttpClient and
	 * therefore the cookies for custom handling.
	 * 
	 */
	public HttpClient doClientConnect() throws Exception{
		client = super.doClientConnect();
		return client;
	}

	/*
	 * A getter to access the HTTP Client
	 */
	public HttpClient getClient() {
		if(client == null)
			try {
				doClientConnect();
			} catch (Exception e) {
				e.printStackTrace();
			}
		return client;
	}
	
	/*
	 * The following are a standard set of getters and setters used by spring to set our configuration,
	 * they are all the frequencies for the different polling endpoints 
	 */
	
	public long getWebViewPollingFrequencyTrax() {
		return webViewPollingFrequencyTrax;
	}

	public void setWebViewPollingFrequencyTrax(long webViewPollingFrequencyTrax) {
		this.webViewPollingFrequencyTrax = webViewPollingFrequencyTrax;
	}

	public long getWebViewPollingFrequencyVideo() {
		return webViewPollingFrequencyVideo;
	}

	public void setWebViewPollingFrequencyVideo(long webViewPollingFrequencyVideo) {
		this.webViewPollingFrequencyVideo = webViewPollingFrequencyVideo;
	}

	public long getWebViewPollingFrequencyNotam() {
		return webViewPollingFrequencyNotam;
	}

	public void setWebViewPollingFrequencyNotam(long webViewPollingFrequencyNotam) {
		this.webViewPollingFrequencyNotam = webViewPollingFrequencyNotam;
	}

	public long getWebViewPollingFrequencyAdaps() {
		return webViewPollingFrequencyAdaps;
	}

	public void setWebViewPollingFrequencyAdaps(long webViewPollingFrequencyAdaps) {
		this.webViewPollingFrequencyAdaps = webViewPollingFrequencyAdaps;
	}

	public long getWebViewPollingFrequencyApm() {
		return webViewPollingFrequencyApm;
	}

	public void setWebViewPollingFrequencyApm(long webViewPollingFrequencyApm) {
		this.webViewPollingFrequencyApm = webViewPollingFrequencyApm;
	}
}
