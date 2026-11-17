@echo off
rem Offline entry for cmd. Does not change the user or machine execution policy.
powershell.exe -NoProfile -File "%~dp0build-windows.ps1" %*
exit /b %ERRORLEVEL%
