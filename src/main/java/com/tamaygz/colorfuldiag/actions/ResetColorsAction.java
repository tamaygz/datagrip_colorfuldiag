package com.tamaygz.colorfuldiag.actions;

import com.intellij.diagram.DiagramNode;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import com.tamaygz.colorfuldiag.model.ContainerInfo;
import com.tamaygz.colorfuldiag.model.DiagramMetadata;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Action to reset colors for selected tables or all tables.
 */
public class ResetColorsAction extends DiagramActionBase {

    public ResetColorsAction() {
        super("Reset Colors", "Reset colors for selected tables", null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        DiagramMetadata metadata = getMetadata(e);
        if (metadata == null) {
            return;
        }

        Collection<DiagramNode<?>> selectedNodes = getSelectedNodes(e);

        if (selectedNodes.isEmpty()) {
            // Ask if user wants to reset all colors
            int result = Messages.showYesNoDialog(
                    e.getProject(),
                    "No tables selected. Do you want to reset all table colors?",
                    "Reset Colors",
                    Messages.getQuestionIcon()
            );

            if (result == Messages.YES) {
                // Clear all table colors
                metadata.getTables().clear();

                // Also remove tables from containers
                for (ContainerInfo container : metadata.getContainers()) {
                    container.getTables().clear();
                }

                saveMetadata(e, metadata);
                refreshDiagram(e);
            }
        } else {
            // Reset colors for selected tables only
            for (DiagramNode<?> node : selectedNodes) {
                String tableId = getTableId(node);
                if (tableId != null) {
                    metadata.removeTableColor(tableId);

                    // Also remove from any containers
                    for (ContainerInfo container : metadata.getContainers()) {
                        container.removeTable(tableId);
                    }
                }
            }

            saveMetadata(e, metadata);
            refreshDiagram(e);
        }
    }
}
