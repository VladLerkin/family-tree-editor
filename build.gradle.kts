buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.guardsquare:proguard-gradle:7.8.2")
    }
}

plugins {
    // Root project
}

// Надежный способ получения версии из каталога
val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
val appVersion = catalog.findVersion("app-version").get().requiredVersion

allprojects {
    version = appVersion
}

// tasks.register<Delete>("clean") {
//     delete(rootProject.layout.buildDirectory)
// }
