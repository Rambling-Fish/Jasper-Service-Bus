package org.jasper.core.persistence;

import java.util.Map;

import org.apache.log4j.Logger;
import org.jasper.core.JECore;

import com.hazelcast.config.Config;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.MultiMap;

public class MemoryCache {
	
	private HazelcastInstance hazelcastInstance = null;
	static Logger logger = Logger.getLogger(MemoryCache.class.getName());
	private GroupConfig groupConfig;
	private Config config;
	
	public MemoryCache() {
		
        // TODO the deploymentID will be retrieved from the licenseKey subsystem not directly from core
		config = new Config();
		String groupName = JECore.getInstance().getDeploymentID();
		groupConfig = new GroupConfig(groupName, groupName + "_password_nov_20_2013_1520");
		createHazelcastInstance();
	}

	private void createHazelcastInstance() {
		if(hazelcastInstance == null) {
			config.setGroupConfig(groupConfig);
			hazelcastInstance=Hazelcast.newHazelcastInstance(config);
		}
	}
	
	public HazelcastInstance getHazelcastInstance() {
		return hazelcastInstance;
	}
	
	public void setHazelcastInstance(HazelcastInstance hz) {
		hazelcastInstance = hz;
	}
	
	public MultiMap getMultiMap(String name) {
		createHazelcastInstance();
		return hazelcastInstance.getMultiMap(name);
	}
	
	public Map getMap(String name) {
		createHazelcastInstance();
		return hazelcastInstance.getMap(name);
	}
	
	public void shutdown(){
		if(hazelcastInstance != null){
			hazelcastInstance.getLifecycleService().shutdown();
	    	int count = 0;
			try {
				while(hazelcastInstance.getLifecycleService().isRunning()){
					Thread.sleep(500);
		    		count++;
		    		if(count > 20){
		    			hazelcastInstance.getLifecycleService().terminate();
		    			break;
		    		}
		    	}
				hazelcastInstance = null;
			} catch (InterruptedException e) {
				logger.error("Exception while shutting down hazelcast " + e);
			}
    	}
	}
	
}
