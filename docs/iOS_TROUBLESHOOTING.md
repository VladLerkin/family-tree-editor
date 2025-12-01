# iOS Support Troubleshooting

This document addresses known issues with iOS support in the Family Tree KMP project and provides solutions.

## Current Status

iOS support has been **structurally implemented** but requires additional configuration to build successfully:

✅ **Completed:**
- iOS targets added to `core` and `ui` modules (iosX64, iosArm64, iosSimulatorArm64)
- `app-ios` module created with proper structure
- iOS entry point (`MainViewController`) implemented
- iOS source directories created
- Comprehensive documentation (BUILD_IOS.md)

❌ **Known Issue:**
Build fails with `ClassNotFoundException: org.gradle.api.internal.plugins.DefaultArtifactPublicationSet` when iOS targets are enabled.

## The Problem

When building the project with iOS targets enabled, Gradle fails during configuration phase:

```
FAILURE: Build failed with an exception.
* Where:
Build file '/Users/yav/IdeaProjects/rel/core/build.gradle.kts' line: 12
* What went wrong:
org/gradle/api/internal/plugins/DefaultArtifactPublicationSet
> org.gradle.api.internal.plugins.DefaultArtifactPublicationSet
```

This error occurs at the line where iOS targets are declared (e.g., `iosX64()`).

### Root Cause

The issue is likely caused by one or more of the following:

1. **Kotlin/Native distribution not downloaded:** First-time iOS target configuration requires Kotlin/Native toolchain download, which may fail or be incomplete.

2. **Version compatibility:** The current combination of:
   - Gradle 9.1.0
   - Kotlin 2.0.21
   - Compose Multiplatform 1.7.0
   
   May have compatibility issues with iOS targets. Kotlin/Native support for iOS in Compose Multiplatform was still maturing in these versions.

3. **Gradle internal API change:** The `DefaultArtifactPublicationSet` class is an internal Gradle API that may have been refactored between versions.

## Solutions

### Solution 1: Update to Latest Stable Versions (Recommended)

Update `settings.gradle.kts` and `gradle.properties` to use newer, more stable versions:

**settings.gradle.kts:**
```kotlin
plugins {
    id("org.jetbrains.kotlin.multiplatform") version "2.1.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.21" apply false
    id("org.jetbrains.compose") version "1.7.1" apply false
    id("com.android.application") version "8.7.3" apply false
    id("com.android.library") version "8.7.3" apply false
}
```

**gradle.properties:**
```properties
kotlin.version=2.1.21
compose.version=1.7.1
agp.version=8.7.3
```

Then clean and rebuild:
```bash
# Navigate to project root (already there)
./gradlew --stop
./gradlew clean
./gradlew projects
```

### Solution 2: Manually Download Kotlin/Native

If the issue is due to incomplete Kotlin/Native download:

```bash
# Navigate to project root (already there)
./gradlew --stop

# Trigger Kotlin/Native download
./gradlew :core:tasks --dry-run

# Try building again
./gradlew projects
```

### Solution 3: Downgrade Gradle (If Needed)

If the issue persists, try Gradle 8.10 (known to work well with Kotlin 2.0.x):

**gradle/wrapper/gradle-wrapper.properties:**
```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.10-bin.zip
```

Then:
```bash
# Navigate to project root (already there)
./gradlew wrapper --gradle-version=8.10
./gradlew --stop
./gradlew clean projects
```

### Solution 4: Use Official Compose Multiplatform Template

For a guaranteed working iOS setup, reference the official JetBrains template:

```bash
# Clone the official template in a separate directory
git clone https://github.com/JetBrains/compose-multiplatform-template.git

# Compare build configurations
diff compose-multiplatform-template/build.gradle.kts kmp/build.gradle.kts
diff compose-multiplatform-template/settings.gradle.kts kmp/settings.gradle.kts
```

Key differences to look for:
- Kotlin and Compose versions
- Plugin application order
- iOS target configuration patterns
- Framework export settings

### Solution 5: Temporarily Disable iOS for Desktop/Android Development

If you need to continue development on Desktop/Android while investigating iOS:

**Comment out iOS targets in `core/build.gradle.kts`:**
```kotlin
kotlin {
    androidTarget()
    jvm("desktop")
    
    // iOS targets (temporarily disabled)
    // iosX64()
    // iosArm64()
    // iosSimulatorArm64()

    sourceSets {
        val commonMain by getting { /* ... */ }
        val androidMain by getting
        val desktopMain by getting
        
        // Comment out iOS source sets
        // val iosMain by creating { /* ... */ }
    }
}
```

Repeat for `ui/build.gradle.kts` and comment out `:app-ios` in `settings.gradle.kts`:
```kotlin
include(":core")
include(":ui")
include(":app-desktop")
include(":app-android")
// include(":app-ios")  // Temporarily disabled
```

## Verification Steps

Once you've applied a solution, verify iOS support works:

### 1. Check Gradle Configuration
```bash
# Navigate to project root (already there)
./gradlew projects
```
Should show all 5 modules including `:app-ios`.

### 2. Build iOS Framework
```bash
./gradlew :app-ios:linkDebugFrameworkIosSimulatorArm64
```
Should complete successfully and create framework at:
```
app-ios/build/bin/iosSimulatorArm64/debugFramework/FamilyTreeApp.framework
```

### 3. Verify All Targets
```bash
./gradlew :core:build :ui:build
```
Should compile for all targets (Android, Desktop, iOS x3).

### 4. Run on iOS Simulator (via IntelliJ IDEA)
1. Install **Kotlin Multiplatform Mobile** plugin
2. Open Run Configuration
3. Select iOS Simulator
4. Run the app

## Additional Resources

- [Kotlin Multiplatform iOS Documentation](https://kotlinlang.org/docs/multiplatform-ios.html)
- [Compose Multiplatform Releases](https://github.com/JetBrains/compose-multiplatform/releases)
- [Kotlin/Native FAQ](https://kotlinlang.org/docs/native-faq.html)
- [iOS Troubleshooting (Official)](https://kotlinlang.org/docs/native-ios-troubleshooting.html)

## Reporting Issues

If none of these solutions work:

1. Capture full stack trace:
   ```bash
   ./gradlew projects --stacktrace > gradle_error.log 2>&1
   ```

2. Check environment:
   ```bash
   ./gradlew --version
   xcode-select -p
   xcrun --sdk iphoneos --show-sdk-version
   ```

3. Report to JetBrains:
   - [Compose Multiplatform Issue Tracker](https://github.com/JetBrains/compose-multiplatform/issues)
   - [Kotlin Issue Tracker](https://youtrack.jetbrains.com/issues/KT)

## Summary

iOS support is **architecturally ready** but requires resolving a Gradle/Kotlin/Native compatibility issue. The most likely solution is updating to Kotlin 2.1.21+ and Compose Multiplatform 1.7.1+, which have more mature iOS support.

The iOS module structure, entry points, and documentation are already in place and will work once the build configuration is resolved.
