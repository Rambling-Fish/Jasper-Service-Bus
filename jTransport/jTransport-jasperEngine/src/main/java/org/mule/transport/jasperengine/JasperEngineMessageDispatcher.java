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

import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.transport.jms.JmsMessageDispatcher;

/**
 * <code>JasperEngineMessageDispatcher</code> TODO document
 */
public class JasperEngineMessageDispatcher extends JmsMessageDispatcher{

    public JasperEngineMessageDispatcher(OutboundEndpoint endpoint){
        super(endpoint);
    }
}

