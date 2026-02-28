package org.example.fms.core.database;

import org.example.fms.core.security.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
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
            // clearDatabase(conn);

            // Seed holidays for the 10-year calendar range
            seedHolidays(conn);

            // 0. Seed Admin User (Fixed ID for consistency in notices/materials)
            String adminId = "admin-uuid-001";
            String sqlCheck = "SELECT COUNT(*) FROM users WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sqlCheck)) {
                stmt.setString(1, adminId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        String sqlUser = "INSERT INTO users (id, email, password_hash) VALUES (?, ?, ?)";
                        try (PreparedStatement pStmt = conn.prepareStatement(sqlUser)) {
                            pStmt.setString(1, adminId);
                            pStmt.setString(2, "admin@faculty.edu");
                            pStmt.setString(3, PasswordUtil.hashPassword("admin"));
                            pStmt.executeUpdate();
                        }
                        String sqlRole = "INSERT INTO user_roles (id, user_id, role) VALUES (?, ?, ?)";
                        try (PreparedStatement pStmt = conn.prepareStatement(sqlRole)) {
                            pStmt.setString(1, UUID.randomUUID().toString());
                            pStmt.setString(2, adminId);
                            pStmt.setString(3, "super_admin");
                            pStmt.executeUpdate();
                        }
                    }
                }
            }

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
                System.out.println("Seeding multiple faculty staff...");
                String[] teacherNames = {
                        "Dr. Shyam Nepal", "Prof. Krishna Thapa", "Ms. Sarita Rai", "Dr. Binod Mahat",
                        "Er. Pradip Gurung", "Dr. Anjali Sharma", "Mr. Ramesh Poudel", "Ms. Deepa Karki",
                        "Prof. Sanjay Gupta", "Dr. Meena Basnet"
                };
                String[] teacherEmails = {
                        "teacher@faculty.edu", "krishna@faculty.edu", "sarita@faculty.edu",
                        "binod@faculty.edu", "pradip@faculty.edu", "anjali@faculty.edu",
                        "ramesh@faculty.edu", "deepa@faculty.edu", "sanjay@faculty.edu",
                        "meena@faculty.edu"
                };
                String[] designs = {
                        "Professor", "Associate Professor", "Lecturer", "Professor",
                        "Lecturer", "Associate Professor", "Lecturer", "Lecturer",
                        "Professor", "Assistant Professor"
                };

                for (int i = 0; i < teacherNames.length; i++) {
                    String userId = UUID.randomUUID().toString();
                    String staffId = UUID.randomUUID().toString();

                    String sqlUser = "INSERT INTO users (id, email, password_hash) VALUES (?, ?, ?)";
                    try (PreparedStatement stmt = conn.prepareStatement(sqlUser)) {
                        stmt.setString(1, userId);
                        stmt.setString(2, teacherEmails[i]);
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
                        stmt.setString(2, "EMP-00" + (i + 1));
                        stmt.setString(3, userId);
                        stmt.setString(4, teacherNames[i]);
                        stmt.setString(5, designs[i]);
                        stmt.setString(6, "teaching");
                        // Distribute between CS and BA
                        stmt.setString(7, (i % 2 == 0) ? DEPT_CS : DEPT_BA);
                        stmt.setString(8, "permanent");
                        stmt.setString(9, "2020-01-01");
                        stmt.executeUpdate();
                    }
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

            // 8. Seed Course Assignments (Linking teachers to subjects)
            if (isTableEmpty(conn, "course_assignments")) {
                System.out.println("Seeding distributed course assignments...");
                String[] teacherNames = { "Dr. Shyam Nepal", "Prof. Krishna Thapa", "Ms. Sarita Rai", "Dr. Binod Mahat",
                        "Er. Pradip Gurung" };
                List<String> staffIds = new ArrayList<>();
                for (String name : teacherNames) {
                    try (PreparedStatement sStmt = conn
                            .prepareStatement("SELECT id FROM staff WHERE full_name_en = ?")) {
                        sStmt.setString(1, name);
                        try (ResultSet rs = sStmt.executeQuery()) {
                            if (rs.next())
                                staffIds.add(rs.getString("id"));
                        }
                    }
                }

                if (!staffIds.isEmpty()) {
                    String sql = "INSERT INTO course_assignments (id, staff_id, subject_id, academic_year, semester_id, assigned_date) VALUES (?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        // Distribute subjects to teachers
                        String[] progs = { "CSIT", "BCA", "BBM", "BBS" };
                        int tIdx = 0;
                        for (String p : progs) {
                            for (int s = 1; s <= 2; s++) { // Assign first 2 semesters
                                String subCode = p + "-" + s + "01";
                                String semId = "sem-" + p.toLowerCase() + "-" + s;
                                String staffId = staffIds.get(tIdx % staffIds.size());
                                addAssignment(stmt, staffId, subCode, "2026", semId);
                                tIdx++;
                            }
                        }
                        stmt.executeBatch();
                    }
                }
            }

            // 6. Seed Notices (Linked to fixed admin ID)
            if (isTableEmpty(conn, "notices")) {
                System.out.println("Seeding dummy notices...");
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
                String sql = "INSERT INTO learning_materials (id, subject_id, title, material_type, published_by) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    // Using subject codes as IDs as per simplified seeder mapping
                    addMaterial(stmt, "CSIT-101", "C Programming Syllabus", "syllabus", adminId);
                    addMaterial(stmt, "CSIT-101", "Pointers & Arrays Lecture Note", "lecture_note", adminId);
                    addMaterial(stmt, "CSIT-201", "DS Algo Exercises", "assignment", adminId);
                    stmt.executeBatch();
                }
            }

            // 11. Seed Assignments & Submissions
            if (isTableEmpty(conn, "assignments")) {
                System.out.println("Seeding assignments and sample submissions...");
                String assSql = "INSERT INTO assignments (id, subject_id, title, description, deadline, created_by) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(assSql)) {
                    String assId1 = UUID.randomUUID().toString();
                    stmt.setString(1, assId1);
                    stmt.setString(2, "CSIT-101");
                    stmt.setString(3, "C Programming Loop Exercises");
                    stmt.setString(4, "Solve the attached 5 problems based on nested loops.");
                    stmt.setTimestamp(5, java.sql.Timestamp.valueOf("2026-03-01 23:59:59"));
                    stmt.setString(6, adminId);
                    stmt.addBatch();

                    String assId2 = UUID.randomUUID().toString();
                    stmt.setString(1, assId2);
                    stmt.setString(2, "CSIT-201");
                    stmt.setString(3, "DSA Stack Implementation");
                    stmt.setString(4, "Implement a Stack using Array in C++.");
                    stmt.setTimestamp(5, java.sql.Timestamp.valueOf("2026-03-05 18:00:00"));
                    stmt.setString(6, adminId);
                    stmt.addBatch();

                    stmt.executeBatch();

                    // Seed a submission for the first student
                    try (PreparedStatement sStmt = conn.prepareStatement("SELECT id FROM students LIMIT 1")) {
                        try (ResultSet rs = sStmt.executeQuery()) {
                            if (rs.next()) {
                                String studentId = rs.getString("id");
                                String subSql = "INSERT INTO submissions (id, assignment_id, student_id, content_body, status) VALUES (?, ?, ?, ?, ?)";
                                try (PreparedStatement psSub = conn.prepareStatement(subSql)) {
                                    psSub.setString(1, UUID.randomUUID().toString());
                                    psSub.setString(2, assId1);
                                    psSub.setString(3, studentId);
                                    psSub.setString(4, "Here is my loop logic solution...");
                                    psSub.setString(5, "submitted");
                                    psSub.executeUpdate();
                                }
                            }
                        }
                    }
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

    @SuppressWarnings("unused")
    private static void clearDatabase(Connection conn) throws java.sql.SQLException {
        System.out.println("CAUTION: Clearing database for fresh seed...");
        String[] tables = { "holidays", "submissions", "assignments", "exam_results", "learning_materials",
                "student_attendance",
                "course_assignments",
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

    private static void seedHolidays(Connection conn) throws java.sql.SQLException {
        if (!isTableEmpty(conn, "holidays"))
            return;

        System.out.println("Fetching comprehensive Nepali holidays from the-value-crew API (2075-2085 BS)...");

        // API format:
        // https://the-value-crew.github.io/nepali-calendar-api/data/{YYYY}/{MM}
        // Months are 1-indexed (1=Baisakh, 12=Chaitra)
        // Day JSON structure: { "d": <BS day>, "f": "<festival name in Nepali>", "h":
        // <boolean public holiday> }

        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();

        String insertSql = "INSERT INTO holidays (id, bs_year, bs_month, bs_day, name, description, is_public_holiday) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
            int totalFetched = 0;
            int totalErrors = 0;

            for (int year = 2075; year <= 2085; year++) {
                for (int month = 1; month <= 12; month++) {
                    String url = "https://the-value-crew.github.io/nepali-calendar-api/data/" + year + "/" + month;
                    try {
                        java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                                .uri(java.net.URI.create(url))
                                .timeout(java.time.Duration.ofSeconds(15))
                                .GET()
                                .build();
                        java.net.http.HttpResponse<String> resp = httpClient.send(req,
                                java.net.http.HttpResponse.BodyHandlers.ofString());

                        if (resp.statusCode() == 200) {
                            String body = resp.body();
                            // Parse days array manually using Jackson
                            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(body);
                            com.fasterxml.jackson.databind.JsonNode daysNode = root.path("days");

                            if (daysNode.isArray()) {
                                for (int idx = 0; idx < daysNode.size(); idx++) {
                                    com.fasterxml.jackson.databind.JsonNode dayNode = daysNode.get(idx);
                                    int bsDay = idx + 1; // API days are 0-indexed in the array
                                    boolean isPublic = dayNode.path("h").asBoolean(false);
                                    String festival = dayNode.path("f").asText("").trim();

                                    // Store if it's a public holiday OR has a festival/event name
                                    if (isPublic || !festival.isEmpty()) {
                                        stmt.setString(1, UUID.randomUUID().toString());
                                        stmt.setInt(2, year);
                                        stmt.setInt(3, month - 1); // Store 0-indexed (Baisakh=0) to match UI
                                        stmt.setInt(4, bsDay);
                                        stmt.setString(5, festival.isEmpty() ? "Holiday" : festival);
                                        // Nepali month names for description
                                        String[] mnp = { "Baisakh", "Jestha", "Ashadh", "Shrawan", "Bhadra",
                                                "Ashwin", "Kartik", "Mangsir", "Poush", "Magh", "Falgun", "Chaitra" };
                                        stmt.setString(6, "Nepali Calendar event on " + mnp[month - 1] + " " + bsDay
                                                + ", " + year + " BS.");
                                        stmt.setBoolean(7, isPublic);
                                        stmt.addBatch();
                                        totalFetched++;
                                    }
                                }
                            }
                        } else {
                            System.out.println("  API returned status " + resp.statusCode() + " for " + year + "/"
                                    + month + ". Using fallback.");
                            totalErrors++;
                        }
                        // Small delay to avoid hitting rate limits
                        Thread.sleep(50);

                    } catch (Exception e) {
                        System.out.println("  Failed to fetch " + year + "/" + month + ": " + e.getMessage());
                        totalErrors++;
                    }
                }
                System.out.println("  Fetched holidays for year " + year + " BS.");
            }

            if (totalFetched > 0) {
                stmt.executeBatch();
                System.out.println("Successfully seeded " + totalFetched + " holiday/festival events from live API.");
            } else {
                System.out.println("API fetch returned no data (" + totalErrors
                        + " errors). Seeding comprehensive fallback holidays...");
                seedFallbackHolidays(conn);
            }
        } catch (Exception e) {
            System.err.println("Holiday seeding from API failed entirely: " + e.getMessage() + ". Seeding fallback...");
            seedFallbackHolidays(conn);
        }
    }

    /**
     * Comprehensive fallback holidays seeded manually if the API is unavailable.
     * Covers all major official public holidays & major festivals (2075-2085 BS).
     * Month is 0-indexed (0=Baisakh, 11=Chaitra) to match BsCalendarData in UI.
     */
    private static void seedFallbackHolidays(Connection conn) throws java.sql.SQLException {
        String sql = "INSERT INTO holidays (id, bs_year, bs_month, bs_day, name, description, is_public_holiday) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            // === Recurring Annual Holidays (all years 2075-2085) ===
            for (int y = 2075; y <= 2085; y++) {
                // Baisakh 1 - New Year
                addH(stmt, y, 0, 1, "Nepali New Year (Navabarsha)", "Baisakh Shukla Pratipada – national holiday.",
                        true);
                // Shrawan 1 - Shrawan Sombar begins (not public but cultural)
                addH(stmt, y, 3, 1, "Shrawan month begins", "Auspicious month of Shrawan starts.", false);
                // Constitution Day - Ashwin 3
                addH(stmt, y, 5, 3, "Constitution Day", "Promulgation of the Constitution of Nepal 2072.", true);
                // Christmas - Poush 10
                addH(stmt, y, 8, 10, "Christmas Day", "Christian festival celebrated globally.", false);
                // New Year (English) - Poush 17/16
                addH(stmt, y, 8, (y % 4 == 0 ? 17 : 16), "English New Year", "January 1st in AD calendar.", false);
            }

            // === Year-specific Official Public Holidays ===
            // 2075 BS (2018/19 AD)
            addH(stmt, 2075, 0, 1, "Nepali New Year", "Baisakh 1, 2075", true);
            addH(stmt, 2075, 1, 7, "Buddha Purnima", "Birthday of Gautama Buddha.", true);
            addH(stmt, 2075, 3, 4, "Janai Purnima / Raksha Bandhan", "Sacred thread ceremony.", true);
            addH(stmt, 2075, 5, 3, "Ghatasthapana, Dashain begins", "Start of Dashain festival.", false);
            addH(stmt, 2075, 5, 12, "Vijaya Dashami", "Main day of Dashain festival.", true);
            addH(stmt, 2075, 6, 1, "Tihar/Deepawali Begins", "Festival of lights.", false);
            addH(stmt, 2075, 6, 4, "Laxmi Puja", "Worship of Goddess Laxmi during Tihar.", true);
            addH(stmt, 2075, 6, 5, "Govardhan Puja", "Tihar festival day.", true);
            addH(stmt, 2075, 8, 15, "Prithvi Jayanti", "Birthday of King Prithvi Narayan Shah.", true);
            addH(stmt, 2075, 9, 7, "Martyr's Day", "Shaheed Diwas. National holiday.", true);
            addH(stmt, 2075, 10, 7, "Democracy Day", "Prajatantra Diwas.", true);
            addH(stmt, 2075, 10, 19, "Maha Shivaratri", "Shivaratri festival & Army Day.", true);
            addH(stmt, 2075, 11, 7, "Women's Day", "International Women's Day.", false);
            addH(stmt, 2075, 11, 15, "Holi (Hilly)", "Festival of colors in hills.", true);
            addH(stmt, 2075, 11, 16, "Holi (Terai)", "Festival of colors in plains.", true);

            // 2076 BS (2019/20 AD)
            addH(stmt, 2076, 1, 4, "Buddha Purnima", "Vesak/Buddha Jayanti.", true);
            addH(stmt, 2076, 3, 3, "Janai Purnima", "Sacred thread ceremony and Raksha Bandhan.", true);
            addH(stmt, 2076, 5, 1, "Ghatasthapana", "Dashain begins.", false);
            addH(stmt, 2076, 5, 10, "Vijaya Dashami", "Main Dashain holiday.", true);
            addH(stmt, 2076, 5, 11, "Ekadashi", "Post-Dashami ekadashi.", false);
            addH(stmt, 2076, 6, 2, "Deepawali / Tihar", "Festival of Lights.", true);
            addH(stmt, 2076, 6, 3, "Laxmi Puja", "Lakshmi worship day.", true);
            addH(stmt, 2076, 9, 7, "Martyr's Day", "National holiday.", true);
            addH(stmt, 2076, 10, 8, "Maha Shivaratri", "Holy night of Lord Shiva.", true);
            addH(stmt, 2076, 10, 7, "Democracy Day", "Prajatantra Diwas.", true);
            addH(stmt, 2076, 11, 5, "Holi (Hilly)", "Colors festival.", true);
            addH(stmt, 2076, 11, 6, "Holi (Terai)", "Colors festival in Terai.", true);

            // 2077 BS (2020/21 AD)
            addH(stmt, 2077, 1, 22, "Buddha Purnima", "Vesak Day.", true);
            addH(stmt, 2077, 3, 22, "Janai Purnima", "Raksha Bandhan.", true);
            addH(stmt, 2077, 5, 18, "Vijaya Dashami", "Dashain main day.", true);
            addH(stmt, 2077, 6, 9, "Laxmi Puja", "Tihar main day.", true);
            addH(stmt, 2077, 9, 7, "Martyr's Day", "Shaheed Diwas.", true);
            addH(stmt, 2077, 10, 28, "Maha Shivaratri", "Shivaratri.", true);
            addH(stmt, 2077, 10, 7, "Democracy Day", "Prajatantra Diwas.", true);
            addH(stmt, 2077, 11, 13, "Holi (Hilly)", "Festival of Colors.", true);
            addH(stmt, 2077, 11, 14, "Holi (Terai)", "Festival of Colors.", true);

            // 2078 BS (2021/22 AD)
            addH(stmt, 2078, 1, 11, "Buddha Purnima", "Vesak.", true);
            addH(stmt, 2078, 3, 11, "Janai Purnima", "Raksha Bandhan.", true);
            addH(stmt, 2078, 5, 7, "Vijaya Dashami", "Dashain.", true);
            addH(stmt, 2078, 6, 29, "Laxmi Puja", "Tihar.", true);
            addH(stmt, 2078, 9, 7, "Martyr's Day", "Shaheed Diwas.", true);
            addH(stmt, 2078, 10, 17, "Maha Shivaratri", "Major Hindu festival.", true);
            addH(stmt, 2078, 10, 7, "Democracy Day", "Prajatantra Diwas.", true);
            addH(stmt, 2078, 11, 3, "Holi (Hilly)", "Colors.", true);
            addH(stmt, 2078, 11, 4, "Holi (Terai)", "Colors.", true);

            // 2079 BS (2022/23 AD)
            addH(stmt, 2079, 1, 30, "Buddha Purnima", "Vesak.", true);
            addH(stmt, 2079, 3, 1, "Janai Purnima", "Raksha Bandhan.", true);
            addH(stmt, 2079, 5, 25, "Vijaya Dashami", "Dashain.", true);
            addH(stmt, 2079, 6, 17, "Laxmi Puja", "Tihar.", true);
            addH(stmt, 2079, 9, 7, "Martyr's Day", "Shaheed Diwas.", true);
            addH(stmt, 2079, 10, 6, "Maha Shivaratri", "Lord Shiva's holy night.", true);
            addH(stmt, 2079, 10, 7, "Democracy Day", "Prajatantra Diwas.", true);
            addH(stmt, 2079, 11, 21, "Holi (Hilly)", "Holi festival.", true);
            addH(stmt, 2079, 11, 22, "Holi (Terai)", "Holi festival.", true);

            // 2080 BS (2023/24 AD)
            addH(stmt, 2080, 1, 19, "Buddha Purnima", "Vesak.", true);
            addH(stmt, 2080, 3, 20, "Janai Purnima", "Raksha Bandhan.", true);
            addH(stmt, 2080, 5, 14, "Vijaya Dashami", "Dashain.", true);
            addH(stmt, 2080, 6, 6, "Laxmi Puja", "Tihar.", true);
            addH(stmt, 2080, 7, 16, "Chhath Parva", "Sun worship festival.", true);
            addH(stmt, 2080, 9, 7, "Martyr's Day", "Shaheed Diwas.", true);
            addH(stmt, 2080, 10, 23, "Maha Shivaratri", "Lord Shiva's night.", true);
            addH(stmt, 2080, 10, 7, "Democracy Day", "Prajatantra Diwas.", true);
            addH(stmt, 2080, 11, 10, "Holi (Hilly)", "Festival of Colors.", true);
            addH(stmt, 2080, 11, 11, "Holi (Terai)", "Festival of Colors.", true);

            // 2081 BS (2024/25 AD)
            addH(stmt, 2081, 1, 7, "Buddha Purnima", "Vesak.", true);
            addH(stmt, 2081, 3, 8, "Janai Purnima", "Raksha Bandhan.", true);
            addH(stmt, 2081, 5, 3, "Ghatasthapana", "Dashain begins.", false);
            addH(stmt, 2081, 5, 12, "Fulpati", "Dashain.", false);
            addH(stmt, 2081, 5, 3, "Vijaya Dashami", "Main Dashain holiday.", true);
            addH(stmt, 2081, 6, 25, "Laxmi Puja", "Tihar.", true);
            addH(stmt, 2081, 7, 5, "Chhath Parva", "Sun worship.", true);
            addH(stmt, 2081, 9, 7, "Martyr's Day", "Shaheed Diwas.", true);
            addH(stmt, 2081, 10, 12, "Maha Shivaratri", "Shivaratri.", true);
            addH(stmt, 2081, 10, 7, "Democracy Day", "Prajatantra Diwas.", true);
            addH(stmt, 2081, 10, 10, "Gyalpo Lhosar", "Tibetan New Year.", false);
            addH(stmt, 2081, 11, 28, "Holi (Hilly)", "Festival of Colors.", true);
            addH(stmt, 2081, 11, 29, "Holi (Terai)", "Festival of Colors.", true);

            // 2082 BS (2025/26 AD) - Current year with detailed data
            addH(stmt, 2082, 0, 1, "Nepali New Year", "Baisakh 1, 2082 BS.", true);
            addH(stmt, 2082, 1, 26, "Buddha Purnima", "Vesak Day 2082.", true);
            addH(stmt, 2082, 3, 28, "Janai Purnima", "Sacred thread & Raksha Bandhan.", true);
            addH(stmt, 2082, 5, 3, "Constitution Day", "Nepal Constitution Day 2082.", true);
            addH(stmt, 2082, 5, 21, "Ghatasthapana", "Dashain 2082 begins.", false);
            addH(stmt, 2082, 5, 30, "Fulpati", "7th day of Dashain.", false);
            addH(stmt, 2082, 6, 1, "Maha Ashtami", "Dashain 8th day.", true);
            addH(stmt, 2082, 6, 2, "Maha Navami", "Dashain 9th day.", true);
            addH(stmt, 2082, 6, 3, "Vijaya Dashami", "Main Dashain holiday 2082.", true);
            addH(stmt, 2082, 6, 4, "Ekadashi", "Post-Dashami.", false);
            addH(stmt, 2082, 7, 15, "Laxmi Puja", "Tihar 2082.", true);
            addH(stmt, 2082, 7, 16, "Govardhan Puja", "Tihar.", true);
            addH(stmt, 2082, 7, 17, "Bhai Tika", "Brother-sister festival.", true);
            addH(stmt, 2082, 7, 25, "Chhath Parva", "Sun worship 2082.", true);
            addH(stmt, 2082, 8, 15, "Prithvi Narayan Shah Jayanti", "Unification Day of Nepal.", true);
            addH(stmt, 2082, 9, 7, "Martyr's Day", "Shaheed Diwas 2082.", true);
            addH(stmt, 2082, 10, 3, "Maha Shivaratri", "Shivaratri 2082 & Army Day.", true);
            addH(stmt, 2082, 10, 6, "Gyalpo Lhosar", "Tibetan New Year 2082.", false);
            addH(stmt, 2082, 10, 7, "Democracy Day", "Prajatantra Diwas 2082.", true);
            addH(stmt, 2082, 10, 18, "Holi (Hilly)", "Festival of Colors - Hills.", true);
            addH(stmt, 2082, 10, 19, "Holi (Terai)", "Festival of Colors - Terai.", true);
            addH(stmt, 2082, 10, 24, "International Women's Day", "March 8, 2082 BS.", false);
            addH(stmt, 2082, 11, 17, "Ram Nawami", "Birthday of Lord Rama.", false);

            // 2083 BS (2026/27 AD)
            addH(stmt, 2083, 1, 15, "Buddha Purnima", "Vesak 2083.", true);
            addH(stmt, 2083, 3, 17, "Janai Purnima", "Raksha Bandhan 2083.", true);
            addH(stmt, 2083, 5, 3, "Constitution Day", "", true);
            addH(stmt, 2083, 5, 11, "Ghatasthapana", "Dashain 2083 begins.", false);
            addH(stmt, 2083, 5, 20, "Vijaya Dashami", "Dashain main day 2083.", true);
            addH(stmt, 2083, 6, 3, "Laxmi Puja", "Tihar 2083.", true);
            addH(stmt, 2083, 7, 14, "Chhath Parva", "", true);
            addH(stmt, 2083, 9, 7, "Martyr's Day", "", true);
            addH(stmt, 2083, 10, 21, "Maha Shivaratri", "Shivaratri 2083.", true);
            addH(stmt, 2083, 10, 7, "Democracy Day", "", true);
            addH(stmt, 2083, 11, 16, "Holi (Hilly)", "", true);
            addH(stmt, 2083, 11, 17, "Holi (Terai)", "", true);

            // 2084 BS (2027/28 AD)
            addH(stmt, 2084, 1, 5, "Buddha Purnima", "Vesak 2084.", true);
            addH(stmt, 2084, 3, 5, "Janai Purnima", "Raksha Bandhan 2084.", true);
            addH(stmt, 2084, 5, 3, "Constitution Day", "", true);
            addH(stmt, 2084, 5, 1, "Ghatasthapana", "Dashain 2084 begins.", false);
            addH(stmt, 2084, 5, 10, "Vijaya Dashami", "Main Dashain Day 2084.", true);
            addH(stmt, 2084, 6, 22, "Laxmi Puja", "Tihar 2084.", true);
            addH(stmt, 2084, 7, 2, "Chhath Parva", "", true);
            addH(stmt, 2084, 9, 7, "Martyr's Day", "", true);
            addH(stmt, 2084, 10, 11, "Maha Shivaratri", "Shivaratri 2084.", true);
            addH(stmt, 2084, 10, 7, "Democracy Day", "", true);
            addH(stmt, 2084, 11, 5, "Holi (Hilly)", "", true);
            addH(stmt, 2084, 11, 6, "Holi (Terai)", "", true);

            // 2085 BS (2028/29 AD)
            addH(stmt, 2085, 1, 23, "Buddha Purnima", "Vesak 2085.", true);
            addH(stmt, 2085, 3, 23, "Janai Purnima", "Raksha Bandhan 2085.", true);
            addH(stmt, 2085, 5, 3, "Constitution Day", "", true);
            addH(stmt, 2085, 5, 19, "Vijaya Dashami", "Dashain 2085.", true);
            addH(stmt, 2085, 6, 12, "Laxmi Puja", "Tihar 2085.", true);
            addH(stmt, 2085, 9, 7, "Martyr's Day", "", true);
            addH(stmt, 2085, 10, 29, "Maha Shivaratri", "Shivaratri 2085.", true);
            addH(stmt, 2085, 10, 7, "Democracy Day", "", true);
            addH(stmt, 2085, 11, 23, "Holi (Hilly)", "", true);
            addH(stmt, 2085, 11, 24, "Holi (Terai)", "", true);

            stmt.executeBatch();
            System.out.println("Fallback holiday data seeded successfully.");
        }
    }

    private static void addH(PreparedStatement stmt, int y, int m, int d, String name, String desc, boolean isPublic)
            throws java.sql.SQLException {
        stmt.setString(1, UUID.randomUUID().toString());
        stmt.setInt(2, y);
        stmt.setInt(3, m); // 0-indexed month (0=Baisakh)
        stmt.setInt(4, d);
        stmt.setString(5, name);
        stmt.setString(6, desc.isEmpty() ? name : desc);
        stmt.setBoolean(7, isPublic);
        stmt.addBatch();
    }

    /** Public alias for use from FmsApplication at startup. */
    public static void initSchema(Connection conn) {
        initializeSchema(conn);
    }

    /**
     * Public alias: seeds holidays if the table is empty. Call from FmsApplication.
     */
    public static void seedHolidaysPublic(Connection conn) throws java.sql.SQLException {
        seedHolidays(conn);
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
