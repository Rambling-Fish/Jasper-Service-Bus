package org.jasper.jLib.idar.video.transformer;

import org.jasper.jLib.idar.video.codec.IDARVideoDecoder;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;

public class ByteArrayToIDARVideoMessage extends AbstractTransformer {

	@Override
	protected Object doTransform(Object src, String encoding)throws TransformerException {
		if(src instanceof byte[]){
			byte[] byteArray = (byte[])src;
			try {
				return new IDARVideoDecoder().doDecode(byteArray);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return "Error deocoding Message";
			}
		}
		return src;
		
	}

}
