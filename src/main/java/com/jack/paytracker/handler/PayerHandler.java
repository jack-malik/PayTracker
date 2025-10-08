package com.jack.paytracker.handler;

/*******************************************************************************
 * @project PayTracker-tmp - com.jack.paytracker.handler: 10/7/2025 @ 10:40 PM
 * @autor Jack Malik - Primechannel Corporation Ltd.
 *******************************************************************************/

import com.jack.paytracker.db.DbConnection;
import com.jack.paytracker.PayTracker;
import com.jack.paytracker.model.Payer;
import io.muserver.MuRequest;
import io.muserver.MuResponse;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PayerHandler extends GenericPayTrackerHandler {

    public PayerHandler(final PayTracker tracker) {
        registerServer(tracker);
    }

    public void handle(MuRequest request, MuResponse response, Map<String,String> pathParams) throws Exception {

        List<Payer> payerList = new ArrayList<>();

        try (Connection connection = DbConnection.create()) {
            try (PreparedStatement statement = connection.prepareStatement(Payer.PAYER_QUERY_ALL_SQL)) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        Payer payer = new Payer();
                        payer.setId(resultSet.getInt("ID"));
                        payer.setName(resultSet.getString("NAME"));
                        payerList.add(payer);
                    }
                }
            }
        } catch (SQLException e) {
            response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            response.write("Unexpected exception while reading payers information from DB.");
            return;
        }
        response.contentType(MediaType.APPLICATION_JSON);
        response.write(PayTracker.webObjectMapper.writeValueAsString(payerList));
    }
}
