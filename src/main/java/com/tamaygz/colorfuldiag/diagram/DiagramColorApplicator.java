package com.tamaygz.colorfuldiag.diagram;

import java.awt.Color;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.project.Project;
import com.tamaygz.colorfuldiag.model.ContainerInfo;
import com.tamaygz.colorfuldiag.model.DiagramMetadata;
import com.tamaygz.colorfuldiag.model.TableColorInfo;
import com.tamaygz.colorfuldiag.persistence.DiagramMetadataService;

/**
 * Utility class for applying colors to diagram elements.
 * This class integrates with the diagram rendering to apply custom colors.
 */
public class DiagramColorApplicator {

    private final DiagramMetadataService metadataService;

    public DiagramColorApplicator(@NotNull Project project) {
        this.metadataService = DiagramMetadataService.getInstance(project);
    }

    /**
     * Gets the effective color for a table.
     * Returns the table's own color, or the container's color if the table is in a container,
     * or null if no color is set.
     */
    @Nullable
    public Color getEffectiveColor(@NotNull String diagramPath, @NotNull String tableId) {
        DiagramMetadata metadata = metadataService.getOrCreateMetadata(diagramPath);

        // Check for individual table color first
        TableColorInfo tableColor = metadata.getTableColor(tableId);
        if (tableColor != null && tableColor.getAwtColor() != null) {
            return tableColor.getAwtColor();
        }

        // Check if table is in a container
        ContainerInfo container = metadata.findContainerForTable(tableId);
        if (container != null && container.getAwtColor() != null) {
            return container.getAwtColor();
        }

        return null;
    }

    /**
     * Gets the header color for a table (slightly darker than background).
     */
    @Nullable
    public Color getHeaderColor(@NotNull String diagramPath, @NotNull String tableId) {
        Color baseColor = getEffectiveColor(diagramPath, tableId);
        if (baseColor == null) {
            return null;
        }
        return darken(baseColor, 0.15f);
    }

    /**
     * Gets the border color for a table.
     */
    @Nullable
    public Color getBorderColor(@NotNull String diagramPath, @NotNull String tableId) {
        Color baseColor = getEffectiveColor(diagramPath, tableId);
        if (baseColor == null) {
            return null;
        }
        return darken(baseColor, 0.3f);
    }

    /**
     * Darkens a color by a given factor.
     */
    private static Color darken(Color color, float factor) {
        int r = Math.max(0, (int) (color.getRed() * (1 - factor)));
        int g = Math.max(0, (int) (color.getGreen() * (1 - factor)));
        int b = Math.max(0, (int) (color.getBlue() * (1 - factor)));
        return new Color(r, g, b, color.getAlpha());
    }

    /**
     * Gets a contrasting text color (black or white) for the given background.
     */
    public static Color getContrastingTextColor(Color background) {
        if (background == null) {
            return Color.BLACK;
        }
        // Calculate relative luminance
        double luminance = 0.299 * background.getRed()
                + 0.587 * background.getGreen()
                + 0.114 * background.getBlue();
        return luminance > 128 ? Color.BLACK : Color.WHITE;
    }

    /**
     * Checks if a table has a custom color set.
     */
    public boolean hasCustomColor(@NotNull String diagramPath, @NotNull String tableId) {
        return getEffectiveColor(diagramPath, tableId) != null;
    }

    /**
     * Gets color with transparency for overlays.
     */
    public static Color withAlpha(Color color, int alpha) {
        if (color == null) {
            return null;
        }
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
}
