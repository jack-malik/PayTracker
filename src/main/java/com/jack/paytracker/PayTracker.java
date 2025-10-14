package com.jack.paytracker;

/*******************************************************************************
 * @project com.jack.paytracker - com.jack.paytracker.handler: 10/7/2025 @ 7:20 PM
 * @autor Jack Malik - Primechannel Corporation Ltd.
 *******************************************************************************/

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.concurrent.*;
import java.util.Properties;
import java.io.IOException;
import java.io.InputStream;

import io.muserver.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.h2.tools.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * import all type-based handlers used
 */
import com.jack.paytracker.handler.PaymentHandler;
import com.jack.paytracker.handler.CurrencyHandler;
import com.jack.paytracker.handler.PayerHandler;
import com.jack.paytracker.handler.PaymentProcessorHandler;

import com.jack.paytracker.db.DbHelper;

public class PayTracker {

    /**
     * Server configuration values - should move to config file
     */
    public static final int MAX_MUSERVER_INSTANCES_SIZE = 2;

    public static final String SERVER_THREAD_POOL_SIZE_STR = "executor.pool.size";
    public static final int MAX_PAYMENT_TRACKING_TIME_IN_MILIS = 1800000;
    public static final int REFRESH_TIME_IN_MILLIS = 60000; // refresh every minute as per the spec
    public static final int DEFAULT_MU_SERVER_PORT_NUMBER = 51234;
    public enum Status { RUNNABLE, RUNNING, TERMINATED };

    private final static Logger logger = LoggerFactory.getLogger(PayTracker.class);

    /**
     * PayTracker implementation servers
     */
    private static volatile MuServerBuilder webServerImpl;
    private static volatile Server dbServerImpl;
    public static final ObjectMapper webObjectMapper = new ObjectMapper();
    public PayTracker.Status status = Status.RUNNABLE;

    private LinkedBlockingDeque<MuServer> muServerPool =
            new LinkedBlockingDeque<>(MAX_MUSERVER_INSTANCES_SIZE);
    public Map<String, Double> aggregatedPaymentCache =
            Collections.synchronizedMap(new HashMap<String, Double>());
    public HashMap<String, Integer> currencyName2IdCache;
    public HashMap<String, Integer> clientName2IdCache;

    private static final Properties properties = new Properties();

    private static void loadPropertiesFromConfigFile() {

        if (PayTracker.properties.size() == 0) {
            try (InputStream stream = PayTracker.class
                    .getClassLoader()
                    .getResourceAsStream("application.properties")) {
                if (stream == null) {
                    throw new RuntimeException("Unable to find application.properties file.");
                }
                PayTracker.properties.load(stream);
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
        String fullName = PayTracker.class.getSimpleName() + "." + name;
        return (String)PayTracker.properties.getOrDefault(fullName.toLowerCase(),"");
    }

    /**
     * @return integer value of total client connections used by the MuServer
     */
    public int totalConnections() {
        assert this.muServerPool.size() > 0;
        return this.muServerPool.peek().activeConnections().size();
    }

    /**
     * PayTracker constructor -
     */
    public PayTracker() {

        SimpleModule module = new SimpleModule();
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer());
        PayTracker.webObjectMapper.registerModule(module);

        try (InputStream stream = PayTracker.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (stream == null) {
                throw new RuntimeException("Unable to find application.properties file.");
            }
            properties.load(stream);
        } catch (IOException e) {
            System.out.println("Unexpected exception while processing PayTracker properties.");
            throw new RuntimeException(e);
        }
    };

    /**
     * Method used to clean up resoources - close connections, stop servers, etc.
     */
    public void terminate() {
        synchronized (this) {
            try {
                PayTracker.dbServerImpl.stop();
                if (!PayTracker.webServerImpl.executor().isTerminated())
                    PayTracker.webServerImpl.executor().shutdown();
            } catch (Exception e) {
                logger.error("Failed to terminate cleanly: ['" + e.getMessage() + "']. Aborting.");
            } finally {
                this.status = status.TERMINATED;
                logger.debug("Changing PayTracker status to 'TERMINATED' ..");
            }
        }
    }

    /**
     * Method used to start H2 database console web server - the server is a helper object only
     * available to peek into the content of the H2 database storing PayTracker data.
     * @return instance of h2.tools.Server
     */
    public static Server startDbWebServer() {
        /*
        Creating web server for H2 Console for user to query H2 database if desired
        The database contains 3 tables:
        1/. PAYER - containig payer id and name
        2/. CURRENCY - containing currency id and code
        3/. PAYMENT - containing payer payment details
         */
        Server dbWebServer = null;
        try {
            dbWebServer = Server.createWebServer("-web").start();
            logger.info("H2 Database console started. Login at: " + dbWebServer.getURL());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to start H2 database console web server ..");
        }
        return dbWebServer;
    }

    /**
     * Method to build MuServerBuilder object and start instance of MuServer
     * @param executor
     * @return instance of MuServerBuilder
     */
    protected MuServerBuilder startMuServer(ExecutorService executor) {
        /*
        Instance method creating the MuServerBuilder object and initializing it
        for MuServer to be started.
         */
        synchronized (this) {
            MuServerBuilder muServerBuilder = null;
            try {
                if (status.RUNNABLE == this.status) {
                    // create builder - start server instance
                    muServerBuilder = PayTracker.MuServerBuilderInstance(executor);
                    registerHandlers(muServerBuilder);
                    MuServer muServer = muServerBuilder.start();
                    muServerPool.add(muServer);
                    logger.info("MuServer started successfully at ['" + muServer.uri() + "']");
                    this.status = status.RUNNING;
                } else {
                    if (status.RUNNING == this.status) {
                        logger.warn("Attempted to start PayTracker. Application already running.");
                        muServerBuilder = PayTracker.webServerImpl;
                    } else {
                        throw new RuntimeException("Failed to start MuServer. Already terminated.");
                    }
                }
            } catch (Exception ex) {
                throw new RuntimeException("Unexpected exception from MuServer: ['" + ex.getMessage() + "']");
            }
            return muServerBuilder;
        }
    }

    /**
     * Method to start MuServer and h2.tools.Server instances
     * @return boolean true if successful, false otherwise.
     */
    protected boolean startServers() {
        try {
            int execThreadPoolSize = Integer.parseInt(PayTracker.property(PayTracker.SERVER_THREAD_POOL_SIZE_STR));
            PayTracker.webServerImpl = startMuServer(Executors.newFixedThreadPool(execThreadPoolSize));
        } catch (Exception ex) {
            logger.error("Failed to start MuServer: ['" + ex.toString() + "']");
            return false;
        }
        try {
            PayTracker.dbServerImpl = PayTracker.startDbWebServer();
        } catch (Exception ex) {
            logger.error("Failed to start H2 database web server: [" + ex.toString() + "']");
            return false;
        }
        return true;
    }

    /**
     * Creates instance of MuServer and H2 database web server. Creates cache of currencies
     * and payers to be used when processing payments.
     * @return boolean value true if successful, false otherwise
     */
    protected boolean initialize() {
        /*
        Instance method initializing PayTracker object.
        Please note that the MuServerBuilder as well the org.h2.tools.Server are static objects
         */
        if (!startServers()) {
            logger.error("Failed to start PayTracker servers");
            return false;
        }

        if (!buildCache()) {
            logger.error("Failed to cache PayTracker data when instantiating server.");
            return false;
        }

        logger.info("PayTracker successfully initialized.");
        return true;
    }

    /**
     * Builds cache of payers and currency codes to be used when handling payments.
     * @return boolean value true if successful, false otherwise.
     */
    private boolean buildCache() {
        try {
            this.clientName2IdCache = DbHelper.refreshPayerCache(new HashMap<String, Integer>());
            this.currencyName2IdCache = DbHelper.refreshCurrencyCache(new HashMap<String, Integer>());
        } catch (Exception ex) {
            logger.error("Unexpected exception when caching Currency/Client info: ['" + ex.getMessage() + "'].");
            return false;
        }
        logger.info("Successfully built PayTracker static data caches.");
        return true;
    }

    /**
     * Registers PayTracker instance handlers for selective services.
     * @param builder - object of type MuServerBuilder to register handlers with.
     */
    protected void registerHandlers(MuServerBuilder builder) {
        /*
         * Register static handlers
         */
        builder.addHandler(Method.GET, "/", (request, response, pathParams) -> {
            response.contentType("text/html");
            response.write("<h1>Jack's Payment Tracker</h1>");
        });
        builder.addHandler(Method.GET, "/paytracker", (request, response, pathParams) -> {
            response.contentType("text/html");
            response.write("<h1>Jack's Payment Tracker</h1>");
        });
        /*
         * Register MuServer RouteHandlers
         */
        builder.addHandler(Method.GET, "/paytracker/currencies", new CurrencyHandler(this));
        builder.addHandler(Method.GET, "/paytracker/payers", new PayerHandler(this));
        builder.addHandler(Method.GET, "/paytracker/payments", new PaymentHandler(this));
        builder.addHandler(Method.GET, "/paytracker/payment/{currency}/{amount}",
                new PaymentProcessorHandler(this));
    }

    /**
     * Creates instance of MuServer build. Acts as creation of Singleton object for all
     * potential instance os MuServer
     * @param executorService - object of type ExecutorService
     * @return object of type MuServerBuilder
     */
    public static MuServerBuilder MuServerBuilderInstance(ExecutorService executorService) {

        if (null == PayTracker.webServerImpl) {
            synchronized (PayTracker.class) {
                if (null == PayTracker.webServerImpl) {
                    PayTracker.webServerImpl = MuServerBuilder.httpServer()
                            .withHttpPort(PayTracker.DEFAULT_MU_SERVER_PORT_NUMBER)
                            .withHandlerExecutor(executorService)
                            .addShutdownHook(true);
                }
            }
        }
        return PayTracker.webServerImpl;
    }

    /**
     * Main loop executes until predefined time elapses or PayTracker is terminated
     * explicitly when tracking payer payments.
     * @return boolean value true if successful, false otherwise
     */
    public boolean track() {
        /*
        Looping with periodic query PAYMENT table to produce output to the console
        containing aggregated view of all payments grouped by currency code as per the spec
         */
        int elapsedTime = 0;
        while (status.RUNNING == this.status) {

            try {
                DbHelper.refreshAggregatedPaymentCache(this.aggregatedPaymentCache);
            } catch (RuntimeException ex) {
                return false;
            }

            logger.info("Aggregated Payment Report as of " + LocalDateTime.now());
            logger.info("-----------------------------------------------------------");
            int counter = 0;
            for (String ccy: this.aggregatedPaymentCache.keySet()) {
                counter += 1;
                Double amt = this.aggregatedPaymentCache.get(ccy);
                logger.info("\t#" + counter + ": CURRENCY: " + ccy + " - TOTAL PAID: " + amt);
            }
            if (0 == counter) {
                logger.info("\tCurrently no payments found");
            }
            try {
                Thread.sleep(REFRESH_TIME_IN_MILLIS);
            } catch (InterruptedException ex) {
                // ignore
            }
            elapsedTime += REFRESH_TIME_IN_MILLIS;
            if (elapsedTime > PayTracker.MAX_PAYMENT_TRACKING_TIME_IN_MILIS) {
                this.terminate();
            }
        }
        return true;
    }

    public static void main(String[] args) {
        /*
         Instantiate PayTracker
         */
        PayTracker tracker = null;
        try {
            tracker = new PayTracker();
            if (!tracker.initialize()) {
                throw new RuntimeException("Failed to initialize PayTracker.");
            }
        } catch (Exception ex) {
            System.out.println("Failed to start application: ['" + ex.toString() + "'].");
            return;
        }
        /*
        Run PayTracker instance for sometime - terminate gracefully when time elapsed
         */
        try {
            tracker.track();
        } catch (Exception ex) {
            System.out.println("Failed while tracking payment: ['" + ex.toString() + "'].");
            return;
        }
        tracker.terminate();
        logger.info("PayTracker exiting .. ");
    }
}
