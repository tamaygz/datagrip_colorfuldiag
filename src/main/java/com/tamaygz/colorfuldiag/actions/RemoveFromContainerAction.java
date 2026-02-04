package com.tamaygz.colorfuldiag.actions;

import com.intellij.diagram.DiagramNode;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.tamaygz.colorfuldiag.model.ContainerInfo;
import com.tamaygz.colorfuldiag.model.DiagramMetadata;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Action to remove selected tables from their containers.
 */
public class RemoveFromContainerAction extends DiagramActionBase {

    public RemoveFromContainerAction() {
        super("Remove from Container", "Remove this table from its container", null);
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

        boolean anyRemoved = false;

        // Remove tables from their containers
        for (DiagramNode<?> node : selectedNodes) {
            String tableId = getTableId(node);
            if (tableId != null) {
                ContainerInfo container = metadata.findContainerForTable(tableId);
                if (container != null) {
                    container.removeTable(tableId);
                    // Clear the inherited color
                    metadata.removeTableColor(tableId);
                    anyRemoved = true;
                }
            }
        }

        if (anyRemoved) {
            saveMetadata(e, metadata);
            refreshDiagram(e);
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        if (e.getPresentation().isEnabled()) {
            Collection<DiagramNode<?>> selectedNodes = getSelectedNodes(e);
            if (selectedNodes.isEmpty()) {
                e.getPresentation().setEnabled(false);
                return;
            }

            // Check if any selected table is in a container
            DiagramMetadata metadata = getMetadata(e);
            if (metadata == null) {
                e.getPresentation().setEnabled(false);
                return;
            }

            boolean anyInContainer = false;
            for (DiagramNode<?> node : selectedNodes) {
                String tableId = getTableId(node);
                if (tableId != null && metadata.findContainerForTable(tableId) != null) {
                    anyInContainer = true;
                    break;
                }
            }

            e.getPresentation().setEnabled(anyInContainer);
        }
    }
}
