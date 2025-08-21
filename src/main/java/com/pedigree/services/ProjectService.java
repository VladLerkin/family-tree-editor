package com.pedigree.services;

import com.pedigree.model.ProjectLayout;
import com.pedigree.model.ProjectMetadata;
import com.pedigree.storage.ProjectRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ProjectService {
    private final ProjectRepository repository;
    private Path currentProjectPath;
    private ProjectRepository.ProjectData currentData;
    private ProjectLayout currentLayout;
    private ProjectMetadata currentMeta;
    private final LinkedList<Path> recentProjects;
    private final int maxRecent = 10;

    public ProjectService() {
        this.repository = new ProjectRepository();
        this.recentProjects = new LinkedList<>();
    }

    public void createNewProject() {
        this.currentProjectPath = null;
        this.currentData = new ProjectRepository.ProjectData();
        this.currentLayout = new ProjectLayout();
        this.currentMeta = new ProjectMetadata();
    }

    public void openProject(Path path) throws IOException {
        ensureReadable(path);
        ProjectRepository.LoadedProject loaded = repository.read(path);
        this.currentProjectPath = path;
        this.currentData = loaded.data();
        this.currentLayout = loaded.layout();
        this.currentMeta = loaded.meta();
        addToRecent(path);
    }

    public void saveProject() throws IOException {
        if (currentProjectPath == null) {
            throw new IllegalStateException("No project path set. Use saveProjectAs(path) first.");
        }
        repository.write(currentProjectPath, currentData, currentLayout, currentMeta);
    }

    public void saveProjectAs(Path path) throws IOException {
        ensureParentExists(path);
        this.currentProjectPath = path;
        repository.write(currentProjectPath, currentData, currentLayout, currentMeta);
        addToRecent(path);
    }

    public void closeProject() {
        this.currentProjectPath = null;
        this.currentData = null;
        this.currentLayout = null;
        this.currentMeta = null;
    }

    public ProjectRepository.ProjectData getCurrentData() { return currentData; }
    public ProjectLayout getCurrentLayout() { return currentLayout; }
    public ProjectMetadata getCurrentMeta() { return currentMeta; }
    public Path getCurrentProjectPath() { return currentProjectPath; }

    public List<Path> getRecentProjects() { return Collections.unmodifiableList(new ArrayList<>(recentProjects)); }

    private void addToRecent(Path path) {
        recentProjects.remove(path);
        recentProjects.addFirst(path);
        while (recentProjects.size() > maxRecent) {
            recentProjects.removeLast();
        }
    }

    private static void ensureReadable(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new IOException("Project file does not exist: " + path);
        }
        if (!Files.isReadable(path)) {
            throw new IOException("Project file is not readable: " + path);
        }
    }

    private static void ensureParentExists(Path path) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }
}



