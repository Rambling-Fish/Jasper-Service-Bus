package org.jasper.jLib.idar.video.transformer;

import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;
import org.jasper.jLib.idar.video.codec.IDARVideoMessage;

public class IDARtoXML extends AbstractTransformer {

	@Override
	protected Object doTransform(Object src, String enc) throws TransformerException {
		if (src instanceof IDARVideoMessage){
			IDARVideoMessage ivm = (IDARVideoMessage) src;
			return (String) ivm.getIntelliDARinfo();
			
		}
		
		return src;
	}

}
