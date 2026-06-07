# Codespace Development Environment Setup

This directory contains the configuration for automated setup of the StarkLedger Android development environment in GitHub Codespaces.

## Files

### `devcontainer.json`
The main configuration file for the development container. It specifies:
- **Base Image**: Ubuntu 24.04 (latest stable)
- **Post-creation Command**: Automatically runs `setup-android-dev.sh`
- **VS Code Settings**: Chat tool auto-approval enabled
- **VS Code Extensions**: Android NDK Manager, Flutter
- **Features**: Git support

### `setup-android-dev.sh`
Automated installation script that sets up:
- Java 17 JDK
- Android SDK Command-line Tools
- Android build-tools 35.0.0
- Android platforms for API level 35
- Platform tools (adb, fastboot)
- Android Emulator
- Environment variables and PATH configuration

## How It Works

1. **On Codespace Creation**: The `.devcontainer/devcontainer.json` is read
2. **Container Setup**: Ubuntu 24.04 base image is pulled and started
3. **Post-Create Hook**: `setup-android-dev.sh` is automatically executed
4. **Installation**: All Android development tools are installed and configured
5. **Environment**: `.bashrc` is updated with proper environment variables
6. **Ready**: When the terminal opens, everything is ready for development

## What Gets Installed

- **Java 17 OpenJDK**
  - Location: `/usr/lib/jvm/java-17-openjdk-amd64`
  - Used for: Gradle, Kotlin, Android development

- **Android SDK**
  - Location: `~/android-sdk`
  - Includes: Command-line tools, build-tools, platforms

- **Build Tools**
  - Gradle 8.9 (from gradlew wrapper)
  - Kotlin 1.9.23
  - Android build-tools 35.0.0

## Environment Variables

After setup, the following are automatically available:

```bash
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ANDROID_SDK_ROOT=$HOME/android-sdk
ANDROID_HOME=$ANDROID_SDK_ROOT
```

These are added to `~/.bashrc` for persistence across terminal sessions.

## Usage After Codespace Creation

Once the Codespace opens, you can immediately use the build tools:

```bash
# List available tasks
./gradlew tasks

# Build the debug APK
./gradlew app:assembleDebug

# Run tests
./gradlew app:test

# Full build with testing
./gradlew app:build
```

## Customization

To modify the setup process:

1. Edit `setup-android-dev.sh` to add additional tools or configurations
2. Update `devcontainer.json` to install additional VS Code extensions
3. Commit and push changes
4. Create a new Codespace for changes to take effect

## Troubleshooting

If the setup fails or tools are missing:

1. **Manual Re-run**: 
   ```bash
   bash .devcontainer/setup-android-dev.sh
   ```

2. **Verify Installation**:
   ```bash
   java -version
   echo $ANDROID_SDK_ROOT
   which adb
   ```

3. **Rebuild Container**: Delete and recreate the Codespace

## References

- [GitHub Codespaces Documentation](https://docs.github.com/en/codespaces)
- [Dev Containers Specification](https://containers.dev/)
- [Android SDK Command-line Tools](https://developer.android.com/studio/command-line)
