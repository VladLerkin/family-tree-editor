package com.pedigree.services;

import com.pedigree.model.MediaAttachment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

public class MediaManager {

    /**
     * Returns the media root directory for the given project file.
     * For a project "MyProject.ped", sidecar directory is "MyProject.ped_media".
     * For unsaved projects (null path), falls back to "./media".
     */
    public static Path getMediaRoot(Path projectFile) throws IOException {
        if (projectFile == null) {
            Path fallback = Path.of("media");
            if (!Files.exists(fallback)) Files.createDirectories(fallback);
            return fallback;
        }
        Path dir = projectFile.toAbsolutePath().getParent();
        String base = projectFile.getFileName().toString() + "_media";
        Path root = dir.resolve(base).resolve("media");
        if (!Files.exists(root)) {
            Files.createDirectories(root);
        }
        return root;
    }

    /**
     * Copies the given source file into the project's media container and returns a MediaAttachment.
     * The copied file name is prefixed with a UUID to avoid collisions.
     */
    public static MediaAttachment copyIntoProject(Path projectFile, Path sourceFile) throws IOException {
        Objects.requireNonNull(sourceFile, "sourceFile");
        Path mediaRoot = getMediaRoot(projectFile);
        String original = sourceFile.getFileName().toString();
        String targetName = UUID.randomUUID() + "_" + original;
        Path target = mediaRoot.resolve(targetName);
        Files.copy(sourceFile, target);
        MediaAttachment attachment = new MediaAttachment();
        attachment.setFileName(original);
        attachment.setRelativePath(targetName);
        return attachment;
    }

    /**
     * Resolves the absolute path of a media attachment within the project container.
     */
    public static Path resolveAttachmentPath(Path projectFile, MediaAttachment attachment) throws IOException {
        Path mediaRoot = getMediaRoot(projectFile);
        String rel = attachment.getRelativePath();
        if (rel == null || rel.isBlank()) {
            rel = attachment.getFileName();
        }
        return mediaRoot.resolve(rel);
    }
}
