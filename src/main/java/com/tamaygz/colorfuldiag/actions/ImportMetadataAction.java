package com.tamaygz.colorfuldiag.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.tamaygz.colorfuldiag.model.DiagramMetadata;
import com.tamaygz.colorfuldiag.persistence.DiagramMetadataService;
import org.jetbrains.annotations.NotNull;

/**
 * Action to import diagram metadata from a JSON file.
 */
public class ImportMetadataAction extends DiagramActionBase {

    public ImportMetadataAction() {
        super("Import Metadata", "Import plugin metadata from JSON file", null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        DiagramMetadataService service = getMetadataService(e);
        if (service == null) {
            return;
        }

        String diagramPath = getDiagramPath(e);
        if (diagramPath == null) {
            return;
        }

        // Show file chooser
        FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false)
                .withTitle("Import Colorful Diagrams Metadata")
                .withDescription("Select a metadata JSON file to import")
                .withFileFilter(file -> "json".equalsIgnoreCase(file.getExtension()));

        VirtualFile selectedFile = FileChooser.chooseFile(descriptor, e.getProject(), null);
        if (selectedFile == null) {
            return;
        }

        // Check if we should merge or replace existing metadata
        DiagramMetadata existing = getMetadata(e);
        boolean merge = false;

        if (existing != null && !existing.isEmpty()) {
            int result = Messages.showYesNoCancelDialog(
                    e.getProject(),
                    "This diagram already has metadata. Do you want to:\n" +
                            "- Yes: Merge imported data with existing data\n" +
                            "- No: Replace existing data with imported data\n" +
                            "- Cancel: Cancel the import",
                    "Import Metadata",
                    "Merge",
                    "Replace",
                    "Cancel",
                    Messages.getQuestionIcon()
            );

            if (result == Messages.CANCEL) {
                return;
            }
            merge = result == Messages.YES;
        }

        // Import the metadata
        DiagramMetadata imported = service.importMetadata(selectedFile.getPath());

        if (merge && existing != null) {
            // Merge: add imported data to existing
            imported.getTables().forEach((tableId, colorInfo) -> {
                if (existing.getTableColor(tableId) == null) {
                    existing.setTableColor(tableId, colorInfo.getColor());
                }
            });

            imported.getContainers().forEach(container -> {
                if (existing.getContainer(container.getId()) == null) {
                    existing.addContainer(container);
                }
            });

            imported.getNotes().forEach(note -> {
                if (existing.getNote(note.getId()) == null) {
                    existing.addNote(note);
                }
            });

            saveMetadata(e, existing);
        } else {
            // Replace with imported data
            saveMetadata(e, imported);
        }

        refreshDiagram(e);
    }
}
