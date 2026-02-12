import org.jetbrains.compose.*
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {

    androidLibrary {
        namespace = "com.family.tree.ui"
        compileSdk = (project.findProperty("android.compileSdk") as String).toInt()
        minSdk = (project.findProperty("android.minSdk") as String).toInt()
        
        // Java compatibility
        with(java) {
             toolchain {
                 languageVersion.set(JavaLanguageVersion.of((project.findProperty("java.version") as String).toInt()))
             }
        }
    }

    jvm("desktop")
    
    // iOS targets
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    
    // Web target
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.animation)
        }
        val androidMain by getting {
            dependencies {
                implementation("androidx.activity:activity-compose:1.12.1")
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
    // Align Kotlin JVM toolchain in this module to 25 (Android & desktop target compilation)
    jvmToolchain((project.findProperty("java.version") as String).toInt())
}
