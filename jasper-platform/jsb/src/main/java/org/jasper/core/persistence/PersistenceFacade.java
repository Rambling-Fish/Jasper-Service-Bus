package org.jasper.core.persistence;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

import com.hazelcast.core.MultiMap;

public class PersistenceFacade {
	
	private static MemoryCache memCache;
	private static PersistenceFacade facade;
	
	private PersistenceFacade(){
		
	}
	
	public static PersistenceFacade getInstance(){
		if(facade == null){
			facade = new PersistenceFacade();
			memCache = new MemoryCache();
		}
		return facade;
	}
	
	public MultiMap getMultiMap(String name) {
		return memCache.getMultiMap(name);
	}
	
	public Map<String,PersistedObject> getMap(String name) {
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
