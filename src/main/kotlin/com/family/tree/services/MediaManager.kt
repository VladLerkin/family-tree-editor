package com.family.tree.services

import com.family.tree.model.MediaAttachment
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

object MediaManager {
    /**
     * Returns the media root directory for the given project file.
     * For a project "MyProject.ped", sidecar directory is "MyProject.ped_media".
     * For unsaved projects (null path), falls back to "./media".
     */
    @JvmStatic
    @Throws(IOException::class)
    fun getMediaRoot(projectFile: Path?): Path {
        if (projectFile == null) {
            val fallback = Path.of("media")
            if (!Files.exists(fallback)) Files.createDirectories(fallback)
            return fallback
        }
        val dir = projectFile.toAbsolutePath().parent
        val base = projectFile.fileName.toString() + "_media"
        val root = dir.resolve(base).resolve("media")
        if (!Files.exists(root)) {
            Files.createDirectories(root)
        }
        return root
    }

    /**
     * Copies the given source file into the project's media container and returns a MediaAttachment.
     * The copied file name is prefixed with a UUID to avoid collisions.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun copyIntoProject(projectFile: Path?, sourceFile: Path): MediaAttachment {
        Objects.requireNonNull(sourceFile, "sourceFile")
        val mediaRoot = getMediaRoot(projectFile)
        val original = sourceFile.fileName.toString()
        val targetName = UUID.randomUUID().toString() + "_" + original
        val target = mediaRoot.resolve(targetName)
        Files.copy(sourceFile, target)
        val attachment = MediaAttachment()
        attachment.fileName = original
        attachment.relativePath = targetName
        return attachment
    }

    /**
     * Determines if the provided path string looks like an external URL.
     */
    @JvmStatic
    fun isExternalLink(path: String?): Boolean {
        if (path == null) return false
        val p = path.trim().lowercase(Locale.getDefault())
        return p.startsWith("http://") || p.startsWith("https://") || p.startsWith("www.")
    }

    /**
     * Returns a normalized URI for external links (adds https:// to bare www.).
     * Returns null if the path is not an external link.
     */
    @JvmStatic
    fun toExternalUri(path: String?): URI? {
        if (!isExternalLink(path)) return null
        var p = path!!.trim()
        if (p.lowercase(Locale.getDefault()).startsWith("www.")) {
            p = "https://$p"
        }
        return try {
            URI.create(p)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    /**
     * Resolves the absolute path of a media attachment within the project container.
     * For external links, returns null.
     */
    @JvmStatic
    @Throws(IOException::class)
    fun resolveAttachmentPath(projectFile: Path?, attachment: MediaAttachment): Path? {
        var rel = attachment.relativePath
        if (isExternalLink(rel)) return null
        val mediaRoot = getMediaRoot(projectFile)
        if (rel == null || rel.isBlank()) {
            rel = attachment.fileName
        }
        return mediaRoot.resolve(rel)
    }
}
