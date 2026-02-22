package org.example.fms.ui.dialogs;

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

public class SubmitAssignmentDialog extends JDialog {

    private String assignmentId;
    private JTextArea contentArea;
    private JButton submitBtn;

    public SubmitAssignmentDialog(JFrame parent, String assignmentId) {
        super(parent, "Submit Assignment", true);
        this.assignmentId = assignmentId;
        setSize(500, 400);
        setLocationRelativeTo(parent);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 20));
        mainPanel.setBorder(new EmptyBorder(25, 30, 25, 30));
        mainPanel.setBackground(Color.WHITE);

        JLabel header = new JLabel("Submit Your Work");
        header.setFont(new Font("Segoe UI", Font.BOLD, 22));
        mainPanel.add(header, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        centerPanel.add(new JLabel("Paste or type your assignment content below:"), BorderLayout.NORTH);

        contentArea = new JTextArea();
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        centerPanel.add(new JScrollPane(contentArea), BorderLayout.CENTER);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        submitBtn = new JButton("Submit Assignment");
        submitBtn.setBackground(new Color(79, 70, 229));
        submitBtn.setForeground(Color.WHITE);
        submitBtn.setPreferredSize(new Dimension(0, 45));
        submitBtn.addActionListener(e -> submitWork());
        mainPanel.add(submitBtn, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void submitWork() {
        if (contentArea.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Content cannot be empty!");
            return;
        }

        submitBtn.setEnabled(false);
        submitBtn.setText("Submitting...");

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                Map<String, String> payload = new HashMap<>();
                payload.put("assignment_id", assignmentId);
                payload.put("content", contentArea.getText());

                String json = new ObjectMapper().writeValueAsString(payload);
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/v1/dashboard/data?type=submissions"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + SessionManager.getToken())
                        .POST(HttpRequest.BodyPublishers.ofString(json)).build();

                HttpResponse<String> resp = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
                return resp.statusCode() == 201;
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        JOptionPane.showMessageDialog(SubmitAssignmentDialog.this,
                                "Assignment submitted successfully! Status updated to 'submitted'.");
                        dispose();
                    } else {
                        JOptionPane.showMessageDialog(SubmitAssignmentDialog.this,
                                "Failed to submit assignment. Please try again later.", "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(SubmitAssignmentDialog.this,
                            "Error during submission: " + e.getMessage());
                } finally {
                    submitBtn.setEnabled(true);
                    submitBtn.setText("Submit Assignment");
                }
            }
        };
        worker.execute();
    }
}
