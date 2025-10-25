// src/main/java/com/ebms/dao/BillDAO.java
package com.ebms.dao;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.ebms.service.BillService;
import com.ebms.utils.PdfBillGenerator;

public class BillDAO {
    private final BillService billService = new BillService();
    private final ConsumerDAO consumerDAO = new ConsumerDAO();

    /**
     * Return the amount (INR) for a bill id, or null if not found.
     */
    public Double getAmountByBillId(int billId) {
        String sql = "SELECT amount FROM bill WHERE id = ?";
        try (java.sql.Connection con = DBConnection.getConnection();
             java.sql.PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {
            if (con == null || ps == null) { System.err.println("Cannot fetch bill amount: No DB connection"); return null; }
            ps.setInt(1, billId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (java.sql.SQLException e) {
            System.err.println("Failed to fetch bill amount: " + e.getMessage());
        }
        return null;
    }

    /**
     * Mark a bill as paid manually. Sets payment_status='SUCCESS', stores razorpay_payment_id and payment_at=NOW().
     * Returns true if a row was updated.
     */
    public boolean markBillAsPaid(int billId, String razorpayPaymentId) {
        String sql = "UPDATE bill SET payment_status = ?, razorpay_payment_id = ?, payment_at = NOW() WHERE id = ?";
        try (java.sql.Connection con = DBConnection.getConnection();
             java.sql.PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {
            if (con == null || ps == null) { System.err.println("Cannot mark bill paid: No DB connection"); return false; }
            ps.setString(1, "SUCCESS");
            ps.setString(2, razorpayPaymentId);
            ps.setInt(3, billId);
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (java.sql.SQLException e) {
            System.err.println("Failed to mark bill paid: " + e.getMessage());
            return false;
        }
    }
    /**
     * Return the consumer_id for a bill, or null if not found.
     */
    public Integer getConsumerIdByBillId(int billId) {
        String sql = "SELECT consumer_id FROM bill WHERE id = ?";
        try (java.sql.Connection con = DBConnection.getConnection();
             java.sql.PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {
            if (con == null || ps == null) { System.err.println("Cannot fetch bill consumer: No DB connection"); return null; }
            ps.setInt(1, billId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (java.sql.SQLException e) {
            System.err.println("Failed to fetch bill consumer: " + e.getMessage());
        }
        return null;
    }

    /**
     * Return unpaid bills for the given consumer. Unpaid defined as payment_status != 'SUCCESS'
     */
    public java.util.List<com.ebms.model.BillSummary> getUnpaidBillsForConsumer(int consumerId) {
        java.util.List<com.ebms.model.BillSummary> list = new java.util.ArrayList<>();
        String sql = "SELECT id, amount, billing_date, due_date, COALESCE(payment_status, 'PENDING') AS payment_status, razorpay_order_id FROM bill WHERE consumer_id = ? AND COALESCE(payment_status, 'PENDING') <> 'SUCCESS' ORDER BY billing_date DESC, id DESC";
        try (java.sql.Connection con = DBConnection.getConnection();
             java.sql.PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {
            if (con == null || ps == null) { System.err.println("Cannot fetch unpaid bills: No DB connection"); return list; }
            ps.setInt(1, consumerId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    double amt = rs.getDouble("amount");
                    java.sql.Date bd = rs.getDate("billing_date");
                    java.sql.Date dd = rs.getDate("due_date");
                    String status = rs.getString("payment_status");
                    String orderId = rs.getString("razorpay_order_id");
                    com.ebms.model.BillSummary bs = new com.ebms.model.BillSummary(id, amt, bd!=null?bd.toLocalDate():null, dd!=null?dd.toLocalDate():null, status, orderId);
                    list.add(bs);
                }
            }
        } catch (java.sql.SQLException e) {
            System.err.println("Failed to fetch unpaid bills: " + e.getMessage());
        }
        return list;
    }

    public File generateBill(int consumerId, double units) {
        // Delegate to new method that returns id and pdf; keep this method for backward compatibility
        com.ebms.model.BillResult br = generateBillReturnId(consumerId, units);
        return br != null ? br.getPdf() : null;
    }

    /**
     * Create a bill, persist it, generate the PDF and return a BillResult (id, pdf file, amount)
     */
    public com.ebms.model.BillResult generateBillReturnId(int consumerId, double units) {
        double amount = billService.calculateBill(units);
        String sql = "INSERT INTO bill(consumer_id, units, amount, billing_date, due_date) VALUES (?, ?, ?, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 15 DAY))";
        String existsSql = "SELECT 1 FROM consumer WHERE id = ?";
        try (Connection con = DBConnection.getConnection()) {
            if (con == null) {
                System.err.println("Cannot generate bill: No database connection. Check DB settings and server status.");
                return null;
            }
            // Verify consumer exists to avoid FK constraint violation
            try (PreparedStatement check = con.prepareStatement(existsSql)) {
                check.setInt(1, consumerId);
                try (ResultSet rs = check.executeQuery()) {
                    if (!rs.next()) {
                        System.err.println("Cannot generate bill: Consumer ID " + consumerId + " does not exist.");
                        return null;
                    }
                }
            }
            try (PreparedStatement ps = con.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, consumerId);
                ps.setDouble(2, units);
                ps.setDouble(3, amount);
                ps.executeUpdate();
                int generatedId = -1;
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) generatedId = keys.getInt(1);
                }
                // Print a basic summary (no tax/late/discount by default)
                double tax = 0.0, lateFee = 0.0, discount = 0.0;
                double finalAmount = billService.calculateFinalAmount(amount, tax, lateFee, discount);
                String summary = billService.generateBillSummary(consumerId, units, amount, tax, lateFee, discount, finalAmount);
                System.out.println("✅ Bill generated successfully\n" + summary);

                // Generate PDF
                var consumer = consumerDAO.getById(consumerId);
                if (consumer != null) {
                    try {
                        File pdf = PdfBillGenerator.generate(consumer, units, amount);
                        System.out.println("📄 Bill PDF saved: " + pdf.getPath());
                        return new com.ebms.model.BillResult(generatedId, pdf, amount);
                    } catch (IOException io) {
                        System.err.println("Failed to write bill PDF: " + io.getMessage());
                        return new com.ebms.model.BillResult(generatedId, null, amount);
                    }
                } else {
                    System.err.println("Cannot generate bill PDF: Consumer details not found.");
                    return new com.ebms.model.BillResult(generatedId, null, amount);
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to generate bill: " + e.getMessage());
            return null;
        }
        // Should not reach here
        
    }
}

