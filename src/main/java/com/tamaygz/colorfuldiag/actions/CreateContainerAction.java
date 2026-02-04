package com.tamaygz.colorfuldiag.actions;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.tamaygz.colorfuldiag.model.ContainerInfo;
import com.tamaygz.colorfuldiag.model.DiagramMetadata;
import com.tamaygz.colorfuldiag.ui.ContainerDialog;

/**
 * Action to create a new visual container in the diagram.
 */
public class CreateContainerAction extends DiagramActionBase {

    public CreateContainerAction() {
        super("Create Container", "Create a visual container to group tables", null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        DiagramMetadata metadata = getMetadata(e);
        if (metadata == null) {
            return;
        }

        ContainerDialog dialog = new ContainerDialog(e.getProject());
        if (!dialog.showAndGet()) {
            return;
        }

        // Calculate position for the new container
        // Default to center of visible area or offset from existing containers
        int x = 50;
        int y = 50;

        // Offset from existing containers to avoid overlap
        if (!metadata.getContainers().isEmpty()) {
            int maxX = 0;
            int maxY = 0;
            for (ContainerInfo existing : metadata.getContainers()) {
                int[] bounds = existing.getBounds();
                if (bounds != null && bounds.length >= 4) {
                    maxX = Math.max(maxX, bounds[0] + bounds[2]);
                    maxY = Math.max(maxY, bounds[1] + bounds[3]);
                }
            }
            // Place new container to the right of existing ones
            x = maxX + 20;
        }

        ContainerInfo container = dialog.createContainerInfo(x, y);
        metadata.addContainer(container);

        saveMetadata(e, metadata);
        refreshDiagram(e);
    }
}
