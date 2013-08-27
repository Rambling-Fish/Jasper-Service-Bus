package org.jasper.jLib.jCommons.admin;

import java.io.Serializable;
import java.util.Map;

public class JasperAdminMessage implements Serializable{

	private static final long serialVersionUID = -4465620192877833388L;

	public enum Type{
		jsbClusterManagement,
		jtaDataManagement
	}

	public enum Command{
		add,
		delete,
		notify,
		publish,
		update
	}

	private Type type;
	private Command command;
	private String src;
	private String dst;
	private String[] details;
	private Map<String, String[]> map;

	public JasperAdminMessage(Type type, Command command, String src, String dst, String... details) {
		this.type = type;
		this.command = command;
		this.src = src;
		this.dst = dst;
		this.details = details;
	}
	
	public JasperAdminMessage(Type type, Command command, Map<String, String[]> map){
		this.type = type;
		this.command = command;
		this.map = map;
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

	public String[] getDetails() {
		return details;
	}
	
	public Map<String, String[]> getMap() {
		return map;
	}

}
