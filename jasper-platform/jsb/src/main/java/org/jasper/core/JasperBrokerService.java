package org.jasper.core;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.activemq.broker.BrokerService;

public class JasperBrokerService extends BrokerService {

	//TODO properly set and use these booleans
//    private final AtomicBoolean starting = new AtomicBoolean(false);
//    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean stopping = new AtomicBoolean(false);
//    private final AtomicBoolean stopped = new AtomicBoolean(false);

	@Override
	public void start() throws Exception {
		//TODO properly set state booleans
		super.start();
	}
    
	@Override
	public void stop() throws Exception {
		//TODO properly set state booleans
		stopping.set(true);
		super.stop();
	}

	public boolean isStopping() {
		return stopping.get();
	}
	
	

	
}
