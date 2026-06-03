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
