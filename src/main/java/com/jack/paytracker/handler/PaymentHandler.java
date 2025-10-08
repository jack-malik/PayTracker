package com.jack.paytracker.handler;

/*******************************************************************************
 * @project PayTracker-tmp - com.jack.paytracker.handler: 10/7/2025 @ 10:15 PM
 * @autor Jack Malik - Primechannel Corporation Ltd.
 *******************************************************************************/

import com.jack.paytracker.PayTracker;
import com.jack.paytracker.db.DbConnection;
import com.jack.paytracker.db.DbHelper;
import com.jack.paytracker.model.Currency;
import com.jack.paytracker.model.Payment;
import io.muserver.MuRequest;
import io.muserver.MuResponse;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class PaymentHandler extends GenericPayTrackerHandler {

    public PaymentHandler(final PayTracker tracker) {
        registerServer(tracker);
    }

    /**
     * handling method when new payment is made.
     * @param request  The HTTP request.
     * @param response The HTTP response.
     */
    public void handle(MuRequest request, MuResponse response, Map<String,String> pathParams) throws Exception {

        List<Currency> ccyList = new ArrayList<>();
        try (Connection connection = DbConnection.create()) {
            try (PreparedStatement statement = connection.prepareStatement(Payment.PAYMENT_QUERY_ALL_SQL)) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        Currency ccy = new Currency();
                        ccy.setCcyCode(resultSet.getString("CCY_CD"));
                        ccy.setId(resultSet.getInt("ID"));
                        ccyList.add(ccy);
                    }
                }
            }
        } catch (SQLException e) {
            response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            response.write("Unexpected exception while reading payments information from db.");
            return;
        }
        response.contentType(MediaType.APPLICATION_JSON);
        response.write(PayTracker.webObjectMapper.writeValueAsString(ccyList));
    }
}

