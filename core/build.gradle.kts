plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    androidTarget()
    jvm("desktop")
    
    // iOS targets
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
            implementation("io.ktor:ktor-client-core:2.3.13")
        }
        
        androidMain.dependencies {
            implementation("io.ktor:ktor-client-okhttp:2.3.13")
            implementation("androidx.security:security-crypto:1.1.0")
            implementation("com.tom-roush:pdfbox-android:2.0.27.0")
        }
        
        val desktopMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-cio:2.3.13")
            }
        }
        
        iosMain.dependencies {
            implementation("io.ktor:ktor-client-darwin:2.3.13")
        }
    }
    // Align Kotlin JVM toolchain for Android/JVM compilations in this module to 21
    jvmToolchain(21)
}

android {
    namespace = "com.family.tree.core"
    compileSdk = (project.findProperty("android.compileSdk") as String).toInt()
    defaultConfig {
        minSdk = (project.findProperty("android.minSdk") as String).toInt()
        targetSdk = (project.findProperty("android.targetSdk") as String).toInt()
    }
    // Align Java compile options for Android to 21
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}