# 🌳 Family Tree Editor

> A cross-platform family tree editor built with Kotlin Multiplatform and Compose Multiplatform

[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.20-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.10.3-brightgreen)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![Gradle](https://img.shields.io/badge/Gradle-9.4.1-02303A.svg?logo=gradle)](https://gradle.org)

## ✨ Features

- 📊 **Interactive Tree Visualization** - Pan, zoom, and navigate your family tree
- 💾 **JSON Persistence** - Save and load family trees in JSON format
- 📝 **Markdown Export** - Export your family tree to an easy-to-read Markdown document
- 🎨 **Modern UI** - Built with Compose Multiplatform for native look and feel
- 🔍 **Inspector Panel** - View and edit individual and family details
- ⌨️ **Keyboard Shortcuts** - Efficient navigation and editing

## 📦 Download

**Ready-to-use distributions are available for free!**

Get the latest version for your platform:

<div align="center">

### [⬇️ Download from Releases](https://github.com/VladLerkin/family-tree-editor/releases)

</div>

**Available platforms:**
- 🖥️ **macOS** - Universal binary (Apple Silicon)
- 🐧 **Linux** - Debian package (.deb) and AppImage
- 🪟 **Windows** - MSI installer and portable .exe
- 📱 **Android** - APK for Android 8.0+
- 📺 **Android TV** - APK for Android TV
- 🍎 **iOS** - Developer's build

> 💡 No compilation needed! Just download and run for free.

## 🚀 Quick Start

### Desktop
```bash
./gradlew :app-desktop:run
```

### Android
```bash
./gradlew :app-android:installDebug
```

### iOS
```bash
./gradlew :app-ios:linkDebugFrameworkIosSimulatorArm64
```

> 📖 **Detailed guides:** [Desktop](docs/BUILD_DESKTOP.md) • [Android](docs/BUILD_ANDROID.md) • [iOS](docs/BUILD_IOS.md)

## 🏗️ Architecture

```
├── core/          # Shared domain models & serialization
├── ui/            # Shared Compose UI components
├── app-desktop/   # Desktop application
├── app-android/   # Android application
└── app-ios/       # iOS application
```

## 🛠️ Tech Stack

| Component | Version |
|-----------|---------|
| Kotlin | 2.3.20 |
| Compose Multiplatform | 1.10.3 |
| Gradle | 9.4.1 |
| Android Gradle Plugin | 9.1.0 |
| JDK | 25 |
| Android Target SDK | 36 |

## 🐛 Troubleshooting

<details>
<summary><b>JDK Version Issues</b></summary>

Ensure you're using JDK 25:
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
```
</details>

<details>
<summary><b>Android SDK Not Found</b></summary>

Create `local.properties`:
```properties
sdk.dir=/Users/<you>/Library/Android/sdk
```
</details>

<details>
<summary><b>iOS Build Issues</b></summary>

See [iOS Troubleshooting Guide](docs/iOS_TROUBLESHOOTING.md)
</details>

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

---

<p align="center">Made with ❤️ using Kotlin Multiplatform</p>
