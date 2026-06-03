import org.jetbrains.compose.*
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.compose)
}

dependencies {
    implementation(project(":core"))
    implementation(project(":ui"))
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")
    implementation(libs.koin.core)
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
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
            packageVersion = libs.versions.app.version.get()
            
            description = "Family Tree Editor Application"
            copyright = "© 2026 Family Tree. All rights reserved."
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
        
        buildTypes.release.proguard {
            version.set("7.8.2")
            
            val proguardRules = files("proguard-rules.pro")
            val javaHome = System.getProperty("java.home")
            val jmodsDir = File(javaHome, "jmods")
            
            val localRulesFile = File(project.layout.buildDirectory.asFile.get(), "tmp/local-proguard-rules.pro")
            localRulesFile.parentFile.mkdirs()
            
            if (jmodsDir.exists()) {
                // Current JDK has jmods (like on GitHub Actions), manually include them since the Compose plugin fails to do so on Java 25
                val jmodsPath = jmodsDir.absolutePath
                localRulesFile.writeText("""
                    # Manually include current JDK modules for ProGuard since Compose plugin does not detect JDK 25's structure
                    -libraryjars $jmodsPath/java.base.jmod(!module-info.class)
                    -libraryjars $jmodsPath/java.desktop.jmod(!module-info.class)
                    -libraryjars $jmodsPath/java.datatransfer.jmod(!module-info.class)
                    -libraryjars $jmodsPath/java.logging.jmod(!module-info.class)
                    -libraryjars $jmodsPath/java.xml.jmod(!module-info.class)
                    -libraryjars $jmodsPath/java.prefs.jmod(!module-info.class)
                    -libraryjars $jmodsPath/java.naming.jmod(!module-info.class)
                    -libraryjars $jmodsPath/java.security.jgss.jmod(!module-info.class)
                    -libraryjars $jmodsPath/java.instrument.jmod(!module-info.class)
                    -libraryjars $jmodsPath/jdk.unsupported.jmod(!module-info.class)
                """.trimIndent())
                configurationFiles.from(proguardRules, localRulesFile)
            } else {
                // Fallback to local JDK in SDKMAN if current JDK lacks jmods (like on local developer machine)
                val sdkmanJavaDir = File(System.getProperty("user.home"), ".sdkman/candidates/java")
                val fallbackJdk = sdkmanJavaDir.listFiles()?.firstOrNull { File(it, "jmods").exists() }
                if (fallbackJdk != null) {
                    val jmodsPath = File(fallbackJdk, "jmods").absolutePath
                    localRulesFile.writeText("""
                        # Automatically generated fallback for local JDK missing jmods
                        -libraryjars $jmodsPath/java.base.jmod(!module-info.class)
                        -libraryjars $jmodsPath/java.desktop.jmod(!module-info.class)
                        -libraryjars $jmodsPath/java.datatransfer.jmod(!module-info.class)
                        -libraryjars $jmodsPath/java.logging.jmod(!module-info.class)
                        -libraryjars $jmodsPath/java.xml.jmod(!module-info.class)
                        -libraryjars $jmodsPath/java.prefs.jmod(!module-info.class)
                        -libraryjars $jmodsPath/java.naming.jmod(!module-info.class)
                        -libraryjars $jmodsPath/java.security.jgss.jmod(!module-info.class)
                        -libraryjars $jmodsPath/java.instrument.jmod(!module-info.class)
                        -libraryjars $jmodsPath/jdk.unsupported.jmod(!module-info.class)
                    """.trimIndent())
                    configurationFiles.from(proguardRules, localRulesFile)
                } else {
                    configurationFiles.from(proguardRules)
                }
            }
            
            isEnabled.set(true)
            optimize.set(false)
        }
    }
}
