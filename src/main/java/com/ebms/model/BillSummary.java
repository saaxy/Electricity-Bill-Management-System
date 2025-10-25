package com.ebms.model;

import java.time.LocalDate;

public class BillSummary {
    private int id;
    private double amount;
    private LocalDate billingDate;
    private LocalDate dueDate;
    private String paymentStatus;
    private String razorpayOrderId;

    public BillSummary(int id, double amount, LocalDate billingDate, LocalDate dueDate, String paymentStatus, String razorpayOrderId) {
        this.id = id;
        this.amount = amount;
        this.billingDate = billingDate;
        this.dueDate = dueDate;
        this.paymentStatus = paymentStatus;
        this.razorpayOrderId = razorpayOrderId;
    }

    public int getId() { return id; }
    public double getAmount() { return amount; }
    public LocalDate getBillingDate() { return billingDate; }
    public LocalDate getDueDate() { return dueDate; }
    public String getPaymentStatus() { return paymentStatus; }
    public String getRazorpayOrderId() { return razorpayOrderId; }

    @Override
    public String toString() {
        String amt = String.format("₹%.2f", amount);
        String due = (dueDate != null) ? dueDate.toString() : "-";
        return String.format("ID:%d — %s — Due:%s — Status:%s", id, amt, due, paymentStatus!=null?paymentStatus:"PENDING");
    }
}
