# Building and Running the iOS App

This guide explains how to build and run the Family Tree iOS application on macOS using iOS Simulator.

**Note:** This project is configured for simulator-only builds. Device builds are not supported without modifying the Xcode project configuration.

## Prerequisites

### Required Software
1. **macOS** (required for iOS development)
2. **Xcode** (latest stable version, 15.x or 16.x recommended)
   - Download from App Store or install command line tools:
     ```bash
     xcode-select --install
     ```
3. **JDK 21** (already configured in the project)
4. **Kotlin Multiplatform Mobile plugin** for IntelliJ IDEA (optional but recommended)

### Verify Xcode Installation
```bash
# Check if Xcode is installed
xcode-select -p

# List available iOS simulators
xcrun simctl list devices
```

## Important: Simulator-Only Configuration

**This project is configured for iOS Simulator builds only.** The Xcode project has been set up with `SDKROOT=iphonesimulator`, which restricts builds to iOS Simulator and prevents building for physical devices.

### Code Signing Setup

The project is pre-configured with `TEAM_ID=SIMULATOR` in `iosApp/Configuration/Config.xcconfig`, which allows building for the iOS Simulator without an Apple Developer account.

**No additional configuration needed** - you can proceed to building for simulator!

### If You Need Device Support

To enable building for physical iOS devices, you would need to:
1. Change `SDKROOT` from `iphonesimulator` to `iphoneos` in the Xcode project file
2. Configure a valid Team ID in `iosApp/Configuration/Config.xcconfig`
3. Set up proper code signing certificates and provisioning profiles

**For detailed code signing instructions, see [XCODE_CODE_SIGNING.md](XCODE_CODE_SIGNING.md)**

## Project Structure

The iOS app is organized as follows:
```
Project Root:
├── app-ios/              # iOS launcher module
│   └── src/
│       └── iosMain/
│           └── kotlin/
│               └── com/family/tree/ios/
│                   └── main.kt    # iOS entry point
├── core/                 # Shared business logic (supports iOS)
└── ui/                   # Shared Compose UI (supports iOS)
```

## Supported iOS Targets

The project is configured for **iOS Simulator only** and supports two simulator targets:
- **iosX64** — iOS Simulator on Intel Mac
- **iosSimulatorArm64** — iOS Simulator on Apple Silicon (M1/M2/M3)

**Note:** The iosArm64 target (for real iOS devices) is defined in the Kotlin build configuration but the Xcode project is restricted to simulator builds only (`SDKROOT=iphonesimulator`).

## iOS Configuration in build.gradle.kts

### Important: No Separate `ios {}` Block Required

Unlike Android development, **iOS targets in Kotlin Multiplatform do NOT require a separate configuration block** like `android {}`. This is a common source of confusion.

#### Why Android Has an `android {}` Block

In modules like `core/build.gradle.kts` and `ui/build.gradle.kts`, you'll see:

```kotlin
android {
    namespace = "com.family.tree.core"
    compileSdk = (project.findProperty("android.compileSdk") as String).toInt()
    defaultConfig {
        minSdk = (project.findProperty("android.minSdk") as String).toInt()
        targetSdk = (project.findProperty("android.targetSdk") as String).toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
```

This `android {}` block is required because:
- Android uses the **Android Gradle Plugin** (`com.android.library`)
- The plugin requires Android-specific settings (SDK versions, Java compatibility, etc.)

#### Why iOS Doesn't Have an `ios {}` Block

**iOS configuration is done entirely within the `kotlin {}` block:**

```kotlin
kotlin {
    androidTarget()
    jvm("desktop")
    
    // iOS targets - this IS the iOS configuration!
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    
    sourceSets {
        commonMain.dependencies {
            // dependencies
        }
    }
}
```

The iOS targets (`iosX64()`, `iosArm64()`, `iosSimulatorArm64()`) **are already the full iOS configuration**. There is no separate `ios {}` block because:
- iOS doesn't use a separate Gradle plugin like Android
- iOS compilation is handled by the Kotlin Multiplatform plugin itself
- iOS-specific settings (if needed) are configured in the framework binary definition

#### Why This Project Uses Multi-Module Architecture

**Important:** This project differs from the standard Kotlin Multiplatform template structure.

**Standard KMP Template (Single "shared" Module):**
```kotlin
// In a typical KMP project, you'll see:
// shared/build.gradle.kts
kotlin {
    androidTarget()
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"  // Framework name in one place
            isStatic = true
        }
    }
    
    sourceSets {
        commonMain.dependencies {
            // All dependencies here
        }
    }
}
```

In standard templates, **iOS targets and framework configuration are in the same module**.

**This Project (Multi-Module Architecture):**
```
Project Root:
├── core/build.gradle.kts      # Declares iosX64(), iosArm64(), iosSimulatorArm64()
├── ui/build.gradle.kts        # Declares iosX64(), iosArm64(), iosSimulatorArm64()  
└── app-ios/build.gradle.kts   # Framework configuration with baseName = "FamilyTreeApp"
```

**Why the difference?**
- **Separation of concerns:** `core` (business logic) and `ui` (Compose UI) are reusable libraries
- **Each module declares iOS targets** so they can be compiled for iOS
- **Only `app-ios` creates the framework** that bundles everything for Xcode
- **Better scalability:** Large projects benefit from multiple modules

**Both approaches are valid:**
- ✅ Single "shared" module — simpler, good for small projects (standard template)
- ✅ Multi-module — more organized, better for medium/large projects (this project)

**Key takeaway:** The `baseName = "FamilyTreeApp"` in `app-ios/build.gradle.kts` determines the framework name you import in Swift/Xcode, regardless of how many modules exist.

#### Framework Configuration (in app-ios module)

For the iOS application module (`app-ios/build.gradle.kts`), you configure the framework binary:

```kotlin
kotlin {
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "FamilyTreeApp"
            isStatic = true
        }
    }
}
```

This configures the iOS framework that will be embedded in your Xcode project.

### Summary

✅ **iOS is already configured** if you see iOS targets in your `kotlin {}` block  
✅ **No additional `ios {}` block is needed** - it doesn't exist in Kotlin Multiplatform  
✅ **The `android {}` block is Android-specific** and has no iOS equivalent  
✅ **Run configurations appear in IntelliJ IDEA** after installing the KMM plugin (see below)

## Building the iOS Framework

### Build for Simulator (Apple Silicon)
```bash
# Navigate to project root (already there)
./gradlew :app-ios:linkDebugFrameworkIosSimulatorArm64
```

### Build for Simulator (Intel Mac)
```bash
# Navigate to project root (already there)
./gradlew :app-ios:linkDebugFrameworkIosX64
```

**Note:** Device builds (iosArm64) are not supported with the current simulator-only Xcode configuration.

## Running on iOS Simulator

### Method 1: From IntelliJ IDEA (Recommended)

This is the easiest method if you have IntelliJ IDEA (or Android Studio) with the Kotlin Multiplatform Mobile plugin.

#### Prerequisites

Before you can run iOS apps from IntelliJ IDEA, you need:
- **IntelliJ IDEA Ultimate** (Community Edition doesn't support iOS development) or **Android Studio**
- **Kotlin Multiplatform Mobile (KMM) plugin** installed
- **Xcode** installed on your Mac
- **macOS** operating system (iOS development requires macOS)

#### Step 1: Install Kotlin Multiplatform Mobile Plugin

1. **Open IntelliJ IDEA**
2. **Open Plugin Settings:**
   - Go to **IntelliJ IDEA > Settings** (or **Preferences** on older versions)
   - Or use shortcut: **⌘,** (Cmd+Comma) on Mac
3. **Navigate to Plugins:**
   - In the left sidebar, click **Plugins**
4. **Search for the plugin:**
   - Click the **Marketplace** tab
   - In the search box, type: `Kotlin Multiplatform Mobile`
   - Or search for: `KMM` or `Kotlin Multiplatform`
5. **Install the plugin:**
   - Find **"Kotlin Multiplatform Mobile"** by JetBrains
   - Click **Install**
   - Click **OK** or **Apply**
   - **Restart IntelliJ IDEA** when prompted

#### Step 2: Open the Project

1. **Open your KMP project** in IntelliJ IDEA
2. **Wait for indexing to complete** (status bar at bottom)
3. **Sync Gradle:**
   - Click the **Gradle** tab on the right side
   - Click the **Reload All Gradle Projects** button (circular arrows icon)
   - Wait for sync to complete

#### Step 3: Create iOS Run Configuration

1. **Open Run Configuration menu:**
   - **Method A:** Go to **Run > Edit Configurations...** from the top menu
   - **Method B:** Click the run configuration dropdown (next to the Run button ▶️) and select **Edit Configurations...**
   - **Method C:** Use shortcut: **⌃⌥R** (Control+Option+R) on Mac, then select **Edit Configurations**

2. **Add a new iOS configuration:**
   - In the "Run/Debug Configurations" dialog, click the **+** button (top-left corner)
   - Scroll down and look for one of these options (depending on your IDE and plugin version):
     - **iOS Application** (older KMM plugin versions)
     - **Kotlin Mobile** (newer plugin versions - this is what you want for running iOS apps)
     - **iOS Run Configuration**
     - **Kotlin Multiplatform > iOS Application**
   
   **Which option to choose:**
   - ✅ **Kotlin Mobile** — Use this to run your iOS app (supports both simulator and device)
   - ❌ **Kotlin Mobile Test** — This is for running iOS unit tests only, NOT for running your app
   
   **If you don't see any iOS/Kotlin Mobile options:**
   - Verify the KMM plugin is installed and enabled (Settings > Plugins)
   - Restart IntelliJ IDEA after installing the plugin
   - Check that you're using IntelliJ IDEA Ultimate (not Community Edition)
   - Ensure your project has `app-ios` module properly configured
   - Try **File > Invalidate Caches** and restart

3. **Configure the iOS run configuration:**
   - **Name:** Enter a name like `iOS App` or `FamilyTree iOS`
   - **Execute target:** Select **app-ios** from the dropdown
     
     **If app-ios is not available in the dropdown:**
     - **Solution 1:** Sync Gradle project:
       - Click the **Gradle** tab on the right side of IntelliJ IDEA
       - Click the **Reload All Gradle Projects** button (circular arrows icon 🔄)
       - Wait for sync to complete, then check the dropdown again
     - **Solution 2:** Verify app-ios module is included:
       - Open `settings.gradle.kts` in your project root
       - Ensure it contains: `include(":app-ios")`
       - If missing or commented out, uncomment or add it, then sync Gradle
     - **Solution 3:** Check iOS targets are enabled:
       - Open `core/build.gradle.kts` and `ui/build.gradle.kts`
       - Verify iOS targets are not commented out (look for `iosX64()`, `iosArm64()`, `iosSimulatorArm64()`)
       - If commented out, you may need to enable them (see iOS_TROUBLESHOOTING.md)
     - **Solution 4:** Invalidate caches and restart:
       - Go to **File > Invalidate Caches...**
       - Check **Invalidate and Restart**
       - Click **Invalidate and Restart** button
       - After restart, sync Gradle again
     - **Solution 5:** Close and reopen the project:
       - **File > Close Project**
       - Reopen the project from the welcome screen
       - Wait for indexing and Gradle sync to complete
     - **Solution 6:** Check project structure:
       - Go to **File > Project Structure** (or ⌘;)
       - Under **Modules**, verify that `app-ios` module is listed
       - If not listed, reimport the project: **File > New > Project from Existing Sources**
   
   - **Configuration:** Select **Debug** (or Release)
   - **Target:** Select your Mac's simulator architecture:
     - **iosSimulatorArm64** for Apple Silicon Mac (M1/M2/M3)
     - **iosX64** for Intel Mac
   - **Device:** Choose an iOS Simulator from the dropdown list
     - Example: "iPhone 15 Pro", "iPhone 14", "iPad Pro"
     - If no simulators appear, you may need to install them via Xcode
   - **Xcode project:** Leave empty (or auto-detected)

4. **Apply and save:**
   - Click **Apply**
   - Click **OK**

#### Step 4: Build and Run

1. **Select your iOS configuration:**
   - In the toolbar, make sure your new iOS run configuration is selected (dropdown next to Run button)

2. **Build the framework:**
   - IntelliJ IDEA will automatically build the iOS framework before running
   - Or manually build: **Build > Build Project**

3. **Run the app:**
   - Click the **Run** button (▶️ green play icon)
   - Or use keyboard shortcut: **⌃R** (Control+R) on Mac
   - Or go to **Run > Run 'iOS App'**

4. **Wait for the simulator:**
   - IntelliJ IDEA will launch Xcode Simulator
   - The app will be installed and launched automatically
   - This may take 30-60 seconds on first run

#### Step 5: Verify the App is Running

- The iOS Simulator window should open
- Your app should launch and display the Family Tree UI
- Console output appears in IntelliJ IDEA's Run panel
- You can interact with the app in the simulator

#### Troubleshooting Method 1

**"iOS Application" option not available in Run Configurations:**
- Solution 1: Verify KMM plugin is installed: **Settings > Plugins > Installed**, look for "Kotlin Multiplatform Mobile"
- Solution 2: Restart IntelliJ IDEA after plugin installation
- Solution 3: Check you're using IntelliJ IDEA Ultimate (Community Edition doesn't support iOS development)
- Solution 4: Try Android Studio instead of IntelliJ IDEA Community
- Solution 5: Update IntelliJ IDEA to the latest version
- Solution 6: Invalidate caches: **File > Invalidate Caches > Invalidate and Restart**

**No simulators available in Device dropdown:**
- Open Xcode and go to **Window > Devices and Simulators**
- Click the **Simulators** tab
- Click **+** to add a new simulator
- Choose device type (e.g., iPhone 15 Pro) and iOS version
- Restart IntelliJ IDEA
- See "Error: No simulators available" section below for detailed instructions

**"Command PhaseScriptExecution failed" or framework not found:**
- Manually build the framework first:
  ```bash
  ./gradlew :app-ios:linkDebugFrameworkIosSimulatorArm64
  ```
- In IntelliJ, **Build > Rebuild Project**
- Try running again

**Simulator launches but app doesn't appear:**
- Check IntelliJ IDEA console for error messages
- Verify `MainViewController()` is correctly implemented in `app-ios/src/iosMain/kotlin/com/family/tree/ios/main.kt`
- Try cleaning build: **Build > Clean Project**, then rebuild

**"Xcode not found" error:**
- Install Xcode from the App Store
- Run: `sudo xcode-select --switch /Applications/Xcode.app/Contents/Developer`
- Verify: `xcode-select -p` should output a valid path

**Architecture mismatch errors:**
- Ensure selected target matches your Mac:
  - Apple Silicon (M1/M2/M3) → use **iosSimulatorArm64**
  - Intel Mac → use **iosX64**
- Rebuild the framework for the correct architecture

**Alternative: Use Method 2 or Method 3 below if IntelliJ IDEA method doesn't work**

### Method 2: Using Xcode (Detailed Step-by-Step)

This method requires creating an Xcode wrapper project to package and run your KMP app.

#### Step 1: Build the Kotlin Framework

First, build the iOS framework from your KMP project:

```bash
# Navigate to project root (already there)

# For Apple Silicon Mac (M1/M2/M3) - iOS Simulator
./gradlew :app-ios:linkDebugFrameworkIosSimulatorArm64

# For Intel Mac - iOS Simulator
./gradlew :app-ios:linkDebugFrameworkIosX64

# For Real iOS Device
./gradlew :app-ios:linkDebugFrameworkIosArm64
```

The framework will be generated at:
- Simulator (Apple Silicon): `app-ios/build/bin/iosSimulatorArm64/debugFramework/FamilyTreeApp.framework`
- Simulator (Intel): `app-ios/build/bin/iosX64/debugFramework/FamilyTreeApp.framework`
- Device: `app-ios/build/bin/iosArm64/debugFramework/FamilyTreeApp.framework`

#### Step 2: Create a New Xcode Project

1. **Open Xcode**
2. **Create a new project:**
   - Click **File** → **New** → **Project** (or press ⇧⌘N)
   - Select **iOS** → **App**
   - Click **Next**
3. **Configure the project:**
   - **Product Name:** `FamilyTreeWrapper` (or any name you prefer)
   - **Team:** Select your development team (or leave as "None" for simulator-only)
   - **Organization Identifier:** `com.family.tree` (or your own)
   - **Interface:** Select **SwiftUI**
   - **Language:** Select **Swift**
   - **Storage:** Core Data (unchecked), CloudKit (unchecked), Testing (optional)
   - Click **Next**
4. **Choose location:**
   - Save it somewhere convenient (e.g., `ios-wrapper/`)
   - **Do NOT** add to the KMP project git repository (uncheck "Create Git repository")
   - Click **Create**

#### Step 3: Link the Kotlin Framework

1. **Open your Xcode project** (if not already open)

2. **Add the framework to your project:**
   - In Xcode, select your project in the Project Navigator (left sidebar)
   - Select the **app target** (not the project itself)
   - Go to the **General** tab
   - Scroll down to **Frameworks, Libraries, and Embedded Content** section
   - Click the **+** button
   - Click **Add Other...** → **Add Files...**
   - Navigate to the framework location:
     - For Apple Silicon Simulator: `app-ios/build/bin/iosSimulatorArm64/debugFramework/`
     - For Intel Simulator: `app-ios/build/bin/iosX64/debugFramework/`
   - Select `FamilyTreeApp.framework`
   - Ensure **Embed & Sign** is selected (or **Embed Without Signing** for simulator)
   - Click **Add**

3. **Configure Framework Search Paths:**
   - Still in your target settings, go to **Build Settings** tab
   - Search for "Framework Search Paths"
   - Double-click on **Framework Search Paths**
   - Add the path to your framework directory:
     - For Apple Silicon: `$(PROJECT_DIR)/../../app-ios/build/bin/iosSimulatorArm64/debugFramework`
     - For Intel: `$(PROJECT_DIR)/../../app-ios/build/bin/iosX64/debugFramework`
   - Make sure it's set to **recursive** (checkbox on the right)

4. **Disable Bitcode (if needed):**
   - In **Build Settings**, search for "Bitcode"
   - Set **Enable Bitcode** to **No**
   - (Kotlin/Native frameworks don't support Bitcode)

#### Step 4: Write Swift Wrapper Code

1. **Open the main app file:**
   - In Xcode Project Navigator, find `FamilyTreeWrapperApp.swift` (or `YourProjectNameApp.swift`)
   - This file contains the `@main` struct

2. **Replace its contents with:**
   ```swift
   import SwiftUI
   import FamilyTreeApp  // Import your Kotlin framework

   @main
   struct FamilyTreeWrapperApp: App {
       var body: some Scene {
           WindowGroup {
               ContentView()
           }
       }
   }
   ```

3. **Open ContentView.swift** and replace with:
   ```swift
   import SwiftUI
   import UIKit
   import FamilyTreeApp  // Import your Kotlin framework

   struct ContentView: UIViewControllerRepresentable {
       func makeUIViewController(context: Context) -> UIViewController {
           // Call the Kotlin Multiplatform entry point
           return MainKt.MainViewController()
       }
       
       func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
           // No updates needed
       }
   }

   struct ContentView_Previews: PreviewProvider {
       static var previews: some View {
           ContentView()
       }
   }
   ```

#### Step 5: Configure Simulator Target

1. **Select a simulator:**
   - At the top of Xcode, click on the device dropdown (next to your scheme name)
   - Select any iOS Simulator (e.g., "iPhone 15 Pro")
   - Make sure it matches your framework architecture:
     - Apple Silicon Mac → Use any simulator (they run on arm64)
     - Intel Mac → Ensure you built for iosX64

2. **Verify deployment target:**
   - In Project settings → **General** tab
   - Check **Minimum Deployments** → should be iOS 14.0 or higher
   - (Compose Multiplatform requires iOS 14+)

#### Step 6: Build and Run

1. **Build the project:**
   - Click **Product** → **Build** (or press ⌘B)
   - Fix any compilation errors if they appear

2. **Run on simulator:**
   - Click the **Run** button (▶️) or press ⌘R
   - Xcode will launch the iOS Simulator and install your app
   - Your Kotlin Multiplatform app should now appear!

#### Step 7: Verify the App is Running

- The app should launch and display your Family Tree UI
- Check Xcode console for any Kotlin `println()` output
- You can interact with the app in the simulator

#### Troubleshooting Xcode Wrapper

**Error: "No such module 'FamilyTreeApp'"**

This error occurs when Xcode cannot find or import your Kotlin Multiplatform framework. Follow these steps in order:

**Step 1: Verify the Framework Exists**
```bash
# Check if the framework was built successfully
ls -la app-ios/build/bin/iosSimulatorArm64/debugFramework/FamilyTreeApp.framework
# or for Intel Mac:
ls -la app-ios/build/bin/iosX64/debugFramework/FamilyTreeApp.framework
```

If the framework doesn't exist, rebuild it:
```bash
# Navigate to project root (already there)
# For Apple Silicon Mac:
./gradlew :app-ios:linkDebugFrameworkIosSimulatorArm64
# For Intel Mac:
./gradlew :app-ios:linkDebugFrameworkIosX64
```

**Step 2: Verify Framework is Added to Xcode Project**

1. Open your Xcode wrapper project
2. Select your project in the Project Navigator (left sidebar)
3. Select your **app target** (not the project itself)
4. Go to the **General** tab
5. Scroll to **Frameworks, Libraries, and Embedded Content**
6. Check if `FamilyTreeApp.framework` is listed
7. If not listed, click **+** and add it (see Step 3 in Method 2)
8. Make sure it's set to **Embed & Sign** or **Embed Without Signing**

**Step 3: Verify Framework Search Paths**

1. In your target settings, go to **Build Settings** tab
2. Search for "Framework Search Paths"
3. Verify it includes the correct path:
   - For Apple Silicon: `$(PROJECT_DIR)/../../app-ios/build/bin/iosSimulatorArm64/debugFramework`
   - For Intel: `$(PROJECT_DIR)/../../app-ios/build/bin/iosX64/debugFramework`
4. Or use absolute path if relative doesn't work:
   - `/Users/YOUR_USERNAME/path/to/app-ios/build/bin/iosSimulatorArm64/debugFramework`
5. Ensure the path is set to **recursive** (checkbox on the right)

**Step 4: Check Build Phases**

1. In your target settings, go to **Build Phases** tab
2. Expand **Link Binary With Libraries**
3. Verify `FamilyTreeApp.framework` is listed
4. If it shows in red or has a warning icon, remove it and re-add it
5. Expand **Embed Frameworks** (if it exists)
6. Verify `FamilyTreeApp.framework` is listed there too

**Step 5: Verify Import Statement**

In your Swift files, make sure you're using the correct import:
```swift
import FamilyTreeApp  // Correct - matches baseName in build.gradle.kts
```

NOT:
```swift
import app_ios     // Wrong - old name
import AppIos      // Wrong - should use FamilyTreeApp
import FamilyTree  // Wrong - incomplete name
```

**Step 6: Clean and Rebuild Xcode Project**

1. In Xcode, go to **Product** → **Clean Build Folder** (⇧⌘K)
2. Close Xcode completely
3. Delete Xcode's derived data:
   ```bash
   rm -rf ~/Library/Developer/Xcode/DerivedData/*
   ```
4. Reopen your Xcode project
5. Build again (⌘B)

**Step 7: Verify Architecture Match**

Make sure your simulator and framework architectures match:
- **Apple Silicon Mac** → Build for `iosSimulatorArm64` → Run on any simulator
- **Intel Mac** → Build for `iosX64` → Run on any simulator

Check your Mac architecture:
```bash
uname -m
# arm64 = Apple Silicon
# x86_64 = Intel
```

**Step 8: Check for Module Map (Advanced)**

If the framework exists but still not recognized:
```bash
# Verify the framework has a module map
ls -la app-ios/build/bin/iosSimulatorArm64/debugFramework/FamilyTreeApp.framework/Modules/module.modulemap
```

If the file exists, the framework is properly structured.

**Step 9: Try Absolute Import (Workaround)**

If all else fails, try adding the framework using absolute path:
1. Remove `FamilyTreeApp.framework` from your project
2. In Xcode, **File** → **Add Files to "ProjectName"...**
3. Navigate to the absolute path of the framework
4. Select the framework and check "Copy items if needed"
5. Click **Add**

**Error: "Framework not found FamilyTreeApp"**
- Check that framework exists at the specified path
- Rebuild the framework: `./gradlew :app-ios:linkDebugFrameworkIosSimulatorArm64`
- Verify Framework Search Paths are correct

**App crashes immediately on launch**
- Check Xcode console for error messages
- Ensure you're using the correct framework for your architecture
- Verify `MainKt.MainViewController()` is correctly called

**"Symbol not found" or linking errors**
- Ensure **Embed & Sign** (or **Embed Without Signing**) is selected for the framework
- Clean and rebuild the Xcode project

**Framework architecture mismatch**
- Apple Silicon Mac + Simulator → use `iosSimulatorArm64` framework
- Intel Mac + Simulator → use `iosX64` framework
- Real Device → use `iosArm64` framework and configure code signing

### Method 3: Command Line with Simulator (Advanced)

This method allows you to build and install your app from the command line after setting up an Xcode wrapper project (see Method 2).

#### Prerequisites

You must first complete **Method 2** to create the Xcode wrapper project. This method builds on that setup.

#### Step-by-Step Command Line Workflow

**Step 1: Build the Kotlin Framework**

```bash
# Navigate to project root (already there)

# For Apple Silicon Mac - iOS Simulator
./gradlew :app-ios:linkDebugFrameworkIosSimulatorArm64

# For Intel Mac - iOS Simulator
./gradlew :app-ios:linkDebugFrameworkIosX64
```

**Step 2: Build the Xcode Wrapper Project**

```bash
# Navigate to your Xcode wrapper project directory
cd ios-wrapper  # or wherever you created the Xcode project

# Build the app using xcodebuild
xcodebuild \
  -project FamilyTreeWrapper.xcodeproj \
  -scheme FamilyTreeWrapper \
  -configuration Debug \
  -sdk iphonesimulator \
  -derivedDataPath ./build

# The .app bundle will be created at:
# ./build/Build/Products/Debug-iphonesimulator/FamilyTreeWrapper.app
```

**Alternative: Build for specific architecture**

```bash
# For Apple Silicon simulator
xcodebuild \
  -project FamilyTreeWrapper.xcodeproj \
  -scheme FamilyTreeWrapper \
  -configuration Debug \
  -sdk iphonesimulator \
  -arch arm64 \
  -derivedDataPath ./build

# For Intel simulator
xcodebuild \
  -project FamilyTreeWrapper.xcodeproj \
  -scheme FamilyTreeWrapper \
  -configuration Debug \
  -sdk iphonesimulator \
  -arch x86_64 \
  -derivedDataPath ./build
```

**Step 3: List Available Simulators**

```bash
# List all available simulators with their UDIDs
xcrun simctl list devices available

# Example output:
# -- iOS 17.2 --
#     iPhone 15 Pro (12345678-1234-1234-1234-123456789ABC) (Shutdown)
#     iPhone 15 Pro Max (87654321-4321-4321-4321-CBA987654321) (Shutdown)
```

**Step 4: Boot the Simulator**

```bash
# Boot a simulator using its UDID (recommended - more reliable)
xcrun simctl boot 12345678-1234-1234-1234-123456789ABC

# Or boot by name (may fail if multiple devices have the same name)
xcrun simctl boot "iPhone 15 Pro"

# Open the Simulator app GUI
open -a Simulator
```

**Step 5: Install the App on Simulator**

```bash
# Install the app on the booted simulator
xcrun simctl install booted ./build/Build/Products/Debug-iphonesimulator/FamilyTreeWrapper.app

# Or specify the simulator UDID explicitly
xcrun simctl install 12345678-1234-1234-1234-123456789ABC \
  ./build/Build/Products/Debug-iphonesimulator/FamilyTreeWrapper.app
```

**Step 6: Launch the App**

```bash
# Get your app's bundle identifier (usually com.family.tree.FamilyTreeWrapper)
# Launch the app on the booted simulator
xcrun simctl launch booted com.family.tree.FamilyTreeWrapper

# Or specify the simulator UDID explicitly
xcrun simctl launch 12345678-1234-1234-1234-123456789ABC com.family.tree.FamilyTreeWrapper
```

#### Complete Workflow Script

Here's a complete script that automates the entire process:

```bash
#!/bin/bash

# Configuration
DEVICE_UDID="12345678-1234-1234-1234-123456789ABC"  # Replace with your simulator UDID
BUNDLE_ID="com.family.tree.FamilyTreeWrapper"      # Replace with your bundle ID
KMP_PROJECT_DIR="/path/to/kmp"                      # Path to KMP project
XCODE_PROJECT_DIR="/path/to/ios-wrapper"            # Path to Xcode wrapper

# Step 1: Build Kotlin framework
echo "Building Kotlin framework..."
cd "$KMP_PROJECT_DIR"
./gradlew :app-ios:linkDebugFrameworkIosSimulatorArm64

# Step 2: Build Xcode project
echo "Building Xcode project..."
cd "$XCODE_PROJECT_DIR"
xcodebuild \
  -project FamilyTreeWrapper.xcodeproj \
  -scheme FamilyTreeWrapper \
  -configuration Debug \
  -sdk iphonesimulator \
  -derivedDataPath ./build

# Step 3: Boot simulator
echo "Booting simulator..."
xcrun simctl boot "$DEVICE_UDID" 2>/dev/null || echo "Simulator already booted"

# Step 4: Open Simulator app
open -a Simulator

# Step 5: Install app
echo "Installing app..."
xcrun simctl install "$DEVICE_UDID" \
  ./build/Build/Products/Debug-iphonesimulator/FamilyTreeWrapper.app

# Step 6: Launch app
echo "Launching app..."
xcrun simctl launch "$DEVICE_UDID" "$BUNDLE_ID"

echo "Done! App should now be running on the simulator."
```

#### Useful Commands for Development

```bash
# Uninstall the app from simulator
xcrun simctl uninstall booted com.family.tree.FamilyTreeWrapper

# Terminate the running app
xcrun simctl terminate booted com.family.tree.FamilyTreeWrapper

# View simulator logs
xcrun simctl spawn booted log stream --predicate 'process == "FamilyTreeWrapper"'

# Take a screenshot
xcrun simctl io booted screenshot screenshot.png

# Record video
xcrun simctl io booted recordVideo --type=mp4 recording.mp4
# Press Ctrl+C to stop recording

# Reset simulator (erase all data)
xcrun simctl erase "$DEVICE_UDID"

# Shutdown simulator
xcrun simctl shutdown "$DEVICE_UDID"
```

#### Troubleshooting Command Line Build

**Error: "xcodebuild: error: Unable to find a destination"**
- Ensure simulator is available: `xcrun simctl list devices`
- Try adding `-destination` flag:
  ```bash
  xcodebuild -project FamilyTreeWrapper.xcodeproj \
    -scheme FamilyTreeWrapper \
    -destination 'platform=iOS Simulator,name=iPhone 15 Pro' \
    -configuration Debug
  ```

**Error: "No such file or directory" when installing**
- Verify the .app bundle exists at the specified path
- Check the path: `ls ./build/Build/Products/Debug-iphonesimulator/`
- Rebuild if needed

**Error: "Failed to install the app on the simulator"**
- Ensure simulator is booted: `xcrun simctl list devices | grep Booted`
- Try shutting down and rebooting the simulator
- Check Xcode console for detailed error messages

**App doesn't launch or crashes immediately**
- Check simulator console: `xcrun simctl spawn booted log stream`
- Verify bundle identifier matches: check in Xcode project settings
- Ensure framework is properly embedded in the .app bundle

**Framework not found when building**
- Rebuild Kotlin framework first
- Verify Framework Search Paths in Xcode project
- Clean Xcode build: `rm -rf ./build` then rebuild

## Useful iOS Simulator Commands

```bash
# List all available simulators with their UDIDs
xcrun simctl list devices available

# Example output:
# -- iOS 17.2 --
#     iPhone 15 Pro (12345678-1234-1234-1234-123456789ABC) (Shutdown)
#     iPhone 15 Pro Max (87654321-4321-4321-4321-CBA987654321) (Shutdown)

# Boot a specific simulator (RECOMMENDED: use UDID instead of name)
xcrun simctl boot 12345678-1234-1234-1234-123456789ABC

# Or boot by name (less reliable, may fail with "Invalid device or device pair" error)
xcrun simctl boot "iPhone 15 Pro"

# Shutdown a simulator (by UDID or name)
xcrun simctl shutdown 12345678-1234-1234-1234-123456789ABC
# or
xcrun simctl shutdown "iPhone 15 Pro"

# Erase simulator data (by UDID or name)
xcrun simctl erase 12345678-1234-1234-1234-123456789ABC
# or
xcrun simctl erase "iPhone 15 Pro"

# Open Simulator app
open -a Simulator
```

## Debugging

### Logging
Use `println()` in Kotlin code — output will appear in:
- IntelliJ IDEA console (when running from IDE)
- Xcode console (when running from Xcode)

Example:
```kotlin
println("[DEBUG] Loading individuals: ${individuals.size}")
```

### Breakpoints
- Set breakpoints in IntelliJ IDEA for Kotlin code
- The debugger will attach when running with the Kotlin Multiplatform Mobile plugin

### Xcode Integration
For native debugging:
1. Build the framework with debug symbols
2. Open the Xcode wrapper project
3. Use LLDB debugger for native code inspection

## iOS Simulator vs Real Device

**This project is configured for iOS Simulator only.**

| Feature | iOS Simulator | Real Device |
|---------|---------------|-------------|
| CPU Architecture | x86_64 (Intel) or arm64 (M1+) | arm64 (A-series) |
| Performance | Usually faster (Mac is more powerful) | Real device performance |
| Project Support | ✅ Fully supported | ❌ Not configured (requires changing SDKROOT) |
| Sensors | Limited emulation | Full hardware support |
| Camera | Test images only | Full camera support |
| GPS/Location | Can be set manually | Real GPS data |
| Touch ID/Face ID | Emulated | Real biometric hardware |
| App Store/TestFlight | Not available | Available |

For the Family Tree app (primarily visualization), **iOS Simulator is sufficient** for development and testing.

## Troubleshooting

### Error: "Invalid device or device pair" when booting simulator

**Symptoms:**
```
Invalid device or device pair: iPhone 15 Pro
```
Or accompanied by CoreSimulator version mismatch warnings:
```
CoreSimulator detected version change. Framework version (1051.9.4) does not match existing job version (1048).
```

**Cause:** This error occurs when:
1. Multiple simulators have the same name
2. CoreSimulator service has version mismatches after Xcode updates
3. Using device name instead of UDID to identify simulators

**Solution:**

1. **Use UDID instead of device name (recommended):**
   ```bash
   # List all simulators with UDIDs
   xcrun simctl list devices available
   
   # Boot using the UDID (more reliable)
   xcrun simctl boot 12345678-1234-1234-1234-123456789ABC
   ```

2. **Kill and restart CoreSimulator service:**
   ```bash
   # Kill the CoreSimulator service
   sudo killall -9 com.apple.CoreSimulator.CoreSimulatorService
   
   # Or reboot CoreSimulator
   xcrun simctl shutdown all
   
   # Try booting again
   xcrun simctl boot <DEVICE_UDID>
   ```

3. **Delete and recreate the simulator:**
   ```bash
   # Delete the problematic simulator
   xcrun simctl delete "iPhone 15 Pro"
   
   # Create a new one
   xcrun simctl create "iPhone 15 Pro" "iPhone 15 Pro" "iOS17.2"
   
   # List to get the new UDID
   xcrun simctl list devices available
   ```

4. **Restart Xcode and Simulator app:**
   - Quit Xcode and Simulator apps completely
   - Reopen Xcode
   - Try booting the simulator again

### Error: Xcode not found
```bash
sudo xcode-select --switch /Applications/Xcode.app/Contents/Developer
```

### Error: No simulators available

**Symptoms:**
```bash
xcrun simctl list devices available
== Devices ==
# (empty or no devices listed)
```

**Cause:** iOS Simulators are not installed on your system. This can happen with:
- Fresh Xcode installation
- Xcode command-line tools only (without full Xcode app)
- After Xcode updates that don't include default simulators

**Solution:**

#### Option 1: Install Simulators via Xcode (Recommended)

1. **Open Xcode** and ensure it's fully installed (not just command-line tools)
   
2. **Navigate to Simulator Settings:**
   - Open Xcode
   - Go to **Xcode** → **Settings** (or **Preferences** in older versions)
   - Select the **Platforms** tab (or **Components** in older Xcode versions)
   
3. **Download iOS Simulator Runtimes:**
   - You'll see a list of available iOS versions
   - Click the **download icon** (⬇️) next to the iOS version you want
   - Wait for the download and installation to complete (can be several GB)
   - Common choices: latest iOS version (e.g., iOS 17.x, iOS 18.x)

4. **Create a Simulator Device:**
   - Open Xcode → **Window** → **Devices and Simulators** (⇧⌘2)
   - Select the **Simulators** tab
   - Click the **+** button in the bottom-left corner
   - Choose:
     - **Simulator Name:** e.g., "iPhone 15 Pro"
     - **Device Type:** e.g., iPhone 15 Pro, iPad Pro, etc.
     - **OS Version:** Select from installed runtimes
   - Click **Create**

5. **Verify the simulator was created:**
   ```bash
   xcrun simctl list devices available
   ```
   You should now see your newly created simulator with its UDID.

#### Option 2: Create Simulator via Command Line

If you already have iOS runtimes installed but no simulator devices:

```bash
# List available device types
xcrun simctl list devicetypes

# Example output:
# com.apple.CoreSimulator.SimDeviceType.iPhone-15-Pro
# com.apple.CoreSimulator.SimDeviceType.iPad-Pro-12-9-inch-6th-generation

# List available iOS runtimes
xcrun simctl list runtimes

# Example output:
# iOS 17.2 (17.2 - 21C62) - com.apple.CoreSimulator.SimRuntime.iOS-17-2

# Create a new simulator
xcrun simctl create "iPhone 15 Pro" "com.apple.CoreSimulator.SimDeviceType.iPhone-15-Pro" "com.apple.CoreSimulator.SimRuntime.iOS-17-2"

# Simpler syntax (if device type and runtime names are unique)
xcrun simctl create "iPhone 15 Pro" "iPhone 15 Pro" "iOS17.2"

# Verify it was created
xcrun simctl list devices available
```

#### Option 3: Install Xcode if You Only Have Command-Line Tools

If you only installed Xcode command-line tools, you need the full Xcode app:

1. **Download Xcode from App Store:**
   - Open App Store
   - Search for "Xcode"
   - Click **Get** or **Download**
   - Wait for installation (Xcode is ~10-15 GB)

2. **Or download from Apple Developer:**
   - Go to https://developer.apple.com/download/
   - Download the latest Xcode version
   - Install the `.xip` file

3. **Set Xcode as active developer directory:**
   ```bash
   sudo xcode-select --switch /Applications/Xcode.app/Contents/Developer
   ```

4. **Accept Xcode license:**
   ```bash
   sudo xcodebuild -license accept
   ```

5. **Launch Xcode once** to complete installation and install additional components

6. **Then follow Option 1 or Option 2** to install simulators

#### Troubleshooting Simulator Installation

**If iOS runtimes are not available in Xcode:**
- Ensure you're running the latest Xcode version
- Check your macOS version is compatible with desired iOS version
- Go to **Xcode** → **Settings** → **Platforms** and manually download runtimes

**If `xcrun simctl create` fails:**
```bash
# Check if runtime is properly installed
xcrun simctl list runtimes | grep iOS

# If no runtimes listed, install via Xcode Settings → Platforms
# Or download manually:
xcodebuild -downloadPlatform iOS
```

**Verify your setup:**
```bash
# Check Xcode installation
xcode-select -p
# Should output: /Applications/Xcode.app/Contents/Developer

# List all simulators
xcrun simctl list devices

# Check available device types and runtimes
xcrun simctl list devicetypes
xcrun simctl list runtimes
```

### Build fails with "Could not find iOS SDK"
```bash
# Verify Xcode command line tools
xcode-select --install

# Accept Xcode license
sudo xcodebuild -license accept
```

### Framework not found in Xcode
1. Check that framework was built: `app-ios/build/bin/`
2. Verify framework search paths in Xcode build settings
3. Clean and rebuild: `./gradlew clean :app-ios:linkDebugFrameworkIosSimulatorArm64`

### App crashes on launch
- Check console logs for Kotlin exceptions
- Verify all dependencies are properly linked
- Ensure iOS target SDK matches compiled framework

## Packaging for Distribution

**Not applicable** - this project is configured for iOS Simulator only and cannot be packaged for App Store or device distribution.

To enable distribution, you would need to:
1. Change `SDKROOT` from `iphonesimulator` to `iphoneos` in the Xcode project
2. Configure proper code signing with a valid Team ID
3. Build the release framework for iosArm64: `./gradlew :app-ios:linkReleaseFrameworkIosArm64`
4. Create and archive a release build in Xcode
5. Upload to App Store Connect or export for ad-hoc distribution

## Next Steps

- Set up continuous integration for iOS builds
- Add iOS-specific UI adaptations if needed
- Configure App Store metadata and assets
- Implement iOS-specific features (if any)

## Resources

- [Kotlin Multiplatform Documentation](https://kotlinlang.org/docs/multiplatform.html)
- [Compose Multiplatform for iOS](https://www.jetbrains.com/lp/compose-multiplatform/)
- [Apple Developer Documentation](https://developer.apple.com/documentation/)
- [iOS Simulator Guide](https://developer.apple.com/documentation/xcode/running-your-app-in-simulator-or-on-a-device)
