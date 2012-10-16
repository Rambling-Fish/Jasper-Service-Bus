package org.jasper.jLib.webview.notam.transformer;

import org.jasper.jLib.webview.notam.decoder.WebViewNotamDecoder;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;

public class StringToWebViewNotam extends AbstractTransformer {

	@Override
	protected Object doTransform(Object src, String encoding) throws TransformerException {
		if(src instanceof String){
			return WebViewNotamDecoder.doDecode((String)src);
        }
		return src;
	}

}
