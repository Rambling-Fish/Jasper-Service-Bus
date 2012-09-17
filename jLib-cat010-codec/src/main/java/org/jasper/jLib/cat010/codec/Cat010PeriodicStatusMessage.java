package org.jasper.jLib.cat010.codec;

import java.io.Serializable;

public class Cat010PeriodicStatusMessage extends AbstractCat010Message implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -4142374752499368140L;
	private Cat010SystemStatus systemStatus;

	public Cat010PeriodicStatusMessage(){
		this.msgType = Cat010MessageType.periodic_status_message;
	}
	
	public void setSystemStatus(Cat010SystemStatus systemStatus) {
		this.systemStatus = systemStatus;
	}

	public Cat010SystemStatus getSystemStatus() {
		return systemStatus;
	}

	public String toString(){
		StringBuilder sb = new StringBuilder();

		sb.append("-----------------------------------------------------------------------------------------------------------------------------------------------\n");
	
		if(dataSourceIdSac != null && dataSourceIdSic != null) sb.append("Data Source ID  = " + dataSourceIdSac + "," + dataSourceIdSic + "\n");
		if(msgType != null)                                    sb.append("  Message Type  = " + msgType + "\n");
		if(timeOfDay != null){
			int hrs = ((timeOfDay.intValue()) / 60) / 60;
			int min = ((timeOfDay.intValue()) / 60) % 60;
			double sec = timeOfDay - (3600*hrs + 60*min);
			String timeOfDayFormated = hrs + ":" + min + ":" + sec + " UTC";
			sb.append("   Time of Day  = " + timeOfDayFormated + "\n");
		}
		
		if(systemStatus !=null)       sb.append("  System Status = " + systemStatus + "\n");
		sb.append("-----------------------------------------------------------------------------------------------------------------------------------------------\n");
		return sb.toString();	
	}	
}
