package org.jasper.core.persistence;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

import com.hazelcast.core.MultiMap;

public interface PersistenceFacade {

	public abstract MultiMap<?, ?> getMultiMap(String name);

	public abstract Map<?, ?> getMap(String name);

	public abstract Object getSharedMemoryInstance();

	public abstract BlockingQueue<PersistedObject> getQueue(String name);

	public abstract void shutdown();

}