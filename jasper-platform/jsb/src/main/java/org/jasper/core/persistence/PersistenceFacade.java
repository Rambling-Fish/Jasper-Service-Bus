package org.jasper.core.persistence;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;

import com.hazelcast.core.MultiMap;

public class PersistenceFacade {
	
	private MemoryCache memCache;
	
	public PersistenceFacade(Properties prop){
		String localIP = prop.getProperty("persisitence.localIp");
		String groupName  = prop.getProperty("persisitence.groupName");
		String groupPassword  = prop.getProperty("persisitence.groupPassword");
		memCache = new MemoryCache(localIP, groupName, groupPassword);
	}
	
	public PersistenceFacade(String localIP, String groupName, String groupPassword){
		memCache = new MemoryCache(localIP, groupName, groupPassword);
	}
	
	public MultiMap<?,?> getMultiMap(String name) {
		return memCache.getMultiMap(name);
	}
	
	public Map<?,?> getMap(String name) {
		return memCache.getMap(name);
	}
	
	public Object getSharedMemoryInstance(){
		return memCache.getHazelcastInstance();
	}
	
	public BlockingQueue<PersistedObject> getQueue(String name) {
		return memCache.getQueue(name);
	}
	
	public void shutdown(){
		memCache.shutdown();
	}
	
}
