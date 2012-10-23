package org.jasper.jLib.cat010.codec;

import java.io.Serializable;

public class Cat010SubTrack  implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -5132834610586423011L;

	public enum Cat010SubTrackType{
		unknown,
		aircraft,
		vehicle,
		human
	}
	
	/*
	 * Note: We are not using primitives because we need to keep track of
	 * values that are not set, i.e. they need to be null by default.
	 */
	private Integer trackId;
	private Integer sensorId;
	private Cat010SubTrackType type;
	
	public Cat010SubTrack(){
		
	}
	
	public Cat010SubTrack(Integer trackId, Integer sensorId, Cat010SubTrackType type) {
		this.trackId = trackId;
		this.sensorId = sensorId;
		this.type = type;
	}
	
	public void setTrackId(Integer trackId) {
		this.trackId = trackId;
	}
	public Integer getTrackId() {
		return trackId;
	}
	public void setSensorId(Integer sensorId) {
		this.sensorId = sensorId;
	}
	public Integer getSensorId() {
		return sensorId;
	}
	public void setType(Cat010SubTrackType type) {
		this.type = type;
	}
	public Cat010SubTrackType getType() {
		return type;
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder();

		sb.append("[");
		if(trackId != null)  sb.append("trackId:" + trackId);
		if(sensorId != null) sb.append(", sensorId:" + sensorId);
		if(type != null)     sb.append(", type:"  + type);
		sb.append("]");
		
		return sb.toString();
	}
	
}
