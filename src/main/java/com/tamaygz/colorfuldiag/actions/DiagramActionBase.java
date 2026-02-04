package com.tamaygz.colorfuldiag.actions;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.swing.Icon;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.diagram.DiagramBuilder;
import com.intellij.diagram.DiagramDataKeys;
import com.intellij.diagram.DiagramDataModel;
import com.intellij.diagram.DiagramNode;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.tamaygz.colorfuldiag.diagram.DiagramEditorListener;
import com.tamaygz.colorfuldiag.diagram.OverlayPanel;
import com.tamaygz.colorfuldiag.model.DiagramMetadata;
import com.tamaygz.colorfuldiag.persistence.DiagramMetadataService;

/**
 * Base class for all diagram-related actions.
 * Provides common functionality for accessing diagram context.
 * 
 * Actions can be invoked from:
 * 1. Diagram context menu (has DiagramBuilder context)
 * 2. Tool window toolbar (no DiagramBuilder, needs to find active diagram)
 */
public abstract class DiagramActionBase extends AnAction {

    public DiagramActionBase() {
        super();
    }

    public DiagramActionBase(@Nullable String text) {
        super(text);
    }

    public DiagramActionBase(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
        super(text, description, icon);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    /**
     * Gets the DiagramBuilder from the action event context.
     */
    @Nullable
    protected DiagramBuilder getDiagramBuilder(@NotNull AnActionEvent e) {
        return e.getData(DiagramDataKeys.BUILDER);
    }

    /**
     * Gets the DiagramDataModel from the action event.
     */
    @Nullable
    protected DiagramDataModel<?> getDiagramModel(@NotNull AnActionEvent e) {
        DiagramBuilder builder = getDiagramBuilder(e);
        return builder != null ? builder.getDataModel() : null;
    }

    /**
     * Gets selected nodes from the diagram.
     */
    @NotNull
    protected Collection<DiagramNode<?>> getSelectedNodes(@NotNull AnActionEvent e) {
        DiagramBuilder builder = getDiagramBuilder(e);
        if (builder == null) {
            return Collections.emptyList();
        }

        // Get selected nodes from the builder's data model
        DiagramDataModel<?> model = builder.getDataModel();
        if (model == null) {
            return Collections.emptyList();
        }

        // Return all nodes as a fallback - actions can filter as needed
        Collection<? extends DiagramNode<?>> allNodes = model.getNodes();
        return allNodes != null ? new java.util.ArrayList<>(allNodes) : Collections.emptyList();
    }

    /**
     * Gets the current diagram file.
     */
    @Nullable
    protected VirtualFile getDiagramFile(@NotNull AnActionEvent e) {
        // First try from event context
        VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
        if (file != null) {
            return file;
        }
        
        // Fallback: try to get from currently selected editor
        Project project = e.getProject();
        if (project != null) {
            FileEditorManager editorManager = FileEditorManager.getInstance(project);
            VirtualFile[] selectedFiles = editorManager.getSelectedFiles();
            if (selectedFiles.length > 0) {
                return selectedFiles[0];
            }
        }
        
        return null;
    }

    /**
     * Gets the diagram path for metadata storage.
     * Falls back to any active overlay path if no direct path available.
     */
    @Nullable
    protected String getDiagramPath(@NotNull AnActionEvent e) {
        VirtualFile file = getDiagramFile(e);
        if (file != null) {
            return file.getPath();
        }
        
        // Fallback: get path from any active overlay
        Map<String, OverlayPanel> overlays = DiagramEditorListener.getAllOverlayPanels();
        if (!overlays.isEmpty()) {
            return overlays.keySet().iterator().next();
        }
        
        return null;
    }

    /**
     * Gets the metadata service.
     */
    @Nullable
    protected DiagramMetadataService getMetadataService(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        return project != null ? DiagramMetadataService.getInstance(project) : null;
    }

    /**
     * Gets or creates metadata for the current diagram.
     * Can work from overlay context when DiagramBuilder isn't available.
     */
    @Nullable
    protected DiagramMetadata getMetadata(@NotNull AnActionEvent e) {
        // First try to get from overlay directly (faster)
        String diagramPath = getDiagramPath(e);
        if (diagramPath != null) {
            OverlayPanel overlay = DiagramEditorListener.getOverlayPanel(diagramPath);
            if (overlay != null && overlay.getMetadata() != null) {
                return overlay.getMetadata();
            }
        }
        
        // Fallback: load from service
        DiagramMetadataService service = getMetadataService(e);
        if (service == null || diagramPath == null) {
            // Last resort: try any overlay
            OverlayPanel anyOverlay = DiagramEditorListener.getAnyOverlayPanel();
            if (anyOverlay != null) {
                return anyOverlay.getMetadata();
            }
            return null;
        }

        return service.getOrCreateMetadata(diagramPath);
    }
    
    /**
     * Gets the active overlay panel for the current context.
     */
    @Nullable
    protected OverlayPanel getActiveOverlay(@NotNull AnActionEvent e) {
        String diagramPath = getDiagramPath(e);
        if (diagramPath != null) {
            OverlayPanel overlay = DiagramEditorListener.getOverlayPanel(diagramPath);
            if (overlay != null) {
                return overlay;
            }
        }
        
        // Fallback to any available overlay
        return DiagramEditorListener.getAnyOverlayPanel();
    }

    /**
     * Saves metadata for the current diagram.
     */
    protected void saveMetadata(@NotNull AnActionEvent e, @NotNull DiagramMetadata metadata) {
        Project project = e.getProject();
        DiagramMetadataService service = getMetadataService(e);
        String diagramPath = getDiagramPath(e);

        if (service != null && diagramPath != null) {
            service.saveMetadataToPath(
                    DiagramMetadataService.getMetadataFilePath(diagramPath),
                    metadata
            );
            
            // Update the overlay panel with the new metadata
            DiagramEditorListener.updateOverlayMetadata(diagramPath, metadata);
        }
        
        // Also update any active overlay directly
        OverlayPanel overlay = getActiveOverlay(e);
        if (overlay != null) {
            overlay.setMetadata(metadata);
            overlay.repaint();
        }

        // Ensure overlay is attached to the diagram
        if (project != null) {
            com.tamaygz.colorfuldiag.diagram.DiagramEditorFactoryListener.tryAttachOverlayToCurrentDiagram(project);
        }
    }

    /**
     * Refreshes the diagram view.
     */
    protected void refreshDiagram(@NotNull AnActionEvent e) {
        DiagramBuilder builder = getDiagramBuilder(e);
        if (builder != null) {
            try {
                builder.update(true, false);
            } catch (Exception ex) {
                // Ignore refresh errors
            }
        }
        
        // Also refresh overlay
        OverlayPanel overlay = getActiveOverlay(e);
        if (overlay != null) {
            overlay.repaint();
        }
    }

    /**
     * Gets the table identifier from a diagram node.
     */
    @Nullable
    protected String getTableId(@NotNull DiagramNode<?> node) {
        Object element = node.getIdentifyingElement();
        if (element != null) {
            // For database tables, this is typically the fully qualified name
            return element.toString();
        }
        return null;
    }

    /**
     * Checks if action should be enabled.
     * Enables when we have a diagram builder OR an active overlay.
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        DiagramBuilder builder = getDiagramBuilder(e);
        boolean hasOverlay = DiagramEditorListener.getAnyOverlayPanel() != null;
        
        // Enable if we have a diagram builder OR an active overlay
        e.getPresentation().setEnabledAndVisible(builder != null || hasOverlay);
    }
    
    /**
     * Checks if the action requires selected nodes to be enabled.
     * Override in subclasses that need selection.
     */
    protected boolean requiresSelection() {
        return false;
    }
}
