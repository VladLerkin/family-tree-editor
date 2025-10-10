# Linux: Run and Package (Portable archives)

Prerequisites:
- Linux distribution with desktop environment (GNOME/KDE/etc.).
- JDK 25 installed and `JAVA_HOME` pointing to it (jpackage is included with the JDK).
- Maven 3.8+ (or use the Maven Wrapper in this repo: `./mvnw`).

Run during development:
1. From the project root, run (JavaFX plugin will launch the app):
   ./mvnw -Dprism.order=sw javafx:run

Tips:
- If you have multiple JDKs, ensure the one on PATH/JAVA_HOME is JDK 25.
- The `-Dprism.order=sw` flag forces software rendering; remove it for hardware acceleration.

Build portable distribution archives:
1. From the project root, run:
   mvn -Plinux clean package
2. Portable archives will be created under `target/dist/`:
   - family-tree-editor-${project.version}-linux-portable.tar.gz
   - family-tree-editor-${project.version}-linux-portable.zip

How to run the portable build:
1. Extract the archive (tar.gz or zip) to any directory.
2. Inside the extracted `family-tree-editor` folder, run:
   ./run.sh
   (Ensure `run.sh` is executable: `chmod +x run.sh` if needed.)

Notes:
- We stopped producing a DEB package by default to align with common GitHub release distribution for Linux (portable archives).
- If you need a native installer (DEB/RPM/AppImage), it can be added later; for now the portable archives are the recommended cross-distro option.
- You can pass an app icon to future installers via `--icon path/to/icon.png` in jpackage arguments when installer packaging is introduced.
