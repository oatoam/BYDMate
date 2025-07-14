#!/bin/bash

# Push the APK to device
adb push build/outputs/screensender.apk /data/local/tmp/

# Set executable permissions for run.sh
adb push run.sh /data/local/tmp/
adb shell chmod 755 /data/local/tmp/run.sh

# Execute the app
adb shell "/data/local/tmp/run.sh"