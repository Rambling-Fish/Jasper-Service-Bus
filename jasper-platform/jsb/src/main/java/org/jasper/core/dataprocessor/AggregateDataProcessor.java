package org.jasper.core.dataprocessor;

import org.apache.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

public class AggregateDataProcessor implements DataProcessor {

	static Logger logger = Logger.getLogger(AggregateDataProcessor.class.getName()); 
	private JsonArray cache;
	
	public AggregateDataProcessor(){
		 cache = new JsonArray();
	}

    public void add(JsonElement jsonElement){
    	if (jsonElement == null) {
            return;
        }

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
    
    public JsonElement process(){
    	return (cache.size()==0)?null:cache;
    }
	
}
