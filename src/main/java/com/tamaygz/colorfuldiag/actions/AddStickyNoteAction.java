package com.tamaygz.colorfuldiag.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.tamaygz.colorfuldiag.model.DiagramMetadata;
import com.tamaygz.colorfuldiag.model.StickyNoteInfo;
import com.tamaygz.colorfuldiag.ui.StickyNoteDialog;
import org.jetbrains.annotations.NotNull;

/**
 * Action to add a sticky note to the diagram.
 */
public class AddStickyNoteAction extends DiagramActionBase {

    public AddStickyNoteAction() {
        super("Add Sticky Note", "Add a sticky note to the diagram", null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        DiagramMetadata metadata = getMetadata(e);
        if (metadata == null) {
            return;
        }

        StickyNoteDialog dialog = new StickyNoteDialog(e.getProject());
        if (!dialog.showAndGet()) {
            return;
        }

        // Calculate position for the new note
        int x = 100;
        int y = 100;

        // Offset from existing notes to avoid overlap
        if (!metadata.getNotes().isEmpty()) {
            int maxX = 0;
            int maxY = 0;
            for (StickyNoteInfo existing : metadata.getNotes()) {
                int[] pos = existing.getPosition();
                int[] size = existing.getSize();
                if (pos != null && pos.length >= 2) {
                    int width = size != null && size.length >= 2 ? size[0] : 150;
                    int height = size != null && size.length >= 2 ? size[1] : 100;
                    maxX = Math.max(maxX, pos[0] + width);
                    maxY = Math.max(maxY, pos[1] + height);
                }
            }
            // Place new note to the right of existing ones
            x = maxX + 20;
        }

        StickyNoteInfo note = dialog.createStickyNoteInfo(x, y);
        metadata.addNote(note);

        saveMetadata(e, metadata);
        refreshDiagram(e);
    }
}
