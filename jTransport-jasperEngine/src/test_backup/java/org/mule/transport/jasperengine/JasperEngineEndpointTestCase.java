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

import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class JasperEngineEndpointTestCase extends AbstractMuleTestCase
{
    /* For general guidelines on writing transports see
       http://www.mulesoft.org/documentation/display/MULE3USER/Creating+Transports */

    @Test
    public void validEndpointURI() throws Exception
    {
        // TODO test creating and asserting Endpoint values eg

        /*
        EndpointURI url = new MuleEndpointURI("tcp://localhost:7856");
        assertEquals("tcp", url.getScheme());
        assertEquals("tcp://localhost:7856", url.getAddress());
        assertNull(url.getEndpointName());
        assertEquals(7856, url.getPort());
        assertEquals("localhost", url.getHost());
        assertEquals("tcp://localhost:7856", url.getAddress());
        assertEquals(0, url.getParams().size());
        */

        throw new UnsupportedOperationException("validEndpointURI");
    }
}
