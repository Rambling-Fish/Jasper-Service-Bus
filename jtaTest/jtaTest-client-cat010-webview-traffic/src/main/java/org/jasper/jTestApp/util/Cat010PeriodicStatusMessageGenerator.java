package org.jasper.jTestApp.util;

import org.jasper.jLib.cat010.codec.Cat010PeriodicStatusMessage;
import org.jasper.jLib.cat010.codec.Cat010SystemStatus;

public class Cat010PeriodicStatusMessageGenerator {
	
	//static method to create Periodic Status Message
	public static Cat010PeriodicStatusMessage getPeriodicStatusMessage(){
		Cat010PeriodicStatusMessage msg = new Cat010PeriodicStatusMessage();
		msg.setDataSourceIdSac((byte) 0);
		msg.setDataSourceIdSic((byte) 1);
		
		
		long millisSinceGMTMidnight = System.currentTimeMillis() % (24L * 60*60*1000);
		msg.setTimeOfDay( ((double)millisSinceGMTMidnight) / 1000);
		
		Cat010SystemStatus systemStatus = new Cat010SystemStatus();
		systemStatus.setDiv(Cat010SystemStatus.DIV.normal_operation);
		systemStatus.setNogo(Cat010SystemStatus.NOGO.operational);
		systemStatus.setOvl(Cat010SystemStatus.OVL.no_overload);
		systemStatus.setTsv(Cat010SystemStatus.TSV.valid);
		systemStatus.setTtf(Cat010SystemStatus.TTF.test_target_operative);
		msg.setSystemStatus(systemStatus );
		
		return msg;
	}

	/*
	 * we ignore the string parameter, as our mule flow will
	 * look for a method with a String parameter, we simply 
	 * call the parameter-less version of this method
	 */
	public static Cat010PeriodicStatusMessage getPeriodicStatusMessage(String str){
		return getPeriodicStatusMessage();
	}
	
}
