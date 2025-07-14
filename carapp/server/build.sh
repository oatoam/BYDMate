#!/bin/bash

# Exit on error
set -e

# Configuration
ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
BUILD_TOOLS_VERSION="34.0.0"
BUILD_TOOLS="$ANDROID_HOME/build-tools/$BUILD_TOOLS_VERSION"
PLATFORM="$ANDROID_HOME/platforms/android-34"
OUT_DIR="build/outputs"

# Create output directories
mkdir -p "$OUT_DIR/classes"
mkdir -p "$OUT_DIR/dex"

# Create a directory for extracted framework classes
# mkdir -p "$OUT_DIR/framework"
# cd "$OUT_DIR/framework"
# unzip -q "$PLATFORM/android.jar"
# rm -f META-INF/MANIFEST.MF
# cd ../../

# Compile Java sources
echo "Compiling Java sources..."
javac -source 11 -target 11 \
      -classpath "$OUT_DIR/framework:$PLATFORM/android.jar" \
      -d "$OUT_DIR/classes" \
      $(find src/main -name "*.java")
# src/main/java/com/toddmo/screensender/ScreenCapture.java

# Convert to DEX format
echo "Converting to DEX format..."
"$BUILD_TOOLS/d8" \
    --lib "$PLATFORM/android.jar" \
    --output "$OUT_DIR/dex" \
    $(find "$OUT_DIR/classes" -name "*.class")

# Create APK
echo "Creating APK..."
"$BUILD_TOOLS/aapt2" link -o "$OUT_DIR/screensender.apk" \
    --manifest src/main/AndroidManifest.xml \
    -I "$PLATFORM/android.jar"

# Add DEX to APK
echo "Adding DEX to APK..."
zip -uj "$OUT_DIR/screensender.apk" "$OUT_DIR/dex/classes.dex"

echo "Build complete: $OUT_DIR/screensender.apk"

