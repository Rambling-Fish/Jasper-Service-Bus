package org.jasper.jLib.jCommons.admin;

import java.io.Serializable;
import java.util.Map;

public class JasperAdminMessage implements Serializable{

//	 The Jasper Admin Message is used to allow a JTA connected to the JSB core
//	 to publish it's ontology so it can be added to the core's ontology model.
//	 It is also used to tell the core to remove a JTA from the model whenever
//	 it disconnects from the core.
//	 
//	 When a JTA connects to the core via the JMS broker, the broker sends a 
//	 JAM message to the delegate to inform it of the JTA connection. The
//	 delegate then sends a JAM to the JTA to tell it to publish it's
//	 ontology.  The JTA will populate a JAM message with its ontology triples
//	 and send it to the delegate.
//	  
//	 The following details how the message should be used and how it should
//	 be populated.

	private static final long serialVersionUID = -4465620192877833388L;

	public enum Type{
		ontologyManagement
	}

	public enum Command{
		get_ontology,
		jta_connect,
		jta_disconnect
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
