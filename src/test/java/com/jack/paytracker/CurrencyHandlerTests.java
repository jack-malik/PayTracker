package com.jack.paytracker;

import java.util.Map;

import com.jack.paytracker.db.DbConnectionPool;
import com.jack.paytracker.handler.CurrencyHandler;
import io.muserver.MuRequest;
import io.muserver.MuResponse;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/*******************************************************************************
 * @project PayTracker - com.jack.paytracker: 10/11/2025 @ 2:59 PM
 * @autor Jack Malik - Primechannel Corporation Ltd.
 *******************************************************************************/
public class CurrencyHandlerTests {

    private TestInfo testInfo;
    private final static Logger logger = LoggerFactory.getLogger(PayTrackerTests.class);

    @Mock
    private DbConnectionPool connectionPool;

    @Mock
    private CurrencyHandler currencyHandler;


    @BeforeAll
    static void setup() {

    }

    @BeforeEach
    void init(TestInfo testInfo) {
        this.testInfo = testInfo;
    }

    @Test
    @DisplayName("Testing CurrencyHandler.handle() Method")
    void testCurrencyHandlerHandleMethod() {
        logger.info("Executing: " + testInfo.getDisplayName());
        MuRequest request;
        MuResponse response;
        Map<String,String> pathParams;
/*
        try {
            currencyHandler.handle(request, response, pathParams);
        } catch (Exception ex) {
            //
        }
 */
        // check the data type, content and status of the response object
    }
}
