# Project Guidelines

This document summarizes how this repository is structured and how to work with it. It covers:
- Coding conventions used in the current codebase
- Code organization and package structure
- Unit and integration testing approaches

Last updated: 2025-08-23 22:37


## 1. Coding conventions used in this codebase

General
- Language level: Java 25 (see pom.xml maven.compiler.release 25). Use modern Java features judiciously where it improves clarity, but prefer simple, readable code over clever constructs.
- Character encoding: UTF‑8.
- Null-safety: Use `Objects.requireNonNull` for required constructor arguments and important setters. Prefer immutable IDs and avoid exposing internal mutable collections directly.
- Collections mutability: For project-domain lists that drive UI state, use `com.pedigree.util.DirtyObservableList` to automatically set the global dirty flag on mutations.
- Dirty state: Mutations to model properties call `com.pedigree.util.DirtyFlag.setModified()` in setters. After successful persistence, `DirtyFlag.clear()` is invoked by storage.
- Logging: There is no logging framework configured at the moment. UI-facing errors are reported via `com.pedigree.ui.Dialogs` and the status bar. If logging is added later, prefer SLF4J API.

Naming and formatting
- Packages: all lower-case (e.g., `com.pedigree.model`).
- Classes and interfaces: PascalCase (e.g., `MainApplication`, `ProjectService`).
- Methods and fields: camelCase (e.g., `getCurrentData`, `undoRedoService`).
- Constants: UPPER_SNAKE_CASE (none broadly used so far).
- Indentation: 4 spaces, no tabs.
- Braces: opening brace on the same line; always use braces for multi-line blocks. Single-line guards are tolerated but keep them consistent and clear.
- Imports: prefer explicit imports; avoid wildcard imports.
- Visibility: keep fields private and expose via getters/setters unless immutability is desired; favor `final` where appropriate.
- Comments: concise; prefer self-explanatory code and meaningful method names.

Error handling
- UI actions catch exceptions and surface messages with `Dialogs.showError(title, message)` rather than throwing to the top.
- Service and storage layers may throw checked `IOException` and validate preconditions using simple helper methods (`ensureReadable`, `ensureParentExists`).

JavaFX/UI specifics
- UI is built programmatically (no FXML). Keep scene-graph creation in UI classes (e.g., `MainWindow`, `ToolbarFactory`).
- Keep rendering/layout logic in dedicated packages (`render`, `layout`).
- Connect UI components to services using dependency injection via constructors where possible.

Serialization
- JSON via Jackson (`jackson-databind`, `jackson-datatype-jsr310`).
- Streams are kept open for ZIP operations by disabling Jackson AUTO_CLOSE features in `ProjectRepository`.


## 2. Code organization and package structure

Top-level: `src/main/java/com/pedigree/...`

Key packages and responsibilities
- `com.pedigree.app`
  - Application entry point and JavaFX bootstrap.
  - `MainApplication` launches the primary stage and wires services into the main window.

- `com.pedigree.ui`
  - JavaFX UI components, dialogs, list views, and window composition.
  - Handles user actions, status updates, and invokes services. Examples: `MainWindow`, `Dialogs`, `IndividualsListView`, `FamiliesListView`, `PropertiesInspector`.

- `com.pedigree.editor`
  - Canvas and interaction logic (selection, zoom/pan, alignment, clipboard placeholders) and command abstractions.
  - `CanvasView`, `CanvasPane`, `SelectionModel`, `AlignAndDistributeController`, plus the base `Command` and `CommandStack`.
  - Subpackage `com.pedigree.editor.commands` contains concrete command implementations (add/move/edit/assign/etc.).

- `com.pedigree.model`
  - Domain model: `Individual`, `Family`, `Relationship`, `Tag`, `Note`, `MediaAttachment`, layout/meta records.
  - Models are simple POJOs; lists are `DirtyObservableList`s to mark project dirty on change.

- `com.pedigree.layout`
  - Automatic layout engine and related structures. `PedigreeLayoutEngine` computes node positions; `ProjectLayout` stores persistent viewport/zoom when applicable.

- `com.pedigree.render`
  - Rendering helpers and metrics (`TreeRenderer`, `NodeMetrics`). Separate from UI assembly to keep drawing logic testable.

- `com.pedigree.services`
  - Application services coordinating domain, persistence, and UI. Examples: `ProjectService` (open/save/close, recent projects, current context), `UndoRedoService`, `TagService`, `NoteService`, `MediaService`.

- `com.pedigree.storage`
  - Persistence and file formats.
  - `ProjectRepository` reads/writes a ZIP-based project: entries `data.json`, `layout.json`, `meta.json` (see `ProjectFormat`). Uses Jackson and rotates backups via `BackupService`.

- `com.pedigree.gedcom`
  - Import/export from/to GEDCOM files (`GedcomImporter`, `GedcomExporter`).

- `com.pedigree.export`
  - Exporters for HTML, SVG, and images (`HtmlExporter`, `SvgExporter`, `ImageExporter`).

- `com.pedigree.print`
  - Printing support (`PrintService`).

- `com.pedigree.search`
  - Quick search dialog helpers and data lookup utilities.

- `com.pedigree.util`
  - Cross-cutting utilities like `DirtyFlag` and `DirtyObservableList`.

Build and distribution
- Maven project. JavaFX dependencies are declared in pom.xml. Profiles `mac` and `windows` package the app using the JDK’s `jpackage` via the exec plugin.
- No explicit module-info; non-modular JavaFX application packaged with `--add-modules` for the required JavaFX modules.


## 3. Unit and integration testing approaches

Current status
- There are no test dependencies or test sources in the repository yet (no JUnit/TestNG in pom.xml, no `src/test/java`). The following is the recommended approach for introducing tests incrementally.

Recommended unit testing setup
- Framework: JUnit 5 (Jupiter). Add dependencies to `pom.xml`:
  - `org.junit.jupiter:junit-jupiter-api` (test scope)
  - `org.junit.jupiter:junit-jupiter-engine` (test scope)
- Maven Surefire Plugin: configure to run JUnit Platform.
- Test source directory: `src/test/java` mirroring the main package structure.

What to unit test first (high ROI)
- `com.pedigree.util`
  - `DirtyObservableList`: verify that mutations set `DirtyFlag` and that no-ops behave as expected.
  - `DirtyFlag`: clear/set behavior and thread-visibility for the `volatile` flag.
- `com.pedigree.storage`
  - `ProjectRepository`: round-trip tests for read/write using a temporary directory and sample `ProjectData` with individuals/families/relationships/tags; ensure Jackson preserves data and that `DirtyFlag.clear()` is called after save/load.
  - `BackupService`: rotation behavior (use temp dir; assert expected number of backups and naming).
- `com.pedigree.model`
  - POJO setters/getters maintain invariants (e.g., non-null fields enforced via constructors; setters trigger `DirtyFlag`).
- `com.pedigree.layout` and `com.pedigree.render`
  - Pure functions and calculations wherever possible; validate deterministic outputs for fixed inputs.

JavaFX/UI testing
- Unit-test non-visual logic extracted from UI classes whenever possible.
- For JavaFX-involved logic (e.g., selection model, alignment/distribution), write tests that do not require a visible stage by using `Platform.startup` or TestFX with headless settings if added later.
- Prefer to keep heavy UI integration tests manual for now: the app starts via `MainApplication` and user flows can be validated manually (open, edit, save, close).

Integration testing
- Storage integration:
  - Scenario: create `ProjectData`, write with `ProjectRepository.write()`, then read with `read()` and compare domain objects.
  - Validate ZIP entry names (`ProjectFormat`) and JSON structure via Jackson.
- Import/export integration:
  - GEDCOM: import from a small fixture file and assert object counts and key fields; export current data and verify GEDCOM text contains expected entries.
  - HTML/SVG/Image exporters: run exporters with a minimal dataset and assert files exist and are non-empty; for SVG/HTML, optionally parse and verify key tags.
- Undo/redo integration:
  - With `UndoRedoService` and `CommandStack`, apply a command (e.g., add individual), undo, redo; assert model state after each step.

Practical tips for tests
- Use `java.nio.file.Files.createTempDirectory` for filesystem tests; ensure cleanup with try-with-resources or delete-on-exit.
- For time-dependent fields (e.g., `ProjectMetadata.modifiedAt`), assert relative differences rather than exact equality.
- Keep tests deterministic and avoid reliance on system-specific paths or locale where possible.

Continuous verification
- Once tests are added, run with `mvn test`.
- For packaging verification, use `mvn -Pmac clean package` on macOS or `mvn -Pwindows clean package` on Windows as described under docs/BUILD_* files.


## 4. Contribution notes (optional but useful)
- Keep responsibilities separated by package. If a class crosses boundaries (e.g., UI directly modifying storage), consider introducing a service.
- Favor small, focused methods and classes. If a UI class grows beyond ~500 lines, consider extracting subcomponents.
- When introducing new model properties, ensure setters tick `DirtyFlag` and lists are `DirtyObservableList`s.
- Prefer constructor injection for services into UI components to ease future testing.
