package com.tamaygz.colorfuldiag.diagram;

import com.intellij.diagram.DiagramBuilder;
import com.intellij.diagram.DiagramDataModel;
import com.intellij.diagram.DiagramNode;
import com.intellij.diagram.extras.DiagramExtras;
import com.intellij.diagram.extras.providers.DiagramDnDProvider;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Extended diagram functionality for Colorful Diagrams.
 * Provides additional drag-and-drop capabilities for containers.
 */
public class ColorfulDiagramExtras<T> extends DiagramExtras<T> {

    @Override
    public @Nullable DiagramDnDProvider<T> getDnDProvider() {
        return new ColorfulDnDProvider<>();
    }

    /**
     * Custom drag-and-drop provider for handling table drops onto containers.
     */
    private static class ColorfulDnDProvider<T> implements DiagramDnDProvider<T> {

        @Override
        public boolean isAcceptedForDnD(@NotNull Object element) {
            // Accept any diagram element for DnD
            return true;
        }

        @Override
        public @Nullable String getActionForDnD(@NotNull Object element) {
            return "Move to Container";
        }
    }
}
