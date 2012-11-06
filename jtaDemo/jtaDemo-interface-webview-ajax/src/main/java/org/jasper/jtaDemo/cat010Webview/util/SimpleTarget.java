package org.jasper.jtaDemo.cat010Webview.util;

import java.io.Serializable;

public class SimpleTarget implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -6257455082143184069L;

	private String trackId;
	private int x;
	private int y;
	private int textX;
	private int textY;
	
	
	
	public SimpleTarget(String trackId, int x, int y, int textX, int textY) {
		super();
		this.trackId = trackId;
		this.x = x;
		this.y = y;
		this.textX = textX;
		this.textY = textY;
	}
	public String getTrackId() {
		return trackId;
	}
	public void setTrackId(String trackId) {
		this.trackId = trackId;
	}
	public int getX() {
		return x;
	}
	public void setX(int x) {
		this.x = x;
	}
	public int getY() {
		return y;
	}
	public void setY(int y) {
		this.y = y;
	}
	public int getTextX() {
		return textX;
	}
	public void setTextX(int textX) {
		this.textX = textX;
	}
	public int getTextY() {
		return textY;
	}
	public void setTextY(int textY) {
		this.textY = textY;
	}
	
}
