param([string]$ToolchainRoot=(Join-Path $PSScriptRoot '../toolchain'))
$ErrorActionPreference='Stop'
New-Item -ItemType Directory -Force -Path $ToolchainRoot | Out-Null
$ToolchainRoot=(Resolve-Path -LiteralPath $ToolchainRoot).Path
Add-Type -AssemblyName System.IO.Compression.FileSystem
$packages=Get-Content -Raw -LiteralPath "$PSScriptRoot/toolchain.json" | ConvertFrom-Json
foreach($package in $packages){
    $archive=Join-Path $ToolchainRoot $package.name
    if(-not(Test-Path -LiteralPath $archive)){Invoke-WebRequest -Uri $package.url -OutFile $archive}
    if((Get-FileHash -LiteralPath $archive -Algorithm $package.algorithm).Hash.ToLower() -ne $package.hash.ToLower()){throw "Checksum mismatch: $archive"}
    $destination=Join-Path $ToolchainRoot $package.folder
    if($package.name -eq 'gradle-8.13-bin.zip'){if(-not(Test-Path -LiteralPath "$ToolchainRoot/gradle-8.13")){[System.IO.Compression.ZipFile]::ExtractToDirectory($archive,$ToolchainRoot)};continue}
    if(-not(Test-Path -LiteralPath $destination)){[System.IO.Compression.ZipFile]::ExtractToDirectory($archive,$destination)}
}
if(-not(Test-Path -LiteralPath "$ToolchainRoot/platforms")){New-Item -ItemType Junction -Path "$ToolchainRoot/platforms" -Value "$ToolchainRoot/platform" | Out-Null}
New-Item -ItemType Directory -Force -Path "$ToolchainRoot/build-tools" | Out-Null
if(-not(Test-Path -LiteralPath "$ToolchainRoot/build-tools/36.0.0")){New-Item -ItemType Junction -Path "$ToolchainRoot/build-tools/36.0.0" -Value "$ToolchainRoot/buildtools/android-16" | Out-Null}
Write-Output 'SDK e Gradle prontos. Configure sua chave privada de assinatura em build.gradle e execute build.ps1.'
