package org.jasper.core.notification.triggers;

import org.apache.jena.atlas.json.JsonArray;

public class Trigger {
		
	public int expiry;
	public int polling;
	public static long notificationExpiry;

	public Trigger(int expiry, int polling) {
		this.expiry  = expiry;
		this.polling = polling;
	}
	
	public Trigger(){
		
	}
	
	public boolean evaluate(JsonArray response){
		return false;
	}
	
	public void setNotificationExpiry(){
		notificationExpiry = (System.currentTimeMillis() + expiry);
	}
	
	/*
	 * Determines if the current notification has expired or not. If it has not
	 * expired and the time left to expiry is less than the polling period, 
	 * the polling period is set to time left to expiry to allow for one more
	 * attempt to get data
	 */
	public boolean isNotificationExpired(){
		long time = System.currentTimeMillis();
		long timeLeft = (notificationExpiry - time);
		if ((timeLeft < polling) && (timeLeft > 0)) polling = (int)timeLeft;
		return (notificationExpiry - (System.currentTimeMillis() + polling) <= 0);
	}


}
