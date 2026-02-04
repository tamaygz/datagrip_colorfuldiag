package com.tamaygz.colorfuldiag.diagram;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.tamaygz.colorfuldiag.model.DiagramMetadata;
import com.tamaygz.colorfuldiag.persistence.DiagramMetadataService;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Listens for diagram editor events to load/apply metadata.
 * When a diagram editor is opened, creates and attaches an overlay panel for rendering
 * containers, sticky notes, and colored tables.
 */
public class DiagramEditorListener implements FileEditorManagerListener {

    private static final Map<String, OverlayPanel> overlayPanels = new HashMap<>();

    @Override
    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        // Check if this is a diagram file
        if (!isDiagramFile(file)) {
            return;
        }

        Project project = source.getProject();
        if (project == null || project.isDisposed()) {
            return;
        }

        // Get the file editor
        FileEditor fileEditor = source.getSelectedEditor(file);
        if (fileEditor == null) {
            return;
        }

        // Get the component from the file editor
        JComponent component = fileEditor.getComponent();
        if (component == null) {
            return;
        }

        // Create and attach overlay panel
        attachOverlayPanel(project, file, component);
    }

    @Override
    public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        if (isDiagramFile(file)) {
            String key = file.getPath();
            OverlayPanel panel = overlayPanels.remove(key);
            if (panel != null && panel.getParent() != null) {
                panel.getParent().remove(panel);
            }
        }
    }

    private void attachOverlayPanel(Project project, VirtualFile file, JComponent editorComponent) {
        String filePath = file.getPath();
        
        // Check if overlay already exists
        if (overlayPanels.containsKey(filePath)) {
            return;
        }

        // Create overlay panel
        OverlayPanel overlayPanel = new OverlayPanel();
        
        // Load metadata from service
        DiagramMetadataService service = DiagramMetadataService.getInstance(project);
        DiagramMetadata metadata = service.loadMetadata(file);
        overlayPanel.setMetadata(metadata);

        // If the editor component is a JLayeredPane or similar, add overlay
        if (editorComponent instanceof JLayeredPane layeredPane) {
            layeredPane.add(overlayPanel, JLayeredPane.PALETTE_LAYER);
            overlayPanel.setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());
            overlayPanels.put(filePath, overlayPanel);
        } else if (editorComponent instanceof JPanel panel) {
            // If it's a regular panel, we need to add the overlay as an overlay component
            // Usually diagram editors use a specific layout - we need to overlay on top
            panel.setLayout(new OverlayLayout(panel));
            overlayPanel.setPreferredSize(panel.getSize());
            panel.add(overlayPanel);
            overlayPanel.setBounds(0, 0, panel.getWidth(), panel.getHeight());
            overlayPanels.put(filePath, overlayPanel);
        } else {
            // For other component types, try to find a parent container
            Container parent = editorComponent.getParent();
            if (parent instanceof JLayeredPane layeredPane) {
                layeredPane.add(overlayPanel, JLayeredPane.PALETTE_LAYER);
                overlayPanel.setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());
                overlayPanels.put(filePath, overlayPanel);
            }
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

    /**
     * Updates the overlay panel metadata for a specific diagram.
     * Called when diagram metadata changes (colors applied, containers created, etc.)
     */
    public static void updateOverlayMetadata(String diagramPath, DiagramMetadata metadata) {
        OverlayPanel panel = overlayPanels.get(diagramPath);
        if (panel != null) {
            panel.setMetadata(metadata);
            panel.repaint();
        }
    }
}
