package org.jasper.jsc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;



public class PropertiesUtil
{
 
  private static Properties props;
  private final static String CATALINA_HOME =  System.getProperty("catalina.base");
  private final static String JCLIENT_PROPERTIES = "JClient.properties";
 
  static
  {
    props = new Properties();
    try
    {
      PropertiesUtil util = new PropertiesUtil();
      props = util.getPropertiesFromClasspath("org/jasper/jsc/resources/JClient.properties");
      String deployToWebContainer = "no";
      if (props.getProperty("jclient.tw") != null) {
    	  deployToWebContainer = props.getProperty("jclient.tw");
      }
		 if (deployToWebContainer.equalsIgnoreCase("yes")) {
			 Properties props2 = new Properties();
			 String catalinaPropFileName = (CATALINA_HOME + (String) File.separator + JCLIENT_PROPERTIES );
			 System.out.println("deploying into tomcat for TW" + catalinaPropFileName); 
			 props2 = util.getPropertiesFromCatalina(catalinaPropFileName );
		     System.out.println("deploying into tomcat for TW" + catalinaPropFileName); 
		     props.putAll(props2);
		 }
    }
    catch (FileNotFoundException e)
    {
      e.printStackTrace();
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
  }
 
  // private constructor
  private PropertiesUtil()
  {
  }
 
  public static String getProperty(String key)
  {
    return props.getProperty(key);
  }
 
  public static Set<Object> getkeys()
  {
    return props.keySet();
  }
 
  /**
   * loads properties file from classpath
   *
   * @param propFileName
   * @return
   * @throws IOException
   */
  private Properties getPropertiesFromClasspath(String propFileName)
                                                                    throws IOException
  {
    Properties props = new Properties();
    InputStream inputStream =
        this.getClass().getClassLoader().getResourceAsStream(propFileName);
 
    if (inputStream == null)
    {
      throw new FileNotFoundException("property file '" + propFileName
          + "' not found in the classpath");
    }
 
    props.load(inputStream);
    return props;
  }
  
  /**
   * loads properties file from catalina.
   * Properties are located elsewhere when deployed in tomcat.
   *
   * @param propFileName
   * @return
   * @throws IOException
   */
  @SuppressWarnings("unused")
	private Properties getPropertiesFromCatalina(String propFileName)
                                                                    throws IOException
  {
	Properties props = new Properties();
    // Need to fill this with the proper location of the properties file in Tomcat.
    // Ok for now.
    InputStream inputStream = new FileInputStream(propFileName);
        
    if (inputStream == null)
    {
      throw new FileNotFoundException("property file '" + propFileName
          + "' not found");
    }
 
    props.load(inputStream);
    return props;
  }
}