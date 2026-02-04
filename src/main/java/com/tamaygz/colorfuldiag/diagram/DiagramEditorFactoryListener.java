package com.tamaygz.colorfuldiag.diagram;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindowManager;
import com.tamaygz.colorfuldiag.model.DiagramMetadata;
import com.tamaygz.colorfuldiag.persistence.DiagramMetadataService;
import com.tamaygz.colorfuldiag.ui.ColorfulDiagramsToolWindowFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Lower-level editor factory listener that catches all editor types including
 * database diagrams which may not trigger FileEditorManagerListener events.
 */
public class DiagramEditorFactoryListener implements EditorFactoryListener {
    
    private static final Logger LOG = Logger.getInstance(DiagramEditorFactoryListener.class);
    private static final Map<String, OverlayPanel> overlayPanels = new HashMap<>();

    @Override
    public void editorCreated(@NotNull EditorFactoryEvent event) {
        // EditorFactory creates text editors, not file editors
        // We need a different approach for diagrams
    }

    @Override
    public void editorReleased(@NotNull EditorFactoryEvent event) {
        // Clean up if needed
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
            
            // Get currently selected file
            VirtualFile[] selectedFiles = editorManager.getSelectedFiles();
            if (selectedFiles.length == 0) {
                LOG.debug("No selected files");
                return;
            }

            VirtualFile file = selectedFiles[0];
            FileEditor selectedEditor = editorManager.getSelectedEditor(file);
            
            if (selectedEditor == null) {
                LOG.debug("No selected editor for file: " + file.getName());
                return;
            }

            LOG.info("Attempting to attach overlay to: " + file.getName());
            LOG.info("Editor class: " + selectedEditor.getClass().getName());

            attachOverlayIfDiagram(project, file, selectedEditor);
        } catch (Exception e) {
            LOG.warn("Error in tryAttachOverlayToCurrentDiagram", e);
        }
    }

    private static void attachOverlayIfDiagram(Project project, VirtualFile file, FileEditor editor) {
        String filePath = file.getPath();
        
        // Check if already attached
        if (overlayPanels.containsKey(filePath)) {
            LOG.debug("Overlay already attached to: " + filePath);
            return;
        }

        JComponent component = editor.getComponent();
        if (component == null) {
            LOG.warn("Editor component is null");
            return;
        }

        LOG.info("Editor component: " + component.getClass().getName());
        LOG.info("Component size: " + component.getWidth() + "x" + component.getHeight());

        // Check if this looks like a diagram editor
        String className = editor.getClass().getName();
        if (!className.contains("diagram") && !className.contains("Diagram") && 
            !className.contains("uml") && !className.contains("UML") &&
            !className.contains("Database")) {
            LOG.debug("Not a diagram editor: " + className);
            return;
        }

        // Create overlay panel
        OverlayPanel overlayPanel = new OverlayPanel();
        overlayPanel.setOpaque(false);
        overlayPanel.setVisible(true);
        
        // Load metadata
        DiagramMetadataService service = DiagramMetadataService.getInstance(project);
        DiagramMetadata metadata = service.loadMetadata(file);
        overlayPanel.setMetadata(metadata);

        // Attach to component
        if (attachOverlayToComponent(component, overlayPanel)) {
            overlayPanels.put(filePath, overlayPanel);
            LOG.info("✓ Overlay attached successfully to: " + filePath);
            
            // Show tool window
            showToolWindow(project);
        } else {
            LOG.warn("Failed to attach overlay to component");
        }
    }

    private static boolean attachOverlayToComponent(JComponent component, OverlayPanel overlayPanel) {
        try {
            // Get the actual drawable area - traverse to find the right container
            Container target = findDiagramCanvas(component);
            
            if (target == null) {
                LOG.warn("Could not find diagram canvas");
                return false;
            }

            LOG.info("Target canvas: " + target.getClass().getName());

            // Set bounds
            overlayPanel.setBounds(0, 0, target.getWidth(), target.getHeight());
            
            // Add to component
            if (target instanceof JLayeredPane layeredPane) {
                layeredPane.add(overlayPanel, JLayeredPane.PALETTE_LAYER);
            } else {
                target.add(overlayPanel);
                // Bring to front
                if (target instanceof JComponent jcomp) {
                    jcomp.setComponentZOrder(overlayPanel, 0);
                }
            }

            // Add resize listener
            target.addComponentListener(new java.awt.event.ComponentAdapter() {
                @Override
                public void componentResized(java.awt.event.ComponentEvent e) {
                    overlayPanel.setBounds(0, 0, target.getWidth(), target.getHeight());
                    overlayPanel.repaint();
                }

                @Override
                public void componentShown(java.awt.event.ComponentEvent e) {
                    overlayPanel.repaint();
                }
            });

            target.repaint();
            return true;
        } catch (Exception e) {
            LOG.error("Error attaching overlay", e);
            return false;
        }
    }

    private static Container findDiagramCanvas(JComponent component) {
        // Try the component itself first
        if (isValidContainer(component)) {
            return component;
        }

        // Check all descendants using BFS
        java.util.Queue<Component> queue = new java.util.LinkedList<>();
        queue.add(component);

        while (!queue.isEmpty()) {
            Component current = queue.poll();
            
            if (current instanceof Container container) {
                // Prefer JLayeredPane (diagram canvases often use this)
                if (container instanceof JLayeredPane) {
                    if (isValidContainer(container)) {
                        return container;
                    }
                }
                
                // Check children
                for (Component child : container.getComponents()) {
                    queue.add(child);
                    if (child instanceof JLayeredPane && isValidContainer(child)) {
                        return (Container) child;
                    }
                }
            }
        }

        // Fallback to the component if it's a container
        return component instanceof Container ? (Container) component : null;
    }

    private static boolean isValidContainer(Component comp) {
        return comp != null && comp.isVisible() && comp.getWidth() > 0 && comp.getHeight() > 0;
    }

    private static void showToolWindow(Project project) {
        if (project == null || project.isDisposed()) {
            return;
        }

        try {
            ToolWindowManager mgr = ToolWindowManager.getInstance(project);
            var toolWindow = mgr.getToolWindow(ColorfulDiagramsToolWindowFactory.TOOL_WINDOW_ID);
            if (toolWindow != null) {
                toolWindow.show();
            }
        } catch (Exception e) {
            LOG.debug("Could not show tool window", e);
        }
    }
}
