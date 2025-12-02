# Android App Build Instructions

## Signing Configuration

The Android app requires a keystore for signing release builds. 

### Local Development

For local builds, a keystore file `release.keystore` is required in the `app-android` directory.

If you don't have one, you can create it using:

```bash
keytool -genkey -v -keystore app-android/release.keystore \
  -alias release \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass android123 \
  -keypass android123 \
  -dname "CN=Family Tree Editor, OU=Development, O=Family Tree, L=Unknown, ST=Unknown, C=US"
```

**Note:** This is a development keystore. For production releases, you should use a secure keystore with strong passwords.

### CI/CD (GitHub Actions)

The GitHub Actions workflow automatically creates a keystore for release builds. The keystore is not committed to the repository for security reasons.

## Building

### Debug Build
```bash
./gradlew :app-android:assembleDebug
```

### Release Build
```bash
./gradlew :app-android:assembleRelease
```

The APK will be located at:
- Debug: `app-android/build/outputs/apk/debug/app-android-debug.apk`
- Release: `app-android/build/outputs/apk/release/app-android-release.apk`

## Verifying Signature

To verify the APK is properly signed:

```bash
java -jar $ANDROID_HOME/build-tools/*/lib/apksigner.jar verify --print-certs app-android/build/outputs/apk/release/app-android-release.apk
```

You should see output showing the signer certificate information.
