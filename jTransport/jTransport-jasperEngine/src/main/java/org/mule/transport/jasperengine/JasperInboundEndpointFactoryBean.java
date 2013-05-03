package org.mule.transport.jasperengine;

import org.mule.api.config.ConfigurationException;
import org.mule.api.endpoint.EndpointFactory;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.config.i18n.MessageFactory;
import org.mule.config.spring.factories.InboundEndpointFactoryBean;
import org.mule.endpoint.AbstractEndpoint;

public class JasperInboundEndpointFactoryBean extends InboundEndpointFactoryBean {

	
    @Override
    public Object doGetObject() throws Exception
    {
        EndpointFactory ef = muleContext.getEndpointFactory();
        if (ef != null)
        {
            InboundEndpoint inboundEndpoint = ef.getInboundEndpoint(this);
            if (inboundEndpoint instanceof AbstractEndpoint)
            {
                AbstractEndpoint.class.cast(inboundEndpoint).setAnnotations(getAnnotations());
            }
            
            Object prop = inboundEndpoint.getProperty("uri");
            if(prop !=null && prop instanceof String && connector instanceof JasperEngineConnector) {
            	((JasperEngineConnector)connector).registerInboundEndpointUri(inboundEndpoint.getEndpointURI().getAddress(), (String) inboundEndpoint.getProperty("uri"));
            }
            
            return inboundEndpoint;
        }
        else
        {
            throw new ConfigurationException(MessageFactory.createStaticMessage("EndpointFactory not found in Registry"));
        }
    }
	
}
