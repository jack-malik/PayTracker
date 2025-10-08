package com.jack.paytracker.db;

/*******************************************************************************
 * @project PayTracker-tmp - com.jack.paytracker.db: 10/7/2025 @ 5:36 PM
 * @autor Jack Malik - Primechannel Corporation Ltd.
 *******************************************************************************/

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

public class DbHelper {

    public static final String AGGREGATED_PAYMENT_SQL_QUERY_STR = "SELECT CCY.CCY_CD AS CURRENCY, " +
            "SUM(PMT.PAY_AMOUNT) AS TOTAL_PAID FROM PAYTRACKER.PAYMENT PMT, PAYTRACKER.CURRENCY CCY " +
            "WHERE CCY.ID = PMT.PAY_CCY_ID GROUP BY PMT.PAY_CCY_ID";


    public static final String SQL_PAYER_QUERY_ALL_STR = "SELECT * FROM PAYTRACKER.PAYER";
    public static final String SQL_PAYMENT_QUERY_ALL_STR = "SELECT * FROM PAYTRACKER.PAYMENT";
    public static final String SQL_CURRENCY_QUERY_ALL_STR = "SELECT * FROM PAYTRACKER.CURRENCY";


    public static HashMap<String, Integer> refreshCurrencyCache(HashMap<String, Integer> ccyName2IdMap) {

        try (Connection connection = DbConnection.create()) {
            try (PreparedStatement statement = connection.prepareStatement(SQL_CURRENCY_QUERY_ALL_STR)) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        ccyName2IdMap.put(resultSet.getString("CCY_CD"), resultSet.getInt("ID"));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Unexpected exception when caching Currency information.");
            throw new RuntimeException("Failed to cache Currency information");
        }
        return ccyName2IdMap;
    }

    public static List<Payment> refreshPaymentCache(List<Payment> paymentList) {

        try (Connection connection = DbConnection.create()) {
            try (PreparedStatement statement = connection.prepareStatement(Payment.PAYMENT_QUERY_ALL_SQL)) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        Payment payment = new Payment();
                        payment.setId(resultSet.getInt("ID"));
                        payment.setPayerId(resultSet.getInt("PAYER_ID"));
                        payment.setCurrency(resultSet.getString("PAY_CCY_ID"));
                        payment.setAmount(resultSet.getInt("PAY_AMOUNT"));
                        paymentList.add(payment);
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Unexpected exception when caching payments: '" + e.getMessage() + "'.");
            return null;
        }
        return paymentList;
    }
    public static HashMap<String, Integer> refreshPayerCache(HashMap<String, Integer> payerName2IdMap ) {

        try (Connection connection = DbConnection.create()) {
            try (PreparedStatement statement = connection.prepareStatement(Payer.PAYER_QUERY_ALL_SQL)) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        payerName2IdMap.put(resultSet.getString("NAME"), resultSet.getInt("ID"));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Unexpected exception when caching Currency information.");
            throw new RuntimeException("Failed to cache Currency information");
        }
        return payerName2IdMap;
    }

    public static boolean refreshAggregatedPaymentCache(Map<String, Double> cache) {
        try (Connection connection = DbConnection.create()) {
            try (PreparedStatement statement =
                         connection.prepareStatement(DbHelper.AGGREGATED_PAYMENT_SQL_QUERY_STR)) {
                List<Payment> paymentsForTimestamp = new ArrayList<>();
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        cache.put(resultSet.getString("CURRENCY"),
                                resultSet.getDouble("TOTAL_PAID"));
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Unexpected exception while refreshing pay cache: ['" + e.getMessage() + "']");
            return false;
        }
        return true;
    }

    public static Connection connect() {

        String userHomeDirectory = System.getProperty("user.home");
        System.out.println("User Home Directory: " + userHomeDirectory);
        String fullPathDbFileName = userHomeDirectory + File.separator + "paytracker.mv.db";
        File h2DbFileName = new File(fullPathDbFileName);
        String username = "sa";
        String password = "";
        String h2InitStr = "jdbc:h2:file:~/paytracker;DB_CLOSE_ON_EXIT=TRUE;";//DB_CLOSE_DELAY=-1";
        if (h2DbFileName.exists()) {
            System.out.println("Detected existing database file '" + h2DbFileName + "' .. ");
        } else {
            h2InitStr += ";INIT=RUNSCRIPT FROM 'classpath:/db/schema.sql'";
            System.out.println("Detected no existing database file. Creating new H2 db. for PayTracker");
        }
        //System.out.println("CLASSPATH: " + System.getProperty("java.class.path"));
        Connection dbConnection = null;
        try {
            //Initialize connection - set schema for 'paytracker' - add a couple of dummy Payers
            dbConnection = DriverManager.getConnection(h2InitStr, username, password);
            System.out.println("Connected to H2 database ...");
            dbConnection.setAutoCommit(true);
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Unexpected exception: " + e.getMessage() + ". Aborting.");
            throw new RuntimeException("Failed to initialize H2 Database connection.");
        }
        return dbConnection;
    }
}
