package org.jasper.core.notification.triggers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jasper.core.notification.util.JsonResponseParser;

import com.google.gson.JsonElement;

public class CompareInt implements Trigger, Serializable{

	private static final long serialVersionUID = -4016629738650948052L;
	private String left;
	private String right;
	private String operand;
	private float x;
	private float y;
	static Logger logger = Logger.getLogger(CompareInt.class.getName());
	
	public CompareInt(String left, String operand, String right) {
		this.left    = left;
		this.right   = right;
		this.operand = operand;

		try{
			if(!left.startsWith("http")){
				this.x = Float.parseFloat(left);
			}
			if(!right.startsWith("http")){
				this.y = Float.parseFloat(right);
			}
		} catch(NumberFormatException e){
			logger.error("Exception processing trigger parameters: " + e);
		}
		
	}
	
	public boolean evaluate(JsonElement response){
		JsonResponseParser respParser = new JsonResponseParser();
		
		if(response.isJsonNull()) return false;
		List<Float> list = new ArrayList<Float>();
		 
		list = respParser.parse(response, left);
		 
		if(list == null || list.isEmpty()) return false;
		
		return compare(list);
		
	}
	
	private boolean compare(List<Float> list){
		boolean result = false;
		for(int i=0;i<list.size();i++){
			this.x = list.get(i);
			switch (operand.toLowerCase()) {
			case "eq" :
				if(x == y){
					result = true;
					i = list.size();
				}
				break;
			case "lt" :
				if(x < y){
					result = true;
					i = list.size();
				}
				break;
			case "gt" :
				if(x > y){
					result = true;
					i = list.size();
				}
				break;
			case "ne" :
				if(x != y){
					result = true;
					i = list.size();
				}
				break;
			case "ge" :
				if(x >= y){
					result = true;
					i = list.size();
				}
				break;
			case "le" :
				if(x <= y){
					result = true;
					i = list.size();
				}
				break;
			default :
				logger.warn("Invalid operand received: " + operand);
				result = false;
				break;
			
			} // end of switch
		} // end of if
		
	return result;
	}
	
}
