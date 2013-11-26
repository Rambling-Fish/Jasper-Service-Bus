package org.jasper.core.notification.triggers;

import java.io.Serializable;

import org.apache.jena.atlas.json.JsonArray;

public class Trigger implements Serializable{
		
	private static final long serialVersionUID = 1808933811512364772L;
	private int expiry;
	private int polling;
	private long notificationExpiry;

	public Trigger(int expiry, int polling) {
		this.expiry  = expiry;
		this.polling = polling;
	}
	
	public Trigger(){
		
	}
	
	public boolean evaluate(JsonArray response){
		return false;
	}
	
	public long getNotificationExpiry(){
		return notificationExpiry;
	}
	
	public void setNotificationExpiry(){
		notificationExpiry = (System.currentTimeMillis() + expiry);
	}
	
	public void setPolling(int polling){
		this.polling = polling;
	}
	
	public int getPolling(){
		return polling;
	}
	
	public void setExpiry(int expiry){
		this.expiry = expiry;
	}
	
	public int getExpiry(){
		return expiry;
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
	
}
