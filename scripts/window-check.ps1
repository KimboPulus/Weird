$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$testOutput = Join-Path $root "out\test"
$imageOutput = Join-Path $root "out\window-check.png"

& (Join-Path $PSScriptRoot "check.ps1")

java -cp "$($root)\out;$testOutput" com.kimbopulus.weird.WindowVisualCheck $imageOutput

