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
                String sql = "SELECT student_id, full_name_en, gender, program_id FROM students ORDER BY student_id DESC";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
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
                String sql = "SELECT staff_id, full_name_en, designation, department_id FROM staff ORDER BY staff_id DESC";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
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
        if (!"notices".equals(type)) {
            ResponseUtil.sendError(resp, 400, "BAD_REQUEST", "Only notices can be created via this endpoint");
            return;
        }

        try (Connection conn = DatabaseConnectionManager.getConnection()) {
            JsonNode root = mapper.readTree(req.getReader());
            String title = root.get("title").asText();
            String content = root.get("content").asText();
            String audience = root.get("target_audience").asText();
            String publisherId = (String) req.getAttribute("userId"); // Set by AuthFilter

            if (publisherId == null) {
                // Fallback for debugging if filter is bypassed in local tests
                publisherId = "admin-id";
            }

            String sql = "INSERT INTO notices (id, title, content, target_audience, published_by) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, java.util.UUID.randomUUID().toString());
                stmt.setString(2, title);
                stmt.setString(3, content);
                stmt.setString(4, audience);
                stmt.setString(5, publisherId);
                stmt.executeUpdate();
            }

            ObjectNode response = mapper.createObjectNode();
            response.put("message", "Notice created successfully");
            resp.setStatus(201);
            resp.setContentType("application/json");
            resp.getWriter().write(mapper.writeValueAsString(response));

        } catch (Exception e) {
            e.printStackTrace();
            ResponseUtil.sendError(resp, 500, "DATABASE_ERROR", "Failed to create notice");
        }
    }
}
