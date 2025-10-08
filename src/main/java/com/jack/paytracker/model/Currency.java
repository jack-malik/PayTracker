package com.jack.paytracker.model;

/*******************************************************************************
 * @project com.jack.paytracker - com.jack.paytracker.handler: 10/7/2025 @ 11:25 PM
 * @autor Jack Malik - Primechannel Corporation Ltd.
 *******************************************************************************/

public class Currency {
    public static final String CURRENCY_QUERY_ALL_SQL = "SELECT * FROM PAYTRACKER.CURRENCY";
    static final String SAVE_CURRENCY_SQL = "INSERT INTO PAYTRACKER.CURRENCY (CCY_CD) VALUES (?)";
    private int id = 0;
    private String ccy_code;
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getCcyCode() { return ccy_code; }
    public void setCcyCode(final String code) { this.ccy_code = code; }
}
