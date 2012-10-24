package org.jasper.jLib.cat010.transformer;

import org.jasper.jLib.cat010.codec.Cat010Decoder;
import org.jasper.jLib.cat010.codec.Cat010MessageWithOriginalByteArray;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;

public class ByteArrayToCat010MessageWithOriginalByteArray extends AbstractTransformer {

	@Override
	protected Object doTransform(Object src, String encoding)throws TransformerException {
		if(src instanceof byte[]){
			byte[] byteArray = (byte[])src;
			try {
				return new Cat010MessageWithOriginalByteArray(Cat010Decoder.doDecode(byteArray),byteArray);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return "Error deocding Message";
			}
		}
		return src;
	}

}
