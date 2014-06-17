package org.jasper.core.dataprocessor;

import org.jasper.core.exceptions.JasperRequestException;

import com.google.gson.JsonElement;

public interface DataProcessor{
	
	public void add(JsonElement jsonElement);
	
	public JsonElement process() throws JasperRequestException;

    
}
