package com.tamaygz.colorfuldiag.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.tamaygz.colorfuldiag.model.DiagramMetadata;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Project-level service for managing diagram metadata.
 * Handles loading, saving, and caching of metadata files.
 */
@Service(Service.Level.PROJECT)
public final class DiagramMetadataService {
    private static final Logger LOG = Logger.getInstance(DiagramMetadataService.class);
    private static final String METADATA_SUFFIX = "_colorfuldiag.json";
    private static final String NOTIFICATION_GROUP = "ColorfulDiagrams";

    private final Project project;
    private final Gson gson;
    private final ConcurrentHashMap<String, DiagramMetadata> metadataCache;

    public DiagramMetadataService(Project project) {
        this.project = project;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        this.metadataCache = new ConcurrentHashMap<>();
    }

    public static DiagramMetadataService getInstance(Project project) {
        return project.getService(DiagramMetadataService.class);
    }

    /**
     * Gets the metadata file path for a diagram file.
     */
    public static String getMetadataFilePath(VirtualFile diagramFile) {
        if (diagramFile == null) {
            return null;
        }
        String path = diagramFile.getPath();
        String nameWithoutExtension = diagramFile.getNameWithoutExtension();
        String parentPath = diagramFile.getParent() != null ? diagramFile.getParent().getPath() : "";
        return parentPath + "/" + nameWithoutExtension + METADATA_SUFFIX;
    }

    /**
     * Gets the metadata file path for a diagram path string.
     */
    public static String getMetadataFilePath(String diagramPath) {
        if (diagramPath == null || diagramPath.isEmpty()) {
            return null;
        }
        int lastDot = diagramPath.lastIndexOf('.');
        String basePath = lastDot > 0 ? diagramPath.substring(0, lastDot) : diagramPath;
        return basePath + METADATA_SUFFIX;
    }

    /**
     * Loads metadata for a diagram file.
     * Returns cached version if available, otherwise loads from disk.
     */
    public DiagramMetadata loadMetadata(VirtualFile diagramFile) {
        if (diagramFile == null) {
            return new DiagramMetadata();
        }

        String metadataPath = getMetadataFilePath(diagramFile);
        if (metadataPath == null) {
            return new DiagramMetadata();
        }

        // Check cache first
        DiagramMetadata cached = metadataCache.get(metadataPath);
        if (cached != null) {
            return cached;
        }

        // Load from disk
        return loadMetadataFromPath(metadataPath);
    }

    /**
     * Loads metadata from a specific file path.
     */
    public DiagramMetadata loadMetadataFromPath(String metadataPath) {
        if (metadataPath == null) {
            return new DiagramMetadata();
        }

        // Check cache
        DiagramMetadata cached = metadataCache.get(metadataPath);
        if (cached != null) {
            return cached;
        }

        Path path = Path.of(metadataPath);
        if (!Files.exists(path)) {
            DiagramMetadata empty = new DiagramMetadata();
            metadataCache.put(metadataPath, empty);
            return empty;
        }

        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            DiagramMetadata metadata = gson.fromJson(json, DiagramMetadata.class);
            if (metadata == null) {
                metadata = new DiagramMetadata();
            }
            metadataCache.put(metadataPath, metadata);
            return metadata;
        } catch (IOException e) {
            LOG.warn("Failed to load metadata from: " + metadataPath, e);
            showNotification("Failed to load diagram metadata", NotificationType.WARNING);
            return new DiagramMetadata();
        }
    }

    /**
     * Saves metadata for a diagram file.
     */
    public void saveMetadata(VirtualFile diagramFile, DiagramMetadata metadata) {
        if (diagramFile == null || metadata == null) {
            return;
        }

        String metadataPath = getMetadataFilePath(diagramFile);
        saveMetadataToPath(metadataPath, metadata);
    }

    /**
     * Saves metadata to a specific file path.
     */
    public void saveMetadataToPath(String metadataPath, DiagramMetadata metadata) {
        if (metadataPath == null || metadata == null) {
            return;
        }

        try {
            Path path = Path.of(metadataPath);

            // Don't create empty files
            if (metadata.isEmpty()) {
                if (Files.exists(path)) {
                    Files.delete(path);
                    metadataCache.remove(metadataPath);
                }
                return;
            }

            String json = gson.toJson(metadata);
            Files.writeString(path, json, StandardCharsets.UTF_8);
            metadataCache.put(metadataPath, metadata);
            LOG.info("Saved metadata to: " + metadataPath);
        } catch (IOException e) {
            LOG.error("Failed to save metadata to: " + metadataPath, e);
            showNotification("Failed to save diagram metadata", NotificationType.ERROR);
        }
    }

    /**
     * Gets or creates metadata for a diagram, caching it for future use.
     */
    public DiagramMetadata getOrCreateMetadata(String diagramPath) {
        String metadataPath = getMetadataFilePath(diagramPath);
        if (metadataPath == null) {
            return new DiagramMetadata();
        }

        DiagramMetadata cached = metadataCache.get(metadataPath);
        if (cached != null) {
            return cached;
        }

        return loadMetadataFromPath(metadataPath);
    }

    /**
     * Invalidates the cache for a specific diagram.
     */
    public void invalidateCache(String diagramPath) {
        String metadataPath = getMetadataFilePath(diagramPath);
        if (metadataPath != null) {
            metadataCache.remove(metadataPath);
        }
    }

    /**
     * Clears all cached metadata.
     */
    public void clearCache() {
        metadataCache.clear();
    }

    /**
     * Exports metadata to a specified file path.
     */
    public void exportMetadata(String diagramPath, String exportPath) {
        DiagramMetadata metadata = getOrCreateMetadata(diagramPath);
        try {
            String json = gson.toJson(metadata);
            Files.writeString(Path.of(exportPath), json, StandardCharsets.UTF_8);
            showNotification("Metadata exported successfully", NotificationType.INFORMATION);
        } catch (IOException e) {
            LOG.error("Failed to export metadata to: " + exportPath, e);
            showNotification("Failed to export metadata", NotificationType.ERROR);
        }
    }

    /**
     * Imports metadata from a specified file path.
     */
    public DiagramMetadata importMetadata(String importPath) {
        try {
            String json = Files.readString(Path.of(importPath), StandardCharsets.UTF_8);
            DiagramMetadata metadata = gson.fromJson(json, DiagramMetadata.class);
            if (metadata != null) {
                showNotification("Metadata imported successfully", NotificationType.INFORMATION);
            }
            return metadata != null ? metadata : new DiagramMetadata();
        } catch (IOException e) {
            LOG.error("Failed to import metadata from: " + importPath, e);
            showNotification("Failed to import metadata", NotificationType.ERROR);
            return new DiagramMetadata();
        }
    }

    private void showNotification(String content, NotificationType type) {
        Notification notification = new Notification(
                NOTIFICATION_GROUP,
                "Colorful Diagrams",
                content,
                type
        );
        Notifications.Bus.notify(notification, project);
    }
}
