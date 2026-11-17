#requires -Version 5.1
<#
.SYNOPSIS
  Offline x64 build of xueximod_compute.dll from src/xueximod_compute.c.
  Exports Java_com_bslx_XueXiMod_shu_compute_XuexiComputeNative_handshake,
  trajectoryBatch, turnBatch, trackingStepBatch, corridorOverlapsBatch,
  corridorContactBatch, and corridorBlockedBatch. x64 leaves those names undecorated.

.DESCRIPTION
  Discovers vswhere, vcvars64.bat, CMake, and a JDK 17 with JNI headers.
  Does not download tools, change the user or machine PATH or JAVA_HOME,
  copy the DLL into the game or resource directories, or link the JVM.
  Repository, JDK, and tool paths stay Unicode: vcvars64 runs in a child,
  its environment is read back as UTF-16, and cmake.exe is started directly.
  Paths are not written into a temporary batch file.
  Exits 2 while src/xueximod_compute.c is still absent.
#>
[CmdletBinding()]
param(
    [string]$JdkHome,
    [string]$VsInstall,
    [string]$Vcvars64,
    [string]$Cmake,
    [string]$Ninja,
    [string]$VsWhere,
    [string]$BuildDir,
    [ValidateSet('Release', 'RelWithDebInfo', 'Debug')]
    [string]$Configuration = 'Release',
    [string]$Generator,
    [switch]$Clean
)

$ErrorActionPreference = 'Stop'

function Write-Info {
    param([string]$Message)
    Write-Host "[xueximod_compute] $Message"
}

function Exit-Build {
    param(
        [int]$Code,
        [string]$Message
    )
    Write-Host "[xueximod_compute] ERROR ${Code}: $Message"
    exit $Code
}

function Assert-NoCmdMeta {
    param(
        [string]$Path,
        [string]$Label
    )
    if ($Path -match '["&|<>^%]') {
        Exit-Build 9 "Refusing $Label because it contains cmd metacharacters: $Path"
    }
}

function Format-ProcessArgument {
    param([string]$Text)
    if ([string]::IsNullOrEmpty($Text)) {
        return '""'
    }
    if ($Text -notmatch '[\s"]') {
        return $Text
    }
    $escaped = ''
    $pendingSlash = 0
    foreach ($character in $Text.ToCharArray()) {
        if ($character -eq '\') {
            $pendingSlash++
            continue
        }
        if ($character -eq '"') {
            $escaped += ('\' * (($pendingSlash * 2) + 1)) + '"'
            $pendingSlash = 0
            continue
        }
        if ($pendingSlash -gt 0) {
            $escaped += ('\' * $pendingSlash)
            $pendingSlash = 0
        }
        $escaped += $character
    }
    if ($pendingSlash -gt 0) {
        $escaped += ('\' * ($pendingSlash * 2))
    }
    return '"' + $escaped + '"'
}

function Get-VcvarsEnvironment {
    param([string]$VcvarsPath)
    $marker = 'XUEXIMOD_ENV_BEGIN_' + [guid]::NewGuid().ToString('N')
    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = Join-Path $env:SystemRoot 'System32\cmd.exe'
    $startInfo.Arguments = '/d /u /c call "%XUEXIMOD_VCVARS%" && echo ' + $marker + ' && set'
    $startInfo.UseShellExecute = $false
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $startInfo.StandardOutputEncoding = [System.Text.Encoding]::Unicode
    $startInfo.EnvironmentVariables['XUEXIMOD_VCVARS'] = $VcvarsPath
    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $startInfo
    [void]$process.Start()
    $outputTask = $process.StandardOutput.ReadToEndAsync()
    $errorTask = $process.StandardError.ReadToEndAsync()
    $process.WaitForExit()
    $errorText = $errorTask.Result
    if ($errorText) {
        foreach ($errorLine in ($errorText -split '\r?\n')) {
            if ($errorLine -ne '') {
                Write-Info "vcvars: $errorLine"
            }
        }
    }
    return [pscustomobject]@{
        ExitCode = $process.ExitCode
        Marker = $marker
        OutputText = $outputTask.Result
    }
}

function ConvertFrom-EnvironmentDump {
    param(
        [string]$OutputText,
        [string]$Marker
    )
    $dumpLines = $OutputText -split '\r?\n'
    $markerIndex = -1
    for ($lineIndex = 0; $lineIndex -lt $dumpLines.Length; $lineIndex++) {
        if ($dumpLines[$lineIndex].Trim() -eq $Marker) {
            $markerIndex = $lineIndex
            break
        }
    }
    if ($markerIndex -lt 0) {
        return $null
    }
    for ($lineIndex = 0; $lineIndex -lt $markerIndex; $lineIndex++) {
        if ($dumpLines[$lineIndex] -ne '') {
            Write-Info "vcvars: $($dumpLines[$lineIndex])"
        }
    }
    $environmentMap = New-Object 'System.Collections.Generic.Dictionary[string,string]' -ArgumentList ([System.StringComparer]::OrdinalIgnoreCase)
    for ($lineIndex = $markerIndex + 1; $lineIndex -lt $dumpLines.Length; $lineIndex++) {
        $dumpLine = $dumpLines[$lineIndex]
        $splitAt = $dumpLine.IndexOf('=')
        if ($splitAt -le 0) {
            continue
        }
        $variableName = $dumpLine.Substring(0, $splitAt)
        $variableValue = $dumpLine.Substring($splitAt + 1)
        $environmentMap[$variableName] = $variableValue
    }
    [void]$environmentMap.Remove('XUEXIMOD_VCVARS')
    return $environmentMap
}

function Find-FirstCompiler {
    param($EnvironmentMap)
    if (-not $EnvironmentMap.ContainsKey('Path')) {
        return $null
    }
    foreach ($pathEntry in ($EnvironmentMap['Path'] -split ';')) {
        $trimmedEntry = $pathEntry.Trim().Trim('"')
        if ([string]::IsNullOrWhiteSpace($trimmedEntry)) {
            continue
        }
        $compilerCandidate = Join-Path $trimmedEntry 'cl.exe'
        if (Test-Path -LiteralPath $compilerCandidate) {
            return [System.IO.Path]::GetFullPath($compilerCandidate)
        }
    }
    return $null
}

function Start-CapturedCommand {
    param(
        [string]$FilePath,
        [string]$ArgumentText,
        $EnvironmentMap
    )
    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = $FilePath
    $startInfo.Arguments = $ArgumentText
    $startInfo.UseShellExecute = $false
    foreach ($variableName in @($EnvironmentMap.Keys)) {
        $variableValue = $EnvironmentMap[$variableName]
        if ($null -eq $variableValue) {
            $variableValue = ''
        }
        try {
            $startInfo.EnvironmentVariables[$variableName] = [string]$variableValue
        } catch {
            Exit-Build 4 "Could not apply captured environment variable $variableName."
        }
    }
    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $startInfo
    [void]$process.Start()
    $process.WaitForExit()
    return $process.ExitCode
}

function Test-Jdk17Home {
    param([string]$Root)
    if ([string]::IsNullOrWhiteSpace($Root)) {
        return $false
    }
    $jni = Join-Path $Root 'include\jni.h'
    $md = Join-Path $Root 'include\win32\jni_md.h'
    $release = Join-Path $Root 'release'
    if (-not (Test-Path -LiteralPath $jni) -or -not (Test-Path -LiteralPath $md) -or -not (Test-Path -LiteralPath $release)) {
        return $false
    }
    $text = Get-Content -LiteralPath $release -Raw
    return $text -match 'JAVA_VERSION="17(\.|")'
}

$ComputeRoot = [System.IO.Path]::GetFullPath($PSScriptRoot)
Assert-NoCmdMeta -Path $ComputeRoot -Label 'compute root'
$Source = Join-Path $ComputeRoot 'src\xueximod_compute.c'

Write-Info "Offline Windows x64 build. No download, no PATH/JAVA_HOME change, no copy into the game."
if (-not (Test-Path -LiteralPath $Source)) {
    Exit-Build 2 "C source is not present: $Source. Expected src/xueximod_compute.c with the ABI 1 JNI exports. This script does not create a replacement file."
}

if ([string]::IsNullOrWhiteSpace($BuildDir) -or $BuildDir -eq 'build') {
    $BuildFull = [System.IO.Path]::GetFullPath((Join-Path $ComputeRoot 'build'))
} elseif ($BuildDir -eq 'out') {
    $BuildFull = [System.IO.Path]::GetFullPath((Join-Path $ComputeRoot 'out'))
} else {
    $candidate = $BuildDir
    if (-not [System.IO.Path]::IsPathRooted($candidate)) {
        $candidate = Join-Path $ComputeRoot $candidate
    }
    $BuildFull = [System.IO.Path]::GetFullPath($candidate)
    $allowedBuild = [System.IO.Path]::GetFullPath((Join-Path $ComputeRoot 'build'))
    $allowedOut = [System.IO.Path]::GetFullPath((Join-Path $ComputeRoot 'out'))
    $sameBuild = $BuildFull.Equals($allowedBuild, [System.StringComparison]::OrdinalIgnoreCase)
    $sameOut = $BuildFull.Equals($allowedOut, [System.StringComparison]::OrdinalIgnoreCase)
    if (-not $sameBuild -and -not $sameOut) {
        Exit-Build 9 "Refusing build directory '$BuildFull'. Only native/compute/build or native/compute/out is allowed."
    }
}
Assert-NoCmdMeta -Path $BuildFull -Label 'build directory'

function Find-VsWhere {
    if (-not [string]::IsNullOrWhiteSpace($VsWhere)) {
        if (-not (Test-Path -LiteralPath $VsWhere)) {
            Exit-Build 4 "VsWhere does not exist: $VsWhere"
        }
        return [System.IO.Path]::GetFullPath($VsWhere)
    }
    $candidates = @()
    if (${env:ProgramFiles(x86)}) {
        $candidates += (Join-Path ${env:ProgramFiles(x86)} 'Microsoft Visual Studio\Installer\vswhere.exe')
    }
    if ($env:ProgramFiles) {
        $candidates += (Join-Path $env:ProgramFiles 'Microsoft Visual Studio\Installer\vswhere.exe')
    }
    foreach ($candidate in $candidates) {
        if (Test-Path -LiteralPath $candidate) {
            return $candidate
        }
    }
    $command = Get-Command vswhere.exe -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }
    return $null
}

function Get-VsWhereProperty {
    param(
        [string]$Locator,
        [string]$PropertyName
    )
    $value = & $Locator -latest -products * -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property $PropertyName
    if ($LASTEXITCODE -ne 0 -or -not $value) {
        return $null
    }
    return (($value | Select-Object -First 1).ToString().Trim())
}

$locator = Find-VsWhere
if (-not [string]::IsNullOrWhiteSpace($VsInstall)) {
    if (-not (Test-Path -LiteralPath $VsInstall)) {
        Exit-Build 4 "VsInstall does not exist: $VsInstall"
    }
    $VsRoot = [System.IO.Path]::GetFullPath($VsInstall)
} else {
    if (-not $locator) {
        Exit-Build 4 "vswhere.exe was not found. Pass -VsInstall or -VsWhere. This script does not download Visual Studio."
    }
    Write-Info "vswhere: $locator"
    $VsRoot = Get-VsWhereProperty -Locator $locator -PropertyName 'installationPath'
    if (-not $VsRoot -or -not (Test-Path -LiteralPath $VsRoot)) {
        Exit-Build 4 "vswhere found no Visual C++ x64 toolset. This script does not install components."
    }
}
Assert-NoCmdMeta -Path $VsRoot -Label 'Visual Studio install'
Write-Info "Visual Studio: $VsRoot"

if (-not [string]::IsNullOrWhiteSpace($Vcvars64)) {
    $leaf = [System.IO.Path]::GetFileName($Vcvars64)
    if ($leaf -ne 'vcvars64.bat') {
        Exit-Build 6 "Refusing '$Vcvars64'. Only vcvars64.bat keeps the host and target at x64."
    }
    if (-not (Test-Path -LiteralPath $Vcvars64)) {
        Exit-Build 4 "Vcvars64 does not exist: $Vcvars64"
    }
    $VcvarsPath = [System.IO.Path]::GetFullPath($Vcvars64)
} else {
    $VcvarsPath = Join-Path $VsRoot 'VC\Auxiliary\Build\vcvars64.bat'
    if (-not (Test-Path -LiteralPath $VcvarsPath)) {
        Exit-Build 4 "vcvars64.bat not found at $VcvarsPath"
    }
}
Assert-NoCmdMeta -Path $VcvarsPath -Label 'vcvars64.bat'
Write-Info "vcvars64: $VcvarsPath"

$resolvedJdk = $null
if (-not [string]::IsNullOrWhiteSpace($JdkHome)) {
    $resolvedJdk = [System.IO.Path]::GetFullPath($JdkHome)
} else {
    foreach ($envName in @('JDK_HOME', 'JAVA_HOME')) {
        $envValue = [Environment]::GetEnvironmentVariable($envName, 'Process')
        if ([string]::IsNullOrWhiteSpace($envValue)) {
            $envValue = [Environment]::GetEnvironmentVariable($envName, 'User')
        }
        if ([string]::IsNullOrWhiteSpace($envValue)) {
            $envValue = [Environment]::GetEnvironmentVariable($envName, 'Machine')
        }
        if (Test-Jdk17Home $envValue) {
            $resolvedJdk = [System.IO.Path]::GetFullPath($envValue)
            break
        }
    }
}
if (-not $resolvedJdk) {
    $scanRoots = @()
    if ($env:ProgramFiles) {
        $scanRoots += (Join-Path $env:ProgramFiles 'Java')
        $scanRoots += (Join-Path $env:ProgramFiles 'Eclipse Adoptium')
        $scanRoots += (Join-Path $env:ProgramFiles 'Microsoft')
        $scanRoots += (Join-Path $env:ProgramFiles 'Amazon Corretto')
    }
    foreach ($scanRoot in $scanRoots) {
        if (-not (Test-Path -LiteralPath $scanRoot)) {
            continue
        }
        $dirs = Get-ChildItem -LiteralPath $scanRoot -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending
        foreach ($dir in $dirs) {
            if (Test-Jdk17Home $dir.FullName) {
                $resolvedJdk = $dir.FullName
                break
            }
        }
        if ($resolvedJdk) {
            break
        }
    }
}
if (-not (Test-Jdk17Home $resolvedJdk)) {
    Exit-Build 3 "JDK 17 with include/jni.h and include/win32/jni_md.h was not found. Pass -JdkHome. JAVA_HOME was not modified. A JRE is not enough, and jvm.lib is not linked."
}
Assert-NoCmdMeta -Path $resolvedJdk -Label 'JDK home'
Write-Info "JDK 17: $resolvedJdk"

if (-not [string]::IsNullOrWhiteSpace($Cmake)) {
    if (-not (Test-Path -LiteralPath $Cmake)) {
        Exit-Build 5 "Cmake does not exist: $Cmake"
    }
    $CmakePath = [System.IO.Path]::GetFullPath($Cmake)
} else {
    $bundledCmake = Join-Path $VsRoot 'Common7\IDE\CommonExtensions\Microsoft\CMake\CMake\bin\cmake.exe'
    if (Test-Path -LiteralPath $bundledCmake) {
        $CmakePath = $bundledCmake
    } else {
        $cmakeCommand = Get-Command cmake.exe -ErrorAction SilentlyContinue
        if ($cmakeCommand) {
            $CmakePath = $cmakeCommand.Source
        } else {
            Exit-Build 5 "cmake.exe was not found. Pass -Cmake. This script does not download CMake."
        }
    }
}
Assert-NoCmdMeta -Path $CmakePath -Label 'cmake'
Write-Info "CMake: $CmakePath"

$NinjaPath = $null
if (-not [string]::IsNullOrWhiteSpace($Ninja)) {
    if (-not (Test-Path -LiteralPath $Ninja)) {
        Exit-Build 5 "Ninja does not exist: $Ninja"
    }
    $NinjaPath = [System.IO.Path]::GetFullPath($Ninja)
} else {
    $bundledNinja = Join-Path $VsRoot 'Common7\IDE\CommonExtensions\Microsoft\CMake\Ninja\ninja.exe'
    if (Test-Path -LiteralPath $bundledNinja) {
        $NinjaPath = $bundledNinja
    } else {
        $ninjaCommand = Get-Command ninja.exe -ErrorAction SilentlyContinue
        if ($ninjaCommand) {
            $NinjaPath = $ninjaCommand.Source
        }
    }
}
if ($NinjaPath) {
    Assert-NoCmdMeta -Path $NinjaPath -Label 'ninja'
    Write-Info "Ninja: $NinjaPath"
}

if ([string]::IsNullOrWhiteSpace($Generator)) {
    if ($NinjaPath) {
        $Generator = 'Ninja'
    } else {
        $vsVersion = $null
        if ($locator) {
            $vsVersion = Get-VsWhereProperty -Locator $locator -PropertyName 'installationVersion'
        }
        if ($vsVersion -like '17.*') {
            $Generator = 'Visual Studio 17 2022'
        } else {
            Exit-Build 4 "Ninja was not found and Visual Studio is not version 17 (got '$vsVersion'). Pass -Generator. This script does not download Ninja."
        }
    }
}
if ($Generator -match 'Win32|ARM|x86') {
    Exit-Build 6 "Refusing generator '$Generator'. The DLL must be x64."
}
$isNinja = $Generator -eq 'Ninja'
$isVisualStudio = $Generator -like 'Visual Studio *'
if (-not $isNinja -and -not $isVisualStudio) {
    Exit-Build 4 "Unsupported generator '$Generator'. Use Ninja or 'Visual Studio 17 2022'."
}
if ($isNinja -and -not $NinjaPath) {
    Exit-Build 5 "Generator Ninja requires ninja.exe. Pass -Ninja. This script does not download Ninja."
}
Write-Info "Generator: $Generator"
Write-Info "Configuration: $Configuration"

if (Test-Path -LiteralPath $BuildFull) {
    if (-not (Test-Path -LiteralPath $BuildFull -PathType Container)) {
        Exit-Build 9 "Refusing to replace a non-directory at $BuildFull"
    }
}
if ($Clean) {
    if (Test-Path -LiteralPath $BuildFull) {
        Write-Info "Removing only the allowed build directory: $BuildFull"
        Remove-Item -LiteralPath $BuildFull -Recurse -Force
    }
}
New-Item -ItemType Directory -Path $BuildFull -Force | Out-Null

Write-Info "Checking cl after vcvars64 (no compile)."
$captured = Get-VcvarsEnvironment -VcvarsPath $VcvarsPath
if ($captured.ExitCode -ne 0) {
    Exit-Build 4 "vcvars64.bat failed with exit $($captured.ExitCode)."
}
$environmentMap = ConvertFrom-EnvironmentDump -OutputText $captured.OutputText -Marker $captured.Marker
if ($null -eq $environmentMap) {
    Exit-Build 4 "vcvars64.bat did not produce an environment listing."
}
$clLine = Find-FirstCompiler -EnvironmentMap $environmentMap
if (-not $clLine) {
    Exit-Build 4 "vcvars64.bat ran but cl.exe was not on PATH inside that process."
}
if ($clLine -notmatch '(?i)\\Hostx64\\x64\\cl\.exe$') {
    Exit-Build 6 "Refusing compiler '$clLine'. Expected Hostx64\\x64\\cl.exe to match the 64-bit JVM."
}
Write-Info "x64 compiler: $clLine"

$configureParts = New-Object System.Collections.Generic.List[string]
$configureParts.Add('-S')
$configureParts.Add((Format-ProcessArgument $ComputeRoot))
$configureParts.Add('-B')
$configureParts.Add((Format-ProcessArgument $BuildFull))
$configureParts.Add('-G')
$configureParts.Add((Format-ProcessArgument $Generator))
if ($isNinja) {
    $configureParts.Add((Format-ProcessArgument ('-DCMAKE_MAKE_PROGRAM=' + $NinjaPath)))
    $configureParts.Add((Format-ProcessArgument ('-DCMAKE_BUILD_TYPE=' + $Configuration)))
} else {
    $configureParts.Add('-A')
    $configureParts.Add('x64')
    $configureParts.Add('-T')
    $configureParts.Add('host=x64')
}
$configureParts.Add((Format-ProcessArgument ('-DJDK_HOME=' + $resolvedJdk)))
$configureArguments = ($configureParts.ToArray() -join ' ')
Write-Info "Configuring. JNI headers only; the JVM library is not linked."
Write-Info "CMake arguments: $configureArguments"
$configureCode = Start-CapturedCommand -FilePath $CmakePath -ArgumentText $configureArguments -EnvironmentMap $environmentMap
if ($configureCode -ne 0) {
    Exit-Build 7 "CMake configure failed with exit $configureCode. If the C source or ABI is missing, that is expected until author A hands it off."
}

$cachePath = Join-Path $BuildFull 'CMakeCache.txt'
if (-not (Test-Path -LiteralPath $cachePath)) {
    Exit-Build 7 "CMake configure did not write $cachePath"
}
$cache = Get-Content -LiteralPath $cachePath -Raw
$pointerFacts = New-Object System.Collections.Generic.List[string]
$cacheWidth = [regex]::Match($cache, 'CMAKE_SIZEOF_VOID_P:INTERNAL=([0-9]+)')
if ($cacheWidth.Success) {
    $pointerFacts.Add("CMakeCache.txt=$($cacheWidth.Groups[1].Value)")
}
$cmakeFilesRoot = Join-Path $BuildFull 'CMakeFiles'
if (Test-Path -LiteralPath $cmakeFilesRoot) {
    $compilerCmakeFiles = @(Get-ChildItem -LiteralPath $cmakeFilesRoot -Recurse -Filter 'CMakeCCompiler.cmake' -File -ErrorAction SilentlyContinue)
    foreach ($compilerCmake in $compilerCmakeFiles) {
        $compilerText = Get-Content -LiteralPath $compilerCmake.FullName -Raw
        $dataWidth = [regex]::Match($compilerText, 'set\(CMAKE_C_SIZEOF_DATA_PTR\s+"?([0-9]+)"?\)')
        if ($dataWidth.Success) {
            $pointerFacts.Add("$($compilerCmake.Name)=$($dataWidth.Groups[1].Value)")
        }
    }
}
if ($pointerFacts.Count -eq 0) {
    Exit-Build 6 "Pointer width was not recorded in CMakeCache.txt or CMakeCCompiler.cmake. Refusing to treat a missing field as 64-bit."
}
$wrongWidth = @($pointerFacts | Where-Object { $_ -notmatch '=8$' })
if ($wrongWidth.Count -gt 0) {
    Exit-Build 6 ("Pointer width is not 8 bytes: " + ($pointerFacts -join '; '))
}
Write-Info ("Pointer width 8: " + ($pointerFacts -join '; '))
$compilerMatch = [regex]::Match($cache, 'CMAKE_C_COMPILER:FILEPATH=([^\r\n]+)')
if ($compilerMatch.Success) {
    $recordedCompiler = $compilerMatch.Groups[1].Value.Trim()
    Write-Info "CMake C compiler: $recordedCompiler"
    if ($recordedCompiler -match '(?i)\\cl\.exe$' -and $recordedCompiler -notmatch '(?i)\\Hostx64\\x64\\cl\.exe$') {
        Exit-Build 6 "CMake selected a non-x64 cl: $recordedCompiler"
    }
}
$linkHits = Get-ChildItem -LiteralPath $BuildFull -Recurse -File -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -in @('CMakeCache.txt', 'build.ninja', 'link.txt', 'flags.make') -or $_.Extension -in @('.vcxproj', '.rsp') } |
    Select-String -Pattern 'jvm\.lib|libjvm\.so|libjvm\.dylib|-ljvm' -ErrorAction SilentlyContinue |
    Select-Object -First 5
if ($linkHits) {
    $hitText = ($linkHits | ForEach-Object { $_.Path + ':' + $_.LineNumber }) -join '; '
    Exit-Build 7 "Refusing configure output that references the JVM library: $hitText"
}

$buildParts = New-Object System.Collections.Generic.List[string]
$buildParts.Add('--build')
$buildParts.Add((Format-ProcessArgument $BuildFull))
if (-not $isNinja) {
    $buildParts.Add('--config')
    $buildParts.Add((Format-ProcessArgument $Configuration))
}
$buildArguments = ($buildParts.ToArray() -join ' ')
Write-Info "Building $Configuration."
Write-Info "CMake arguments: $buildArguments"
$buildCode = Start-CapturedCommand -FilePath $CmakePath -ArgumentText $buildArguments -EnvironmentMap $environmentMap
if ($buildCode -ne 0) {
    Exit-Build 8 "Build failed with exit $buildCode."
}

$dll = Join-Path $BuildFull 'xueximod_compute.dll'
if (-not (Test-Path -LiteralPath $dll)) {
    Exit-Build 8 "Build returned 0 but did not produce $dll"
}
Write-Info "DLL: $dll"
Write-Info "Left the DLL in the build directory. Did not copy it to resources, run/, or a game directory."
exit 0
