$ErrorActionPreference = 'Continue'
Set-Location -LiteralPath 'D:/alianximod'
@{ User = [System.Security.Principal.WindowsIdentity]::GetCurrent().Name; UserProfile = $env:USERPROFILE; PriorGradleUserHome = $env:GRADLE_USER_HOME; JavaHome = $env:JAVA_HOME } | ConvertTo-Json | Set-Content -LiteralPath 'D:/alianximod/docs/资源库/过程归档/水环接入-20261027/compile-environment.json' -Encoding utf8
$env:GRADLE_USER_HOME = 'C:/Users/baishang/.gradle'
& '.\gradlew.bat' compileJava --offline --no-daemon 2>&1 | Tee-Object -FilePath 'D:/alianximod/docs/资源库/过程归档/水环接入-20261027/compile-cache.log'
$compileExitCode = $LASTEXITCODE
Write-Output "GRADLE_EXIT_CODE=$compileExitCode"
exit $compileExitCode
