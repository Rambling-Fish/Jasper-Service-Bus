package org.jasper.jLib.idar.video.codec;

import java.io.Serializable;

public class IDARVideoMessage implements Serializable{

	private static final long serialVersionUID = -2872731535936179287L;
	protected byte[] IDARjpeg;
	protected String intelliDARinfo;
	
	public byte[] getIDARjpeg(){
		return IDARjpeg;
	}
	
	public void setIDARJpeg(byte[] IDARjpeg){
		this.IDARjpeg = IDARjpeg;
	}
	
	public String getIntelliDARinfo(){
		return intelliDARinfo;
	}
	
	public void setIntelliDARinfo(String info){
		this.intelliDARinfo = info;
	}
}
