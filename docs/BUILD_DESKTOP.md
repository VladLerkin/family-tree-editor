# Desktop (Compose Multiplatform): Build and Run

This guide explains how to build and run the Kotlin Multiplatform (KMP) Desktop application on macOS, Linux, and Windows.

## Prerequisites

- **JDK 21** (required for Gradle and Compose Multiplatform)
- **Gradle 9.1.0** (included via wrapper: `./gradlew` or `gradlew.bat`)
- **Internet access** for Gradle to download dependencies
- **Linux only:** Desktop environment (GNOME/KDE/etc.) for GUI display

### Verify JDK Installation

Check your Java version:
```bash
java -version
```
Ensure it reports JDK 21. If you have multiple JDKs installed, set `JAVA_HOME`:

**macOS:**
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

**Linux:**
```bash
export JAVA_HOME=/path/to/jdk-21
# Common locations: /usr/lib/jvm/java-21-openjdk-amd64 (Ubuntu/Debian)
#                   /usr/lib/jvm/java-21-openjdk (Fedora/RHEL)
```

**Windows (PowerShell):**
```powershell
$env:JAVA_HOME="C:\Path\To\jdk-21"
```

**Windows (CMD):**
```cmd
set JAVA_HOME=C:\Path\To\jdk-21
```

## Quick Start

### macOS / Linux

From the `kmp` directory:
```bash
cd kmp
./gradlew :app-desktop:run
```

### Windows

From the `kmp` directory:

**PowerShell:**
```powershell
cd kmp
.\gradlew.bat :app-desktop:run
```

**CMD:**
```cmd
cd kmp
gradlew.bat :app-desktop:run
```

The Desktop application window will open with:
- **Left panel:** Individuals and Families lists
- **Center:** Interactive canvas with family tree (pan/zoom)
- **Right panel:** Properties inspector

## Build Without Running

To compile the Desktop application without launching it:

**macOS/Linux:**
```bash
cd kmp
./gradlew :app-desktop:build
```

**Windows:**
```powershell
cd kmp
.\gradlew.bat :app-desktop:build
```

## Run from IntelliJ IDEA

1. Open the `kmp` directory as a project (IntelliJ will detect Gradle)
2. Sync Gradle dependencies (IntelliJ usually prompts automatically)
3. Navigate to `app-desktop/src/jvmMain/kotlin/com/family/tree/desktop/Main.kt`
4. Click the green ▶️ icon next to `fun main()` or use Run → Run 'Main'
5. Ensure the project JDK is set to 21 in **File → Project Structure → Project SDK**

## Desktop Features

- **File Operations:**
  - Open/Save JSON (cross-platform simple format)
  - Open/Save .rel (ZIP-based project format with layout/metadata)
  - Export SVG (current view or fit-to-view)
  - Export PNG (planned)

- **Canvas Interaction:**
  - **Pan:** Click and drag on canvas
  - **Zoom:** Mouse wheel / trackpad scroll (zooms under cursor)
  - **Accelerated Zoom:** Hold Cmd (macOS) or Ctrl (Windows/Linux) while scrolling
  - **Toolbar Buttons:** `[-]` zoom out, `[+]` zoom in, `[Reset]` fit-to-view

- **Keyboard Shortcuts:**
  - `Esc` — Clear selection
  - `+` / `=` / `NumPad +` — Zoom in (animated)
  - `-` / `NumPad -` — Zoom out (animated)
  - Hold `Cmd`/`Ctrl` for faster zoom steps

- **Selection & Editing:**
  - Click a person card to select
  - Double-click a person card to open editor dialog
  - Right-click (or long-press on trackpad) for context menu: Edit, Center On, Reset

## Clean Build

If you encounter build issues or want to ensure a fresh compile:

**macOS/Linux:**
```bash
cd kmp
./gradlew clean :app-desktop:build
```

**Windows:**
```powershell
cd kmp
.\gradlew.bat clean :app-desktop:build
```

## Packaging (Future)

Native installers (DMG for macOS, MSI for Windows, DEB/RPM for Linux) are planned but not yet implemented in the KMP branch. The Desktop app currently runs via Gradle or IDE.

### Platform-Specific Packaging

**macOS (DMG):**
- Compose Desktop can create DMG installers with `createDistributable` / `packageDmg` tasks.
- For signed/notarized builds, you'll need a Developer ID certificate.

**Windows (MSI/EXE):**
- Requires WiX Toolset (MSI) or Inno Setup (EXE).
- Compose Desktop `packageMsi` / `packageExe` tasks handle creation.

**Linux (Portable Archives / DEB / RPM):**
- **Portable distributions** (tar.gz/zip) are the recommended cross-distro option for GitHub releases.
- DEB packages require `fakeroot` and `dpkg-deb` (available on Debian/Ubuntu: `sudo apt install fakeroot dpkg`).
- RPM packages require `rpmbuild` (available on Fedora/RHEL: `sudo dnf install rpm-build`).
- For AppImage, additional tooling is required (not included in Compose Desktop by default).
- Compose Desktop tasks: `createDistributable`, `packageDeb`, `packageRpm`.

For distribution, you can use:
- **Compose Desktop's `createDistributable` / `packageDistributionForCurrentOS` tasks** (once configured)
- **jpackage** (manual packaging with a custom runtime image)

Refer to [Compose Multiplatform Desktop packaging docs](https://github.com/JetBrains/compose-multiplatform/tree/master/tutorials/Native_distributions_and_local_execution) for details.

## Troubleshooting

### "Unsupported class file major version 65" or similar
- Ensure Gradle is using JDK 21. Check with `./gradlew --version` and verify the "JVM" line reports version 21.
- Set `JAVA_HOME` as shown above or configure the Gradle JVM in IntelliJ (**Preferences → Build, Execution, Deployment → Build Tools → Gradle → Gradle JVM**).

### "Could not resolve dependencies" or network errors
- Ensure you have internet access for Gradle to download dependencies from Maven Central and Google Maven.
- If behind a corporate proxy, configure Gradle's proxy settings in `~/.gradle/gradle.properties`.

### Application window does not appear or crashes on launch
- Check the console output for stack traces.
- Ensure no other process is using the same port or resources.
- Try a clean build: `./gradlew clean :app-desktop:run`.

### Graphics rendering issues (blank canvas, flickering, etc.)
- Update your graphics drivers.
- **Linux:** Ensure you have necessary OpenGL libraries installed:
  - Ubuntu/Debian: `sudo apt install libgl1-mesa-glx libgl1-mesa-dri`
  - Fedora/RHEL: `sudo dnf install mesa-libGL mesa-dri-drivers`
  - Arch: `sudo pacman -S mesa`
- Try forcing software rendering (if supported by Compose Desktop; currently no flag equivalent to JavaFX's `-Dprism.order=sw`).
- **Linux Wayland users:** If you experience issues, try running under X11 instead (set `GDM_BACKEND=x11` or log into an X11 session).

## Notes

- The KMP Desktop target uses **Compose for Desktop 1.7.0** and **Kotlin 2.0.21**.
- The build is **non-modular** (does not use Java Platform Module System).
- The Desktop app shares the `:core` and `:ui` modules with the Android app, providing cross-platform business logic and UI.
- File dialogs use AWT's `FileDialog` on Desktop for native look and feel.
