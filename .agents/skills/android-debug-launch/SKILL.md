---
name: android-debug-launch
description: Instructions for correctly launching Android debug builds via ADB by checking for applicationIdSuffix.
---

# Launching Android Debug Builds

When asked to launch or debug an Android application via CLI (using `adb shell am start ...`), you **must always** check the `build.gradle` or `build.gradle.kts` file for an `applicationIdSuffix` before running the command.

Debug builds frequently append a suffix (such as `.debug`) to the application ID. If you launch the base `applicationId` without this suffix, you will likely launch an older release version already installed on the device, rather than the newly built debug version, leading to confusion.

### Workflow
1. Check the `build.gradle(.kts)` file of the Android application module.
2. Verify the base `applicationId` in the `defaultConfig` block.
3. Check the `buildTypes { debug { ... } }` block for any `applicationIdSuffix` (e.g., `.debug`).
4. Construct the correct package name by concatenating the base ID and the suffix.
5. Launch the app using the correct component name (e.g., `adb shell am start -n com.example.app.debug/com.example.app.MainActivity`).
