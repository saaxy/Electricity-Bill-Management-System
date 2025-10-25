package com.ebms.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ebms.dao.DBConnection;

public class SchemaInitializer {
    private static final String DEFAULT_RELATIVE_PATH = "database/schema.sql";
    private static final AtomicBoolean initialized = new AtomicBoolean(false);

    public static void ensureInitialized() {
        if (initialized.get()) return;
        // Only one thread should run initialization
        if (!initialized.compareAndSet(false, true)) return;
        try {
            String schemaPath = resolveSchemaPath();
            if (schemaPath == null) {
                System.err.println("Schema file not found (looked for 'database/schema.sql'). Skipping initialization.");
                initialized.set(false);
                return;
            }
            String content = readFile(schemaPath);
            if (content == null || content.isEmpty()) {
                System.err.println("Schema file is empty or unreadable. Skipping initialization.");
                initialized.set(false);
                return;
            }
            // Remove full-line comments starting with --
            content = content.replace("\r\n", "\n").replaceAll("(?m)^--.*$", "").trim();
            String[] statements = content.split(";");

            try (Connection con = DBConnection.getConnection()) {
                if (con == null) {
                    System.err.println("Cannot initialize schema: No database connection.");
                    initialized.set(false);
                    return;
                }
                try (Statement st = con.createStatement()) {
                    for (String stmt : statements) {
                        String s = stmt.trim();
                        if (s.isEmpty()) continue;
                        try {
                            st.execute(s);
                        } catch (SQLException se) {
                            if (isIgnorableSchemaError(se)) {
                                System.out.println("ℹ️ Skipping benign schema error: " + se.getMessage());
                                // continue with next statement
                            } else {
                                System.err.println("⚠️ Schema statement failed: " + s + " — " + se.getMessage());
                                // continue to next statement without aborting entire init
                            }
                        }
                    }
                }
                System.out.println("✅ Database schema initialized/verified from schema.sql");
            } catch (SQLException e) {
                System.err.println("Schema initialization failed: " + e.getMessage());
                initialized.set(false);
            }
        } catch (Exception e) {
            System.err.println("Schema initialization unexpected error: " + e.getMessage());
            initialized.set(false);
        }
    }

    private static boolean isIgnorableSchemaError(SQLException e) {
        // MySQL error codes for common idempotent operations
        // 1050: Table already exists
        // 1060: Duplicate column name
        // 1061: Duplicate key name (index)
        // 1062: Duplicate entry (INSERT IGNORE typically avoids this, but be tolerant)
        // 1091: Can't DROP; check that column/key exists
        int code = e.getErrorCode();
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        if (code == 1050 || code == 1060 || code == 1061 || code == 1062 || code == 1091) {
            return true;
        }
        // Fallback string checks
        return msg.contains("already exists") || msg.contains("duplicate") || msg.contains("can\u2019t drop") || msg.contains("can't drop");
    }

    private static String resolveSchemaPath() {
        // Try relative path first
        File rel = new File(DEFAULT_RELATIVE_PATH);
        if (rel.exists()) return rel.getPath();
        // Try from working directory
        String cwd = System.getProperty("user.dir");
        File alt = new File(cwd, DEFAULT_RELATIVE_PATH);
        if (alt.exists()) return alt.getPath();
        return null;
    }

    private static String readFile(String path) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        } catch (IOException e) {
            System.err.println("Failed to read schema file: " + e.getMessage());
            return null;
        }
    }
}
