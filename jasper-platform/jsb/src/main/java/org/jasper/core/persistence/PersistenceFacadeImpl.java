package org.jasper.core.persistence;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;

import com.hazelcast.core.MultiMap;

public class PersistenceFacadeImpl implements PersistenceFacade {
	
	private MemoryCache memCache;
	
	public PersistenceFacadeImpl(Properties prop){
		String localIP = prop.getProperty("persisitence.localIp");
		String groupName  = prop.getProperty("persisitence.groupName");
		String groupPassword  = prop.getProperty("persisitence.groupPassword");
		memCache = new MemoryCache(localIP, groupName, groupPassword);
	}
	
	public PersistenceFacadeImpl(String localIP, String groupName, String groupPassword){
		memCache = new MemoryCache(localIP, groupName, groupPassword);
	}
	
	/* (non-Javadoc)
	 * @see org.jasper.core.persistence.PersistenceFacade#getMultiMap(java.lang.String)
	 */
	@Override
	public MultiMap<?,?> getMultiMap(String name) {
		return memCache.getMultiMap(name);
	}
	
	/* (non-Javadoc)
	 * @see org.jasper.core.persistence.PersistenceFacade#getMap(java.lang.String)
	 */
	@Override
	public Map<?,?> getMap(String name) {
		return memCache.getMap(name);
	}
	
	/* (non-Javadoc)
	 * @see org.jasper.core.persistence.PersistenceFacade#getSharedMemoryInstance()
	 */
	@Override
	public Object getSharedMemoryInstance(){
		return memCache.getHazelcastInstance();
	}
	
	/* (non-Javadoc)
	 * @see org.jasper.core.persistence.PersistenceFacade#getQueue(java.lang.String)
	 */
	@Override
	public BlockingQueue<PersistedObject> getQueue(String name) {
		return memCache.getQueue(name);
	}
	
	/* (non-Javadoc)
	 * @see org.jasper.core.persistence.PersistenceFacade#shutdown()
	 */
	@Override
	public void shutdown(){
		memCache.shutdown();
	}
	
}
