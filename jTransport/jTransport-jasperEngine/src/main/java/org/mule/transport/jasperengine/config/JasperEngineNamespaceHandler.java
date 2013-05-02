/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jasperengine.config;

import org.mule.config.spring.factories.InboundEndpointFactoryBean;
import org.mule.config.spring.factories.OutboundEndpointFactoryBean;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.processors.CheckExclusiveAttributes;
import org.mule.config.spring.parsers.specific.FilterDefinitionParser;
import org.mule.config.spring.parsers.specific.MessageProcessorDefinitionParser;
import org.mule.config.spring.parsers.specific.TransactionDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.TransportEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.TransportGlobalEndpointDefinitionParser;
import org.mule.transport.jasperengine.JasperEngineConnector;
import org.mule.transport.jasperengine.JasperInboundEndpointFactoryBean;
import org.mule.transport.jasperengine.transformers.JasperEngineMessageToObject;
import org.mule.transport.jasperengine.transformers.ObjectToJasperEngineMessage;
import org.mule.transport.jms.JmsClientAcknowledgeTransactionFactory;
import org.mule.transport.jms.JmsTransactionFactory;
import org.mule.transport.jms.config.JmsConnectorDefinitionParser;
import org.mule.transport.jms.config.JmsNamespaceHandler;
import org.mule.transport.jms.filters.JmsPropertyFilter;
import org.mule.transport.jms.filters.JmsSelectorFilter;
import org.mule.transport.jms.jndi.SimpleJndiNameResolver;

/**
 * Registers a Bean Definition Parser for handling <code><jasperengine:connector></code> elements
 * and supporting endpoint elements.
 */
public class JasperEngineNamespaceHandler extends JmsNamespaceHandler
{
    public void init(){
        registerJmsTransportEndpoints();

        registerMuleBeanDefinitionParser("connector", new JmsConnectorDefinitionParser()).addAlias(
            NUMBER_OF_CONSUMERS_ATTRIBUTE, NUMBER_OF_CONSUMERS_PROPERTY).registerPreProcessor(
            new CheckExclusiveAttributes(new String[][]{
                new String[]{NUMBER_OF_CONCURRENT_TRANSACTED_RECEIVERS_ATTRIBUTE},
                new String[]{NUMBER_OF_CONSUMERS_ATTRIBUTE}}));

        registerBeanDefinitionParser("connector", new JmsConnectorDefinitionParser(JasperEngineConnector.class));


        registerBeanDefinitionParser("transaction", new TransactionDefinitionParser(JmsTransactionFactory.class));
        registerBeanDefinitionParser("client-ack-transaction", new TransactionDefinitionParser(JmsClientAcknowledgeTransactionFactory.class));
        
        registerBeanDefinitionParser("jasperenginemessage-to-object-transformer", new MessageProcessorDefinitionParser(JasperEngineMessageToObject.class));

        registerBeanDefinitionParser("object-to-jasperenginemessage-transformer", new MessageProcessorDefinitionParser(ObjectToJasperEngineMessage.class));
        registerBeanDefinitionParser("property-filter", new FilterDefinitionParser(JmsPropertyFilter.class));
        registerBeanDefinitionParser("selector", new FilterDefinitionParser(JmsSelectorFilter.class));
        registerBeanDefinitionParser("default-jndi-name-resolver", new ChildDefinitionParser("jndiNameResolver", SimpleJndiNameResolver.class));
        registerBeanDefinitionParser("custom-jndi-name-resolver", new ChildDefinitionParser("jndiNameResolver"));
    }

    protected void registerJmsTransportEndpoints(){
        registerJmsEndpointDefinitionParser("endpoint", new TransportGlobalEndpointDefinitionParser(JasperEngineConnector.JASPERENGINE, TransportGlobalEndpointDefinitionParser.PROTOCOL, TransportGlobalEndpointDefinitionParser.RESTRICTED_ENDPOINT_ATTRIBUTES, JMS_ATTRIBUTES, new String[][]{}));
        registerJmsEndpointDefinitionParser("inbound-endpoint", new TransportEndpointDefinitionParser(JasperEngineConnector.JASPERENGINE, TransportEndpointDefinitionParser.PROTOCOL, JasperInboundEndpointFactoryBean.class, TransportEndpointDefinitionParser.RESTRICTED_ENDPOINT_ATTRIBUTES, JMS_ATTRIBUTES, new String[][]{}));
        registerJmsEndpointDefinitionParser("outbound-endpoint", new TransportEndpointDefinitionParser(JasperEngineConnector.JASPERENGINE, TransportEndpointDefinitionParser.PROTOCOL, OutboundEndpointFactoryBean.class, TransportEndpointDefinitionParser.RESTRICTED_ENDPOINT_ATTRIBUTES, JMS_ATTRIBUTES, new String[][]{}));
    }
}
