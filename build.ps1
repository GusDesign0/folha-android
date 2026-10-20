param(
    [string]$ToolchainRoot = (Join-Path $PSScriptRoot '..\toolchain'),
    [string]$OutputApk = (Join-Path $PSScriptRoot 'Folha.apk')
)
$ErrorActionPreference = 'Stop'
& "$PSScriptRoot\fetch-deps.ps1"
$ToolchainRoot = (Resolve-Path -LiteralPath $ToolchainRoot).Path
$jdk = Get-ChildItem -LiteralPath (Join-Path $ToolchainRoot 'jdk') -Directory | Select-Object -First 1
$javaBin = Join-Path $jdk.FullName 'bin'
$env:JAVA_HOME = $jdk.FullName
$env:PATH = "$javaBin;$env:PATH"
$platform = Join-Path $ToolchainRoot 'platform\android-36\android.jar'
$tools = Join-Path $ToolchainRoot 'buildtools\android-16'
$build = Join-Path $PSScriptRoot 'build'
New-Item -ItemType Directory -Force "$build\classes", "$build\dex", "$build\tests", "$ToolchainRoot\private" | Out-Null
function Check-Exit([string]$step) { if ($LASTEXITCODE -ne 0) { throw "$step failed with exit code $LASTEXITCODE" } }
$sources = Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'src') -Filter '*.java' -Recurse | ForEach-Object FullName
& "$javaBin\javac.exe" -encoding UTF-8 --release 8 -classpath "$platform;$PSScriptRoot\libs\*" -d "$build\classes" @sources
Check-Exit 'Java compilation'
& "$javaBin\javac.exe" -encoding UTF-8 -d "$build\tests" (Join-Path $PSScriptRoot 'src\br\com\folha\notes\Ink.java') (Join-Path $PSScriptRoot 'tests\InkTest.java')
Check-Exit 'Test compilation'
& "$javaBin\java.exe" -cp "$build\tests" InkTest
Check-Exit 'Ink scenarios'
& "$tools\aapt2.exe" compile --dir (Join-Path $PSScriptRoot 'res') -o "$build\resources.zip"
Check-Exit 'Resource compilation'
& "$tools\aapt2.exe" link -o "$build\base.apk" -I $platform -A (Join-Path $PSScriptRoot 'assets') --manifest (Join-Path $PSScriptRoot 'AndroidManifest.xml') --min-sdk-version 29 --target-sdk-version 36 --auto-add-overlay "$build\resources.zip"
Check-Exit 'Resource linking'
& "$javaBin\jar.exe" cf "$build\classes.jar" -C "$build\classes" .
Check-Exit 'Class archive'
$dependencies=Get-ChildItem -LiteralPath "$PSScriptRoot\libs" -Filter '*.jar' | ForEach-Object FullName
& "$tools\d8.bat" --release --min-api 29 --lib $platform --output "$build\dex" "$build\classes.jar" @dependencies
Check-Exit 'DEX compilation'
Copy-Item -LiteralPath "$build\base.apk" -Destination "$build\unsigned.apk" -Force
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::Open("$build\unsigned.apk", 'Update')
try { foreach($dex in Get-ChildItem -LiteralPath "$build\dex" -Filter '*.dex') { [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,$dex.FullName,$dex.Name) | Out-Null } } finally { $zip.Dispose() }
& "$tools\zipalign.exe" -f -p 4 "$build\unsigned.apk" "$build\aligned.apk"
Check-Exit 'Alignment'
$key = Join-Path $ToolchainRoot 'private\folha.p12'
if (-not (Test-Path -LiteralPath $key)) {
    & "$javaBin\keytool.exe" -genkeypair -keystore $key -storepass folha-local-dev -keypass folha-local-dev -alias folha -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=Folha Local Development, O=Folha, C=BR' -storetype PKCS12
    Check-Exit 'Local signing key'
}
& "$tools\apksigner.bat" sign --ks $key --ks-key-alias folha --ks-pass pass:folha-local-dev --key-pass pass:folha-local-dev --v4-signing-enabled false --out $OutputApk "$build\aligned.apk"
Check-Exit 'APK signing'
& "$tools\apksigner.bat" verify --verbose $OutputApk
Check-Exit 'Signature verification'
& "$tools\aapt2.exe" dump badging $OutputApk
Check-Exit 'Manifest verification'
Write-Output "APK ready: $OutputApk"
