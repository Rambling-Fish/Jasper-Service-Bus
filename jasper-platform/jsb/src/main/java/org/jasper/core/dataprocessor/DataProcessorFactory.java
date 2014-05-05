package org.jasper.core.dataprocessor;

import org.apache.log4j.Logger;
import org.jasper.core.constants.JasperConstants;

public class DataProcessorFactory {
	
	static Logger logger = Logger.getLogger(DataProcessorFactory.class.getName()); 
	
	public static DataProcessor createDataProcessor(){
		return createDataProcessor(JasperConstants.DEFAULT_PROCESSING_SCHEME);
	}
	
	public static DataProcessor createDataProcessor(String scheme){
		switch (scheme.toLowerCase()) {
		case JasperConstants.AGGREGATE_SCHEME :
    		return new AggregateDataProcessor();
		case JasperConstants.COALESCE_SCHEME :
			return new CoalesceDataProcessor();
		default :
			logger.error("Unknown processing scheme: " + scheme + " returning aggregate scheme ");
			return new AggregateDataProcessor();
    	}
		
	}

}
