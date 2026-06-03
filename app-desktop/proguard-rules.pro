# ProGuard rules for Family Tree Editor Desktop

# General JVM and Coroutines
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
-keep class kotlinx.coroutines.** { *; }

# Koin
-keep class org.koin.** { *; }

# Kotlinx Serialization
-keep class kotlinx.serialization.json.** { *; }
-keepclassmembernames class * {
    @kotlinx.serialization.SerialName <fields>;
}

# Ktor
-keep class io.ktor.** { *; }

# Desktop specific
-keep class com.family.tree.desktop.MainKt { *; }
-keep class androidx.compose.desktop.** { *; }
-dontwarn androidx.compose.ui.platform.AccessibilityConfigImpl

# Models
-keep class com.family.tree.core.model.** { *; }

# Fix for Java 25 / ProGuard 7.7 issues with new class versions
-dontwarn **
-ignorewarnings

# Manually include Java modules for ProGuard since Java 25 candidate on the system lacks JMODs
-libraryjars /Users/vlad/.sdkman/candidates/java/21.0.9-tem/jmods/java.base.jmod(!module-info.class)
-libraryjars /Users/vlad/.sdkman/candidates/java/21.0.9-tem/jmods/java.desktop.jmod(!module-info.class)
-libraryjars /Users/vlad/.sdkman/candidates/java/21.0.9-tem/jmods/java.datatransfer.jmod(!module-info.class)
-libraryjars /Users/vlad/.sdkman/candidates/java/21.0.9-tem/jmods/java.logging.jmod(!module-info.class)
-libraryjars /Users/vlad/.sdkman/candidates/java/21.0.9-tem/jmods/java.xml.jmod(!module-info.class)
-libraryjars /Users/vlad/.sdkman/candidates/java/21.0.9-tem/jmods/java.prefs.jmod(!module-info.class)
-libraryjars /Users/vlad/.sdkman/candidates/java/21.0.9-tem/jmods/java.naming.jmod(!module-info.class)
-libraryjars /Users/vlad/.sdkman/candidates/java/21.0.9-tem/jmods/java.security.jgss.jmod(!module-info.class)
-libraryjars /Users/vlad/.sdkman/candidates/java/21.0.9-tem/jmods/java.instrument.jmod(!module-info.class)
-libraryjars /Users/vlad/.sdkman/candidates/java/21.0.9-tem/jmods/jdk.unsupported.jmod(!module-info.class)
