import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

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
    
    // Web target
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    val generateBuildConfig by tasks.registering {
        val version = project.version.toString()
        val packageName = "com.family.tree.core"
        val outputDir = file("$buildDir/generated/buildConfig/commonMain/kotlin")
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
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
                implementation("io.ktor:ktor-client-core:3.0.3")
            }
        }
        
        androidMain.dependencies {
            implementation("io.ktor:ktor-client-okhttp:3.0.3")
            implementation("androidx.security:security-crypto:1.1.0")
            implementation("com.tom-roush:pdfbox-android:2.0.27.0")
        }
        
        val desktopMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-cio:3.0.3")
            }
        }
        
        iosMain.dependencies {
            implementation("io.ktor:ktor-client-darwin:3.0.3")
        }
        
        val wasmJsMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-js:3.0.3")
            }
        }
    }
    // Align Kotlin JVM toolchain for Android/JVM compilations in this module to 25
    jvmToolchain((project.findProperty("java.version") as String).toInt())
}

android {
    namespace = "com.family.tree.core"
    compileSdk = (project.findProperty("android.compileSdk") as String).toInt()
    defaultConfig {
        minSdk = (project.findProperty("android.minSdk") as String).toInt()
        targetSdk = (project.findProperty("android.targetSdk") as String).toInt()
    }
    // Align Java compile options for Android to 25
    val javaVer = (project.findProperty("java.version") as String).toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(javaVer)
        targetCompatibility = JavaVersion.toVersion(javaVer)
    }
}