package org.mule.transport.webview.config;

import org.mule.config.spring.parsers.collection.ChildListEntryDefinitionParser;
import org.mule.config.spring.parsers.collection.ChildMapEntryDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.generic.ParentDefinitionParser;
import org.mule.config.spring.parsers.specific.FilterDefinitionParser;
import org.mule.config.spring.parsers.specific.MessageProcessorDefinitionParser;
import org.mule.config.spring.parsers.specific.SecurityFilterDefinitionParser;
import org.mule.endpoint.URIBuilder;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.components.StaticResourceMessageProcessor;
import org.mule.transport.http.config.HttpNamespaceHandler;
import org.mule.transport.http.filters.HttpBasicAuthenticationFilter;
import org.mule.transport.http.filters.HttpRequestWildcardFilter;
import org.mule.transport.http.transformers.HttpClientMethodResponseToObject;
import org.mule.transport.http.transformers.HttpRequestBodyToParamMap;
import org.mule.transport.http.transformers.HttpResponseToString;
import org.mule.transport.http.transformers.MuleMessageToHttpResponse;
import org.mule.transport.http.transformers.ObjectToHttpClientMethodRequest;
import org.mule.transport.webview.WebViewConnector;

public class WebViewNamespaceHandler extends HttpNamespaceHandler {

    public void init()
    {
        registerStandardTransportEndpoints(WebViewConnector.HTTP, URIBuilder.SOCKET_ATTRIBUTES)
            .addAlias("contentType", HttpConstants.HEADER_CONTENT_TYPE)
            .addAlias("method", WebViewConnector.HTTP_METHOD_PROPERTY);
        
        //Register our WebView Connector as the one to use
        registerConnectorDefinitionParser(WebViewConnector.class);
      
        //Register our WebView Connector as the polling-connector
        registerBeanDefinitionParser("polling-connector", new MuleOrphanDefinitionParser(WebViewConnector.class, true));

        registerBeanDefinitionParser("payloadParameterName", new ChildListEntryDefinitionParser("payloadParameterNames", ChildMapEntryDefinitionParser.VALUE));
        registerBeanDefinitionParser("requiredParameter", new ChildMapEntryDefinitionParser("requiredParams"));
        registerBeanDefinitionParser("optionalParameter", new ChildMapEntryDefinitionParser("optionalParams"));
        
        registerBeanDefinitionParser("http-response-to-object-transformer", new MessageProcessorDefinitionParser(HttpClientMethodResponseToObject.class));
        registerBeanDefinitionParser("http-response-to-string-transformer", new MessageProcessorDefinitionParser(HttpResponseToString.class));
        registerBeanDefinitionParser("object-to-http-request-transformer", new MessageProcessorDefinitionParser(ObjectToHttpClientMethodRequest.class));
        registerBeanDefinitionParser("message-to-http-response-transformer", new MessageProcessorDefinitionParser(MuleMessageToHttpResponse.class));
        registerBeanDefinitionParser("body-to-parameter-map-transformer", new MessageProcessorDefinitionParser(HttpRequestBodyToParamMap.class));

        registerBeanDefinitionParser("error-filter", new ParentDefinitionParser());
        registerBeanDefinitionParser("request-wildcard-filter", new FilterDefinitionParser(HttpRequestWildcardFilter.class));
        registerBeanDefinitionParser("basic-security-filter", new SecurityFilterDefinitionParser(HttpBasicAuthenticationFilter.class));

        registerMuleBeanDefinitionParser("static-resource-handler",
                new MessageProcessorDefinitionParser(StaticResourceMessageProcessor.class));
    }
	
}
