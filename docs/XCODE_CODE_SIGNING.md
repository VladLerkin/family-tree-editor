# Xcode Code Signing Configuration

## Project Configuration

**This project is configured for iOS Simulator only.** The Xcode project has `SDKROOT=iphonesimulator`, which means it can only build for iOS Simulator and cannot build for physical devices.

## The Problem

When building the iOS app, you may encounter this error:

```
Signing for "iosApp" requires a development team. Select a development team in the Run/Debug Configurations > Xcode Application > {0} > Options editor.
Build failed with 5 errors and 0 warnings
```

This occurs because Xcode requires code signing configuration, even for simulator builds.

## Root Cause

The Xcode project is configured with:
- **Code Signing Style:** Automatic
- **Development Team:** `${TEAM_ID}` (references Config.xcconfig)
- **TEAM_ID in Config.xcconfig:** Empty

When TEAM_ID is empty, Xcode cannot automatically manage code signing and fails the build.

## Solutions

Choose the solution that fits your needs:

### Solution 1: Disable Code Signing for Simulator (Recommended for Quick Start)

If you only need to run on the iOS Simulator and don't plan to deploy to a real device, you can disable code signing.

#### Option A: Modify Config.xcconfig (Easiest)

Edit `iosApp/Configuration/Config.xcconfig` and set a placeholder team ID:

```
TEAM_ID=SIMULATOR
```

This allows the build to proceed without requiring an actual Apple Developer account.

#### Option B: Modify Xcode Project Directly

1. Open the Xcode project:
   ```bash
   open iosApp/iosApp.xcodeproj
   ```

2. Select the project in the Project Navigator (left sidebar)

3. Select the **iosApp** target

4. Go to **Signing & Capabilities** tab

5. **For Debug configuration:**
   - Uncheck **"Automatically manage signing"**
   - Leave **Team** as "None"
   - Set **Signing Certificate** to "Sign to Run Locally"
   - Leave **Provisioning Profile** empty

6. Build the project in Xcode or from IntelliJ IDEA

**Note:** This approach requires modifying the project file each time you regenerate or update it.

### Solution 2: Add Your Development Team (For Real Device Testing)

If you need to run on a real iOS device, you must configure a valid development team.

#### Prerequisites

- Apple Developer account (free or paid)
- Xcode installed and signed in with your Apple ID

#### Steps

1. **Find your Team ID:**

   **Option A: Via Xcode**
   - Open Xcode
   - Go to **Xcode > Settings** (or Preferences)
   - Select **Accounts** tab
   - Sign in with your Apple ID if not already signed in
   - Select your account
   - Click on your team name
   - Your **Team ID** will be displayed (format: XXXXXXXXXX - 10 alphanumeric characters)

   **Option B: Via Terminal**
   ```bash
   # List all available teams
   security find-identity -v -p codesigning
   ```

   **Option C: Via Apple Developer Portal**
   - Go to https://developer.apple.com/account
   - Sign in with your Apple ID
   - Go to **Membership** section
   - Your **Team ID** is displayed

2. **Configure TEAM_ID in Config.xcconfig:**

   Edit `iosApp/Configuration/Config.xcconfig`:
   ```
   TEAM_ID=YOUR_TEAM_ID_HERE
   ```

   Example:
   ```
   TEAM_ID=ABC1234567
   ```

3. **Open Xcode project and verify:**
   ```bash
   open iosApp/iosApp.xcodeproj
   ```

4. **In Xcode:**
   - Select the project → **iosApp** target
   - Go to **Signing & Capabilities** tab
   - Ensure **"Automatically manage signing"** is checked
   - Verify your Team is selected
   - Xcode should show "Signing for iPhone..." or "Signing for iPad..."

5. **Build the project:**
   ```bash
   # Navigate to project root (already there)
   ./gradlew :app-ios:linkDebugFrameworkIosSimulatorArm64
   ```

   Or build from IntelliJ IDEA or Xcode.

### Solution 3: Use Manual Code Signing (Advanced)

For complete control over provisioning profiles and certificates:

1. Open the Xcode project:
   ```bash
   open iosApp/iosApp.xcodeproj
   ```

2. Select the project → **iosApp** target

3. Go to **Signing & Capabilities** tab

4. Uncheck **"Automatically manage signing"**

5. Configure manually:
   - **Signing Certificate:** Choose your development certificate
   - **Provisioning Profile:** Select or import your provisioning profile
   - **Team:** Select your team

6. Apply the same settings for both Debug and Release configurations

## Verification

After applying any solution, verify the build works:

### Test Simulator Build

```bash
# Navigate to project root (already there)

# For Apple Silicon Mac
./gradlew :app-ios:linkDebugFrameworkIosSimulatorArm64

# For Intel Mac
./gradlew :app-ios:linkDebugFrameworkIosX64
```

Expected output:
```
BUILD SUCCESSFUL
```

The framework should be created at:
- Apple Silicon: `app-ios/build/bin/iosSimulatorArm64/debugFramework/FamilyTreeApp.framework`
- Intel: `app-ios/build/bin/iosX64/debugFramework/FamilyTreeApp.framework`

**Note:** Device builds are not supported with the current simulator-only Xcode configuration (`SDKROOT=iphonesimulator`).

## Troubleshooting

### Error: "No signing certificate found"

**Cause:** Your Mac doesn't have development certificates installed.

**Solution:**
1. Open Xcode
2. Go to **Xcode > Settings > Accounts**
3. Select your Apple ID
4. Click **"Manage Certificates..."**
5. Click **+** → **"Apple Development"**
6. A new certificate will be created and installed

### Error: "No provisioning profile found"

**Cause:** Automatic provisioning couldn't create/download a profile.

**Solution:**
1. Ensure your Team ID is correct in Config.xcconfig
2. In Xcode, select the target
3. Go to **Signing & Capabilities**
4. Ensure **"Automatically manage signing"** is checked
5. Click the error/warning icon and follow Xcode's suggestions
6. Xcode will automatically create a provisioning profile

### Error: "The app ID cannot be registered to your development team"

**Cause:** The bundle identifier is already registered to a different team, or you're using a free Apple ID with bundle ID restrictions.

**Solution:**
1. Edit `iosApp/Configuration/Config.xcconfig`
2. Change the bundle identifier to something unique:
   ```
   PRODUCT_BUNDLE_IDENTIFIER=com.yourname.familytree$(TEAM_ID)
   ```
3. Try building again

### Build works in terminal but fails in IntelliJ IDEA

**Cause:** IntelliJ IDEA may be using a different build configuration.

**Solution:**
1. In IntelliJ IDEA, go to **Run > Edit Configurations**
2. Select your iOS run configuration
3. Check the **Execution target** and **Configuration** settings
4. Ensure they match your terminal build (Debug/Release, correct architecture)
5. Rebuild and try again

### Device builds are not available

**Note:** This project is configured for simulator-only builds (`SDKROOT=iphonesimulator`).

**To enable device builds:**
1. Change `SDKROOT` from `iphonesimulator` to `iphoneos` in the Xcode project file (`iosApp/iosApp.xcodeproj/project.pbxproj`)
2. Configure valid code signing with a Team ID in `Config.xcconfig`
3. Set up development certificates and provisioning profiles in Xcode
4. Register your device in your Apple Developer account

## Best Practices

### For Simulator-Only Development (Current Configuration)
- The project is pre-configured with `TEAM_ID=SIMULATOR` in `Config.xcconfig`
- The Xcode project uses `SDKROOT=iphonesimulator` to restrict builds to simulator only
- This allows building without an Apple Developer account
- This configuration is committed to the repository for all developers

### If You Need to Add Device Support
- Change `SDKROOT` from `iphonesimulator` to `iphoneos` in the Xcode project
- Add `iosApp/Configuration/Config.xcconfig` to `.gitignore`
- Commit a template file: `iosApp/Configuration/Config.xcconfig.template`
- Each developer creates their own Config.xcconfig with their Team ID
- Document the setup process in your project README

Example `.gitignore` entry:
```gitignore
# iOS code signing configuration (personal)
iosApp/Configuration/Config.xcconfig
```

Example `Config.xcconfig.template`:
```
# Copy this file to Config.xcconfig and fill in your Team ID
# Get your Team ID from Xcode > Settings > Accounts
TEAM_ID=YOUR_TEAM_ID_HERE

PRODUCT_NAME=FamilyTree
PRODUCT_BUNDLE_IDENTIFIER=com.family.tree.ios$(TEAM_ID)

CURRENT_PROJECT_VERSION=1
MARKETING_VERSION=0.1

JAVA_HOME=/Users/YOUR_USERNAME/.sdkman/candidates/java/21.0.8-tem
```

### For CI/CD
- The simulator-only configuration works out-of-the-box in CI environments
- No code signing certificates or Team ID needed for simulator builds
- Consider using fastlane for automated testing on simulators

## Related Documentation

- [BUILD_IOS.md](BUILD_IOS.md) - Complete iOS build instructions
- [iOS_TROUBLESHOOTING.md](iOS_TROUBLESHOOTING.md) - iOS build configuration issues
- [Apple Code Signing Guide](https://developer.apple.com/support/code-signing/)
- [Xcode Build Settings Reference](https://developer.apple.com/documentation/xcode/build-settings-reference)

## Summary

**Current Configuration:**
- Project is configured for **iOS Simulator only** (`SDKROOT=iphonesimulator`)
- Pre-configured with `TEAM_ID=SIMULATOR` in Config.xcconfig
- No Apple Developer account required
- Builds work out-of-the-box for simulator targets

**To Enable Device Support:**
1. Change `SDKROOT` from `iphonesimulator` to `iphoneos` in the Xcode project file
2. Configure a valid Team ID in Config.xcconfig
3. Set up development certificates and provisioning profiles
