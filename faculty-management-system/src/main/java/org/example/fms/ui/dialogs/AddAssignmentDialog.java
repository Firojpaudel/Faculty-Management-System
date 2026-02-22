package org.example.fms.ui.dialogs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.fms.ui.SessionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public class AddAssignmentDialog extends JDialog {

    private JComboBox<ComboItem> subjectBox;
    private JTextField titleField;
    private JTextArea descArea;
    private JTextField deadlineField;
    private JButton submitBtn;

    public AddAssignmentDialog(JFrame parent) {
        super(parent, "Create New Assignment", true);
        setSize(500, 600);
        setLocationRelativeTo(parent);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 20));
        mainPanel.setBorder(new EmptyBorder(25, 30, 25, 30));
        mainPanel.setBackground(Color.WHITE);

        JLabel header = new JLabel("New Assignment");
        header.setFont(new Font("Segoe UI", Font.BOLD, 22));
        mainPanel.add(header, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 15, 5);

        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(new JLabel("Subject:"), gbc);
        gbc.gridx = 1;
        subjectBox = new JComboBox<>();
        loadSubjects();
        form.add(subjectBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        form.add(new JLabel("Title:"), gbc);
        gbc.gridx = 1;
        titleField = new JTextField();
        form.add(titleField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        form.add(new JLabel("Deadline (YYYY-MM-DD HH:mm:ss):"), gbc);
        gbc.gridx = 1;
        deadlineField = new JTextField("2080-12-30 23:59:59");
        form.add(deadlineField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        form.add(new JLabel("Description:"), gbc);
        gbc.gridy = 4;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        descArea = new JTextArea();
        descArea.setLineWrap(true);
        form.add(new JScrollPane(descArea), gbc);

        mainPanel.add(form, BorderLayout.CENTER);

        submitBtn = new JButton("Create Assignment");
        submitBtn.setBackground(new Color(79, 70, 229));
        submitBtn.setForeground(Color.WHITE);
        submitBtn.setPreferredSize(new Dimension(0, 45));
        submitBtn.addActionListener(e -> createAssignment());
        mainPanel.add(submitBtn, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void loadSubjects() {
        SwingWorker<JsonNode, Void> worker = new SwingWorker<>() {
            @Override
            protected JsonNode doInBackground() throws Exception {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/v1/dashboard/data?type=faculty_classes"))
                        .header("Authorization", "Bearer " + SessionManager.getToken())
                        .GET().build();
                HttpResponse<String> resp = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
                return new ObjectMapper().readTree(resp.body());
            }

            @Override
            protected void done() {
                try {
                    JsonNode nodes = get();
                    for (JsonNode node : nodes) {
                        subjectBox.addItem(
                                new ComboItem(node.get("subject_name").asText(), node.get("subject_id").asText()));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }

    private void createAssignment() {
        submitBtn.setEnabled(false);
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                Map<String, String> payload = new HashMap<>();
                payload.put("subject_id", ((ComboItem) subjectBox.getSelectedItem()).getValue());
                payload.put("title", titleField.getText());
                payload.put("description", descArea.getText());
                payload.put("deadline", deadlineField.getText());

                String json = new ObjectMapper().writeValueAsString(payload);
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/v1/dashboard/data?type=assignments"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + SessionManager.getToken())
                        .POST(HttpRequest.BodyPublishers.ofString(json)).build();
                return HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString()).statusCode() == 201;
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        JOptionPane.showMessageDialog(AddAssignmentDialog.this, "Assignment created!");
                        dispose();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                submitBtn.setEnabled(true);
            }
        };
        worker.execute();
    }

    private static class ComboItem {
        private String label;
        private String value;

        public ComboItem(String label, String value) {
            this.label = label;
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
