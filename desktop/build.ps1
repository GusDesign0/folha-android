param([string]$OutputDir=(Join-Path $PSScriptRoot 'build'))
$ErrorActionPreference='Stop'
New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
$sdk=Join-Path $PSScriptRoot 'vendor/webview2'
$csc='C:/Windows/Microsoft.NET/Framework64/v4.0.30319/csc.exe'
$refs=@('System.dll','System.Core.dll','System.Drawing.dll','System.Windows.Forms.dll','System.Web.dll','System.Web.Extensions.dll','System.Security.dll','System.IO.Compression.dll','System.IO.Compression.FileSystem.dll',"$sdk/lib/net462/Microsoft.Web.WebView2.Core.dll","$sdk/lib/net462/Microsoft.Web.WebView2.WinForms.dll")
$argsCompile=@('/nologo','/target:winexe','/platform:x64','/optimize+',('/out:'+(Join-Path $OutputDir 'Folha.exe')))
foreach($reference in $refs){$argsCompile+=('/reference:'+$reference)}
$argsCompile+=(Get-ChildItem -LiteralPath $PSScriptRoot -Filter '*.cs' | ForEach-Object FullName)
& $csc @argsCompile
if($LASTEXITCODE -ne 0){throw 'Falha ao compilar Folha para Windows'}
Copy-Item -LiteralPath "$sdk/lib/net462/Microsoft.Web.WebView2.Core.dll","$sdk/lib/net462/Microsoft.Web.WebView2.WinForms.dll","$sdk/runtimes/win-x64/native/WebView2Loader.dll" -Destination $OutputDir -Force
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'web'),(Join-Path $PSScriptRoot 'licenses') -Destination $OutputDir -Recurse -Force
if(Test-Path -LiteralPath "$PSScriptRoot/google-client.json"){Copy-Item -LiteralPath "$PSScriptRoot/google-client.json" -Destination $OutputDir -Force}
@'
<?xml version="1.0" encoding="utf-8"?><configuration><startup useLegacyV2RuntimeActivationPolicy="true"><supportedRuntime version="v4.0" sku=".NETFramework,Version=v4.8"/></startup><runtime><AppContextSwitchOverrides value="Switch.System.Net.DontEnableSystemDefaultTlsVersions=false"/></runtime></configuration>
'@ | Set-Content -LiteralPath (Join-Path $OutputDir 'Folha.exe.config') -Encoding UTF8
$taskContent=Join-Path $PSScriptRoot '../assets/content'
if(-not(Test-Path -LiteralPath $taskContent)){$taskContent=Join-Path $PSScriptRoot '../app/assets/content'}
if(Test-Path -LiteralPath $taskContent){
 Copy-Item -LiteralPath $taskContent -Destination $OutputDir -Recurse -Force
 New-Item -ItemType Directory -Force (Join-Path $OutputDir 'web/content') | Out-Null
 Get-ChildItem -LiteralPath $taskContent -Filter '*.png' | Copy-Item -Destination (Join-Path $OutputDir 'web/content') -Force
}
Write-Output "Folha pronto em $OutputDir"
