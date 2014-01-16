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
	private String dtaParms;
	private String UDEInstance;
	private String output;
	private String version;
	private String contentType;
	
	public PersistedObject(String key, String correlationID, String request, String ruri, String dtaParms,
			Destination replyTo, boolean isNotificationRequest, String UDEInstance, String output,
			String version, String contentType) {
		this.key = key;
		this.correlationID = correlationID;
		this.request = request;
		this.ruri = ruri;
		this.dtaParms = dtaParms;
		this.replyTo = replyTo;
		this.isNotificationRequest = isNotificationRequest;
		this.UDEInstance = UDEInstance;
		this.output = output;
		this.version = version;
		this.contentType = contentType;
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
	
	public String getDtaParms(){
		return dtaParms;
	}
	
	public void setDtaParms(String dtaParms){
		this.dtaParms = dtaParms;
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
	
	public String getVersion(){
		return version;
	}
	
	public void setVersion(String version){
		this.version = version;
	}
	
	public String getContentType(){
		return contentType;
	}
	
	public void setContentType(String contentType){
		this.contentType = contentType;
	}
}
