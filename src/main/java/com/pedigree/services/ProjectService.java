package com.pedigree.services;

import com.pedigree.model.ProjectLayout;
import com.pedigree.model.ProjectMetadata;
import com.pedigree.storage.ProjectRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

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
        loadRecentProjects();
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
        // Ensure current project is present and moved to top of recent list after save
        addToRecent(currentProjectPath);
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
        if (path == null) return;
        Path abs = path.toAbsolutePath().normalize();
        // Remove any existing entries matching either raw or normalized absolute
        recentProjects.removeIf(p -> p.equals(path) || p.toAbsolutePath().normalize().equals(abs));
        recentProjects.addFirst(abs);
        while (recentProjects.size() > maxRecent) {
            recentProjects.removeLast();
        }
        saveRecentProjects();
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

    private static Path recentStorePath() {
        String home = System.getProperty("user.home");
        Path dir = Paths.get(home, ".pedigree-editor");
        return dir.resolve("recent-projects.txt");
    }

    private void loadRecentProjects() {
        try {
            Path file = recentStorePath();
            if (Files.exists(file)) {
                List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                List<Path> paths = lines.stream()
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Paths::get)
                        .filter(Files::exists)
                        .limit(maxRecent)
                        .collect(Collectors.toList());
                recentProjects.clear();
                for (int i = paths.size() - 1; i >= 0; i--) {
                    // maintain order: first line is most recent
                    // we'll addFirst in reverse to preserve order
                    recentProjects.addFirst(paths.get(i));
                }
            }
        } catch (Exception ignored) {
            // best-effort: ignore errors reading recent list
        }
    }

    private void saveRecentProjects() {
        try {
            Path file = recentStorePath();
            Path dir = file.getParent();
            if (dir != null && !Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            List<String> lines = recentProjects.stream()
                    .map(p -> p.toAbsolutePath().toString())
                    .collect(Collectors.toList());
            Files.write(file, lines, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (Exception ignored) {
            // best-effort: ignore errors writing recent list
        }
    }
}



