package org.jasper.core.persistence;

import java.io.Serializable;
import java.util.List;

import javax.jms.Destination;

import org.jasper.core.notification.triggers.Trigger;


public class PersistedObject implements Serializable{
	
	private static final long serialVersionUID = 4733352197348993633L;
	private Destination replyTo;
	private String correlationID;
	private String messageID;
	private String request;
	private String ruri;
	private List<Trigger> triggers;
	
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
	
	public boolean hasTriggers(){
		return triggers.isEmpty();
	}
	
	public String getCorrelationID(){
		return correlationID;
	}
	
	public void setCorrelationID(String correlationID){
		this.correlationID = correlationID;
	}
	
	public String getMessageID(){
		return messageID;
	}
	
	public void setMessageID(String messageID){
		this.messageID = messageID;
	}
	
	public String getRequest(){
		return request;
	}
	
	public void setRequest(String request){
		this.request = request;
	}
	
	public String getRURI(){
		return ruri;
	}
	
	public void setRURI(String ruri){
		this.ruri = ruri;
	}
	
}
