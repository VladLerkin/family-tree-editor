import org.jetbrains.compose.*

plugins {
    // Root project keeps minimal configuration; real plugins applied in subprojects
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

subprojects {
}