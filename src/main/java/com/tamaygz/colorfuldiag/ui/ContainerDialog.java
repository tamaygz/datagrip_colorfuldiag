package com.tamaygz.colorfuldiag.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.tamaygz.colorfuldiag.model.ContainerInfo;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog for creating or editing a container.
 */
public class ContainerDialog extends DialogWrapper {
    private JTextField titleField;
    private JButton colorButton;
    private Color selectedColor;

    private static final Color DEFAULT_COLOR = new Color(0x45B7D1);

    // Preset colors for containers
    private static final Color[] CONTAINER_COLORS = {
            new Color(0x45B7D1), // Blue
            new Color(0x4ECDC4), // Teal
            new Color(0x96CEB4), // Green
            new Color(0xFF6B6B), // Red
            new Color(0xFECE2F), // Yellow
            new Color(0xA55EEA), // Purple
            new Color(0xFF9F43), // Orange
            new Color(0xFD79A8), // Pink
    };

    public ContainerDialog(@Nullable Project project) {
        this(project, null);
    }

    public ContainerDialog(@Nullable Project project, @Nullable ContainerInfo existingContainer) {
        super(project);
        this.selectedColor = existingContainer != null && existingContainer.getAwtColor() != null
                ? existingContainer.getAwtColor()
                : DEFAULT_COLOR;
        setTitle(existingContainer != null ? "Edit Container" : "Create Container");
        init();

        if (existingContainer != null) {
            titleField.setText(existingContainer.getTitle());
        }
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title field
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Title:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        titleField = new JTextField(20);
        titleField.setText("New Container");
        panel.add(titleField, gbc);

        // Color selection
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        panel.add(new JLabel("Color:"), gbc);

        gbc.gridx = 1;
        JPanel colorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

        colorButton = new JButton();
        colorButton.setPreferredSize(new Dimension(40, 25));
        colorButton.setBackground(selectedColor);
        colorButton.setOpaque(true);
        colorButton.setFocusPainted(false);
        colorButton.addActionListener(e -> {
            Color newColor = ColorPickerDialog.showDialog(null, selectedColor);
            if (newColor != null) {
                selectedColor = newColor;
                colorButton.setBackground(selectedColor);
            }
        });
        colorPanel.add(colorButton);

        // Quick color buttons
        for (Color color : CONTAINER_COLORS) {
            JButton quickColorBtn = new JButton();
            quickColorBtn.setPreferredSize(new Dimension(20, 20));
            quickColorBtn.setBackground(color);
            quickColorBtn.setOpaque(true);
            quickColorBtn.setBorderPainted(true);
            quickColorBtn.setFocusPainted(false);
            quickColorBtn.addActionListener(e -> {
                selectedColor = color;
                colorButton.setBackground(selectedColor);
            });
            colorPanel.add(quickColorBtn);
        }

        panel.add(colorPanel, gbc);

        panel.setPreferredSize(new Dimension(400, 100));
        return panel;
    }

    public String getContainerTitle() {
        return titleField.getText().trim();
    }

    public Color getContainerColor() {
        return selectedColor;
    }

    public String getContainerColorHex() {
        return String.format("#%02X%02X%02X",
                selectedColor.getRed(), selectedColor.getGreen(), selectedColor.getBlue());
    }

    public ContainerInfo createContainerInfo(int x, int y) {
        return new ContainerInfo(
                getContainerTitle(),
                getContainerColorHex(),
                x, y, 300, 200
        );
    }

    @Override
    protected void doOKAction() {
        if (getContainerTitle().isEmpty()) {
            titleField.setText("Container");
        }
        super.doOKAction();
    }
}
