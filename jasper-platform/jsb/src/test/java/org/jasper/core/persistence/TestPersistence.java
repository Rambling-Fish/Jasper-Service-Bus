package org.jasper.core.persistence;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;

import javax.jms.Destination;

import junit.framework.TestCase;

import org.jasper.core.notification.triggers.Trigger;
import org.junit.Before;
import org.junit.Test;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.MultiMap;


public class TestPersistence extends TestCase {
	private PersistenceFacade cachingSys;
	private String ipAddr;
	Properties props;
	private String password = "facadePassword";
	private String hazelcastGroup = UUID.randomUUID().toString();

	
	/*
	 * This tests the PersistenceFacadeFactory class
	 */
	@Test
	public void testPersisenceFacadeFactory() throws Exception {
		System.out.println("=========================");
		System.out.println("RUNNING PERSISTENCE TESTS");
		System.out.println("=========================");

		cachingSys = PersistenceFacadeFactory.getFacade(ipAddr, hazelcastGroup, password);
		cachingSys.shutdown();
		cachingSys = PersistenceFacadeFactory.getFacade(props);
		cachingSys.shutdown();
	}
	
	/*
	 * This tests the PersistenceFacadeImpl class
	 */
	@Test
	public void testPersistenceFacade() throws Exception {
		MultiMap<String,String> multiMap;
		Map<String,String> myMap;
		cachingSys = new PersistenceFacadeImpl(props);
		multiMap = (MultiMap<String, String>) cachingSys.getMultiMap("testMap");
		myMap = (Map<String, String>) cachingSys.getMap("aMap");
		Object myObj = cachingSys.getSharedMemoryInstance();
		BlockingQueue myQ = cachingSys.getQueue("myQ");
		
		TestCase.assertNotNull(multiMap);
		TestCase.assertNotNull(myMap);
		TestCase.assertNotNull(myObj);
		TestCase.assertNotNull(myQ);
		
		cachingSys.shutdown();
	}
	
	/*
	 * This tests the MemoryCache class
	 */
	@Test
	public void testMemoryCache() throws Exception {
		HazelcastInstance inst;
		MemoryCache cache = new MemoryCache(ipAddr, "group", "password");
		cachingSys = new PersistenceFacadeImpl(props);
		inst = (HazelcastInstance) cachingSys.getSharedMemoryInstance();
		cache.setHazelcastInstance(inst);
		cache.shutdown();
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
		TestCase.assertEquals(contentType, "contentType");
		TestCase.assertEquals(corrId, "correlationId");
		TestCase.assertEquals(dtaParms, "dtaParms");
		TestCase.assertEquals(key, "key");
		TestCase.assertNull(notify);
		TestCase.assertEquals(output, "output");
		TestCase.assertNull(dest);
		TestCase.assertEquals(request, "request");
		TestCase.assertNull(triggerList);
		TestCase.assertEquals(inst, "UDEInstance");
		TestCase.assertEquals(version, "version");
		
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


	@Before
	public void setUp() throws Exception {
		ipAddr = InetAddress.getLocalHost().getHostAddress();
		props = new Properties();
		props.setProperty("persisitence.localIp", ipAddr);
		props.setProperty("persisitence.groupName", hazelcastGroup);
		props.setProperty("persisitence.groupPassword", password);
	}
}
