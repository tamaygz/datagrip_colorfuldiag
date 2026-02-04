package com.tamaygz.colorfuldiag.diagram;

import com.tamaygz.colorfuldiag.model.DiagramMetadata;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

/**
 * Helper class that provides color resolution from metadata.
 * This integrates our custom colors with the diagram rendering system.
 */
public class DiagramMetadataDataModel {
    
    private final DiagramMetadata metadata;

    public DiagramMetadataDataModel(DiagramMetadata metadata) {
        this.metadata = metadata;
    }

    /**
     * Gets the effective color for a diagram element.
     * Checks metadata for custom color first, then falls back to container color.
     */
    @Nullable
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

    /**
     * Gets the metadata instance.
     */
    public DiagramMetadata getMetadata() {
        return metadata;
    }
}
