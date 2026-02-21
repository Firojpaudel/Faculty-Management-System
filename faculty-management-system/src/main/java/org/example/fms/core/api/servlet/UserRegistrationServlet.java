package org.example.fms.core.api.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.fms.core.api.dto.UserCreateDTO;
import org.example.fms.core.audit.AuditLogger;
import org.example.fms.core.database.dao.UserDao;
import org.example.fms.core.util.ResponseUtil;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Endpoint for creating new user accounts.
 * Protected by AuthFilter. Requires 'super_admin' role (enforced here).
 */
@WebServlet("/api/v1/users")
public class UserRegistrationServlet extends HttpServlet {

    private final ObjectMapper mapper = new ObjectMapper();
    private final UserDao userDao = new UserDao();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String actorId = (String) req.getAttribute("userId");
        String actorRole = (String) req.getAttribute("userRole");

        // 1. RBAC Check: Only Super Admin can create arbitrary accounts directly
        if (!"super_admin".equals(actorRole)) {
            ResponseUtil.sendForbidden(resp, "Insufficient permissions to create user accounts.");
            return;
        }

        try {
            // 2. Parse payload
            UserCreateDTO dto = mapper.readValue(req.getInputStream(), UserCreateDTO.class);

            // Basic Validation
            if (dto.getEmail() == null || dto.getPlainPassword() == null || dto.getRole() == null) {
                ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_ERROR",
                        "Email, password, and role are required");
                return;
            }

            if (dto.getPlainPassword().length() < 8) {
                ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_ERROR",
                        "Password must be at least 8 characters");
                return;
            }

            // 3. Create User in Database
            String newUserId = userDao.createUserWithRole(dto);

            // 4. Log Action to Audit Trail
            // Hide password hash before auditing
            dto.setPlainPassword("[REDACTED]");
            AuditLogger.log(req, actorId, actorRole, AuditLogger.Action.CREATE, "Core", "User", newUserId, null,
                    mapper.writeValueAsString(dto));

            // 5. Build Response
            Map<String, String> data = new HashMap<>();
            data.put("id", newUserId);
            data.put("email", dto.getEmail());
            data.put("role", dto.getRole());

            ResponseUtil.sendCreated(resp, data);

        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate") || e.getMessage().contains("UNIQUE")) {
                ResponseUtil.sendError(resp, HttpServletResponse.SC_CONFLICT, "CONFLICT",
                        "A user with this email already exists.");
            } else {
                e.printStackTrace();
                ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "DATABASE_ERROR",
                        "Failed to create user.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "BAD_REQUEST", "Invalid request payload");
        }
    }
}
