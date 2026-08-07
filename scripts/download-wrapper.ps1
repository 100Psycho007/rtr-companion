# Downloads the Gradle wrapper JAR required to run ./gradlew
# Run once after cloning: .\scripts\download-wrapper.ps1

$wrapperDir  = "$PSScriptRoot\..\gradle\wrapper"
$wrapperJar  = "$wrapperDir\gradle-wrapper.jar"
$wrapperUrl  = "https://raw.githubusercontent.com/gradle/gradle/v8.7.0/gradle/wrapper/gradle-wrapper.jar"

if (Test-Path $wrapperJar) {
    Write-Host "gradle-wrapper.jar already exists — skipping download."
    exit 0
}

Write-Host "Downloading gradle-wrapper.jar..."
Invoke-WebRequest -Uri $wrapperUrl -OutFile $wrapperJar
Write-Host "Done. You can now run .\gradlew assembleDebug"
