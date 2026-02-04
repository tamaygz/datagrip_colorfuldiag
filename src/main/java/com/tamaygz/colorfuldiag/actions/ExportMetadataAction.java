package com.tamaygz.colorfuldiag.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.tamaygz.colorfuldiag.model.DiagramMetadata;
import com.tamaygz.colorfuldiag.persistence.DiagramMetadataService;
import org.jetbrains.annotations.NotNull;

/**
 * Action to export diagram metadata to a JSON file.
 */
public class ExportMetadataAction extends DiagramActionBase {

    public ExportMetadataAction() {
        super("Export Metadata", "Export plugin metadata to JSON file", null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        DiagramMetadata metadata = getMetadata(e);
        if (metadata == null || metadata.isEmpty()) {
            return;
        }

        DiagramMetadataService service = getMetadataService(e);
        if (service == null) {
            return;
        }

        String diagramPath = getDiagramPath(e);
        if (diagramPath == null) {
            return;
        }

        // Show file save dialog
        FileSaverDescriptor descriptor = new FileSaverDescriptor(
                "Export Colorful Diagrams Metadata",
                "Choose where to save the metadata file",
                "json"
        );

        FileSaverDialog dialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, e.getProject());

        // Suggest the default metadata filename
        VirtualFile diagramFile = getDiagramFile(e);
        VirtualFile baseDir = diagramFile != null ? diagramFile.getParent() : null;
        String suggestedName = diagramFile != null
                ? diagramFile.getNameWithoutExtension() + "_colorfuldiag"
                : "diagram_colorfuldiag";

        VirtualFileWrapper wrapper = dialog.save(baseDir, suggestedName);
        if (wrapper != null) {
            String exportPath = wrapper.getFile().getAbsolutePath();
            if (!exportPath.endsWith(".json")) {
                exportPath += ".json";
            }
            service.exportMetadata(diagramPath, exportPath);
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        if (e.getPresentation().isEnabled()) {
            DiagramMetadata metadata = getMetadata(e);
            e.getPresentation().setEnabled(metadata != null && !metadata.isEmpty());
        }
    }
}
