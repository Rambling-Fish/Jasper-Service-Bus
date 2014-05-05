package org.jasper.core.persistence;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.mule.util.concurrent.ConcurrentHashSet;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.EntryView;
import com.hazelcast.core.IMap;
import com.hazelcast.map.EntryProcessor;
import com.hazelcast.map.MapInterceptor;
import com.hazelcast.monitor.LocalMapStats;
import com.hazelcast.query.Predicate;

public class LocalIMap<K, V> implements IMap {

	private Map<K,V> localmap;
	private Map<String, EntryListener> listeners;
	private Map<String, Boolean> listenersIncludeValue;

	public LocalIMap(){
		localmap = new ConcurrentHashMap<K, V>();
		listeners = new ConcurrentHashMap<String, EntryListener>();
		listenersIncludeValue = new ConcurrentHashMap<String, Boolean>();
	}
	
	@Override
	public int size() {
		return localmap.size();
	}

	@Override
	public boolean isEmpty() {
		return localmap.isEmpty();
	}

	@Override
	public void putAll(Map m) {
		localmap.putAll(m);
		for(Object key:m.keySet()){
			Object value = m.get(key);
			for(String registrationId:listeners.keySet()){
				listenersIncludeValue.get(registrationId);
				EntryEvent<K, V> event = new EntryEvent<K, V>(null, null, 0, (K) key, (listenersIncludeValue.get(registrationId)?(V)value:null));
				listeners.get(registrationId).entryAdded(event);	
			}
		}
	}

	@Override
	public void clear() {
		localmap.clear();
	}
	
	@Override
	public boolean containsKey(Object key) {
		return localmap.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return localmap.containsValue(value);
	}

	@Override
	public Object get(Object key) {
		return localmap.get(key);
	}

	@Override
	public Object put(Object key, Object value) {
		Object old_object = localmap.remove(key);
		localmap.put((K)key, (V)value);
		for(String registrationId:listeners.keySet()){
			listenersIncludeValue.get(registrationId);
			EntryEvent<K, V> event = new EntryEvent<K, V>(null, null, 0, (K) key, (listenersIncludeValue.get(registrationId)?(V)value:null));
			if(old_object == null){
				listeners.get(registrationId).entryAdded(event);	
			}else{
				listeners.get(registrationId).entryUpdated(event);	
			}
		}
		return old_object;
	}

	@Override
	public Object remove(Object key) {
		Object value = localmap.remove(key);
		if(value == null) return null;
		
		for(String registrationId:listeners.keySet()){
			listenersIncludeValue.get(registrationId);
			EntryEvent<K, V> event = new EntryEvent<K, V>(null, null, 0, (K) key, (listenersIncludeValue.get(registrationId)?(V)value:null));
			listeners.get(registrationId).entryRemoved(event);	
		}
		return value;
	}

	@Override
	public boolean remove(Object key, Object value) {
		if (localmap.containsKey(key) && localmap.get(key).equals(value)) {
			localmap.remove(key);
			for(String registrationId:listeners.keySet()){
				listenersIncludeValue.get(registrationId);
				EntryEvent<K, V> event = new EntryEvent<K, V>(null, null, 0, (K) key, (listenersIncludeValue.get(registrationId)?(V)value:null));
				listeners.get(registrationId).entryRemoved(event);	
			}
			return true;
		} else{
			return false;
		}
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
	
	@Override
	public Set keySet() {
		return localmap.keySet();
	}

	@Override
	public Collection values() {
		return localmap.values();
	}

	@Override
	public Set entrySet() {
		return localmap.entrySet();
	}
	

	
	//################

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
	public String getName() {
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
	public void delete(Object key) {
		// TODO Auto-generated method stub

	}

	@Override
	public void flush() {
		// TODO Auto-generated method stub

	}

	@Override
	public Map getAll(Set keys) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future getAsync(Object key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future putAsync(Object key, Object value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future putAsync(Object key, Object value, long ttl, TimeUnit timeunit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future removeAsync(Object key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean tryRemove(Object key, long timeout, TimeUnit timeunit) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean tryPut(Object key, Object value, long timeout, TimeUnit timeunit) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Object put(Object key, Object value, long ttl, TimeUnit timeunit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void putTransient(Object key, Object value, long ttl, TimeUnit timeunit) {
		// TODO Auto-generated method stub

	}

	@Override
	public Object putIfAbsent(Object key, Object value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object putIfAbsent(Object key, Object value, long ttl, TimeUnit timeunit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean replace(Object key, Object oldValue, Object newValue) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Object replace(Object key, Object value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void set(Object key, Object value) {
		// TODO Auto-generated method stub

	}

	@Override
	public void set(Object key, Object value, long ttl, TimeUnit timeunit) {
		// TODO Auto-generated method stub

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
	public String addLocalEntryListener(EntryListener listener) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String addInterceptor(MapInterceptor interceptor) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeInterceptor(String id) {
		// TODO Auto-generated method stub

	}

	@Override
	public String addEntryListener(EntryListener listener, Object key, boolean includeValue) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String addEntryListener(EntryListener listener, Predicate predicate, boolean includeValue) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String addEntryListener(EntryListener listener, Predicate predicate, Object key, boolean includeValue) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public EntryView getEntryView(Object key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean evict(Object key) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Set keySet(Predicate predicate) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set entrySet(Predicate predicate) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection values(Predicate predicate) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set localKeySet() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set localKeySet(Predicate predicate) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addIndex(String attribute, boolean ordered) {
		// TODO Auto-generated method stub

	}

	@Override
	public LocalMapStats getLocalMapStats() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object executeOnKey(Object key, EntryProcessor entryProcessor) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map executeOnEntries(EntryProcessor entryProcessor) {
		// TODO Auto-generated method stub
		return null;
	}

}
