package org.jasper.jLib.metpx.transformer;

import org.jasper.jLib.metpx.decoder.BulletinDecoder;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;

public class ByteArrayToBulletin extends AbstractTransformer {

	@Override
	protected Object doTransform(Object src, String enc) throws TransformerException {
		if(src instanceof byte[]){
			byte[] byteArray = (byte[])src;
			try {
				return new BulletinDecoder().doDecode(byteArray);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return "Error deocoding Message";
			}
		}
		return src;
	}

}
