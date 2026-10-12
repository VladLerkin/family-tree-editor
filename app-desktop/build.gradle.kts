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
        named("main") {
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
        val javaToolchains = project.extensions.findByType<JavaToolchainService>()
        val targetVersion = libs.versions.java.get().toInt()
        val toolchainLauncher = runCatching {
            javaToolchains?.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(targetVersion))
            }?.orNull
        }.getOrNull()
        val toolchainJavaHome = toolchainLauncher?.metadata?.installationPath?.asFile?.absolutePath

        val candidates = listOfNotNull(
            task.javaHome.getOrNull(),
            toolchainJavaHome,
            System.getenv("JAVA_HOME"),
            System.getProperty("java.home")
        )

        val localRulesFile = File(project.layout.buildDirectory.asFile.get(), "tmp/local-proguard-rules.pro")
        val resolvedJdkLib = resolveJdkLibraries(candidates, targetVersion.toString())
        
        if (resolvedJdkLib != null) {
            println("ProGuard: Using JDK JMODs at ${resolvedJdkLib.first}")
            localRulesFile.writeText(resolvedJdkLib.second)
        } else {
            println("WARNING: ProGuard could not find a JDK JMODs folder! Preverification might fail.")
            localRulesFile.writeText("# No JDK JMODs found")
        }
    }
}

fun resolveJdkLibraries(primaryCandidates: List<String>, targetVersion: String): Pair<String, String>? {
    // 1. Try primary candidates first (toolchain, task.javaHome, JAVA_HOME, java.home)
    for (candidate in primaryCandidates) {
        val dir = File(candidate)
        val result = findJmodsInJdkDir(dir)
        if (result != null) return result
    }

    // 2. Search standard directories, prioritizing folders matching targetVersion
    val searchRoots = mutableListOf<File>()
    val userHome = System.getProperty("user.home")
    searchRoots.add(File(userHome, ".sdkman/candidates/java"))
    searchRoots.add(File("/opt/hostedtoolcache"))
    searchRoots.add(File("C:/hostedtoolcache/windows"))
    searchRoots.add(File(userHome, "hostedtoolcache"))

    val osName = System.getProperty("os.name").lowercase()
    if (osName.contains("mac")) {
        searchRoots.add(File("/Library/Java/JavaVirtualMachines"))
        searchRoots.add(File(userHome, "Library/Java/JavaVirtualMachines"))
    } else if (osName.contains("win")) {
        searchRoots.add(File("C:/Program Files/Java"))
        searchRoots.add(File("C:/Users/runneradmin/.jdks"))
    } else {
        searchRoots.add(File("/usr/lib/jvm"))
    }

    // Collect all directories containing jmods
    val jmodsDirs = mutableListOf<File>()
    for (root in searchRoots) {
        if (root.exists() && root.isDirectory) {
            collectJmodsDirs(root, 0, jmodsDirs)
        }
    }

    // Sort to prioritize folders matching targetVersion (e.g. "25"), then highest version
    jmodsDirs.sortWith(Comparator { a, b ->
        val aMatch = a.absolutePath.contains(targetVersion)
        val bMatch = b.absolutePath.contains(targetVersion)
        when {
            aMatch && !bMatch -> -1
            !aMatch && bMatch -> 1
            else -> b.absolutePath.compareTo(a.absolutePath)
        }
    })

    if (jmodsDirs.isNotEmpty()) {
        val best = jmodsDirs.first()
        return createJmodsResult(best)
    }

    return null
}

fun findJmodsInJdkDir(dir: File): Pair<String, String>? {
    if (!dir.exists() || !dir.isDirectory) return null
    val directJmods = File(dir, "jmods")
    if (directJmods.exists() && File(directJmods, "java.base.jmod").exists()) {
        return createJmodsResult(directJmods)
    }
    val contentsHomeJmods = File(dir, "Contents/Home/jmods")
    if (contentsHomeJmods.exists() && File(contentsHomeJmods, "java.base.jmod").exists()) {
        return createJmodsResult(contentsHomeJmods)
    }
    return null
}

fun createJmodsResult(jmodsDir: File): Pair<String, String> {
    val jmodsPath = jmodsDir.absolutePath
    val rules = """
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
    """.trimIndent()
    return Pair(jmodsPath, rules)
}

fun collectJmodsDirs(dir: File, depth: Int, out: MutableList<File>) {
    if (depth > 4) return
    val jmods = File(dir, "jmods")
    if (jmods.exists() && File(jmods, "java.base.jmod").exists()) {
        out.add(jmods)
        return
    }
    val contentsJmods = File(dir, "Contents/Home/jmods")
    if (contentsJmods.exists() && File(contentsJmods, "java.base.jmod").exists()) {
        out.add(contentsJmods)
        return
    }
    val subdirs = dir.listFiles { f -> f.isDirectory } ?: return
    for (sub in subdirs) {
        collectJmodsDirs(sub, depth + 1, out)
    }
}

tasks.register<JavaExec>("runCacheTest") {
    group = "verification"
    description = "Runs the cache unit test for GenealogyTools."
    mainClass.set("com.family.tree.desktop.TestCacheKt")
    classpath = sourceSets["main"].runtimeClasspath
}





