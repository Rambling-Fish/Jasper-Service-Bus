package org.jasper.jsc;

import java.util.List;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.naming.*;
import javax.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;

/**
 * Utility class for JMS sample programs.
 * <p>
 * Set the <code>USE_JNDI</code> variable to true or false depending on whether
 * your provider uses JNDI.
 * <p>
 * Contains the following methods:
 * <ul>
 * <li>getQueueConnectionFactory
 * <li>getQueue
 * <li>jndiLookup
 */
public class SampleUtilities {
	public  static final   boolean     USE_JNDI    = false;
	public  static final   String      QUEUECONFAC = "QueueConnectionFactory";
	private static         Context     jndiContext = null;

	/**
	 * Returns a QueueConnectionFactory object. If provider uses JNDI, serves as a
	 * wrapper around jndiLookup method. If provider does not use JNDI, substitute
	 * provider-specific code here.
	 * 
	 * @return a QueueConnectionFactory object
	 * @throws javax.naming.NamingException
	 *           (or other exception) if name cannot be found
	 */
	public static javax.jms.QueueConnectionFactory getQueueConnectionFactory()
			throws Exception {
		
		if (USE_JNDI) {
			return (QueueConnectionFactory) jndiLookup("ConnectionFactory");
		}
		String transportURL = PropertiesUtil.getProperty("jclient.transport");
		ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory(transportURL);
		
		return cf;
	}

	/**
	 * Returns a Queue object. If provider uses JNDI, serves as a wrapper around
	 * jndiLookup method. If provider does not use JNDI, substitute
	 * provider-specific code here.
	 * 
	 * @param name
	 *          String specifying queue name
	 * @param session
	 *          a QueueSession object
	 * 
	 * @return a Queue object
	 * @throws javax.naming.NamingException
	 *           (or other exception) if name cannot be found
	 */
	public static javax.jms.Queue getQueue(String name,
			javax.jms.QueueSession session) throws Exception {
		if (USE_JNDI) {
			return (javax.jms.Queue) jndiLookup(name);
		}
		else {
			return session.createQueue(name);
		}
	}

	/**
	 * Creates a JNDI InitialContext object if none exists yet. Then looks up the
	 * string argument and returns the associated object.
	 * 
	 * @param name
	 *          the name of the object to be looked up
	 * 
	 * @return the object bound to <code>name</code>
	 * @throws javax.naming.NamingException
	 *           if name cannot be found
	 */
	public static Object jndiLookup(String name) throws NamingException {
		Object obj = null;

		if (jndiContext == null) {
			try {
				jndiContext = new InitialContext();
			}
			catch (NamingException e) {
				System.out.println("Could not create JNDI context: " + e.toString());
				throw e;
			}
		}
		try {
			obj = jndiContext.lookup(name);
		}
		catch (NamingException e) {
			System.out.println("JNDI lookup failed: " + e.toString());
			throw e;
		}
		return obj;
	}

	/**
	 * 
	 * Accepts the query string part of the url.
	 * 
	 * <p>
	 * an empty query string will produce an empty Map a parameter without a value
	 * (?test) will be mapped to an empty List<String> Also knows how to handle
	 * keys repeated in legitimate query strings
	 * 
	 * @param url
	 *          url represent the query string as passed by TW.
	 * @throws UnsupportedEncodingException 
	 */

	public static Map<String, List<String>> getUrlParameters(String uri) 
			throws UnsupportedEncodingException
  {
		Map<String, List<String>> params = new HashMap<String, List<String>>();
		String[] urlParts = uri.split("\\?");
		
		if (urlParts.length > 1) {
			String query = urlParts[1];
			
			for (String param : query.split("&")) {
				String pair[] = param.split("=");
				// Very unlikely the decode to UTF-8 throws and UnsupportedEncodingException. 
				String key = URLDecoder.decode(pair[0], "UTF-8");
				String value = "";
				
				if (pair.length > 1) {
					value = URLDecoder.decode(pair[1], "UTF-8");
				}
				
				List<String> values = params.get(key);
				
				if (values == null) {
					values = new ArrayList<String>();
					params.put(key, values);
				}
				
				values.add(value);
			}
		}
		return params;
	}

	public static boolean validateQueryString(String queryUri)
			throws UnsupportedEncodingException {
		Map<String, List<String>> myMap;
		int j = 0;
		int i = 0;
		myMap = SampleUtilities.getUrlParameters(queryUri);
		for (String key : myMap.keySet()) {
			// Put a validation check here (i.e. key = Ward|VisitId|etc ..
			// Will do that shortly. For now just iterate and print.
		//	System.out.println("query param # " + i + " is " + key);
			i++;
			for (String value : myMap.get(key)) {
			//	System.out.println("query value # " + j + " equals to " + value);
				j++;
			}
			j = 0;
		}
		return true;
	}

}