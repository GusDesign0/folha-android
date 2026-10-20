$ErrorActionPreference='Stop'
$deps=@(
    @{Name='pdfbox.aar';Url='https://repo.maven.apache.org/maven2/com/tom-roush/pdfbox-android/2.0.27.0/pdfbox-android-2.0.27.0.aar';Sha='30277f879cfd571db2a137582c95516a0d4ea6778e945519bc58ca93d57d88c7'},
    @{Name='bcprov.jar';Url='https://repo.maven.apache.org/maven2/org/bouncycastle/bcprov-jdk15to18/1.72/bcprov-jdk15to18-1.72.jar';Sha='ea66ea8a450810b2193e8bf9a7ad3e46307c9896224c0f407d1b7d96ba1221cc'},
    @{Name='bcpkix.jar';Url='https://repo.maven.apache.org/maven2/org/bouncycastle/bcpkix-jdk15to18/1.72/bcpkix-jdk15to18-1.72.jar';Sha='d9b97477b72499bcee02f5a906510810257ff36a94bf69fbca0b1e65e7ffdb6e'},
    @{Name='bcutil.jar';Url='https://repo.maven.apache.org/maven2/org/bouncycastle/bcutil-jdk15to18/1.72/bcutil-jdk15to18-1.72.jar';Sha='d92184bdeb3105a11ad9e36acbd66b5f8eed091b08b9c8f3e2549e42b7f131f1'}
)
New-Item -ItemType Directory -Force "$PSScriptRoot\libs","$PSScriptRoot\build\pdfbox","$PSScriptRoot\assets" | Out-Null
foreach($dep in $deps){$file=Join-Path "$PSScriptRoot\libs" $dep.Name;if(-not(Test-Path -LiteralPath $file)){Invoke-WebRequest -Uri $dep.Url -OutFile $file};if((Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash.ToLower() -ne $dep.Sha){throw "Invalid checksum for $($dep.Name)"}}
if(-not(Test-Path -LiteralPath "$PSScriptRoot\libs\pdfbox.jar") -or -not(Test-Path -LiteralPath "$PSScriptRoot\assets\com\tom_roush\pdfbox\resources\ttf\LiberationSans-Regular.ttf")){
    Copy-Item -LiteralPath "$PSScriptRoot\libs\pdfbox.aar" -Destination "$PSScriptRoot\build\pdfbox.zip" -Force
    Expand-Archive -LiteralPath "$PSScriptRoot\build\pdfbox.zip" -DestinationPath "$PSScriptRoot\build\pdfbox" -Force
    Copy-Item -LiteralPath "$PSScriptRoot\build\pdfbox\classes.jar" -Destination "$PSScriptRoot\libs\pdfbox.jar" -Force
    Copy-Item -Path "$PSScriptRoot\build\pdfbox\assets\*" -Destination "$PSScriptRoot\assets" -Recurse -Force
}
