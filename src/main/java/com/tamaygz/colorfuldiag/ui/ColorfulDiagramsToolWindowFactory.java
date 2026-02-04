package com.tamaygz.colorfuldiag.ui;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;

/**
 * Tool window factory for the Colorful Diagrams toolbox.
 * Creates a dockable tool window with diagram customization controls.
 * Users can dock, float, or reposition this tool window as needed.
 */
public class ColorfulDiagramsToolWindowFactory implements ToolWindowFactory {

    public static final String TOOL_WINDOW_ID = "Colorful Diagrams";

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // Create the main panel
        SimpleToolWindowPanel panel = new SimpleToolWindowPanel(true, true);

        // Create action group with all diagram actions
        DefaultActionGroup actionGroup = createActionGroup();

        // Create action toolbar
        ActionManager actionManager = ActionManager.getInstance();
        var actionToolbar = actionManager.createActionToolbar(
                ActionPlaces.TOOLWINDOW_TOOLBAR_BAR,
                actionGroup,
                false // false = vertical layout, true = horizontal
        );

        // Set the toolbar in the tool window
        panel.setToolbar(actionToolbar.getComponent());

        // Add a content panel to the tool window
        var content = toolWindow.getContentManager().getFactory().createContent(
                panel,
                "Colorful Diagrams",
                false
        );
        toolWindow.getContentManager().addContent(content);
    }

    /**
     * Creates the action group containing all diagram customization actions.
     * Uses registered action IDs from plugin.xml.
     */
    private DefaultActionGroup createActionGroup() {
        DefaultActionGroup group = new DefaultActionGroup("ColorfulDiagrams.ToolWindowActions", false);
        ActionManager actionManager = ActionManager.getInstance();

        // Table coloring
        group.add(actionManager.getAction("ColorfulDiagrams.ColorTable"));

        // Container operations
        group.addSeparator();
        group.add(actionManager.getAction("ColorfulDiagrams.CreateContainer"));
        group.add(actionManager.getAction("ColorfulDiagrams.AddToContainerContext"));
        group.add(actionManager.getAction("ColorfulDiagrams.RemoveFromContainerContext"));

        // Sticky notes
        group.addSeparator();
        group.add(actionManager.getAction("ColorfulDiagrams.AddStickyNote"));

        // Reset and utilities
        group.addSeparator();
        group.add(actionManager.getAction("ColorfulDiagrams.ResetColors"));

        group.addSeparator();
        group.add(actionManager.getAction("ColorfulDiagrams.ExportMetadata"));
        group.add(actionManager.getAction("ColorfulDiagrams.ImportMetadata"));

        return group;
    }
}
