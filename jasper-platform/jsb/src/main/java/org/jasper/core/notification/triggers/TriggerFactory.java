package org.jasper.core.notification.triggers;


public class TriggerFactory {
	Trigger trigger = null;

	public TriggerFactory() {
	
	}
	
	public Trigger createTrigger(String type, String...params){
		switch (type.toLowerCase()){
		case "count" :
			if(params.length != 3){
				return null;
			}
			else{
				trigger = new Count(params[0], params[1], params[2]);
				break;
			}
		case "range" :
			if(params.length != 3){
				return null;
			}
			else{
				trigger = new Range(params[0], params[1], params[2]);
				break;
			}
		default : return null;
		}
		
		return trigger;
	}

}
