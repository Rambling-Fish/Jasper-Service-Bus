package org.jasper.core.notification.triggers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import org.apache.log4j.Logger;
import org.jasper.core.notification.util.JsonResponseParser;

public class Range extends Trigger implements Serializable{
	private static final long serialVersionUID = -4016645138650948052L;
	private String left;
	private String minString;
	private String maxString;
	private float min;
	private float max;
	private float x;
	static Logger logger = Logger.getLogger(Range.class.getName());

	
	public Range(int expiry, int polling, String left, String min, String max) {
		super(expiry, polling);
		this.left    = left;
		this.minString   = min;
		this.maxString = max;

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
	
	@Override
	public boolean evaluate(JsonArray ruriArray){
		JsonResponseParser respParser = new JsonResponseParser();
		
		if(ruriArray.size() == 0) return false;
		
		List<Integer> list = new ArrayList<Integer>();
		list = respParser.parse(ruriArray, left);
		 
		if(list.isEmpty()) return false;
		
		return compare(list);
		
	}
	
	private boolean compare(List<Integer> list){
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
