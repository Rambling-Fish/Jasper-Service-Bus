package org.jasper.core.persistence;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.mule.util.concurrent.ConcurrentHashSet;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.MultiMap;
import com.hazelcast.monitor.LocalMultiMapStats;

public class LocalMultiMap<K, V> implements MultiMap {
	
	private Map<K,Collection<V>> localmap;
	private Map<String, EntryListener> listeners;
	private Map<String, Boolean> listenersIncludeValue;

	public LocalMultiMap(){
		localmap = new ConcurrentHashMap<K, Collection<V>>();
		listeners = new ConcurrentHashMap<String, EntryListener>();
		listenersIncludeValue = new ConcurrentHashMap<String, Boolean>();
	}
	
	@Override
	public boolean put(Object key, Object value) {
		if(key == null || value == null) return false;
		if(!localmap.containsKey(key)){
			localmap.put((K) key, new ConcurrentHashSet());
		}
		
		boolean result = localmap.get(key).add((V) value);
		if(result){
			for(String registrationId:listeners.keySet()){
				listenersIncludeValue.get(registrationId);
				EntryEvent<K, V> event = new EntryEvent<K, V>(null, null, 0, (K) key, (listenersIncludeValue.get(registrationId)?(V)value:null));
				listeners.get(registrationId).entryAdded(event);	
			}
		}
		return result;
	}

	@Override
	public Collection get(Object key) {
		return localmap.get(key);
	}

	@Override
	public boolean remove(Object key, Object value) {
		if(!localmap.containsKey(key)){
			return false;
		}
		
		boolean result = localmap.get(key).remove((V) value);
		if(result){
			for(String registrationId:listeners.keySet()){
				listenersIncludeValue.get(registrationId);
				EntryEvent<K, V> event = new EntryEvent<K, V>(null, null, 0, (K) key, (listenersIncludeValue.get(registrationId)?(V)value:null));
				listeners.get(registrationId).entryRemoved(event);	
			}
		}
		return result ;
		
	}

	@Override
	public Collection<V> remove(Object key) {
		if(key == null) return null;
		Collection<V> collection = localmap.remove(key);
		if(collection == null) return null;
		
		for(String registrationId:listeners.keySet()){
			listenersIncludeValue.get(registrationId);
			for(V value:collection){
				EntryEvent<K, V> event = new EntryEvent<K, V>(null, null, 0, (K) key, (listenersIncludeValue.get(registrationId)?value:null));
				listeners.get(registrationId).entryRemoved(event );
			}
			
		}
		
		return collection;
	}
	
	@Override
	public Set entrySet() {
		return localmap.entrySet();
	}

	@Override
	public boolean containsKey(Object key) {
		return localmap.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		for(Entry<K, Collection<V>> entry:localmap.entrySet()){
			if(entry.getValue().contains(value)){
				return true;
			}
		}
		
		return false;
	}

	@Override
	public boolean containsEntry(Object key, Object value) {
		return localmap.get(key).contains(value);
	}

	@Override
	public int size() {
		int size = 0;
		for(Entry<K, Collection<V>> entry:localmap.entrySet()){
			size += entry.getValue().size();
		}
		return size;
	}
	
	@Override
	public String addEntryListener(EntryListener listener, boolean includeValue) {
		String registrationId = UUID.randomUUID().toString();
		
		listeners.put(registrationId,listener);
		listenersIncludeValue.put(registrationId, includeValue);
		
		return registrationId;
	}

	@Override
	public boolean removeEntryListener(String registrationId) {
		EntryListener<K, V> listener = listeners.remove(registrationId);
		listenersIncludeValue.remove(registrationId);
		return (listener != null);
	}
	
	//##############
	
	@Override
	public String addEntryListener(EntryListener listener, Object key, boolean includeValue) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPartitionKey() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getServiceName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set localKeySet() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set keySet() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection values() {
		// TODO Auto-generated method stub
		return null;
	}
	


	@Override
	public void clear() {
		// TODO Auto-generated method stub

	}

	@Override
	public int valueCount(Object key) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String addLocalEntryListener(EntryListener listener) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void lock(Object key) {
		// TODO Auto-generated method stub

	}

	@Override
	public void lock(Object key, long leaseTime, TimeUnit timeUnit) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isLocked(Object key) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean tryLock(Object key) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean tryLock(Object key, long time, TimeUnit timeunit) throws InterruptedException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void unlock(Object key) {
		// TODO Auto-generated method stub

	}

	@Override
	public void forceUnlock(Object key) {
		// TODO Auto-generated method stub

	}

	@Override
	public LocalMultiMapStats getLocalMultiMapStats() {
		// TODO Auto-generated method stub
		return null;
	}

}
