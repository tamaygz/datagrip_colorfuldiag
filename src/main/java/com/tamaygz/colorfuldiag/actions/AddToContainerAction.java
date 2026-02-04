package com.tamaygz.colorfuldiag.actions;

import com.intellij.diagram.DiagramNode;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import com.tamaygz.colorfuldiag.model.ContainerInfo;
import com.tamaygz.colorfuldiag.model.DiagramMetadata;
import com.tamaygz.colorfuldiag.ui.ContainerSelectionDialog;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * Action to add selected tables to a container.
 */
public class AddToContainerAction extends DiagramActionBase {

    public AddToContainerAction() {
        super("Add to Container", "Add this table to a container", null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Collection<DiagramNode<?>> selectedNodes = getSelectedNodes(e);
        if (selectedNodes.isEmpty()) {
            return;
        }

        DiagramMetadata metadata = getMetadata(e);
        if (metadata == null) {
            return;
        }

        List<ContainerInfo> containers = metadata.getContainers();
        if (containers.isEmpty()) {
            Messages.showInfoMessage(
                    e.getProject(),
                    "No containers available. Please create a container first.",
                    "Add to Container"
            );
            return;
        }

        ContainerSelectionDialog dialog = new ContainerSelectionDialog(e.getProject(), containers);
        if (!dialog.showAndGet()) {
            return;
        }

        ContainerInfo selectedContainer = dialog.getSelectedContainer();
        if (selectedContainer == null) {
            return;
        }

        // Add tables to the selected container
        for (DiagramNode<?> node : selectedNodes) {
            String tableId = getTableId(node);
            if (tableId != null) {
                // Remove from any existing container first
                for (ContainerInfo container : containers) {
                    container.removeTable(tableId);
                }
                // Add to the selected container
                selectedContainer.addTable(tableId);

                // If table doesn't have its own color, inherit from container
                if (metadata.getTableColor(tableId) == null && selectedContainer.getColor() != null) {
                    metadata.setTableColor(tableId, selectedContainer.getColor());
                }
            }
        }

        saveMetadata(e, metadata);
        refreshDiagram(e);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        if (e.getPresentation().isEnabled()) {
            Collection<DiagramNode<?>> selectedNodes = getSelectedNodes(e);
            e.getPresentation().setEnabled(!selectedNodes.isEmpty());
        }
    }
}
