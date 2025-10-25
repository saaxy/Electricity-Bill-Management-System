package com.ebms.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.ebms.utils.HashUtil;

public class AdminDAO {
    public boolean authenticate(String username, String passwordPlain) {
        String sql = "SELECT password_hash FROM admin WHERE username = ?";
        try (Connection con = DBConnection.getConnection()) {
            if (con == null) {
                System.err.println("Cannot authenticate admin: No database connection.");
                return false;
            }
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return false;
                    String storedHash = rs.getString(1);
                    String providedHash = HashUtil.sha256(passwordPlain);
                    return storedHash != null && storedHash.equalsIgnoreCase(providedHash);
                }
            }
        } catch (SQLException e) {
            System.err.println("Admin authentication failed: " + e.getMessage());
            return false;
        }
    }

    public boolean changePassword(String username, String newPasswordPlain) {
        String sql = "UPDATE admin SET password_hash = ? WHERE username = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {
            if (con == null || ps == null) { System.err.println("Cannot change admin password: No database connection."); return false; }
            String hash = com.ebms.utils.HashUtil.sha256(newPasswordPlain);
            ps.setString(1, hash);
            ps.setString(2, username);
            int rows = ps.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Failed to change admin password: " + e.getMessage());
            return false;
        }
    }
}
