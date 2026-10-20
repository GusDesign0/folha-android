$ErrorActionPreference='Stop'
$taskBase=Join-Path $PSScriptRoot 'build/sync-tests'
New-Item -ItemType Directory -Force -Path "$taskBase/classes" | Out-Null
$taskJson=Join-Path $taskBase 'json-20250517.jar'
if(-not(Test-Path -LiteralPath $taskJson)){Invoke-WebRequest 'https://repo.maven.apache.org/maven2/org/json/json/20250517/json-20250517.jar' -OutFile $taskJson}
$taskJava=Join-Path $PSScriptRoot '../toolchain/jdk/jdk-21.0.12.1+1/bin'
& "$taskJava/javac.exe" --release 8 -encoding UTF-8 -cp $taskJson -d "$taskBase/classes" "$PSScriptRoot/src/br/com/folha/notes/DriveSync.java" "$PSScriptRoot/tests/SyncTest.java"
if($LASTEXITCODE -ne 0){throw 'Sync test compilation failed'}
$taskExpected=node -e "const c=require('node:crypto').createHash('sha256').update('folha-conflict:12345678-1234-1234-1234-123456789abc:00000000-1111-2222-3333-444444444444').digest('hex');console.log(c.slice(0,8)+'-'+c.slice(8,12)+'-'+c.slice(12,16)+'-'+c.slice(16,20)+'-'+c.slice(20,32))"
$taskRun=Join-Path $taskBase ([guid]::NewGuid().ToString())
& "$taskJava/java.exe" -cp "$taskBase/classes;$taskJson" br.com.folha.notes.SyncTest $taskRun $taskExpected
if($LASTEXITCODE -ne 0){throw 'Sync test scenarios failed'}
Write-Output "Wire fixture: $taskRun/wire"
