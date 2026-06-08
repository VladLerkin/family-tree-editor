# iOS Support Troubleshooting

This document addresses known issues, common compilation errors, and setup problems with iOS support in the Family Tree KMP project.

## Current Status

✅ **iOS support is fully implemented and building successfully.**

With the upgrade to **Kotlin 2.4.0**, **Compose Multiplatform 1.11.1**, and **Gradle 9.x / Java 25**, the previous build configuration issues (such as `DefaultArtifactPublicationSet` ClassNotFoundException) have been fully resolved. 

The iOS targets (`iosArm64`, `iosSimulatorArm64`) compile and link successfully:
```bash
./gradlew :app-ios:linkDebugFrameworkIosSimulatorArm64
```

---

## 1. Common KMP iOS Build Issues & Solutions

### A. Kotlin/Native Compiler Daemon / Download Issues

**Symptom:** Gradle build freezes or fails during compiler download/execution.

**Root Cause:** The first time an iOS target is built, Gradle downloads the Kotlin/Native toolchain. If this download is interrupted, it can leave the environment in a corrupt state.

**Solutions:**
1. **Force download toolchain:**
   ```bash
   ./gradlew --stop
   ./gradlew :core:downloadKotlinNativeDistribution
   ```
2. **Disable Kotlin Native Compiler Daemon temporarily (if it freezes):**
   Ensure this property is present in `gradle.properties`:
   ```properties
   kotlin.native.disableCompilerDaemon=true
   ```
3. **Clear Konan Cache (Nuclear Option):**
   If the toolchain is corrupted:
   ```bash
   rm -rf ~/.konan/
   ./gradlew --stop
   ./gradlew :app-ios:linkDebugFrameworkIosSimulatorArm64
   ```

---

### B. Simulator Architecture Mismatch

**Symptom:** Xcode build fails with errors like:
`building for iOS Simulator-x86_64 but attempting to link with file built for iOS Simulator-arm64` or vice versa.

**Root Cause:**
- **Apple Silicon Mac (M1/M2/M3):** Simulators run natively on `arm64`.
- **Intel Mac:** Simulators run on `x86_64`.
- You built the debug framework for one architecture (e.g., `iosSimulatorArm64`), but Xcode is trying to build for another.

**Solutions:**
1. Verify your machine architecture:
   ```bash
   uname -m
   ```
2. Match the Gradle build target:
   - For **Apple Silicon (M1/M2/M3)**:
     ```bash
     ./gradlew :app-ios:linkDebugFrameworkIosSimulatorArm64
     ```
   - For **Intel Mac**:
     ```bash
     ./gradlew :app-ios:linkDebugFrameworkIosX64
     ```
3. Verify Xcode is running natively (not via Rosetta) if on Apple Silicon.

---

### C. "No such module 'FamilyTreeApp'" in Xcode Swift Code

**Symptom:** Xcode fails compiling SwiftUI/Swift code with: `No such module 'FamilyTreeApp'`.

**Solutions:**
1. **Build the framework first:**
   Ensure the framework has been compiled by Gradle.
   - Run `./gradlew :app-ios:linkDebugFrameworkIosSimulatorArm64`
2. **Verify Framework Search Paths in Xcode:**
   - Go to your Xcode target settings -> **Build Settings** -> search for **Framework Search Paths**.
   - Ensure the path points to the correct build output:
     `$(PROJECT_DIR)/../app-ios/build/bin/iosSimulatorArm64/debugFramework`
   - Make sure it is set to **recursive**.
3. **Verify General Framework Linkage:**
   - In Xcode target settings -> **General** tab -> scroll to **Frameworks, Libraries, and Embedded Content**.
   - Make sure `FamilyTreeApp.framework` is added and marked as **Embed & Sign** (or **Embed Without Signing**).

---

### D. Xcode Command Line Tools / Simulator Not Found

**Symptom:** Error running `./gradlew projects` or other tasks suggesting Xcode is not configured, or no simulators are available.

**Solutions:**
1. Ensure Command Line Tools are active:
   ```bash
   xcode-select -p
   ```
   If it returns a path that is not `/Applications/Xcode.app/...`, reset it:
   ```bash
   sudo xcode-select --switch /Applications/Xcode.app/Contents/Developer
   ```
2. Check available simulators:
   ```bash
   xcrun simctl list devices available
   ```
   If no devices are listed, open Xcode, go to `Settings > Platforms`, and download the latest iOS simulator platform.

---

## 2. Resolved Historical Issues

### `ClassNotFoundException: org.gradle.api.internal.plugins.DefaultArtifactPublicationSet`

**Problem:** When configuring iOS targets, the build crashed during the configuration phase on older Gradle/Kotlin combinations.
**Status:** **Resolved.** Fixed by upgrading Gradle to 9.5.x, Kotlin to 2.4.x, and Compose Multiplatform to 1.11.x.

---

## 3. Verification & Validation Steps

To verify your iOS build environment is fully functional:

1. **Gradle Clean and Configuration:**
   ```bash
   ./gradlew clean projects
   ```
2. **Compile and Link Framework:**
   ```bash
   ./gradlew :app-ios:linkDebugFrameworkIosSimulatorArm64
   ```
3. **Verify Output Path:**
   Check that `FamilyTreeApp.framework` is generated under:
   `app-ios/build/bin/iosSimulatorArm64/debugFramework/FamilyTreeApp.framework`

---

## Additional Resources

- [Building and Running the iOS App (docs/BUILD_IOS.md)](BUILD_IOS.md)
- [Xcode Setup Reference (docs/XCODE_SETUP.md)](XCODE_SETUP.md)
- [Xcode Code Signing Guide (docs/XCODE_CODE_SIGNING.md)](XCODE_CODE_SIGNING.md)
- [Official Kotlin Multiplatform iOS Documentation](https://kotlinlang.org/docs/multiplatform-ios.html)
