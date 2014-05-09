package org.jasper.core.notification.triggers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jasper.core.notification.util.JsonResponseParser;

import com.google.gson.JsonElement;

public class Range implements Trigger, Serializable{
	private static final long serialVersionUID = -4016645138650948052L;
	private String left;
	private float min;
	private float max;
	private float x;
	static Logger logger = Logger.getLogger(Range.class.getName());

	
	public Range(String left, String min, String max) {
		this.left    = left;

		try{
			if(!left.startsWith("http")){
				this.x = Float.parseFloat(left);
			}
			this.min = Float.parseFloat(min);
			this.max = Float.parseFloat(max);
		} catch(NumberFormatException e){
			logger.error("Exception processing trigger parameters: " + e);
		}
		
	}
	
	public boolean evaluate(JsonElement response){
		JsonResponseParser respParser = new JsonResponseParser();
		
		if(response == null || response.isJsonNull()) return false;
		List<Float> list = new ArrayList<Float>();
		 
		list = respParser.parse(response, left);
		 
		if(list == null || list.isEmpty()) return false;
		
		return compare(list);
		
	}
	
	private boolean compare(List<Float> list){
		boolean result = false;
		for(int i=0;i<list.size();i++){
			this.x = list.get(i);
			if(x >= min && x <= max){
				result = true;
				i = list.size();
			}
		}
		
	return result;
	}
	
}
