package com.tamaygz.colorfuldiag.diagram;

import com.tamaygz.colorfuldiag.model.ContainerInfo;
import com.tamaygz.colorfuldiag.model.DiagramMetadata;
import com.tamaygz.colorfuldiag.model.StickyNoteInfo;
import com.intellij.openapi.diagnostic.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/**
 * Overlay panel that renders containers and sticky notes on top of the diagram.
 * This is a transparent panel that overlays the diagram editor.
 */
public class OverlayPanel extends JPanel {

    private DiagramMetadata metadata;
    private ContainerInfo selectedContainer;
    private StickyNoteInfo selectedNote;
    private Point dragStart;
    private boolean isDragging;
    private boolean isResizing;
    private ResizeHandle resizeHandle;

    private static final int HANDLE_SIZE = 8;
    private static final int CONTAINER_ARC = 10;
    private static final int NOTE_ARC = 5;
    private static final float CONTAINER_ALPHA = 0.15f;
    private static final float NOTE_ALPHA = 0.9f;

    private enum ResizeHandle {
        NONE, N, S, E, W, NE, NW, SE, SW
    }

    public OverlayPanel() {
        setOpaque(false);
        setLayout(null);
        setupMouseListeners();
    }

    public void setMetadata(DiagramMetadata metadata) {
        this.metadata = metadata;
        repaint();
    }

    private void setupMouseListeners() {
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleMousePressed(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                handleMouseReleased(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                handleMouseDragged(e);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                updateCursor(e);
            }
        };

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    private void handleMousePressed(MouseEvent e) {
        Point p = e.getPoint();
        dragStart = p;

        // Check for sticky note selection first (they're on top)
        selectedNote = findNoteAt(p);
        if (selectedNote != null) {
            resizeHandle = getResizeHandle(selectedNote, p);
            if (resizeHandle != ResizeHandle.NONE) {
                isResizing = true;
            } else {
                isDragging = true;
            }
            selectedContainer = null;
            repaint();
            return;
        }

        // Check for container selection
        selectedContainer = findContainerAt(p);
        if (selectedContainer != null) {
            resizeHandle = getResizeHandle(selectedContainer, p);
            if (resizeHandle != ResizeHandle.NONE) {
                isResizing = true;
            } else {
                isDragging = true;
            }
            repaint();
        }
    }

    private void handleMouseReleased(MouseEvent e) {
        isDragging = false;
        isResizing = false;
        resizeHandle = ResizeHandle.NONE;
        dragStart = null;
    }

    private void handleMouseDragged(MouseEvent e) {
        if (dragStart == null) return;

        Point p = e.getPoint();
        int dx = p.x - dragStart.x;
        int dy = p.y - dragStart.y;

        if (isResizing) {
            if (selectedNote != null) {
                resizeNote(selectedNote, dx, dy);
            } else if (selectedContainer != null) {
                resizeContainer(selectedContainer, dx, dy);
            }
        } else if (isDragging) {
            if (selectedNote != null) {
                moveNote(selectedNote, dx, dy);
            } else if (selectedContainer != null) {
                moveContainer(selectedContainer, dx, dy);
            }
        }

        dragStart = p;
        repaint();
    }

    private void updateCursor(MouseEvent e) {
        Point p = e.getPoint();

        StickyNoteInfo note = findNoteAt(p);
        if (note != null) {
            ResizeHandle handle = getResizeHandle(note, p);
            setCursor(getCursorForHandle(handle));
            return;
        }

        ContainerInfo container = findContainerAt(p);
        if (container != null) {
            ResizeHandle handle = getResizeHandle(container, p);
            setCursor(getCursorForHandle(handle));
            return;
        }

        setCursor(Cursor.getDefaultCursor());
    }

    private Cursor getCursorForHandle(ResizeHandle handle) {
        return switch (handle) {
            case N, S -> Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
            case E, W -> Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
            case NE, SW -> Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
            case NW, SE -> Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);
            default -> Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
        };
    }

    private ContainerInfo findContainerAt(Point p) {
        if (metadata == null) return null;
        List<ContainerInfo> containers = metadata.getContainers();
        // Search in reverse order (top-most first)
        for (int i = containers.size() - 1; i >= 0; i--) {
            ContainerInfo container = containers.get(i);
            Rectangle bounds = container.getBoundsAsRectangle();
            if (bounds.contains(p)) {
                return container;
            }
        }
        return null;
    }

    private StickyNoteInfo findNoteAt(Point p) {
        if (metadata == null) return null;
        List<StickyNoteInfo> notes = metadata.getNotes();
        // Search in reverse order (top-most first)
        for (int i = notes.size() - 1; i >= 0; i--) {
            StickyNoteInfo note = notes.get(i);
            Rectangle bounds = new Rectangle(
                    note.getPositionAsPoint(),
                    note.getSizeAsDimension()
            );
            if (bounds.contains(p)) {
                return note;
            }
        }
        return null;
    }

    private ResizeHandle getResizeHandle(ContainerInfo container, Point p) {
        Rectangle bounds = container.getBoundsAsRectangle();
        return getResizeHandleForRect(bounds, p);
    }

    private ResizeHandle getResizeHandle(StickyNoteInfo note, Point p) {
        Rectangle bounds = new Rectangle(
                note.getPositionAsPoint(),
                note.getSizeAsDimension()
        );
        return getResizeHandleForRect(bounds, p);
    }

    private ResizeHandle getResizeHandleForRect(Rectangle bounds, Point p) {
        boolean onLeft = Math.abs(p.x - bounds.x) <= HANDLE_SIZE;
        boolean onRight = Math.abs(p.x - (bounds.x + bounds.width)) <= HANDLE_SIZE;
        boolean onTop = Math.abs(p.y - bounds.y) <= HANDLE_SIZE;
        boolean onBottom = Math.abs(p.y - (bounds.y + bounds.height)) <= HANDLE_SIZE;

        if (onTop && onLeft) return ResizeHandle.NW;
        if (onTop && onRight) return ResizeHandle.NE;
        if (onBottom && onLeft) return ResizeHandle.SW;
        if (onBottom && onRight) return ResizeHandle.SE;
        if (onTop) return ResizeHandle.N;
        if (onBottom) return ResizeHandle.S;
        if (onLeft) return ResizeHandle.W;
        if (onRight) return ResizeHandle.E;

        return ResizeHandle.NONE;
    }

    private void moveContainer(ContainerInfo container, int dx, int dy) {
        int[] bounds = container.getBounds();
        bounds[0] += dx;
        bounds[1] += dy;
        container.setBounds(bounds);
    }

    private void resizeContainer(ContainerInfo container, int dx, int dy) {
        int[] bounds = container.getBounds();
        switch (resizeHandle) {
            case N -> { bounds[1] += dy; bounds[3] -= dy; }
            case S -> bounds[3] += dy;
            case E -> bounds[2] += dx;
            case W -> { bounds[0] += dx; bounds[2] -= dx; }
            case NE -> { bounds[1] += dy; bounds[3] -= dy; bounds[2] += dx; }
            case NW -> { bounds[0] += dx; bounds[2] -= dx; bounds[1] += dy; bounds[3] -= dy; }
            case SE -> { bounds[2] += dx; bounds[3] += dy; }
            case SW -> { bounds[0] += dx; bounds[2] -= dx; bounds[3] += dy; }
            default -> {}
        }
        // Enforce minimum size
        bounds[2] = Math.max(100, bounds[2]);
        bounds[3] = Math.max(50, bounds[3]);
        container.setBounds(bounds);
    }

    private void moveNote(StickyNoteInfo note, int dx, int dy) {
        int[] pos = note.getPosition();
        pos[0] += dx;
        pos[1] += dy;
        note.setPosition(pos);
    }

    private void resizeNote(StickyNoteInfo note, int dx, int dy) {
        int[] size = note.getSize();
        int[] pos = note.getPosition();
        switch (resizeHandle) {
            case N -> { pos[1] += dy; size[1] -= dy; }
            case S -> size[1] += dy;
            case E -> size[0] += dx;
            case W -> { pos[0] += dx; size[0] -= dx; }
            case NE -> { pos[1] += dy; size[1] -= dy; size[0] += dx; }
            case NW -> { pos[0] += dx; size[0] -= dx; pos[1] += dy; size[1] -= dy; }
            case SE -> { size[0] += dx; size[1] += dy; }
            case SW -> { pos[0] += dx; size[0] -= dx; size[1] += dy; }
            default -> {}
        }
        // Enforce minimum size
        size[0] = Math.max(80, size[0]);
        size[1] = Math.max(40, size[1]);
        note.setSize(size);
        note.setPosition(pos);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Debug: Draw border to show overlay is attached
        g2d.setColor(new Color(0, 150, 255, 50));
        g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                1, new float[]{5, 5}, 0));
        g2d.drawRect(0, 0, getWidth() - 1, getHeight() - 1);

        if (metadata == null) {
            // No metadata yet - show empty state
            g2d.setColor(new Color(100, 100, 100, 100));
            g2d.setFont(g2d.getFont().deriveFont(11f));
            g2d.drawString("Colorful Diagrams Ready - Add sticky notes or containers", 10, 30);
            g2d.dispose();
            return;
        }

        // Draw containers first (they're behind everything)
        for (ContainerInfo container : metadata.getContainers()) {
            drawContainer(g2d, container);
        }

        // Draw sticky notes on top
        for (StickyNoteInfo note : metadata.getNotes()) {
            drawStickyNote(g2d, note);
        }

        g2d.dispose();
    }

    private void drawContainer(Graphics2D g2d, ContainerInfo container) {
        Rectangle bounds = container.getBoundsAsRectangle();
        Color color = container.getAwtColor();
        if (color == null) {
            color = new Color(0x45B7D1);
        }

        // Draw background with transparency
        Color bgColor = new Color(
                color.getRed(), color.getGreen(), color.getBlue(),
                (int) (255 * CONTAINER_ALPHA)
        );
        g2d.setColor(bgColor);
        g2d.fill(new RoundRectangle2D.Float(
                bounds.x, bounds.y, bounds.width, bounds.height,
                CONTAINER_ARC, CONTAINER_ARC
        ));

        // Draw border
        boolean isSelected = container.equals(selectedContainer);
        g2d.setColor(isSelected ? color.darker() : color);
        g2d.setStroke(new BasicStroke(isSelected ? 2 : 1,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                1, new float[]{5, 3}, 0));
        g2d.draw(new RoundRectangle2D.Float(
                bounds.x, bounds.y, bounds.width, bounds.height,
                CONTAINER_ARC, CONTAINER_ARC
        ));

        // Draw title bar
        g2d.setColor(color);
        g2d.fillRoundRect(bounds.x, bounds.y,
                Math.min(bounds.width, g2d.getFontMetrics().stringWidth(container.getTitle()) + 20),
                20, 5, 5);

        // Draw title text
        g2d.setColor(DiagramColorApplicator.getContrastingTextColor(color));
        g2d.setFont(g2d.getFont().deriveFont(Font.BOLD, 11f));
        g2d.drawString(container.getTitle() != null ? container.getTitle() : "",
                bounds.x + 5, bounds.y + 14);
    }

    private void drawStickyNote(Graphics2D g2d, StickyNoteInfo note) {
        Point pos = note.getPositionAsPoint();
        Dimension size = note.getSizeAsDimension();
        Color color = note.getAwtColor();

        // Draw shadow
        g2d.setColor(new Color(0, 0, 0, 30));
        g2d.fillRoundRect(pos.x + 3, pos.y + 3, size.width, size.height, NOTE_ARC, NOTE_ARC);

        // Draw background
        Color bgColor = new Color(
                color.getRed(), color.getGreen(), color.getBlue(),
                (int) (255 * NOTE_ALPHA)
        );
        g2d.setColor(bgColor);
        g2d.fillRoundRect(pos.x, pos.y, size.width, size.height, NOTE_ARC, NOTE_ARC);

        // Draw border
        boolean isSelected = note.equals(selectedNote);
        g2d.setColor(isSelected ? color.darker().darker() : color.darker());
        g2d.setStroke(new BasicStroke(isSelected ? 2 : 1));
        g2d.drawRoundRect(pos.x, pos.y, size.width, size.height, NOTE_ARC, NOTE_ARC);

        // Draw fold corner
        int foldSize = 12;
        g2d.setColor(color.darker());
        g2d.fillPolygon(
                new int[]{pos.x + size.width - foldSize, pos.x + size.width, pos.x + size.width},
                new int[]{pos.y + size.height, pos.y + size.height - foldSize, pos.y + size.height},
                3
        );

        // Draw text
        if (note.getText() != null && !note.getText().isEmpty()) {
            g2d.setColor(DiagramColorApplicator.getContrastingTextColor(color));
            g2d.setFont(g2d.getFont().deriveFont(11f));

            // Word wrap text
            FontMetrics fm = g2d.getFontMetrics();
            int lineHeight = fm.getHeight();
            int textX = pos.x + 5;
            int textY = pos.y + lineHeight;
            int maxWidth = size.width - 10;
            int maxHeight = size.height - 10;

            String[] words = note.getText().split(" ");
            StringBuilder line = new StringBuilder();
            for (String word : words) {
                String testLine = line.length() > 0 ? line + " " + word : word;
                if (fm.stringWidth(testLine) > maxWidth) {
                    if (textY + lineHeight > pos.y + maxHeight) break;
                    g2d.drawString(line.toString(), textX, textY);
                    textY += lineHeight;
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(testLine);
                }
            }
            if (line.length() > 0 && textY <= pos.y + maxHeight) {
                g2d.drawString(line.toString(), textX, textY);
            }
        }
    }
}
