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
import kotlin.jvm.JvmName

class ProjectService {
    private val repository: ProjectRepository = ProjectRepository()
    private var _currentProjectPath: Path? = null
    private var _currentData: ProjectRepository.ProjectData? = null
    private var _currentLayout: ProjectLayout? = null
    private var _currentMeta: ProjectMetadata? = null
    private val recentProjectsList: LinkedList<Path> = LinkedList()
    private val maxRecent: Int = 10

    var currentProjectPath: Path?
        get() = _currentProjectPath
        private set(value) { _currentProjectPath = value }

    val currentData: ProjectRepository.ProjectData?
        get() = _currentData

    val currentLayout: ProjectLayout?
        get() = _currentLayout

    val currentMeta: ProjectMetadata?
        get() = _currentMeta

    val recentProjects: List<Path>
        get() = java.util.Collections.unmodifiableList(java.util.ArrayList(recentProjectsList))

    init {
        loadRecentProjects()
    }

    fun createNewProject() {
        _currentProjectPath = null
        _currentData = ProjectRepository.ProjectData()
        _currentLayout = ProjectLayout()
        _currentMeta = ProjectMetadata()
    }

    @Throws(IOException::class)
    fun openProject(path: Path) {
        ensureReadable(path)
        val loaded: ProjectRepository.LoadedProject = repository.read(path)
        _currentProjectPath = path
        _currentData = loaded.data
        _currentLayout = loaded.layout
        _currentMeta = loaded.meta
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
        _currentProjectPath = null
        _currentData = null
        _currentLayout = null
        _currentMeta = null
    }


    private fun addToRecent(path: Path?) {
        if (path == null) return
        val abs = path.toAbsolutePath().normalize()
        recentProjectsList.removeIf { p: Path -> p == path || p.toAbsolutePath().normalize() == abs }
        recentProjectsList.addFirst(abs)
        while (recentProjectsList.size > maxRecent) {
            recentProjectsList.removeLast()
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
                recentProjectsList.clear()
                for (i in paths.size - 1 downTo 0) {
                    recentProjectsList.addFirst(paths[i])
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
            val lines = recentProjectsList.asSequence()
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