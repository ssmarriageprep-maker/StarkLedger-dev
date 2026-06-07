# Android Development Setup for StarkLedger

This document describes the automated Android development environment setup for GitHub Codespaces.

## ✨ Automatic Setup

When you create a new GitHub Codespace for this repository, the Android development environment is automatically configured with:

✅ Java 17 JDK  
✅ Android SDK (~/android-sdk)  
✅ Build Tools 35.0.0  
✅ Android Platforms API 35  
✅ Command-line Tools, Platform Tools, Emulator  
✅ Gradle 8.9 & Kotlin 1.9.23  

**No manual installation needed!**

## 🚀 Quick Start

After opening a Codespace, open a terminal and run:

```bash
# Verify everything is set up
./gradlew --version

# Build the app
./gradlew app:build

# Run tests
./gradlew app:test

# Build debug APK
./gradlew app:assembleDebug
```

## 📁 Configuration Files

**`.devcontainer/devcontainer.json`**
- Main configuration file for the container environment
- Specifies base image, post-creation commands, and VS Code settings

**`.devcontainer/setup-android-dev.sh`**
- Automated setup script that installs all tools
- Runs automatically when the Codespace is created
- Can be run manually: `bash .devcontainer/setup-android-dev.sh`

**`.devcontainer/README.md`**
- Detailed documentation about the dev container setup

## 🔧 Environment Variables

The following are automatically configured in your shell:

```
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ANDROID_SDK_ROOT=$HOME/android-sdk
ANDROID_HOME=$ANDROID_SDK_ROOT
```

These are persisted in `~/.bashrc` so they're available in all future terminal sessions.

## 📋 Local Setup (Without Codespaces)

If you want to set up Android development locally, use:

```bash
source ./android-dev-env.sh
```

This script configures all necessary environment variables for your local machine.

## 🛠️ System Requirements (for local development)

- Ubuntu 24.04 LTS (or similar Linux distribution)
- 8GB+ RAM
- 10GB+ free disk space
- Internet connection for downloading SDKs

## 📚 Useful Commands

```bash
# List all available build tasks
./gradlew tasks --all

# Build with specific variant
./gradlew app:buildDebug      # Debug build
./gradlew app:buildRelease    # Release build

# Run unit tests
./gradlew app:testDebug

# Build and run on connected device
./gradlew app:installDebug
adb shell am start -n com.starklabs.moneytracker/.MainActivity

# Check Java/Android setup
java -version
$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager --list
adb devices
```

## 🔍 Troubleshooting

**Issue: "SDK not found" error**
- Solution: Run `bash .devcontainer/setup-android-dev.sh` or create a new Codespace

**Issue: Gradle daemon won't start**
- Solution: Clear the gradle cache: `rm -rf ~/.gradle/caches && ./gradlew clean`

**Issue: Java version mismatch**
- Solution: Verify: `/usr/lib/jvm/java-17-openjdk-amd64/bin/java -version`

**Issue: Codespace setup takes too long**
- Expected: First-time setup downloads ~5GB of Android SDK components (may take 5-10 minutes)
- Subsequent Codespaces: Faster due to cached container layers

## 📖 For More Information

- [StarkLedger Documentation](./CLAUDE.md)
- [GitHub Codespaces Docs](https://docs.github.com/en/codespaces)
- [Android SDK Docs](https://developer.android.com/studio/command-line)
- [Gradle Docs](https://docs.gradle.org/current/userguide/userguide.html)

---

**Happy coding! 🚀**
