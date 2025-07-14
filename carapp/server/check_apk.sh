#!/bin/bash

# Extract and check APK contents
echo "Checking APK contents..."
unzip -l build/outputs/screensender.apk

# Check specific class
echo -e "\nChecking for ScreenCapture class..."
unzip -l build/outputs/screensender.apk | grep ScreenCapture 