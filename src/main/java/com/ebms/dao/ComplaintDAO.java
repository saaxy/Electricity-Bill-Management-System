package com.ebms.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.ebms.model.Complaint;
import com.ebms.utils.SchemaInitializer;

public class ComplaintDAO {

    public int addComplaint(Complaint complaint) {
        // Ensure schema is present
        SchemaInitializer.ensureInitialized();

        String sql = "INSERT INTO complaint(description, status) VALUES(?, ?)";
        try (Connection con = DBConnection.getConnection()) {
            if (con == null) {
                System.err.println("Cannot add complaint: No database connection.");
                return -1;
            }
            try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, complaint.getDescription());
                String status = complaint.getStatus();
                if (status == null || status.isBlank()) status = "OPEN";
                ps.setString(2, status);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        complaint.setId(id);
                        System.out.println("✅ Complaint recorded. ID=" + id);
                        return id;
                    }
                }
                System.out.println("✅ Complaint recorded (no ID returned)");
            }
        } catch (SQLException e) {
            System.err.println("Failed to add complaint: " + e.getMessage());
        }
        return -1;
    }

    public Complaint getById(int id) {
        String sql = "SELECT id, description, status FROM complaint WHERE id = ?";
        try (Connection con = DBConnection.getConnection()) {
            if (con == null) { System.err.println("Cannot fetch complaint: No DB connection."); return null; }
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new Complaint(
                                rs.getInt("id"),
                                rs.getString("description"),
                                rs.getString("status")
                        );
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to fetch complaint: " + e.getMessage());
        }
        return null;
    }

    public List<Complaint> getAll() {
        List<Complaint> list = new ArrayList<>();
        String sql = "SELECT id, description, status FROM complaint ORDER BY id DESC";
        try (Connection con = DBConnection.getConnection();
             Statement st = con != null ? con.createStatement() : null) {
            if (con == null || st == null) { System.err.println("Cannot fetch complaints: No DB connection."); return list; }
            try (ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    list.add(new Complaint(
                            rs.getInt("id"),
                            rs.getString("description"),
                            rs.getString("status")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to fetch complaints: " + e.getMessage());
        }
        return list;
    }

    public boolean updateStatus(int id, String status) {
        String sql = "UPDATE complaint SET status = ? WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {
            if (con == null || ps == null) { System.err.println("Cannot update complaint: No DB connection."); return false; }
            ps.setString(1, status);
            ps.setInt(2, id);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ Complaint status updated. ID=" + id + ", status=" + status);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Failed to update complaint status: " + e.getMessage());
        }
        return false;
    }
}
