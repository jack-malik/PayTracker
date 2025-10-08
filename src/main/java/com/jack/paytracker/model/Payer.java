package com.jack.paytracker.model;

/*******************************************************************************
 * @project PayTracker-tmp - com.jack.paytracker.handler: 10/7/2025 @ 10:20 PM
 * @autor Jack Malik - Primechannel Corporation Ltd.
 *******************************************************************************/

public class Payer {
    public static final String SAVE_PAYER_SQL = "INSERT INTO PAYTRACKER.PAYER (NAME) VALUES (?)";
    public static final String PAYER_QUERY_ALL_SQL = "SELECT * FROM PAYTRACKER.PAYER";
    private int id = 0;
    private String name;
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(final String name) { this.name = name; }
}
