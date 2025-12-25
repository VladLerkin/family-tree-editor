import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

repositories {
    google()
    mavenCentral()
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "family-tree-web.js"
            }
        }
        binaries.executable()
    }
    
    sourceSets {
        val wasmJsMain by getting {
            dependencies {
                implementation(project(":ui"))
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.ui)
                implementation(compose.material3)
            }
        }
    }
}

tasks.named("wasmJsBrowserProductionWebpack") {
    doLast {
        val sourceHtml = file("src/wasmJsMain/resources/index.html")
        val targetHtml = file("build/dist/wasmJs/productionExecutable/index.html")
        sourceHtml.copyTo(targetHtml, overwrite = true)
    }
}

tasks.named("wasmJsBrowserDevelopmentWebpack") {
    doLast {
        val sourceHtml = file("src/wasmJsMain/resources/index.html")
        val targetHtml = file("build/dist/wasmJs/developmentExecutable/index.html")
        sourceHtml.copyTo(targetHtml, overwrite = true)
    }
}
