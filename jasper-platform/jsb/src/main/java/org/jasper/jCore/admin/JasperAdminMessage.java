package org.jasper.jCore.admin;

import java.io.Serializable;

public class JasperAdminMessage implements Serializable{

	private static final long serialVersionUID = -4465620192877833388L;

	public enum Type{
		jsbClusterManagement,
	}
	
	public enum Command{
		add,
		delete,
		update
//		getUpates
	}
	
	private Type type;
	private Command command;
	private String src;
	private String dst;
	private String details;
	
	public JasperAdminMessage(Type type, Command command, String src, String dst, String details) {
		this.type = type;
		this.command = command;
		this.src = src;
		this.dst = dst;
		this.details = details;
	}

	public Type getType() {
		return type;
	}

	public Command getCommand() {
		return command;
	}

	public String getSrc() {
		return src;
	}
	
	public String getDst() {
		return dst;
	}
	
	public String getDetails() {
		return details;
	}
	
}
