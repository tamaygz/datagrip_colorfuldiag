package com.tamaygz.colorfuldiag.model;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a visual container that groups tables in a diagram.
 */
public class ContainerInfo {
    private String id;
    private String title;
    private String color; // Hex color string
    private int[] bounds; // [x, y, width, height]
    private List<String> tables; // List of table IDs in this container

    public ContainerInfo() {
        this.id = UUID.randomUUID().toString();
        this.tables = new ArrayList<>();
        this.bounds = new int[]{0, 0, 300, 200};
    }

    public ContainerInfo(String title, String color, int x, int y, int width, int height) {
        this();
        this.title = title;
        this.color = color;
        this.bounds = new int[]{x, y, width, height};
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int[] getBounds() {
        return bounds;
    }

    public void setBounds(int[] bounds) {
        this.bounds = bounds;
    }

    public Rectangle getBoundsAsRectangle() {
        if (bounds == null || bounds.length < 4) {
            return new Rectangle(0, 0, 300, 200);
        }
        return new Rectangle(bounds[0], bounds[1], bounds[2], bounds[3]);
    }

    public void setBoundsFromRectangle(Rectangle rect) {
        this.bounds = new int[]{rect.x, rect.y, rect.width, rect.height};
    }

    public List<String> getTables() {
        return tables;
    }

    public void setTables(List<String> tables) {
        this.tables = tables != null ? tables : new ArrayList<>();
    }

    public void addTable(String tableId) {
        if (tables == null) {
            tables = new ArrayList<>();
        }
        if (!tables.contains(tableId)) {
            tables.add(tableId);
        }
    }

    public void removeTable(String tableId) {
        if (tables != null) {
            tables.remove(tableId);
        }
    }

    public boolean containsTable(String tableId) {
        return tables != null && tables.contains(tableId);
    }

    public Color getAwtColor() {
        if (color == null || color.isEmpty()) {
            return null;
        }
        try {
            return Color.decode(color);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContainerInfo that = (ContainerInfo) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
