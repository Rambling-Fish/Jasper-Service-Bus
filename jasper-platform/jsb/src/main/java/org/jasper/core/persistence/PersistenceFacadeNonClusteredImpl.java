package org.jasper.core.persistence;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import com.hazelcast.core.IMap;
import com.hazelcast.core.MultiMap;

public class PersistenceFacadeNonClusteredImpl implements PersistenceFacade {
	
	private static Map<String,MultiMap> multiMap = new ConcurrentHashMap<String, MultiMap>();
	private static Map<String,IMap> map = new ConcurrentHashMap<String, IMap>();
	
	@Override
	public <K, V> MultiMap<K, V> getMultiMap(String name) {
		if(!multiMap.containsKey(name)){
			multiMap.put(name, new LocalMuliMap<K,V>());
		}
		return multiMap.get(name);
	}
	
	@Override
	public <K, V> IMap<K, V> getMap(String name) {
		if(!map.containsKey(name)){
			map.put(name, new LocalIMap<K,V>());
		}
		return map.get(name);
	}
	@Override
	public Object getSharedMemoryInstance() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public BlockingQueue<PersistedObject> getQueue(String name) {
		return new LinkedBlockingQueue<PersistedObject>();
	}
	@Override
	public void shutdown() {
		// TODO Auto-generated method stub
		
	}

	
}
