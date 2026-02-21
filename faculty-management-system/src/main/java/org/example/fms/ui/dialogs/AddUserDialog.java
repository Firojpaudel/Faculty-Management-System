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

public class AddUserDialog extends JDialog {

    private JTextField emailField;
    private JPasswordField passwordField;
    private JComboBox<String> roleCombo;
    private JButton submitBtn;

    public AddUserDialog(JFrame parent) {
        super(parent, "Add New User", true);
        setSize(400, 350);
        setLocationRelativeTo(parent);
        setResizable(false);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(20, 30, 20, 30));

        JLabel title = new JLabel("Create System User");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel formPanel = new JPanel(new GridLayout(6, 1, 0, 5));

        formPanel.add(new JLabel("Email Address:"));
        emailField = new JTextField();
        formPanel.add(emailField);

        formPanel.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        formPanel.add(passwordField);

        formPanel.add(new JLabel("Assign Role:"));
        String[] roles = { "super_admin", "campus_chief", "department_head", "faculty", "admission_officer",
                "it_support" };
        roleCombo = new JComboBox<>(roles);
        formPanel.add(roleCombo);

        submitBtn = new JButton("Create User");
        submitBtn.setBackground(new Color(79, 70, 229));
        submitBtn.setForeground(Color.WHITE);
        submitBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        submitBtn.setFocusPainted(false);
        submitBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        submitBtn.addActionListener(e -> registerUser());

        mainPanel.add(title);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        mainPanel.add(formPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        mainPanel.add(submitBtn);

        add(mainPanel);
    }

    private void registerUser() {
        String email = emailField.getText();
        String password = new String(passwordField.getPassword());
        String role = (String) roleCombo.getSelectedItem();

        if (email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        submitBtn.setText("Submitting...");
        submitBtn.setEnabled(false);

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            private String error = "Unknown error";

            @Override
            protected Boolean doInBackground() {
                try {
                    String json = String.format("{\"email\":\"%s\",\"plainPassword\":\"%s\",\"role\":\"%s\"}", email,
                            password, role);
                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create("http://localhost:8080/api/v1/users"))
                            .header("Content-Type", "application/json")
                            .header("Authorization", "Bearer " + SessionManager.getToken())
                            .POST(HttpRequest.BodyPublishers.ofString(json))
                            .build();

                    HttpResponse<String> res = HttpClient.newHttpClient().send(req,
                            HttpResponse.BodyHandlers.ofString());
                    JsonNode node = new ObjectMapper().readTree(res.body());
                    if (node.path("success").asBoolean()) {
                        return true;
                    } else {
                        error = node.path("error").path("message").asText();
                        return false;
                    }
                } catch (Exception ex) {
                    error = ex.getMessage();
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        JOptionPane.showMessageDialog(AddUserDialog.this, "User Created Successfully!");
                        dispose();
                    } else {
                        JOptionPane.showMessageDialog(AddUserDialog.this, error, "Failed", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    submitBtn.setText("Create User");
                    submitBtn.setEnabled(true);
                }
            }
        };
        worker.execute();
    }
}
