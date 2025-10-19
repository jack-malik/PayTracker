package com.jack.paytracker.model;

/*******************************************************************************
 * @project PayTracker-tmp - com.jack.paytracker.handler: 10/7/2025 @ 10:21 PM
 * @autor Jack Malik - Primechannel Corporation Ltd.
 *******************************************************************************/

import java.time.*;
import java.time.temporal.ChronoUnit;

public class Payment {
    public static final String PAYMENT_QUERY_ALL_SQL = "SELECT * FROM PAYTRACKER.PAYMENT";
    public static final String SAVE_PAYMENT_SQL = "INSERT INTO PAYTRACKER.PAYMENT (PAYER_ID, PAY_CCY_ID, " +
            "PAY_AMOUNT, PAY_TIME, CREATE_TIME) VALUES (?,?,?,?,?)";
    private int id = 0;
    private int payerId = 0;
    private int currencyId;
    private Double amount;
    private LocalDateTime payTime;
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getPayerId() { return payerId; }
    public void setPayerId(int payerId) { this.payerId = payerId; }

    public int getCurrency() { return currencyId; }

    public void setCurrencyId(final int ccyId) { this.currencyId = ccyId; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public LocalDateTime getTimestamp() { return payTime; }
    public void setPayTime(LocalDateTime timestamp) { this.payTime = timestamp.truncatedTo(ChronoUnit.SECONDS); }
    @Override
    public String toString() { return getTimestamp() + "- PAYMENT: [PAYER=" + getPayerId() + "|CCY=" +
        getCurrency() + "|AMOUNT: " + getAmount();
    }
}
