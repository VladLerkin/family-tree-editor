import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidLibrary {
        namespace = "com.family.tree.core"
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
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.ktor.client.core)
                implementation(libs.koin.core)
            }
        }
        
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.security.crypto)
            implementation(libs.pdfbox.android)
        }
        
        val desktopMain by getting {
            dependencies {
                implementation(libs.ktor.client.cio)
            }
        }
        
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        
        val wasmJsMain by getting {
            dependencies {
                implementation(libs.ktor.client.js)
            }
        }
    }
    targets.all {
        compilations.all {
            compilerOptions.configure {
                freeCompilerArgs.add("-Xexpect-actual-classes")
            }
        }
    }
    // Align Kotlin JVM toolchain for Android/JVM compilations in this module to 25
    jvmToolchain(libs.versions.java.get().toInt())
}
