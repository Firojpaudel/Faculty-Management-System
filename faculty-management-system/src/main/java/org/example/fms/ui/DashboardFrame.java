package org.example.fms.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.ArrayList;

public class DashboardFrame extends JFrame {

    private CardLayout cardLayout;
    private JPanel cardsPanel;
    private JLabel lblTotalStudents, lblActiveStaff, lblNewNotices;

    public DashboardFrame() {
        setTitle("Faculty Management System - Dashboard");
        setSize(1100, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        Color sidebarBg = new Color(30, 41, 59);
        Color sidebarHover = new Color(51, 65, 85);
        Color primaryBlue = new Color(79, 70, 229);

        getRootPane().putClientProperty("JRootPane.titleBarBackground", sidebarBg);
        getRootPane().putClientProperty("JRootPane.titleBarForeground", Color.WHITE);

        // Sidebar Panel
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(sidebarBg);
        // Widened sidebar to prevent truncation (...) on buttons and titles
        sidebar.setPreferredSize(new Dimension(280, getHeight()));
        sidebar.setBorder(new EmptyBorder(20, 0, 0, 0));

        // Use HTML to multi-line the brand title so it doesn't truncate
        JLabel brand = new JLabel("<html><div style='text-align: center;'>Faculty Management<br>System</div></html>");
        brand.setForeground(Color.WHITE);
        brand.setFont(new Font("Segoe UI", Font.BOLD, 20));
        brand.setAlignmentX(Component.CENTER_ALIGNMENT);
        brand.setBorder(BorderFactory.createEmptyBorder(10, 0, 40, 0));
        sidebar.add(brand);

        String[] allMenuItems = { "Dashboard Overview", "User Management", "Student Directory", "Staff Records",
                "Subjects & Courses", "My Attendance", "Learning Materials", "Library", "My Results", "Leave Requests",
                "Notices", "Logout" };

        List<String> visibleItems = new ArrayList<>();
        String role = SessionManager.getRole();

        for (String item : allMenuItems) {
            if ("student".equalsIgnoreCase(role)) {
                if (item.equals("User Management") || item.equals("Staff Records") || item.equals("Student Directory")
                        || item.equals("Leave Requests")) {
                    continue; // Skip admin-only modules for students
                }
            } else {
                // Admins/Staff don't need the student-specific "My ..." views
                if (item.equals("My Attendance") || item.equals("My Results")) {
                    continue;
                }
            }
            visibleItems.add(item);
        }

        String[] menuItems = visibleItems.toArray(new String[0]);

        // Card Layout for main content to switch between tables
        cardLayout = new CardLayout();
        cardsPanel = new JPanel(cardLayout);
        cardsPanel.setBackground(new Color(248, 250, 252));
        cardsPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Initialize Views (Tables)
        cardsPanel.add(createDashboardOverview(), "Dashboard Overview");

        if (!"student".equalsIgnoreCase(role)) {
            cardsPanel.add(
                    createTablePanel("User", new String[] { "ID", "Email", "Role", "Created At" }, new Object[][] {}),
                    "User Management");
            cardsPanel.add(createTablePanel("Staff",
                    new String[] { "Staff ID", "Full Name", "Designation", "Department ID" },
                    new Object[][] {}), "Staff Records");
            cardsPanel
                    .add(createTablePanel("Leave", new String[] { "ID", "Staff Name", "Leave Type", "Dates", "Status" },
                            new Object[][] {}), "Leave Requests");
        }

        cardsPanel.add(createTablePanel("Student",
                new String[] { "Student ID", "Full Name", "Gender", "Program ID" }, new Object[][] {}),
                "Student Directory");

        if ("student".equalsIgnoreCase(role)) {
            cardsPanel.add(createTablePanel("My Attendance", new String[] { "Date", "Status" }, new Object[][] {}),
                    "My Attendance");
            cardsPanel.add(
                    createTablePanel("My Subject", new String[] { "Code", "Name", "Type", "Credits" },
                            new Object[][] {}),
                    "Subjects & Courses");
            cardsPanel.add(createTablePanel("Learning Material", new String[] { "Title", "Type", "Subject" },
                    new Object[][] {}), "Learning Materials");
            cardsPanel.add(createTablePanel("Library Book",
                    new String[] { "ID", "Title", "Author", "Category", "Available" }, new Object[][] {}), "Library");
            cardsPanel.add(
                    createTablePanel("My Result", new String[] { "Subject", "Marks", "Grade", "Exam Type" },
                            new Object[][] {}),
                    "My Results");
        } else {
            cardsPanel.add(createTablePanel("Subject", new String[] { "Code", "Name", "Credits", "Type" },
                    new Object[][] {}), "Subjects & Courses");
        }

        cardsPanel.add(createTablePanel("Notice", new String[] { "ID", "Title", "Audience", "Published Date" },
                new Object[][] {}), "Notices");

        for (String item : menuItems) {
            JButton btn = new JButton(item);
            btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
            btn.setBackground(sidebarBg);
            btn.setForeground(Color.LIGHT_GRAY);
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 15));
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            btn.setBorder(new EmptyBorder(12, 25, 12, 25));

            // Hover effect
            btn.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    btn.setBackground(sidebarHover);
                    btn.setForeground(Color.WHITE);
                }

                public void mouseExited(java.awt.event.MouseEvent evt) {
                    btn.setBackground(sidebarBg);
                    btn.setForeground(Color.LIGHT_GRAY);
                }
            });

            if (item.equals("Logout")) {
                btn.setForeground(new Color(239, 68, 68)); // Red color for logout
                sidebar.add(Box.createVerticalGlue()); // Push logout to bottom
                btn.addActionListener(e -> {
                    SessionManager.clear();
                    new LoginFrame().setVisible(true);
                    this.dispose();
                });
            } else {
                btn.addActionListener(e -> cardLayout.show(cardsPanel, item));
            }

            sidebar.add(btn);
        }

        // Main Content Area
        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BorderLayout());
        mainContent.setBackground(new Color(248, 250, 252));

        // Header View
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(new EmptyBorder(25, 30, 25, 30));

        JLabel welcomeLabel = new JLabel("Welcome back to the System.");
        welcomeLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        headerPanel.add(welcomeLabel, BorderLayout.WEST);

        JLabel roleBadge = new JLabel("Role: " + SessionManager.getRole().toUpperCase());
        roleBadge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        roleBadge.setForeground(primaryBlue);
        headerPanel.add(roleBadge, BorderLayout.EAST);

        mainContent.add(headerPanel, BorderLayout.NORTH);
        mainContent.add(cardsPanel, BorderLayout.CENTER);

        // Assemble Frame
        add(sidebar, BorderLayout.WEST);
        add(mainContent, BorderLayout.CENTER);
    }

    private JPanel createDashboardOverview() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(248, 250, 252));
        panel.setBorder(new EmptyBorder(30, 30, 30, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 0.3;

        // Stats Cards (Row 1)
        gbc.gridx = 0;
        gbc.gridy = 0;
        lblTotalStudents = new JLabel("...");
        panel.add(createStatCard("Total Students", lblTotalStudents, new Color(79, 70, 229)), gbc);
        gbc.gridx = 1;
        lblActiveStaff = new JLabel("...");
        panel.add(createStatCard("Active Staff", lblActiveStaff, new Color(16, 185, 129)), gbc);
        gbc.gridx = 2;
        lblNewNotices = new JLabel("...");
        panel.add(createStatCard("New Notices", lblNewNotices, new Color(245, 158, 11)), gbc);

        fetchStats();

        // Info Text (Row 2)
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        gbc.weighty = 0.7;

        JTextArea infoText = new JTextArea(
                "System Operational Status: NORMAL\n\n" +
                        "You are currently logged in with " + SessionManager.getRole().toUpperCase() + " privileges.\n"
                        +
                        "The Faculty Management System provides integrated management for:\n" +
                        "• Unified Student Records & Academic Tracking\n" +
                        "• Staff Leave Management & Designations\n" +
                        "• Course Assignments & Syllabus Distribution\n" +
                        "• Centralized Notice Board Announcements");
        infoText.setEditable(false);
        infoText.setOpaque(false);
        infoText.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        infoText.setForeground(new Color(71, 85, 105));
        infoText.setBorder(new EmptyBorder(30, 0, 0, 0));

        panel.add(infoText, gbc);
        return panel;
    }

    private JPanel createStatCard(String title, JLabel valueLabel, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                new EmptyBorder(25, 25, 25, 25)));

        JLabel lTitle = new JLabel(title);
        lTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lTitle.setForeground(new Color(100, 116, 139));

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        valueLabel.setForeground(accent);

        card.add(lTitle, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private void fetchStats() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/v1/dashboard/data?type=stats"))
                        .header("Authorization", "Bearer " + SessionManager.getToken())
                        .GET()
                        .build();

                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    JsonNode node = new ObjectMapper().readTree(resp.body());
                    SwingUtilities.invokeLater(() -> {
                        lblTotalStudents.setText(node.path("students").asText("0"));
                        lblActiveStaff.setText(node.path("staff").asText("0"));
                        lblNewNotices.setText(node.path("notices").asText("0"));
                    });
                }
                return null;
            }
        };
        worker.execute();
    }

    private JPanel createTablePanel(String entityName, String[] columns, Object[][] data) {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBackground(new Color(248, 250, 252));

        JLabel titleLabel = new JLabel(entityName + " Records");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(new Color(30, 41, 59));
        panel.add(titleLabel, BorderLayout.NORTH);

        DefaultTableModel model = new DefaultTableModel(data, columns);
        JTable table = new JTable(model);
        table.setRowHeight(35);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        table.getTableHeader().setBackground(new Color(241, 245, 249));
        table.setShowGrid(true);
        table.setGridColor(new Color(226, 232, 240));

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);

        JButton btnRefresh = new JButton("Refresh " + entityName + "s");
        btnRefresh.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JButton btnAdd = new JButton("Add New " + entityName);
        btnAdd.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnAdd.setBackground(new Color(79, 70, 229));
        btnAdd.setForeground(Color.WHITE);

        btnRefresh.addActionListener(e -> fetchData(entityName, model));

        btnAdd.addActionListener(e -> {
            if ("User".equals(entityName)) {
                new org.example.fms.ui.dialogs.AddUserDialog(this).setVisible(true);
            } else if ("Student".equals(entityName)) {
                new org.example.fms.ui.dialogs.AddStudentDialog(this).setVisible(true);
            } else if ("Staff".equals(entityName)) {
                new org.example.fms.ui.dialogs.AddStaffDialog(this).setVisible(true);
            } else if ("Notice".equals(entityName)) {
                new org.example.fms.ui.dialogs.AddNoticeDialog(this).setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this,
                        "The " + entityName + " Add Module is scheduled for the next Development Phase.",
                        "Feature Pending", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        // Role-based visibility for Add New button
        String currentUserRole = org.example.fms.ui.SessionManager.getRole();
        if (!"super_admin".equalsIgnoreCase(currentUserRole) && !"admin".equalsIgnoreCase(currentUserRole)
                && !"staff".equalsIgnoreCase(currentUserRole)) {
            btnAdd.setVisible(false);
        }

        // Add row selection listener for detail popups
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                showDetailPopup(entityName, table);
            }
        });

        buttonPanel.add(btnRefresh);
        buttonPanel.add(btnAdd);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        // Fetch initial data on load
        fetchData(entityName, model);

        return panel;
    }

    private void fetchData(String entityName, DefaultTableModel model) {
        String type = "";
        if ("User".equals(entityName))
            type = "users";
        else if ("Student".equals(entityName))
            type = "students";
        else if ("Staff".equals(entityName))
            type = "staff";
        else if ("Subject".equals(entityName))
            type = "subjects";
        else if ("Leave".equals(entityName))
            type = "leaves";
        else if ("Notice".equals(entityName))
            type = "notices";
        else if ("My Attendance".equals(entityName))
            type = "my_attendance";
        else if ("My Subject".equals(entityName))
            type = "my_subjects";
        else if ("Learning Material".equals(entityName))
            type = "learning_materials";
        else if ("Library Book".equals(entityName))
            type = "library";
        else if ("My Result".equals(entityName))
            type = "my_results";

        final String finalType = type;

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                String token = SessionManager.getToken();
                if (token == null)
                    return null;

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/v1/dashboard/data?type=" + finalType))
                        .header("Authorization", "Bearer " + token)
                        .GET()
                        .build();

                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode arrayNode = mapper.readTree(resp.body());

                    SwingUtilities.invokeLater(() -> {
                        model.setRowCount(0); // Clear existing data
                        for (JsonNode row : arrayNode) {
                            if ("users".equals(finalType)) {
                                model.addRow(new Object[] { row.get("id").asText(), row.get("email").asText(),
                                        row.get("role").asText(), row.get("created_at").asText() });
                            } else if ("students".equals(finalType)) {
                                model.addRow(new Object[] { row.get("student_id").asText(), row.get("name").asText(),
                                        row.get("gender").asText(), row.get("program").asText() });
                            } else if ("staff".equals(finalType)) {
                                model.addRow(new Object[] { row.get("staff_id").asText(), row.get("name").asText(),
                                        row.get("designation").asText(), row.get("department").asText() });
                            } else if ("subjects".equals(finalType)) {
                                model.addRow(new Object[] { row.get("code").asText(), row.get("name").asText(),
                                        row.get("credits").asText(), row.get("type").asText() });
                            } else if ("leaves".equals(finalType)) {
                                model.addRow(new Object[] { row.get("id").asText(), row.get("staff_name").asText(),
                                        row.get("leave_type").asText(), row.get("dates").asText(),
                                        row.get("status").asText() });
                            } else if ("notices".equals(finalType)) {
                                model.addRow(new Object[] { row.get("id").asText(), row.get("title").asText(),
                                        row.get("audience").asText(), row.get("date").asText() });
                            } else if ("my_attendance".equals(finalType)) {
                                model.addRow(new Object[] { row.get("date").asText(), row.get("status").asText() });
                            } else if ("my_subjects".equals(finalType)) {
                                model.addRow(new Object[] { row.get("code").asText(), row.get("name").asText(),
                                        row.get("type").asText(), row.get("credits").asText() });
                            } else if ("learning_materials".equals(finalType)) {
                                model.addRow(new Object[] { row.get("title").asText(), row.get("type").asText(),
                                        row.get("subject").asText() });
                            } else if ("library".equals(finalType)) {
                                model.addRow(new Object[] { row.get("id").asText(), row.get("title").asText(),
                                        row.get("author").asText(), row.get("category").asText(),
                                        row.get("available").asText() });
                            } else if ("my_results".equals(finalType)) {
                                model.addRow(new Object[] { row.get("subject").asText(), row.get("marks").asText(),
                                        row.get("grade").asText(), row.get("type").asText() });
                            }
                        }
                    });
                }
                return null;
            }
        };
        worker.execute();
    }

    private void showDetailPopup(String entityName, JTable table) {
        int row = table.getSelectedRow();
        if (row == -1)
            return;

        StringBuilder details = new StringBuilder("<html><body style='width: 300px; padding: 10px;'>");
        details.append("<h2 style='color: #1e293b; border-bottom: 1px solid #e2e3e6;'>").append(entityName)
                .append(" Details</h2>");
        details.append("<table style='width: 100%;'>");

        for (int i = 0; i < table.getColumnCount(); i++) {
            details.append("<tr>")
                    .append("<td style='font-weight: bold; color: #4b5563; padding: 5px 0;'>")
                    .append(table.getColumnName(i)).append(":</td>")
                    .append("<td style='color: #1f2937; padding: 5px 0;'>")
                    .append(table.getValueAt(row, i)).append("</td>")
                    .append("</tr>");
        }

        details.append("</table>");
        details.append("<div style='margin-top: 20px; color: #6b7280; font-size: 0.9em;'>")
                .append("Record viewed securely on ").append(new java.util.Date())
                .append("</div>");
        details.append("</body></html>");

        JDialog dialog = new JDialog(this, entityName + " Information", true);
        dialog.setLayout(new BorderLayout());
        JLabel content = new JLabel(details.toString());
        content.setBorder(new EmptyBorder(10, 20, 20, 20));
        dialog.add(content, BorderLayout.CENTER);

        JButton btnClose = new JButton("Close");
        btnClose.addActionListener(e -> dialog.dispose());
        JPanel btnPanel = new JPanel();
        btnPanel.add(btnClose);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
}
