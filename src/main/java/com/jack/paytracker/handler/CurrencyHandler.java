package com.jack.paytracker.handler;

/*******************************************************************************
 * @project com.jack.paytracker - com.jack.paytracker.handler: 10/7/2025 @ 11:20 PM
 * @autor Jack Malik - Primechannel Corporation Ltd.
 *******************************************************************************/

import com.jack.paytracker.PayTracker;
import com.jack.paytracker.db.DbConnectionPool;
import com.jack.paytracker.model.Currency;
import io.muserver.MuRequest;
import io.muserver.MuResponse;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CurrencyHandler extends GenericPayTrackerHandler {

    public CurrencyHandler(final PayTracker tracker) {
        registerTracker(tracker);
    }

    public void handle(MuRequest request, MuResponse response, Map<String,String> pathParams) throws Exception {
        List<Currency> ccyList = new ArrayList<>();
        try (DbConnectionPool.DbConnection connection = DbConnectionPool.get()) {
            try (PreparedStatement statement = connection.get().prepareStatement(Currency.CURRENCY_QUERY_ALL_SQL)) {
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
            response.write("Unexpected exception while reading currencies information from db.");
            return;
        }
        response.contentType(MediaType.APPLICATION_JSON);
        response.write(PayTracker.webObjectMapper.writeValueAsString(ccyList));
    }
}
