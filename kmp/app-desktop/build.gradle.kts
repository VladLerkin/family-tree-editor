import org.jetbrains.compose.*

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation(project(":ui"))
    implementation(compose.desktop.currentOs)
}

kotlin {
    jvmToolchain(21)
    sourceSets {
        val main by getting {
            kotlin.srcDirs("src/jvmMain/kotlin")
            resources.srcDirs("src/jvmMain/resources")
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.family.tree.desktop.MainKt"
        nativeDistributions {
            // For now keep defaults; packaging can be added later
        }
    }
}
