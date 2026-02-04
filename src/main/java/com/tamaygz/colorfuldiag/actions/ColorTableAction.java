package com.tamaygz.colorfuldiag.actions;

import com.intellij.diagram.DiagramNode;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.tamaygz.colorfuldiag.model.DiagramMetadata;
import com.tamaygz.colorfuldiag.model.TableColorInfo;
import com.tamaygz.colorfuldiag.ui.ColorPickerDialog;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Collection;

/**
 * Action to color selected tables in the diagram.
 */
public class ColorTableAction extends DiagramActionBase {

    public ColorTableAction() {
        super("Color Selected Tables", "Apply a color to selected tables", null);
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

        // Get initial color from first selected table
        Color initialColor = null;
        for (DiagramNode<?> node : selectedNodes) {
            String tableId = getTableId(node);
            if (tableId != null) {
                TableColorInfo colorInfo = metadata.getTableColor(tableId);
                if (colorInfo != null && colorInfo.getAwtColor() != null) {
                    initialColor = colorInfo.getAwtColor();
                    break;
                }
            }
        }

        // Show color picker
        Color selectedColor = ColorPickerDialog.showDialog(e.getProject(), initialColor);
        if (selectedColor == null) {
            return;
        }

        String colorHex = TableColorInfo.colorToHex(selectedColor);

        // Apply color to all selected tables
        for (DiagramNode<?> node : selectedNodes) {
            String tableId = getTableId(node);
            if (tableId != null) {
                metadata.setTableColor(tableId, colorHex);
            }
        }

        // Save and refresh
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
