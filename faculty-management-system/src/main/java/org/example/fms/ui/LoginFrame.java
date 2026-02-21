package org.example.fms.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class LoginFrame extends JFrame {

    private JTextField emailField;
    private JPasswordField passwordField;
    private JButton loginButton;

    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    public LoginFrame() {
        setTitle("Faculty Management System");
        setSize(450, 420);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Styling via FlatLaf properties
        Color primaryBlue = new Color(79, 70, 229);
        getRootPane().putClientProperty("JRootPane.titleBarBackground", primaryBlue);
        getRootPane().putClientProperty("JRootPane.titleBarForeground", Color.WHITE);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(40, 50, 40, 50));

        // Header
        JLabel titleLabel = new JLabel("System Login");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setForeground(primaryBlue);

        JLabel subtitleLabel = new JLabel("Faculty Management System");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitleLabel.setForeground(Color.GRAY);

        // Form Fields (Grid layout for neat labels on top)
        JPanel formPanel = new JPanel(new GridLayout(4, 1, 0, 8));
        formPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        formPanel.setMaximumSize(new Dimension(350, 140));
        formPanel.setOpaque(false);

        JLabel emailLabel = new JLabel("Email Address");
        emailLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));

        emailField = new JTextField();
        emailField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        emailField.putClientProperty("JTextField.placeholderText", "e.g., admin@faculty.edu");

        JLabel passwordLabel = new JLabel("Password");
        passwordLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));

        passwordField = new JPasswordField();
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passwordField.putClientProperty("JTextField.placeholderText", "••••••••");

        formPanel.add(emailLabel);
        formPanel.add(emailField);
        formPanel.add(passwordLabel);
        formPanel.add(passwordField);

        // Login Button
        loginButton = new JButton("SIGN IN");
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        loginButton.setForeground(Color.WHITE);
        loginButton.setBackground(primaryBlue);
        loginButton.setFocusPainted(false);
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(350, 40));

        loginButton.addActionListener(e -> attemptLogin());

        // Assembly
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        mainPanel.add(subtitleLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 35)));
        mainPanel.add(formPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 25)));
        mainPanel.add(loginButton);

        add(mainPanel);
    }

    private void attemptLogin() {
        String email = emailField.getText();
        String password = new String(passwordField.getPassword());

        if (email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter both email and password.", "Required Fields",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        loginButton.setText("Signing in...");
        loginButton.setEnabled(false);

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            private String errorMessage = "Unknown Error";

            @Override
            protected Boolean doInBackground() {
                try {
                    String jsonBody = String.format("{\"email\":\"%s\", \"password\":\"%s\"}", email, password);

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("http://localhost:8080/api/v1/auth/login"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                            .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 404) {
                        errorMessage = "Auth API not found. Tomcat is still starting up, please wait a moment.";
                        return false;
                    }

                    // Strict parsing to catch nested Jackson errors
                    JsonNode rootNode = mapper.readTree(response.body());

                    if (rootNode.path("success").asBoolean()) {
                        String token = rootNode.path("data").path("token").asText();
                        String role = rootNode.path("data").path("role").asText();
                        SessionManager.setToken(token);
                        SessionManager.setRole(role);
                        return true;
                    } else {
                        errorMessage = rootNode.path("error").path("message").asText();
                        return false;
                    }
                } catch (Exception ex) {
                    errorMessage = "Server error or connection failed. Details: " + ex.getMessage();
                    ex.printStackTrace();
                    return false;
                } catch (Throwable t) {
                    errorMessage = "Critical Error: " + t.getMessage();
                    t.printStackTrace();
                    return false;
                }
            }

            @Override
            protected void done() {
                loginButton.setText("SIGN IN");
                loginButton.setEnabled(true);
                try {
                    if (get()) {
                        DashboardFrame dashboard = new DashboardFrame();
                        dashboard.setVisible(true);
                        LoginFrame.this.dispose();
                    } else {
                        JOptionPane.showMessageDialog(LoginFrame.this, errorMessage, "Authentication Failed",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(LoginFrame.this, "UI Exception: " + ex.getMessage(), "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }
}
