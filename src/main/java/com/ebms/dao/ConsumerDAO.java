
// src/main/java/com/ebms/dao/ConsumerDAO.java
package com.ebms.dao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.ebms.model.Consumer;
import com.ebms.utils.SchemaInitializer;

public class ConsumerDAO {
    public int addConsumer(Consumer consumer) {
        // Ensure schema is present before we attempt inserts
        SchemaInitializer.ensureInitialized();
        
        if (isConsumerExists(consumer)) {
            System.out.println("⚠️ Consumer already exists! Not added.");
            return -1;
        }
    String sql = "INSERT INTO consumer(name, address, email, meterNumber, phone, birthdate, passcode) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection()) {
            if (con == null) {
                System.err.println("Cannot add consumer: No database connection. Check DB settings and server status.");
                return -1;
            }
            try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, consumer.getName());
            ps.setString(2, consumer.getAddress());
            ps.setString(3, consumer.getEmail());
            ps.setString(4, consumer.getMeterNumber());
            ps.setString(5, consumer.getPhone());
            java.time.LocalDate bd = consumer.getBirthdate() != null ? consumer.getBirthdate() : java.time.LocalDate.of(1999,1,1);
            ps.setDate(6, java.sql.Date.valueOf(bd));
            String pass = consumer.getPasscode();
            if (pass == null || pass.isBlank()) {
                pass = String.format("%02d%02d%02d", bd.getDayOfMonth(), bd.getMonthValue(), bd.getYear() % 100);
            }
            ps.setString(7, pass);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    consumer.setId(id);
                    System.out.println("✅ Consumer added successfully! ID=" + id);
                    return id;
                }
            }
            System.out.println("✅ Consumer added successfully! (no ID returned)");
            }
        } catch (SQLException e) {
            System.err.println("Failed to add consumer: " + e.getMessage());
        }
        return -1;
    }

    public boolean authenticate(int id, String password) {
        String sql = "SELECT passcode FROM consumer WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {
            if (con == null || ps == null) { System.err.println("Cannot authenticate: No database connection."); return false; }
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String current = rs.getString(1);
                    return current != null && current.equals(password);
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to authenticate consumer: " + e.getMessage());
        }
        return false;
    }

    public boolean changePassword(int id, String newPassword) {
        String sql = "UPDATE consumer SET passcode = ? WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {
            if (con == null || ps == null) { System.err.println("Cannot change password: No database connection."); return false; }
            ps.setString(1, newPassword);
            ps.setInt(2, id);
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Failed to change consumer password: " + e.getMessage());
            return false;
        }
    }

    public List<Consumer> getAllConsumers() {
        List<Consumer> list = new ArrayList<>();
    String sql = "SELECT * FROM consumer";
        try (Connection con = DBConnection.getConnection()) {
            if (con == null) {
                System.err.println("Cannot fetch consumers: No database connection. Check DB settings and server status.");
                return list;
            }
            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
            // Determine optional columns
            java.sql.ResultSetMetaData md = rs.getMetaData();
            int colCount = md.getColumnCount();
            boolean hasBirthdate = false, hasPasscode = false, hasPhone = false;
            for (int i = 1; i <= colCount; i++) {
                String col = md.getColumnLabel(i);
                if ("birthdate".equalsIgnoreCase(col)) hasBirthdate = true;
                if ("passcode".equalsIgnoreCase(col)) hasPasscode = true;
                if ("phone".equalsIgnoreCase(col)) hasPhone = true;
            }

            while (rs.next()) {
                Consumer c = new Consumer();
                c.setId(rs.getInt("id"));
                c.setName(rs.getString("name"));
                c.setAddress(rs.getString("address"));
                c.setEmail(rs.getString("email"));
                c.setMeterNumber(rs.getString("meterNumber"));
                if (hasBirthdate) {
                    java.sql.Date bd = rs.getDate("birthdate");
                    if (bd != null) c.setBirthdate(bd.toLocalDate());
                }
                if (hasPasscode) {
                    c.setPasscode(rs.getString("passcode"));
                }
                if (hasPhone) {
                    c.setPhone(rs.getString("phone"));
                }
                list.add(c);
            }
            }
        } catch (SQLException e) {
            System.err.println("Failed to fetch consumers: " + e.getMessage());
        }
        return list;
    }

    public Consumer getById(int id) {
    String sql = "SELECT * FROM consumer WHERE id = ?";
        try (Connection con = DBConnection.getConnection()) {
            if (con == null) {
                System.err.println("Cannot fetch consumer: No database connection.");
                return null;
            }
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Consumer c = new Consumer();
                        c.setId(rs.getInt("id"));
                        c.setName(rs.getString("name"));
                        c.setAddress(rs.getString("address"));
                        c.setEmail(rs.getString("email"));
                        c.setMeterNumber(rs.getString("meterNumber"));
                        java.sql.ResultSetMetaData md = rs.getMetaData();
                        int colCount = md.getColumnCount();
                        boolean hasBirthdate = false, hasPasscode = false, hasPhone = false;
                        for (int i = 1; i <= colCount; i++) {
                            String col = md.getColumnLabel(i);
                            if ("birthdate".equalsIgnoreCase(col)) hasBirthdate = true;
                            if ("passcode".equalsIgnoreCase(col)) hasPasscode = true;
                            if ("phone".equalsIgnoreCase(col)) hasPhone = true;
                        }
                        if (hasBirthdate) {
                            java.sql.Date bd = rs.getDate("birthdate");
                            if (bd != null) c.setBirthdate(bd.toLocalDate());
                        }
                        if (hasPasscode) {
                            c.setPasscode(rs.getString("passcode"));
                        }
                        if (hasPhone) {
                            c.setPhone(rs.getString("phone"));
                        }
                        return c;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to fetch consumer by id: " + e.getMessage());
        }
        return null;
    }

    public boolean deleteById(int id) {
        String sql = "DELETE FROM consumer WHERE id = ?";
        try (Connection con = DBConnection.getConnection()) {
            if (con == null) {
                System.err.println("Cannot delete consumer: No database connection.");
                return false;
            }
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, id);
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    System.out.println("✅ Consumer deleted (and related bills via cascade if any). ID=" + id);
                    return true;
                } else {
                    System.out.println("ℹ️ No consumer found with ID=" + id);
                    return false;
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to delete consumer: " + e.getMessage());
            return false;
        }
    }

    public boolean update(Consumer consumer) {
    String sql = "UPDATE consumer SET name = ?, address = ?, email = ?, meterNumber = ?, phone = ?, " +
        "birthdate = IFNULL(?, birthdate), passcode = IFNULL(?, passcode) WHERE id = ?";
        try (Connection con = DBConnection.getConnection()) {
            if (con == null) {
                System.err.println("Cannot update consumer: No database connection.");
                return false;
            }
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, consumer.getName());
                ps.setString(2, consumer.getAddress());
                ps.setString(3, consumer.getEmail());
                ps.setString(4, consumer.getMeterNumber());
                ps.setString(5, consumer.getPhone());
                // Birthdate to set (nullable -> keep existing)
                java.sql.Date bdSql = null;
                if (consumer.getBirthdate() != null) {
                    bdSql = java.sql.Date.valueOf(consumer.getBirthdate());
                }
                if (bdSql != null) {
                    ps.setDate(6, bdSql);
                } else {
                    ps.setNull(6, java.sql.Types.DATE);
                }
                // Passcode: if provided use it; else if birthdate provided compute DDMMYY; else keep existing
                String pass = consumer.getPasscode();
                if ((pass == null || pass.isBlank()) && consumer.getBirthdate() != null) {
                    var bd = consumer.getBirthdate();
                    pass = String.format("%02d%02d%02d", bd.getDayOfMonth(), bd.getMonthValue(), bd.getYear() % 100);
                }
                if (pass != null && !pass.isBlank()) {
                    ps.setString(7, pass);
                } else {
                    ps.setNull(7, java.sql.Types.VARCHAR);
                }
                ps.setInt(8, consumer.getId());
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    System.out.println("✅ Consumer updated. ID=" + consumer.getId());
                    return true;
                } else {
                    System.out.println("ℹ️ No consumer found with ID=" + consumer.getId());
                    return false;
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to update consumer: " + e.getMessage());
            return false;
        }
    }
    public boolean isConsumerExists(Consumer consumer) {
        // Check by presumed unique key: meterNumber
        String sql = "SELECT COUNT(*) FROM consumer WHERE meterNumber = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, consumer.getMeterNumber());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0; // exists if count > 0
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to check consumer existence: " + e.getMessage());
        }
        return false;
    }
    
}