pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    // Pin plugin versions here; libraries use gradle.properties
    id("org.jetbrains.kotlin.multiplatform") version "2.2.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.21" apply false
    id("org.jetbrains.compose") version "1.9.3" apply false
    id("com.android.application") version "8.13.2" apply false
    id("com.android.library") version "8.13.2" apply false
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "family-tree-kmp"
include(":core")
include(":ui")
include(":app-desktop")
include(":app-android")
include(":app-ios")
include(":app-web")
