package org.jasper.core.persistence;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.hazelcast.core.EntryListener;
import com.hazelcast.core.EntryView;
import com.hazelcast.core.IMap;
import com.hazelcast.core.MultiMap;
import com.hazelcast.map.EntryProcessor;
import com.hazelcast.map.MapInterceptor;
import com.hazelcast.monitor.LocalMapStats;
import com.hazelcast.monitor.LocalMultiMapStats;
import com.hazelcast.query.Predicate;

public class PersistenceFacadeNonClusteredImpl implements PersistenceFacade {
	
	private static Map<String,MultiMap<?, ?>> multiMap = new ConcurrentHashMap<String, MultiMap<?,?>>();
	private static Map<String,IMap<?, ?>> map = new ConcurrentHashMap<>();
	
//	@Override
//	public MultiMap<?, ?> getMultiMap(String name) {
//		
//		if(multiMap.containsKey(name)) return multiMap.get(name);
//		
//		MultiMap<?, ?> result = new MultiMap<K, V>() {
//
//			@Override
//			public void destroy() {
//				// TODO Auto-generated method stub
//				
//			}
//
//			@Override
//			public Object getId() {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public String getPartitionKey() {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public String getServiceName() {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public String addEntryListener(EntryListener<K, V> arg0,
//					boolean arg1) {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public String addEntryListener(EntryListener<K, V> arg0, K arg1,
//					boolean arg2) {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public String addLocalEntryListener(EntryListener<K, V> arg0) {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public void clear() {
//				// TODO Auto-generated method stub
//				
//			}
//
//			@Override
//			public boolean containsEntry(K arg0, V arg1) {
//				// TODO Auto-generated method stub
//				return false;
//			}
//
//			@Override
//			public boolean containsKey(K arg0) {
//				// TODO Auto-generated method stub
//				return false;
//			}
//
//			@Override
//			public boolean containsValue(Object arg0) {
//				// TODO Auto-generated method stub
//				return false;
//			}
//
//			@Override
//			public Set<Entry<K, V>> entrySet() {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public void forceUnlock(K arg0) {
//				// TODO Auto-generated method stub
//				
//			}
//
//			@Override
//			public Collection<V> get(K arg0) {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public LocalMultiMapStats getLocalMultiMapStats() {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public String getName() {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public boolean isLocked(K arg0) {
//				// TODO Auto-generated method stub
//				return false;
//			}
//
//			@Override
//			public Set<K> keySet() {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public Set<K> localKeySet() {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public void lock(K arg0) {
//				// TODO Auto-generated method stub
//				
//			}
//
//			@Override
//			public void lock(K arg0, long arg1, TimeUnit arg2) {
//				// TODO Auto-generated method stub
//				
//			}
//
//			@Override
//			public boolean put(K arg0, V arg1) {
//				// TODO Auto-generated method stub
//				return false;
//			}
//
//			@Override
//			public Collection<V> remove(Object arg0) {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public boolean remove(Object arg0, Object arg1) {
//				// TODO Auto-generated method stub
//				return false;
//			}
//
//			@Override
//			public boolean removeEntryListener(String arg0) {
//				// TODO Auto-generated method stub
//				return false;
//			}
//
//			@Override
//			public int size() {
//				// TODO Auto-generated method stub
//				return 0;
//			}
//
//			@Override
//			public boolean tryLock(K arg0) {
//				// TODO Auto-generated method stub
//				return false;
//			}
//
//			@Override
//			public boolean tryLock(K arg0, long arg1, TimeUnit arg2)
//					throws InterruptedException {
//				// TODO Auto-generated method stub
//				return false;
//			}
//
//			@Override
//			public void unlock(K arg0) {
//				// TODO Auto-generated method stub
//				
//			}
//
//			@Override
//			public int valueCount(K arg0) {
//				// TODO Auto-generated method stub
//				return 0;
//			}
//
//			@Override
//			public Collection<V> values() {
//				// TODO Auto-generated method stub
//				return null;
//			}
//		};
//		
//		return ;
//	}
//
//	@Override
//	public Map<?, ?> getMap(String name) {
//		if(multiMap.containsKey(name)) return map.get(name);
//	
//		IMap<?, ?> result = new IMap<K, V>() {
//
//			@Override
//			public int size() {
//				// TODO Auto-generated method stub
//				return 0;
//			}
//
//			@Override
//			public boolean isEmpty() {
//				// TODO Auto-generated method stub
//				return false;
//			}
//
//			@Override
//			public void putAll(Map<? extends K, ? extends V> m) {
//				// TODO Auto-generated method stub
//				
//			}
//
//			@Override
//			public void clear() {
//				// TODO Auto-generated method stub
//				
//			}
//
//			@Override
//			public void destroy() {
//				// TODO Auto-generated method stub
//				
//			}
//
//			@Override
//			public Object getId() {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public String getName() {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public String getPartitionKey() {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public String getServiceName() {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public String addEntryListener(EntryListener<K, V> arg0,
//					boolean arg1) {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public String addEntryListener(EntryListener<K, V> arg0, K arg1,
//					boolean arg2) {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public String addEntryListener(EntryListener<K, V> arg0,
//					Predicate<K, V> arg1, boolean arg2) {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public String addEntryListener(EntryListener<K, V> arg0,
//					Predicate<K, V> arg1, K arg2, boolean arg3) {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public void addIndex(String arg0, boolean arg1) {
//				// TODO Auto-generated method stub
//				
//			}
//
//			@Override
//			public String addInterceptor(MapInterceptor arg0) {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public String addLocalEntryListener(EntryListener<K, V> arg0) {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public boolean containsKey(Object arg0) {
//				// TODO Auto-generated method stub
//				return false;
//			}
//
//			@Override
//			public boolean containsValue(Object arg0) {
//				// TODO Auto-generated method stub
//				return false;
//			}
//
//			@Override
//			public void delete(Object arg0) {
//				// TODO Auto-generated method stub
//				
//			}
//
//			@Override
//			public Set<java.util.Map.Entry<K, V>> entrySet() {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public Set<java.util.Map.Entry<K, V>> entrySet(Predicate arg0) {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public boolean evict(K arg0) {
//				// TODO Auto-generated method stub
//				return false;
//			}
//
//			@Override
//			public Map<K, Object> executeOnEntries(EntryProcessor arg0) {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public Object executeOnKey(K arg0, EntryProcessor arg1) {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public void flush() {
//				// TODO Auto-generated method stub
//				
//			}
//
//			@Override
//			public void forceUnlock(K arg0) {
//				// TODO Auto-generated method stub
//				
//			}
//
//			@Override
//			public V get(Object arg0) {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public Map<K, V> getAll(Set<K> arg0) {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public Future<V> getAsync(K arg0) {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public EntryView<K, V> getEntryView(K arg0) {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public LocalMapStats getLocalMapStats() {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public boolean isLocked(K arg0) {
//				// TODO Auto-generated method stub
//				return false;
//			}
//
//			@Override
//			public Set<K> keySet() {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public Set<K> keySet(Predicate arg0) {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public Set<K> localKeySet() {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public Set<K> localKeySet(Predicate arg0) {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public void lock(K arg0) {
//				// TODO Auto-generated method stub
//				
//			}
//
//			@Override
//			public void lock(K arg0, long arg1, TimeUnit arg2) {
//				// TODO Auto-generated method stub
//				
//			}
//
//			@Override
//			public V put(K arg0, V arg1) {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public V put(K arg0, V arg1, long arg2, TimeUnit arg3) {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public Future<V> putAsync(K arg0, V arg1) {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public Future<V> putAsync(K arg0, V arg1, long arg2, TimeUnit arg3) {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public V putIfAbsent(K arg0, V arg1) {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public V putIfAbsent(K arg0, V arg1, long arg2, TimeUnit arg3) {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public void putTransient(K arg0, V arg1, long arg2, TimeUnit arg3) {
//				// TODO Auto-generated method stub
//				
//			}
//
//			@Override
//			public V remove(Object arg0) {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public boolean remove(Object arg0, Object arg1) {
//				// TODO Auto-generated method stub
//				return false;
//			}
//
//			@Override
//			public Future<V> removeAsync(K arg0) {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public boolean removeEntryListener(String arg0) {
//				// TODO Auto-generated method stub
//				return false;
//			}
//
//			@Override
//			public void removeInterceptor(String arg0) {
//				// TODO Auto-generated method stub
//				
//			}
//
//			@Override
//			public V replace(K arg0, V arg1) {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public boolean replace(K arg0, V arg1, V arg2) {
//				// TODO Auto-generated method stub
//				return false;
//			}
//
//			@Override
//			public void set(K arg0, V arg1) {
//				// TODO Auto-generated method stub
//				
//			}
//
//			@Override
//			public void set(K arg0, V arg1, long arg2, TimeUnit arg3) {
//				// TODO Auto-generated method stub
//				
//			}
//
//			@Override
//			public boolean tryLock(K arg0) {
//				// TODO Auto-generated method stub
//				return false;
//			}
//
//			@Override
//			public boolean tryLock(K arg0, long arg1, TimeUnit arg2)
//					throws InterruptedException {
//				// TODO Auto-generated method stub
//				return false;
//			}
//
//			@Override
//			public boolean tryPut(K arg0, V arg1, long arg2, TimeUnit arg3) {
//				// TODO Auto-generated method stub
//				return false;
//			}
//
//			@Override
//			public boolean tryRemove(K arg0, long arg1, TimeUnit arg2) {
//				// TODO Auto-generated method stub
//				return false;
//			}
//
//			@Override
//			public void unlock(K arg0) {
//				// TODO Auto-generated method stub
//				
//			}
//
//			@Override
//			public Collection<V> values() {
//				// TODO Auto-generated method stub
//				return null;
//			}
//
//			@Override
//			public Collection<V> values(Predicate arg0) {
//				// TODO Auto-generated method stub
//				return null;
//			}
//		};
//		
//		return result;
//	}
//		

	@Override
	public Object getSharedMemoryInstance() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BlockingQueue<PersistedObject> getQueue(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void shutdown() {
		// TODO Auto-generated method stub

	}

	@Override
	public MultiMap<?, ?> getMultiMap(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<?, ?> getMap(String name) {
		// TODO Auto-generated method stub
		return null;
	}

}
