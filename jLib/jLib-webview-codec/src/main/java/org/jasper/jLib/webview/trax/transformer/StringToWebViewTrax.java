package org.jasper.jLib.webview.trax.transformer;

import org.jasper.jLib.webview.trax.decoder.WebViewTraxDecoder;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;

public class StringToWebViewTrax extends AbstractTransformer {

	@Override
	protected Object doTransform(Object src, String encoding) throws TransformerException {
		if(src instanceof String){
			return WebViewTraxDecoder.doDecode((String)src);
        }
		return src;
	}

}
