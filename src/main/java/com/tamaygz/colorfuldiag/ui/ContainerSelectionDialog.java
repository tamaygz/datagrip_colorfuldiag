package com.tamaygz.colorfuldiag.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.tamaygz.colorfuldiag.model.ContainerInfo;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Dialog for selecting a container to add a table to.
 */
public class ContainerSelectionDialog extends DialogWrapper {
    private final List<ContainerInfo> containers;
    private JList<ContainerInfo> containerList;
    private ContainerInfo selectedContainer;

    public ContainerSelectionDialog(@Nullable Project project, List<ContainerInfo> containers) {
        super(project);
        this.containers = containers;
        setTitle("Select Container");
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        if (containers == null || containers.isEmpty()) {
            panel.add(new JLabel("No containers available. Create a container first."), BorderLayout.CENTER);
            return panel;
        }

        containerList = new JList<>(containers.toArray(new ContainerInfo[0]));
        containerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        containerList.setCellRenderer(new ContainerListCellRenderer());

        if (!containers.isEmpty()) {
            containerList.setSelectedIndex(0);
        }

        JScrollPane scrollPane = new JScrollPane(containerList);
        scrollPane.setPreferredSize(new Dimension(300, 200));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    @Override
    protected void doOKAction() {
        if (containerList != null) {
            selectedContainer = containerList.getSelectedValue();
        }
        super.doOKAction();
    }

    public ContainerInfo getSelectedContainer() {
        return selectedContainer;
    }

    private static class ContainerListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {

            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof ContainerInfo container) {
                setText(container.getTitle());

                // Create a color icon
                Color color = container.getAwtColor();
                if (color != null) {
                    setIcon(new ColorIcon(color, 16, 16));
                }
            }

            return this;
        }
    }

    private static class ColorIcon implements Icon {
        private final Color color;
        private final int width;
        private final int height;

        public ColorIcon(Color color, int width, int height) {
            this.color = color;
            this.width = width;
            this.height = height;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(color);
            g.fillRect(x, y, width, height);
            g.setColor(Color.DARK_GRAY);
            g.drawRect(x, y, width - 1, height - 1);
        }

        @Override
        public int getIconWidth() {
            return width;
        }

        @Override
        public int getIconHeight() {
            return height;
        }
    }
}
