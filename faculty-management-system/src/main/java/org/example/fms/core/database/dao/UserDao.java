package org.example.fms.core.database.dao;

import org.example.fms.core.api.dto.UserCreateDTO;
import org.example.fms.core.database.DatabaseConnectionManager;
import org.example.fms.core.security.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class UserDao {

    /**
     * Creates a new user and assigns them a role.
     * This method handles the transaction internally.
     *
     * @param dto the User creation request object
     * @return the generated UUID string of the new user, or null if creation
     *         failed.
     */
    public String createUserWithRole(UserCreateDTO dto) throws SQLException {
        String userId = UUID.randomUUID().toString();
        String roleId = UUID.randomUUID().toString();

        String insertUserSql = "INSERT INTO users (id, email, password_hash) VALUES (?, ?, ?)";
        String insertRoleSql = "INSERT INTO user_roles (id, user_id, role) VALUES (?, ?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseConnectionManager.getConnection();
            conn.setAutoCommit(false); // Start transaction

            try (PreparedStatement userStmt = conn.prepareStatement(insertUserSql);
                    PreparedStatement roleStmt = conn.prepareStatement(insertRoleSql)) {

                // Insert into users
                userStmt.setString(1, userId);
                userStmt.setString(2, dto.getEmail());
                userStmt.setString(3, PasswordUtil.hashPassword(dto.getPlainPassword()));
                userStmt.executeUpdate();

                // Insert into user_roles
                roleStmt.setString(1, roleId);
                roleStmt.setString(2, userId);
                roleStmt.setString(3, dto.getRole());
                roleStmt.executeUpdate();

                conn.commit(); // Commit transaction
                return userId;

            } catch (SQLException e) {
                conn.rollback(); // Rollback on error
                throw e;
            }
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true); // Restore default
                conn.close();
            }
        }
    }
}
