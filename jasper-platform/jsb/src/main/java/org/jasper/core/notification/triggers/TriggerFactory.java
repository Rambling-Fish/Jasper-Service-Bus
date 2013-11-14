package org.jasper.core.notification.triggers;


public class TriggerFactory {
	Trigger trigger = null;

	public TriggerFactory() {
	
	}
	
	public Trigger createTrigger(String type, int expiry, int polling, String...params){
		switch (type.toLowerCase()){
		case "compareint" :
			if(params.length != 3){
				return null;
			}
			else{
				trigger = new CompareInt(expiry, polling, params[0], params[1], params[2]);
				break;
			}
		case "range" :
			if(params.length != 3){
				return null;
			}
			else{
				trigger = new Range(expiry, polling, params[0], params[1], params[2]);
				break;
			}
		default : return null;
		}
		
		return trigger;
	}

}
