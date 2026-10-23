param(
 [string]$ToolchainRoot=(Join-Path $PSScriptRoot '../toolchain'),
 [string]$OutputApk=(Join-Path $PSScriptRoot 'Folha.apk')
)
$ErrorActionPreference='Stop'
& "$PSScriptRoot/fetch-deps.ps1"
& "$PSScriptRoot/test-core.ps1" -ToolchainRoot $ToolchainRoot
$ToolchainRoot=(Resolve-Path -LiteralPath $ToolchainRoot).Path
$taskJdk=Get-ChildItem -LiteralPath "$ToolchainRoot/jdk" -Directory | Select-Object -First 1
$env:JAVA_HOME=$taskJdk.FullName
$env:PATH="$($taskJdk.FullName)/bin;$env:PATH"
$taskGradle=Join-Path $ToolchainRoot 'gradle-8.13/bin/gradle.bat'
if(-not(Test-Path -LiteralPath $taskGradle)){throw 'Gradle 8.13 is required. See BUILD.md.'}
('sdk.dir='+$ToolchainRoot.Replace('\','/')) | Set-Content -LiteralPath "$PSScriptRoot/local.properties" -Encoding ASCII
& $taskGradle -p $PSScriptRoot assembleRelease --console=plain
if($LASTEXITCODE -ne 0){throw 'Android build failed'}
Copy-Item -LiteralPath "$PSScriptRoot/build/outputs/apk/release/Folha-arm64-v8a-release.apk" -Destination $OutputApk -Force
& "$ToolchainRoot/build-tools/36.0.0/apksigner.bat" verify --verbose --print-certs $OutputApk
if($LASTEXITCODE -ne 0){throw 'Signature verification failed'}
Write-Output "APK ready: $OutputApk"
