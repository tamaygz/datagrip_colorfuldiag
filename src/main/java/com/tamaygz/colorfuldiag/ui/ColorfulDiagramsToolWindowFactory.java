package com.tamaygz.colorfuldiag.ui;

import java.awt.Component;
import java.awt.Font;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.tamaygz.colorfuldiag.diagram.DiagramEditorFactoryListener;
import com.tamaygz.colorfuldiag.diagram.DiagramEditorListener;
import com.tamaygz.colorfuldiag.diagram.OverlayPanel;
import com.tamaygz.colorfuldiag.model.DiagramMetadata;

/**
 * Tool window factory for the Colorful Diagrams toolbox.
 * Creates a dockable tool window with diagram customization controls and status info.
 */
public class ColorfulDiagramsToolWindowFactory implements ToolWindowFactory {

    public static final String TOOL_WINDOW_ID = "Colorful Diagrams";

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // Create the main panel
        SimpleToolWindowPanel panel = new SimpleToolWindowPanel(true, true);

        // Create action group with all diagram actions
        DefaultActionGroup actionGroup = createActionGroup(project);

        // Create action toolbar
        ActionManager actionManager = ActionManager.getInstance();
        var actionToolbar = actionManager.createActionToolbar(
                ActionPlaces.TOOLWINDOW_TOOLBAR_BAR,
                actionGroup,
                false // false = vertical layout
        );

        // Set the target component for toolbar actions
        actionToolbar.setTargetComponent(panel);

        // Set the toolbar in the tool window
        panel.setToolbar(actionToolbar.getComponent());

        // Create status panel
        JPanel statusPanel = createStatusPanel(project);
        panel.setContent(new JBScrollPane(statusPanel));

        // Add a content panel to the tool window
        var content = toolWindow.getContentManager().getFactory().createContent(
                panel,
                "Colorful Diagrams",
                false
        );
        toolWindow.getContentManager().addContent(content);

        // Set up periodic status refresh
        Timer statusRefreshTimer = new Timer(true);
        statusRefreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!project.isDisposed()) {
                    SwingUtilities.invokeLater(() -> updateStatusPanel(statusPanel, project));
                } else {
                    cancel();
                }
            }
        }, 1000, 2000); // Update every 2 seconds
    }

    /**
     * Creates the status panel showing current diagram and overlay status.
     */
    private JPanel createStatusPanel(Project project) {
        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));
        statusPanel.setBorder(JBUI.Borders.empty(8));

        // Title
        JBLabel titleLabel = new JBLabel("Status");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusPanel.add(titleLabel);
        statusPanel.add(Box.createVerticalStrut(8));

        // Status labels (will be updated)
        statusPanel.add(createStatusLabel("diagram-status", "Current Diagram: None"));
        statusPanel.add(createStatusLabel("overlay-status", "Overlay: Not attached"));
        statusPanel.add(createStatusLabel("metadata-status", "Metadata: -"));
        statusPanel.add(createStatusLabel("details-status", ""));

        statusPanel.add(Box.createVerticalStrut(16));

        // Help text
        JBLabel helpLabel = new JBLabel("<html><b>Tips:</b><br>" +
                "• Open a database diagram to enable features<br>" +
                "• Use actions above to customize<br>" +
                "• Click 'Refresh Overlay' if elements don't appear</html>");
        helpLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        helpLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        statusPanel.add(helpLabel);

        statusPanel.add(Box.createVerticalGlue());

        return statusPanel;
    }

    private JBLabel createStatusLabel(String name, String text) {
        JBLabel label = new JBLabel(text);
        label.setName(name);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(JBUI.Borders.empty(2, 0));
        return label;
    }

    private void updateStatusPanel(JPanel statusPanel, Project project) {
        try {
            FileEditorManager editorManager = FileEditorManager.getInstance(project);
            VirtualFile[] selectedFiles = editorManager.getSelectedFiles();

            String diagramStatus = "Current Diagram: None";
            String overlayStatus = "Overlay: Not attached";
            String metadataStatus = "Metadata: -";
            String detailsStatus = "";
            
            // First check if we have any overlay at all
            var allOverlays = DiagramEditorListener.getAllOverlayPanels();
            if (!allOverlays.isEmpty()) {
                // We have overlays - find the relevant one
                for (Map.Entry<String, OverlayPanel> entry : allOverlays.entrySet()) {
                    String path = entry.getKey();
                    OverlayPanel overlay = entry.getValue();
                    
                    // Extract diagram name from path
                    String name = path;
                    int lastSlash = path.lastIndexOf('/');
                    int lastBackslash = path.lastIndexOf('\\');
                    int lastDot = Math.max(path.lastIndexOf('.'), Math.max(lastSlash, lastBackslash));
                    if (lastDot > 0) {
                        name = path.substring(Math.max(lastSlash, lastBackslash) + 1);
                    }
                    
                    diagramStatus = "Diagram: " + name;
                    overlayStatus = "Overlay: ✓ Attached";
                    
                    // Get metadata from overlay
                    DiagramMetadata metadata = overlay.getMetadata();
                    if (metadata != null) {
                        int noteCount = metadata.getNotes().size();
                        int containerCount = metadata.getContainers().size();
                        int colorCount = metadata.getTables().size();
                        metadataStatus = String.format("Notes: %d | Containers: %d | Colors: %d",
                                noteCount, containerCount, colorCount);
                    }
                    break; // Use first overlay found
                }
            } else if (selectedFiles.length > 0) {
                // No overlays yet, show selected file info
                VirtualFile file = selectedFiles[0];
                FileEditor editor = editorManager.getSelectedEditor(file);
                
                if (editor != null) {
                    String editorClass = editor.getClass().getSimpleName();
                    diagramStatus = "Editor: " + file.getName();
                    detailsStatus = "Type: " + editorClass;
                    
                    // Check if it looks like a diagram
                    String className = editorClass.toLowerCase();
                    if (className.contains("diagram") || className.contains("graph") ||
                        className.contains("uml") || className.contains("database")) {
                        overlayStatus = "Overlay: Pending...";
                    }
                }
            }

            // Update labels
            for (Component comp : statusPanel.getComponents()) {
                if (comp instanceof JBLabel label) {
                    switch (label.getName()) {
                        case "diagram-status" -> label.setText(diagramStatus);
                        case "overlay-status" -> label.setText(overlayStatus);
                        case "metadata-status" -> label.setText(metadataStatus);
                        case "details-status" -> label.setText(detailsStatus);
                    }
                }
            }
        } catch (Exception e) {
            // Ignore errors during status update
        }
    }

    /**
     * Creates the action group containing all diagram customization actions.
     */
    private DefaultActionGroup createActionGroup(Project project) {
        DefaultActionGroup group = new DefaultActionGroup("ColorfulDiagrams.ToolWindowActions", false);
        ActionManager actionManager = ActionManager.getInstance();

        // Refresh overlay action (custom)
        group.add(new RefreshOverlayAction(project));
        group.addSeparator();

        // Table coloring
        addActionSafe(group, actionManager, "ColorfulDiagrams.ColorTable");

        // Container operations
        group.addSeparator();
        addActionSafe(group, actionManager, "ColorfulDiagrams.CreateContainer");

        // Sticky notes
        group.addSeparator();
        addActionSafe(group, actionManager, "ColorfulDiagrams.AddStickyNote");

        // Reset and utilities
        group.addSeparator();
        addActionSafe(group, actionManager, "ColorfulDiagrams.ResetColors");

        group.addSeparator();
        addActionSafe(group, actionManager, "ColorfulDiagrams.ExportMetadata");
        addActionSafe(group, actionManager, "ColorfulDiagrams.ImportMetadata");

        return group;
    }

    /**
     * Safely adds an action to the group, only if it exists.
     */
    private void addActionSafe(DefaultActionGroup group, ActionManager actionManager, String actionId) {
        AnAction action = actionManager.getAction(actionId);
        if (action != null) {
            group.add(action);
        }
    }

    /**
     * Action to manually refresh/reattach overlays.
     */
    private static class RefreshOverlayAction extends DumbAwareAction {
        private static final Icon REFRESH_ICON = IconLoader.getIcon("/icons/refresh.svg", RefreshOverlayAction.class);
        
        private final Project project;

        RefreshOverlayAction(Project project) {
            super("Refresh Overlay", "Reattach overlay panel to current diagram", REFRESH_ICON);
            this.project = project;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            DiagramEditorFactoryListener.tryAttachOverlayToCurrentDiagram(project);
            DiagramEditorFactoryListener.refreshAllOverlays(project);
        }
    }
}
