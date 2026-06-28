$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$testRoot = Join-Path $root "src\test\java"
$testOutput = Join-Path $root "out\test"

& (Join-Path $PSScriptRoot "build.ps1")

if (Test-Path $testOutput) {
    Remove-Item -Recurse -Force $testOutput
}

New-Item -ItemType Directory -Force $testOutput | Out-Null

$tests = Get-ChildItem -Path $testRoot -Filter *.java -Recurse | ForEach-Object { $_.FullName }
if ($tests.Count -eq 0) {
    throw "No smoke check files found."
}

javac -cp (Join-Path $root "out") -d $testOutput $tests
java -cp "$($root)\out;$testOutput" com.kimbopulus.weird.SimulationSmokeCheck
java -cp "$($root)\out;$testOutput" com.kimbopulus.weird.ModelRegressionCheck
java -cp "$($root)\out;$testOutput" com.kimbopulus.weird.TrainingSessionSmokeCheck
java -cp "$($root)\out;$testOutput" com.kimbopulus.weird.SettingsSmokeCheck
