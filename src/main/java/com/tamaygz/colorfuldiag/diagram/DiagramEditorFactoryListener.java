package com.tamaygz.colorfuldiag.diagram;

import javax.swing.SwingUtilities;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * Lower-level editor factory listener that helps catch all editor types.
 * Works in conjunction with DiagramEditorListener for comprehensive diagram detection.
 */
public class DiagramEditorFactoryListener implements EditorFactoryListener {
    
    private static final Logger LOG = Logger.getInstance(DiagramEditorFactoryListener.class);

    @Override
    public void editorCreated(@NotNull EditorFactoryEvent event) {
        // EditorFactory creates text editors, not file editors
        // Diagrams are handled by DiagramEditorListener
    }

    @Override
    public void editorReleased(@NotNull EditorFactoryEvent event) {
        // Clean up handled by DiagramEditorListener
    }

    /**
     * Attempt to attach overlay to a diagram editor when detected.
     * This is called from multiple entry points to ensure we catch diagrams.
     */
    public static void tryAttachOverlayToCurrentDiagram(Project project) {
        if (project == null || project.isDisposed()) {
            return;
        }

        try {
            FileEditorManager editorManager = FileEditorManager.getInstance(project);
            
            // Get currently selected files
            VirtualFile[] selectedFiles = editorManager.getSelectedFiles();
            if (selectedFiles.length == 0) {
                LOG.debug("No selected files");
                return;
            }

            for (VirtualFile file : selectedFiles) {
                FileEditor selectedEditor = editorManager.getSelectedEditor(file);
                
                if (selectedEditor == null) {
                    continue;
                }

                String className = selectedEditor.getClass().getName().toLowerCase();
                
                // Check if this looks like a diagram editor
                if (className.contains("diagram") || className.contains("uml") || 
                    className.contains("graph") || className.contains("database") ||
                    className.contains("schema") || className.contains("erd")) {
                    
                    LOG.info("Found potential diagram editor: " + selectedEditor.getClass().getName());
                    
                    // Use SwingUtilities to ensure we're on EDT
                    SwingUtilities.invokeLater(() -> {
                        DiagramEditorListener.reattachOverlay(project, file.getPath());
                    });
                }
            }
        } catch (Exception e) {
            LOG.warn("Error in tryAttachOverlayToCurrentDiagram", e);
        }
    }

    /**
     * Forces refresh of all overlay panels in the project.
     */
    public static void refreshAllOverlays(Project project) {
        if (project == null || project.isDisposed()) {
            return;
        }

        try {
            FileEditorManager editorManager = FileEditorManager.getInstance(project);
            for (VirtualFile file : editorManager.getOpenFiles()) {
                OverlayPanel panel = DiagramEditorListener.getOverlayPanel(file.getPath());
                if (panel != null) {
                    panel.repaint();
                }
            }
        } catch (Exception e) {
            LOG.warn("Error refreshing overlays", e);
        }
    }
}
