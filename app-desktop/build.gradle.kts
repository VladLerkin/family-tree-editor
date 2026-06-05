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
    implementation(libs.ktor.client.core)
    implementation(libs.kotlinx.serialization.json)
    implementation("org.slf4j:slf4j-nop:2.0.12")
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
        jvmArgs += listOf("--enable-native-access=ALL-UNNAMED")
        
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
                infoPlist {
                    extraKeysRawXml = """
                        <key>NSMicrophoneUsageDescription</key>
                        <string>Приложение использует микрофон для голосового ввода данных</string>
                    """.trimIndent()
                }
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
            val localRulesFile = File(project.layout.buildDirectory.asFile.get(), "tmp/local-proguard-rules.pro")
            localRulesFile.parentFile.mkdirs()
            if (!localRulesFile.exists()) {
                localRulesFile.writeText("# Placeholder")
            }
            
            configurationFiles.from(proguardRules, localRulesFile)
            
            isEnabled.set(true)
            optimize.set(false)
        }
    }
}

tasks.withType<org.jetbrains.compose.desktop.application.tasks.AbstractProguardTask>().configureEach {
    val task = this
    doFirst {
        val javaHomeVal = task.javaHome.getOrNull()?.toString() ?: System.getProperty("java.home")
        val jmodsDir = findJmodsDir(javaHomeVal)
        val localRulesFile = File(project.layout.buildDirectory.asFile.get(), "tmp/local-proguard-rules.pro")
        
        if (jmodsDir != null) {
            val jmodsPath = jmodsDir.absolutePath
            println("ProGuard: Using JDK JMODs at $jmodsPath")
            localRulesFile.writeText("""
                # Automatically resolved JDK modules for ProGuard
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
        } else {
            println("WARNING: ProGuard could not find a JDK JMODs folder! Preverification might fail.")
            localRulesFile.writeText("# No JDK JMODs found")
        }
    }
}

fun findJmodsDir(currentJavaHome: String): File? {
    // 1. Try current javaHome
    val currentJmods = File(currentJavaHome, "jmods")
    if (currentJmods.exists() && currentJmods.isDirectory) {
        return currentJmods
    }
    
    // 2. Try standard search paths
    val searchDirs = mutableListOf<File>()
    val userHome = System.getProperty("user.home")
    
    // SDKMAN
    searchDirs.add(File(userHome, ".sdkman/candidates/java"))
    
    // GitHub Actions Hosted Tool Cache
    searchDirs.add(File("/opt/hostedtoolcache"))
    searchDirs.add(File("C:/hostedtoolcache/windows"))
    searchDirs.add(File(userHome, "hostedtoolcache"))
    
    // OS specific defaults
    val osName = System.getProperty("os.name").lowercase()
    if (osName.contains("mac")) {
        searchDirs.add(File("/Library/Java/JavaVirtualMachines"))
        searchDirs.add(File(userHome, "Library/Java/JavaVirtualMachines"))
    } else if (osName.contains("win")) {
        searchDirs.add(File("C:/Program Files/Java"))
        searchDirs.add(File("C:/Users/runneradmin/.jdks"))
    } else {
        searchDirs.add(File("/usr/lib/jvm"))
    }
    
    // Search recursively up to 3 levels deep for any directory named "jmods"
    for (dir in searchDirs) {
        if (dir.exists() && dir.isDirectory) {
            val found = findJmodsRecursively(dir, 0)
            if (found != null) {
                return found
            }
        }
    }
    
    return null
}

fun findJmodsRecursively(dir: File, depth: Int): File? {
    if (depth > 3) return null
    
    val jmods = File(dir, "jmods")
    if (jmods.exists() && jmods.isDirectory && jmods.listFiles()?.any { it.name.endsWith(".jmod") } == true) {
        return jmods
    }
    
    val subdirs = dir.listFiles { f -> f.isDirectory } ?: return null
    for (sub in subdirs) {
        val found = findJmodsRecursively(sub, depth + 1)
        if (found != null) {
            return found
        }
    }
    return null
}

tasks.register<JavaExec>("runCacheTest") {
    group = "verification"
    description = "Runs the cache unit test for GenealogyTools."
    mainClass.set("com.family.tree.desktop.TestCacheKt")
    classpath = sourceSets["main"].runtimeClasspath
}





