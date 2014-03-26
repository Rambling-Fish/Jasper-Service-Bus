package org.jasper.core.notification.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

public class JsonResponseParser{

	
	public List<Float> parse(JsonElement response, String ruri){
		List<Float> list = new ArrayList<Float>();
	
		if(response.isJsonArray()){
			JsonArray jsonArray = response.getAsJsonArray();
			if(jsonArray.size() == 0) return null;
		
			for(JsonElement item:jsonArray){
				if(item.isJsonObject()){
					JsonElement jelem = item.getAsJsonObject().get(ruri).getAsJsonPrimitive();
					if(jelem != null){
						try{
							list.add(Float.parseFloat(jelem.getAsString()));
						}catch(NumberFormatException ex){
							// Do nothing as we do not add invalid data
						}
					}
				}
				else if(item.isJsonArray()){
				
				}
			}
		}
		else if(response.isJsonObject()){
			JsonElement jelem = response.getAsJsonObject().get(ruri);
			if(jelem != null){
				list.add(Float.parseFloat(jelem.getAsString()));
			}
		}
		else if(response.isJsonPrimitive()){
			//TODO how to handle this case?
		}
			
		return list;
	}
	
}
