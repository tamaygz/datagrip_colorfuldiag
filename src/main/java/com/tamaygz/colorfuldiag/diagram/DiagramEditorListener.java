package com.tamaygz.colorfuldiag.diagram;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
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
 * 
 * Uses a glass pane approach for reliable overlay rendering.
 */
public class DiagramEditorListener implements FileEditorManagerListener {

    private static final Logger LOG = Logger.getInstance(DiagramEditorListener.class);
    private static final Map<String, OverlayPanel> overlayPanels = new ConcurrentHashMap<>();
    private static final Map<String, JComponent> editorComponents = new ConcurrentHashMap<>();
    
    // Diagram editor class patterns for detection
    private static final String[] DIAGRAM_CLASS_PATTERNS = {
            "diagram", "Diagram", "uml", "UML", "Graph", "graph",
            "Database", "database", "DbDiagram", "ERD", "erd",
            "Schema", "schema", "Visualiz", "visualiz"
    };

    @Override
    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        try {
            LOG.info("=== File opened: " + file.getName() + " ===");
            
            Project project = source.getProject();
            if (project == null || project.isDisposed()) {
                return;
            }

            // Get the file editor
            FileEditor fileEditor = source.getSelectedEditor(file);
            if (fileEditor == null) {
                LOG.info("No file editor for: " + file.getName());
                return;
            }

            String editorClassName = fileEditor.getClass().getName();
            LOG.info("Editor type: " + editorClassName);
            
            // Check if this is a diagram editor
            if (!isDiagramEditor(fileEditor)) {
                LOG.info("Not a diagram editor (class: " + fileEditor.getClass().getSimpleName() + ")");
                return;
            }

            LOG.info("✓ Diagram detected: " + file.getName());

            JComponent component = fileEditor.getComponent();
            if (component == null) {
                LOG.warn("Editor component is null");
                return;
            }

            // Store component for later use
            editorComponents.put(file.getPath(), component);
            
            // Delay attachment to ensure component is fully rendered
            // Use invokeLater to ensure we're on EDT and component is ready
            SwingUtilities.invokeLater(() -> {
                attachOverlayWithDelay(project, file, component, 0);
            });
            
        } catch (Exception e) {
            LOG.error("Error in fileOpened", e);
        }
    }

    /**
     * Attaches overlay with retry mechanism for when component isn't ready yet.
     */
    private void attachOverlayWithDelay(Project project, VirtualFile file, JComponent component, int attempt) {
        if (project.isDisposed()) return;
        
        // Check if component is ready (has size)
        if (component.getWidth() <= 0 || component.getHeight() <= 0) {
            if (attempt < 10) {
                // Retry after a short delay
                Timer timer = new Timer(100 * (attempt + 1), e -> {
                    attachOverlayWithDelay(project, file, component, attempt + 1);
                });
                timer.setRepeats(false);
                timer.start();
                return;
            } else {
                LOG.warn("Component never became ready after " + attempt + " attempts");
                // Try attaching anyway
            }
        }
        
        LOG.info("Component ready (attempt " + attempt + "): " + component.getWidth() + "x" + component.getHeight());
        attachOverlayPanel(project, file, component);
        showToolWindow(project);
    }

    @Override
    public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        String key = file.getPath();
        editorComponents.remove(key);
        
        OverlayPanel panel = overlayPanels.remove(key);
        if (panel != null) {
            Container parent = panel.getParent();
            if (parent != null) {
                parent.remove(panel);
                parent.repaint();
            }
        }
        LOG.info("Cleaned up overlay for: " + file.getName());
    }

    private void attachOverlayPanel(Project project, VirtualFile file, JComponent editorComponent) {
        String filePath = file.getPath();
        
        // Check if overlay already exists
        if (overlayPanels.containsKey(filePath)) {
            LOG.info("Overlay already exists for: " + filePath);
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

        // Try multiple attachment strategies
        boolean attached = false;
        
        // Strategy 1: Use glass pane (most reliable)
        attached = tryAttachToGlassPane(editorComponent, overlayPanel, filePath);
        
        // Strategy 2: Use layered pane
        if (!attached) {
            attached = tryAttachToLayeredPane(editorComponent, overlayPanel, filePath);
        }
        
        // Strategy 3: Direct attachment to editor component
        if (!attached) {
            attached = tryDirectAttachment(editorComponent, overlayPanel, filePath);
        }

        if (attached) {
            overlayPanels.put(filePath, overlayPanel);
            LOG.info("✓ Overlay attached successfully to: " + file.getName());
            logComponentHierarchy(editorComponent);
        } else {
            LOG.warn("✗ Failed to attach overlay to: " + file.getName());
        }
    }

    /**
     * Strategy 1: Attach to the root pane's glass pane (most reliable).
     */
    private boolean tryAttachToGlassPane(JComponent editorComponent, OverlayPanel overlayPanel, String filePath) {
        try {
            JRootPane rootPane = SwingUtilities.getRootPane(editorComponent);
            if (rootPane == null) {
                LOG.info("No root pane found");
                return false;
            }

            // Create a custom glass pane that contains our overlay
            JPanel glassPane = new JPanel(null) {
                @Override
                protected void paintComponent(Graphics g) {
                    // Don't paint background - keep transparent
                }
            };
            glassPane.setOpaque(false);
            glassPane.setVisible(true);
            
            // Calculate bounds relative to root pane
            Point editorLocation = SwingUtilities.convertPoint(
                editorComponent, 0, 0, rootPane.getLayeredPane()
            );
            
            overlayPanel.setBounds(
                editorLocation.x, editorLocation.y,
                editorComponent.getWidth(), editorComponent.getHeight()
            );
            
            glassPane.add(overlayPanel);
            
            // Store reference for repositioning
            final JRootPane finalRootPane = rootPane;
            
            // Add component listener to track size/position changes
            editorComponent.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    updateOverlayBounds(editorComponent, overlayPanel, finalRootPane);
                }

                @Override
                public void componentMoved(ComponentEvent e) {
                    updateOverlayBounds(editorComponent, overlayPanel, finalRootPane);
                }

                @Override
                public void componentShown(ComponentEvent e) {
                    overlayPanel.setVisible(true);
                    overlayPanel.repaint();
                }

                @Override
                public void componentHidden(ComponentEvent e) {
                    overlayPanel.setVisible(false);
                }
            });
            
            // Use the layered pane instead of replacing glass pane
            rootPane.getLayeredPane().add(glassPane, JLayeredPane.PALETTE_LAYER);
            glassPane.setBounds(0, 0, rootPane.getWidth(), rootPane.getHeight());
            
            // Also add listener to layered pane for resize
            rootPane.getLayeredPane().addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    glassPane.setBounds(0, 0, finalRootPane.getWidth(), finalRootPane.getHeight());
                    updateOverlayBounds(editorComponent, overlayPanel, finalRootPane);
                }
            });
            
            LOG.info("Attached via layered pane glass approach");
            return true;
            
        } catch (Exception e) {
            LOG.warn("Glass pane attachment failed", e);
            return false;
        }
    }

    private void updateOverlayBounds(JComponent editorComponent, OverlayPanel overlayPanel, JRootPane rootPane) {
        try {
            Point editorLocation = SwingUtilities.convertPoint(
                editorComponent, 0, 0, rootPane.getLayeredPane()
            );
            overlayPanel.setBounds(
                editorLocation.x, editorLocation.y,
                editorComponent.getWidth(), editorComponent.getHeight()
            );
            overlayPanel.revalidate();
            overlayPanel.repaint();
        } catch (Exception e) {
            LOG.debug("Error updating bounds: " + e.getMessage());
        }
    }

    /**
     * Strategy 2: Attach to a JLayeredPane in the component hierarchy.
     */
    private boolean tryAttachToLayeredPane(JComponent editorComponent, OverlayPanel overlayPanel, String filePath) {
        try {
            // Find a layered pane in the hierarchy
            JLayeredPane layeredPane = findLayeredPane(editorComponent);
            if (layeredPane == null) {
                LOG.info("No layered pane found");
                return false;
            }

            LOG.info("Found layered pane: " + layeredPane.getClass().getName());
            
            overlayPanel.setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());
            layeredPane.add(overlayPanel, JLayeredPane.PALETTE_LAYER);
            
            layeredPane.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    overlayPanel.setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());
                    overlayPanel.repaint();
                }
            });
            
            layeredPane.revalidate();
            layeredPane.repaint();
            
            LOG.info("Attached via layered pane");
            return true;
            
        } catch (Exception e) {
            LOG.warn("Layered pane attachment failed", e);
            return false;
        }
    }

    /**
     * Strategy 3: Direct attachment to the editor component.
     */
    private boolean tryDirectAttachment(JComponent editorComponent, OverlayPanel overlayPanel, String filePath) {
        try {
            // Find a suitable container to attach to
            Container foundTarget = findBestContainer(editorComponent);
            if (foundTarget == null) {
                foundTarget = editorComponent;
            }
            
            // Make effectively final for use in inner class
            final Container target = foundTarget;

            LOG.info("Direct attachment to: " + target.getClass().getName());
            
            overlayPanel.setBounds(0, 0, target.getWidth(), target.getHeight());
            target.add(overlayPanel);
            
            // Try to put on top
            if (target instanceof JComponent jc) {
                jc.setComponentZOrder(overlayPanel, 0);
            }
            
            target.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    overlayPanel.setBounds(0, 0, target.getWidth(), target.getHeight());
                    overlayPanel.repaint();
                }
            });
            
            target.revalidate();
            target.repaint();
            
            LOG.info("Attached directly");
            return true;
            
        } catch (Exception e) {
            LOG.warn("Direct attachment failed", e);
            return false;
        }
    }

    @Nullable
    private JLayeredPane findLayeredPane(Component component) {
        // Check component itself
        if (component instanceof JLayeredPane) {
            return (JLayeredPane) component;
        }

        // Check children (BFS)
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                if (child instanceof JLayeredPane) {
                    return (JLayeredPane) child;
                }
            }
            // Recurse into children
            for (Component child : container.getComponents()) {
                JLayeredPane found = findLayeredPane(child);
                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    @Nullable
    private Container findBestContainer(JComponent editorComponent) {
        // Look for scroll pane's viewport
        if (editorComponent instanceof JScrollPane scrollPane) {
            JViewport viewport = scrollPane.getViewport();
            if (viewport != null) {
                Component view = viewport.getView();
                if (view instanceof Container c && c.getWidth() > 0) {
                    return c;
                }
            }
        }

        // Check children for a good candidate
        for (Component child : editorComponent.getComponents()) {
            if (child instanceof JScrollPane sp) {
                JViewport viewport = sp.getViewport();
                if (viewport != null) {
                    Component view = viewport.getView();
                    if (view instanceof Container c && c.getWidth() > 0) {
                        return c;
                    }
                }
            }
            if (child instanceof JPanel panel && panel.getWidth() > 0) {
                return panel;
            }
        }

        return editorComponent;
    }

    /**
     * Checks if a FileEditor is a diagram editor by examining its class name.
     */
    private boolean isDiagramEditor(FileEditor editor) {
        if (editor == null) {
            return false;
        }
        
        String className = editor.getClass().getName().toLowerCase();
        String simpleName = editor.getClass().getSimpleName().toLowerCase();
        
        // Check for diagram-related patterns
        for (String pattern : DIAGRAM_CLASS_PATTERNS) {
            if (className.contains(pattern.toLowerCase()) || 
                simpleName.contains(pattern.toLowerCase())) {
                return true;
            }
        }
        
        // Also check component hierarchy for diagram indicators
        JComponent component = editor.getComponent();
        if (component != null) {
            String componentClass = component.getClass().getName().toLowerCase();
            for (String pattern : DIAGRAM_CLASS_PATTERNS) {
                if (componentClass.contains(pattern.toLowerCase())) {
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * Logs the component hierarchy for debugging.
     */
    private void logComponentHierarchy(JComponent component) {
        LOG.info("=== Component Hierarchy ===");
        logComponentTree(component, 0);
        LOG.info("===========================");
    }

    private void logComponentTree(Component component, int depth) {
        String indent = "  ".repeat(depth);
        String info = String.format("%s%s [%dx%d] visible=%b",
                indent,
                component.getClass().getSimpleName(),
                component.getWidth(),
                component.getHeight(),
                component.isVisible()
        );
        LOG.info(info);
        
        if (depth < 5 && component instanceof Container container) {
            for (Component child : container.getComponents()) {
                logComponentTree(child, depth + 1);
            }
        }
    }

    /**
     * Shows the Colorful Diagrams tool window.
     */
    private void showToolWindow(Project project) {
        if (project == null || project.isDisposed()) {
            return;
        }

        ApplicationManager.getApplication().invokeLater(() -> {
            ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
            ToolWindow toolWindow = toolWindowManager.getToolWindow(ColorfulDiagramsToolWindowFactory.TOOL_WINDOW_ID);
            if (toolWindow != null) {
                toolWindow.show();
            }
        });
    }

    /**
     * Updates the overlay panel metadata for a specific diagram.
     */
    public static void updateOverlayMetadata(String diagramPath, DiagramMetadata metadata) {
        OverlayPanel panel = overlayPanels.get(diagramPath);
        if (panel != null) {
            panel.setMetadata(metadata);
            panel.repaint();
        }
    }

    /**
     * Gets the overlay panel for a diagram path.
     */
    @Nullable
    public static OverlayPanel getOverlayPanel(String diagramPath) {
        return overlayPanels.get(diagramPath);
    }

    /**
     * Forces reattachment of overlay for a diagram.
     */
    public static void reattachOverlay(Project project, String diagramPath) {
        JComponent component = editorComponents.get(diagramPath);
        if (component == null) {
            LOG.warn("No component found for: " + diagramPath);
            return;
        }

        // Remove existing overlay
        OverlayPanel existingPanel = overlayPanels.remove(diagramPath);
        if (existingPanel != null && existingPanel.getParent() != null) {
            existingPanel.getParent().remove(existingPanel);
        }

        // Get the file
        FileEditorManager editorManager = FileEditorManager.getInstance(project);
        for (VirtualFile file : editorManager.getOpenFiles()) {
            if (file.getPath().equals(diagramPath)) {
                new DiagramEditorListener().attachOverlayPanel(project, file, component);
                break;
            }
        }
    }
}
