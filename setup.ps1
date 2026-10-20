param([string]$ToolchainRoot=(Join-Path $PSScriptRoot '..\toolchain'))
$ErrorActionPreference='Stop'
New-Item -ItemType Directory -Force $ToolchainRoot | Out-Null
$ToolchainRoot=(Resolve-Path -LiteralPath $ToolchainRoot).Path
$packages=Get-Content -Raw -LiteralPath "$PSScriptRoot\toolchain.json" | ConvertFrom-Json
foreach($package in $packages){
    $archive=Join-Path $ToolchainRoot $package.name
    if(-not(Test-Path -LiteralPath $archive)){
        Write-Output "Downloading $($package.name) from $($package.url)"
        Invoke-WebRequest -Uri $package.url -OutFile $archive
    }
    if((Get-FileHash -LiteralPath $archive -Algorithm $package.algorithm).Hash.ToLower() -ne $package.hash.ToLower()){throw "Checksum mismatch: $archive"}
    $destination=Join-Path $ToolchainRoot $package.folder
    if(-not(Test-Path -LiteralPath $destination)){Expand-Archive -LiteralPath $archive -DestinationPath $destination}
}
& "$PSScriptRoot\build.ps1" -ToolchainRoot $ToolchainRoot
