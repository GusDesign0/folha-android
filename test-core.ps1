param([string]$ToolchainRoot=(Join-Path $PSScriptRoot '../toolchain'))
$ErrorActionPreference='Stop'
$taskJava=(Get-ChildItem -LiteralPath "$ToolchainRoot/jdk" -Directory | Select-Object -First 1).FullName
$taskClasses=Join-Path $PSScriptRoot 'build/core-tests'
New-Item -ItemType Directory -Force -Path $taskClasses | Out-Null
& "$taskJava/bin/javac.exe" --release 8 -encoding UTF-8 -d $taskClasses "$PSScriptRoot/src/br/com/folha/notes/Ink.java" "$PSScriptRoot/tests/InkTest.java" "$PSScriptRoot/src/br/com/folha/notes/TwoFingerTap.java" "$PSScriptRoot/src/br/com/folha/notes/ShapeRecognizer.java" "$PSScriptRoot/src/br/com/folha/notes/RulerGuide.java" "$PSScriptRoot/tests/GesturesShapesTest.java" "$PSScriptRoot/src/br/com/folha/notes/Paper.java" "$PSScriptRoot/tests/PaperTest.java" "$PSScriptRoot/src/br/com/folha/notes/GlyphData.java" "$PSScriptRoot/src/br/com/folha/notes/JapaneseGeometry.java" "$PSScriptRoot/tests/JapaneseTest.java"
if($LASTEXITCODE -ne 0){throw 'Core test compilation failed'}
& "$taskJava/bin/java.exe" -cp $taskClasses InkTest
if($LASTEXITCODE -ne 0){throw 'Ink scenarios failed'}
& "$taskJava/bin/java.exe" -cp $taskClasses br.com.folha.notes.GesturesShapesTest
if($LASTEXITCODE -ne 0){throw 'Gesture scenarios failed'}

& "$taskJava/bin/java.exe" -cp $taskClasses br.com.folha.notes.PaperTest --check
if($LASTEXITCODE -ne 0){throw 'Paper model scenarios failed'}

& "$taskJava/bin/java.exe" -cp $taskClasses br.com.folha.notes.JapaneseTest
if($LASTEXITCODE -ne 0){throw 'Japanese assist scenarios failed'}
