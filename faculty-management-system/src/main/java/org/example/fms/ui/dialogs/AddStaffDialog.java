package org.example.fms.ui.dialogs;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class AddStaffDialog extends JDialog {

    private JTextField fNameEn, shiftType, staffType, employmentType, deptId;
    private JButton submitBtn;

    public AddStaffDialog(JFrame parent) {
        super(parent, "Add New Staff", true);
        setSize(500, 400);
        setLocationRelativeTo(parent);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(20, 30, 20, 30));

        JLabel title = new JLabel("Staff Registration (Phase 2 Preview)");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Currently under active API construction.");
        subtitle.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        subtitle.setForeground(Color.RED);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel formPanel = new JPanel(new GridLayout(6, 2, 10, 10));

        formPanel.add(new JLabel("Full Name (EN):"));
        fNameEn = new JTextField();
        formPanel.add(fNameEn);
        formPanel.add(new JLabel("Staff Type:"));
        staffType = new JTextField("teaching");
        formPanel.add(staffType);
        formPanel.add(new JLabel("Employment Type:"));
        employmentType = new JTextField("permanent");
        formPanel.add(employmentType);
        formPanel.add(new JLabel("Shift Type:"));
        shiftType = new JTextField("day");
        formPanel.add(shiftType);
        formPanel.add(new JLabel("Department ID:"));
        deptId = new JTextField();
        formPanel.add(deptId);

        submitBtn = new JButton("Save Record Locally");
        submitBtn.setBackground(new Color(79, 70, 229));
        submitBtn.setForeground(Color.WHITE);
        submitBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        submitBtn.setFocusPainted(false);
        submitBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        submitBtn.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                    "The backend API endpoint for Staff Registration (/api/v1/staff) will be implemented in the next phase! For now, this acts as a UI mock.",
                    "Development Notice", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        });

        mainPanel.add(title);
        mainPanel.add(subtitle);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        mainPanel.add(formPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        mainPanel.add(submitBtn);

        add(mainPanel);
    }
}
