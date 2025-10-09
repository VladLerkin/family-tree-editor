# Linux: Run and Package (DEB)

Prerequisites:
- Linux distribution with desktop environment (GNOME/KDE/etc.).
- JDK 25 installed and `JAVA_HOME` pointing to it (jpackage is included with the JDK).
- Maven 3.8+ (or use the Maven Wrapper in this repo: `./mvnw`).
- For DEB packaging: Debian/Ubuntu or a derivative with `dpkg-deb` available. On RPM-based distros, you can adapt to `--type rpm` (not configured by default here).

Run during development:
1. From the project root, run (JavaFX plugin will launch the app):
   ./mvnw -Dprism.order=sw javafx:run

Tips:
- If you have multiple JDKs, ensure the one on PATH/JAVA_HOME is JDK 25.
- The `-Dprism.order=sw` flag forces software rendering; remove it for hardware acceleration.

Build DEB installer with desktop integration:
1. From the project root, run:
   mvn -Plinux clean package
2. The `.deb` package will be created under:
   target/dist/Pedigree Chart Editor-${project.version}.deb

What the installer does:
- Uses `jpackage` to create a self-contained app with the required JavaFX modules.
- Installs a desktop entry (.desktop) and menu shortcut via `--linux-shortcut`.
- Registers category as "Utility" and menu group "Pedigree" so the app appears in the desktop environment's application menu.

Notes:
- The Linux packaging profile targets DEB by default. If you need RPM, you can change `--type deb` to `--type rpm` in the `pom.xml` under the `linux` profile and ensure `rpmbuild` is installed.
- The app icon is not specified yet; you can pass `--icon path/to/icon.png` in the jpackage arguments if you add an icon file to the repository.
- Package version must be numeric X.Y.Z; the build normalizes the Maven version and avoids leading 0 for compatibility.
