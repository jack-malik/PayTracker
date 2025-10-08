package com.jack.paytracker.handler;

/*******************************************************************************
 * @project com.jack.paytracker - com.jack.paytracker.handler: 10/7/2025 @ 11:20 PM
 * @autor Jack Malik - Primechannel Corporation Ltd.
 *******************************************************************************/

import com.jack.paytracker.PayTracker;
import io.muserver.*;

public abstract class GenericPayTrackerHandler implements RouteHandler {

    protected PayTracker tracker;
    protected void registerServer(PayTracker instance) {
        this.tracker = instance;
    }
}
