# macOS Distributable (DMG)

Prerequisites:
- macOS with Xcode Command Line Tools installed (for `hdiutil` utilities used by the OS).
- JDK 24 installed and `JAVA_HOME` pointing to it (jpackage is included with the JDK).
- Maven 3.8+.

Build steps:
1. From the project root, run:
   mvn -Pmac clean package
2. The DMG will be created under:
   target/dist/Pedigree Chart Editor-${project.version}.dmg

Notes:
- Code signing and notarization are not configured here. The resulting DMG is suitable for development/testing.
- If macOS gatekeeper blocks the app, use context menu “Open” on first launch or sign/notarize it per your distribution needs.
- The build profile uses jpackage to create a custom runtime image with the required JavaFX modules for a self-contained app bundle.
- The macOS DMG requires a numeric app version (major.minor.patch). The build extracts this from the project version and ignores any qualifier like -SNAPSHOT.
- macOS installer tools do not accept versions starting with 0. If your project version begins with 0, the build automatically uses 1.minor.patch for the DMG.
