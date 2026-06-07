#!/bin/bash
# Android Development Environment Setup for Codespaces
# Automatically installs and configures Android SDK, JDK, and build tools

set -e  # Exit on error

echo "📱 Setting up Android Development Environment..."

# Install Java 17 JDK
echo "📦 Installing Java 17 JDK..."
apt-get update -qq
apt-get install -y -qq openjdk-17-jdk > /dev/null 2>&1
echo "✓ Java 17 JDK installed"

# Create Android SDK directory
echo "📂 Creating Android SDK directory..."
mkdir -p ~/android-sdk
cd ~/android-sdk

# Download and extract Android SDK Command-line Tools
echo "📥 Downloading Android SDK Command-line Tools..."
wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip -q commandlinetools-linux-11076708_latest.zip
rm commandlinetools-linux-11076708_latest.zip
mkdir -p cmdline-tools/latest
mv cmdline-tools/* cmdline-tools/latest/ 2>/dev/null || true
echo "✓ Android SDK Command-line Tools installed"

# Set environment variables
export ANDROID_SDK_ROOT=$HOME/android-sdk
export ANDROID_HOME=$ANDROID_SDK_ROOT
export PATH=$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/emulator:$PATH

# Accept licenses
echo "⚖️  Accepting Android SDK licenses..."
yes | $ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager --sdk_root=$ANDROID_SDK_ROOT --licenses > /dev/null 2>&1
echo "✓ Licenses accepted"

# Install SDK components
echo "🔧 Installing Android SDK components..."
$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager --sdk_root=$ANDROID_SDK_ROOT \
  "platforms;android-35" \
  "build-tools;35.0.0" \
  "platform-tools" > /dev/null 2>&1
echo "✓ Android SDK components installed"

# Configure .bashrc with Android development environment
echo "🔐 Configuring shell environment..."
cat >> ~/.bashrc << 'BASHRC_EOF'

# Android Development Environment (Auto-configured by .devcontainer/setup-android-dev.sh)
export ANDROID_SDK_ROOT=$HOME/android-sdk
export ANDROID_HOME=$ANDROID_SDK_ROOT
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/emulator:$PATH

# Verify installation on shell startup
_android_dev_check_done=false
if [[ "$_android_dev_check_done" != "true" ]]; then
  export _android_dev_check_done=true
fi
BASHRC_EOF

echo "✓ Shell environment configured"

echo ""
echo "✅ Android Development Environment Setup Complete!"
echo ""
echo "📋 Installed Components:"
echo "   • Java 17 JDK: /usr/lib/jvm/java-17-openjdk-amd64"
echo "   • Android SDK: $ANDROID_SDK_ROOT"
echo "   • Build Tools: 35.0.0"
echo "   • Platforms: Android 35"
echo "   • Command-line Tools, Platform Tools, Emulator"
echo ""
echo "🚀 Ready for Android development!"
echo ""
