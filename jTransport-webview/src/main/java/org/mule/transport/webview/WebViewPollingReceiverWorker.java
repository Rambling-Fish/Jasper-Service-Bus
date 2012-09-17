package org.mule.transport.webview;

import java.lang.reflect.Method;

import org.mule.transport.AbstractPollingMessageReceiver;
import org.mule.transport.PollingReceiverWorker;

public class WebViewPollingReceiverWorker extends PollingReceiverWorker {

	private String pollMethodName;
	
	public WebViewPollingReceiverWorker(AbstractPollingMessageReceiver pollingMessageReceiver) {
		this(pollingMessageReceiver,null);
	}
	public WebViewPollingReceiverWorker(AbstractPollingMessageReceiver pollingMessageReceiver, String pollMethodName) {
		super(pollingMessageReceiver);
		this.pollMethodName = pollMethodName;
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.mule.transport.PollingReceiverWorker#poll()
	 * 
	 * we overwrite this method so that we can call a specific
	 * polling method via reflection, if no polling name is set
	 * then we call the default method on the receiver, which is
	 * performPoll(); 
	 *
	 */
    protected void poll() throws Exception
    {
    	if(pollMethodName == null){
    		receiver.performPoll();
    	}else{
    		Method  method = receiver.getClass().getDeclaredMethod(pollMethodName, null);
    		method.invoke(receiver,null);
    	}
        
    }

}
