# Family Tree Editor — How to Run the Project

Below is the shortest path to run the app locally and (optionally) build an installer.

Requirements
- JDK 25 (mandatory). Check with `java -version` and ensure the `JAVA_HOME` environment variable points to it.
- Internet access so Maven can download dependencies.
- Nothing extra to install: the repository includes the Maven Wrapper (`mvnw`, `mvnw.cmd`).

Quick Start (Windows)
1) Open a terminal in the project root.
2) Run:
   PowerShell:
   .\mvnw.cmd "-Dprism.order=sw" javafx:run
   
   Alternatives:
   - PowerShell (stop-parsing):
     .\mvnw.cmd --% -Dprism.order=sw javafx:run
   - CMD.exe:
     .\mvnw.cmd -Dprism.order=sw javafx:run
   
   Notes:
   - In PowerShell, always wrap -D system properties in quotes ("-Dname=value") or use `--%`. Otherwise, PowerShell may split the argument and Maven will fail with an error like: Unknown lifecycle phase '.order=sw'.
   - The JavaFX Maven plugin will launch the application (MainApplication).
   - The `-Dprism.order=sw` flag forces software rendering. You can remove it to use hardware acceleration.

Quick Start (macOS/Linux)
1) Open a terminal in the project root.
2) Run:
   ./mvnw -Dprism.order=sw javafx:run

Run from IntelliJ IDEA
- Open the project (pom.xml) as a Maven project.
- Create an "Application" run configuration:
  - Main class: `com.pedigree.app.MainApplication`
  - Use classpath of module: `family-tree-editor`
- Run the configuration. If you have multiple JDKs installed, make sure JDK 25 is selected (Project SDK and Run configuration JRE).

Build an Installer
- Windows (MSI):
  .\mvnw.cmd -Pwindows clean package
  The MSI will be created at `target/dist/family-tree-editor-${project.version}-windows.msi`.

- macOS (DMG):
  mvn -Pmac clean package
  The DMG will be created at `target/dist/family-tree-editor-${project.version}-macos.dmg`.

- Linux (DEB, with a menu shortcut):
  mvn -Plinux clean package
  The package will be created at `target/dist/family-tree-editor-${project.version}-linux-amd64.deb`.
  The `fakeroot` package is required (used by jpackage). Install it via your package manager: apt/dnf/pacman. If `fakeroot` is missing, the build will not fail, but the DEB packaging step will be skipped.
  It is recommended to install `binutils` (provides `objcopy`) for a more compact runtime. If `binutils` is absent, the build will still succeed: we disable the corresponding jlink plugin by default.

Detailed instructions
- Windows: `docs/BUILD_WIN.md`
- macOS: `docs/BUILD_MAC.md`
- Linux: `docs/BUILD_LINUX.md`

Common issues
- "Cannot find wrapperUrl in .mvn\\wrapper\\maven-wrapper.properties":
  Update your repository (git pull). The file should already contain both `distributionUrl` and `wrapperUrl`. Manually, you can set:
  distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.8/apache-maven-3.9.8-bin.zip
  wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar

- JavaFX/rendering does not start:
  Try adding `-Dprism.order=sw` to the run command.

Versions and environment
- The project builds with Maven, Java 25, JavaFX 24.0.1.
- Entry point: `com.pedigree.app.MainApplication`.
