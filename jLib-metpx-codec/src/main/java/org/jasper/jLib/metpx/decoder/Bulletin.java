package org.jasper.jLib.metpx.decoder;

import java.io.Serializable;

public class Bulletin implements Serializable{

	private static final long serialVersionUID = -8541579054767704061L;	
	private String header;
	private String destination;
	private String content;
	private String from;
	private String country;
	private String scope;
	private String origin;
	private String time;
	
	
	public String getHeader(){
		return header;
	}
	
	public void setHeader(String header){
		this.header = header;
	}
	
	public String getFrom(){
		return from;
	}
	
	public void setFrom(String from){
		this.from = from;
	}
	
	public String getCountry(){
		return country;
	}
	
	public void setCountry(String country){
		this.country = country;
	}
	
	public String getScope(){
		return scope;
	}
	
	public void setScope(String scope){
		this.scope = scope;
	}
	
	public String getOrigin(){
		return origin;
	}
	
	public void setOrigin(String origin){
		this.origin = origin;
	}
	
	public String getTime(){
		return time;
	}
	
	public void setTime(String time){
		this.time = time;
	}
	
	public String getDestination() {
		return destination;
	}
	
	public void setDestination(String destination){
		this.destination = destination;
	}
	
	public String getContent(){
		return content;
	}
	
	public void setContent(String content){
		this.content = content;
	}
}