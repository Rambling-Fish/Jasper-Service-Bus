package org.jasper.jLib.idar.video.transformer;

import org.jasper.jLib.idar.video.codec.IDARVideoMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;

public class IDARtoJPEG extends AbstractTransformer {

	@Override
	protected Object doTransform(Object src, String enc) throws TransformerException {
		if (src instanceof IDARVideoMessage){
			IDARVideoMessage ivm = (IDARVideoMessage) src;
			return (byte[]) ivm.getIDARjpeg();
			
		}
		return null;
	}

}
