package com.jack.paytracker;

/*******************************************************************************
 * @project com.jack.paytracker - com.jack.paytracker.handler: 10/7/2025 @ 7:20 PM
 * @autor Jack Malik - Primechannel Corporation Ltd.
 *******************************************************************************/

import java.sql.SQLException;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.concurrent.*;

import io.muserver.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.h2.tools.Server;

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
    public static final int SERVER_THREAD_POOL_SIZE = 8;
    public static final int MAX_PAYMENT_TRACKING_TIME_IN_MILIS = 1800000;
    public static final int REFRESH_TIME_IN_MILLIS = 60000; // refresh every minute as per the spec
    public static final int DEFAULT_MU_SERVER_PORT_NUMBER = 51234;
    public enum Status { RUNNABLE, RUNNING, TERMINATED };

    /**
     * PayTracker implementation servers
     */
    public static volatile MuServerBuilder webServerImpl;
    public static volatile Server dbServerImpl;
    public static final ObjectMapper webObjectMapper = new ObjectMapper();
    public PayTracker.Status status = Status.RUNNABLE;

    /**
     * Db master connections and caches - master connection implemented for recovery
     * purposes if needed. PayTracker connection pool implemented in DbConnection.
     */
    protected Connection dbMasterConnection = DbHelper.connect();
    private LinkedBlockingDeque<Connection> dbConnectionPool =
            new LinkedBlockingDeque<>(SERVER_THREAD_POOL_SIZE);
    public LinkedBlockingDeque<MuServer> muServerPool =
            new LinkedBlockingDeque<>(MAX_MUSERVER_INSTANCES_SIZE);
    public Map<String, Double> aggregatedPaymentCache =
            Collections.synchronizedMap(new HashMap<String, Double>());
    public HashMap<String, Integer> currencyName2IdCache;
    public HashMap<String, Integer> clientName2IdCache;

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

        if (!initialize()) {
            System.out.println("Failed to initialize PayTracker .. ");
            throw new RuntimeException("Failed to initialize PayTracker.");
        }
        System.out.println("PayTracker successfully initialized .. ");
    };

    /**
     * Method used to clean up resoources - close connections, stop servers, etc.
     */
    public void terminate() {
        synchronized (this) {
            try {
                if (!this.dbMasterConnection.isClosed()) {
                    this.dbMasterConnection.close();
                }
                PayTracker.dbServerImpl.stop();
                PayTracker.webServerImpl.executor().close();
            } catch (Exception e) {
                System.out.println("Failed to terminate cleanly: ['" + e.getMessage() + "']. Aborting.");
            } finally {
                this.status = Status.TERMINATED;
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
            System.out.println("H2 Database console started. Login at: " + dbWebServer.getURL());
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Unexpected exception: ['" + e.getMessage() + "']. Aborting.");
            throw new RuntimeException("Failed to initialize H2 Database.");
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
                if (Status.RUNNABLE == this.status) {
                    // create builder - start server instance
                    muServerBuilder = PayTracker.MuServerBuilderInstance(executor);
                    registerHandlers(muServerBuilder);
                    MuServer muServer = muServerBuilder.start();
                    muServerPool.add(muServer);
                    System.out.println("MuServer started successfully at ['" + muServer.uri() + "']");
                    this.status = Status.RUNNING;
                } else {
                    if (Status.RUNNING == this.status) {
                        System.out.println("MuServer already running.");
                        muServerBuilder = PayTracker.webServerImpl;
                    } else {
                        throw new RuntimeException("Failed to start MuServer. Already terminated.");
                    }
                }
            } catch (Exception ex) {
                System.out.println("Unexpected exception from MuServer: ['" + ex.getMessage() + "']");
                throw new RuntimeException("Failed to start MuServer.");
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
            PayTracker.webServerImpl = startMuServer(Executors.newFixedThreadPool(PayTracker.SERVER_THREAD_POOL_SIZE));
        } catch (Exception ex) {
            System.out.println("Failed to start MuServer: ['" + ex.toString() + "']");
            return false;
        }
        try {
            PayTracker.dbServerImpl = PayTracker.startDbWebServer();
        } catch (Exception ex) {
            System.out.println("Failed to start H2 database web server: [" + ex.toString() + "']");
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
            System.out.println("Failed to start PayTracker servers");
            return false;
        }

        if (!buildCache()) {
            System.out.println("Failed to cache PayTracker data when instantiating server");
            return false;
        }

        try {
            this.dbMasterConnection = DbHelper.connect();
            System.out.println("Created master H2 database connection.");
        } catch (RuntimeException ex) {
            System.out.println("Failed to create master database connection to H2 database.");
            return false;
        }
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
            System.out.println("Unexpected exception when caching Currency and Client info: ['" + ex.getMessage() + "'].");
            return false;
        }
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
        while (Status.RUNNING == this.status) {
            try {
                if (!DbHelper.refreshAggregatedPaymentCache(this.aggregatedPaymentCache)) {
                    throw new RuntimeException("Failed to refresh payment cache.");
                }
                System.out.println("\nAggregated Payment Report as of " + LocalDateTime.now());
                System.out.println("-----------------------------------------------------------");
                int counter = 0;
                for (String ccy: this.aggregatedPaymentCache.keySet()) {
                    counter += 1;
                    Double amt = this.aggregatedPaymentCache.get(ccy);
                    System.out.println("\t#" + counter + ": CURRENCY: " + ccy + " - TOTAL PAID: " + amt);
                }
                if (0 == counter) {
                    System.out.println("\tCurrently no payments found");
                }
                Thread.sleep(REFRESH_TIME_IN_MILLIS);
                elapsedTime += REFRESH_TIME_IN_MILLIS;
                if (elapsedTime > PayTracker.MAX_PAYMENT_TRACKING_TIME_IN_MILIS) {
                    this.terminate();
                }
            } catch (InterruptedException ex) {
                // ignore
            } catch (Exception ex) {
                System.out.println("Unexpected exception while tracking payments: '" + ex.getMessage() + "'.");
                return false;
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
        System.out.println("PayTracker exiting .. ");
    }
}
