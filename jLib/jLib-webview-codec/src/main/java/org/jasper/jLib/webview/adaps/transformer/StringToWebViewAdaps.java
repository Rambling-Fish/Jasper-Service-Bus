package org.jasper.jLib.webview.adaps.transformer;

import org.jasper.jLib.webview.adaps.decoder.WebViewAdapsDecoder;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;

public class StringToWebViewAdaps extends AbstractTransformer {

	@Override
	protected Object doTransform(Object src, String encoding) throws TransformerException {
		if(src instanceof String){
			return WebViewAdapsDecoder.doDecode((String)src);
        }
		return src;
	}

}
