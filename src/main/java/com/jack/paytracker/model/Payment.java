package com.jack.paytracker.model;

/*******************************************************************************
 * @project PayTracker-tmp - com.jack.paytracker.handler: 10/7/2025 @ 10:21 PM
 * @autor Jack Malik - Primechannel Corporation Ltd.
 *******************************************************************************/

import java.time.*;

public class Payment {
    public static final String PAYMENT_QUERY_ALL_SQL = "SELECT * FROM PAYTRACKER.PAYMENT";
    public static final String SAVE_PAYMENT_SQL = "INSERT INTO PAYTRACKER.PAYMENT (PAYER_ID, PAY_CCY_ID, PAY_AMOUNT, PAY_TIME) VALUES (?, ?, ?, ?)";
    private int id = 0;
    private int payerId = 0;
    private String currency;
    private int amount;
    private LocalDateTime timestamp;
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getPayerId() { return payerId; }
    public void setPayerId(int payerId) { this.payerId = payerId; }

    public String getCurrency() { return currency; }

    public void setCurrency(final String ccy) { this.currency = ccy; }
    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    @Override
    public String toString() { return getTimestamp() + "- PAYMENT: [PAYER=" + getPayerId() + "|CCY=" +
        getCurrency() + "|AMOUNT: " + getAmount();
    }
}
