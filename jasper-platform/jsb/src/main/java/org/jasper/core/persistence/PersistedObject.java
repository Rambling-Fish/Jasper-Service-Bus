package org.jasper.core.persistence;

import java.io.Serializable;
import java.util.List;

import javax.jms.Destination;

import org.jasper.core.notification.triggers.Trigger;


public class PersistedObject implements Serializable{
	
	private static final long serialVersionUID = 4733352197348993633L;
	private Destination replyTo;
	private String correlationID;
	private String request;
	private String ruri;
	private String notification;
	private boolean isNotificationRequest;
	private List<Trigger> triggers;
	private String key;
	private String jtaParms;
	private String UDEInstance;
	private String output;
	
	public PersistedObject(String key, String correlationID, String request, String ruri, Destination replyTo,
			boolean isNotificationRequest, String UDEInstance, String output) {
		this.key = key;
		this.correlationID = correlationID;
		this.request = request;
		this.ruri = ruri;
		this.replyTo = replyTo;
		this.isNotificationRequest = isNotificationRequest;
		this.UDEInstance = UDEInstance;
		this.output = output;
	}
	
	public PersistedObject() {
		
	}
	
	public Destination getReplyTo(){
		return replyTo;
	}
	
	public void setReplyTo(Destination replyTo){
		this.replyTo = replyTo;
	}
	
	public List<Trigger> getTriggers(){
		return triggers;
	}
	
	public void setTriggers(List<Trigger> triggers){
		this.triggers = triggers;
	}
		
	public String getCorrelationID(){
		return correlationID;
	}
	
	public void setCorrelationID(String correlationID){
		this.correlationID = correlationID;
	}
	
	public String getRequest(){
		return request;
	}
	
	public void setRequest(String request){
		this.request = request;
	}
	
	public void setKey(String key){
		this.key = key;
	}
	
	public String getKey(){
		return key;
	}
	
	public String getRURI(){
		return ruri;
	}
	
	public void setRURI(String ruri){
		this.ruri = ruri;
	}
	
	public String getNotification(){
		return notification;
	}
	
	public void setNotification(String notification){
		this.notification = notification;
	}
	
	public void setIsNotificationRequest(boolean isNotificationRequest){
		this.isNotificationRequest = isNotificationRequest;
	}
	
	public String getJtaParms(){
		return jtaParms;
	}
	
	public void setJtaParms(String jtaParms){
		this.jtaParms = jtaParms;
	}
	
	public String getUDEInstance(){
		return UDEInstance;
	}
	
	public void setUDEInstance(String UDEInstance){
		this.UDEInstance = UDEInstance;
	}
	
	public String getOutput(){
		return output;
	}
	
	public void setOutput(String output){
		this.output = output;
	}
	
	public boolean isNotificationRequest(){
		return isNotificationRequest;
	}
}
