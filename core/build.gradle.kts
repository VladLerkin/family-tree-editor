import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    val ktorVersion = project.findProperty("ktor.version") as String
    val serializationVersion = project.findProperty("serialization.version") as String


    androidLibrary {
        namespace = "com.family.tree.core"
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

    val generateBuildConfig by tasks.registering {
        val version = project.version.toString()
        val packageName = "com.family.tree.core"
        val outputDir = layout.buildDirectory.dir("generated/buildConfig/commonMain/kotlin").get().asFile
        inputs.property("version", version)
        outputs.dir(outputDir)

        doLast {
            val outputFile = file("${outputDir.path}/${packageName.replace(".", "/")}/BuildConfig.kt")
            outputFile.parentFile.mkdirs()
            outputFile.writeText(
                """
                package $packageName

                /**
                 * Build configuration constants.
                 * Automatically generated from gradle.properties.
                 */
                object BuildConfig {
                    const val APP_VERSION = "$version"
                }
                """.trimIndent()
            )
        }
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(generateBuildConfig.map { it.outputs.files.asPath })
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
                implementation("io.ktor:ktor-client-core:$ktorVersion")
            }
        }
        
        androidMain.dependencies {
            implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
            implementation("androidx.security:security-crypto:1.1.0")
            implementation("com.tom-roush:pdfbox-android:2.0.27.0")
        }
        
        val desktopMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-cio:$ktorVersion")
            }
        }
        
        iosMain.dependencies {
            implementation("io.ktor:ktor-client-darwin:$ktorVersion")
        }
        
        val wasmJsMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-js:$ktorVersion")
            }
        }
    }
    // Align Kotlin JVM toolchain for Android/JVM compilations in this module to 25
    jvmToolchain((project.findProperty("java.version") as String).toInt())
}
