package com.ebms.model;

import java.io.File;

public class BillResult {
    private final int id;
    private final File pdf;
    private final double amount;

    public BillResult(int id, File pdf, double amount) {
        this.id = id;
        this.pdf = pdf;
        this.amount = amount;
    }

    public int getId() { return id; }
    public File getPdf() { return pdf; }
    public double getAmount() { return amount; }
}
