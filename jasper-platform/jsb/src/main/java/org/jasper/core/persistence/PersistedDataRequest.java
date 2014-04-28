package org.jasper.core.persistence;

import java.io.Serializable;

import javax.jms.Destination;

public class PersistedDataRequest implements Serializable{

	private static final long serialVersionUID = 2863184111497082255L;
	
	private String correlationID;
	private Destination reply2Q;
	private String request;
	private long timestampMillis;
	
	public PersistedDataRequest(String correlationID, Destination reply2Q, String request, long timestampMillis) {
		super();
		this.correlationID = correlationID;
		this.reply2Q = reply2Q;
		this.request = request;
		this.setTimestampMillis(timestampMillis);
	}
	public String getCorrelationID() {
		return correlationID;
	}
	public void setCorrelationID(String correlationID) {
		this.correlationID = correlationID;
	}
	public Destination getReply2Q() {
		return reply2Q;
	}
	public void setReply2Q(Destination reply2q) {
		reply2Q = reply2q;
	}
	public String getRequest() {
		return request;
	}
	public void setRequest(String request) {
		this.request = request;
	}
	public long getTimestampMillis() {
		return timestampMillis;
	}
	public void setTimestampMillis(long timestampMillis) {
		this.timestampMillis = timestampMillis;
	}
	
	public String toString(){
		return correlationID + " - " + reply2Q + " - " +  timestampMillis + " - " + request;
	}

	
}
