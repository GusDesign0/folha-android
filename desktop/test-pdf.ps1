param([Parameter(Mandatory=$true)][string]$InputList,[string]$OutputDir=(Join-Path $PSScriptRoot 'qa-build'))
$ErrorActionPreference='Stop'
$InputList=(Resolve-Path -LiteralPath $InputList).Path
& "$PSScriptRoot/build.ps1" -OutputDir $OutputDir
Copy-Item -LiteralPath "$PSScriptRoot/tests/pdf-qa.html" -Destination "$OutputDir/web/pdf-qa.html" -Force
$taskReport=Join-Path (Resolve-Path -LiteralPath $OutputDir).Path 'pdf-results.json'
$taskProcess=Start-Process -FilePath "$OutputDir/Folha.exe" -ArgumentList @('--pdf-test',('"'+$InputList+'"'),('"'+$taskReport+'"')) -WindowStyle Hidden -PassThru -Wait
Get-Content -LiteralPath $taskReport
if($taskProcess.ExitCode -ne 0){throw 'PDF integration failed'}
