package com.tamaygz.colorfuldiag.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;

import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;

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

        // Color picker with preview
        colorPickerPanel = new JPanel(new BorderLayout());

        // Create a color preview panel showing old vs new
        JPanel colorPreview = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                int halfWidth = getWidth() / 2;
                // Old color on left
                g.setColor(initialColor);
                g.fillRect(0, 0, halfWidth, getHeight());
                // New color on right
                g.setColor(selectedColor);
                g.fillRect(halfWidth, 0, halfWidth, getHeight());
                // Border
                g.setColor(Color.BLACK);
                g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                g.drawLine(halfWidth, 0, halfWidth, getHeight());
                // Labels
                g.setColor(getContrastColor(initialColor));
                g.drawString("Old", 5, getHeight() / 2);
                g.setColor(getContrastColor(selectedColor));
                g.drawString("New", halfWidth + 5, getHeight() / 2);
            }
        };
        colorPreview.setPreferredSize(new Dimension(100, 60));

        // RGB sliders with hex sync
        JTextField hexField = new JTextField(6);
        JSlider redSlider = new JSlider(0, 255, selectedColor.getRed());
        JSlider greenSlider = new JSlider(0, 255, selectedColor.getGreen());
        JSlider blueSlider = new JSlider(0, 255, selectedColor.getBlue());
        
        // Update helper that syncs all components
        Runnable updateFromSliders = () -> {
            selectedColor = new Color(redSlider.getValue(), greenSlider.getValue(), blueSlider.getValue());
            hexField.setText(String.format("%02X%02X%02X",
                    selectedColor.getRed(), selectedColor.getGreen(), selectedColor.getBlue()));
            colorPreview.repaint();
        };
        
        // Slider listeners
        redSlider.addChangeListener(e -> {
            if (!redSlider.getValueIsAdjusting() || redSlider.getValueIsAdjusting()) {
                updateFromSliders.run();
            }
        });
        greenSlider.addChangeListener(e -> {
            if (!greenSlider.getValueIsAdjusting() || greenSlider.getValueIsAdjusting()) {
                updateFromSliders.run();
            }
        });
        blueSlider.addChangeListener(e -> {
            if (!blueSlider.getValueIsAdjusting() || blueSlider.getValueIsAdjusting()) {
                updateFromSliders.run();
            }
        });

        // Hex input that syncs to sliders
        hexField.setText(String.format("%02X%02X%02X",
                selectedColor.getRed(), selectedColor.getGreen(), selectedColor.getBlue()));
        hexField.addActionListener(e -> updateFromHex(hexField, redSlider, greenSlider, blueSlider, colorPreview));
        hexField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                updateFromHex(hexField, redSlider, greenSlider, blueSlider, colorPreview);
            }
        });

        JPanel slidersPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        slidersPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        slidersPanel.add(new JLabel("Red:"));
        slidersPanel.add(redSlider);
        slidersPanel.add(new JLabel("Green:"));
        slidersPanel.add(greenSlider);
        slidersPanel.add(new JLabel("Blue:"));
        slidersPanel.add(blueSlider);

        colorPickerPanel.add(colorPreview, BorderLayout.WEST);
        colorPickerPanel.add(slidersPanel, BorderLayout.CENTER);

        mainPanel.add(colorPickerPanel, BorderLayout.CENTER);

        // Hex input panel
        JPanel hexPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        hexPanel.add(new JLabel("Hex: #"));
        hexPanel.add(hexField);
        mainPanel.add(hexPanel, BorderLayout.SOUTH);

        return mainPanel;
    }
    
    private void updateFromHex(JTextField hexField, JSlider redSlider, JSlider greenSlider, 
                                JSlider blueSlider, JPanel colorPreview) {
        try {
            String hex = hexField.getText().trim();
            if (hex.startsWith("#")) {
                hex = hex.substring(1);
            }
            if (hex.length() == 6) {
                Color newColor = Color.decode("#" + hex);
                selectedColor = newColor;
                // Update sliders without triggering their listeners (use setValue directly)
                redSlider.setValue(newColor.getRed());
                greenSlider.setValue(newColor.getGreen());
                blueSlider.setValue(newColor.getBlue());
                colorPreview.repaint();
            }
        } catch (NumberFormatException ex) {
            // Invalid hex, ignore
        }
    }
    
    private Color getContrastColor(Color color) {
        // Calculate luminance and return black or white for contrast
        double luminance = (0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue()) / 255;
        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
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
                    // Refresh the entire panel to update sliders and preview
                    colorPickerPanel.repaint();
                    getContentPane().repaint();
                }
            });
            panel.add(colorButton);
        }

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
