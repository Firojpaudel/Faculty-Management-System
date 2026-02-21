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

public class AddNoticeDialog extends JDialog {

    private JTextField titleField;
    private JTextArea contentArea;
    private JComboBox<String> audienceBox;
    private JButton submitBtn;

    public AddNoticeDialog(JFrame parent) {
        super(parent, "Publish New Notice", true);
        setSize(500, 500);
        setLocationRelativeTo(parent);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 20));
        mainPanel.setBorder(new EmptyBorder(25, 30, 25, 30));
        mainPanel.setBackground(Color.WHITE);

        JLabel header = new JLabel("Create Announcement");
        header.setFont(new Font("Segoe UI", Font.BOLD, 20));
        header.setForeground(new Color(30, 41, 59));
        mainPanel.add(header, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 15, 5);

        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(new JLabel("Notice Title:"), gbc);
        gbc.gridx = 1;
        titleField = new JTextField();
        form.add(titleField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        form.add(new JLabel("Target Audience:"), gbc);
        gbc.gridx = 1;
        audienceBox = new JComboBox<>(new String[] { "all", "staff", "students" });
        form.add(audienceBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        form.add(new JLabel("Notice Content:"), gbc);

        gbc.gridy = 3;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        contentArea = new JTextArea();
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        form.add(new JScrollPane(contentArea), gbc);

        mainPanel.add(form, BorderLayout.CENTER);

        submitBtn = new JButton("Publish Notice");
        submitBtn.setBackground(new Color(79, 70, 229));
        submitBtn.setForeground(Color.WHITE);
        submitBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        submitBtn.setPreferredSize(new Dimension(0, 40));
        submitBtn.addActionListener(e -> publishNotice());
        mainPanel.add(submitBtn, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void publishNotice() {
        if (titleField.getText().isEmpty() || contentArea.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Title and Content cannot be empty!");
            return;
        }

        submitBtn.setEnabled(false);
        submitBtn.setText("Publishing...");

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                var payload = new java.util.HashMap<String, String>();
                payload.put("title", titleField.getText());
                payload.put("content", contentArea.getText());
                payload.put("target_audience", (String) audienceBox.getSelectedItem());

                String json = new ObjectMapper().writeValueAsString(payload);

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/v1/dashboard/data?type=notices"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + SessionManager.getToken())
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                HttpResponse<String> res = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
                return res.statusCode() == 201;
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        JOptionPane.showMessageDialog(AddNoticeDialog.this, "Notice published successfully!");
                        dispose();
                    } else {
                        JOptionPane.showMessageDialog(AddNoticeDialog.this, "Failed to publish notice.", "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(AddNoticeDialog.this, "Error: " + e.getMessage());
                } finally {
                    submitBtn.setEnabled(true);
                    submitBtn.setText("Publish Notice");
                }
            }
        };
        worker.execute();
    }
}
