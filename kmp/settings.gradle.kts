pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    // Pin plugin versions here; libraries use gradle.properties
    id("org.jetbrains.kotlin.multiplatform") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" apply false
    id("org.jetbrains.compose") version "1.7.0" apply false
    id("com.android.application") version "8.12.0" apply false
    id("com.android.library") version "8.12.0" apply false
}

rootProject.name = "family-tree-kmp"
include(":core")
include(":ui")
include(":app-desktop")
include(":app-android")
