package com.jack.paytracker.db;

/*******************************************************************************
 * @project com.jack.paytracker - com.jack.paytracker.handler: 10/7/2025 @ 11:20 PM
 * @autor Jack Malik - Primechannel Corporation Ltd.
 *******************************************************************************/

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class DbConnection {
    private static final String url = "jdbc:h2:file:~/paytracker;DB_CLOSE_ON_EXIT=TRUE;";
    private static final String user = "sa";
    private static final String pass = "";
    public static Connection create() throws SQLException {
        Connection conn = DriverManager.getConnection(url, user, pass);
        conn.setAutoCommit(true);
        return conn;
    }
}
