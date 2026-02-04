package com.tamaygz.colorfuldiag.actions;

import com.intellij.diagram.DiagramBuilder;
import com.intellij.diagram.DiagramDataKeys;
import com.intellij.diagram.DiagramDataModel;
import com.intellij.diagram.DiagramNode;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.tamaygz.colorfuldiag.model.DiagramMetadata;
import com.tamaygz.colorfuldiag.persistence.DiagramMetadataService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.Collections;

/**
 * Base class for all diagram-related actions.
 * Provides common functionality for accessing diagram context.
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
        return e.getData(CommonDataKeys.VIRTUAL_FILE);
    }

    /**
     * Gets the diagram path for metadata storage.
     */
    @Nullable
    protected String getDiagramPath(@NotNull AnActionEvent e) {
        VirtualFile file = getDiagramFile(e);
        return file != null ? file.getPath() : null;
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
     */
    @Nullable
    protected DiagramMetadata getMetadata(@NotNull AnActionEvent e) {
        DiagramMetadataService service = getMetadataService(e);
        String diagramPath = getDiagramPath(e);

        if (service == null || diagramPath == null) {
            return null;
        }

        return service.getOrCreateMetadata(diagramPath);
    }

    /**
     * Saves metadata for the current diagram.
     */
    protected void saveMetadata(@NotNull AnActionEvent e, @NotNull DiagramMetadata metadata) {
        DiagramMetadataService service = getMetadataService(e);
        String diagramPath = getDiagramPath(e);

        if (service != null && diagramPath != null) {
            service.saveMetadataToPath(
                    DiagramMetadataService.getMetadataFilePath(diagramPath),
                    metadata
            );
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

    @Override
    public void update(@NotNull AnActionEvent e) {
        // By default, enable action only when we have a diagram builder
        DiagramBuilder builder = getDiagramBuilder(e);
        e.getPresentation().setEnabledAndVisible(builder != null);
    }
}
