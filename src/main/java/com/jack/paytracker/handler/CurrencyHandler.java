package com.jack.paytracker.handler;

/*******************************************************************************
 * @project com.jack.paytracker - com.jack.paytracker.handler: 10/7/2025 @ 11:20 PM
 * @autor Jack Malik - Primechannel Corporation Ltd.
 *******************************************************************************/

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
//import java.sql.Connection;
import java.net.URI;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.jack.paytracker.PayTracker;
import com.jack.paytracker.db.DbConnectionPool;
import com.jack.paytracker.model.Currency;
import io.muserver.MuRequest;
import io.muserver.MuResponse;

public class CurrencyHandler extends GenericPayTrackerHandler {

//    public static final String RE_PATTERN = "http://127\\.0\\.0\\.1:51234/paytracker/currencies"; //"^(.)+(/+)+((/)*(.)*)*$";
    public static final String RE_PATTERN = "^((.)+(/+)+((/)*(.)*)*$)";

    public static final Pattern handlerPattern = Pattern.compile(RE_PATTERN);
    private final static Logger logger = LoggerFactory.getLogger(CurrencyHandler.class);
    public CurrencyHandler(final PayTracker tracker) {
        registerTracker(tracker);
    }

    public String reStr = "";
    protected List<Currency> dbReadCcyList() throws RuntimeException {
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
            throw new RuntimeException("Failed to retrieve currencies. Exception: ['" + e.getMessage() + "'].");
        }
        return ccyList;
    }
    public void handle(MuRequest request, MuResponse response, Map<String,String> pathParams) throws Exception {

        String fullPath = (String)request.uri().toString();
        String path = (String)request.uri().getPath();
        if (!this.isValidURL(fullPath)) {
            logger.error("Invalid URL detected: '" + path + "'.");
            response.contentType("text/html");
            response.write("<html><h1>Jack's Payment Tracker</h1><p>Message: '" + path +
                    "' - URL path: '" + fullPath + "' invalid on this server. Try something else ..</p></html>");
            return;
        }
        if (handlerPattern.matcher(RE_PATTERN).matches()) {
            List<Currency> ccyList = dbReadCcyList();
            try {
                ccyList = dbReadCcyList();
            } catch (RuntimeException re) {
                String msg = "Exception while processing request: " + path + ". Exception: " + re.getMessage() + "']";
                logger.error(msg);
                response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
                response.write(msg);
            }
            response.contentType(MediaType.APPLICATION_JSON);
            response.write(PayTracker.webObjectMapper.writeValueAsString(ccyList));
            return;
        }
        response.contentType("text/html");
        response.write("<html><h1>Jack's Payment Tracker</h1><p>Message: '" + path +
                "' - url path invalid on this server. Try something else ..</p></html>");
    }
}
