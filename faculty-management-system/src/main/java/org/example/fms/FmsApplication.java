package org.example.fms;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.example.fms.core.database.DatabaseConnectionManager;
import org.example.fms.core.security.PasswordUtil;

import javax.swing.UIManager;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

/**
 * Main application runner for the MBMC Faculty Management System.
 * Uses an Embedded Tomcat server to run the full application easily via IDEs.
 */
public class FmsApplication {

    public static void main(String[] args) throws Exception {
        // 1. Initialize Database with Massive Data (Only if empty)
        try (Connection conn = DatabaseConnectionManager.getConnection()) {
            // First, always make sure the schema (including the holidays table) is
            // up-to-date
            org.example.fms.core.database.DatabaseSeeder.initSchema(conn);

            String checkSql = "SELECT COUNT(*) FROM subjects";
            try (PreparedStatement stmt = conn.prepareStatement(checkSql)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        System.out.println("No subject data found. Initializing database with dummy data...");
                        org.example.fms.core.database.DatabaseSeeder.seedDummyData();
                    } else {
                        System.out.println("Database already contains data. Skipping default seeding.");
                        // But always re-seed holidays if they are missing (separate table)
                        org.example.fms.core.database.DatabaseSeeder.seedHolidaysPublic(conn);
                    }
                }
            }
        }

        // 2. Ensure the Super Admin account exists
        seedInitialSuperAdmin();

        // 3. Start Embedded Tomcat Server
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(8080);
        tomcat.getConnector(); // Force initialization of the default connector

        // Initialize as an API-only context instead of a full webapp
        Context context = tomcat.addContext("", new File(".").getAbsolutePath());

        // Explicitly Register Servlets
        Tomcat.addServlet(context, "loginServlet", new org.example.fms.core.security.LoginServlet());
        context.addServletMappingDecoded("/api/v1/auth/login", "loginServlet");

        Tomcat.addServlet(context, "studentProfileServlet",
                new org.example.fms.core.api.servlet.StudentProfileServlet());
        context.addServletMappingDecoded("/api/v1/students", "studentProfileServlet");

        Tomcat.addServlet(context, "userRegistrationServlet",
                new org.example.fms.core.api.servlet.UserRegistrationServlet());
        context.addServletMappingDecoded("/api/v1/users", "userRegistrationServlet");

        Tomcat.addServlet(context, "dashboardDataServlet", new org.example.fms.core.api.servlet.DashboardDataServlet());
        context.addServletMappingDecoded("/api/v1/dashboard/data", "dashboardDataServlet");

        // Explicitly Register Auth Filter
        org.apache.tomcat.util.descriptor.web.FilterDef filterDef = new org.apache.tomcat.util.descriptor.web.FilterDef();
        filterDef.setFilterName("authFilter");
        filterDef.setFilterClass(org.example.fms.core.security.AuthFilter.class.getName());
        context.addFilterDef(filterDef);

        org.apache.tomcat.util.descriptor.web.FilterMap filterMap = new org.apache.tomcat.util.descriptor.web.FilterMap();
        filterMap.setFilterName("authFilter");
        filterMap.addURLPatternDecoded("/api/v1/*"); // Protect all API endpoints
        context.addFilterMap(filterMap);

        System.out.println("---------------------------------------------------------");
        System.out.println("Starting Faculty Management System...");
        System.out.println("Server running at: http://localhost:8080");
        System.out.println("Super Admin Email: admin@faculty.edu");
        System.out.println("Super Admin Password: admin");
        System.out.println("---------------------------------------------------------");

        // 4. Launch the Java Swing Desktop Client
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel("com.formdev.flatlaf.FlatLightLaf");
            } catch (Exception ex) {
                System.err.println("Could not initialize FlatLaf");
            }
            new org.example.fms.ui.LoginFrame().setVisible(true);
        });

        tomcat.start();
        tomcat.getServer().await();
    }

    /**
     * Checks if a super admin exists, and if not, injects one to allow the user to
     * log in immediately.
     */
    private static void seedInitialSuperAdmin() {
        try (Connection conn = DatabaseConnectionManager.getConnection()) {
            String checkSql = "SELECT COUNT(*) FROM users WHERE email = 'admin@faculty.edu'";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        System.out.println("No super admin found. Seeding initial admin account...");

                        String userId = "admin-uuid-001";
                        String roleId = UUID.randomUUID().toString();

                        String insertUser = "INSERT INTO users (id, email, password_hash) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE email = VALUES(email)";
                        try (PreparedStatement userStmt = conn.prepareStatement(insertUser)) {
                            userStmt.setString(1, userId);
                            userStmt.setString(2, "admin@faculty.edu");
                            userStmt.setString(3, PasswordUtil.hashPassword("admin")); // Password is 'admin'
                            userStmt.executeUpdate();
                        }

                        String insertRole = "INSERT INTO user_roles (id, user_id, role) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE role = VALUES(role)";
                        try (PreparedStatement roleStmt = conn.prepareStatement(insertRole)) {
                            roleStmt.setString(1, roleId);
                            roleStmt.setString(2, userId);
                            roleStmt.setString(3, "super_admin");
                            roleStmt.executeUpdate();
                        }
                        System.out.println("Admin seeded successfully.");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not seed admin database. Is XAMPP MySQL running?");
            e.printStackTrace();
        }
    }
}
