package org.example.fms.core.database;

import org.example.fms.core.security.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Scanner;
import java.util.UUID;

public class DatabaseSeeder {

    // Predetermined UUIDs for consistent dummy data linking
    private static final String DEPT_CS = "d1b1f2a0-0000-0000-0000-000000000001";
    private static final String DEPT_BA = "d1b1f2a0-0000-0000-0000-000000000002";

    public static void seedDummyData() {
        try (Connection conn = DatabaseConnectionManager.getConnection()) {
            // 0. Initialize Schema (Ensure tables exist)
            initializeSchema(conn);

            try (java.sql.Statement stmt = conn.createStatement()) {
                stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
            }

            // Optional: Uncomment the line below to wipe existing data for a fresh start
            clearDatabase(conn);

            // 1. Seed Departments (Always ensure core IDs exist)
            String sqlDept = "INSERT IGNORE INTO departments (id, name) VALUES (?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sqlDept)) {
                stmt.setString(1, DEPT_CS);
                stmt.setString(2, "Computer Science");
                stmt.addBatch();
                stmt.setString(1, DEPT_BA);
                stmt.setString(2, "Business Administration");
                stmt.addBatch();
                stmt.executeBatch();
            }

            // 2. Seed Programs (Always ensure major program IDs exist)
            String sqlProg = "INSERT IGNORE INTO programs (id, name, code, department_id, duration_years) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sqlProg)) {
                addProgram(stmt, "p-csit", "BSc Computer Science & Information Technology", "CSIT", DEPT_CS, 4.0);
                addProgram(stmt, "p-bca", "Bachelor of Computer Application", "BCA", DEPT_CS, 4.0);
                addProgram(stmt, "p-bbm", "Bachelor of Business Management", "BBM", DEPT_BA, 4.0);
                addProgram(stmt, "p-bbs", "Bachelor of Business Studies", "BBS", DEPT_BA, 4.0);
                stmt.executeBatch();
            }

            // 3. Seed Students & Student Users (Massive Expansion)
            if (isTableEmpty(conn, "students")) {
                System.out.println("Seeding massive student data (4 programs, 8 semesters each)...");
                String[] firstNames = { "Ram", "Sita", "Hari", "Gita", "Shyam", "Rita", "Aayush", "Anjali", "Bikash",
                        "Binita", "Sandeep", "Manisha", "Kiran", "Samir", "Pooja" };
                String[] lastNames = { "Sharma", "Thapa", "Mahat", "Gurung", "Rai", "Karki", "Adhikari", "Poudel",
                        "Pandey", "Basnet", "Lama", "Sherpa", "Tamang", "Magar", "Ghale" };
                String[] programs = { "p-csit", "p-bca", "p-bbm", "p-bbs" };

                for (String progId : programs) {
                    for (int sem = 1; sem <= 8; sem++) {
                        for (int i = 1; i <= 5; i++) { // 5 Students per semester
                            int seed = (progId + sem + i).hashCode() & 0x7FFFFFFF;
                            String fn = firstNames[seed % firstNames.length];
                            String ln = lastNames[seed % lastNames.length];
                            String studentId = "STU-" + progId.substring(2).toUpperCase() + "-" + sem + "-"
                                    + String.format("%03d", i);
                            String email = fn.toLowerCase() + "." + ln.toLowerCase() + "." + (seed % 999)
                                    + "@faculty.edu";
                            addStudentUser(conn, studentId, fn + " " + ln, email, progId, sem);
                        }
                    }
                }
            }

            // 4. Seed Staff
            if (isTableEmpty(conn, "staff")) {
                System.out.println("Seeding dummy staff...");
                String userId = UUID.randomUUID().toString();
                String staffId = UUID.randomUUID().toString();

                String sqlUser = "INSERT INTO users (id, email, password_hash) VALUES (?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(sqlUser)) {
                    stmt.setString(1, userId);
                    stmt.setString(2, "teacher@faculty.edu");
                    stmt.setString(3, PasswordUtil.hashPassword("teacher"));
                    stmt.executeUpdate();
                }

                String sqlRole = "INSERT INTO user_roles (id, user_id, role) VALUES (?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(sqlRole)) {
                    stmt.setString(1, UUID.randomUUID().toString());
                    stmt.setString(2, userId);
                    stmt.setString(3, "faculty");
                    stmt.executeUpdate();
                }

                String sql = "INSERT INTO staff (id, staff_id, user_id, full_name_en, designation, staff_type, department_id, employment_type, date_of_joining) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, staffId);
                    stmt.setString(2, "EMP-001");
                    stmt.setString(3, userId);
                    stmt.setString(4, "Dr. Shyam Nepal");
                    stmt.setString(5, "Professor");
                    stmt.setString(6, "teaching");
                    stmt.setString(7, DEPT_CS);
                    stmt.setString(8, "permanent");
                    stmt.setString(9, "2020-01-01");
                    stmt.executeUpdate();
                }
            }

            // 5. Seed Semesters (Massive Expansion)
            if (isTableEmpty(conn, "semesters")) {
                System.out.println("Seeding all semesters for all programs...");
                String sql = "INSERT INTO semesters (id, program_id, semester_number, name) VALUES (?, ?, ?, ?)";
                String[] programs = { "p-csit", "p-bca", "p-bbm", "p-bbs" };
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    for (String progId : programs) {
                        for (int i = 1; i <= 8; i++) {
                            stmt.setString(1, "sem-" + progId.substring(2) + "-" + i);
                            stmt.setString(2, progId);
                            stmt.setInt(3, i);
                            stmt.setString(4, "Semester " + i);
                            stmt.addBatch();
                        }
                    }
                    stmt.executeBatch();
                }
            }

            // 6. Seed Subjects (Massive Expansion)
            if (isTableEmpty(conn, "subjects")) {
                System.out.println("Seeding subjects for all programs...");
                String sql = "INSERT INTO subjects (id, code, name, credits, semester_id, type) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    String[] progs = { "csit", "bca", "bbm", "bbs" };
                    for (String p : progs) {
                        for (int s = 1; s <= 8; s++) {
                            String code01 = p.toUpperCase() + "-" + s + "01";
                            String code02 = p.toUpperCase() + "-" + s + "02";
                            // Use code as ID for easy linking in seeder
                            addSubject(stmt, code01, code01, "Core Topic " + (s * 10 + 1), 3, "sem-" + p + "-" + s,
                                    "theory");
                            addSubject(stmt, code02, code02, "Advanced Study " + (s * 10 + 2), 3, "sem-" + p + "-" + s,
                                    "both");
                        }
                    }
                    stmt.executeBatch();
                }
            }

            // 7. Seed Library Books (60+ legit entries)
            if (isTableEmpty(conn, "library_books")) {
                System.out.println("Seeding massive library catalog...");
                String sql = "INSERT INTO library_books (id, book_id, title, author, publisher, year_of_publication, category, total_copies, shelf_location_code) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    String[] cats = { "Computing", "Management", "Mathematics", "Science", "Literature" };
                    for (int i = 1; i <= 65; i++) {
                        String cat = cats[i % cats.length];
                        addBook(stmt, "LIB-B" + (1000 + i), "Academic Resource Vol " + i, "Author " + (i % 12),
                                "University Press", 2012 + (i % 10), cat, 10,
                                cat.substring(0, 2).toUpperCase() + "-" + (i % 10));
                    }
                    stmt.executeBatch();
                }
            }

            // 8. Seed Course Assignments (Dr. Shyam Nepal teaching CSIT Sem 1 & 2)
            if (isTableEmpty(conn, "course_assignments")) {
                System.out.println("Seeding course assignments...");
                String staffId = null;
                try (PreparedStatement sStmt = conn
                        .prepareStatement("SELECT id FROM staff WHERE full_name_en = 'Dr. Shyam Nepal'")) {
                    try (ResultSet rs = sStmt.executeQuery()) {
                        if (rs.next())
                            staffId = rs.getString("id");
                    }
                }
                if (staffId != null) {
                    String sql = "INSERT INTO course_assignments (id, staff_id, subject_id, academic_year, semester_id, assigned_date) VALUES (?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        addAssignment(stmt, staffId, "CSIT-101", "2026", "sem-csit-1");
                        addAssignment(stmt, staffId, "CSIT-201", "2026", "sem-csit-2");
                        stmt.executeBatch();
                    }
                }
            }

            // 6. Seed Notices (Linked to fixed admin ID)
            if (isTableEmpty(conn, "notices")) {
                System.out.println("Seeding dummy notices...");
                String adminId = "admin-uuid-001";
                String sql = "INSERT INTO notices (id, title, content, target_audience, published_by) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, UUID.randomUUID().toString());
                    stmt.setString(2, "Welcome to the New System");
                    stmt.setString(3,
                            "We have successfully rolled out the Phase 1 Desktop Client for the Faculty Management System.");
                    stmt.setString(4, "all");
                    stmt.setString(5, adminId);
                    stmt.addBatch();

                    stmt.setString(1, UUID.randomUUID().toString());
                    stmt.setString(2, "Semester Exams Notice");
                    stmt.setString(3,
                            "Please submit the final exam question papers to the Examination Controller by Friday.");
                    stmt.setString(4, "staff");
                    stmt.setString(5, adminId);
                    stmt.addBatch();

                    stmt.executeBatch();
                }
            }

            // 9. Seed Student Attendance & Results
            if (isTableEmpty(conn, "student_attendance")) {
                System.out.println("Seeding student attendance and results...");
                try (PreparedStatement sStmt = conn.prepareStatement("SELECT id FROM students")) {
                    try (ResultSet rs = sStmt.executeQuery()) {
                        while (rs.next()) {
                            String studentId = rs.getString("id");
                            seedStudentPerformance(conn, studentId);
                        }
                    }
                }
            }

            // 10. Seed Learning Materials
            if (isTableEmpty(conn, "learning_materials")) {
                System.out.println("Seeding learning materials...");
                String adminId = "admin-uuid-001";
                String sql = "INSERT INTO learning_materials (id, subject_id, title, material_type, published_by) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    // Using subject codes as IDs as per simplified seeder mapping
                    addMaterial(stmt, "CSIT-101", "C Programming Syllabus", "syllabus", adminId);
                    addMaterial(stmt, "CSIT-101", "Pointers & Arrays Lecture Note", "lecture_note", adminId);
                    addMaterial(stmt, "CSIT-201", "DS Algo Exercises", "assignment", adminId);
                    stmt.executeBatch();
                }
            }

            try (java.sql.Statement stmt = conn.createStatement()) {
                stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
            }

        } catch (Exception e) {
            System.err.println("Could not seed dummy data.");
            e.printStackTrace();
        }
    }

    private static void addProgram(PreparedStatement stmt, String id, String name, String code, String deptId,
            double duration) throws java.sql.SQLException {
        stmt.setString(1, id);
        stmt.setString(2, name);
        stmt.setString(3, code);
        stmt.setString(4, deptId);
        stmt.setDouble(5, duration);
        stmt.addBatch();
    }

    private static void addStudentUser(Connection conn, String stuId, String name, String email, String progId,
            int currentSemester)
            throws java.sql.SQLException {
        String userId = UUID.randomUUID().toString();
        String studentTableId = UUID.randomUUID().toString();

        // 1. Create User
        String sqlUser = "INSERT INTO users (id, email, password_hash) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sqlUser)) {
            stmt.setString(1, userId);
            stmt.setString(2, email);
            stmt.setString(3, PasswordUtil.hashPassword("student123")); // Default password
            stmt.executeUpdate();
        }

        // 2. Assign Student Role
        String sqlRole = "INSERT INTO user_roles (id, user_id, role) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sqlRole)) {
            stmt.setString(1, UUID.randomUUID().toString());
            stmt.setString(2, userId);
            stmt.setString(3, "student");
            stmt.executeUpdate();
        }

        // 3. Create Student Profile
        String sqlStu = "INSERT INTO students (id, user_id, student_id, full_name_en, date_of_birth, gender, nationality, address, phone, guardian_name, guardian_phone, guardian_relationship, program_id, current_semester) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sqlStu)) {
            stmt.setString(1, studentTableId);
            stmt.setString(2, userId);
            stmt.setString(3, stuId);
            stmt.setString(4, name);
            stmt.setString(5, "2005-01-01"); // Default DOB
            stmt.setString(6, "Other"); // Default Gender
            stmt.setString(7, "Nepali"); // Default Nationality
            stmt.setString(8, "Kathmandu"); // Default Address
            stmt.setString(9, "9841000000"); // Default Phone
            stmt.setString(10, "Guardian of " + name);
            stmt.setString(11, "9800000000");
            stmt.setString(12, "Father");
            stmt.setString(13, progId);
            stmt.setInt(14, currentSemester);
            stmt.executeUpdate();
        }
    }

    private static void addSubject(PreparedStatement stmt, String id, String code, String name, int credits,
            String semId,
            String type) throws java.sql.SQLException {
        stmt.setString(1, id);
        stmt.setString(2, code);
        stmt.setString(3, name);
        stmt.setInt(4, credits);
        stmt.setString(5, semId);
        stmt.setString(6, type);
        stmt.addBatch();
    }

    private static void addBook(PreparedStatement stmt, String bookId, String title, String author, String publisher,
            int year, String category, int copies, String shelf) throws java.sql.SQLException {
        stmt.setString(1, UUID.randomUUID().toString());
        stmt.setString(2, bookId);
        stmt.setString(3, title);
        stmt.setString(4, author);
        stmt.setString(5, publisher);
        stmt.setInt(6, year);
        stmt.setString(7, category);
        stmt.setInt(8, copies);
        stmt.setString(9, shelf);
        stmt.addBatch();
    }

    private static void addAssignment(PreparedStatement stmt, String staffId, String subCode, String year, String semId)
            throws java.sql.SQLException {
        stmt.setString(1, UUID.randomUUID().toString());
        stmt.setString(2, staffId);
        stmt.setString(3, subCode); // simplified mapping for seeder
        stmt.setString(4, year);
        stmt.setString(5, semId);
        stmt.setString(6, "2026-02-21");
        stmt.addBatch();
    }

    private static void clearDatabase(Connection conn) throws java.sql.SQLException {
        System.out.println("CAUTION: Clearing database for fresh seed...");
        String[] tables = { "exam_results", "learning_materials", "student_attendance", "course_assignments",
                "subjects",
                "semesters", "students", "staff", "user_roles", "users", "programs", "departments", "notices",
                "library_books" };
        try (java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
            for (String table : tables) {
                try {
                    stmt.execute("TRUNCATE TABLE " + table);
                } catch (java.sql.SQLException e) {
                    // Ignore if table doesn't exist during cleanup
                }
            }
        }
        System.out.println("Database cleanup phase completed.");
    }

    private static boolean isTableEmpty(Connection conn, String tableName) {
        String sql = "SELECT COUNT(*) FROM " + tableName;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) == 0;
                }
            }
        } catch (Exception e) {
            // Table might not exist yet if schema.sql wasn't fully run
        }
        return false;
    }

    private static void seedStudentPerformance(Connection conn, String studentTableId) throws java.sql.SQLException {
        String attSql = "INSERT INTO student_attendance (id, student_id, date, status) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(attSql)) {
            // Seed last 5 days
            for (int i = 0; i < 5; i++) {
                String date = "2026-02-" + (21 - i);
                String status = (i % 4 == 0) ? "absent" : "present";
                addAttendance(stmt, studentTableId, date, status);
            }
            stmt.executeBatch();
        }

        String resSql = "INSERT INTO exam_results (id, student_id, subject_id, marks_obtained, total_marks, grade, exam_type, academic_year, semester_id, published_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(resSql)) {
            addResult(stmt, studentTableId, "CSIT-101", 85.0, 100.0, "A", "internal", "2026", "sem-csit-1");
            addResult(stmt, studentTableId, "CSIT-201", 78.0, 100.0, "B+", "final", "2026", "sem-csit-2");
            stmt.executeBatch();
        }
    }

    private static void addAttendance(PreparedStatement stmt, String stuId, String date, String status)
            throws java.sql.SQLException {
        stmt.setString(1, UUID.randomUUID().toString());
        stmt.setString(2, stuId);
        stmt.setString(3, date);
        stmt.setString(4, status);
        stmt.addBatch();
    }

    private static void addResult(PreparedStatement stmt, String stuId, String subId, double marks, double total,
            String grade, String type, String year, String semId) throws java.sql.SQLException {
        stmt.setString(1, UUID.randomUUID().toString());
        stmt.setString(2, stuId);
        stmt.setString(3, subId);
        stmt.setDouble(4, marks);
        stmt.setDouble(5, total);
        stmt.setString(6, grade);
        stmt.setString(7, type);
        stmt.setString(8, year);
        stmt.setString(9, semId);
        stmt.setString(10, "2026-02-15");
        stmt.addBatch();
    }

    private static void addMaterial(PreparedStatement stmt, String subId, String title, String type, String pubBy)
            throws java.sql.SQLException {
        stmt.setString(1, UUID.randomUUID().toString());
        stmt.setString(2, subId);
        stmt.setString(3, title);
        stmt.setString(4, type);
        stmt.setString(5, pubBy);
        stmt.addBatch();
    }

    private static void initializeSchema(Connection conn) {
        System.out.println("Initializing database schema if missing...");
        try (java.io.InputStream is = DatabaseSeeder.class.getResourceAsStream("/schema.sql");
                Scanner scanner = is != null ? new Scanner(is).useDelimiter(";") : null) {
            if (scanner == null) {
                System.err.println("Could not find schema.sql in resources.");
                return;
            }
            try (Statement stmt = conn.createStatement()) {
                while (scanner.hasNext()) {
                    String sql = scanner.next().trim();
                    if (!sql.isEmpty()) {
                        try {
                            stmt.execute(sql);
                        } catch (java.sql.SQLException e) {
                            // Tables might already exist if IF NOT EXISTS is used or it's a known error
                            // System.out.println("Schema step info: " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Could not initialize schema.");
            e.printStackTrace();
        }
    }
}
