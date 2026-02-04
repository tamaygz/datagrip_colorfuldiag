package com.tamaygz.colorfuldiag.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import com.intellij.openapi.project.Project;
import com.tamaygz.colorfuldiag.model.ContainerInfo;
import com.tamaygz.colorfuldiag.model.TableColorInfo;

/**
 * Quick color picker popup menu for containers.
 */
public class QuickColorPickerPopup extends JPopupMenu {
    
    // Predefined colors for quick selection
    private static final Color[] QUICK_COLORS = {
        new Color(0x45B7D1), // Teal
        new Color(0x96CEB4), // Mint
        new Color(0xFFEEAD), // Cream
        new Color(0xD4A5A5), // Dusty Rose
        new Color(0x9B59B6), // Purple
        new Color(0x3498DB), // Blue
        new Color(0xE74C3C), // Red
        new Color(0x2ECC71), // Green
        new Color(0xF39C12), // Orange
        new Color(0x1ABC9C), // Turquoise
    };

    public QuickColorPickerPopup(Project project, ContainerInfo container, Consumer<Color> onColorSelected) {
        // Quick color buttons
        JPanel colorPanel = new JPanel(new GridLayout(2, 5, 2, 2));
        colorPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        for (Color color : QUICK_COLORS) {
            JButton colorBtn = createColorButton(color, e -> {
                onColorSelected.accept(color);
                setVisible(false);
            });
            colorPanel.add(colorBtn);
        }
        add(colorPanel);
        
        addSeparator();
        
        // Custom color option
        JMenuItem customItem = new JMenuItem("Choose Custom Color...");
        customItem.addActionListener(e -> {
            Color current = container.getAwtColor();
            Color newColor = ColorPickerDialog.showDialog(project,
                current != null ? current : QUICK_COLORS[0]
            );
            if (newColor != null) {
                onColorSelected.accept(newColor);
            }
        });
        add(customItem);
        
        addSeparator();
        
        // Remove color option
        JMenuItem removeItem = new JMenuItem("Remove Color");
        removeItem.addActionListener(e -> {
            onColorSelected.accept(null);
        });
        add(removeItem);
    }
    
    private JButton createColorButton(Color color, ActionListener listener) {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Fill with color
                g2d.setColor(color);
                g2d.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 4, 4);
                
                // Border
                g2d.setColor(color.darker());
                g2d.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 4, 4);
                
                g2d.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(24, 24));
        btn.setToolTipText(TableColorInfo.colorToHex(color));
        btn.addActionListener(listener);
        return btn;
    }
}
