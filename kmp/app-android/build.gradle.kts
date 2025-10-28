plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.family.tree.android"
    compileSdkVersion((project.findProperty("android.compileSdk") as String).toInt())

    defaultConfig {
        applicationId = "com.family.tree.android"
        minSdk = (project.findProperty("android.minSdk") as String).toInt()
        targetSdk = (project.findProperty("android.targetSdk") as String).toInt()
        versionCode = 1
        versionName = "0.1"
    }
    buildFeatures { compose = true }
    composeOptions {
        kotlinCompilerExtensionVersion = (project.findProperty("compose.version") as String)
    }
    // Align Java toolchain for Android to 17
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Align Kotlin JVM toolchain for Android to 17
kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":ui"))
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
