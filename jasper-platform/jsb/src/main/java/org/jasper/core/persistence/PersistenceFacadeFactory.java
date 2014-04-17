package org.jasper.core.persistence;

import java.util.Properties;

public class PersistenceFacadeFactory {

	public static PersistenceFacade getFacade(Properties prop) {
		return new PersistenceFacadeImpl(prop);
	}
	
	public static PersistenceFacade getFacade(String localIP, String groupName, String groupPassword){
		return new PersistenceFacadeImpl(localIP, groupName, groupPassword);
	}

	public static PersistenceFacade getNonClusteredFacade() {
		return new PersistenceFacadeNonClusteredImpl();
	}

}
