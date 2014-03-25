package org.jasper.core.dataprocessor;

import com.google.gson.JsonElement;

public interface DataProcessor{
	
	public void add(JsonElement jsonElement);
	
	public JsonElement process();

    
}
