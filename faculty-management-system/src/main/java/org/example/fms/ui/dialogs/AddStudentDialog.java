package org.example.fms.ui.dialogs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.fms.core.api.dto.StudentProfileDTO;
import org.example.fms.ui.SessionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AddStudentDialog extends JDialog {

    private JTextField fNameEn, dob, gender, nationality, address, phone, gName, gPhone, gRel, email, programId;
    private JButton submitBtn;

    public AddStudentDialog(JFrame parent) {
        super(parent, "Add New Student", true);
        setSize(800, 600);
        setLocationRelativeTo(parent);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(20, 30, 20, 30));

        JLabel title = new JLabel("Enroll New Student");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel formPanel = new JPanel(new GridLayout(6, 4, 15, 15));

        formPanel.add(new JLabel("Full Name (EN):"));
        fNameEn = new JTextField();
        formPanel.add(fNameEn);
        formPanel.add(new JLabel("Date of Birth (YYYY-MM-DD):"));
        dob = new JTextField();
        formPanel.add(dob);
        formPanel.add(new JLabel("Gender:"));
        gender = new JTextField();
        formPanel.add(gender);
        formPanel.add(new JLabel("Nationality:"));
        nationality = new JTextField("Nepalese");
        formPanel.add(nationality);
        formPanel.add(new JLabel("Address:"));
        address = new JTextField();
        formPanel.add(address);
        formPanel.add(new JLabel("Phone:"));
        phone = new JTextField();
        formPanel.add(phone);
        formPanel.add(new JLabel("Email Address:"));
        email = new JTextField();
        formPanel.add(email);
        formPanel.add(new JLabel("Guardian Name:"));
        gName = new JTextField();
        formPanel.add(gName);
        formPanel.add(new JLabel("Guardian Phone:"));
        gPhone = new JTextField();
        formPanel.add(gPhone);
        formPanel.add(new JLabel("Guardian Relation:"));
        gRel = new JTextField();
        formPanel.add(gRel);
        formPanel.add(new JLabel("Program ID:"));
        programId = new JTextField();
        formPanel.add(programId);

        submitBtn = new JButton("Enroll Student");
        submitBtn.setBackground(new Color(79, 70, 229));
        submitBtn.setForeground(Color.WHITE);
        submitBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        submitBtn.setFocusPainted(false);
        submitBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        submitBtn.addActionListener(e -> registerStudent());

        mainPanel.add(title);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        mainPanel.add(formPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        mainPanel.add(submitBtn);

        add(mainPanel);
    }

    private void registerStudent() {
        if (fNameEn.getText().isEmpty() || dob.getText().isEmpty() || phone.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill required fields (Name, DOB, Phone).");
            return;
        }

        submitBtn.setText("Submitting...");
        submitBtn.setEnabled(false);

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            private String error = "Unknown error";

            @Override
            protected Boolean doInBackground() {
                try {
                    StudentProfileDTO dto = new StudentProfileDTO();
                    dto.setFullNameEn(fNameEn.getText());
                    dto.setDateOfBirth(java.sql.Date.valueOf(dob.getText()));
                    dto.setGender(gender.getText());
                    dto.setNationality(nationality.getText());
                    dto.setAddress(address.getText());
                    dto.setPhone(phone.getText());
                    dto.setEmail(email.getText().isEmpty() ? null : email.getText());
                    dto.setGuardianName(gName.getText());
                    dto.setGuardianPhone(gPhone.getText());
                    dto.setGuardianRelationship(gRel.getText());
                    dto.setProgramId(programId.getText().isEmpty() ? null : programId.getText());

                    ObjectMapper mapper = new ObjectMapper();
                    String json = mapper.writeValueAsString(dto);

                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create("http://localhost:8080/api/v1/students"))
                            .header("Content-Type", "application/json")
                            .header("Authorization", "Bearer " + SessionManager.getToken())
                            .POST(HttpRequest.BodyPublishers.ofString(json))
                            .build();

                    HttpResponse<String> res = HttpClient.newHttpClient().send(req,
                            HttpResponse.BodyHandlers.ofString());
                    JsonNode node = mapper.readTree(res.body());
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
                        JOptionPane.showMessageDialog(AddStudentDialog.this, "Student Enrolled Successfully!");
                        dispose();
                    } else {
                        JOptionPane.showMessageDialog(AddStudentDialog.this, error, "Failed",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    submitBtn.setText("Enroll Student");
                    submitBtn.setEnabled(true);
                }
            }
        };
        worker.execute();
    }
}
