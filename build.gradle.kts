import org.jetbrains.compose.*

plugins {
    // Root project keeps minimal configuration; real plugins applied in subprojects
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
    version = project.findProperty("app.version")?.toString() ?: "1.0.0"
}

subprojects {
}