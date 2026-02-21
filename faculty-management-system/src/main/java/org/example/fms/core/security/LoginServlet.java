package org.example.fms.core.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.fms.core.database.DatabaseConnectionManager;
import org.example.fms.core.util.ResponseUtil;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles user authentication and JWT token generation.
 */
@WebServlet("/api/v1/auth/login")
public class LoginServlet extends HttpServlet {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            // Parse JSON request body
            LoginRequest loginRequest = mapper.readValue(req.getInputStream(), LoginRequest.class);
            String email = loginRequest.getEmail();
            String password = loginRequest.getPassword();

            if (email == null || password == null) {
                ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "BAD_REQUEST",
                        "Email and password are required");
                return;
            }

            // Authenticate user against DB
            try (Connection conn = DatabaseConnectionManager.getConnection()) {
                String query = "SELECT u.id, u.password_hash, u.is_active, r.role " +
                        "FROM users u " +
                        "JOIN user_roles r ON u.id = r.user_id " +
                        "WHERE u.email = ? AND u.deleted_at IS NULL LIMIT 1";

                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, email);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            boolean isActive = rs.getBoolean("is_active");
                            if (!isActive) {
                                ResponseUtil.sendForbidden(resp, "Account is disabled");
                                return;
                            }

                            String hash = rs.getString("password_hash");
                            if (PasswordUtil.checkPassword(password, hash)) {
                                String userId = rs.getString("id");
                                String role = rs.getString("role");

                                // Generate JWT
                                String token = JwtUtil.generateToken(userId, role);

                                // Return successful response with token
                                Map<String, Object> data = new HashMap<>();
                                data.put("token", token);
                                data.put("role", role);
                                data.put("userId", userId);

                                ResponseUtil.sendOk(resp, data);
                                // Optional: Update last_login_at here (omitted for brevity)
                                return;
                            }
                        }
                    }
                }
            }

            // If we reach here, authentication failed
            ResponseUtil.sendUnauthorized(resp, "Invalid email or password");

        } catch (Exception e) {
            e.printStackTrace();
            ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                    "Login process failed");
        }
    }

    // Inner class to map the incoming JSON logic request
    public static class LoginRequest {
        private String email;
        private String password;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
