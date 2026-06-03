# ProGuard/R8 rules for Family Tree Editor Android

# General Kotlin Multiplatform and Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory {}

# Koin
-keep class org.koin.** { *; }
-keepclassmembers class * {
    @org.koin.core.annotation.KoinInternalApi *;
}

# Kotlinx Serialization
-keepattributes RuntimeVisibleAnnotations, AnnotationDefault
-keep class kotlinx.serialization.json.** { *; }
-keepclassmembernames class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclassmembers class ** {
    *** Companion;
    *** $serializer;
}

# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Voyager
-keep class cafe.adriel.voyager.** { *; }

# PDFBox Android
-keep class com.tomroush.pdfbox.** { *; }
-dontwarn com.tomroush.pdfbox.**
-dontwarn com.gemalto.jp2.JP2Decoder

# Google ErrorProne (common dependency from Tink/Security)
-dontwarn com.google.errorprone.annotations.**

# Keep models in core package to avoid serialization issues
-keep class com.family.tree.core.model.** { *; }

# Compose
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }

# Reflection support
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
