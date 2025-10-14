package com.jack.paytracker.db;

/*******************************************************************************
 * @project com.jack.paytracker - com.jack.paytracker.handler: 10/7/2025 @ 11:20 PM
 * @autor Jack Malik - Primechannel Corporation Ltd.
 *******************************************************************************/

import com.jack.paytracker.PayTracker;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbConnectionPool implements AutoCloseable {

    private final static Logger logger = LoggerFactory.getLogger(DbConnectionPool.class);
    public static final String PROP_POOL_SIZE = "pool.size";
    private static final String PROP_DB_URL = "db.url";
    private static final String PROP_DB_URL_INIT = "db.url.init";
    private static final String PROP_DB_USER = "db.user.name";
    private static final String PROP_DB_PASSWORD = "db.user.password";
    private static final String PROP_DB_DIR_NAME = "db.dir.name";
    private static final String PROP_DB_FILE_NAME = "db.file.name";
    private static final Properties properties = new Properties();

    /**
     * Pool-specific implementation of database connection with autoexecution of close() connection method
     */
    public static class DbConnection implements AutoCloseable {

        private Connection impl;
        private static int count;

        private int id;

        public DbConnection(int id) {
            synchronized (DbConnection.class) {
                count += 1;
            }
            try {
                this.impl = DbConnection.connection();
            } catch (SQLException ex) {
                throw new RuntimeException(ex.getMessage());
            }
            this.id = id;
            logger.info("DbConnection-#" + this.id + " to H2 database ...");
        }

        public Connection get() { return impl; };

        public static Connection connection() throws SQLException {

            // get db file name and check if exists
            String fullPathFileName = DbConnectionPool.property(DbConnectionPool.PROP_DB_DIR_NAME) +
                    File.separator + DbConnectionPool.property(DbConnectionPool.PROP_DB_FILE_NAME);
            File dbFile = new File(fullPathFileName);

            // get database url
            StringBuilder buf = new StringBuilder(property(DbConnectionPool.PROP_DB_URL));
            if (!dbFile.exists()) {
                logger.info("Detected no existing database file. Creating new H2 db. for PayTracker");
                buf.append(property(DbConnectionPool.PROP_DB_URL_INIT));
            }
            //System.out.println("CLASSPATH: " + System.getProperty("java.class.path"));
            Connection jdbcConnection = null;
            try {
                //Initialize connection - set schema for 'paytracker' - add a couple of dummy Payers
                String login = property(DbConnectionPool.PROP_DB_USER);
                String password = property(DbConnectionPool.PROP_DB_PASSWORD);
                jdbcConnection = DriverManager.getConnection(buf.toString(), login, password);
                jdbcConnection.setAutoCommit(true);
            } catch (SQLException e) {
                throw new RuntimeException("Unexpected exception when connecting to H2: '" + e.getMessage() + "'");
            }
            return jdbcConnection;
        }

        @Override
        public void close() {
            // return actual connection stored to the pool of connections
            try {
                poolImpl.put(this);
            } catch (InterruptedException ex) {
                try {
                    impl.close();
                } catch (SQLException sqlex) {
                    impl = null;
                    return;
                }
            }
            logger.info("Current DB pool size     : " + DbConnectionPool.instance().size() + ".");
            logger.info("Total connections created: " + DbConnectionPool.instance().totalConnections() + ".");
        }
    }

    private static final LinkedBlockingDeque<DbConnection> poolImpl =
            new LinkedBlockingDeque<>(Integer.parseInt(
                    DbConnectionPool.property(PROP_POOL_SIZE)));

    /**
     * Load properties related to database used from application.properties files
      */
    private static void loadPropertiesFromConfigFile() {

        if (DbConnectionPool.properties.size() == 0) {
            // No properties cached - open application.properties file to read DbConnectionPool properties
            try (InputStream stream = PayTracker.class.getClassLoader().getResourceAsStream("application.properties")) {
                if (stream == null) {
                    throw new RuntimeException("Unable to find application.properties file.");
                }
                DbConnectionPool.properties.load(stream);
            } catch (IOException e) {
                System.out.println("Unexpected exception while processing PayTracker properties.");
                throw new RuntimeException(e);
            }
        }
    };

    public static String property(final String name) {

        if (properties.size() == 0) {
            loadPropertiesFromConfigFile();
        }
        String fullName = DbConnectionPool.class.getSimpleName().toLowerCase() + "." + name;
        return (String)DbConnectionPool.properties.getOrDefault(fullName,"");
    }

    private static final class InstanceHolder {
        public static final DbConnectionPool instance = new DbConnectionPool();
    }

    public static DbConnectionPool instance() {
        return InstanceHolder.instance;
    }

    public static DbConnection get() throws SQLException {

        if (poolImpl.isEmpty()) {
            try {
                poolImpl.put(new DbConnection(poolImpl.size()+1));
            } catch (InterruptedException ex) {
                throw new RuntimeException("Exception getting DB connection: '" + ex.getMessage() + "'");
            }
        }
        return poolImpl.removeLast();
    }

    public int size() { return poolImpl.size(); };

    public int totalConnections() { return DbConnection.count; };

    /**
     * Cleanup method for class instance
     */
    @Override
    public void close() {
        // remove each DbConnection wrapper object from pool implementation and close each jdbc connection
        while (poolImpl.size() > 0) {
            DbConnection conn = poolImpl.removeLast();
            Connection jdbcConn = conn.get();
            try {
                jdbcConn.close();
            } catch (SQLException ex) {
                // ignore
            }
        }
    }
}
