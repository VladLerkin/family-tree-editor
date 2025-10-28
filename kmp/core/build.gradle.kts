plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    androidTarget()
    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
            }
        }
        val androidMain by getting
        val desktopMain by getting
    }
    // Align Kotlin JVM toolchain for Android/JVM compilations in this module to 17
    jvmToolchain(17)
}

android {
    namespace = "com.family.tree.core"
    compileSdk = (project.findProperty("android.compileSdk") as String).toInt()
    defaultConfig {
        minSdk = (project.findProperty("android.minSdk") as String).toInt()
        targetSdk = (project.findProperty("android.targetSdk") as String).toInt()
    }
    // Align Java compile options for Android to 17
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}