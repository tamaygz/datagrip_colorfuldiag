package com.tamaygz.colorfuldiag.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Root model class for all diagram metadata.
 * This is the structure that gets serialized to JSON.
 */
public class DiagramMetadata {
    private Map<String, TableColorInfo> tables;
    private List<ContainerInfo> containers;
    private List<StickyNoteInfo> notes;

    public DiagramMetadata() {
        this.tables = new HashMap<>();
        this.containers = new ArrayList<>();
        this.notes = new ArrayList<>();
    }

    public Map<String, TableColorInfo> getTables() {
        return tables;
    }

    public void setTables(Map<String, TableColorInfo> tables) {
        this.tables = tables != null ? tables : new HashMap<>();
    }

    public List<ContainerInfo> getContainers() {
        return containers;
    }

    public void setContainers(List<ContainerInfo> containers) {
        this.containers = containers != null ? containers : new ArrayList<>();
    }

    public List<StickyNoteInfo> getNotes() {
        return notes;
    }

    public void setNotes(List<StickyNoteInfo> notes) {
        this.notes = notes != null ? notes : new ArrayList<>();
    }

    // Table operations
    public void setTableColor(String tableId, String color) {
        if (tables == null) {
            tables = new HashMap<>();
        }
        TableColorInfo info = new TableColorInfo(tableId, color);
        tables.put(tableId, info);
    }

    public TableColorInfo getTableColor(String tableId) {
        return tables != null ? tables.get(tableId) : null;
    }

    public void removeTableColor(String tableId) {
        if (tables != null) {
            tables.remove(tableId);
        }
    }

    // Container operations
    public void addContainer(ContainerInfo container) {
        if (containers == null) {
            containers = new ArrayList<>();
        }
        containers.add(container);
    }

    public ContainerInfo getContainer(String containerId) {
        if (containers == null) return null;
        return containers.stream()
                .filter(c -> c.getId().equals(containerId))
                .findFirst()
                .orElse(null);
    }

    public void removeContainer(String containerId) {
        if (containers != null) {
            containers.removeIf(c -> c.getId().equals(containerId));
        }
    }

    public ContainerInfo findContainerForTable(String tableId) {
        if (containers == null) return null;
        return containers.stream()
                .filter(c -> c.containsTable(tableId))
                .findFirst()
                .orElse(null);
    }

    // Sticky note operations
    public void addNote(StickyNoteInfo note) {
        if (notes == null) {
            notes = new ArrayList<>();
        }
        notes.add(note);
    }

    public StickyNoteInfo getNote(String noteId) {
        if (notes == null) return null;
        return notes.stream()
                .filter(n -> n.getId().equals(noteId))
                .findFirst()
                .orElse(null);
    }

    public void removeNote(String noteId) {
        if (notes != null) {
            notes.removeIf(n -> n.getId().equals(noteId));
        }
    }

    public boolean isEmpty() {
        return (tables == null || tables.isEmpty())
                && (containers == null || containers.isEmpty())
                && (notes == null || notes.isEmpty());
    }
}
