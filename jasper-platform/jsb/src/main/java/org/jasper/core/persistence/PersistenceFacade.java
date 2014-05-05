package org.jasper.core.persistence;

import java.util.concurrent.BlockingQueue;

import com.hazelcast.core.IMap;
import com.hazelcast.core.MultiMap;

public interface PersistenceFacade {

	public abstract <K, V> MultiMap<K, V> getMultiMap(String name);

	public abstract <K, V> IMap<K, V> getMap(String name);

	public abstract Object getSharedMemoryInstance();

	public abstract BlockingQueue<PersistedObject> getQueue(String name);

	public abstract void shutdown();

}