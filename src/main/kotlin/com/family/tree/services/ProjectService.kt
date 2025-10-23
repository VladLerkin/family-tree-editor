package com.family.tree.services

import com.family.tree.model.ProjectLayout
import com.family.tree.model.ProjectMetadata
import com.family.tree.storage.ProjectRepository
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.LinkedList

class ProjectService {
    private val repository: ProjectRepository = ProjectRepository()
    private var currentProjectPath: Path? = null
    private var currentData: ProjectRepository.ProjectData? = null
    private var currentLayout: ProjectLayout? = null
    private var currentMeta: ProjectMetadata? = null
    private val recentProjects: LinkedList<Path> = LinkedList()
    private val maxRecent: Int = 10

    init {
        loadRecentProjects()
    }

    fun createNewProject() {
        currentProjectPath = null
        currentData = ProjectRepository.ProjectData()
        currentLayout = ProjectLayout()
        currentMeta = ProjectMetadata()
    }

    @Throws(IOException::class)
    fun openProject(path: Path) {
        ensureReadable(path)
        val loaded: ProjectRepository.LoadedProject = repository.read(path)
        currentProjectPath = path
        currentData = loaded.data
        currentLayout = loaded.layout
        currentMeta = loaded.meta
        addToRecent(path)
    }

    @Throws(IOException::class)
    fun saveProject() {
        val path = currentProjectPath
            ?: throw IllegalStateException("No project path set. Use saveProjectAs(path) first.")
        repository.write(path, currentData, currentLayout, currentMeta)
        addToRecent(path)
    }

    @Throws(IOException::class)
    fun saveProjectAs(path: Path) {
        ensureParentExists(path)
        currentProjectPath = path
        repository.write(path, currentData, currentLayout, currentMeta)
        addToRecent(path)
    }

    fun closeProject() {
        currentProjectPath = null
        currentData = null
        currentLayout = null
        currentMeta = null
    }

    fun getCurrentData(): ProjectRepository.ProjectData? = currentData
    fun getCurrentLayout(): ProjectLayout? = currentLayout
    fun getCurrentMeta(): ProjectMetadata? = currentMeta
    fun getCurrentProjectPath(): Path? = currentProjectPath

    fun getRecentProjects(): List<Path> = java.util.Collections.unmodifiableList(java.util.ArrayList(recentProjects))

    private fun addToRecent(path: Path?) {
        if (path == null) return
        val abs = path.toAbsolutePath().normalize()
        recentProjects.removeIf { p -> p == path || p.toAbsolutePath().normalize() == abs }
        recentProjects.addFirst(abs)
        while (recentProjects.size > maxRecent) {
            recentProjects.removeLast()
        }
        saveRecentProjects()
    }

    @Throws(IOException::class)
    private fun ensureReadable(path: Path) {
        if (!Files.exists(path)) {
            throw IOException("Project file does not exist: $path")
        }
        if (!Files.isReadable(path)) {
            throw IOException("Project file is not readable: $path")
        }
    }

    @Throws(IOException::class)
    private fun ensureParentExists(path: Path) {
        val parent = path.toAbsolutePath().parent
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent)
        }
    }

    private fun recentStorePath(): Path {
        val home = System.getProperty("user.home")
        val dir = Paths.get(home, ".family-tree-editor")
        return dir.resolve("recent-projects.txt")
    }

    private fun loadRecentProjects() {
        try {
            val file = recentStorePath()
            if (Files.exists(file)) {
                val lines = Files.readAllLines(file, StandardCharsets.UTF_8)
                val paths = lines.asSequence()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .map { Paths.get(it) }
                    .filter { Files.exists(it) }
                    .take(maxRecent)
                    .toList()
                recentProjects.clear()
                for (i in paths.size - 1 downTo 0) {
                    recentProjects.addFirst(paths[i])
                }
            }
        } catch (_: Exception) {
            // best-effort: ignore errors reading recent list
        }
    }

    private fun saveRecentProjects() {
        try {
            val file = recentStorePath()
            val dir = file.parent
            if (dir != null && !Files.exists(dir)) {
                Files.createDirectories(dir)
            }
            val lines = recentProjects.asSequence()
                .map { it.toAbsolutePath().toString() }
                .toList()
            Files.write(
                file,
                lines,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            )
        } catch (_: Exception) {
            // best-effort: ignore errors writing recent list
        }
    }
}