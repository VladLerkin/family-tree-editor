# Family Tree KMP (Kotlin Multiplatform + Compose)

This directory contains the Kotlin Multiplatform migration branch of the app. It coexists with the legacy Maven/JavaFX project in the repo root.

## Tech matrix
- Gradle: 8.10 (via wrapper)  
- Kotlin: 2.1.0  
- Compose Multiplatform: 1.7.1  
- Android Gradle Plugin (AGP): 8.7.3  
- JDK for Gradle: 21  
- Android JVM target: 17

## Modules
- :core — shared domain, layout, serialization (`kotlinx.serialization` JSON)
- :ui — shared UI (Compose MPP), desktop/Android/iOS specifics via expect/actual
- :app-desktop — Compose Desktop launcher
- :app-android — Android launcher (Compose)
- :app-ios — iOS launcher (Compose Multiplatform for iOS)

## How to run

### Desktop
```
cd kmp
./gradlew :app-desktop:run
```
Expected: Window with lists (Individuals/Families), center canvas (tree), inspector (right).

**For detailed instructions:** See [docs/BUILD_DESKTOP.md](docs/BUILD_DESKTOP.md)

### Android
```
cd kmp
./gradlew :app-android:installDebug
adb shell monkey -p com.family.tree.android -c android.intent.category.LAUNCHER 1
```
Expected: App launches on connected device/emulator in landscape mode.

**For detailed instructions:** See [docs/BUILD_ANDROID.md](docs/BUILD_ANDROID.md)

### iOS
**Note:** iOS support is structurally complete but currently requires resolving a build configuration issue. See [docs/iOS_TROUBLESHOOTING.md](docs/iOS_TROUBLESHOOTING.md) for details and solutions.

```
cd kmp
./gradlew :app-ios:linkDebugFrameworkIosSimulatorArm64
```
Expected: Framework built for iOS Simulator on Apple Silicon. Use IntelliJ IDEA with Kotlin Multiplatform Mobile plugin or create Xcode wrapper to run.

**For detailed instructions:** See [docs/BUILD_IOS.md](docs/BUILD_IOS.md)  
**Troubleshooting:** See [docs/iOS_TROUBLESHOOTING.md](docs/iOS_TROUBLESHOOTING.md)

### Desktop gestures and shortcuts
- Zoom: mouse wheel / trackpad scroll (zoom under cursor)
  - Tip: hold Cmd/Ctrl to accelerate wheel zoom
- Pan: drag on canvas
- Toolbar: [-] and [+] buttons for zoom, Reset to fit-to-view
- File: Open JSON, Save JSON (Compose/AWT dialogs)
- Keyboard:
  - Esc — clear selection
  - + / = / NumPad + — zoom in (animated)
  - - / NumPad - — zoom out (animated)

## How to build (Android)
Requirements: Android SDK 35, Build-Tools 35.0.0, Platform-Tools.

```
cd kmp
./gradlew :app-android:assembleDebug
# Install & run on emulator/device (optional)
adb uninstall com.family.tree.android || true
./gradlew :app-android:installDebug
adb shell monkey -p com.family.tree.android -c android.intent.category.LAUNCHER 1
```

Notes:
- Android file dialogs are not implemented yet (placeholders). Desktop has Open/Save JSON.
- Wheel/keyboard zoom are desktop-only. Android supports drag pan and toolbar +/-.

## Project state
- Shared models & simple layout (`core`) migrated.
- UI skeleton with canvas rendering, selection, pan/zoom, and right-side inspector.
- JSON persistence in `core` (DTOs + encode/decode) for simple Open/Save.
- Desktop: native file dialogs working. Android: SAF integration planned.

## Troubleshooting
- Use JDK 21 for Gradle (wrapper). If system JAVA_HOME points to another JDK, set Gradle JVM in IDE to 21 or `export JAVA_HOME=$(/usr/libexec/java_home -v 21)`.
- If Android assemble fails with SDK location not found, create `kmp/local.properties`:
  ```
  sdk.dir=/Users/<you>/Library/Android/sdk
  ```
- If Gradle reports missing standard Android source dirs in :ui, it is informational (KMP uses `commonMain` / `androidMain` / `desktopMain`).
- If build artifacts (build/, .gradle/) show up in `git status`, run once from repo root:
  ```
  git rm -r --cached kmp/**/build kmp/**/.gradle kmp/**/build/intermediates
  git add kmp/.gitignore
  git commit -m "kmp: untrack build outputs; honor .gitignore"
  ```

## Next steps (migration plan)
- Re-enable Desktop keyboard shortcuts (Esc, +/- and Cmd/Ctrl + +/-) with Compose 1.7 compatible APIs.
- Android SAF-based Open/Save wiring via expect/actual and Activity Result API.
- Expand inspector details and polish list/selection (optional animations).
- Prepare basic CI job: `./gradlew :core:build :ui:build :app-desktop:build`.
