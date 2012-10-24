package org.jasper.jLib.cat010.codec;

import java.io.Serializable;


public abstract class AbstractCat010Message implements Cat010Message, Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -2999900343205704643L;

	public enum Cat010MessageType{
		target_report,
		start_of_update_cycle,
		periodic_status_message,
		event_triggered_status_message;
	}
	
	/*
	 * Note: We are not using primitives because we need to keep track of
	 * values that are not set, i.e. they need to be null by default.
	 */
	protected Cat010MessageType msgType;
	protected Byte dataSourceIdSac;
	protected Byte dataSourceIdSic;
	protected Double timeOfDay;
	
	
	public Byte getDataSourceIdSac() {
		return dataSourceIdSac;
	}

	public void setDataSourceIdSac(Byte dataSourceIdSac) {
		this.dataSourceIdSac = dataSourceIdSac;
	}

	public Byte getDataSourceIdSic() {
		return dataSourceIdSic;
	}

	public void setDataSourceIdSic(Byte dataSourceIdSic) {
		this.dataSourceIdSic = dataSourceIdSic;
	}
	
	public Cat010MessageType getMessageType(){
		return msgType;
	}

	public void setTimeOfDay(Double timeOfDay) {
		this.timeOfDay = timeOfDay;
	}

	public Double getTimeOfDay() {
		return timeOfDay;
	}
}
