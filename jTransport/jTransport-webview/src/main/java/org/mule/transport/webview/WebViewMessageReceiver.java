package org.mule.transport.webview;

import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;

import org.apache.commons.httpclient.Cookie;
import org.apache.log4j.Logger;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transport.Connector;
import org.mule.api.transport.PropertyScope;
import org.mule.config.i18n.CoreMessages;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.session.DefaultMuleSession;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.PollingHttpMessageReceiver;
import org.mule.transport.http.i18n.HttpMessages;
import org.mule.util.ObjectUtils;
import org.mule.util.StringUtils;

public class WebViewMessageReceiver extends PollingHttpMessageReceiver {

	/*
	 * We define an outbound endpoint for each WebView endpoint we
	 * communicate with
	 */
    private OutboundEndpoint outboundLoginEndpoint;
	private OutboundEndpoint outboundTraxEndpoint;
	private OutboundEndpoint outboundVideoEndpoint;
	private OutboundEndpoint outboundNotamEndpoint;
	private OutboundEndpoint outboundAdapsEndpoint;
	private OutboundEndpoint outboundApmEndpoint;
	private OutboundEndpoint outboundSessionKeepAliveEndpoint;
	private OutboundEndpoint outboundClockEndpoint;
	private OutboundEndpoint outboundLogoutEndpoint;

	//Boolean to keep track if we are logged in to webview
	private boolean isLoggedInToWebView = false;
	
	//Each method has a sequence number for each request
	private int traxSeqNumber = 0;
	private int videoSeqNumber = 0;
	private int notamSeqNumb = 0;
	private int adapsSeqNumb = 0;
	private int apmSeqNumb = 0;
	
	/*
	 * I've found that mule isn't handling cookies the way WebView
	 * expects, so we are handling the cookies manually, we need to
	 * keep track of the csrftoken and sessionid, these are used by
	 * all endpoints
	 */
	private String csrftoken = "";
	private String sessionid = "";
	private int retryLimit = 3;

	
	private Logger logger = Logger.getLogger("org.jasper");
		
	
    public WebViewMessageReceiver(Connector connector, FlowConstruct flowConstruct, final InboundEndpoint endpoint) throws CreateException{
		super(connector, flowConstruct, endpoint);
	}
    
    public void doStart() throws MuleException{
        try
        {
    		WebViewConnector pollingConnector;
    		
    		// we need to check if the connector which creates us is a WebVeiw connector, if not we throw an exception
    		if (connector instanceof WebViewConnector){
    			pollingConnector = (WebViewConnector) connector;
    		} else {
    			throw new CreateException(HttpMessages.pollingReciverCannotbeUsed(), this);
    		}
    		
    		//Check to see if we are already logged in, if not attempt the login process
    		for(int i=0; i < retryLimit; i++) {
    			if(!isLoggedInToWebView) webViewLogin();
    			if(isLoggedInToWebView) {
    				break;
    			}
    		}
    
    		if (!isLoggedInToWebView) {
    			logger.error("Failed to login to webview - check configuration settings");
    			return;
    		}
    		
    		/*
    		 * The WebView connector, and by extension the MessageReciever needs to be able to poll different
    		 * endpoints at different rates. If the rate is greater then 500 (milliseconds) than we schedule
    		 * the corresponding message, note we use reflection and so the name of the method we are scheduling
    		 * must match one that is below, i.e. sendTrax corresponds to the public method sendTrax()
    		 */
    		if(pollingConnector.getWebViewPollingFrequencyTrax() > 500) this.schedule("sendTrax", pollingConnector.getWebViewPollingFrequencyTrax());
    		if(pollingConnector.getWebViewPollingFrequencyAdaps() > 500) this.schedule("sendAdaps", pollingConnector.getWebViewPollingFrequencyAdaps());
    		if(pollingConnector.getWebViewPollingFrequencyNotam() > 500) this.schedule("sendNotam", pollingConnector.getWebViewPollingFrequencyNotam());
    		if(pollingConnector.getWebViewPollingFrequencyVideo() > 500) this.schedule("sendVideo", pollingConnector.getWebViewPollingFrequencyVideo());
    		if(pollingConnector.getWebViewPollingFrequencyApm() > 500) this.schedule("sendApm", pollingConnector.getWebViewPollingFrequencyApm());
        }
        catch (Exception ex)
        {
            throw new CreateException(CoreMessages.failedToScheduleWork(), ex, this);
        }
	}
    
    public void doDisconnect() throws Exception{
    	webViewLogout();
    	super.doDisconnect();
    }

	private void webViewLogout() throws MuleException {
    	if(outboundLogoutEndpoint == null) outboundLogoutEndpoint = createOutboundEndpointForWebViewPosts("/webview/accounts/logout");
        sendHttpRequest(outboundLogoutEndpoint, "GET", null);
        isLoggedInToWebView = false;
    }

	private void webViewLogin() throws MuleException{
       	
    	if (!(connector instanceof WebViewConnector)){
    		logger.error("connector is not an instanceof WebViewConnector, aborting webViewLogin sequenece");
    		return;
    	}
    	
    	//If endpoint used for login isn't created, then we create it
        if (outboundLoginEndpoint == null){
            EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(endpoint);
            endpointBuilder.setMessageProcessors(Collections.<MessageProcessor>emptyList());
            endpointBuilder.setResponseMessageProcessors(Collections.<MessageProcessor>emptyList());
            endpointBuilder.setMessageProcessors(Collections.<MessageProcessor>emptyList());
            endpointBuilder.setResponseMessageProcessors(Collections.<MessageProcessor>emptyList());
            endpointBuilder.setExchangePattern(MessageExchangePattern.REQUEST_RESPONSE);

            outboundLoginEndpoint = connector.getMuleContext().getEndpointFactory().getOutboundEndpoint(endpointBuilder);
        }
    	
        /*
         * Initial Login Request
         * Note that we don't call "routeMessage(MuleMessage)" on the response, we simple ignore it,
         * as we don't want the rest of the mule flow to deal with the response, this is the first
         * step in the login process, which is to download the initial login page, this will give us
         * the cookie info we need, specifically the csrftoken
         */
        try {
        	sendHttpRequest(outboundLoginEndpoint, "GET", null);
        } catch (Exception ex) {
        	logger.warn(ex.getLocalizedMessage());
        	isLoggedInToWebView = false;
        	return;

        }
    	
    	/*
    	 * We then create our login POST request, the body format is:
    	 * csrfmiddlewaretoken=<csrftoken>&csrfmiddlewaretoken=<csrftoken>&username=<username>&password=<password>&next=
    	 * 
    	 * the csrftoken and sessionid should automatically updated by the sendHttpRequest(...) method
    	 * 
    	 */
    	String userInfo = outboundLoginEndpoint.getEndpointURI().getUserInfo();
        String username = userInfo.split(":")[0];
        String password = userInfo.split(":")[1];
        String postBody = "csrfmiddlewaretoken="+ csrftoken + "&" +
        	              "csrfmiddlewaretoken="+ csrftoken + "&" +
        	              "username="+ username + "&" +
        	              "password="+ password + "&next=";
        sendHttpRequest(outboundLoginEndpoint, "POST", postBody); 	
        
        //set login boolean to true
        isLoggedInToWebView = true;
        
        /*
         * WebView requires keep alive requests, the default web client
         * behaviour is 20 seconds and sends both keep alive and clock
         * posts, so we do the same
         */
        schedule("sendSessionKeepAlivePost", 20000);
        schedule("sendClockPost", 20000);
    }
    
    protected void schedule(String poolMethodName, long frequency) throws RejectedExecutionException, NullPointerException, IllegalArgumentException	{
		synchronized (schedules){
		    // we use scheduleWithFixedDelay to prevent queue-up of tasks when
		    // polling takes longer than the specified frequency, e.g. when the
		    // polled database or network is slow or returns large amounts of
		    // data.
			
			/*
			 * We've created a new polling receiver worker that allows us to specify the name of the method that will do the polling
			 * this allows us to have different methods polling with the same connector. We then need to add the worker to the schedule
			 */
			WebViewPollingReceiverWorker pollingReceiverWorker = new WebViewPollingReceiverWorker(this,poolMethodName);
		    ScheduledFuture schedule = connector.getScheduler().scheduleWithFixedDelay(new WebViewPollingReceiverWorkerSchedule(pollingReceiverWorker), DEFAULT_STARTUP_DELAY, frequency, this.getTimeUnit());
		    schedules.put(schedule, pollingReceiverWorker);
		
		    if (logger.isDebugEnabled())
		    {
		        logger.debug(ObjectUtils.identityToShortString(this) + " scheduled "
		                     + ObjectUtils.identityToShortString(schedule) + " with " + frequency + " " + getTimeUnit() + " polling frequency");
		    }
		}
	}
    
    public void sendTrax() throws Exception{
    	//we send the HTTP POST request and than route the response down the mule chain to be processed
    	routeMessage(sendTraxPost()); 
	}
    
    public void sendAdaps() throws Exception{ 
    	//we send the HTTP POST request and than route the response down the mule chain to be processed
    	routeMessage(sendAdapsPost());
    }
    
    public void sendNotam() throws Exception{
    	//we send the HTTP POST request and than route the response down the mule chain to be processed
    	routeMessage(sendNotamPost());
    }
    
    /*
     * We've tested this method but we currently don't poll Video Data,
     * leaving it in place just in case we need it in the future.
     */
    public void sendVideo() throws Exception{
    	//we send the HTTP POST request and than route the response down the mule chain to be processed
    	routeMessage(sendVideoPost());
    }
    
    /*
     * We've tested this method but we currently don't poll any Apm data,
     * there are 3 types of APM data that are polled by WebView web clients,
     * by default, but we have no need for this data at the present time,
     * leaving this method in place just in case we need it in the future.
     */
    public void sendApm() throws Exception{
    	//we send the HTTP POST request and than route the response down the mule chain to be processed
    	routeMessage(sendApmGridlockPost());
    	routeMessage(sendApmDailySummaryPost());
    	routeMessage(sendApmMonitorPost());
    }
       
    /*
     * This is the generic send method, we specify which endpoint to use, the HTTP Method (POST or GET) and the Body,
     * the body is null if there is no body.
     */
	private MuleMessage sendHttpRequest(OutboundEndpoint ep, String httpMethodProperty, String body) throws MuleException{
		
		/*
		 * We keep track of the orginal cookies so that we can log when they change
		 */
		String orig_sessionid = sessionid;
		String orig_csrftoken = csrftoken;
		
		/*
		 * I've noticed that WebView only accepts a single Cookie header in the
		 * following format:
		 * 
		 * Cookie: csrftoken=<csrftoken>; sessionid=<sessionid>
		 * 
		 * And MULE's default cookie handling has multiple Cookie headers with other custom
		 * info which isn't accepted by WebView. I've decided to clear the Cookies
		 * from the HttpClient used for communication and add my own "Cookie" header as
		 * a Mule Property. The following block will check if sessionid and csrftoken have
		 * been previously set, if so it will clear the HttpClient's cookies and than add
		 * our custom Cookie header. The sessionid and csrftoken are updated near the end of
		 * this method after once we've received a response.
		 */
		String cookie = null;
    	if(!"".equals(sessionid)) cookie = "sessionid=" + sessionid;
    	if(!"".equals(sessionid) && !"".equals(csrftoken)) cookie += "; ";
    	if(!"".equals(csrftoken)) cookie += "csrftoken=" + csrftoken;
    	if(cookie !=null){
    		((WebViewConnector) connector).getClient().getState().clearCookies();
    		ep.getProperties().put("Cookie", cookie);
    	}
    	
    	//We create our default message
        MuleMessage request = new DefaultMuleMessage(StringUtils.EMPTY, ep.getProperties(), connector.getMuleContext());

        //Set our HTTP Method type and body if present
        request.setOutboundProperty(HttpConnector.HTTP_METHOD_PROPERTY, httpMethodProperty);
        if(body != null) request.setPayload(body);
        
        MuleSession session = new DefaultMuleSession(flowConstruct, connector.getMuleContext());
        
        MuleEvent event = new DefaultMuleEvent(request, ep.getExchangePattern(), session);
        
        //We send the event, get a result and get the message from the result
        MuleEvent result = ep.process(event);
        MuleMessage message = null;
        if (result != null) message = result.getMessage();

        /*
         * If there's a response we update the csrftoken and sessionid if present in the 
         * Set-Cookie header, and log any changes to these parameters
         */
        if (message != null){
        	Cookie[] setCookie = message.getInboundProperty("Set-Cookie");
        	if(setCookie != null) updateClientCookies(setCookie);
       		if(!csrftoken.equals(orig_csrftoken)) logger.info("Cookies Updated : orig_csrftoken = " + orig_csrftoken + " ; csrftoken = " + csrftoken);
       		if(!sessionid.equals(orig_sessionid)) logger.info("Cookies Updated : orig_sessionid = " + orig_sessionid + " ; sessionid = " + sessionid);
        }
        
        /*
         * We return the response message to be routed or dropped. Login,
         * session keep alive and clock requests will be droped, while
         * trax, adaps, notams, apm and video messages are routed within MULE
         */
        return message;
    }
	
    /*
     * we handle the cookies manually and so we update the csrftoken
     * and sessionid info with each request/response
     */
    private void updateClientCookies(Cookie[] cookies) {
    	for (Cookie cookie:cookies){
    		if(cookie.getName().equals("csrftoken")){
    			synchronized(csrftoken){
    				csrftoken = cookie.getValue();
    			}
    		}
    		if(cookie.getName().equals("sessionid")){
    			synchronized(sessionid){
    				sessionid = cookie.getValue();
    		
    			}
    		}
    	}	
	}
    
	/*
	 * Creates our outbound enpoints, we simply need to specify the path, for example, if the POST is:
	 * 
	 * POST /webview/trax HTTP/1.1
	 *  
	 * then the path is "/webview/trax"
	 * 
	 */
	private OutboundEndpoint createOutboundEndpointForWebViewPosts(String path) throws MuleException {
		String host = endpoint.getEndpointURI().getUri().getHost();
		EndpointBuilder enB = new EndpointURIEndpointBuilder("http://" + host + path, connector.getMuleContext());
		enB.setConnector(connector);
       	enB.setMessageProcessors(Collections.<MessageProcessor>emptyList());
        enB.setResponseMessageProcessors(Collections.<MessageProcessor>emptyList());
        enB.setMessageProcessors(Collections.<MessageProcessor>emptyList());
        enB.setResponseMessageProcessors(Collections.<MessageProcessor>emptyList());
        enB.setExchangePattern(MessageExchangePattern.REQUEST_RESPONSE);
        
        OutboundEndpoint ep = connector.getMuleContext().getEndpointFactory().getOutboundEndpoint(enB);
        /*
         * WebView requires the following HTTP Headers so we add them to the endpoint properties and
         * they will be inclueded by MULE
         */
        ep.getProperties().put("X-Requested-With", "XMLHttpRequest");
        ep.getProperties().put("Content-Type", "application/x-www-form-urlencoded");
        return ep;
	}
	
	/*
	 * The following methods methods all do the same thing. They all send POST messages to
	 * WebView to poll for information. There are 9 messages.
	 * 
	 * 2 maintenance messages
	 *    -> session keep alive : as the name suggests periodic messages
	 *    -> clock : I assume it's used to synchronize the time on the web client and the server
	 * 
	 * 3 messages we currently support and use
	 *    -> trax  : target information that is being tracked, this included position, velocity, name, etc.
	 *    -> notam : notice to airmen info
	 *    -> adaps : weather and runway info
	 *    
	 * 4 messages we initially implemented but currently don't use
	 *    -> video
	 *    -> apm : 3 different apm messages all sent to same endpoint, they are related to air traffic departures and arrivals
	 *    
	 * 
	 * The flow of each method is the same:
	 *   1. Check if outbound endpoint is created, if not create it
	 *   2. Create request body (all have a request body except for keep alive). The body contains the request method type and
	 *      parameters. An example : grasp=T,0,14,trax|live(4.444444444444445,1343749704096,0).
	 *   3. Send the HTTP POST request and get a response
	 *   4. Add message type info to the response message, this info is added so we can more easily route the message within MULE
	 *   5. Return the response message
	 *   
	 *   
	 * For each of the currently supported messages I've included a copy of a sample WebClient request from Safari,
	 * which was the basis for our implementation.
	 */
	
	
	//Sample Keep Alive POST
	//
	//	POST /webview/watchdog/api/session_keep_alive HTTP/1.1
	//	Host: 66.46.93.196
	//	User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_4) AppleWebKit/536.25 (KHTML, like Gecko) Version/6.0 Safari/536.25
	//	Content-Length: 0
	//	Accept: */*
	//	Origin: http://66.46.93.196
	//	X-Requested-With: XMLHttpRequest
	//	Content-Type: application/xml
	//	Referer: http://66.46.93.196/webview/cdm/
	//	Accept-Language: en-us
	//	Accept-Encoding: gzip, deflate
	//	Cookie: csrftoken=6d30eceb39132a6fbb55ef9e2a63f21f; sessionid=a454a589f8ae0a369320c8884f0022fc
	//	Connection: keep-alive
	public MuleMessage sendSessionKeepAlivePost() throws MuleException {
		if(outboundSessionKeepAliveEndpoint == null) outboundSessionKeepAliveEndpoint = createOutboundEndpointForWebViewPosts("/webview/watchdog/api/session_keep_alive");
		MuleMessage msg = sendHttpRequest(outboundSessionKeepAliveEndpoint, "POST", null);
		return msg;
	}
	
	// Sample Clock POST
	//
	//	POST /webview/clock HTTP/1.1
	//	Host: 66.46.93.196
	//	User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_4) AppleWebKit/536.25 (KHTML, like Gecko) Version/6.0 Safari/536.25
	//	Content-Length: 46
	//	Accept: */*
	//	Origin: http://66.46.93.196
	//	X-Requested-With: XMLHttpRequest
	//	Content-Type: application/x-www-form-urlencoded
	//	Referer: http://66.46.93.196/webview/cdm/
	//	Accept-Language: en-us
	//	Accept-Encoding: gzip, deflate
	//	Cookie: csrftoken=6d30eceb39132a6fbb55ef9e2a63f21f; sessionid=a454a589f8ae0a369320c8884f0022fc
	//	Connection: keep-alive
	//
	//	grasp=T%2C0%2C0%2Cclock%7Cclock(1343749680922)
	public MuleMessage sendClockPost() throws MuleException {
		if(outboundClockEndpoint == null) outboundClockEndpoint = createOutboundEndpointForWebViewPosts("/webview/clock");
		
		String postBody = "grasp=T%2C0%2C0%2Cclock%7Cclock(" + System.currentTimeMillis() + ")";
		
		MuleMessage msg = sendHttpRequest(outboundClockEndpoint, "POST", postBody);
		HashMap<String,Object> msgType = new HashMap<String,Object>();
		msgType.put("MessageType", "clock");
		msg.addProperties(msgType,PropertyScope.INBOUND);
		return msg;
	}

	//Sample Trax Post
	//
	//	POST /webview/trax HTTP/1.1
	//	Host: 66.46.93.196
	//	User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_4) AppleWebKit/536.25 (KHTML, like Gecko) Version/6.0 Safari/536.25
	//	Content-Length: 57
	//	Accept: */*
	//	Origin: http://66.46.93.196
	//	X-Requested-With: XMLHttpRequest
	//	Content-Type: application/x-www-form-urlencoded
	//	Referer: http://66.46.93.196/webview/cdm/
	//	Accept-Language: en-us
	//	Accept-Encoding: gzip, deflate
	//	Cookie: csrftoken=6d30eceb39132a6fbb55ef9e2a63f21f; sessionid=a454a589f8ae0a369320c8884f0022fc
	//	Connection: keep-alive
	//
	//	grasp=T,0,14,trax|live(4.444444444444445,1343749704096,0)
	private MuleMessage sendTraxPost() throws MuleException {
		if(outboundTraxEndpoint == null) outboundTraxEndpoint = createOutboundEndpointForWebViewPosts("/webview/trax");
		
		//I've experimented with different zoom factors and haven't noticed a difference
		String[] zoomFactor = {"1000"};
//				               "500",
//				   			   "333.3333333333333",
//				               "222.2222222222222",
//				               "111.1111111111111",
//				                "55.5555555555555",
//				                "27.7777777777778",
//				                "13.8888888888889",
//				                 "4.444444444444445",
//				                 "2.222222222222222",
//				                 "1.111111111111111",
//				                 "0"};
		String postBody = "grasp=T,0," + traxSeqNumber + ",trax|live(" + zoomFactor[traxSeqNumber%zoomFactor.length] + "," + System.currentTimeMillis() + ",0)";
		MuleMessage msg = sendHttpRequest(outboundTraxEndpoint, "POST", postBody);
		HashMap<String,Object> msgType = new HashMap<String,Object>();
		msgType.put("MessageType", "trax");
		msgType.put("zoomFactor", zoomFactor[traxSeqNumber%zoomFactor.length]);
		msg.addProperties(msgType,PropertyScope.INBOUND);
		traxSeqNumber++;
		return msg;
	}
	//Sample Notam POST
	//
	//	POST /webview/notam HTTP/1.1
	//	Host: 66.46.93.196
	//	User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_4) AppleWebKit/536.25 (KHTML, like Gecko) Version/6.0 Safari/536.25
	//	Content-Length: 39
	//	Accept: */*
	//	Origin: http://66.46.93.196
	//	X-Requested-With: XMLHttpRequest
	//	Content-Type: application/x-www-form-urlencoded
	//	Referer: http://66.46.93.196/webview/cdm/
	//	Accept-Language: en-us
	//	Accept-Encoding: gzip, deflate
	//	Cookie: csrftoken=6d30eceb39132a6fbb55ef9e2a63f21f; sessionid=a454a589f8ae0a369320c8884f0022fc
	//	Connection: keep-alive
	//
	//	grasp=T,0,12,notam|notam(1343749698096)
	private MuleMessage sendNotamPost() throws MuleException {
		if(outboundNotamEndpoint == null) outboundNotamEndpoint = createOutboundEndpointForWebViewPosts("/webview/notam");
		
		String postBody = "grasp=T,0," + notamSeqNumb++ + ",notam|notam("+ System.currentTimeMillis() + ")";
		
		MuleMessage msg = sendHttpRequest(outboundNotamEndpoint, "POST", postBody);
		HashMap<String,Object> msgType = new HashMap<String,Object>();
		msgType.put("MessageType", "notam");
		msg.addProperties(msgType,PropertyScope.INBOUND);
		return msg;
	}
	
	//Sample Adaps POST
	//
	//	POST /webview/adaps HTTP/1.1
	//	Host: 66.46.93.196
	//	User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_4) AppleWebKit/536.25 (KHTML, like Gecko) Version/6.0 Safari/536.25
	//	Content-Length: 42
	//	Accept: */*
	//	Origin: http://66.46.93.196
	//	X-Requested-With: XMLHttpRequest
	//	Content-Type: application/x-www-form-urlencoded
	//	Referer: http://66.46.93.196/webview/cdm/
	//	Accept-Language: en-us
	//	Accept-Encoding: gzip, deflate
	//	Cookie: sessionid=a454a589f8ae0a369320c8884f0022fc; csrftoken=6d30eceb39132a6fbb55ef9e2a63f21f
	//	Connection: keep-alive
	//
	//	grasp=T,0,11,adaps|adaps(0, 1343749692096)
	private MuleMessage sendAdapsPost() throws MuleException {
		if(outboundAdapsEndpoint == null) outboundAdapsEndpoint = createOutboundEndpointForWebViewPosts("/webview/adaps");
		
		String postBody = "grasp=T,0," + adapsSeqNumb++ + ",adaps|adaps(0,"+ System.currentTimeMillis() + ")";
		
		MuleMessage msg = sendHttpRequest(outboundAdapsEndpoint, "POST", postBody);
		HashMap<String,Object> msgType = new HashMap<String,Object>();
		msgType.put("MessageType", "adaps");
		msg.addProperties(msgType,PropertyScope.INBOUND);
		return msg;
	}
	
	//Currently not used, but should work
	private MuleMessage sendVideoPost() throws MuleException {
		if(outboundVideoEndpoint == null) outboundVideoEndpoint = createOutboundEndpointForWebViewPosts("/webview/video");
		
		String postBody = "grasp=T,0," + videoSeqNumber++ + ",video|video()";
		
		MuleMessage msg = sendHttpRequest(outboundVideoEndpoint, "POST", postBody);
		HashMap<String,Object> msgType = new HashMap<String,Object>();
		msgType.put("MessageType", "video");
		msg.addProperties(msgType,PropertyScope.INBOUND);
		return msg;
	}
	
	//Currently not used, but should work
	private MuleMessage sendApmGridlockPost() throws MuleException {
		
		if(outboundApmEndpoint == null) outboundApmEndpoint = createOutboundEndpointForWebViewPosts("/webview/apm");
		
		String postBody = "grasp=T,0," + apmSeqNumb++ + ",apm|apm-gridlock("+ System.currentTimeMillis() + ")";
		
		MuleMessage msg = sendHttpRequest(outboundApmEndpoint, "POST", postBody);
		HashMap<String,Object> msgType = new HashMap<String,Object>();
		msgType.put("MessageType", "apm-gridlock");
		msg.addProperties(msgType,PropertyScope.INBOUND);
		return msg;
	}
	
	//Currently not used, but should work
	private MuleMessage sendApmDailySummaryPost() throws MuleException {
		
		if(outboundApmEndpoint == null) outboundApmEndpoint = createOutboundEndpointForWebViewPosts("/webview/apm");
		
		String postBody = "grasp=T,0," + apmSeqNumb++ + ",apm|apm-daily-summary("+ System.currentTimeMillis() + ")";
		
		MuleMessage msg = sendHttpRequest(outboundApmEndpoint, "POST", postBody);
		HashMap<String,Object> msgType = new HashMap<String,Object>();
		msgType.put("MessageType", "apm-daily-summary");
		msg.addProperties(msgType,PropertyScope.INBOUND);
		return msg;
	}
	
	//Currently not used, but should work
	private MuleMessage sendApmMonitorPost() throws MuleException {
		
		if(outboundApmEndpoint == null) outboundApmEndpoint = createOutboundEndpointForWebViewPosts("/webview/apm");
		
		String postBody = "grasp=T,0," + apmSeqNumb++ + ",apm|apm-monitor(1,"+ System.currentTimeMillis() + ")";
		
		MuleMessage msg = sendHttpRequest(outboundApmEndpoint, "POST", postBody);
		HashMap<String,Object> msgType = new HashMap<String,Object>();
		msgType.put("MessageType", "apm-monitor");
		msg.addProperties(msgType,PropertyScope.INBOUND);
		return msg;
	}


}
