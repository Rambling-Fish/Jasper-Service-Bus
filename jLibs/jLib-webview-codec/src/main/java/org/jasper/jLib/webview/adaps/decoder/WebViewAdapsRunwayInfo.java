package org.jasper.jLib.webview.adaps.decoder;

import java.io.Serializable;

public class WebViewAdapsRunwayInfo implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 5177093091024687480L;
	
	private String runway;
	private String rvr_a;
	private String rvr_b;
	private String edge_lights;
	private String loc;
	private String gp;
	private String dme;
	
	public WebViewAdapsRunwayInfo(String runway, String rvr_a, String rvr_b,
			String edge_lights, String loc, String gp, String dme) {
		super();
		this.runway = runway;
		this.rvr_a = rvr_a;
		this.rvr_b = rvr_b;
		this.edge_lights = edge_lights;
		this.loc = loc;
		this.gp = gp;
		this.dme = dme;
	}

	public String getRunway() {
		return runway;
	}

	public void setRunway(String runway) {
		this.runway = runway;
	}

	public String getRvr_a() {
		return rvr_a;
	}

	public void setRvr_a(String rvr_a) {
		this.rvr_a = rvr_a;
	}

	public String getRvr_b() {
		return rvr_b;
	}

	public void setRvr_b(String rvr_b) {
		this.rvr_b = rvr_b;
	}

	public String getEdge_lights() {
		return edge_lights;
	}

	public void setEdge_lights(String edge_lights) {
		this.edge_lights = edge_lights;
	}

	public String getLoc() {
		return loc;
	}

	public void setLoc(String loc) {
		this.loc = loc;
	}

	public String getGp() {
		return gp;
	}

	public void setGp(String gp) {
		this.gp = gp;
	}

	public String getDme() {
		return dme;
	}

	public void setDme(String dme) {
		this.dme = dme;
	}
	
}
