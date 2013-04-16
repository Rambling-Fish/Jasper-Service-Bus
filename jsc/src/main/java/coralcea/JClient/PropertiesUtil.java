package coralcea.JClient;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;



public class PropertiesUtil
{
 
  private static Properties props;
 
  static
  {
    props = new Properties();
    try
    {
      PropertiesUtil util = new PropertiesUtil();
      props = util.getPropertiesFromClasspath("coralcea/JClient/resources/JClient.properties");
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
   * a place holder in case other properties are located elsewhere when deployed in tomcat.
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
    InputStream inputStream = null;
        
 
    if (inputStream == null)
    {
      throw new FileNotFoundException("property file '" + propFileName
          + "' not found");
    }
 
    props.load(inputStream);
    return props;
  }
}