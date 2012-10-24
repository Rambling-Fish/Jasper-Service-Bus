package org.jasper.jLib.cat010.codec;

import java.io.Serializable;

public class Cat010MessageWithOriginalByteArray  implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 362031934841224960L;
	private Cat010Message msg;
	private byte[] oringalByteArray;
	
	public Cat010MessageWithOriginalByteArray(Cat010Message msg,byte[] oringalByteArray) {
		this.msg = msg;
		this.oringalByteArray = oringalByteArray;
	}

	public Cat010Message getMsg() {
		return msg;
	}

	public byte[] getOringalByteArray() {
		return oringalByteArray;
	}

}
