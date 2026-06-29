$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$buildRoot = Join-Path $root "out"
$releaseRoot = Join-Path $root "release"
$tempRoot = Join-Path $releaseRoot "_temp"
$inputRoot = Join-Path $tempRoot "input"
$jarPath = Join-Path $inputRoot "Weird.jar"
$manifestPath = Join-Path $tempRoot "manifest.mf"
$musicSource = Join-Path $root "data\music\domowka-theme.wav"
$appRoot = Join-Path $releaseRoot "Weird"
$zipPath = Join-Path $releaseRoot "Weird-release.zip"

if (Test-Path $releaseRoot) {
    Remove-Item -Recurse -Force $releaseRoot
}

New-Item -ItemType Directory -Force $inputRoot | Out-Null

& (Join-Path $PSScriptRoot "build.ps1")

@"
Main-Class: com.kimbopulus.weird.Main
"@ | Set-Content -Encoding ASCII $manifestPath

jar --create --file $jarPath --manifest $manifestPath -C $buildRoot .

jpackage `
    --type app-image `
    --name Weird `
    --dest $releaseRoot `
    --input $inputRoot `
    --main-jar Weird.jar `
    --main-class com.kimbopulus.weird.Main `
    --app-version 1.0.0 `
    --vendor KimboPulus

$musicDest = Join-Path $appRoot "data\music"
New-Item -ItemType Directory -Force $musicDest | Out-Null
Copy-Item $musicSource (Join-Path $musicDest "domowka-theme.wav") -Force
Copy-Item (Join-Path $root "data\music\lightning.wav") (Join-Path $musicDest "lightning.wav") -Force
Copy-Item (Join-Path $root "data\music\u_39xav15uou-lightning-237994.mp3") (Join-Path $musicDest "u_39xav15uou-lightning-237994.mp3") -Force

$launcher = Join-Path $releaseRoot "Launch Weird.bat"
@"
@echo off
setlocal
cd /d "%~dp0Weird"
start "" Weird.exe
"@ | Set-Content -Encoding ASCII $launcher

Compress-Archive -Path $appRoot, $launcher -DestinationPath $zipPath -Force

Remove-Item -Recurse -Force $tempRoot
Write-Host "Release created at $zipPath"
