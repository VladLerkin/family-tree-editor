pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    id("org.jetbrains.kotlin.multiplatform") version "2.3.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.20" apply false
    id("org.jetbrains.compose") version "1.10.3" apply false
    id("com.android.application") version "9.1.1" apply false
    id("com.android.library") version "9.1.1" apply false
    id("com.android.kotlin.multiplatform.library") version "9.1.1" apply false

    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "family-tree-kmp"
include(":core")
include(":ui")
include(":app-desktop")
include(":app-android")
include(":app-ios")
include(":app-web")
