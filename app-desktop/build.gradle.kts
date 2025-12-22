import org.jetbrains.compose.*
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

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
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "FamilyTreeEditor"
            packageVersion = "1.3.24"
            
            description = "Family Tree Editor Application"
            copyright = "© 2024 Family Tree. All rights reserved."
            vendor = "Family Tree"
            
            linux {
                modules("java.instrument", "jdk.unsupported")
                iconFile.set(project.file("src/jvmMain/resources/icon.png"))
            }
            
            macOS {
                bundleID = "com.family.tree.desktop"
                iconFile.set(project.file("src/jvmMain/resources/icon.icns"))
                dockName = "Family Tree"
            }
            
            windows {
                menuGroup = "Family Tree"
                iconFile.set(project.file("src/jvmMain/resources/icon.ico"))
                upgradeUuid = "12345678-1234-1234-1234-123456789012" // Fixed UUID for upgrades
            }
        }
    }
}
