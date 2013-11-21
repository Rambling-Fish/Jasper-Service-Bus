package org.jasper.core.notification.triggers;

import java.io.Serializable;

import org.apache.jena.atlas.json.JsonArray;

public class Trigger implements Serializable{
		
	private static final long serialVersionUID = 1808933811512364772L;
	private int expiry;
	private int polling;
	private static long notificationExpiry;
	private long nextRun;
	private long lastRun;

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
		nextRun = System.currentTimeMillis() + polling;
	}
	
	/*
	 * Determines if the current notification has expired or not. If it has not
	 * expired and the time left to expiry is less than the polling period, 
	 * the polling period is set to time left to expiry to allow for one more
	 * attempt to get data
	 */
	public boolean isNotificationExpired(){
		long now = System.currentTimeMillis();
		long timeLeft = (notificationExpiry - now);
		if ((timeLeft < polling) && (timeLeft > 0)) polling = (int)timeLeft;
		return (notificationExpiry - (System.currentTimeMillis() + polling) <= 0);
	}
	
	public boolean isTimeToRun(){
		long now = System.currentTimeMillis();
		System.out.println(nextRun - now);
		return ((nextRun - now) <=0);
	}
	
	public void updateRunTime(){
		lastRun = System.currentTimeMillis();
		nextRun = (lastRun + polling);
	}

}
