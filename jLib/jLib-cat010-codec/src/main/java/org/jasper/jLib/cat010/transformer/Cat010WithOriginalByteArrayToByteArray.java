package org.jasper.jLib.cat010.transformer;

import org.jasper.jLib.cat010.codec.Cat010MessageWithOriginalByteArray;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;

public class Cat010WithOriginalByteArrayToByteArray extends AbstractTransformer {

	@Override
	protected Object doTransform(Object src, String encoding)throws TransformerException {
		if(src instanceof Cat010MessageWithOriginalByteArray){
			try {
				return ((Cat010MessageWithOriginalByteArray)src).getOringalByteArray();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return "Error getting cat010 message";
			}
		}
		return src;
	}

}
