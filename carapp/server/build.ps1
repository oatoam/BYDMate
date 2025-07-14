# build.ps1

# Exit on error
$ErrorActionPreference = 'Stop'

# Configuration
$ANDROID_HOME = $env:ANDROID_HOME
if (-not $ANDROID_HOME) {
    $ANDROID_HOME = Join-Path $env:USERPROFILE "AppData\Local\Android\Sdk"
}
$BUILD_TOOLS_VERSION = "34.0.0"
$BUILD_TOOLS = Join-Path $ANDROID_HOME "build-tools\$BUILD_TOOLS_VERSION"
$PLATFORM = Join-Path $ANDROID_HOME "platforms\android-34"
$OUT_DIR = Join-Path $PWD "build\outputs"

# Create output directories
Write-Output "Creating output directories..."
New-Item -ItemType Directory -Path "$OUT_DIR\classes" -Force | Out-Null
New-Item -ItemType Directory -Path "$OUT_DIR\dex" -Force | Out-Null

# Compile Java sources
Write-Output "Compiling Java sources..."
& javac --release 11 `
      -classpath "$OUT_DIR\framework;$PLATFORM\android.jar" `
      -d "$OUT_DIR\classes" `
      (Get-ChildItem -Path "src\main\java\" -Recurse -Filter *.java).FullName

if ($LASTEXITCODE -ne 0) {
    Write-Output "Error compiling Java sources."
    exit $LASTEXITCODE
}

# Convert to DEX format
Write-Output "Converting to DEX format..."
& "$BUILD_TOOLS\d8" `
    --lib "$PLATFORM\android.jar" `
    --output "$OUT_DIR\dex" `
    (Get-ChildItem -Path "$OUT_DIR\classes\" -Filter *.class).FullName

if ($LASTEXITCODE -ne 0) {
    Write-Output "Error converting to DEX format."
    exit $LASTEXITCODE
}

# Create APK
Write-Output "Creating APK..."
& "$BUILD_TOOLS\aapt2" link -o "$OUT_DIR\datacollector-server.apk" `
    --manifest src\main\AndroidManifest.xml `
    -I "$PLATFORM\android.jar"

if ($LASTEXITCODE -ne 0) {
    Write-Output "Error creating APK."
    exit $LASTEXITCODE
}

# Add DEX to APK
Write-Output "Adding DEX to APK..."
& zip -uj "$OUT_DIR\datacollector-server.apk" "$OUT_DIR\dex\classes.dex"

if ($LASTEXITCODE -ne 0) {
    Write-Output "Error adding DEX to APK."
    exit $LASTEXITCODE
}

Write-Output "Build complete: $OUT_DIR\datacollector-server.apk"