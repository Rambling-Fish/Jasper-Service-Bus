package org.jasper.jLib.jCommons.admin;

import java.io.Serializable;

public class JasperAdminMessage implements Serializable{

	/*
		 The Jasper Admin Message is used to allow a JTA connected to the JSB core
		 to publish it's ontology so it can be added to the core's ontology model.
		 It is also used to tell the core to remove a JTA from the model whenever
		 it disconnects from the core.
		 
		 When a JTA connects to the core via the JMS broker, the broker sends a 
		 JAM message to the delegate to inform it of the JTA connection. The
		 delegate then sends a JAM to the JTA to tell it to publish it's
		 ontology.  The JTA will populate a JAM message with its ontology triples
		 and send it to the delegate.
		  
		 The following details how the message should be used and how it should
		 be populated.
	
		 JTA Connect - JTA connects to the broker. The broker creates a JAM msg
		 and sends it to the global delegate queue. The message is set as:
		 Type:    ontologyManagement
		 Command: jta_connect
		 details: index 0 set to full jta id (in the format <vendor>:<appname>:<version>:<deploymentId>)
		 async request - does not expect a response
		
		 JTA Disconnect - JTA disconnects from broker. Broker creates JAM msg
		 and sends it to global delegate queue. The message is set as:
		 Type:    ontologyManagement
		 Command: jta_disconnect
		 details: index 0 set to full jta id (in the format <vendor>:<appname>:<version>:<deploymentId>)
		 async request - does not expect a response
		
		 Request for Ontology - When delegate receives a jta_connect msg
		 it creates a JAM msg and sends it to the JTA to request the
		 JTA to publish its ontology triples. The JAM msg is set as:
		 Type:    ontologyManagement
		 Command: get_ontology
		 details: null
		 sync request - expect a response, specifically an Array of Triples, each triple is a String array, i.e String[][] will be returned
		 
		 Summary Table
		    _____________________________________________________________________________________________________________
			|Command		| Details																	| Response		|
			|---------------|---------------------------------------------------------------------------|---------------|
			|get_ontology	| null																		| String[][]	|
			|jta_connect	| index[0] = jta ID in format <vendor>:<appname>:<version>:<deploymentId>	| no-response	|
			|jta_disconnect	| index[0] = jta ID in format <vendor>:<appname>:<version>:<deploymentId>	| no-response	|
			|---------------|---------------------------------------------------------------------------|---------------|
		
		 
	 */

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
	private Serializable[] details;

	public JasperAdminMessage(Type type, Command command, Serializable... details) {
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

	public Serializable[] getDetails() {
		return details;
	}
	
}
