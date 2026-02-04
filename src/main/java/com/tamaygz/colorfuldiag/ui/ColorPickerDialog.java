package com.tamaygz.colorfuldiag.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.ColorPicker;
import com.intellij.ui.ColorPickerListener;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog for selecting a color for tables or other elements.
 */
public class ColorPickerDialog extends DialogWrapper {
    private Color selectedColor;
    private final Color initialColor;
    private JPanel colorPickerPanel;

    // Preset colors for quick selection
    private static final Color[] PRESET_COLORS = {
            new Color(0xFF6B6B), // Red
            new Color(0x4ECDC4), // Teal
            new Color(0x45B7D1), // Blue
            new Color(0x96CEB4), // Green
            new Color(0xFECE2F), // Yellow
            new Color(0xFF9F43), // Orange
            new Color(0xA55EEA), // Purple
            new Color(0xFD79A8), // Pink
            new Color(0x00B894), // Emerald
            new Color(0x74B9FF), // Light Blue
            new Color(0xDFE6E9), // Light Gray
            new Color(0x636E72), // Dark Gray
    };

    public ColorPickerDialog(@Nullable Project project, @Nullable Color initialColor) {
        super(project);
        this.initialColor = initialColor != null ? initialColor : Color.WHITE;
        this.selectedColor = this.initialColor;
        setTitle("Select Color");
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setPreferredSize(new Dimension(400, 350));

        // Preset colors panel
        JPanel presetPanel = createPresetColorsPanel();
        mainPanel.add(presetPanel, BorderLayout.NORTH);

        // Color picker
        colorPickerPanel = new JPanel(new BorderLayout());
        JPanel pickerWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));

        // Create a simple color preview and HSB sliders
        JPanel colorPreview = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(selectedColor);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Color.BLACK);
                g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            }
        };
        colorPreview.setPreferredSize(new Dimension(100, 60));

        // RGB sliders
        JPanel slidersPanel = createRGBSlidersPanel(colorPreview);

        colorPickerPanel.add(colorPreview, BorderLayout.WEST);
        colorPickerPanel.add(slidersPanel, BorderLayout.CENTER);

        mainPanel.add(colorPickerPanel, BorderLayout.CENTER);

        // Hex input
        JPanel hexPanel = createHexInputPanel(colorPreview);
        mainPanel.add(hexPanel, BorderLayout.SOUTH);

        return mainPanel;
    }

    private JPanel createPresetColorsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Preset Colors"));

        for (Color presetColor : PRESET_COLORS) {
            JButton colorButton = new JButton();
            colorButton.setPreferredSize(new Dimension(28, 28));
            colorButton.setBackground(presetColor);
            colorButton.setOpaque(true);
            colorButton.setBorderPainted(true);
            colorButton.setFocusPainted(false);
            colorButton.addActionListener(e -> {
                selectedColor = presetColor;
                if (colorPickerPanel != null) {
                    colorPickerPanel.repaint();
                }
            });
            panel.add(colorButton);
        }

        return panel;
    }

    private JPanel createRGBSlidersPanel(JPanel colorPreview) {
        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JSlider redSlider = new JSlider(0, 255, selectedColor.getRed());
        JSlider greenSlider = new JSlider(0, 255, selectedColor.getGreen());
        JSlider blueSlider = new JSlider(0, 255, selectedColor.getBlue());

        Runnable updateColor = () -> {
            selectedColor = new Color(redSlider.getValue(), greenSlider.getValue(), blueSlider.getValue());
            colorPreview.repaint();
        };

        redSlider.addChangeListener(e -> updateColor.run());
        greenSlider.addChangeListener(e -> updateColor.run());
        blueSlider.addChangeListener(e -> updateColor.run());

        panel.add(new JLabel("Red:"));
        panel.add(redSlider);
        panel.add(new JLabel("Green:"));
        panel.add(greenSlider);
        panel.add(new JLabel("Blue:"));
        panel.add(blueSlider);

        return panel;
    }

    private JPanel createHexInputPanel(JPanel colorPreview) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel("Hex: #"));

        JTextField hexField = new JTextField(6);
        hexField.setText(String.format("%02X%02X%02X",
                selectedColor.getRed(), selectedColor.getGreen(), selectedColor.getBlue()));

        hexField.addActionListener(e -> {
            try {
                String hex = hexField.getText().trim();
                if (hex.startsWith("#")) {
                    hex = hex.substring(1);
                }
                selectedColor = Color.decode("#" + hex);
                colorPreview.repaint();
            } catch (NumberFormatException ex) {
                // Invalid hex, ignore
            }
        });

        panel.add(hexField);

        return panel;
    }

    public Color getSelectedColor() {
        return selectedColor;
    }

    public String getSelectedColorHex() {
        return String.format("#%02X%02X%02X",
                selectedColor.getRed(), selectedColor.getGreen(), selectedColor.getBlue());
    }

    public static Color showDialog(@Nullable Project project, @Nullable Color initialColor) {
        ColorPickerDialog dialog = new ColorPickerDialog(project, initialColor);
        if (dialog.showAndGet()) {
            return dialog.getSelectedColor();
        }
        return null;
    }
}
