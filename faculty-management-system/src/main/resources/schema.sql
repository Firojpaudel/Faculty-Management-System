-- Phase 1: Core Foundation Schema for MBMC Faculty Management System

CREATE DATABASE IF NOT EXISTS mbmc_fms;
USE mbmc_fms;

-- Users Table
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    last_login_at TIMESTAMP NULL,
    failed_login_attempts INT DEFAULT 0,
    locked_until TIMESTAMP NULL,
    two_fa_enabled BOOLEAN DEFAULT FALSE,
    two_fa_secret VARCHAR(255) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    created_by VARCHAR(36) NULL,
    updated_by VARCHAR(36) NULL
);

-- User Roles Table
CREATE TABLE IF NOT EXISTS user_roles (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    role ENUM(
        'super_admin', 'campus_chief', 'finance_officer', 
        'admission_officer', 'examination_controller', 'it_support', 
        'hr_officer', 'librarian', 'department_head', 'faculty', 
        'support_staff', 'nurse', 'student'
    ) NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_by VARCHAR(36) NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Immutable Audit Logs Table
CREATE TABLE IF NOT EXISTS audit_logs (
    id VARCHAR(36) PRIMARY KEY,
    actor_id VARCHAR(36) NULL,
    actor_role VARCHAR(255) NULL,
    action ENUM('CREATE', 'UPDATE', 'DELETE', 'LOGIN', 'LOGOUT', 'LOGIN_FAILED', 'UNLOCK', 'EXPORT') NOT NULL,
    module VARCHAR(255) NOT NULL,
    resource_type VARCHAR(255) NOT NULL,
    resource_id VARCHAR(36) NULL,
    old_value JSON NULL,
    new_value JSON NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ip_address VARCHAR(45) NULL,
    device_fingerprint VARCHAR(255) NULL,
    session_id VARCHAR(255) NULL
);

-- Schools / Departments (Supporting table for Staff/Programs)
CREATE TABLE IF NOT EXISTS departments (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Staff Table
CREATE TABLE IF NOT EXISTS staff (
    id VARCHAR(36) PRIMARY KEY,
    staff_id VARCHAR(50) UNIQUE NOT NULL,
    user_id VARCHAR(36) NULL,
    full_name_en VARCHAR(255) NOT NULL,
    full_name_np VARCHAR(255) CHARACTER SET utf8mb4 NULL,
    designation VARCHAR(255) NOT NULL,
    staff_type ENUM('teaching', 'administrative', 'support') NOT NULL,
    department_id VARCHAR(36) NULL,
    employment_type ENUM('permanent', 'part_time', 'visiting', 'contract') NOT NULL,
    is_department_head BOOLEAN DEFAULT FALSE,
    date_of_joining DATE NOT NULL,
    date_of_leaving DATE NULL,
    qualification JSON NULL,
    phone VARCHAR(50) NULL,
    email VARCHAR(255) NULL,
    national_id VARCHAR(255) NULL,
    salary_grade VARCHAR(255) NULL,
    shift_type ENUM('morning', 'day', 'night') NULL,
    status ENUM('active', 'inactive', 'on_leave', 'terminated') DEFAULT 'active',
    emergency_contact_name VARCHAR(255) NULL,
    emergency_contact_phone VARCHAR(50) NULL,
    contract_expiry_date DATE NULL,
    profile_photo_url VARCHAR(500) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    created_by VARCHAR(36) NULL,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (department_id) REFERENCES departments(id)
);

-- Students Table
CREATE TABLE IF NOT EXISTS students (
    id VARCHAR(36) PRIMARY KEY,
    student_id VARCHAR(50) UNIQUE NOT NULL,
    user_id VARCHAR(36) NULL,
    full_name_en VARCHAR(255) NOT NULL,
    full_name_np VARCHAR(255) CHARACTER SET utf8mb4 NULL,
    date_of_birth DATE NOT NULL,
    gender VARCHAR(50) NOT NULL,
    nationality VARCHAR(100) NOT NULL,
    ethnicity VARCHAR(100) NULL,
    address VARCHAR(500) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    email VARCHAR(255) NULL,
    guardian_name VARCHAR(255) NOT NULL,
    guardian_phone VARCHAR(50) NOT NULL,
    guardian_relationship VARCHAR(50) NOT NULL,
    program_id VARCHAR(36) NULL, -- Will be a foreign key to programs table later
    batch_id VARCHAR(36) NULL,
    section_id VARCHAR(36) NULL,
    current_semester INT NULL,
    enrollment_status ENUM('active', 'passed_out', 'dropout', 'suspended', 'on_leave') DEFAULT 'active',
    scholarship_type VARCHAR(255) NULL,
    fee_category VARCHAR(255) NULL,
    tu_registration_number VARCHAR(100) NULL,
    neb_registration_number VARCHAR(100) NULL,
    profile_photo_url VARCHAR(500) NULL,
    documents JSON NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    created_by VARCHAR(36) NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Programs Table
CREATE TABLE IF NOT EXISTS programs (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) UNIQUE NOT NULL,
    department_id VARCHAR(36) NULL,
    duration_years DECIMAL(4,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (department_id) REFERENCES departments(id)
);

-- Semesters Table
CREATE TABLE IF NOT EXISTS semesters (
    id VARCHAR(36) PRIMARY KEY,
    program_id VARCHAR(36) NOT NULL,
    semester_number INT NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (program_id) REFERENCES programs(id)
);

-- Subjects Table
CREATE TABLE IF NOT EXISTS subjects (
    id VARCHAR(36) PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    credits INT NOT NULL,
    program_id VARCHAR(36) NULL,
    semester_id VARCHAR(36) NULL,
    type ENUM('theory', 'practical', 'both', 'project') DEFAULT 'theory',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (program_id) REFERENCES programs(id),
    FOREIGN KEY (semester_id) REFERENCES semesters(id)
);

-- Course Assignments (Faculty teaching a Subject)
CREATE TABLE IF NOT EXISTS course_assignments (
    id VARCHAR(36) PRIMARY KEY,
    staff_id VARCHAR(36) NOT NULL,
    subject_id VARCHAR(36) NOT NULL,
    academic_year VARCHAR(20) NOT NULL,
    semester_id VARCHAR(36) NOT NULL,
    assigned_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (staff_id) REFERENCES staff(id),
    FOREIGN KEY (subject_id) REFERENCES subjects(id),
    FOREIGN KEY (semester_id) REFERENCES semesters(id)
);

-- Notices / Announcements
CREATE TABLE IF NOT EXISTS notices (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    target_audience ENUM('all', 'staff', 'students', 'department') DEFAULT 'all',
    department_id VARCHAR(36) NULL,
    published_by VARCHAR(36) NOT NULL,
    published_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expiry_date TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (published_by) REFERENCES users(id),
    FOREIGN KEY (department_id) REFERENCES departments(id)
);

-- Leave Requests (Staff)
CREATE TABLE IF NOT EXISTS leave_requests (
    id VARCHAR(36) PRIMARY KEY,
    staff_id VARCHAR(36) NOT NULL,
    leave_type ENUM('sick', 'casual', 'annual', 'maternity', 'unpaid') NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason TEXT NOT NULL,
    status ENUM('pending', 'approved', 'rejected') DEFAULT 'pending',
    approved_by VARCHAR(36) NULL,
    applied_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (staff_id) REFERENCES staff(id),
    FOREIGN KEY (approved_by) REFERENCES users(id)
);

-- Attendance (Staff)
CREATE TABLE IF NOT EXISTS staff_attendance (
    id VARCHAR(36) PRIMARY KEY,
    staff_id VARCHAR(36) NOT NULL,
    date DATE NOT NULL,
    status ENUM('present', 'absent', 'late', 'half_day', 'on_leave') NOT NULL,
    check_in_time TIME NULL,
    check_out_time TIME NULL,
    remarks VARCHAR(255) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (staff_id) REFERENCES staff(id)
);

-- Library Books Table
CREATE TABLE IF NOT EXISTS library_books (
    id VARCHAR(36) PRIMARY KEY,
    book_id VARCHAR(50) UNIQUE NOT NULL,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    publisher VARCHAR(255) NULL,
    year_of_publication INT NULL,
    category VARCHAR(100) NULL,
    total_copies INT DEFAULT 1,
    available_copies INT DEFAULT 1,
    shelf_location_code VARCHAR(50) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
-- Student Attendance Table
CREATE TABLE IF NOT EXISTS student_attendance (
    id VARCHAR(36) PRIMARY KEY,
    student_id VARCHAR(36) NOT NULL,
    date DATE NOT NULL,
    status ENUM('present', 'absent', 'late', 'on_leave') NOT NULL,
    remarks VARCHAR(255) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(id)
);

-- Learning Materials Table
CREATE TABLE IF NOT EXISTS learning_materials (
    id VARCHAR(36) PRIMARY KEY,
    subject_id VARCHAR(36) NOT NULL,
    title VARCHAR(255) NOT NULL,
    material_type ENUM('syllabus', 'lecture_note', 'assignment', 'reference_book') NOT NULL,
    content_url TEXT NULL,
    content_body TEXT NULL,
    published_by VARCHAR(36) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (subject_id) REFERENCES subjects(id),
    FOREIGN KEY (published_by) REFERENCES users(id)
);

-- Exam Results / Marksheets Table
CREATE TABLE IF NOT EXISTS exam_results (
    id VARCHAR(36) PRIMARY KEY,
    student_id VARCHAR(36) NOT NULL,
    subject_id VARCHAR(36) NOT NULL,
    marks_obtained DECIMAL(5,2) NOT NULL,
    total_marks DECIMAL(5,2) NOT NULL,
    grade VARCHAR(5) NULL,
    exam_type ENUM('internal', 'final', 'mid_term') NOT NULL,
    academic_year VARCHAR(20) NOT NULL,
    semester_id VARCHAR(36) NOT NULL,
    published_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES students(id),
    FOREIGN KEY (subject_id) REFERENCES subjects(id),
    FOREIGN KEY (semester_id) REFERENCES semesters(id)
);

-- Assignments Table
CREATE TABLE IF NOT EXISTS assignments (
    id VARCHAR(36) PRIMARY KEY,
    subject_id VARCHAR(36) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NULL,
    deadline TIMESTAMP NOT NULL,
    created_by VARCHAR(36) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (subject_id) REFERENCES subjects(id),
    FOREIGN KEY (created_by) REFERENCES users(id)
);

-- Student Submissions Table
CREATE TABLE IF NOT EXISTS submissions (
    id VARCHAR(36) PRIMARY KEY,
    assignment_id VARCHAR(36) NOT NULL,
    student_id VARCHAR(36) NOT NULL,
    content_body TEXT NULL,
    file_url VARCHAR(500) NULL,
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status ENUM('submitted', 'late', 'graded', 'returned') DEFAULT 'submitted',
    marks DECIMAL(5,2) NULL,
    feedback TEXT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (assignment_id) REFERENCES assignments(id),
    FOREIGN KEY (student_id) REFERENCES students(id)
);

-- Holidays Table
CREATE TABLE IF NOT EXISTS holidays (
    id VARCHAR(36) PRIMARY KEY,
    bs_year INT NOT NULL,
    bs_month INT NOT NULL,
    bs_day INT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    is_public_holiday BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
