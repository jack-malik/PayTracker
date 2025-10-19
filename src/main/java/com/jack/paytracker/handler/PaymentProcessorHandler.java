package com.jack.paytracker.handler;

/*******************************************************************************
 * @project com.jack.paytracker - com.jack.paytracker.handler: 10/7/2025 @ 11:25 PM
 * @autor Jack Malik - Primechannel Corporation Ltd.
 *******************************************************************************/

import java.time.temporal.ChronoUnit;
import java.util.Map;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;

import java.time.LocalDateTime;
import javax.ws.rs.core.Response;

import com.jack.paytracker.db.DbConnectionPool;
import com.jack.paytracker.db.DbHelper;
import io.muserver.MuRequest;
import io.muserver.MuResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jack.paytracker.PayTracker;
import com.jack.paytracker.model.*;

public class PaymentProcessorHandler extends GenericPayTrackerHandler {

    private final static Logger logger = LoggerFactory.getLogger(PaymentProcessorHandler.class);

    public PaymentProcessorHandler(final PayTracker tracker) {
        registerTracker(tracker);
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
            logger.error("Failed to handle 'payment' request. Exception: [" + ex.getMessage() + "'].");
            response.contentType("text/html");
            response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            response.write("<html><h1>Jack's Payment Tracker</h1><p>Message: '" + path +
                    "' - Failed to handler request: '" + path + "'. </p></html>");
            return;
        }

        response.contentType("text/html");
        StringBuilder buffer = new StringBuilder();
        buffer.append("<html><h1>Jack's Payment Tracker</h1><p>Message: '")
                .append(path)
                .append(" request processed successfully.</p></html>")
                .append("<h3>Current Aggregated Payment Report</h3>")
                .append("<p>---------------------------------------------------------------</p>");
        try {
            DbHelper.refreshAggregatedPaymentCache(tracker.aggregatedPaymentCache);
        } catch (RuntimeException ex) {
            String msg = "Failed to refresh PayTracker payment cache while handling payment: [" +
                    ex.getMessage() + "'].";
            logger.error(msg);
            buffer.append("msg");
            response.write(buffer.toString());
            return;
        }
        int counter = 0;
        for (String ccy: tracker.aggregatedPaymentCache.keySet()) {
            counter += 1;
            Double amt = tracker.aggregatedPaymentCache.get(ccy);
            buffer.append("<p>#")
                    .append(counter)
                    .append(": CURRENCY: ")
                    .append(ccy)
                    .append(" - TOTAL PAID: ")
                    .append(amt)
                    .append("</p>");
        }
        buffer.append("<p>---------------------------------------------------------------</p>");
        buffer.append("<p><b>Total current client connections: ")
                .append(tracker.totalConnections())
                .append("<b></p>")
                .append("<p>---------------------------------------------------------------</p>");
        response.write(buffer.toString());
    }

    public void processPayment(Map<String, String> params)
            throws Exception {

        String ccy = params.get("currency");
        String amountStr = params.get("amount");
        String optionalClientName = params.get("payer");
        Integer ccyId = tracker.currencyName2IdCache.getOrDefault(ccy, 0);
        Integer payerId = tracker.clientName2IdCache.getOrDefault(optionalClientName, 1);
        try (DbConnectionPool.DbConnection connection = DbConnectionPool.get()) {
            Payer payer = new Payer();
            payer.setId(payerId);
            payer.setName(optionalClientName);
            Payment payment = new Payment();
            payment.setPayerId(payerId);
            payment.setCurrencyId(ccyId);
            payment.setAmount(Double.parseDouble(amountStr));
            payment.setPayTime(LocalDateTime.now());
            //                statement.setDouble(3, payment.getAmount());
//                statement.setTimestamp(4,
            try (PreparedStatement statement = connection.get().prepareStatement(Payment.SAVE_PAYMENT_SQL)) {
                statement.setInt(1, payerId);
                statement.setInt(2, ccyId);
                statement.setDouble(3, Double.parseDouble(amountStr));
                statement.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)));
                statement.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)));
                statement.executeUpdate();
            }
        }
        logger.info("Successfully processed payment: [CCY=" + ccy + "|AMT=" + amountStr + "].");
    }
}
