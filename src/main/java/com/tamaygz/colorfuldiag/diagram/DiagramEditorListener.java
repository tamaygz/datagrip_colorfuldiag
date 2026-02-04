package com.tamaygz.colorfuldiag.diagram;

import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.tamaygz.colorfuldiag.persistence.DiagramMetadataService;
import org.jetbrains.annotations.NotNull;

/**
 * Listens for diagram editor events to load/apply metadata.
 */
public class DiagramEditorListener implements EditorFactoryListener {

    @Override
    public void editorCreated(@NotNull EditorFactoryEvent event) {
        // This listener can be used to detect when editors are created
        // For diagram-specific handling, we rely on file editor listeners
    }

    @Override
    public void editorReleased(@NotNull EditorFactoryEvent event) {
        // Clean up when editor is released
    }

    /**
     * File editor listener for diagram files.
     * This is the main listener that handles diagram open/close events.
     */
    public static class DiagramFileEditorListener implements FileEditorManagerListener {

        @Override
        public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
            // Check if this is a diagram file
            if (isDiagramFile(file)) {
                loadMetadataForDiagram(source.getProject(), file);
            }
        }

        @Override
        public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
            if (isDiagramFile(file)) {
                // Optionally save metadata when file is closed
                saveMetadataForDiagram(source.getProject(), file);
            }
        }

        private boolean isDiagramFile(VirtualFile file) {
            if (file == null) {
                return false;
            }
            String extension = file.getExtension();
            // DataGrip diagram files typically have .uml extension
            // or are opened from database objects
            return "uml".equalsIgnoreCase(extension)
                    || "dbdiagram".equalsIgnoreCase(extension)
                    || file.getPath().contains("diagrams");
        }

        private void loadMetadataForDiagram(Project project, VirtualFile file) {
            if (project == null || project.isDisposed()) {
                return;
            }

            DiagramMetadataService service = DiagramMetadataService.getInstance(project);
            // Load metadata - this caches it for later use
            service.loadMetadata(file);
        }

        private void saveMetadataForDiagram(Project project, VirtualFile file) {
            if (project == null || project.isDisposed()) {
                return;
            }

            // Metadata is saved on-demand when changes are made
            // This is just a cleanup point
            DiagramMetadataService service = DiagramMetadataService.getInstance(project);
            service.invalidateCache(file.getPath());
        }
    }
}
