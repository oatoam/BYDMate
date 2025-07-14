# deploy.ps1

# Push the APK to device
Write-Output "Pushing APK to device..."
& adb push "build\outputs\apk\debug\server-debug.apk" "/data/local/tmp/bydmate.server.apk"

if ($LASTEXITCODE -ne 0) {
    Write-Output "Error pushing APK to device."
    exit $LASTEXITCODE
}

# Push run.sh to device
Write-Output "Pushing run.sh to device..."
& adb push "run.sh" "/data/local/tmp/"

if ($LASTEXITCODE -ne 0) {
    Write-Output "Error pushing run.sh to device."
    exit $LASTEXITCODE
}

# Set executable permissions for run.sh
Write-Output "Setting executable permissions for run.sh..."
& adb shell "chmod 755 /data/local/tmp/run.sh"

if ($LASTEXITCODE -ne 0) {
    Write-Output "Error setting executable permissions for run.sh."
    exit $LASTEXITCODE
}

# Execute the app
Write-Output "Executing the app..."
& adb shell "/data/local/tmp/run.sh"

if ($LASTEXITCODE -ne 0) {
    Write-Output "Error executing the app."
    exit $LASTEXITCODE
}

Write-Output "Deployment complete."