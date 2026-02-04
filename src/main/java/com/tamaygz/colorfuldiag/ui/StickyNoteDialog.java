package com.tamaygz.colorfuldiag.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.tamaygz.colorfuldiag.model.StickyNoteInfo;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog for creating or editing a sticky note.
 */
public class StickyNoteDialog extends DialogWrapper {
    private JTextArea textArea;
    private JButton colorButton;
    private Color selectedColor;

    private static final Color DEFAULT_COLOR = new Color(0xFFEAA7);

    // Preset colors for sticky notes
    private static final Color[] NOTE_COLORS = {
            new Color(0xFFEAA7), // Yellow
            new Color(0xFFB3BA), // Pink
            new Color(0xBAE1FF), // Light Blue
            new Color(0xBAFFBA), // Light Green
            new Color(0xFFDFBA), // Peach
            new Color(0xE2BAFF), // Lavender
            new Color(0xFFFFBA), // Pale Yellow
            new Color(0xBAFFFF), // Cyan
    };

    public StickyNoteDialog(@Nullable Project project) {
        this(project, null);
    }

    public StickyNoteDialog(@Nullable Project project, @Nullable StickyNoteInfo existingNote) {
        super(project);
        this.selectedColor = existingNote != null && existingNote.getAwtColor() != null
                ? existingNote.getAwtColor()
                : DEFAULT_COLOR;
        setTitle(existingNote != null ? "Edit Sticky Note" : "Add Sticky Note");
        init();

        if (existingNote != null && existingNote.getText() != null) {
            textArea.setText(existingNote.getText());
        }
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        // Color selection at top
        JPanel colorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        colorPanel.add(new JLabel("Color:"));

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
                textArea.setBackground(selectedColor);
            }
        });
        colorPanel.add(colorButton);

        // Quick color buttons
        for (Color color : NOTE_COLORS) {
            JButton quickColorBtn = new JButton();
            quickColorBtn.setPreferredSize(new Dimension(20, 20));
            quickColorBtn.setBackground(color);
            quickColorBtn.setOpaque(true);
            quickColorBtn.setBorderPainted(true);
            quickColorBtn.setFocusPainted(false);
            quickColorBtn.addActionListener(e -> {
                selectedColor = color;
                colorButton.setBackground(selectedColor);
                textArea.setBackground(selectedColor);
            });
            colorPanel.add(quickColorBtn);
        }

        panel.add(colorPanel, BorderLayout.NORTH);

        // Text area
        textArea = new JTextArea(5, 30);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBackground(selectedColor);
        textArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(350, 150));
        panel.add(scrollPane, BorderLayout.CENTER);

        panel.setPreferredSize(new Dimension(400, 220));
        return panel;
    }

    public String getNoteText() {
        return textArea.getText();
    }

    public Color getNoteColor() {
        return selectedColor;
    }

    public String getNoteColorHex() {
        return String.format("#%02X%02X%02X",
                selectedColor.getRed(), selectedColor.getGreen(), selectedColor.getBlue());
    }

    public StickyNoteInfo createStickyNoteInfo(int x, int y) {
        return new StickyNoteInfo(
                getNoteText(),
                getNoteColorHex(),
                x, y
        );
    }
}
