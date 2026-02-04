package com.tamaygz.colorfuldiag.diagram;

import com.intellij.diagram.DiagramDataModel;
import com.intellij.diagram.DiagramNode;
import com.tamaygz.colorfuldiag.model.DiagramMetadata;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Collection;

/**
 * Wrapper around DiagramDataModel that applies custom colors and styling from metadata.
 * This integrates our custom colors with the native diagram rendering.
 */
public class DiagramMetadataDataModel implements DiagramDataModel<Object> {
    
    private final DiagramDataModel<?> delegate;
    private final DiagramMetadata metadata;

    public DiagramMetadataDataModel(DiagramDataModel<?> delegate, DiagramMetadata metadata) {
        this.delegate = delegate;
        this.metadata = metadata;
    }

    /**
     * Gets the effective color for a diagram element.
     * Checks metadata for custom color first, then falls back to default.
     */
    public Color getElementColor(Object element) {
        if (metadata == null || element == null) {
            return null;
        }

        // Try to get the element identifier
        String identifier = String.valueOf(element);
        
        // Check for individual color
        var tableColor = metadata.getTableColor(identifier);
        if (tableColor != null && tableColor.getAwtColor() != null) {
            return tableColor.getAwtColor();
        }

        // Check if element is in a container
        var container = metadata.findContainerForTable(identifier);
        if (container != null && container.getAwtColor() != null) {
            return container.getAwtColor();
        }

        return null;
    }

    @Nullable
    @Override
    public Collection<? extends DiagramNode<Object>> getNodes() {
        return (Collection<? extends DiagramNode<Object>>) delegate.getNodes();
    }

    @Nullable
    @Override
    public String getNodeName(DiagramNode<Object> node) {
        return delegate.getNodeName(node);
    }

    @Nullable
    @Override
    public DiagramNode<Object> getNodeByIdentifier(String id) {
        return (DiagramNode<Object>) delegate.getNodeByIdentifier(id);
    }

    @Override
    public void dispose() {
        delegate.dispose();
    }
}
