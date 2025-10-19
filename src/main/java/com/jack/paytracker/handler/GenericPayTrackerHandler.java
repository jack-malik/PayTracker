package com.jack.paytracker.handler;

/*******************************************************************************
 * @project com.jack.paytracker - com.jack.paytracker.handler: 10/7/2025 @ 11:20 PM
 * @autor Jack Malik - Primechannel Corporation Ltd.
 *******************************************************************************/

import org.apache.commons.validator.routines.*;
//import org.apache.commons.validator.routines.UrlValidator;

import com.jack.paytracker.PayTracker;
import io.muserver.*;

import java.net.MalformedURLException;

public abstract class GenericPayTrackerHandler implements RouteHandler {

    protected PayTracker tracker;
    protected void registerTracker(PayTracker instance) {
        this.tracker = instance;
    }

    public boolean isValidURL(String url) throws MalformedURLException {
        String[] schemes = new String[]{"http", "https"};
        UrlValidator validator = new UrlValidator(schemes);
        return validator.isValid(url);
    }
}
