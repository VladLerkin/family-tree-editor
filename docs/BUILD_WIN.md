# macOS Distributable (DMG)

Prerequisites:
- windows with WiX Toolset v3.x installed https://github.com/wixtoolset/wix3/releases.
- JDK 24 installed and `JAVA_HOME` pointing to it (jpackage is included with the JDK).
- Maven 3.8+.

Build steps:
1. From the project root, run:
   mvn -P windows clean package
2. The MSI will be created under:
   target/dist/Pedigree Chart Editor-${project.version}.msi

Notes:
- Code signing and notarization are not configured here. The resulting DMG is suitable for development/testing.
- The build profile uses jpackage to create a custom runtime image with the required JavaFX modules for a self-contained app bundle.
- The win msi requires a numeric app version (major.minor.patch). The build extracts this from the project version and ignores any qualifier like -SNAPSHOT.
