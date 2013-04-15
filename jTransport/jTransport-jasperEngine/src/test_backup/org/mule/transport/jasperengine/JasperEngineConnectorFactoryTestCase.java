/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jasperengine;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.api.transport.Connector;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JasperEngineConnectorFactoryTestCase extends AbstractMuleContextTestCase
{
	private static String URI = "vm://localhost:61616";
	private InboundEndpoint inEndpoint;
	
    /* For general guidelines on writing transports see
       http://www.mulesoft.org/documentation/display/MULE3USER/Creating+Transports */

    @Test
    public void createFromFactory() throws Exception
    {
        inEndpoint = muleContext.getEndpointFactory().getInboundEndpoint(getEndpointURI());
        Connector connector = new JasperEngineConnector(muleContext);
        
//        assertNotNull(inEndpoint);
//        assertNotNull(inEndpoint.getConnector());
//        assertTrue(connector instanceof JasperEngineConnector);
//        assertEquals(getEndpointURI(), inEndpoint.getEndpointURI().getAddress());
    }

    public String getEndpointURI() {
        return URI;
        
    }
    
}