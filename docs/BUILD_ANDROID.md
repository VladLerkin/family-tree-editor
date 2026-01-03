# Android (Compose Multiplatform): Build and Run

This guide explains how to build and run the Kotlin Multiplatform (KMP) Android application on a USB device or emulator.

## Prerequisites

- **JDK 25** (required for Gradle and Compose Multiplatform)
- **Android SDK 35** (Platform 35, Build-Tools 35.0.0, Platform-Tools)
- **Gradle 9.1.0** (included via wrapper: `./gradlew` or `gradlew.bat`)
- **Android device or emulator** (API level 35 recommended)

### Install Android SDK

If you have Android Studio installed, the SDK is typically located at:
- **macOS/Linux:** `~/Library/Android/sdk` or `~/Android/Sdk`
- **Windows:** `C:\Users\<YourName>\AppData\Local\Android\Sdk`

If Gradle cannot find the SDK automatically, create `local.properties` with one of the following:

**macOS:**
```properties
sdk.dir=/Users/<you>/Library/Android/sdk
```

**Linux:**
```properties
sdk.dir=/home/<you>/Android/Sdk
```

**Windows:**
```properties
sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
```

### Verify Android Device/Emulator

**USB Device:**
1. Enable **Developer Options** and **USB Debugging** on your Android device
2. Connect via USB
3. Verify connection:
   ```bash
   adb devices
   ```
   Should show your device with status `device`


**Emulator:**
1. Create an AVD (Android Virtual Device) in Android Studio or via `avdmanager`
2. Start the emulator:
   ```bash
   emulator -avd <AVD_Name>
   ```
   or launch from Android Studio (AVD Manager)

## Quick Start

### Build Debug APK

**macOS/Linux:**
```bash
# Navigate to project root (already there)
./gradlew :app-android:assembleDebug
```

**Windows:**
```powershell
# Navigate to project root (already there)
.\gradlew.bat :app-android:assembleDebug
```

The APK will be created at:
```
app-android/build/outputs/apk/debug/app-android-debug.apk
```

### Install on Device/Emulator

**macOS/Linux:**
```bash
# Navigate to project root (already there)
./gradlew :app-android:installDebug
```

**Windows:**
```powershell
# Navigate to project root (already there)
.\gradlew.bat :app-android:installDebug
```

This installs the app on the connected device or running emulator.

### Launch the App

After installation, launch the app:

**Using ADB:**
```bash
adb shell monkey -p com.family.tree.android -c android.intent.category.LAUNCHER 1
```

**Or manually:**
- Find "Family Tree" app icon on your device/emulator and tap it

**Or using Android Studio:**
- Open the project in Android Studio
- Select `app-android` run configuration
- Click the green ▶️ Run button

## Build, Install, and Run (One Command)

**macOS/Linux:**
```bash
# Navigate to project root (already there)
./gradlew :app-android:installDebug
adb shell am start -n com.family.tree.android/.MainActivity
```

**Windows:**
```powershell
# Navigate to project root (already there)
.\gradlew.bat :app-android:installDebug
adb shell am start -n com.family.tree.android/.MainActivity
```

## Uninstall Previous Version

If you need to uninstall the old version before installing a new one:

**macOS/Linux:**
```bash
adb uninstall com.family.tree.android || true
./gradlew :app-android:installDebug
```

**Windows:**
```powershell
adb uninstall com.family.tree.android
.\gradlew.bat :app-android:installDebug
```

## Run from Android Studio

1. Open the project directory in Android Studio
2. Sync Gradle dependencies (Android Studio usually prompts automatically)
3. Select **app-android** from the run configuration dropdown (top toolbar)
4. Ensure a device/emulator is connected/running
5. Click the green ▶️ Run button
6. Android Studio will build, install, and launch the app

## Android Features

- **Canvas Interaction:**
  - **Pan:** Drag on canvas with one finger
  - **Zoom:** Pinch to zoom in/out (planned), or use toolbar buttons `[-]` and `[+]`
  - **Selection:** Tap a person card to select

- **Editing:**
  - **Long-press** a person card to open context menu (Edit, Center On, Reset)
  - **Double-tap** is currently under development for quick edit

- **Screen Orientation:**
  - The app is locked to **landscape mode** for optimal family tree viewing

- **UI:**
  - **Fullscreen mode:** Status bar and navigation bar are hidden for immersive experience
  - **Top toolbar:** Three-dot menu (⋮) for File operations and settings (limited compared to Desktop)
  - **Left panel:** Individuals and Families lists
  - **Center:** Interactive canvas
  - **Right panel:** Properties inspector

- **File Operations (Planned):**
  - Open/Save .rel and JSON files via Android Storage Access Framework (SAF) — **not yet implemented**
  - Currently, file dialogs show placeholders

## Clean Build

If you encounter build issues:

**macOS/Linux:**
```bash
# Navigate to project root (already there)
./gradlew clean :app-android:assembleDebug
```

**Windows:**
```powershell
# Navigate to project root (already there)
.\gradlew.bat clean :app-android:assembleDebug
```

## View Logs (Debugging)

To view app logs in real-time:
```bash
adb logcat -s "FamilyTree"
```

Or view all logs:
```bash
adb logcat
```

In Android Studio, use the **Logcat** tab at the bottom.

## Troubleshooting

### "SDK location not found"
- Create `local.properties` as shown in Prerequisites
- Ensure the path points to your actual Android SDK directory

### "adb: command not found"
- Add Android SDK Platform-Tools to your PATH:
  - **macOS/Linux:** `export PATH=$PATH:~/Library/Android/sdk/platform-tools` (add to `~/.bashrc` or `~/.zshrc`)
  - **Windows:** Add `C:\Users\<you>\AppData\Local\Android\Sdk\platform-tools` to System Environment Variables → Path

### "no devices/emulators found"
- Ensure device is connected via USB with USB debugging enabled
- Or start an emulator before running Gradle install/run commands
- Verify with `adb devices`

### "Installation failed with message INSTALL_FAILED_UPDATE_INCOMPATIBLE"
- Uninstall the existing app first: `adb uninstall com.family.tree.android`
- Then reinstall: `./gradlew :app-android:installDebug`

### App crashes on launch
- Check Logcat for stack traces: `adb logcat`
- Ensure you're using a device/emulator with API level 35 or compatible
- Try a clean build: `./gradlew clean :app-android:assembleDebug :app-android:installDebug`

### "Unsupported class file major version 65"
- Ensure Gradle is using JDK 25: `./gradlew --version`
- Set `JAVA_HOME` to JDK 25 as shown in Prerequisites

### Build is very slow
- Enable Gradle daemon (should be on by default)
- Add `org.gradle.jvmargs=-Xmx4096m` to `~/.gradle/gradle.properties` (or `gradle.properties`) to allocate more memory
- Use `--parallel` flag: `./gradlew --parallel :app-android:assembleDebug`

## Packaging for Release (Future)

To create a signed release APK or AAB (Android App Bundle) for Google Play:

1. Create a keystore:
   ```bash
   keytool -genkey -v -keystore release.keystore -alias my_key -keyalg RSA -keysize 2048 -validity 10000
   ```

2. Configure signing in `app-android/build.gradle.kts` (signingConfigs block)

3. Build release:
   ```bash
   ./gradlew :app-android:assembleRelease
   ```

4. APK will be at `app-android/build/outputs/apk/release/app-android-release.apk`

For AAB (required for Google Play):
```bash
./gradlew :app-android:bundleRelease
```

Refer to [Android documentation](https://developer.android.com/studio/publish/app-signing) for detailed signing and publishing instructions.

## Notes

- The KMP Android app uses **Compose for Android 1.9.3**, **Kotlin 2.3.0**, and **AGP 8.13.2**
- **Target SDK:** 35, **Min SDK:** 24 (Android 7.0+)
- The app shares the `:core` and `:ui` modules with Desktop, providing cross-platform business logic and UI
- Android file pickers (SAF) are planned but not yet implemented; currently shows placeholder dialogs
- Fullscreen mode hides system bars for an immersive family tree viewing experience
