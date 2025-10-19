package com.jack.paytracker.db;

/*******************************************************************************
 * @project PayTracker-tmp - com.jack.paytracker.db: 10/7/2025 @ 5:36 PM
 * @autor Jack Malik - Primechannel Corporation Ltd.
 *******************************************************************************/

import com.jack.paytracker.PayTracker;
import com.jack.paytracker.model.Payer;
import com.jack.paytracker.model.Payment;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class DbHelper {

    public static final String AGGREGATED_PAYMENT_SQL_QUERY_STR = "SELECT CCY.CCY_CD AS CURRENCY, " +
            "SUM(PMT.PAY_AMOUNT) AS TOTAL_PAID FROM PAYTRACKER.PAYMENT PMT, PAYTRACKER.CURRENCY CCY " +
            "WHERE CCY.ID = PMT.PAY_CCY_ID GROUP BY PMT.PAY_CCY_ID";

    public static final String SQL_PAYER_QUERY_ALL_STR = "SELECT * FROM PAYTRACKER.PAYER";
    public static final String SQL_PAYMENT_QUERY_ALL_STR = "SELECT * FROM PAYTRACKER.PAYMENT";
    public static final String SQL_CURRENCY_QUERY_ALL_STR = "SELECT * FROM PAYTRACKER.CURRENCY";

    private final static Logger logger = LoggerFactory.getLogger(DbHelper.class);

    public static HashMap<String, Integer> refreshCurrencyCache(HashMap<String, Integer> ccyName2IdMap) {

//        try (Connection connection = DbConnectionPool.create()) {
        try (DbConnectionPool.DbConnection connection = DbConnectionPool.get()) {
            try (PreparedStatement statement = connection.get().prepareStatement(SQL_CURRENCY_QUERY_ALL_STR)) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        ccyName2IdMap.put(resultSet.getString("CCY_CD"), resultSet.getInt("ID"));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unexpected exception when caching Currency information.");
        }
        return ccyName2IdMap;
    }

    public static List<Payment> refreshPaymentCache(List<Payment> paymentList) {

        try (DbConnectionPool.DbConnection connection = DbConnectionPool.get()) {
            try (PreparedStatement statement = connection.get().prepareStatement(Payment.PAYMENT_QUERY_ALL_SQL)) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        Payment payment = new Payment();
                        payment.setId(resultSet.getInt("ID"));
                        payment.setPayerId(resultSet.getInt("PAYER_ID"));
                        payment.setCurrencyId(resultSet.getInt("PAY_CCY_ID"));
                        payment.setAmount(resultSet.getDouble("PAY_AMOUNT"));
                        paymentList.add(payment);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Unexpected exception when caching payments: '" + e.getMessage() + "'.");
            return null;
        }
        return paymentList;
    }
    public static HashMap<String, Integer> refreshPayerCache(HashMap<String, Integer> payerName2IdMap ) {

        try (DbConnectionPool.DbConnection connection = DbConnectionPool.get()) {
            try (PreparedStatement statement = connection.get().prepareStatement(Payer.PAYER_QUERY_ALL_SQL)) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        payerName2IdMap.put(resultSet.getString("NAME"), resultSet.getInt("ID"));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unexpected exception when caching Currency information.");
        }
        return payerName2IdMap;
    }

    public static void refreshAggregatedPaymentCache(Map<String, Double> cache) throws RuntimeException {
        try (DbConnectionPool.DbConnection connection = DbConnectionPool.get()) {
            try (PreparedStatement statement =
                         connection.get().prepareStatement(DbHelper.AGGREGATED_PAYMENT_SQL_QUERY_STR)) {
                List<Payment> paymentsForTimestamp = new ArrayList<>();
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        cache.put(resultSet.getString("CURRENCY"),
                                resultSet.getDouble("TOTAL_PAID"));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unexpected exception while refreshing pay cache: ['" +
                e.getMessage() + "']");
        }
    }
}
