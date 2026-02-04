package com.tamaygz.colorfuldiag.model;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a sticky note in the diagram.
 */
public class StickyNoteInfo {
    private String id;
    private String text;
    private String color; // Hex color string
    private int[] position; // [x, y]
    private int[] size; // [width, height]

    public StickyNoteInfo() {
        this.id = UUID.randomUUID().toString();
        this.position = new int[]{0, 0};
        this.size = new int[]{150, 100};
        this.color = "#FFEAA7"; // Default yellow color
    }

    public StickyNoteInfo(String text, String color, int x, int y) {
        this();
        this.text = text;
        this.color = color;
        this.position = new int[]{x, y};
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int[] getPosition() {
        return position;
    }

    public void setPosition(int[] position) {
        this.position = position;
    }

    public Point getPositionAsPoint() {
        if (position == null || position.length < 2) {
            return new Point(0, 0);
        }
        return new Point(position[0], position[1]);
    }

    public void setPositionFromPoint(Point point) {
        this.position = new int[]{point.x, point.y};
    }

    public int[] getSize() {
        return size;
    }

    public void setSize(int[] size) {
        this.size = size;
    }

    public Dimension getSizeAsDimension() {
        if (size == null || size.length < 2) {
            return new Dimension(150, 100);
        }
        return new Dimension(size[0], size[1]);
    }

    public void setSizeFromDimension(Dimension dim) {
        this.size = new int[]{dim.width, dim.height};
    }

    public Color getAwtColor() {
        if (color == null || color.isEmpty()) {
            return Color.decode("#FFEAA7"); // Default yellow
        }
        try {
            return Color.decode(color);
        } catch (NumberFormatException e) {
            return Color.decode("#FFEAA7");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StickyNoteInfo that = (StickyNoteInfo) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
