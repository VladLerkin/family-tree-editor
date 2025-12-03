# Solving "No such module 'FamilyTreeApp'" Error in Xcode

## Problem
When trying to import the module in a Swift file:
```swift
import FamilyTreeApp
```

Xcode shows error: **"No such module 'FamilyTreeApp'"**

## Cause
To work with a Kotlin Multiplatform framework in iOS, you need to create an Xcode wrapper project that will use the built framework.

## Quick Solution

### Step 1: Build the Framework
```bash
cd /Users/yav/IdeaProjects/rel
./gradlew :app-ios:linkDebugFrameworkIosSimulatorArm64
```

The framework will be created at:
```
app-ios/build/bin/iosSimulatorArm64/debugFramework/FamilyTreeApp.framework
```

### Step 2: Create New Xcode Project

1. Open Xcode
2. **File → New → Project**
3. Select **iOS → App**
4. Project settings:
   - Product Name: `FamilyTreeWrapper` (or any other name)
   - Interface: **SwiftUI**
   - Language: **Swift**
   - Save outside the project folder (e.g., in `ios-wrapper`)

### Step 3: Add Framework to Xcode Project

1. In Xcode, select your project in Project Navigator
2. Select your application **target**
3. Go to **General** tab
4. Scroll to **Frameworks, Libraries, and Embedded Content**
5. Click **+** (plus)
6. Click **Add Other... → Add Files...**
7. Navigate to:
   ```
   /Users/yav/IdeaProjects/rel/app-ios/build/bin/iosSimulatorArm64/debugFramework/
   ```
8. Select `FamilyTreeApp.framework`
9. **Important:** Change the value on the right to **Embed & Sign**

### Step 4: Configure Framework Search Paths

1. In target settings, go to **Build Settings** tab
2. Find **Framework Search Paths**
3. Add path:
   ```
   /Users/yav/IdeaProjects/rel/app-ios/build/bin/iosSimulatorArm64/debugFramework
   ```
4. Ensure **recursive** checkbox is enabled

### Step 5: Create Swift UI

In your main Swift file (e.g., `ContentView.swift`):

```swift
import SwiftUI
import FamilyTreeApp  // This import should now work

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        return Main_iosKt.MainViewController()
    }
    
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}
```

### Step 6: Build and Run

1. In Xcode: **Product → Clean Build Folder** (⇧⌘K)
2. Select iOS Simulator (e.g., iPhone 15 Pro)
3. **Product → Build** (⌘B)
4. **Product → Run** (⌘R)

## If Error Still Occurs

### Check 1: Does Framework Exist?
```bash
ls -la /Users/yav/IdeaProjects/rel/app-ios/build/bin/iosSimulatorArm64/debugFramework/FamilyTreeApp.framework
```

If not - rebuild:
```bash
cd /Users/yav/IdeaProjects/rel
./gradlew :app-ios:linkDebugFrameworkIosSimulatorArm64
```

### Check 2: Correct Architecture?

Check your Mac architecture:
```bash
uname -m
```

- If `arm64` (Apple Silicon M1/M2/M3) → use `iosSimulatorArm64`
- If `x86_64` (Intel Mac) → use `iosX64`:
  ```bash
  ./gradlew :app-ios:linkDebugFrameworkIosX64
  ```
  And change the path in Xcode to:
  ```
  /Users/yav/IdeaProjects/rel/app-ios/build/bin/iosX64/debugFramework
  ```

### Check 3: Clean Xcode

```bash
# Close Xcode completely, then:
rm -rf ~/Library/Developer/Xcode/DerivedData/*
```

Then open Xcode again and rebuild.

### Check 4: Correct Module Name

In Swift files, use the **exact** name:
```swift
import FamilyTreeApp  // ✅ Correct
```

DO NOT use:
```swift
import app_ios      // ❌ Incorrect
import AppIos       // ❌ Incorrect  
import FamilyTree   // ❌ Incorrect
```

## Alternative Method: Using IntelliJ IDEA

If you have IntelliJ IDEA Ultimate installed:

1. Install **Kotlin Multiplatform Mobile** plugin:
   - IntelliJ IDEA → Settings → Plugins
   - Search for "Kotlin Multiplatform Mobile"
   - Install and restart IDE

2. Open project in IntelliJ IDEA

3. **Run → Edit Configurations...**

4. Click **+** → select **Kotlin Mobile**

5. Configure:
   - Execute target: **app-ios**
   - Select iOS Simulator

6. Click **Run** (▶️)

IntelliJ IDEA will automatically create an Xcode project and launch the application.

## Additional Documentation

Detailed documentation in English:
- Complete guide: `docs/BUILD_IOS.md`
- Troubleshooting: `docs/iOS_TROUBLESHOOTING.md`

## Project Structure

```
Project Root:
├── app-ios/              # iOS module
│   ├── build.gradle.kts  # baseName = "FamilyTreeApp"
│   └── src/iosMain/kotlin/com/family/tree/ios/main.kt
├── core/                 # Shared business logic
└── ui/                   # Shared UI (Compose Multiplatform)
```

The `FamilyTreeApp` framework exports the `MainViewController()` function, which creates an iOS view controller with Compose UI.

## Why Doesn't This Project Match the Standard Template?

**Important:** This project differs from standard Kotlin Multiplatform templates.

### Standard KMP Template (Single "shared" Module)

In typical KMP projects, the configuration looks like this:

```kotlin
// shared/build.gradle.kts
kotlin {
    androidTarget()
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"  // Framework name is set here
            isStatic = true
        }
    }
    
    sourceSets {
        commonMain.dependencies {
            // All dependencies in one place
        }
    }
}
```

In standard templates, **iOS targets and framework configuration are in one module**.

### This Project (Multi-Module Architecture)

```
Project Root:
├── core/build.gradle.kts      # Declares iosX64(), iosArm64(), iosSimulatorArm64()
├── ui/build.gradle.kts        # Declares iosX64(), iosArm64(), iosSimulatorArm64()
└── app-ios/build.gradle.kts   # Framework configuration: baseName = "FamilyTreeApp"
```

### What's the Difference and Why?

**Why is the project structured differently:**
- **Separation of concerns:** `core` (business logic) and `ui` (Compose UI) are reusable libraries
- **Each module declares iOS targets** for iOS compilation
- **Only `app-ios` creates the framework**, which combines all modules for Xcode
- **Better scalability:** large projects benefit from module separation

**Both approaches are correct:**
- ✅ Single "shared" module — simpler, suitable for small projects (standard template)
- ✅ Multiple modules — more organized, better for medium and large projects (this project)

**Main takeaway:** The `baseName = "FamilyTreeApp"` name in the `app-ios/build.gradle.kts` file determines the framework name you use in Swift/Xcode, regardless of the number of modules in the project.

### How to Check Configuration in Your Project

Open the `app-ios/build.gradle.kts` file and find:

```kotlin
kotlin {
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "FamilyTreeApp"  // ← This name is used in import
            isStatic = true
        }
    }
}
```

**The `baseName` value determines the module name for import in Swift:**
```swift
import FamilyTreeApp  // Name is taken from baseName
```

## Summary

✅ Framework built correctly (`baseName = "FamilyTreeApp"`)  
✅ Framework is located at `app-ios/build/bin/.../FamilyTreeApp.framework`  
✅ But to use it, **you need an Xcode wrapper project**  
✅ Follow the instructions above to create an Xcode project
