package org.jasper.core.notification.triggers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import org.apache.log4j.Logger;
import org.jasper.core.notification.util.JsonResponseParser;

public class CompareInt extends Trigger implements Serializable{
	private static final long serialVersionUID = -4016629738650948052L;
	private String left;
	private String right;
	private String operand;
	private float x;
	private float y;
	static Logger logger = Logger.getLogger(CompareInt.class.getName());
	
	public CompareInt(int expiry, int polling, String left, String operand, String right) {
		super(expiry, polling);
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
