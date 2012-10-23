package org.jasper.jLib.cat010.codec;

import org.jasper.jLib.cat010.codec.AbstractCat010Message.Cat010MessageType;

public interface Cat010Message {

	public Byte getDataSourceIdSac();

	public void setDataSourceIdSac(Byte dataSourceIdSac);

	public Byte getDataSourceIdSic();
	
	public void setDataSourceIdSic(Byte dataSourceIdSic);
	
	public Cat010MessageType getMessageType();

	public void setTimeOfDay(Double timeOfDay);

	public Double getTimeOfDay();
	
}
