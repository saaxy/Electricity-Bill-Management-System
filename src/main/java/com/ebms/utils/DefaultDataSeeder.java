package com.ebms.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.ebms.dao.DBConnection;

public class DefaultDataSeeder {
    // Choose a stable, unique meter number for the default consumer
    public static final String DEFAULT_METER = "DEFAULT-0001";

    public static Integer seedDefaultConsumer() {
        // Ensure schema exists first
        SchemaInitializer.ensureInitialized();

        String findSql = "SELECT id FROM consumer WHERE meterNumber = ?";
    String insertSql = "INSERT INTO consumer(name, address, email, meterNumber, phone) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection()) {
            if (con == null) {
                System.err.println("Cannot seed default consumer: No database connection.");
                return null;
            }
            try (PreparedStatement find = con.prepareStatement(findSql)) {
                find.setString(1, DEFAULT_METER);
                try (ResultSet rs = find.executeQuery()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        System.out.println("ℹ️ Default consumer already exists. ID=" + id);
                        return id;
                    }
                }
            }
            try (PreparedStatement ins = con.prepareStatement(insertSql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                ins.setString(1, "Default Consumer");
                ins.setString(2, "N/A");
                ins.setString(3, "default@example.com");
                ins.setString(4, DEFAULT_METER);
                ins.setString(5, "0000000000");
                ins.executeUpdate();
                try (ResultSet keys = ins.getGeneratedKeys()) {
                    if (keys.next()) {
                        int id = keys.getInt(1);
                        System.out.println("✅ Default consumer created. ID=" + id);
                        return id;
                    }
                }
                System.out.println("✅ Default consumer created. (no ID returned)");
                return null;
            }
        } catch (SQLException e) {
            System.err.println("Failed to seed default consumer: " + e.getMessage());
            return null;
        }
    }

    // Optional: create a zero-units default bill for the default consumer if none exists
    public static void seedDefaultBillIfMissing(int consumerId) {
        String existsBillSql = "SELECT 1 FROM bill WHERE consumer_id = ? LIMIT 1";
        String insertBillSql = "INSERT INTO bill(consumer_id, units, amount, billing_date, due_date) " +
                "VALUES (?, 0, 0, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 15 DAY))";
        try (Connection con = DBConnection.getConnection()) {
            if (con == null) {
                System.err.println("Cannot seed default bill: No database connection.");
                return;
            }
            try (PreparedStatement chk = con.prepareStatement(existsBillSql)) {
                chk.setInt(1, consumerId);
                try (ResultSet rs = chk.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("ℹ️ Default bill already exists for consumer " + consumerId + ". Skipping.");
                        return;
                    }
                }
            }
            try (PreparedStatement ins = con.prepareStatement(insertBillSql)) {
                ins.setInt(1, consumerId);
                ins.executeUpdate();
                System.out.println("✅ Default bill created for consumer " + consumerId + ".");
            }
        } catch (SQLException e) {
            System.err.println("Failed to seed default bill: " + e.getMessage());
        }
    }
}
