import org.jetbrains.compose.*
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.compose)
}

kotlin {

    androidLibrary {
        namespace = "com.family.tree.ui"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        
        // Java compatibility
        with(java) {
             toolchain {
                 languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get().toInt()))
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
                implementation(libs.androidx.activity.compose)
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
    // Align Kotlin JVM toolchain in this module to 25 (Android & desktop target compilation)
    jvmToolchain(libs.versions.java.get().toInt())
}
