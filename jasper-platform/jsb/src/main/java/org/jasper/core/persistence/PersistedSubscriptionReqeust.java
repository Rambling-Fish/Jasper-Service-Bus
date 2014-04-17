package org.jasper.core.persistence;

import java.io.Serializable;
import java.util.List;

import javax.jms.Destination;

import org.jasper.core.notification.triggers.Trigger;

public class PersistedSubscriptionReqeust implements Serializable {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 544973952304909155L;
	private String	ruri;
	private String	subscriptionId;
	private String	correlationID;
	private Destination	reply2q;
	private List<Trigger>	triggerList;
	private int	expiry;
	private long	timestampMillis;

	public PersistedSubscriptionReqeust(String ruri, String subscriptionId, String correlationID, Destination reply2q, List<Trigger> triggerList, int expiry, long timestampMillis) {
		this.ruri = ruri;
		this.subscriptionId = subscriptionId;
		this.correlationID = correlationID;
		this.reply2q = reply2q;
		this.triggerList = triggerList;
		this.expiry = expiry;
		this.timestampMillis = timestampMillis;
	}

	public String getRuri() {
		return ruri;
	}

	public void setRuri(String ruri) {
		this.ruri = ruri;
	}

	public String getSubscriptionId() {
		return subscriptionId;
	}

	public void setSubscriptionId(String subscriptionId) {
		this.subscriptionId = subscriptionId;
	}

	public String getCorrelationID() {
		return correlationID;
	}

	public void setCorrelationID(String correlationID) {
		this.correlationID = correlationID;
	}

	public Destination getReply2q() {
		return reply2q;
	}

	public void setReply2q(Destination reply2q) {
		this.reply2q = reply2q;
	}

	public List<Trigger> getTriggerList() {
		return triggerList;
	}

	public void setTriggerList(List<Trigger> triggerList) {
		this.triggerList = triggerList;
	}

	public int getExpiry() {
		return expiry;
	}

	public void setExpiry(int expiry) {
		this.expiry = expiry;
	}

	public long getTimestampMillis() {
		return timestampMillis;
	}

	public void setTimestampMillis(long timestampMillis) {
		this.timestampMillis = timestampMillis;
	}
	
	
}
