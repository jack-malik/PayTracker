package com.jack.paytracker.handler;

/*******************************************************************************
 * @project com.jack.paytracker - com.jack.paytracker.handler: 10/7/2025 @ 11:25 PM
 * @autor Jack Malik - Primechannel Corporation Ltd.
 *******************************************************************************/

import java.util.Map;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.sql.SQLException;

import java.time.LocalDateTime;

import javax.ws.rs.core.Response;

import com.jack.paytracker.db.DbConnection;
import com.jack.paytracker.db.DbHelper;
import io.muserver.MuRequest;
import io.muserver.MuResponse;

import com.jack.paytracker.PayTracker;
import com.jack.paytracker.model.Payment;

public class PaymentProcessorHandler extends GenericPayTrackerHandler {

    public PaymentProcessorHandler(final PayTracker tracker) {
        registerServer(tracker);
    }

    /**
     * handling method when new payment is made.
     * @param request  The HTTP request.
     * @param response The HTTP response.
     */
    public void handle(MuRequest request, MuResponse response, Map<String,String> params) throws Exception {
        /*
         * Validates uri provided. Accepts requests of the form: /paytracker/payment/USD/100
         * but also of the more generic common form: /paytracker/payment?currency=USD&amount=100.
         */
//        Map<String, String> params = parseParameters(request);
        if (params == null) {
            String path = (String)request.uri().getPath();
            response.contentType("text/html");
            response.write("<html><h1>Jack's Payment Tracker</h1><p>Message: '" + path +
                    "' - url path invalid on this server. Try something else ..</p></html>");
            return;
        }
        String path = (String)request.uri().getPath();
        if (!path.contains("/payment")) {
            response.contentType("text/html");
            response.write("<html><h1>Jack's Payment Tracker</h1><p>Message: '" + path +
                    "' - url path invalid on this server. Try something else ..</p></html>");
            return;
        }
        try {
            processPayment(params);
        } catch (Exception ex) {
            System.out.println("Failed to handler '/payment' request");
            response.contentType("text/html");
            response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            response.write("<html><h1>Jack's Payment Tracker</h1><p>Message: '" + path +
                    "' - Failed to handler request: '" + path + "'. </p></html>");
            return;
        }

        response.contentType("text/html");
        StringBuffer buffer = new StringBuffer();
        buffer.append("<html><h1>Jack's Payment Tracker</h1><p>Message: '" + path +
                " request processed successfully.</p></html>");
        buffer.append("<h3>Current Aggregated Payment Report</h3>");
        buffer.append("<p>---------------------------------------------------------------</p>");

        if (!DbHelper.refreshAggregatedPaymentCache(tracker.aggregatedPaymentCache)) {
            System.out.println("Failed to refresh PayTracker payment cache while handling payment.");
            return;
        }
        int counter = 0;
        for (String ccy: tracker.aggregatedPaymentCache.keySet()) {
            counter += 1;
            Double amt = tracker.aggregatedPaymentCache.get(ccy);
            buffer.append("<p>#" + counter + ": CURRENCY: " + ccy + " - TOTAL PAID: " + amt + "</p>");
        }
        buffer.append("<p>---------------------------------------------------------------</p>");
        buffer.append("<p><b>Total current client connections: " +
                tracker.muServerPool.peek().activeConnections().size() + "<b></p>");
        buffer.append("<p>---------------------------------------------------------------</p>");
        response.write(buffer.toString());
    }

    public void processPayment(Map<String, String> params)
            throws Exception {

        String ccy = params.get("currency");
        String amount = params.get("amount");

        Integer ccyId = tracker.currencyName2IdCache.getOrDefault(ccy, 1);
        Integer clientId = tracker.clientName2IdCache.getOrDefault(ccyId, 1);
        try (Connection connection = DbConnection.create()) {
            try (PreparedStatement statement = connection.prepareStatement(Payment.SAVE_PAYMENT_SQL)) {
                statement.setInt(1, clientId);
                statement.setInt(2, ccyId);
//                statement.setDouble(3, payment.getAmount());
                statement.setDouble(3, Integer.parseInt(amount));
//                statement.setTimestamp(4, Timestamp.valueOf(payment.getTimestamp()));
                statement.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                statement.executeUpdate();
            }
        }
    }
}
