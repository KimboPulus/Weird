$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$sourceRoot = Join-Path $root "src\main\java"
$output = Join-Path $root "out"

if (Test-Path $output) {
    Remove-Item -Recurse -Force $output
}

New-Item -ItemType Directory -Force $output | Out-Null

$sources = Get-ChildItem -Path $sourceRoot -Filter *.java -Recurse | ForEach-Object { $_.FullName }
if ($sources.Count -eq 0) {
    throw "No Java source files found."
}

javac -d $output $sources
Write-Host "Compiled $($sources.Count) source files to $output"

