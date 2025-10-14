package com.jack.paytracker;

/*******************************************************************************
 * @project PayTracker - com.jack.paytracker: 10/11/2025 @ 3:00 PM
 * @autor Jack Malik - Primechannel Corporation Ltd.
 *******************************************************************************/

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jack.paytracker.db.DbConnectionPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import com.jack.paytracker.handler.PayerHandler;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DisplayName("PayerHandler Tests")
@ExtendWith(MockitoExtension.class)
public class PayerHandlerTests {
    public static final Logger logger = LoggerFactory.getLogger(PayerHandlerTests.class);

    private TestInfo testInfo;
    @Mock
    private PayerHandler payerHandler;

    @Mock
    private DbConnectionPool connectionPool;

    @BeforeAll
    public static void setup(TestInfo testInfo) {
    }

    @BeforeEach
    public void init(TestInfo testInfo) {
        this.testInfo = testInfo;
    }

    @Test
    @DisplayName("Testing PayerHandler.handle() Method")
    void testPayerHandlerHandleMethod() {
        logger.info("Executing: " + testInfo.getDisplayName());
    }
}
