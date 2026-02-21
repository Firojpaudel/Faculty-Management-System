package org.example.fms.core.audit;

import org.example.fms.core.database.DatabaseConnectionManager;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;

/**
 * Handles all writes to the immutable audit_logs table.
 * As per specifications, every create, update, delete, login, and logout event
 * is tracked here.
 */
public class AuditLogger {

    public enum Action {
        CREATE, UPDATE, DELETE, LOGIN, LOGOUT, LOGIN_FAILED, UNLOCK, EXPORT
    }

    /**
     * Logs an action to the audit_logs table.
     *
     * @param request      The current HTTP request to extract IP and Session info
     * @param actorId      The ID of the user performing the action (null if system
     *                     action)
     * @param actorRole    The role of the user (extracted from JWT)
     * @param action       The action type
     * @param module       The module where this occurred (e.g., "Attendance",
     *                     "Library")
     * @param resourceType The type of resource modified (e.g., "User", "Book")
     * @param resourceId   The ID of the specific resource modified
     * @param oldValueJson JSON representation of the data before the action (null
     *                     for CREATE)
     * @param newValueJson JSON representation of the data after the action (null
     *                     for DELETE)
     */
    public static void log(HttpServletRequest request, String actorId, String actorRole,
            Action action, String module, String resourceType, String resourceId,
            String oldValueJson, String newValueJson) {

        String ipAddress = request != null ? request.getRemoteAddr() : "SYSTEM";
        String userAgent = request != null ? request.getHeader("User-Agent") : "SYSTEM";
        String sessionId = request != null ? request.getSession().getId() : null;

        String query = "INSERT INTO audit_logs (id, actor_id, actor_role, action, module, " +
                "resource_type, resource_id, old_value, new_value, ip_address, device_fingerprint, session_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnectionManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, UUID.randomUUID().toString());
            stmt.setString(2, actorId);
            stmt.setString(3, actorRole);
            stmt.setString(4, action.name());
            stmt.setString(5, module);
            stmt.setString(6, resourceType);
            stmt.setString(7, resourceId);
            stmt.setString(8, oldValueJson);
            stmt.setString(9, newValueJson);
            stmt.setString(10, ipAddress);
            stmt.setString(11, userAgent);
            stmt.setString(12, sessionId);

            stmt.executeUpdate();

        } catch (Exception e) {
            // In a production system, failure to audit might require halting the main
            // transaction.
            System.err.println("Critical Error: Failed to write to audit log!");
            e.printStackTrace();
        }
    }
}
