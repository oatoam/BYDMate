# build-all.ps1

# Exit on error
$ErrorActionPreference = 'Stop'

# 1. 编译 server 模块的 APK
Write-Host "Compiling server module APK..."
.\gradlew :server:assembleRelease
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to compile server module APK."
    exit $LASTEXITCODE
}
Write-Host "Server module APK compilation complete."

# 查找 server 模块编译后的 APK 文件
$serverApkDir = "server/build/outputs/apk/release"
$serverApkFile = Get-ChildItem -Path $serverApkDir -Filter "*.apk" | Select-Object -First 1
if (-not $serverApkFile) {
    Write-Error "Server module APK file not found in $serverApkDir."
    exit 1
}
$serverApkPath = $serverApkFile.FullName

# 2. 把 server APK 拷贝到 app 的 asset 目录
Write-Host "Copying server APK to app assets directory..."
$appAssetsDir = "app/src/main/assets"
if (-not (Test-Path $appAssetsDir)) {
    Write-Host "Creating app/src/main/assets directory..."
    New-Item -ItemType Directory -Path $appAssetsDir -Force | Out-Null
}

Copy-Item -Path $serverApkPath -Destination (Join-Path $appAssetsDir "bydmate.server.apk") -Force
Write-Host "Server APK copied to $appAssetsDir as bydmate.server.apk"

# Copy run.sh to assets
Write-Host "Copying server/run.sh to app assets directory as bydmate.run.sh..."
Copy-Item -Path "server/run.sh" -Destination (Join-Path $appAssetsDir "bydmate.run.sh") -Force
Write-Host "server/run.sh copied to $appAssetsDir as bydmate.run.sh"

# # 3. Compile app module APK
# Write-Host "Compiling app module APK..."
# .\gradlew :app:assembleRelease
# if ($LASTEXITCODE -ne 0) {
#     Write-Error "Failed to compile app module APK."
#     exit $LASTEXITCODE
# }
# Write-Host "App module APK compilation complete."

Write-Host "All tasks completed!"
