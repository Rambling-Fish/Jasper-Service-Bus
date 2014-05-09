package org.jasper.core.persistence;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.jms.Destination;

import junit.framework.TestCase;

import org.jasper.core.notification.triggers.Trigger;
import org.junit.Before;
import org.junit.Test;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.MultiMap;
import com.hazelcast.map.EntryProcessor;
import com.hazelcast.map.MapInterceptor;
import com.hazelcast.query.Predicate;


public class TestPersistence extends TestCase implements EntryListener<String, String>{
	private PersistenceFacade cachingSys;
	private String ipAddr;
	Properties props;
	private String password = "facadePassword";
	private String hazelcastGroup = UUID.randomUUID().toString();
	private String ruri = "http://coralcea.ca/jasper/hrSID";
	private static Destination dest;

	
	/*
	 * This tests the PersistenceFacadeFactory class
	 */
	@Test
	public void testPersisenceFacadeFactory() throws Exception {
		System.out.println("=========================");
		System.out.println("RUNNING PERSISTENCE TESTS");
		System.out.println("=========================");

		cachingSys = PersistenceFacadeFactory.getNonClusteredFacade();
		cachingSys = PersistenceFacadeFactory.getFacade(props);
		TestCase.assertNotNull(cachingSys);
		cachingSys.shutdown();
	}
	
	/*
	 * This tests the PersistenceFacadeImpl class
	 */
	@Test
	public void testPersistenceFacade() throws Exception {
		MultiMap<String,String> multiMap;
		Map<String,String> myMap;
		cachingSys = PersistenceFacadeFactory.getNonClusteredFacade();
		multiMap = cachingSys.getMultiMap("testMap");
		myMap = cachingSys.getMap("aMap");
		Object myObj = cachingSys.getSharedMemoryInstance();
		BlockingQueue myQ = cachingSys.getQueue("myQ");
		
		TestCase.assertNotNull(multiMap);
		TestCase.assertNotNull(myMap);
		TestCase.assertNotNull(myQ);
		TestCase.assertNull(myObj);
		
		cachingSys.shutdown();
	}
	
	/*
	 * This tests the PersistedObject class
	 */
	@Test
	public void testPersistedObject() throws Exception {
		Destination replyTo = null;
		List<Trigger> triggerList;
		PersistedObject obj = new PersistedObject("key", "correlationId", "request", "ruri", "dtaParms",
				replyTo, false, "UDEInstance", "output", "version", "contentType", "GET", 10);
		
		// test all getters
		String contentType = obj.getContentType();
		String corrId = obj.getCorrelationID();
		String dtaParms = obj.getDtaParms();
		String key = obj.getKey();
		String notify = obj.getNotification();
		String output = obj.getOutput();
		Destination dest = obj.getReplyTo();
		String request = obj.getRequest();
		String ruri = obj.getRURI();
		triggerList = obj.getTriggers();
		String inst = obj.getUDEInstance();
		String version = obj.getVersion();
		String method  = obj.getMethod();
		int expires    = obj.getExpires();
		obj.setMethod("PUBLISH");
		obj.setExpires(10);
		obj.setSubscriptionId("12345");
		String subId = obj.getSubscriptionId();
		boolean isNotify = obj.isNotificationRequest();
		TestCase.assertEquals(contentType, "contentType");
		TestCase.assertEquals(corrId, "correlationId");
		TestCase.assertEquals(dtaParms, "dtaParms");
		TestCase.assertEquals(ruri, "ruri");
		TestCase.assertEquals(method, "GET");
		TestCase.assertEquals(key, "key");
		TestCase.assertNull(notify);
		TestCase.assertEquals(output, "output");
		TestCase.assertNull(dest);
		TestCase.assertEquals(request, "request");
		TestCase.assertNull(triggerList);
		TestCase.assertEquals(inst, "UDEInstance");
		TestCase.assertEquals(version, "version");
		TestCase.assertEquals(false, isNotify);
		TestCase.assertEquals(expires, 10);
		TestCase.assertEquals(subId, "12345");
		
		// test all setters
		obj = null;
		triggerList = new ArrayList<Trigger>();
		obj = new PersistedObject();
		obj.setContentType("contentType");
		obj.setCorrelationID("correlationID");
		obj.setDtaParms("dtaParms");
		obj.setIsNotificationRequest(true);
		obj.setKey("key");
		obj.setNotification("notification");
		obj.setOutput("output");
		obj.setReplyTo(replyTo);
		obj.setRequest("request");
		obj.setRURI("ruri");
		obj.setUDEInstance("UDEInstance");
		obj.setVersion("version");
		obj.setTriggers(triggerList);
	}
	
	@Test
	public void testPersistedRequests() throws Exception {
		PersistedDataRequest dataReq = new PersistedDataRequest("123", dest, null, System.currentTimeMillis());
		PersistedSubscriptionRequest subReq = new PersistedSubscriptionRequest(ruri, "12345", "123", "application/json", dest, null, -1, System.currentTimeMillis());
		String corrID = dataReq.getCorrelationID();
		dataReq.setCorrelationID("123");
		dataReq.setReply2Q(dest);
		dataReq.setRequest("text");
		Destination newDest = dataReq.getReply2Q();
		String storedRuri = subReq.getRuri();
		subReq.setRuri(ruri);
		TestCase.assertEquals("123", corrID);
		TestCase.assertEquals(ruri, storedRuri);
		TestCase.assertNull(newDest);
		String subID = subReq.getSubscriptionId();
		subReq.setSubscriptionId("12345");
		String responseType = subReq.getResponseType();
		subReq.setResponseType("application/ld+json");
		int expiry = subReq.getExpiry();
		subReq.setExpiry(expiry);
		long time = subReq.getTimestampMillis();
		subReq.setTimestampMillis(time);
		subReq.setTriggerList(null);
		subReq.getTriggerList();
		subReq.setCorrelationID("12");
		subReq.setReply2q(dest);
		subReq.getReply2q();
		
		TestCase.assertEquals("12345", subID);
		TestCase.assertEquals("application/json", responseType);
		TestCase.assertEquals("application/ld+json", subReq.getResponseType());
		TestCase.assertEquals("12", subReq.getCorrelationID());
		
	}
	
	@Test
	public void testLocalIMap() throws Exception {
		// Test local IMap
		PersistenceFacade localCache = PersistenceFacadeFactory.getNonClusteredFacade();
		IMap<String,String> localIMap = localCache.getMap("local");
		Map<String,String> newMap = new HashMap<String,String>();
		newMap.put("1", "one");
		newMap.put("2", "two");
		localIMap.putAll(newMap);
		TestCase.assertEquals(false, localIMap.isEmpty());
		TestCase.assertEquals(true, localIMap.containsKey("1"));
		TestCase.assertEquals(true, localIMap.containsValue("two"));
		String obj = localIMap.remove("2");
		TestCase.assertEquals("two", obj);
		obj = localIMap.remove("7");
		TestCase.assertNull(obj);
		boolean res = localIMap.remove("1", "one");
		TestCase.assertEquals(true, res);
		res = localIMap.remove("6", "test");
		TestCase.assertEquals(false, res);
		localIMap.removeEntryListener("25");
		
		// test misc methods
		localIMap.forceUnlock("2");
		localIMap.localKeySet();
		localIMap.getLocalMapStats();
		EntryProcessor<?, ?> entryProcessor = null;
		localIMap.executeOnEntries(entryProcessor);
		localIMap.executeOnKey("1", entryProcessor);
		localIMap.addIndex(ruri, true);
		Predicate<?, ?> predicate = null;
		localIMap.localKeySet(predicate );
		localIMap.values();
		localIMap.values(predicate);
		localIMap.entrySet(predicate);
		localIMap.keySet();
		localIMap.keySet(predicate);
		TestCase.assertEquals(false, localIMap.evict("1"));
		localIMap.getEntryView("1");
		MapInterceptor interceptor = null;
		localIMap.addInterceptor(interceptor );
		localIMap.removeInterceptor("2");
		localIMap.unlock("1");
		localIMap.tryLock("2");
		localIMap.tryLock("1", 10, TimeUnit.SECONDS);
		localIMap.isLocked("1");
		localIMap.lock("2");
		localIMap.lock("1", 10, TimeUnit.SECONDS);
		localIMap.set("1", "one");
		localIMap.set("1", "one", 10, TimeUnit.SECONDS);
		localIMap.replace("2", "too");
		localIMap.replace("1", "one", "won");
		localIMap.putIfAbsent("3", "three");
		localIMap.putIfAbsent("1", "one", 1, TimeUnit.SECONDS);
		localIMap.putTransient("2", "two", 1, TimeUnit.SECONDS);
		localIMap.putAsync("5",  "five");
		localIMap.getAsync("3");
		localIMap.putAsync("5",  "five", 2, TimeUnit.SECONDS);
		localIMap.removeAsync("3");
		localIMap.getPartitionKey();
		localIMap.getName();
		localIMap.getServiceName();
		localIMap.destroy();
		localIMap.delete("5");
		localIMap.flush();
		Set<String> keys = null;
		localIMap.getAll(keys );
		localIMap.tryRemove("1", 1, TimeUnit.SECONDS);
		localIMap.tryPut("2", "too", 1, TimeUnit.SECONDS);
		localIMap.addLocalEntryListener(this);
		localIMap.put("1", "one", 1, TimeUnit.SECONDS);
		TestCase.assertNotNull(localIMap.entrySet());
		
		String registrationId = localIMap.addEntryListener(this , true);
		TestCase.assertNotNull(registrationId);
		localIMap.removeEntryListener(registrationId);
		
		localIMap.clear();
		TestCase.assertEquals(true, localIMap.isEmpty());
		
		// Test local MultiMap
		
		
		
	}
	
	@Test
	public void testLocalMultiMap() throws Exception {
		PersistenceFacade localCache = PersistenceFacadeFactory.getNonClusteredFacade();
		MultiMap<String,String> localMultiMap = localCache.getMultiMap("multi");
		TestCase.assertEquals(true, localMultiMap.put("1", "one"));
		localMultiMap.put("1", "one");
		TestCase.assertEquals(false, localMultiMap.put("2",  null));
		TestCase.assertNotNull(localMultiMap.get("1"));
		TestCase.assertEquals(false, localMultiMap.remove("2", "two"));
		TestCase.assertEquals(true, localMultiMap.remove("1", "one"));
		TestCase.assertNull(localMultiMap.remove(null));
		TestCase.assertEquals(0, localMultiMap.size());
		
		TestCase.assertEquals(true, localMultiMap.put("2", "two"));
		TestCase.assertNotNull(localMultiMap.entrySet());
		TestCase.assertEquals(true, localMultiMap.containsKey("2"));
		TestCase.assertEquals(false, localMultiMap.containsKey("99"));	
		TestCase.assertEquals(true, localMultiMap.containsValue("two"));
		TestCase.assertEquals(false, localMultiMap.containsValue("99"));
		TestCase.assertEquals(true, localMultiMap.containsEntry("2", "two"));
		TestCase.assertNotNull(localMultiMap.remove("2"));
		TestCase.assertNull(localMultiMap.remove("2"));
		
		// Misc methods
		TestCase.assertNull(localMultiMap.getLocalMultiMapStats());
		TestCase.assertEquals(false, localMultiMap.isLocked("2"));
		localMultiMap.forceUnlock("2");
		localMultiMap.unlock("2");
		localMultiMap.tryLock("3");
		localMultiMap.tryLock("99", 1, TimeUnit.SECONDS);
		localMultiMap.lock("9");
		localMultiMap.lock("9", 1, TimeUnit.SECONDS);
		localMultiMap.clear();
		TestCase.assertNull(localMultiMap.addLocalEntryListener(this));
		TestCase.assertEquals(0, localMultiMap.valueCount("6"));
		TestCase.assertNull(localMultiMap.values());
		TestCase.assertNull(localMultiMap.keySet());
		TestCase.assertNull(localMultiMap.localKeySet());
		TestCase.assertNull(localMultiMap.getName());
		TestCase.assertNull(localMultiMap.getServiceName());
		TestCase.assertNull(localMultiMap.getPartitionKey());
		TestCase.assertEquals(false, localMultiMap.removeEntryListener("123"));
		
	}
	
	/*
	 * This tests the MemoryCache class
	 */
	@Test
	public void testMemoryCache() throws Exception {
		HazelcastInstance inst;
		cachingSys = PersistenceFacadeFactory.getFacade(ipAddr, hazelcastGroup, "testpasswd");
		inst = (HazelcastInstance) cachingSys.getSharedMemoryInstance();
		
		BlockingQueue myQ = cachingSys.getQueue("myQ");
		TestCase.assertNotNull(myQ);
		TestCase.assertNotNull(inst);
		cachingSys.shutdown();
	}

	@Before
	public void setUp() throws Exception {
		ipAddr = InetAddress.getLocalHost().getHostAddress();
		props = new Properties();
		props.setProperty("persisitence.localIp", ipAddr);
		props.setProperty("persisitence.groupName", hazelcastGroup);
		props.setProperty("persisitence.groupPassword", password);
	}

	@Override
	public void entryAdded(EntryEvent<String, String> event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void entryRemoved(EntryEvent<String, String> event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void entryUpdated(EntryEvent<String, String> event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void entryEvicted(EntryEvent<String, String> event) {
		// TODO Auto-generated method stub
		
	}
}
