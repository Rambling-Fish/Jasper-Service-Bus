package org.jasper.jLib.jCommons.admin;

import java.io.Serializable;

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
	
	// JTA Connect - JTA connects to the broker. The broker creates a JAM msg
	// and sends it to the global delegate queue. The message is set as:
	// Type:    ontologyManagement
	// Command: jta_connect
	// jtaName: set to jtaName (in the format <appname>.<version>) for example: jtaDemo-EMR-C.2.0
	// details: null
	
	// JTA Disconnect - JTA disconnects from broker. Broker creates JAM msg
	// and sends it to global delegate queue. The message is set as:
	// Type:    ontologyManagement
	// Command: jta_disconnect
	// jtaName: set to jtaName (in the format <appname>.<version> for example: jtaDemo-EMR-C.2.0
	// details: null
	
	// Request for Ontology - When delegate receives a jta_connect msg
	// it creates a JAM msg and sends it to the JTA to request the
	// JTA to publish its ontology triples. The JAM msg is set as:
	// Type:    ontologyManagement
	// Command: get_ontology
	// jtaName: set to jtaName (in the format <appname>.<version> for example: jtaDemo-EMR-C.2.0
	// details: null
	
	// When the JTA receives the JAM get_ontology message it parses its turtle
	// file and replies with a JAM message. The JAM msg is set as:
	// Type:    ontologyManagement
	// Command: get_ontology
	// jtaName: set to jtaName (in the format <appname>.<version> for example: jtaDemo-EMR-C.2.0
	// details: String[][] with each array containing a triple in the format
	// subject, predicate, object
	

	private static final long serialVersionUID = -4469320192877833388L;

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
	private String[][] details;

	public JasperAdminMessage(Type type, Command command, String[][] details) {
		this.type = type;
		this.command = command;
		this.details = details;
	}

	public Type getType() {
		return type;
	}

	public Command getCommand() {
		return command;
	}

	public String[][] getDetails() {
		return details;
	}
	
}
