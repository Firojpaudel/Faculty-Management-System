package org.example.fms.core.api.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.fms.core.api.dto.StudentProfileDTO;
import org.example.fms.core.api.dto.UserCreateDTO;
import org.example.fms.core.audit.AuditLogger;
import org.example.fms.core.database.dao.StudentDao;
import org.example.fms.core.database.dao.UserDao;
import org.example.fms.core.util.ResponseUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handle student application and enrollment processes.
 * RBAC: 'admission_officer' or 'super_admin'
 */
@WebServlet("/api/v1/students")
public class StudentProfileServlet extends HttpServlet {

    private final ObjectMapper mapper = new ObjectMapper();
    private final StudentDao studentDao = new StudentDao();
    private final UserDao userDao = new UserDao();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String actorId = (String) req.getAttribute("userId");
        String actorRole = (String) req.getAttribute("userRole");

        // 1. RBAC Check: Admission Officer or Super Admin
        if (!"admission_officer".equals(actorRole) && !"super_admin".equals(actorRole)) {
            ResponseUtil.sendForbidden(resp, "Insufficient permissions. Only Admission Officers can enroll students.");
            return;
        }

        try {
            // 2. Parse Profile Payload
            StudentProfileDTO dto = mapper.readValue(req.getInputStream(), StudentProfileDTO.class);

            // Mandatory Field Validation (simplified for Phase 1)
            if (dto.getFullNameEn() == null || dto.getDateOfBirth() == null || dto.getStudentId() == null) {
                ResponseUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "VALIDATION_ERROR",
                        "Full Name (EN), Date of Birth, and Student ID are required");
                return;
            }

            // 3. Auto-Provision User Login Account
            // In a real system, the initial password might be randomly generated and
            // emailed.
            // Here we use a standard default logic for the assignment.
            UserCreateDTO userDto = new UserCreateDTO();
            // Default email logic: fallback to studentId@mbmc.edu.np if no email provided
            String studentEmail = dto.getEmail() != null ? dto.getEmail()
                    : dto.getStudentId().toLowerCase() + "@mbmc.edu.np";
            userDto.setEmail(studentEmail);
            userDto.setPlainPassword(dto.getStudentId() + "123!"); // Default temporary password
            userDto.setRole("student");

            String newUserId = userDao.createUserWithRole(userDto);

            // 4. Create Student Profile Record tied to the new User account
            String newStudentId = studentDao.createStudentProfile(newUserId, dto);

            // 5. Audit Logging (Audit both actions)
            userDto.setPlainPassword("[REDACTED]");
            AuditLogger.log(req, actorId, actorRole, AuditLogger.Action.CREATE, "Core", "User", newUserId, null,
                    mapper.writeValueAsString(userDto));
            AuditLogger.log(req, actorId, actorRole, AuditLogger.Action.CREATE, "Academic", "Student", newStudentId,
                    null, mapper.writeValueAsString(dto));

            // 6. Response
            Map<String, String> data = new HashMap<>();
            data.put("id", newStudentId);
            data.put("student_id", dto.getStudentId());
            data.put("user_id_created", newUserId);

            ResponseUtil.sendCreated(resp, data);

        } catch (Exception e) {
            e.printStackTrace();
            ResponseUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                    "Failed to enroll student profile: " + e.getMessage());
        }
    }
}
