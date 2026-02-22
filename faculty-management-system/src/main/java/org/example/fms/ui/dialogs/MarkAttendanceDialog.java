package org.example.fms.ui.dialogs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.fms.ui.SessionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MarkAttendanceDialog extends JDialog {

    private String subjectId;
    private JPanel studentListPanel;
    private List<AttendanceRow> rows = new ArrayList<>();
    private JButton submitBtn;

    public MarkAttendanceDialog(JFrame parent, String subjectId) {
        super(parent, "Mark Attendance", true);
        this.subjectId = subjectId;
        setSize(500, 600);
        setLocationRelativeTo(parent);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 20));
        mainPanel.setBorder(new EmptyBorder(25, 30, 25, 30));
        mainPanel.setBackground(Color.WHITE);

        JLabel header = new JLabel("Class Attendance - " + new SimpleDateFormat("MMM dd, yyyy").format(new Date()));
        header.setFont(new Font("Segoe UI", Font.BOLD, 18));
        header.setForeground(new Color(30, 41, 59));
        mainPanel.add(header, BorderLayout.NORTH);

        studentListPanel = new JPanel();
        studentListPanel.setLayout(new BoxLayout(studentListPanel, BoxLayout.Y_AXIS));
        studentListPanel.setBackground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(studentListPanel);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        submitBtn = new JButton("Submit Attendance");
        submitBtn.setBackground(new Color(16, 185, 129));
        submitBtn.setForeground(Color.WHITE);
        submitBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        submitBtn.setPreferredSize(new Dimension(0, 45));
        submitBtn.addActionListener(e -> submitAttendance());
        mainPanel.add(submitBtn, BorderLayout.SOUTH);

        add(mainPanel);
        loadStudents();
    }

    private void loadStudents() {
        SwingWorker<JsonNode, Void> worker = new SwingWorker<>() {
            @Override
            protected JsonNode doInBackground() throws Exception {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/v1/dashboard/data?type=class_students&subject_id="
                                + subjectId))
                        .header("Authorization", "Bearer " + SessionManager.getToken())
                        .GET().build();
                HttpResponse<String> resp = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
                return new ObjectMapper().readTree(resp.body());
            }

            @Override
            protected void done() {
                try {
                    JsonNode nodes = get();
                    studentListPanel.removeAll();
                    rows.clear();
                    for (JsonNode node : nodes) {
                        AttendanceRow row = new AttendanceRow(node.get("name").asText(), node.get("id").asText());
                        rows.add(row);
                        studentListPanel.add(row);
                    }
                    studentListPanel.revalidate();
                    studentListPanel.repaint();
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(MarkAttendanceDialog.this, "Failed to load student list.");
                }
            }
        };
        worker.execute();
    }

    private void submitAttendance() {
        submitBtn.setEnabled(false);
        submitBtn.setText("Processing...");

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                ObjectMapper mapper = new ObjectMapper();
                ObjectNode root = mapper.createObjectNode();
                root.put("date", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
                ArrayNode atts = mapper.createArrayNode();
                for (AttendanceRow row : rows) {
                    ObjectNode att = mapper.createObjectNode();
                    att.put("student_id", row.getStudentId());
                    att.put("status", row.getStatus());
                    atts.add(att);
                }
                root.set("attendances", atts);

                String json = mapper.writeValueAsString(root);
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/v1/dashboard/data?type=mark_attendance"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + SessionManager.getToken())
                        .POST(HttpRequest.BodyPublishers.ofString(json)).build();
                return HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString()).statusCode() == 201;
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        JOptionPane.showMessageDialog(MarkAttendanceDialog.this, "Attendance marked successfully!");
                        dispose();
                    } else {
                        JOptionPane.showMessageDialog(MarkAttendanceDialog.this, "Failed to mark attendance.");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                submitBtn.setEnabled(true);
                submitBtn.setText("Submit Attendance");
            }
        };
        worker.execute();
    }

    private static class AttendanceRow extends JPanel {
        private String studentId;
        private JRadioButton rbPresent, rbAbsent;

        public AttendanceRow(String name, String studentId) {
            this.studentId = studentId;
            setLayout(new BorderLayout());
            setBackground(Color.WHITE);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)),
                    BorderFactory.createEmptyBorder(10, 15, 10, 15)));

            JLabel lblName = new JLabel(name);
            lblName.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            add(lblName, BorderLayout.CENTER);

            JPanel options = new JPanel();
            options.setOpaque(false);
            rbPresent = new JRadioButton("P", true);
            rbAbsent = new JRadioButton("A");
            rbPresent.setOpaque(false);
            rbAbsent.setOpaque(false);

            ButtonGroup bg = new ButtonGroup();
            bg.add(rbPresent);
            bg.add(rbAbsent);

            options.add(rbPresent);
            options.add(rbAbsent);
            add(options, BorderLayout.EAST);
        }

        public String getStudentId() {
            return studentId;
        }

        public String getStatus() {
            return rbPresent.isSelected() ? "present" : "absent";
        }
    }
}
