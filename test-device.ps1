param([string]$ToolchainRoot=(Join-Path $PSScriptRoot '..\toolchain'),[switch]$RequireLegacy)
$ErrorActionPreference='Stop'
$ToolchainRoot=(Resolve-Path -LiteralPath $ToolchainRoot).Path
$jdk=Get-ChildItem -LiteralPath "$ToolchainRoot\jdk" -Directory | Select-Object -First 1
$env:JAVA_HOME=$jdk.FullName
$env:PATH="$($jdk.FullName)\bin;$env:PATH"
$tools="$ToolchainRoot\buildtools\android-16"
$platform="$ToolchainRoot\platform\android-36\android.jar"
$build="$PSScriptRoot\build\integration"
New-Item -ItemType Directory -Force "$build\classes","$build\dex" | Out-Null
function Assert-Exit {if($LASTEXITCODE -ne 0){throw "Command failed: $LASTEXITCODE"}}
& "$($jdk.FullName)\bin\javac.exe" --release 8 -encoding UTF-8 -cp "$platform;$PSScriptRoot\build\classes" -d "$build\classes" "$PSScriptRoot\tests\FolhaInstrumentation.java"
Assert-Exit
& "$($jdk.FullName)\bin\jar.exe" cf "$build\classes.jar" -C "$build\classes" .
Assert-Exit
& "$tools\d8.bat" --release --min-api 29 --lib $platform --classpath "$PSScriptRoot\build\classes.jar" --output "$build\dex" "$build\classes.jar"
Assert-Exit
& "$tools\aapt2.exe" link -I $platform --manifest "$PSScriptRoot\tests\AndroidManifest.xml" -o "$build\unsigned.apk"
Assert-Exit
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip=[System.IO.Compression.ZipFile]::Open("$build\unsigned.apk",'Update')
try{[System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,"$build\dex\classes.dex",'classes.dex') | Out-Null}finally{$zip.Dispose()}
& "$tools\zipalign.exe" -f 4 "$build\unsigned.apk" "$build\aligned.apk"
Assert-Exit
& "$tools\apksigner.bat" sign --ks "$ToolchainRoot\private\folha.p12" --ks-key-alias folha --ks-pass pass:folha-local-dev --out "$build\tests.apk" "$build\aligned.apk"
Assert-Exit
& "$ToolchainRoot\platform-tools\adb.exe" install -r "$build\tests.apk"
Assert-Exit
if($RequireLegacy){& "$ToolchainRoot\platform-tools\adb.exe" shell am instrument -w -e requireLegacy true br.com.folha.notes.tests/br.com.folha.notes.FolhaInstrumentation}
else{& "$ToolchainRoot\platform-tools\adb.exe" shell am instrument -w br.com.folha.notes.tests/br.com.folha.notes.FolhaInstrumentation}
Assert-Exit
