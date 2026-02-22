package org.example.fms.core.api.servlet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

@WebServlet("/api/v1/dashboard/data")
public class DashboardDataServlet extends HttpServlet {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String type = req.getParameter("type");

        if (type == null) {
            ResponseUtil.sendError(resp, 400, "BAD_REQUEST", "Type parameter is required (users, students, staff)");
            return;
        }

        ArrayNode results = mapper.createArrayNode();

        try (Connection conn = DatabaseConnectionManager.getConnection()) {
            if ("users".equals(type)) {
                String sql = "SELECT u.id, u.email, r.role, DATE(u.created_at) as created FROM users u JOIN user_roles r ON u.id = r.user_id ORDER BY u.created_at DESC";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            ObjectNode node = mapper.createObjectNode();
                            node.put("id", rs.getString("id").substring(0, 8) + "...");
                            node.put("email", rs.getString("email"));
                            node.put("role", rs.getString("role").toUpperCase());
                            node.put("created_at", rs.getString("created"));
                            results.add(node);
                        }
                    }
                }
            } else if ("students".equals(type)) {
                String role = (String) req.getAttribute("userRole");
                String userId = (String) req.getAttribute("userId");
                String sql = "SELECT student_id, full_name_en, gender, program_id FROM students";

                if ("student".equalsIgnoreCase(role)) {
                    sql += " WHERE user_id = ?";
                }
                sql += " ORDER BY student_id DESC";

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    if ("student".equalsIgnoreCase(role)) {
                        stmt.setString(1, userId);
                    }
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            ObjectNode node = mapper.createObjectNode();
                            node.put("student_id",
                                    rs.getString("student_id") != null ? rs.getString("student_id") : "");
                            node.put("name", rs.getString("full_name_en") != null ? rs.getString("full_name_en") : "");
                            node.put("gender", rs.getString("gender") != null ? rs.getString("gender") : "");
                            node.put("program", rs.getString("program_id") != null ? rs.getString("program_id") : "");
                            results.add(node);
                        }
                    }
                }
            } else if ("staff".equals(type)) {
                String role = (String) req.getAttribute("userRole");
                String userId = (String) req.getAttribute("userId");
                String sql = "SELECT staff_id, full_name_en, designation, department_id FROM staff";

                if ("faculty".equalsIgnoreCase(role)) {
                    sql += " WHERE user_id = ?";
                }
                sql += " ORDER BY staff_id DESC";

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    if ("faculty".equalsIgnoreCase(role)) {
                        stmt.setString(1, userId);
                    }
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            ObjectNode node = mapper.createObjectNode();
                            node.put("staff_id", rs.getString("staff_id") != null ? rs.getString("staff_id") : "");
                            node.put("name", rs.getString("full_name_en") != null ? rs.getString("full_name_en") : "");
                            node.put("designation",
                                    rs.getString("designation") != null ? rs.getString("designation") : "");
                            node.put("department",
                                    rs.getString("department_id") != null ? rs.getString("department_id") : "");
                            results.add(node);
                        }
                    }
                }
            } else if ("subjects".equals(type)) {
                String sql = "SELECT code, name, credits, type FROM subjects ORDER BY code";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            ObjectNode node = mapper.createObjectNode();
                            node.put("code", rs.getString("code") != null ? rs.getString("code") : "");
                            node.put("name", rs.getString("name") != null ? rs.getString("name") : "");
                            node.put("credits", rs.getString("credits") != null ? rs.getString("credits") : "");
                            node.put("type", rs.getString("type") != null ? rs.getString("type") : "");
                            results.add(node);
                        }
                    }
                }
            } else if ("holidays".equals(type)) {
                String year = req.getParameter("year");
                String month = req.getParameter("month");
                String sql = "SELECT bs_day, name, description FROM holidays WHERE bs_year = ? AND bs_month = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, Integer.parseInt(year));
                    stmt.setInt(2, Integer.parseInt(month));
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            ObjectNode node = mapper.createObjectNode();
                            node.put("day", rs.getInt("bs_day"));
                            node.put("name", rs.getString("name"));
                            node.put("description", rs.getString("description"));
                            results.add(node);
                        }
                    }
                }
            } else if ("leaves".equals(type)) {
                String sql = "SELECT l.id, s.full_name_en, l.leave_type, l.start_date, l.end_date, l.status FROM leave_requests l JOIN staff s ON l.staff_id = s.id ORDER BY l.applied_on DESC";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            ObjectNode node = mapper.createObjectNode();
                            node.put("id", rs.getString("id").substring(0, 5) + "...");
                            node.put("staff_name",
                                    rs.getString("full_name_en") != null ? rs.getString("full_name_en") : "");
                            node.put("leave_type",
                                    rs.getString("leave_type") != null ? rs.getString("leave_type") : "");
                            node.put("dates", rs.getString("start_date") + " to " + rs.getString("end_date"));
                            node.put("status", rs.getString("status") != null ? rs.getString("status") : "");
                            results.add(node);
                        }
                    }
                }
            } else if ("notices".equals(type)) {
                String sql = "SELECT id, title, target_audience, DATE(published_date) as pub_date FROM notices ORDER BY published_date DESC";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            ObjectNode node = mapper.createObjectNode();
                            node.put("id", rs.getString("id").substring(0, 5) + "...");
                            node.put("title", rs.getString("title") != null ? rs.getString("title") : "");
                            node.put("audience",
                                    rs.getString("target_audience") != null ? rs.getString("target_audience") : "");
                            node.put("date", rs.getString("pub_date") != null ? rs.getString("pub_date") : "");
                            results.add(node);
                        }
                    }
                }
            } else if ("stats".equals(type)) {
                ObjectNode stats = mapper.createObjectNode();
                try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM students")) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next())
                            stats.put("students", rs.getInt(1));
                    }
                }
                try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM staff")) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next())
                            stats.put("staff", rs.getInt(1));
                    }
                }
                try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM notices")) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next())
                            stats.put("notices", rs.getInt(1));
                    }
                }
                resp.setContentType("application/json");
                resp.setCharacterEncoding("UTF-8");
                resp.getWriter().write(mapper.writeValueAsString(stats));
                return;
            } else if ("my_attendance".equals(type)) {
                String userId = (String) req.getAttribute("userId");
                String sql = "SELECT a.date, a.status FROM student_attendance a " +
                        "JOIN students s ON a.student_id = s.id " +
                        "WHERE s.user_id = ? ORDER BY a.date DESC";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, userId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            ObjectNode node = mapper.createObjectNode();
                            node.put("date", rs.getString("date"));
                            node.put("status", rs.getString("status"));
                            results.add(node);
                        }
                    }
                }
            } else if ("my_results".equals(type)) {
                String userId = (String) req.getAttribute("userId");
                String sql = "SELECT r.academic_year, sub.name as subject, r.marks_obtained, r.total_marks, r.grade, r.exam_type "
                        +
                        "FROM exam_results r " +
                        "JOIN students s ON r.student_id = s.id " +
                        "JOIN subjects sub ON r.subject_id = sub.id " +
                        "WHERE s.user_id = ? ORDER BY r.academic_year DESC";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, userId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            ObjectNode node = mapper.createObjectNode();
                            node.put("subject", rs.getString("subject"));
                            node.put("marks", rs.getDouble("marks_obtained") + "/" + rs.getDouble("total_marks"));
                            node.put("grade", rs.getString("grade"));
                            node.put("type", rs.getString("exam_type"));
                            results.add(node);
                        }
                    }
                }
            } else if ("my_subjects".equals(type)) {
                String userId = (String) req.getAttribute("userId");
                String sql = "SELECT sub.code, sub.name, sub.type, sub.credits " +
                        "FROM subjects sub " +
                        "JOIN students s ON sub.program_id = s.program_id AND sub.semester_id = (SELECT id FROM semesters WHERE program_id = s.program_id AND semester_number = s.current_semester LIMIT 1) "
                        +
                        "WHERE s.user_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, userId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            ObjectNode node = mapper.createObjectNode();
                            node.put("code", rs.getString("code"));
                            node.put("name", rs.getString("name"));
                            node.put("type", rs.getString("type"));
                            node.put("credits", rs.getInt("credits"));
                            results.add(node);
                        }
                    }
                }
            } else if ("learning_materials".equals(type)) {
                String sql = "SELECT m.title, m.material_type, sub.name as subject " +
                        "FROM learning_materials m " +
                        "JOIN subjects sub ON m.subject_id = sub.id";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            ObjectNode node = mapper.createObjectNode();
                            node.put("title", rs.getString("title"));
                            node.put("type", rs.getString("material_type"));
                            node.put("subject", rs.getString("subject"));
                            results.add(node);
                        }
                    }
                }
            } else if ("library".equals(type)) {
                String sql = "SELECT book_id, title, author, category, available_copies FROM library_books";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            ObjectNode node = mapper.createObjectNode();
                            node.put("id", rs.getString("book_id"));
                            node.put("title", rs.getString("title"));
                            node.put("author", rs.getString("author"));
                            node.put("category", rs.getString("category"));
                            node.put("available", rs.getInt("available_copies"));
                            results.add(node);
                        }
                    }
                }
            } else if ("faculty_classes".equals(type)) {
                String userId = (String) req.getAttribute("userId");
                String sql = "SELECT ca.subject_id, sub.name as subject_name, sem.name as semester " +
                        "FROM course_assignments ca " +
                        "JOIN subjects sub ON ca.subject_id = sub.id " +
                        "JOIN semesters sem ON ca.semester_id = sem.id " +
                        "JOIN staff st ON ca.staff_id = st.id " +
                        "WHERE st.user_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, userId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            ObjectNode node = mapper.createObjectNode();
                            node.put("subject_id", rs.getString("subject_id"));
                            node.put("subject_name", rs.getString("subject_name"));
                            node.put("semester", rs.getString("semester"));
                            results.add(node);
                        }
                    }
                }
            } else if ("faculty_assignments".equals(type)) {
                String userId = (String) req.getAttribute("userId");
                String sql = "SELECT a.id, a.title, sub.name as subject, a.deadline, " +
                        "(SELECT COUNT(*) FROM submissions s WHERE s.assignment_id = a.id) as sub_count " +
                        "FROM assignments a " +
                        "JOIN subjects sub ON a.subject_id = sub.id " +
                        "WHERE a.created_by = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, userId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            ObjectNode node = mapper.createObjectNode();
                            node.put("id", rs.getString("id"));
                            node.put("title", rs.getString("title"));
                            node.put("subject", rs.getString("subject"));
                            node.put("deadline", rs.getString("deadline"));
                            node.put("submissions", rs.getInt("sub_count"));
                            results.add(node);
                        }
                    }
                }
            } else if ("my_assignments".equals(type)) {
                String userId = (String) req.getAttribute("userId");
                String sql = "SELECT a.id, a.title, sub.name as subject, a.deadline, " +
                        "COALESCE(subm.status, 'pending') as status " +
                        "FROM assignments a " +
                        "JOIN subjects sub ON a.subject_id = sub.id " +
                        "JOIN students s ON sub.program_id = s.program_id AND sub.semester_id = (SELECT id FROM semesters WHERE program_id = s.program_id AND semester_number = s.current_semester LIMIT 1) "
                        +
                        "LEFT JOIN submissions subm ON a.id = subm.assignment_id AND s.id = subm.student_id " +
                        "WHERE s.user_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, userId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            ObjectNode node = mapper.createObjectNode();
                            node.put("id", rs.getString("id"));
                            node.put("title", rs.getString("title"));
                            node.put("subject", rs.getString("subject"));
                            node.put("deadline", rs.getString("deadline"));
                            node.put("status", rs.getString("status"));
                            results.add(node);
                        }
                    }
                }
            } else if ("class_students".equals(type)) {
                String subId = req.getParameter("subject_id");
                String sql = "SELECT s.id, s.student_id, s.full_name_en FROM students s " +
                        "JOIN semesters sem ON s.program_id = sem.program_id AND s.current_semester = sem.semester_number "
                        +
                        "JOIN course_assignments ca ON sem.id = ca.semester_id " +
                        "WHERE ca.subject_id = ? ORDER BY s.student_id";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, subId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            ObjectNode node = mapper.createObjectNode();
                            node.put("id", rs.getString("id"));
                            node.put("student_id", rs.getString("student_id"));
                            node.put("name", rs.getString("full_name_en"));
                            results.add(node);
                        }
                    }
                }
            } else {
                ResponseUtil.sendError(resp, 400, "BAD_REQUEST", "Unknown type");
                return;
            }

            // Write response
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(mapper.writeValueAsString(results));

        } catch (Exception e) {
            e.printStackTrace();
            ResponseUtil.sendError(resp, 500, "DATABASE_ERROR", "Failed to fetch dashboard data");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String type = req.getParameter("type");
        String userId = (String) req.getAttribute("userId");
        if (userId == null)
            userId = "admin-uuid-001";

        try (Connection conn = DatabaseConnectionManager.getConnection()) {
            JsonNode root = mapper.readTree(req.getReader());

            if ("notices".equals(type)) {
                String title = root.get("title").asText();
                String content = root.get("content").asText();
                String audience = root.get("target_audience").asText();

                String sql = "INSERT INTO notices (id, title, content, target_audience, published_by) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, java.util.UUID.randomUUID().toString());
                    stmt.setString(2, title);
                    stmt.setString(3, content);
                    stmt.setString(4, audience);
                    stmt.setString(5, userId);
                    stmt.executeUpdate();
                }
            } else if ("assignments".equals(type)) {
                String subId = root.get("subject_id").asText();
                String title = root.get("title").asText();
                String desc = root.get("description").asText();
                String deadline = root.get("deadline").asText();

                String sql = "INSERT INTO assignments (id, subject_id, title, description, deadline, created_by) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, java.util.UUID.randomUUID().toString());
                    stmt.setString(2, subId);
                    stmt.setString(3, title);
                    stmt.setString(4, desc);
                    stmt.setTimestamp(5, java.sql.Timestamp.valueOf(deadline));
                    stmt.setString(6, userId);
                    stmt.executeUpdate();
                }
            } else if ("submissions".equals(type)) {
                String assId = root.get("assignment_id").asText();
                String content = root.get("content").asText();

                String studentId = null;
                try (PreparedStatement sStmt = conn.prepareStatement("SELECT id FROM students WHERE user_id = ?")) {
                    sStmt.setString(1, userId);
                    try (ResultSet rs = sStmt.executeQuery()) {
                        if (rs.next())
                            studentId = rs.getString("id");
                    }
                }

                if (studentId == null) {
                    ResponseUtil.sendError(resp, 403, "FORBIDDEN", "Only students can submit assignments");
                    return;
                }

                String sql = "INSERT INTO submissions (id, assignment_id, student_id, content_body) VALUES (?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, java.util.UUID.randomUUID().toString());
                    stmt.setString(2, assId);
                    stmt.setString(3, studentId);
                    stmt.setString(4, content);
                    stmt.executeUpdate();
                }
            } else if ("mark_attendance".equals(type)) {
                String date = root.get("date").asText();
                JsonNode attendances = root.get("attendances");

                String sql = "INSERT INTO student_attendance (id, student_id, date, status) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE status = VALUES(status)";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    for (JsonNode att : attendances) {
                        stmt.setString(1, java.util.UUID.randomUUID().toString());
                        stmt.setString(2, att.get("student_id").asText());
                        stmt.setDate(3, java.sql.Date.valueOf(date));
                        stmt.setString(4, att.get("status").asText());
                        stmt.addBatch();
                    }
                    stmt.executeBatch();
                }
            } else {
                ResponseUtil.sendError(resp, 400, "BAD_REQUEST", "Unsupported creation type");
                return;
            }

            ObjectNode response = mapper.createObjectNode();
            response.put("message", type + " processed successfully");
            resp.setStatus(201);
            resp.setContentType("application/json");
            resp.getWriter().write(mapper.writeValueAsString(response));

        } catch (Exception e) {
            e.printStackTrace();
            ResponseUtil.sendError(resp, 500, "DATABASE_ERROR", "Failed to process request");
        }
    }
}
