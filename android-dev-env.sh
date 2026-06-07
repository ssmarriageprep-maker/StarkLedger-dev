#!/bin/bash
# Android Development Environment Setup
# Source this script before building: source ./android-dev-env.sh

# Set Java 17 as default for Android development
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

# Configure Android SDK
export ANDROID_SDK_ROOT=$HOME/android-sdk
export ANDROID_HOME=$ANDROID_SDK_ROOT
export PATH=$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/emulator:$PATH

echo "✓ Android Development Environment Configured"
echo "  - Java: $(java -version 2>&1 | head -1 | cut -d' ' -f3)"
echo "  - SDK: $ANDROID_SDK_ROOT"
echo "  - Ready to build!"
