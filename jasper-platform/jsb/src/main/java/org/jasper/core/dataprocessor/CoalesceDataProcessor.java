package org.jasper.core.dataprocessor;

import org.apache.log4j.Logger;
import org.jasper.core.constants.JasperConstants;
import org.jasper.core.exceptions.JasperRequestException;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

public class CoalesceDataProcessor implements DataProcessor{

	static Logger logger = Logger.getLogger(CoalesceDataProcessor.class.getName()); 
	private JsonArray cache;
	
	public CoalesceDataProcessor(){
		 cache = new JsonArray();
	}

    public void add(JsonElement jsonElement){
    	if(logger.isInfoEnabled()){
    		logger.info("adding element : " + jsonElement);
    	}
    	//TODO add check that no duplicates are added
    	if(jsonElement.isJsonArray()){
    		cache.addAll(jsonElement.getAsJsonArray());
    	}else{
    		cache.add(jsonElement);
    	}
	}
    
    public JsonElement process() throws JasperRequestException{
		return coalesceOutput();
    }
    
    private JsonElement coalesceOutput() throws JasperRequestException{ 	
    	if(isAllSame()){
    		return cache.get(0); 
    	}
		throw new JasperRequestException(JasperConstants.ResponseCodes.BADREQUEST, "Response data is not identical for coalesce processing scheme");
    }
    
    private boolean isAllSame(){
		JsonElement first = cache.get(0);
    	for(int i = 1; i < cache.size(); i++){
    		JsonElement second = cache.get(i);
    		if( ! first.equals(second) ) return false;
    	}
    	return true;
    }

}
