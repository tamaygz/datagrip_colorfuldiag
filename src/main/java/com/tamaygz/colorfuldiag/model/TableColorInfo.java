package com.tamaygz.colorfuldiag.model;

import java.awt.Color;
import java.util.Objects;

/**
 * Represents color information for a table in the diagram.
 */
public class TableColorInfo {
    private String tableId;
    private String color; // Hex color string like "#FF0000"

    public TableColorInfo() {
    }

    public TableColorInfo(String tableId, String color) {
        this.tableId = tableId;
        this.color = color;
    }

    public String getTableId() {
        return tableId;
    }

    public void setTableId(String tableId) {
        this.tableId = tableId;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
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

    public static String colorToHex(Color color) {
        if (color == null) {
            return null;
        }
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TableColorInfo that = (TableColorInfo) o;
        return Objects.equals(tableId, that.tableId) && Objects.equals(color, that.color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tableId, color);
    }
}
