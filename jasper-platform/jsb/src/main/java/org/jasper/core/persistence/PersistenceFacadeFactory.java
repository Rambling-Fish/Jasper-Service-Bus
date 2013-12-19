package org.jasper.core.persistence;

import java.util.Properties;

public class PersistenceFacadeFactory {

	public static PersistenceFacade getFacade(Properties prop) {
		return new PersistenceFacade(prop);
	}
	
	public static PersistenceFacade getFacade(String localIP, String groupName, String groupPassword){
		return new PersistenceFacade(localIP, groupName, groupPassword);
	}

}
