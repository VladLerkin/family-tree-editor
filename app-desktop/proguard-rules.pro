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
