package com.tamaygz.colorfuldiag.diagram;

import com.intellij.diagram.extras.DiagramExtras;
import com.intellij.diagram.extras.providers.DiagramDnDProvider;
import org.jetbrains.annotations.Nullable;

/**
 * Extended diagram functionality for Colorful Diagrams.
 * Provides additional capabilities for the diagram editor.
 */
public class ColorfulDiagramExtras<T> extends DiagramExtras<T> {

    @Override
    public @Nullable DiagramDnDProvider<T> getDnDProvider() {
        // Return null to use default DnD behavior
        // Custom DnD can be implemented when the API requirements are clear
        return null;
    }
}
