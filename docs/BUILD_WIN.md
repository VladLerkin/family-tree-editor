# Windows: Run and Package

This guide explains how to run the app during development and how to build an MSI installer on Windows.

Prerequisites:
- Windows 10/11
- JDK 25 installed and `JAVA_HOME` pointing to it (jpackage is included with the JDK)
- Maven 3.8+
- (For MSI packaging only) WiX Toolset v3.x installed: https://github.com/wixtoolset/wix3/releases

Run during development:
1. From the project root, run (JavaFX plugin will launch the app):
   PowerShell (recommended):
   .\mvnw.cmd "-Dprism.order=sw" javafx:run
   
   Alternatives:
   - PowerShell stop-parsing:
     .\mvnw.cmd --% -Dprism.order=sw javafx:run
   - CMD.exe:
     .\mvnw.cmd -Dprism.order=sw javafx:run
   
Tips:
- Important for PowerShell: quote -D system properties ("-Dname=value") or use `--%`. Otherwise PowerShell may break the argument and Maven will fail with "Unknown lifecycle phase '.order=sw'".
- If you have multiple JDKs, ensure the one on PATH/JAVA_HOME is JDK 25.
- If you already have Maven installed and on PATH, you may use `mvn` instead of `mvnw.cmd`.
- The `-Dprism.order=sw` flag forces software rendering; remove it if you want hardware acceleration.
- If you see the message "Cannot find wrapperUrl in .mvn\wrapper\maven-wrapper.properties", update your local repo (git pull). Our wrapper config now includes both `distributionUrl` and `wrapperUrl`. Alternatively, ensure that file contains:
  distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.8/apache-maven-3.9.8-bin.zip
  wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar

Build MSI installer:
1. From the project root, run:
   .\mvnw.cmd -Pwindows clean package
2. The MSI will be created under:
   target/dist/Pedigree Chart Editor-${project.version}.msi

Notes:
- The Windows packaging profile uses jpackage to create a custom runtime image with the required JavaFX modules (non-modular app).
- The MSI requires a numeric app version (major.minor.patch). The build extracts this from the project version and ignores any qualifier like -SNAPSHOT.
