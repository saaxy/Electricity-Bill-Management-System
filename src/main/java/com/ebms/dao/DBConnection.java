// src/main/java/com/ebms/dao/DBConnection.java
package com.ebms.dao;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    // Environment overrides for local dev without hardcoding secrets
    private static final String URL = System.getenv().getOrDefault(
        "DB_URL",
        "jdbc:mysql://localhost:3306/electricitydb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&connectTimeout=5000&socketTimeout=5000&createDatabaseIfNotExist=true"
    );
    private static final String USER = System.getenv().getOrDefault("DB_USER", "root");
    private static final String PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "123456789");

    public static Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found. Ensure the connector JAR is on the classpath.");
            return null;
        } catch (SQLException e) {
            System.err.println("Failed to establish database connection: " + e.getMessage());
            return null;
        }
    }
}