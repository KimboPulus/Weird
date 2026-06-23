$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$testOutput = Join-Path $root "out\test"
$imageOutput = Join-Path $root "out\visual-check.png"

& (Join-Path $PSScriptRoot "check.ps1")

java "-Djava.awt.headless=true" -cp "$($root)\out;$testOutput" com.kimbopulus.weird.VisualSmokeCheck $imageOutput
