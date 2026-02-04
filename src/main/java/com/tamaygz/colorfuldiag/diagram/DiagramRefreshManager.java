package com.tamaygz.colorfuldiag.diagram;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.diagram.DiagramBuilder;
import com.intellij.openapi.project.Project;
import com.tamaygz.colorfuldiag.model.DiagramMetadata;

/**
 * Manages real-time updates to diagram visualization when metadata changes.
 * Ensures overlay panels are updated and diagram is properly refreshed.
 */
public class DiagramRefreshManager {
    
    private final Map<String, DiagramBuilder> diagramBuilders = new WeakHashMap<>();
    private final Map<String, Long> lastRefreshTime = new HashMap<>();
    private static final long MIN_REFRESH_INTERVAL = 100; // ms

    @SuppressWarnings("unused")
    public DiagramRefreshManager(@NotNull Project project) {
        // Project is kept for potential future use
    }

    /**
     * Registers a diagram builder for tracking.
     * Called when a diagram editor is created.
     */
    public void registerDiagram(@NotNull String diagramPath, @NotNull DiagramBuilder builder) {
        diagramBuilders.put(diagramPath, builder);
    }

    /**
     * Updates visualization for a specific diagram after metadata changes.
     * Refreshes both the overlay panel and the diagram view.
     */
    public void refreshDiagramVisualization(@NotNull String diagramPath, @NotNull DiagramMetadata metadata) {
        // Debounce rapid updates
        long now = System.currentTimeMillis();
        Long lastRefresh = lastRefreshTime.get(diagramPath);
        if (lastRefresh != null && (now - lastRefresh) < MIN_REFRESH_INTERVAL) {
            return;
        }
        lastRefreshTime.put(diagramPath, now);

        // Update overlay panel
        DiagramEditorListener.updateOverlayMetadata(diagramPath, metadata);

        // Refresh diagram builder if available
        DiagramBuilder builder = diagramBuilders.get(diagramPath);
        if (builder != null) {
            try {
                // Request diagram update
                builder.update(true, false);
            } catch (Exception e) {
                // Silently fail if diagram is disposed or unavailable
            }
        }
    }

    /**
     * Clears registered diagram on close.
     */
    public void unregisterDiagram(@NotNull String diagramPath) {
        diagramBuilders.remove(diagramPath);
        lastRefreshTime.remove(diagramPath);
    }

    /**
     * Gets color for a specific element in a diagram.
     * Used to determine node coloring.
     */
    @Nullable
    public Color getElementColor(@NotNull String diagramPath, @NotNull String elementId, 
                                  @NotNull DiagramMetadata metadata) {
        // Check for individual table color first
        var tableColor = metadata.getTableColor(elementId);
        if (tableColor != null) {
            return tableColor.getAwtColor();
        }

        // Check if element is in a container
        var container = metadata.findContainerForTable(elementId);
        if (container != null) {
            return container.getAwtColor();
        }

        return null;
    }

    /**
     * Applies visual highlighting to indicate containment relationship.
     * Returns a border color or style hint for a table in a container.
     */
    @Nullable
    public Color getContainerBorderColor(@NotNull String diagramPath, @NotNull String elementId,
                                         @NotNull DiagramMetadata metadata) {
        var container = metadata.findContainerForTable(elementId);
        if (container != null && container.getAwtColor() != null) {
            // Return a darker shade of the container color for the border
            var color = container.getAwtColor();
            return darken(color, 0.3f);
        }
        return null;
    }

    /**
     * Darkens a color by the specified factor.
     */
    private static Color darken(Color color, float factor) {
        int r = Math.max(0, (int) (color.getRed() * (1 - factor)));
        int g = Math.max(0, (int) (color.getGreen() * (1 - factor)));
        int b = Math.max(0, (int) (color.getBlue() * (1 - factor)));
        return new Color(r, g, b, color.getAlpha());
    }
}
