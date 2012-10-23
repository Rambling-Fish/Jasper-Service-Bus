package org.jasper.jLib.cat010.transformer;

import org.jasper.jLib.cat010.codec.Cat010Decoder;
import org.jasper.jLib.cat010.codec.Cat010MessageWithOriginalByteArray;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;

public class ByteArrayOrCat010WithOriginalByteArrayToCat010Message extends AbstractTransformer {

	@Override
	protected Object doTransform(Object src, String encoding)throws TransformerException {
		if(src instanceof Cat010MessageWithOriginalByteArray){
			try {
				return ((Cat010MessageWithOriginalByteArray)src).getMsg();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return "Error getting original byte array";
			}
		}else if(src instanceof byte[]){
			try {
				return Cat010Decoder.doDecode((byte[])src);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return "Error getting original byte array";
			}
		}
		return src;
	}

}
