package com.ebms.service;

public class BillService {
    public double calculateBill(double units) {
        if (units <= 100) return units * 3.0;
        else if (units <= 300) return (100 * 3.0) + (units - 100) * 4.5;
        else return (100 * 3.0) + (200 * 4.5) + (units - 300) * 6.0;
    }
    public double calculateLateFee(double amount, int daysLate) {
        if (daysLate <= 0) return 0.0;
        return amount * 0.02 * daysLate; // 2% per day late fee
    }
    public double calculateTotalAmount(double amount, double lateFee) {
        return amount + lateFee;
    }
    public double applyDiscount(double amount, double discountPercent) {
        if (discountPercent <= 0 || discountPercent > 100) return amount;
        return amount - (amount * (discountPercent / 100.0));
    }
    public double calculateTax(double amount, double taxPercent) {
        if (taxPercent <= 0) return 0.0;
        return amount * (taxPercent / 100.0);
    }
    public double calculateFinalAmount(double amount, double tax, double lateFee, double discount) {
        return amount + tax + lateFee - discount;
    }
    public String formatCurrency(double amount) {
        return String.format("₹%.2f", amount);
    }
    public String generateBillSummary(int consumerId, double units, double amount, double tax, double lateFee, double discount, double finalAmount) {
        StringBuilder sb = new StringBuilder();
        sb.append("Bill Summary for Consumer ID: ").append(consumerId).append("\n");
        sb.append("Units Consumed: ").append(units).append(" units\n");
        sb.append("Base Amount: ").append(formatCurrency(amount)).append("\n");
        sb.append("Tax: ").append(formatCurrency(tax)).append("\n");
        sb.append("Late Fee: ").append(formatCurrency(lateFee)).append("\n");
        sb.append("Discount: ").append(formatCurrency(discount)).append("\n");
        sb.append("Final Amount Payable: ").append(formatCurrency(finalAmount)).append("\n");
        return sb.toString();
    }    
}
