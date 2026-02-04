package com.tamaygz.colorfuldiag.diagram;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ComponentListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.tamaygz.colorfuldiag.model.DiagramMetadata;
import com.tamaygz.colorfuldiag.persistence.DiagramMetadataService;
import com.tamaygz.colorfuldiag.ui.ColorfulDiagramsToolWindowFactory;

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
        
        // Auto-show the tool window when diagram opens
        showToolWindow(project);
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
        overlayPanel.setOpaque(false);
        overlayPanel.setVisible(true);
        
        // Load metadata from service
        DiagramMetadataService service = DiagramMetadataService.getInstance(project);
        DiagramMetadata metadata = service.loadMetadata(file);
        overlayPanel.setMetadata(metadata);

        // Try to find the actual canvas/viewport component
        Container target = findAndPrepareCanvasContainer(editorComponent);
        
        if (target != null) {
            // Add overlay on top
            if (target instanceof JLayeredPane layeredPane) {
                overlayPanel.setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());
                layeredPane.add(overlayPanel, JLayeredPane.PALETTE_LAYER);
                overlayPanels.put(filePath, overlayPanel);
            } else {
                // For regular panels, ensure it's on top
                overlayPanel.setBounds(0, 0, target.getWidth(), target.getHeight());
                target.add(overlayPanel);
                // Use compound UI layer ordering
                if (target instanceof JComponent jcomp) {
                    jcomp.setComponentZOrder(overlayPanel, 0);
                }
                overlayPanels.put(filePath, overlayPanel);
            }
            
            // Add resize listener to keep overlay updated
            ComponentListener resizeListener = new java.awt.event.ComponentAdapter() {
                @Override
                public void componentResized(java.awt.event.ComponentEvent e) {
                    overlayPanel.setBounds(0, 0, target.getWidth(), target.getHeight());
                    overlayPanel.revalidate();
                    overlayPanel.repaint();
                }

                @Override
                public void componentShown(java.awt.event.ComponentEvent e) {
                    overlayPanel.repaint();
                }
            };
            
            target.addComponentListener(resizeListener);
            overlayPanel.repaint();
        }
    }

    /**
     * Finds the actual diagram canvas container and prepares it for overlay attachment.
     * Handles different component hierarchies (scroll panes, layered panes, etc.)
     */
    private Container findAndPrepareCanvasContainer(JComponent editorComponent) {
        // Try direct component first
        if (isValidCanvasContainer(editorComponent)) {
            return editorComponent;
        }

        // Check for scroll pane
        if (editorComponent instanceof JScrollPane scrollPane) {
            Component viewport = scrollPane.getViewport().getView();
            if (viewport instanceof Container && isValidCanvasContainer(viewport)) {
                return (Container) viewport;
            }
            // If viewport has parent (layered pane), use that
            Container parent = viewport.getParent();
            if (parent != null && isValidCanvasContainer(parent)) {
                return parent;
            }
        }

        // Check parent hierarchy
        Container parent = editorComponent.getParent();
        while (parent != null) {
            if (parent instanceof JLayeredPane || isValidCanvasContainer(parent)) {
                return parent;
            }
            parent = parent.getParent();
        }

        // Fallback: use the component itself
        return editorComponent instanceof Container ? (Container) editorComponent : null;
    }

    /**
     * Checks if a component is suitable for overlay attachment.
     */
    private boolean isValidCanvasContainer(Component component) {
        return component instanceof Container 
                && component.isVisible()
                && component.getWidth() > 0 
                && component.getHeight() > 0;
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
     * Shows the Colorful Diagrams tool window.
     * The tool window can be docked, floated, or closed by the user.
     */
    private void showToolWindow(Project project) {
        if (project == null || project.isDisposed()) {
            return;
        }

        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        ToolWindow toolWindow = toolWindowManager.getToolWindow(ColorfulDiagramsToolWindowFactory.TOOL_WINDOW_ID);

        if (toolWindow != null) {
            toolWindow.show(() -> {
                // Optional: focus could be set here if needed
            });
        }
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
