package com.tamaygz.colorfuldiag.diagram;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ColorChooserService;
import com.tamaygz.colorfuldiag.model.ContainerInfo;
import com.tamaygz.colorfuldiag.model.DiagramMetadata;
import com.tamaygz.colorfuldiag.model.StickyNoteInfo;
import com.tamaygz.colorfuldiag.services.DiagramMetadataService;
import com.tamaygz.colorfuldiag.ui.QuickColorPickerPopup;
import com.tamaygz.colorfuldiag.ui.RenameContainerDialog;

/**
 * Overlay panel that renders containers and sticky notes on top of the diagram.
 * This is a transparent panel that overlays the diagram editor.
 */
public class OverlayPanel extends JPanel {

    private static final Logger LOG = Logger.getInstance(OverlayPanel.class);
    
    private DiagramMetadata metadata;
    private Project project;
    private String diagramPath;
    private ContainerInfo selectedContainer;
    private StickyNoteInfo selectedNote;
    private Point dragStart;
    private boolean isDragging;
    private boolean isResizing;
    private ResizeHandle resizeHandle;
    private DrawingMode drawingMode = DrawingMode.NONE;
    private Point drawingStart;
    private Rectangle drawingPreview;
    
    // Callback for when metadata changes
    private Consumer<DiagramMetadata> onMetadataChanged;
    
    // Debug mode - set to true to show debug border
    private static final boolean DEBUG_MODE = false;

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
        setDoubleBuffered(true);
        setupMouseListeners();
        LOG.info("OverlayPanel created");
    }
    
    public void setProject(Project project) {
        this.project = project;
    }
    
    public void setDiagramPath(String path) {
        this.diagramPath = path;
    }
    
    public void setDrawingMode(DrawingMode mode) {
        this.drawingMode = mode;
        drawingPreview = null;
        if (mode != DrawingMode.NONE) {
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        } else {
            setCursor(Cursor.getDefaultCursor());
        }
        repaint();
    }
    
    public DrawingMode getDrawingMode() {
        return drawingMode;
    }
    
    public void setOnMetadataChanged(Consumer<DiagramMetadata> callback) {
        this.onMetadataChanged = callback;
    }
    
    private void notifyMetadataChanged() {
        if (onMetadataChanged != null && metadata != null) {
            onMetadataChanged.accept(metadata);
        }
        // Also save via service if project available
        if (project != null && diagramPath != null && metadata != null) {
            DiagramMetadataService.getInstance(project).saveMetadata(diagramPath, metadata);
        }
    }

    public void setMetadata(DiagramMetadata metadata) {
        this.metadata = metadata;
        repaint();
    }
    
    public DiagramMetadata getMetadata() {
        return metadata;
    }

    private void setupMouseListeners() {
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    handleRightClick(e);
                } else {
                    handleMousePressed(e);
                }
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
            
            @Override
            public void mouseClicked(MouseEvent e) {
                // Double-click to edit sticky note or rename container
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    StickyNoteInfo note = findNoteAt(e.getPoint());
                    if (note != null) {
                        showEditNoteDialog(note);
                        return;
                    }
                    ContainerInfo container = findContainerAt(e.getPoint());
                    if (container != null) {
                        showRenameContainerDialog(container);
                        return;
                    }
                }
                // Single left click on container title bar shows color picker
                else if (e.getClickCount() == 1 && SwingUtilities.isLeftMouseButton(e)) {
                    ContainerInfo container = findContainerAt(e.getPoint());
                    if (container != null && isOnContainerTitleBar(container, e.getPoint())) {
                        showQuickColorPicker(container, e);
                    }
                }
            }
        };

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }
    
    private void handleRightClick(MouseEvent e) {
        Point p = e.getPoint();
        
        // Check for sticky note
        StickyNoteInfo note = findNoteAt(p);
        if (note != null) {
            showNoteContextMenu(note, e);
            return;
        }
        
        // Check for container
        ContainerInfo container = findContainerAt(p);
        if (container != null) {
            showContainerContextMenu(container, e);
            return;
        }
        
        // Show canvas context menu
        showCanvasContextMenu(e);
    }
    
    private void showCanvasContextMenu(MouseEvent e) {
        JPopupMenu menu = new JPopupMenu();
        
        JMenuItem createContainer = new JMenuItem("Create Container Here");
        createContainer.addActionListener(ev -> {
            createContainerAt(e.getPoint());
        });
        menu.add(createContainer);
        
        JMenuItem createNote = new JMenuItem("Add Sticky Note Here");
        createNote.addActionListener(ev -> {
            createStickyNoteAt(e.getPoint());
        });
        menu.add(createNote);
        
        menu.addSeparator();
        
        JMenuItem drawContainer = new JMenuItem("Draw Container...");
        drawContainer.addActionListener(ev -> {
            setDrawingMode(DrawingMode.CONTAINER);
        });
        menu.add(drawContainer);
        
        JMenuItem drawNote = new JMenuItem("Draw Sticky Note...");
        drawNote.addActionListener(ev -> {
            setDrawingMode(DrawingMode.STICKY_NOTE);
        });
        menu.add(drawNote);
        
        menu.show(this, e.getX(), e.getY());
    }
    
    private void showContainerContextMenu(ContainerInfo container, MouseEvent e) {
        JPopupMenu menu = new JPopupMenu();
        
        JMenuItem rename = new JMenuItem("Rename Container...");
        rename.addActionListener(ev -> showRenameContainerDialog(container));
        menu.add(rename);
        
        JMenuItem changeColor = new JMenuItem("Change Color...");
        changeColor.addActionListener(ev -> showColorChooser(container));
        menu.add(changeColor);
        
        menu.addSeparator();
        
        JMenuItem moveToFront = new JMenuItem("Bring to Front");
        moveToFront.addActionListener(ev -> {
            if (metadata != null) {
                metadata.getContainers().remove(container);
                metadata.getContainers().add(container);
                notifyMetadataChanged();
                repaint();
            }
        });
        menu.add(moveToFront);
        
        JMenuItem moveToBack = new JMenuItem("Send to Back");
        moveToBack.addActionListener(ev -> {
            if (metadata != null) {
                metadata.getContainers().remove(container);
                metadata.getContainers().add(0, container);
                notifyMetadataChanged();
                repaint();
            }
        });
        menu.add(moveToBack);
        
        menu.addSeparator();
        
        JMenuItem delete = new JMenuItem("Delete Container");
        delete.addActionListener(ev -> {
            if (metadata != null) {
                metadata.getContainers().remove(container);
                selectedContainer = null;
                notifyMetadataChanged();
                repaint();
            }
        });
        menu.add(delete);
        
        menu.show(this, e.getX(), e.getY());
    }
    
    private void showNoteContextMenu(StickyNoteInfo note, MouseEvent e) {
        JPopupMenu menu = new JPopupMenu();
        
        JMenuItem edit = new JMenuItem("Edit Note...");
        edit.addActionListener(ev -> showEditNoteDialog(note));
        menu.add(edit);
        
        JMenuItem changeColor = new JMenuItem("Change Color...");
        changeColor.addActionListener(ev -> showNoteColorChooser(note));
        menu.add(changeColor);
        
        menu.addSeparator();
        
        JMenuItem moveToFront = new JMenuItem("Bring to Front");
        moveToFront.addActionListener(ev -> {
            if (metadata != null) {
                metadata.getNotes().remove(note);
                metadata.getNotes().add(note);
                notifyMetadataChanged();
                repaint();
            }
        });
        menu.add(moveToFront);
        
        menu.addSeparator();
        
        JMenuItem delete = new JMenuItem("Delete Note");
        delete.addActionListener(ev -> {
            if (metadata != null) {
                metadata.getNotes().remove(note);
                selectedNote = null;
                notifyMetadataChanged();
                repaint();
            }
        });
        menu.add(delete);
        
        menu.show(this, e.getX(), e.getY());
    }
    
    private void showQuickColorPicker(ContainerInfo container, MouseEvent e) {
        if (project == null) return;
        QuickColorPickerPopup popup = new QuickColorPickerPopup(project, container, color -> {
            container.setColor(color);
            notifyMetadataChanged();
            repaint();
        });
        popup.show(this, e.getX(), e.getY());
    }
    
    private void showRenameContainerDialog(ContainerInfo container) {
        if (project == null) return;
        RenameContainerDialog dialog = new RenameContainerDialog(project, container.getTitle());
        if (dialog.showAndGet()) {
            container.setTitle(dialog.getNewName());
            notifyMetadataChanged();
            repaint();
        }
    }
    
    private void showColorChooser(ContainerInfo container) {
        if (project == null) return;
        Color current = container.getAwtColor();
        Color newColor = ColorChooserService.getInstance().showDialog(
            null, project, "Choose Container Color",
            current != null ? current : new Color(0x45B7D1),
            true, null, true
        );
        if (newColor != null) {
            container.setColor(newColor);
            notifyMetadataChanged();
            repaint();
        }
    }
    
    private void showEditNoteDialog(StickyNoteInfo note) {
        // Simple edit dialog for note text
        String newText = javax.swing.JOptionPane.showInputDialog(
            this, "Edit sticky note text:", note.getText()
        );
        if (newText != null) {
            note.setText(newText);
            notifyMetadataChanged();
            repaint();
        }
    }
    
    private void showNoteColorChooser(StickyNoteInfo note) {
        if (project == null) return;
        Color current = note.getAwtColor();
        Color newColor = ColorChooserService.getInstance().showDialog(
            null, project, "Choose Note Color",
            current != null ? current : new Color(0xFFEB3B),
            true, null, true
        );
        if (newColor != null) {
            note.setColor(newColor);
            notifyMetadataChanged();
            repaint();
        }
    }
    
    private boolean isOnContainerTitleBar(ContainerInfo container, Point p) {
        Rectangle bounds = container.getBoundsAsRectangle();
        Rectangle titleBar = new Rectangle(bounds.x, bounds.y, bounds.width, 20);
        return titleBar.contains(p);
    }
    
    private void createContainerAt(Point p) {
        if (metadata == null) return;
        ContainerInfo container = new ContainerInfo(
            "Container " + (metadata.getContainers().size() + 1),
            new int[]{p.x - 75, p.y - 50, 150, 100}
        );
        metadata.getContainers().add(container);
        notifyMetadataChanged();
        repaint();
    }
    
    private void createStickyNoteAt(Point p) {
        if (metadata == null) return;
        StickyNoteInfo note = new StickyNoteInfo(
            new int[]{p.x - 60, p.y - 40},
            new int[]{120, 80},
            "New note"
        );
        metadata.getNotes().add(note);
        notifyMetadataChanged();
        repaint();
    }

    private void handleMousePressed(MouseEvent e) {
        Point p = e.getPoint();
        dragStart = p;
        
        // Handle drawing mode
        if (drawingMode != DrawingMode.NONE) {
            drawingStart = p;
            drawingPreview = new Rectangle(p.x, p.y, 0, 0);
            return;
        }

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
        // Complete drawing mode
        if (drawingMode != DrawingMode.NONE && drawingStart != null && drawingPreview != null) {
            completeDrawing();
        }
        
        // Save metadata after drag/resize operations
        if ((isDragging || isResizing) && metadata != null) {
            notifyMetadataChanged();
        }
        
        isDragging = false;
        isResizing = false;
        resizeHandle = ResizeHandle.NONE;
        dragStart = null;
        drawingStart = null;
        drawingPreview = null;
    }
    
    private void completeDrawing() {
        if (drawingPreview == null || drawingPreview.width < 20 || drawingPreview.height < 20) {
            // Too small, cancel
            setDrawingMode(DrawingMode.NONE);
            return;
        }
        
        if (metadata == null) return;
        
        if (drawingMode == DrawingMode.CONTAINER) {
            ContainerInfo container = new ContainerInfo(
                "Container " + (metadata.getContainers().size() + 1),
                new int[]{drawingPreview.x, drawingPreview.y, drawingPreview.width, drawingPreview.height}
            );
            metadata.getContainers().add(container);
        } else if (drawingMode == DrawingMode.STICKY_NOTE) {
            StickyNoteInfo note = new StickyNoteInfo(
                new int[]{drawingPreview.x, drawingPreview.y},
                new int[]{drawingPreview.width, drawingPreview.height},
                "New note"
            );
            metadata.getNotes().add(note);
        }
        
        notifyMetadataChanged();
        setDrawingMode(DrawingMode.NONE);
        repaint();
    }

    private void handleMouseDragged(MouseEvent e) {
        if (dragStart == null) return;

        Point p = e.getPoint();
        
        // Handle drawing mode
        if (drawingMode != DrawingMode.NONE && drawingStart != null) {
            int x = Math.min(drawingStart.x, p.x);
            int y = Math.min(drawingStart.y, p.y);
            int w = Math.abs(p.x - drawingStart.x);
            int h = Math.abs(p.y - drawingStart.y);
            drawingPreview = new Rectangle(x, y, w, h);
            repaint();
            return;
        }
        
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
        // Don't change cursor in drawing mode
        if (drawingMode != DrawingMode.NONE) {
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            return;
        }
        
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
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Debug border (only in debug mode)
        if (DEBUG_MODE) {
            g2d.setColor(new Color(0, 150, 255, 50));
            g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    1, new float[]{5, 5}, 0));
            g2d.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        }

        if (metadata == null) {
            // No metadata yet - just be invisible
            g2d.dispose();
            return;
        }

        // Check if we have anything to draw
        boolean hasElements = !metadata.getContainers().isEmpty() || !metadata.getNotes().isEmpty();
        
        if (!hasElements && DEBUG_MODE) {
            // Show ready message only in debug mode
            g2d.setColor(new Color(100, 100, 100, 100));
            g2d.setFont(g2d.getFont().deriveFont(11f));
            g2d.drawString("Colorful Diagrams Ready - Add sticky notes or containers", 10, 30);
        }

        // Draw containers first (they're behind everything)
        for (ContainerInfo container : metadata.getContainers()) {
            drawContainer(g2d, container);
        }

        // Draw sticky notes on top
        for (StickyNoteInfo note : metadata.getNotes()) {
            drawStickyNote(g2d, note);
        }
        
        // Draw drawing preview
        if (drawingPreview != null && drawingMode != DrawingMode.NONE) {
            drawDrawingPreview(g2d);
        }

        g2d.dispose();
    }
    
    private void drawDrawingPreview(Graphics2D g2d) {
        Color previewColor = drawingMode == DrawingMode.CONTAINER 
            ? new Color(0x45B7D1)
            : new Color(0xFFEB3B);
        
        // Draw semi-transparent fill
        g2d.setColor(new Color(
            previewColor.getRed(), previewColor.getGreen(), previewColor.getBlue(), 50
        ));
        g2d.fillRect(drawingPreview.x, drawingPreview.y, 
                     drawingPreview.width, drawingPreview.height);
        
        // Draw dashed border
        g2d.setColor(previewColor);
        g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                1, new float[]{8, 4}, 0));
        g2d.drawRect(drawingPreview.x, drawingPreview.y, 
                     drawingPreview.width, drawingPreview.height);
        
        // Draw size indicator
        g2d.setFont(g2d.getFont().deriveFont(10f));
        String sizeText = drawingPreview.width + " x " + drawingPreview.height;
        g2d.drawString(sizeText, drawingPreview.x + 5, drawingPreview.y + drawingPreview.height + 15);
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
        String title = container.getTitle() != null ? container.getTitle() : "Container";
        g2d.setColor(color);
        int titleWidth = Math.min(bounds.width, g2d.getFontMetrics().stringWidth(title) + 20);
        g2d.fillRoundRect(bounds.x, bounds.y, titleWidth, 20, 5, 5);

        // Draw title text
        g2d.setColor(DiagramColorApplicator.getContrastingTextColor(color));
        g2d.setFont(g2d.getFont().deriveFont(Font.BOLD, 11f));
        g2d.drawString(title, bounds.x + 5, bounds.y + 14);
    }

    private void drawStickyNote(Graphics2D g2d, StickyNoteInfo note) {
        Point pos = note.getPositionAsPoint();
        Dimension size = note.getSizeAsDimension();
        Color color = note.getAwtColor();
        
        if (color == null) {
            color = new Color(0xFFEB3B); // Default yellow
        }

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

    /**
     * Checks if overlay contains any elements worth rendering.
     */
    @Override
    public boolean contains(int x, int y) {
        // Only intercept mouse events when over an element
        if (metadata == null) {
            return false;
        }
        
        Point p = new Point(x, y);
        
        // Check notes first (on top)
        for (StickyNoteInfo note : metadata.getNotes()) {
            Rectangle bounds = new Rectangle(
                    note.getPositionAsPoint(),
                    note.getSizeAsDimension()
            );
            // Expand bounds slightly for easier interaction
            bounds.grow(HANDLE_SIZE, HANDLE_SIZE);
            if (bounds.contains(p)) {
                return true;
            }
        }
        
        // Check containers
        for (ContainerInfo container : metadata.getContainers()) {
            Rectangle bounds = container.getBoundsAsRectangle();
            bounds.grow(HANDLE_SIZE, HANDLE_SIZE);
            if (bounds.contains(p)) {
                return true;
            }
        }
        
        return false;
    }
}
