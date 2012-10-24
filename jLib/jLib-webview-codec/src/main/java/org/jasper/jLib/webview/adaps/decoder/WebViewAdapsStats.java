package org.jasper.jLib.webview.adaps.decoder;

import java.io.Serializable;

public class WebViewAdapsStats implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 557301868153390136L;
	
	private String fir;
	private String last_update_time;
	private String gusting;
	private String windspeed;
	private String adaps_altimeter;
	private String wind_direction;

	public WebViewAdapsStats(String fir, String last_update_time,
			String gusting, String windspeed, String adaps_altimeter,
			String wind_direction) {
		this.fir = fir;
		this.last_update_time = last_update_time;
		this.gusting = gusting;
		this.windspeed = windspeed;
		this.adaps_altimeter = adaps_altimeter;
		this.wind_direction = wind_direction;
	}

	public String getFir() {
		return fir;
	}

	public void setFir(String fir) {
		this.fir = fir;
	}

	public String getLast_update_time() {
		return last_update_time;
	}

	public void setLast_update_time(String last_update_time) {
		this.last_update_time = last_update_time;
	}

	public String getGusting() {
		return gusting;
	}

	public void setGusting(String gusting) {
		this.gusting = gusting;
	}

	public String getWindspeed() {
		return windspeed;
	}

	public void setWindspeed(String windspeed) {
		this.windspeed = windspeed;
	}

	public String getAdaps_altimeter() {
		return adaps_altimeter;
	}

	public void setAdaps_altimeter(String adaps_altimeter) {
		this.adaps_altimeter = adaps_altimeter;
	}

	public String getWind_direction() {
		return wind_direction;
	}

	public void setWind_direction(String wind_direction) {
		this.wind_direction = wind_direction;
	}
}
