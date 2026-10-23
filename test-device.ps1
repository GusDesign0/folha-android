param([string]$ToolchainRoot=(Join-Path $PSScriptRoot '..\toolchain'),[switch]$RequireLegacy,[switch]$UpdaterOnly,[string]$Device='emulator-5554')
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
& "$($jdk.FullName)\bin\javac.exe" --release 8 -encoding UTF-8 -cp "$platform;$PSScriptRoot\build\intermediates\javac\release\compileReleaseJavaWithJavac\classes;$PSScriptRoot\libs\*" -d "$build\classes" "$PSScriptRoot\tests\FolhaInstrumentation.java"
Assert-Exit
& "$($jdk.FullName)\bin\jar.exe" cf "$PSScriptRoot\build\app-classes-tests.jar" -C "$PSScriptRoot\build\intermediates\javac\release\compileReleaseJavaWithJavac\classes" .
Assert-Exit
& "$($jdk.FullName)\bin\jar.exe" cf "$build\classes.jar" -C "$build\classes" .
Assert-Exit
& "$tools\d8.bat" --release --min-api 29 --lib $platform --classpath "$PSScriptRoot\build\intermediates\compile_and_runtime_not_namespaced_r_class_jar\release\processReleaseResources\R.jar" --classpath "$PSScriptRoot\build\app-classes-tests.jar" --output "$build\dex" "$build\classes.jar"
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
& "$ToolchainRoot\platform-tools\adb.exe" -s $Device install -r "$build\tests.apk"
Assert-Exit
if(Test-Path -LiteralPath "$PSScriptRoot\tests\fixtures\fixture-preservar.pdf"){
    & "$ToolchainRoot\platform-tools\adb.exe" -s $Device shell mkdir -p /sdcard/Android/data/br.com.folha.notes/files
    & "$ToolchainRoot\platform-tools\adb.exe" -s $Device push "$PSScriptRoot\tests\fixtures\fixture-preservar.pdf" /sdcard/Android/data/br.com.folha.notes/files/fixture-preservar.pdf
    Assert-Exit
}
# The future-version APK is parsed and intercepted by an instrumentation monitor; it is NEVER installed.
& "$tools/aapt2.exe" link -I $platform --manifest "$PSScriptRoot/tests/fixtures/update-next.xml" -o "$build/update-next-unsigned.apk"
Assert-Exit
& "$tools/zipalign.exe" -f 4 "$build/update-next-unsigned.apk" "$build/update-next-aligned.apk"
Assert-Exit
& "$tools/apksigner.bat" sign --ks "$ToolchainRoot/private/folha.p12" --ks-key-alias folha --ks-pass pass:folha-local-dev --out "$build/update-next.apk" "$build/update-next-aligned.apk"
Assert-Exit
& "$ToolchainRoot/platform-tools/adb.exe" -s $Device push "$build/update-next.apk" /sdcard/Android/data/br.com.folha.notes/files/update-next.apk
Assert-Exit
& "$ToolchainRoot/platform-tools/adb.exe" -s $Device shell appops set br.com.folha.notes REQUEST_INSTALL_PACKAGES allow
Assert-Exit
if($UpdaterOnly){$taskResults=& "$ToolchainRoot/platform-tools/adb.exe" -s $Device shell am instrument -w -e updaterOnly true br.com.folha.notes.tests/br.com.folha.notes.FolhaInstrumentation}
elseif($RequireLegacy){$taskResults=& "$ToolchainRoot\platform-tools\adb.exe" -s $Device shell am instrument -w -e requireLegacy true br.com.folha.notes.tests/br.com.folha.notes.FolhaInstrumentation}
else{$taskResults=& "$ToolchainRoot\platform-tools\adb.exe" -s $Device shell am instrument -w br.com.folha.notes.tests/br.com.folha.notes.FolhaInstrumentation}
Assert-Exit
$taskResults | Write-Output
if(($taskResults -join "`n") -notmatch 'PASS:' -or ($taskResults -join "`n") -match 'FAIL:|Process crashed'){throw 'Native integration tests did not complete successfully'}
