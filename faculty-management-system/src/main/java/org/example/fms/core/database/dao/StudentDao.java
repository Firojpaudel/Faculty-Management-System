package org.example.fms.core.database.dao;

import org.example.fms.core.api.dto.StudentProfileDTO;
import org.example.fms.core.database.DatabaseConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class StudentDao {

    /**
     * Inserts a new student profile into the system.
     * Requires the corresponding User ID to link the accounts.
     *
     * @param userId The UUID of the authenticated user account for this student.
     * @param dto    The profile details
     * @return The UUID of the created student record.
     */
    public String createStudentProfile(String userId, StudentProfileDTO dto) throws SQLException {
        String studentId = UUID.randomUUID().toString();

        String sql = "INSERT INTO students (id, student_id, user_id, full_name_en, full_name_np, " +
                "date_of_birth, gender, nationality, ethnicity, address, phone, email, " +
                "guardian_name, guardian_phone, guardian_relationship) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnectionManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, studentId);
            stmt.setString(2, dto.getStudentId());
            stmt.setString(3, userId);
            stmt.setString(4, dto.getFullNameEn());
            stmt.setString(5, dto.getFullNameNp());
            stmt.setDate(6, dto.getDateOfBirth());
            stmt.setString(7, dto.getGender());
            stmt.setString(8, dto.getNationality());
            stmt.setString(9, dto.getEthnicity());
            stmt.setString(10, dto.getAddress());
            stmt.setString(11, dto.getPhone());
            stmt.setString(12, dto.getEmail());
            stmt.setString(13, dto.getGuardianName());
            stmt.setString(14, dto.getGuardianPhone());
            stmt.setString(15, dto.getGuardianRelationship());

            // For Phase 1 we are leaving academic fields (program_id, batch, etc.) null on
            // creation
            // They will be updated via a separate endpoint later, handled by the Admission
            // Officer role.

            stmt.executeUpdate();
            return studentId;
        }
    }
}
