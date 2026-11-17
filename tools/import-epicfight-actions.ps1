# Re-runnable importer for the audited Epic Fight sword/knife/roll dev whitelist.
# Writes only under D:\alianximod\dev-authorized\epicfight. Does not delete directories.
# Existing different bytes are reported as conflicts and left untouched.
# A missing asset is written to a unique same-directory temp, checked, then moved without overwrite.
# -DefineOnly loads the preflight and commit functions for an external fixture. It does not import.
# -MeshOnly writes mesh/biped-normal.json, mesh/biped-slim.json and mesh-manifest.json.
# It does not rewrite the action manifest or mark either manifest playback-ready.
param(
    [string]$DevRoot = "D:\alianximod\dev-authorized\epicfight",
    [switch]$DefineOnly,
    [switch]$MeshOnly
)
$ErrorActionPreference = "Stop"

$ExpectedHead = "9d25f19c121c5e51b1db9efe01833a563706eeee"
$ExpectedWhitelistSha256 = "82faea083cfa2dd93d0369cfd68c7e92a30dd9ae4d79118514c32211eacc2882"
$UpstreamRoot = "C:\Users\baishang\AppData\Local\CursorAgentCodex\epicfight-1.20.1-9d25f19c"
$AuditedWhitelistPath = "C:\Users\baishang\AppData\Local\CursorAgentCodex\water-blade-implementation-20261024-eb016963\epicfight-action-manifest.json"
$BipedGitPath = "src/main/resources/assets/epicfight/animmodels/entity/biped.json"
$SlimGitPath = "src/main/resources/assets/epicfight/animmodels/entity/biped_slim_arm.json"
$AnimationPrefix = "src/main/resources/assets/epicfight/animmodels/animations/"

$ExpectedJoints = @(
    "Root", "Thigh_R", "Leg_R", "Knee_R", "Thigh_L", "Leg_L", "Knee_L", "Torso", "Chest", "Head",
    "Shoulder_R", "Arm_R", "Hand_R", "Tool_R", "Elbow_R", "Shoulder_L", "Arm_L", "Hand_L", "Tool_L", "Elbow_L"
)
$ExpectedChildren = @{
    "Root" = @("Thigh_R", "Thigh_L", "Torso")
    "Thigh_R" = @("Leg_R", "Knee_R")
    "Leg_R" = @()
    "Knee_R" = @()
    "Thigh_L" = @("Leg_L", "Knee_L")
    "Leg_L" = @()
    "Knee_L" = @()
    "Torso" = @("Chest")
    "Chest" = @("Head", "Shoulder_R", "Shoulder_L")
    "Head" = @()
    "Shoulder_R" = @("Arm_R")
    "Arm_R" = @("Hand_R", "Elbow_R")
    "Hand_R" = @("Tool_R")
    "Tool_R" = @()
    "Elbow_R" = @()
    "Shoulder_L" = @("Arm_L")
    "Arm_L" = @("Hand_L", "Elbow_L")
    "Hand_L" = @("Tool_L")
    "Tool_L" = @()
    "Elbow_L" = @()
}
$ForbiddenNames = @(
    "mesh", "vertices", "vertex", "weights", "weight", "textures", "texture", "uv", "uvs", "normals", "normal"
)

function Get-Sha256Hex([byte[]]$bytes) {
    $algorithm = [System.Security.Cryptography.SHA256]::Create()
    try {
        return ([System.BitConverter]::ToString($algorithm.ComputeHash($bytes))).Replace("-", "").ToLowerInvariant()
    } finally {
        $algorithm.Dispose()
    }
}

function Read-GitBlobBytes([string]$gitPath) {
    if ([string]::IsNullOrWhiteSpace($gitPath) -or $gitPath.Contains("..") -or $gitPath.Contains("\") -or $gitPath.StartsWith("/")) {
        throw "Rejected git path: $gitPath"
    }
    $start = New-Object System.Diagnostics.ProcessStartInfo
    $start.FileName = "git"
    $start.WorkingDirectory = $UpstreamRoot
    $start.Arguments = "-C `"$UpstreamRoot`" cat-file blob `"HEAD:$gitPath`""
    $start.UseShellExecute = $false
    $start.RedirectStandardOutput = $true
    $start.RedirectStandardError = $true
    $start.CreateNoWindow = $true
    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $start
    $stderr = New-Object System.Text.StringBuilder
    $handler = { if ($EventArgs.Data) { [void]$Event.MessageData.AppendLine($EventArgs.Data) } }
    $registered = Register-ObjectEvent -InputObject $process -EventName ErrorDataReceived -MessageData $stderr -Action $handler
    try {
        [void]$process.Start()
        $process.BeginErrorReadLine()
        $memory = New-Object System.IO.MemoryStream
        $process.StandardOutput.BaseStream.CopyTo($memory)
        $process.WaitForExit()
        if ($process.ExitCode -ne 0) {
            throw "git cat-file failed for ${gitPath}: $($stderr.ToString())"
        }
        return $memory.ToArray()
    } finally {
        Unregister-Event -SourceIdentifier $registered.Name -ErrorAction SilentlyContinue
        $registered | Remove-Job -Force -ErrorAction SilentlyContinue
        $process.Dispose()
    }
}

function Resolve-InsideDev([string]$relativePath) {
    $normalized = $relativePath.Replace("/", "\")
    if ($normalized.Contains("..") -or [System.IO.Path]::IsPathRooted($normalized)) {
        throw "Rejected dev path: $relativePath"
    }
    $rootFull = [System.IO.Path]::GetFullPath($DevRoot)
    if (-not $rootFull.EndsWith("\")) { $rootFull = $rootFull + "\" }
    $full = [System.IO.Path]::GetFullPath((Join-Path $rootFull $normalized))
    if (-not $full.StartsWith($rootFull, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Dev path escaped the authorized directory: $relativePath"
    }
    return $full
}

$script:ImportAssetWriter = {
    param(
        [Parameter(Mandatory = $true)][string]$FullPath,
        [Parameter(Mandatory = $true)][byte[]]$Payload
    )
    $stream = New-Object System.IO.FileStream(
        $FullPath,
        [System.IO.FileMode]::CreateNew,
        [System.IO.FileAccess]::Write,
        [System.IO.FileShare]::None)
    try {
        $stream.Write($Payload, 0, $Payload.Length)
    } finally {
        $stream.Dispose()
    }
}
$script:ImportBeforePublish = $null

function Remove-OwnedTemporary([string]$temporaryPath) {
    if ([string]::IsNullOrWhiteSpace($temporaryPath)) { return }
    if (Test-Path -LiteralPath $temporaryPath) {
        [System.IO.File]::Delete($temporaryPath)
    }
}

function Publish-MissingAsset {
    param(
        [Parameter(Mandatory = $true)][string]$RelativePath,
        [Parameter(Mandatory = $true)][byte[]]$Payload,
        [Parameter(Mandatory = $true)][string]$IncomingSha
    )
    $finalPath = Resolve-InsideDev $RelativePath
    $parent = Split-Path -Parent $finalPath
    if (-not (Test-Path -LiteralPath $parent)) {
        New-Item -ItemType Directory -Path $parent | Out-Null
    }
    $leaf = [System.IO.Path]::GetFileName($finalPath)
    $temporaryPath = Join-Path $parent ($leaf + ".partial-" + [guid]::NewGuid().ToString("N"))
    try {
        & $script:ImportAssetWriter -FullPath $temporaryPath -Payload $Payload
        $writtenBytes = [System.IO.File]::ReadAllBytes($temporaryPath)
        $writtenSha = Get-Sha256Hex $writtenBytes
        if ($writtenBytes.Length -ne $Payload.Length -or $writtenSha -ne $IncomingSha) {
            Remove-OwnedTemporary $temporaryPath
            return [pscustomobject]@{
                Ok = $false
                Phase = "verify"
                Conflict = $true
                Path = $finalPath
                Sha256 = $writtenSha
                Incoming = $IncomingSha
                Error = "temporary hash does not match incoming bytes"
            }
        }
        if ($null -ne $script:ImportBeforePublish) {
            & $script:ImportBeforePublish -FinalPath $finalPath -TemporaryPath $temporaryPath -Payload $Payload
        }
        try {
            [System.IO.File]::Move($temporaryPath, $finalPath)
            return [pscustomobject]@{ Ok = $true; Phase = "done"; Conflict = $false; Error = $null }
        } catch {
            $moveError = [string]$_.Exception.Message
            $finalSha = $null
            $sameContent = $false
            if (Test-Path -LiteralPath $finalPath) {
                $finalBytes = [System.IO.File]::ReadAllBytes($finalPath)
                $finalSha = Get-Sha256Hex $finalBytes
                if ($finalBytes.Length -eq $Payload.Length -and $finalSha -eq $IncomingSha) {
                    $sameContent = $true
                }
            }
            Remove-OwnedTemporary $temporaryPath
            if ($sameContent) {
                return [pscustomobject]@{ Ok = $true; Phase = "done"; Conflict = $false; Error = $null }
            }
            if ($null -ne $finalSha) {
                return [pscustomobject]@{
                    Ok = $false
                    Phase = "publish"
                    Conflict = $true
                    Path = $finalPath
                    Sha256 = $finalSha
                    Incoming = $IncomingSha
                    Error = "final appeared with different bytes"
                }
            }
            return [pscustomobject]@{
                Ok = $false
                Phase = "publish"
                Conflict = $false
                Path = $finalPath
                Sha256 = $null
                Incoming = $IncomingSha
                Error = $moveError
            }
        }
    } catch {
        Remove-OwnedTemporary $temporaryPath
        return [pscustomobject]@{
            Ok = $false
            Phase = "write"
            Conflict = $false
            Path = $finalPath
            Sha256 = $null
            Incoming = $IncomingSha
            Error = [string]$_.Exception.Message
        }
    }
}

function Get-ImportAssetSha([string]$relativePath) {
    $full = Resolve-InsideDev $relativePath
    if (-not (Test-Path -LiteralPath $full)) { return $null }
    return Get-Sha256Hex ([System.IO.File]::ReadAllBytes($full))
}

function Publish-ImportAssets {
    param(
        [Parameter(Mandatory = $true)]$Assets,
        [Parameter(Mandatory = $true)][scriptblock]$ManifestFactory,
        [string]$ManifestRelativePath = "import-manifest.json"
    )
    $conflicts = New-Object System.Collections.Generic.List[object]
    $planned = New-Object System.Collections.Generic.List[object]
    if ($Assets -is [System.Collections.IList] -and $Assets -isnot [byte[]] -and $Assets -isnot [string]) {
        foreach ($asset in $Assets) { $planned.Add($asset) }
    } else {
        $planned.Add($Assets)
    }
    foreach ($asset in $planned) {
        $incoming = Get-Sha256Hex ([byte[]]$asset.Bytes)
        $asset | Add-Member -NotePropertyName IncomingSha -NotePropertyValue $incoming -Force
        $existing = Get-ImportAssetSha ([string]$asset.RelativePath)
        if ($null -ne $existing -and $existing -ne $incoming) {
            $conflicts.Add([pscustomobject]@{
                Path = (Resolve-InsideDev ([string]$asset.RelativePath))
                Sha256 = $existing
                Incoming = $incoming
            })
        }
    }
    $manifestPath = Resolve-InsideDev $ManifestRelativePath
    if ($conflicts.Count -gt 0) {
        return [pscustomobject]@{
            Ok = $false
            Phase = "preflight"
            Conflicts = $conflicts.ToArray()
            Written = @()
            Skipped = @()
            ManifestCommitted = $false
            Error = "content conflict"
        }
    }
    $written = New-Object System.Collections.Generic.List[string]
    $skipped = New-Object System.Collections.Generic.List[string]
    foreach ($asset in $planned) {
        $full = Resolve-InsideDev ([string]$asset.RelativePath)
        if (Test-Path -LiteralPath $full) {
            $skipped.Add([string]$asset.RelativePath)
            continue
        }
        $attempt = Publish-MissingAsset -RelativePath ([string]$asset.RelativePath) -Payload ([byte[]]$asset.Bytes) -IncomingSha ([string]$asset.IncomingSha)
        if (-not $attempt.Ok) {
            $attemptConflicts = @()
            if ($attempt.Conflict) {
                $attemptConflicts = @([pscustomobject]@{ Path = $attempt.Path; Sha256 = $attempt.Sha256; Incoming = $attempt.Incoming })
            }
            return [pscustomobject]@{
                Ok = $false
                Phase = [string]$attempt.Phase
                Conflicts = $attemptConflicts
                Written = $written.ToArray()
                Skipped = $skipped.ToArray()
                ManifestCommitted = $false
                Error = [string]$attempt.Error
            }
        }
        $written.Add([string]$asset.RelativePath)
    }
    $verified = @{}
    foreach ($asset in $planned) {
        $full = Resolve-InsideDev ([string]$asset.RelativePath)
        if (-not (Test-Path -LiteralPath $full)) {
            return [pscustomobject]@{
                Ok = $false
                Phase = "verify"
                Conflicts = @()
                Written = $written.ToArray()
                Skipped = $skipped.ToArray()
                ManifestCommitted = $false
                Error = "missing after write: $($asset.RelativePath)"
            }
        }
        $diskSha = Get-Sha256Hex ([System.IO.File]::ReadAllBytes($full))
        if ($diskSha -ne [string]$asset.IncomingSha) {
            return [pscustomobject]@{
                Ok = $false
                Phase = "verify"
                Conflicts = @([pscustomobject]@{ Path = $full; Sha256 = $diskSha; Incoming = [string]$asset.IncomingSha })
                Written = $written.ToArray()
                Skipped = $skipped.ToArray()
                ManifestCommitted = $false
                Error = "verified hash does not match incoming bytes"
            }
        }
        $verified[[string]$asset.RelativePath] = $diskSha
    }
    $produced = & $ManifestFactory $verified
    if ($produced -isnot [byte[]] -and $null -ne $produced) {
        $wrapped = @($produced)
        if ($wrapped.Count -eq 1 -and $wrapped[0] -is [byte[]]) {
            $produced = [byte[]]$wrapped[0]
        }
    }
    if ($produced -is [object[]] -and $produced.Length -gt 0 -and $produced[0] -is [byte]) {
        $manifestBuffer = New-Object byte[] $produced.Length
        for ($byteIndex = 0; $byteIndex -lt $produced.Length; $byteIndex++) {
            $manifestBuffer[$byteIndex] = [byte]$produced[$byteIndex]
        }
        $produced = $manifestBuffer
    }
    if ($produced -isnot [byte[]]) {
        $producedType = "null"
        if ($null -ne $produced) { $producedType = $produced.GetType().FullName }
        throw "manifest factory did not return a single byte array: $producedType"
    }
    $manifestBytes = [byte[]]$produced
    foreach ($asset in $planned) {
        $full = Resolve-InsideDev ([string]$asset.RelativePath)
        $diskSha = Get-Sha256Hex ([System.IO.File]::ReadAllBytes($full))
        if ($diskSha -ne [string]$verified[[string]$asset.RelativePath]) {
            return [pscustomobject]@{
                Ok = $false
                Phase = "verify"
                Conflicts = @([pscustomobject]@{ Path = $full; Sha256 = $diskSha; Incoming = [string]$verified[[string]$asset.RelativePath] })
                Written = $written.ToArray()
                Skipped = $skipped.ToArray()
                ManifestCommitted = $false
                Error = "asset changed before manifest commit"
            }
        }
    }
    $manifestSha = Get-Sha256Hex $manifestBytes
    if (Test-Path -LiteralPath $manifestPath) {
        $existingManifest = Get-Sha256Hex ([System.IO.File]::ReadAllBytes($manifestPath))
        if ($existingManifest -eq $manifestSha) {
            $skipped.Add($ManifestRelativePath)
            return [pscustomobject]@{
                Ok = $true
                Phase = "done"
                Conflicts = @()
                Written = $written.ToArray()
                Skipped = $skipped.ToArray()
                ManifestCommitted = $true
                Error = $null
            }
        }
    }
    $manifestLeaf = [System.IO.Path]::GetFileName($ManifestRelativePath)
    $temporary = Join-Path (Split-Path -Parent $manifestPath) ($manifestLeaf + ".partial-" + [guid]::NewGuid().ToString("N"))
    try {
        & $script:ImportAssetWriter -FullPath $temporary -Payload $manifestBytes
        $temporarySha = Get-Sha256Hex ([System.IO.File]::ReadAllBytes($temporary))
        if ($temporarySha -ne $manifestSha) { throw "manifest temporary hash mismatch" }
        foreach ($asset in $planned) {
            $full = Resolve-InsideDev ([string]$asset.RelativePath)
            $diskSha = Get-Sha256Hex ([System.IO.File]::ReadAllBytes($full))
            if ($diskSha -ne [string]$verified[[string]$asset.RelativePath]) {
                throw "asset changed before manifest replace"
            }
        }
        if (Test-Path -LiteralPath $manifestPath) {
            [System.IO.File]::Replace($temporary, $manifestPath, [NullString]::Value)
        } else {
            [System.IO.File]::Move($temporary, $manifestPath)
        }
    } catch {
        if (Test-Path -LiteralPath $temporary) { [System.IO.File]::Delete($temporary) }
        return [pscustomobject]@{
            Ok = $false
            Phase = "manifest"
            Conflicts = @()
            Written = $written.ToArray()
            Skipped = $skipped.ToArray()
            ManifestCommitted = $false
            Error = [string]$_.Exception.Message
        }
    }
    $written.Add($ManifestRelativePath)
    return [pscustomobject]@{
        Ok = $true
        Phase = "done"
        Conflicts = @()
        Written = $written.ToArray()
        Skipped = $skipped.ToArray()
        ManifestCommitted = $true
        Error = $null
    }
}

function Assert-NoForbiddenFields($node, [string]$label) {
    if ($null -eq $node) { return }
    if ($node -is [System.Management.Automation.PSCustomObject]) {
        foreach ($property in $node.PSObject.Properties) {
            $lower = $property.Name.ToLowerInvariant()
            if ($ForbiddenNames -contains $lower) {
                throw "Forbidden field '$($property.Name)' in $label"
            }
            Assert-NoForbiddenFields $property.Value "$label.$($property.Name)"
        }
        return
    }
    if ($node -is [string] -or $node -is [ValueType]) { return }
    if ($node -is [System.Collections.IEnumerable]) {
        $index = 0
        foreach ($item in $node) {
            Assert-NoForbiddenFields $item "$label[$index]"
            $index++
        }
    }
}

function ConvertTo-RigNode($node) {
    $name = [string]$node.name
    $transformValues = @($node.transform)
    if ($transformValues.Count -ne 16) {
        throw "Bind transform for $name does not have 16 values"
    }
    $children = @()
    if ($null -ne $node.children) {
        $children = @($node.children)
    }
    $childNodes = New-Object System.Collections.Generic.List[object]
    foreach ($child in $children) {
        $childNodes.Add((ConvertTo-RigNode $child))
    }
    $record = New-Object System.Collections.Specialized.OrderedDictionary
    $record["name"] = $name
    $numberList = New-Object System.Collections.Generic.List[object]
    foreach ($value in $transformValues) { $numberList.Add([double]$value) }
    $record["transform"] = $numberList
    $record["children"] = $childNodes
    return $record
}

function Get-ChildNames($node) {
    $names = New-Object System.Collections.Generic.List[string]
    if ($null -eq $node.children) { return $names }
    foreach ($child in @($node.children)) { $names.Add([string]$child.name) }
    return $names
}

function Assert-Hierarchy($node) {
    $name = [string]$node.name
    if (-not $ExpectedChildren.ContainsKey($name)) {
        throw "Unexpected joint in hierarchy: $name"
    }
    $actual = @(Get-ChildNames $node)
    $expected = @($ExpectedChildren[$name])
    if ($actual.Count -ne $expected.Count) {
        throw "Child count mismatch for ${name}: $($actual -join ',') vs $($expected -join ',')"
    }
    for ($index = 0; $index -lt $expected.Count; $index++) {
        if ($actual[$index] -ne $expected[$index]) {
            throw "Child order mismatch for ${name}: $($actual -join ',') vs $($expected -join ',')"
        }
    }
    if ($null -ne $node.children) {
        foreach ($child in @($node.children)) { Assert-Hierarchy $child }
    }
}

function Get-AnimationFacts($parsed, [string]$gitPath) {
    Assert-NoForbiddenFields $parsed "animation"
    $tracks = @($parsed.animation)
    if ($tracks.Count -lt 1) { throw "No animation tracks in $gitPath" }
    $names = New-Object System.Collections.Generic.List[string]
    $rootMax = [double]::NegativeInfinity
    $sawRoot = $false
    $coordCount = 0
    $ikCount = 0
    $rootFirst = ([string]$tracks[0].name -eq "Root")
    foreach ($track in $tracks) {
        $trackName = [string]$track.name
        $isAuxiliary = ($trackName -eq "Coord" -or $trackName -eq "IK_left" -or $trackName -eq "IK_right")
        if ($trackName -eq "Coord") { $coordCount += 1 }
        if ($trackName -eq "IK_left" -or $trackName -eq "IK_right") { $ikCount += 1 }
        if (-not $isAuxiliary -and -not $ExpectedChildren.ContainsKey($trackName)) {
            [void]$script:ExtraTrackNames.Add($trackName)
        }
        $names.Add($trackName)
        $times = @($track.time)
        $matrices = @($track.transform)
        if ($times.Count -ne $matrices.Count -or $times.Count -lt 1) {
            throw "Time and transform counts differ for $trackName in $gitPath"
        }
        $trackMax = [double]::NegativeInfinity
        foreach ($matrix in $matrices) {
            if (@($matrix).Count -ne 16) { throw "Transform row for $trackName is not 16 values in $gitPath" }
        }
        foreach ($time in $times) {
            $numeric = [double]$time
            if ([double]::IsNaN($numeric) -or [double]::IsInfinity($numeric)) {
                throw "Non-finite time in $trackName of $gitPath"
            }
            if ($numeric -gt $trackMax) { $trackMax = $numeric }
        }
        if ($trackName -eq "Root") {
            $sawRoot = $true
            $rootMax = $trackMax
        }
    }
    $uniqueCount = @($names | Sort-Object -Unique).Count
    if ($uniqueCount -ne $names.Count) { throw "Duplicate animation track in $gitPath" }
    if ($coordCount -gt 1) { throw "More than one Coord track in $gitPath" }
    if (-not $sawRoot) { throw "Root track is missing in $gitPath" }
    $duration = [Math]::Round($rootMax, 4, [MidpointRounding]::AwayFromZero)
    return [pscustomobject]@{
        DurationSeconds = $duration
        TrackCount = $names.Count
        HasCoord = ($coordCount -eq 1)
        HasIk = ($ikCount -gt 0)
        RootFirst = $rootFirst
    }
}

function ConvertTo-PlainJson($value) {
    if ($null -eq $value) { return "null" }
    if ($value -is [bool]) {
        if ($value) { return "true" }
        return "false"
    }
    if ($value -is [string]) {
        $escaped = $value.Replace("\", "\\").Replace('"', '\"').Replace("`r", "\r").Replace("`n", "\n").Replace("`t", "\t")
        return '"' + $escaped + '"'
    }
    if ($value -is [ValueType]) {
        $number = [double]$value
        if ([double]::IsNaN($number) -or [double]::IsInfinity($number)) { throw "Refusing to write a non-finite JSON number" }
        $rounded = [Math]::Round($number)
        if ([Math]::Abs($number - $rounded) -lt 1e-9 -and [Math]::Abs($number) -lt 1000000000000) {
            return ([int64]$rounded).ToString([System.Globalization.CultureInfo]::InvariantCulture)
        }
        return $number.ToString("G15", [System.Globalization.CultureInfo]::InvariantCulture)
    }
    if ($value -is [System.Collections.IDictionary]) {
        $parts = New-Object System.Collections.Generic.List[string]
        foreach ($key in @($value.Keys)) {
            $parts.Add((ConvertTo-PlainJson ([string]$key)) + ":" + (ConvertTo-PlainJson $value[$key]))
        }
        return "{" + ($parts -join ",") + "}"
    }
    if ($value -is [System.Collections.IEnumerable]) {
        $parts = New-Object System.Collections.Generic.List[string]
        foreach ($item in $value) { $parts.Add((ConvertTo-PlainJson $item)) }
        return "[" + ($parts -join ",") + "]"
    }
    throw ("Unsupported JSON value type: " + $value.GetType().FullName)
}

function ConvertTo-Utf8Bytes([string]$text) {
    $encoding = New-Object System.Text.UTF8Encoding $false
    return ,$encoding.GetBytes($text)
}

function New-NumberChannel([int]$stride, $values) {
    $channel = New-Object System.Collections.Specialized.OrderedDictionary
    $channel["stride"] = $stride
    $channel["array"] = $values
    return ,$channel
}

function New-PlayerMeshAsset {
    param(
        [Parameter(Mandatory = $true)][byte[]]$SourceBytes,
        [Parameter(Mandatory = $true)][string]$GitPath,
        [Parameter(Mandatory = $true)][string]$ModelName,
        [Parameter(Mandatory = $true)][string]$SourceSha,
        [Parameter(Mandatory = $true)][int]$ExpectedTextures
    )
    $parsed = [System.Text.Encoding]::UTF8.GetString($SourceBytes) | ConvertFrom-Json
    if ($null -eq $parsed.vertices) { throw "Mesh source has no vertices: $GitPath" }
    $vertices = $parsed.vertices
    $allowed = @("positions", "normals", "uvs", "weights", "vcounts", "vindices", "parts")
    foreach ($property in @($vertices.PSObject.Properties.Name)) {
        if ($allowed -notcontains $property) { throw "Unexpected vertex field $property in $GitPath" }
    }
    function Convert-FlatNumbers($node, [int]$stride, [string]$label, [switch]$Integral) {
        if ($null -eq $node -or [int]$node.stride -ne $stride) { throw "$label stride is not $stride" }
        if ($null -ne $node.count -and [int]$node.count -ne (@($node.array).Count / $stride)) { throw "$label count does not match its array" }
        if ($null -ne $node.count -and [int]$node.count -ne (@($node.array).Count / $stride)) { throw "$label count does not match its array" }
        $values = New-Object System.Collections.Generic.List[object]
        foreach ($item in @($node.array)) {
            $numeric = [double]$item
            if ([double]::IsNaN($numeric) -or [double]::IsInfinity($numeric)) { throw "$label contains a non-finite number" }
            if ($Integral) {
                $rounded = [Math]::Round($numeric)
                if ([Math]::Abs($numeric - $rounded) -gt 0.000001) { throw "$label contains a fractional index" }
                $values.Add([int64]$rounded)
            } else {
                $values.Add($numeric)
            }
        }
        if ($values.Count -lt $stride -or ($values.Count % $stride) -ne 0) { throw "$label length is not divisible by $stride" }
        return ,$values
    }
    $positions = Convert-FlatNumbers $vertices.positions 3 "positions"
    $normals = Convert-FlatNumbers $vertices.normals 3 "normals"
    $textures = Convert-FlatNumbers $vertices.uvs 2 "uvs"
    $weights = Convert-FlatNumbers $vertices.weights 1 "weights"
    $influenceCounts = Convert-FlatNumbers $vertices.vcounts 1 "vcounts" -Integral
    $influencePairs = Convert-FlatNumbers $vertices.vindices 1 "vindices" -Integral
    $positionCount = $positions.Count / 3
    $normalCount = $normals.Count / 3
    $textureCount = $textures.Count / 2
    if ($influenceCounts.Count -ne $positionCount) { throw "vcounts do not match positions in $GitPath" }
    $cursor = 0
    $highestInfluence = 0
    for ($positionIndex = 0; $positionIndex -lt $positionCount; $positionIndex++) {
        $influenceCount = [int]$influenceCounts[$positionIndex]
        if ($influenceCount -lt 1 -or $influenceCount -gt 4) { throw "Influence count $influenceCount at position $positionIndex is outside 1..4" }
        if ($influenceCount -gt $highestInfluence) { $highestInfluence = $influenceCount }
        $weightSum = 0.0
        for ($influenceSlot = 0; $influenceSlot -lt $influenceCount; $influenceSlot++) {
            $pairIndex = ($cursor + $influenceSlot) * 2
            if ($pairIndex + 1 -ge $influencePairs.Count) { throw "vindices ended early in $GitPath" }
            $jointId = [int]$influencePairs[$pairIndex]
            $weightIndex = [int]$influencePairs[$pairIndex + 1]
            if ($jointId -lt 0 -or $jointId -gt 19) { throw "Joint id $jointId is outside the authorized 20 bones" }
            if ($weightIndex -lt 0 -or $weightIndex -ge $weights.Count) { throw "Weight index $weightIndex is outside the weight table" }
            $weight = [double]$weights[$weightIndex]
            if ($weight -le 0.0) { throw "Non-positive weight at position $positionIndex" }
            $weightSum += $weight
        }
        if ([Math]::Abs($weightSum - 1.0) -gt 0.001) { throw "Weight sum $weightSum at position $positionIndex is not 1" }
        $cursor += $influenceCount
    }
    if (($cursor * 2) -ne $influencePairs.Count) { throw "vindices length does not match vcounts in $GitPath" }
    $partNames = @(
        "head", "hat", "torso", "jacket", "leftArm", "rightArm", "leftSleeve", "rightSleeve",
        "leftLeg", "rightLeg", "leftPants", "rightPants"
    )
    $expectedPartCorners = @{
        head = 36; hat = 36; torso = 240; jacket = 240
        leftArm = 108; rightArm = 108; leftSleeve = 108; rightSleeve = 108
        leftLeg = 108; rightLeg = 108; leftPants = 108; rightPants = 108
    }
    $actualPartNames = @($vertices.parts.PSObject.Properties.Name)
    if ($actualPartNames.Count -ne 12) { throw "Expected 12 parts in $GitPath" }
    foreach ($partName in $actualPartNames) {
        if ($partNames -notcontains $partName) { throw "Unexpected part $partName in $GitPath" }
    }
    $parts = New-Object System.Collections.Specialized.OrderedDictionary
    $cornerTotal = 0
    foreach ($partName in $partNames) {
        $raw = Convert-FlatNumbers $vertices.parts.$partName 3 $partName -Integral
        if (($raw.Count % 9) -ne 0) { throw "$partName corners are not grouped into triangles" }
        $cornerCount = $raw.Count / 3
        if ($cornerCount -ne [int]$expectedPartCorners[$partName]) { throw "$partName corner count is $cornerCount" }
        for ($cornerIndex = 0; $cornerIndex -lt $raw.Count; $cornerIndex += 3) {
            $positionSlot = [int]$raw[$cornerIndex]
            $textureSlot = [int]$raw[$cornerIndex + 1]
            $normalSlot = [int]$raw[$cornerIndex + 2]
            if ($positionSlot -lt 0 -or $positionSlot -ge $positionCount) { throw "$partName position index is out of range" }
            if ($textureSlot -lt 0 -or $textureSlot -ge $textureCount) { throw "$partName uv index is out of range" }
            if ($normalSlot -lt 0 -or $normalSlot -ge $normalCount) { throw "$partName normal index is out of range" }
        }
        $parts[$partName] = New-NumberChannel 3 $raw
        $cornerTotal += $cornerCount
    }
    if ($positionCount -ne 260 -or $normalCount -ne 6 -or $cursor -ne 325 -or $cornerTotal -ne 1416 -or $textureCount -ne $ExpectedTextures) {
        throw "$ModelName counts are positions=$positionCount normals=$normalCount influences=$cursor corners=$cornerTotal uvs=$textureCount"
    }
    $counts = New-Object System.Collections.Specialized.OrderedDictionary
    $counts["parts"] = 12
    $counts["positions"] = $positionCount
    $counts["normals"] = $normalCount
    $counts["uvs"] = $textureCount
    $counts["weights"] = $weights.Count
    $counts["influences"] = $cursor
    $counts["corners"] = $cornerTotal
    $counts["triangles"] = [int]($cornerTotal / 3)
    $counts["highestInfluence"] = $highestInfluence
    $document = New-Object System.Collections.Specialized.OrderedDictionary
    $document["schemaVersion"] = 1
    $document["model"] = $ModelName
    $document["upstreamCommit"] = $ExpectedHead
    $document["sourceGitPath"] = $GitPath
    $document["sourceSha256"] = $SourceSha
    $document["localOnly"] = $true
    $document["authorization"] = "local-dev-only player mesh and skin weights"
    $document["rigRelativePath"] = "rig/biped-armature.json"
    $document["coordinateSpace"] = "source-meters"
    $document["coordinateConversionApplied"] = $false
    $document["playbackReady"] = $false
    $document["positions"] = New-NumberChannel 3 $positions
    $document["normals"] = New-NumberChannel 3 $normals
    $document["uvs"] = New-NumberChannel 2 $textures
    $document["weights"] = New-NumberChannel 1 $weights
    $document["vcounts"] = New-NumberChannel 1 $influenceCounts
    $document["vindices"] = New-NumberChannel 2 $influencePairs
    $document["parts"] = $parts
    $document["counts"] = $counts
    $bytes = ConvertTo-Utf8Bytes (ConvertTo-PlainJson $document)
    return [pscustomobject]@{
        Bytes = $bytes
        Model = $ModelName
        SourceGitPath = $GitPath
        SourceSha256 = $SourceSha
        Parts = 12
        Positions = $positionCount
        Normals = $normalCount
        Textures = $textureCount
        Weights = $weights.Count
        Influences = $cursor
        Corners = $cornerTotal
        Triangles = [int]($cornerTotal / 3)
        HighestInfluence = $highestInfluence
    }
}

function Invoke-PlayerMeshImport {
    if (([System.IO.Path]::GetFullPath($DevRoot)) -ne ([System.IO.Path]::GetFullPath("D:\alianximod\dev-authorized\epicfight"))) {
        throw "Dev root is not the authorized directory"
    }
    if (-not (Test-Path -LiteralPath (Join-Path $UpstreamRoot ".git"))) { throw "Upstream clone is missing" }
    $head = (& git -C $UpstreamRoot rev-parse HEAD).Trim()
    if ($head -ne $ExpectedHead) { throw "Upstream HEAD is $head, expected $ExpectedHead" }
    $actionManifestPath = Resolve-InsideDev "import-manifest.json"
    $rigPath = Resolve-InsideDev "rig/biped-armature.json"
    if (-not (Test-Path -LiteralPath $actionManifestPath)) { throw "Action manifest is missing; mesh import will not invent a license" }
    if (-not (Test-Path -LiteralPath $rigPath)) { throw "Existing rig metadata is missing; mesh import does not copy a skeleton" }
    $actionBytes = [System.IO.File]::ReadAllBytes($actionManifestPath)
    $actionBefore = Get-Sha256Hex $actionBytes
    $rigBefore = Get-Sha256Hex ([System.IO.File]::ReadAllBytes($rigPath))
    $actionManifest = [System.Text.Encoding]::UTF8.GetString($actionBytes) | ConvertFrom-Json
    if ([int]$actionManifest.registrationCount -ne 118 -or [int]$actionManifest.fileCount -ne 117 -or [int]$actionManifest.skeletonBoneCount -ne 20) {
        throw "Action manifest is no longer 118 ids / 117 files / 20 bones"
    }
    if ($actionManifest.playbackReady) { throw "Action manifest playbackReady must stay false" }
    if ([string]$actionManifest.skeletonExtractedRelativePath -ne "rig/biped-armature.json") { throw "Action manifest rig path changed" }
    if ([string]$actionManifest.skeletonExtractedSha256 -ne $rigBefore) { throw "Rig hash does not match the action manifest" }
    $normalBytes = Read-GitBlobBytes $BipedGitPath
    $slimBytes = Read-GitBlobBytes $SlimGitPath
    $normalSha = Get-Sha256Hex $normalBytes
    $slimSha = Get-Sha256Hex $slimBytes
    $normal = New-PlayerMeshAsset -SourceBytes $normalBytes -GitPath $BipedGitPath -ModelName "normal" -SourceSha $normalSha -ExpectedTextures 321
    $slim = New-PlayerMeshAsset -SourceBytes $slimBytes -GitPath $SlimGitPath -ModelName "slim" -SourceSha $slimSha -ExpectedTextures 332
    $normalRecord = New-Object System.Collections.Specialized.OrderedDictionary
    $slimRecord = New-Object System.Collections.Specialized.OrderedDictionary
    $normalRecord["model"] = [string]$normal.Model
    $normalRecord["sourceGitPath"] = [string]$normal.SourceGitPath
    $normalRecord["sourceSha256"] = [string]$normal.SourceSha256
    $normalRecord["extractedRelativePath"] = "mesh/biped-normal.json"
    $normalRecord["extractedSha256"] = $null
    $normalRecord["parts"] = [int]$normal.Parts
    $normalRecord["positions"] = [int]$normal.Positions
    $normalRecord["normals"] = [int]$normal.Normals
    $normalRecord["uvs"] = [int]$normal.Textures
    $normalRecord["weights"] = [int]$normal.Weights
    $normalRecord["influences"] = [int]$normal.Influences
    $normalRecord["corners"] = [int]$normal.Corners
    $normalRecord["triangles"] = [int]$normal.Triangles
    $normalRecord["highestInfluence"] = [int]$normal.HighestInfluence
    $slimRecord["model"] = [string]$slim.Model
    $slimRecord["sourceGitPath"] = [string]$slim.SourceGitPath
    $slimRecord["sourceSha256"] = [string]$slim.SourceSha256
    $slimRecord["extractedRelativePath"] = "mesh/biped-slim.json"
    $slimRecord["extractedSha256"] = $null
    $slimRecord["parts"] = [int]$slim.Parts
    $slimRecord["positions"] = [int]$slim.Positions
    $slimRecord["normals"] = [int]$slim.Normals
    $slimRecord["uvs"] = [int]$slim.Textures
    $slimRecord["weights"] = [int]$slim.Weights
    $slimRecord["influences"] = [int]$slim.Influences
    $slimRecord["corners"] = [int]$slim.Corners
    $slimRecord["triangles"] = [int]$slim.Triangles
    $slimRecord["highestInfluence"] = [int]$slim.HighestInfluence
    $meshRecords = New-Object System.Collections.Generic.List[object]
    $meshRecords.Add($normalRecord)
    $meshRecords.Add($slimRecord)
    $meshManifest = New-Object System.Collections.Specialized.OrderedDictionary
    $meshManifest["schemaVersion"] = 1
    $meshManifest["artifact"] = "player-mesh"
    $meshManifest["upstreamCommit"] = $ExpectedHead
    $meshManifest["verifiedHead"] = $head
    $meshManifest["localOnly"] = $true
    $meshManifest["distribution"] = "local-dev-only"
    $meshManifest["playbackReady"] = $false
    $meshManifest["runtimeStatus"] = "pending-runtime"
    $meshManifest["coordinateSpace"] = "source-meters"
    $meshManifest["coordinateConversionApplied"] = $false
    $meshManifest["rigRelativePath"] = "rig/biped-armature.json"
    $meshManifest["rigSha256"] = $rigBefore
    $meshManifest["skeletonBoneCount"] = 20
    $meshManifest["actionRegistrationCount"] = 118
    $meshManifest["actionFileCount"] = 117
    $meshManifest["meshes"] = $meshRecords
    $planned = New-Object System.Collections.Generic.List[object]
    $planned.Add([pscustomobject]@{ RelativePath = "mesh/biped-normal.json"; Bytes = [byte[]]$normal.Bytes })
    $planned.Add([pscustomobject]@{ RelativePath = "mesh/biped-slim.json"; Bytes = [byte[]]$slim.Bytes })
    $script:PlayerMeshManifest = $meshManifest
    $script:PlayerMeshRecords = $meshRecords
    $meshManifestFactory = {
        param($verifiedHashes)
        foreach ($record in $script:PlayerMeshRecords) {
            $relative = [string]$record["extractedRelativePath"]
            $record["extractedSha256"] = [string]$verifiedHashes[$relative]
        }
        return ,(ConvertTo-Utf8Bytes (ConvertTo-PlainJson $script:PlayerMeshManifest))
    }
    try {
        $publication = Publish-ImportAssets -Assets $planned -ManifestFactory $meshManifestFactory -ManifestRelativePath "mesh-manifest.json"
    } catch {
        throw ("mesh publish failed: " + $_.Exception.ToString() + [Environment]::NewLine + $_.ScriptStackTrace + [Environment]::NewLine + $_.InvocationInfo.PositionMessage)
    }
    $actionAfter = Get-Sha256Hex ([System.IO.File]::ReadAllBytes($actionManifestPath))
    $rigAfter = Get-Sha256Hex ([System.IO.File]::ReadAllBytes($rigPath))
    if ($actionAfter -ne $actionBefore) { throw "Mesh import rewrote the action manifest" }
    if ($rigAfter -ne $rigBefore) { throw "Mesh import rewrote the rig metadata" }
    Write-Output ("meshNormal positions=$($normal.Positions) normals=$($normal.Normals) uvs=$($normal.Textures) weights=$($normal.Weights) influences=$($normal.Influences) corners=$($normal.Corners) triangles=$($normal.Triangles) highestInfluence=$($normal.HighestInfluence)")
    Write-Output ("meshSlim positions=$($slim.Positions) normals=$($slim.Normals) uvs=$($slim.Textures) weights=$($slim.Weights) influences=$($slim.Influences) corners=$($slim.Corners) triangles=$($slim.Triangles) highestInfluence=$($slim.HighestInfluence)")
    Write-Output ("meshWrite=$($publication.Written.Count) meshSkip=$($publication.Skipped.Count) meshConflict=$($publication.Conflicts.Count)")
    Write-Output "meshPlaybackReady=false meshRuntimeStatus=pending-runtime"
    Write-Output ("meshManifestCommitted=" + $publication.ManifestCommitted)
    if (-not $publication.Ok) {
        foreach ($conflict in @($publication.Conflicts)) {
            Write-Output ("MESH CONFLICT " + $conflict.Path + " existing=" + $conflict.Sha256 + " incoming=" + $conflict.Incoming)
        }
        throw ("mesh import failed: " + [string]$publication.Phase + " " + [string]$publication.Error)
    }
    $bipedLeak = Get-ChildItem -LiteralPath $DevRoot -Recurse -Filter "biped.json" -File -ErrorAction SilentlyContinue
    if ($bipedLeak) { throw "Full biped.json was written into the dev directory" }
}

if (-not $DefineOnly) {
if ($MeshOnly) {
    Invoke-PlayerMeshImport
    exit 0
}
if (([System.IO.Path]::GetFullPath($DevRoot)) -ne ([System.IO.Path]::GetFullPath("D:\alianximod\dev-authorized\epicfight"))) {
    throw "Dev root is not the authorized directory"
}
if (-not (Test-Path -LiteralPath (Join-Path $UpstreamRoot ".git"))) {
    throw "Upstream clone is missing"
}
$head = (& git -C $UpstreamRoot rev-parse HEAD).Trim()
if ($head -ne $ExpectedHead) {
    throw "Upstream HEAD is $head, expected $ExpectedHead"
}

$whitelistBytes = $null
if (Test-Path -LiteralPath $AuditedWhitelistPath) {
    $whitelistBytes = [System.IO.File]::ReadAllBytes($AuditedWhitelistPath)
} else {
    $fallback = Resolve-InsideDev "audited-whitelist.json"
    if (-not (Test-Path -LiteralPath $fallback)) { throw "Audited whitelist is missing" }
    $whitelistBytes = [System.IO.File]::ReadAllBytes($fallback)
}
$whitelistSha = Get-Sha256Hex $whitelistBytes
if ($whitelistSha -ne $ExpectedWhitelistSha256) {
    throw "Whitelist hash $whitelistSha does not match the audited hash"
}
$whitelist = [System.Text.Encoding]::UTF8.GetString($whitelistBytes) | ConvertFrom-Json
if ($whitelist.upstreamCommit -ne $ExpectedHead) { throw "Whitelist commit does not match the fixed HEAD" }
if ($whitelist.counts.registrations -ne 118 -or $whitelist.counts.deduplicatedAnimationFiles -ne 117 -or $whitelist.counts.aliases -ne 1) {
    throw "Whitelist counts are not 118/117/1"
}
$entries = @($whitelist.entries)
$dedup = @($whitelist.deduplicatedAnimationFiles)
if ($entries.Count -ne 118 -or $dedup.Count -ne 117) { throw "Whitelist entry arrays do not match the declared counts" }

$ids = New-Object System.Collections.Generic.List[string]
$pathToEntries = @{}
foreach ($entry in $entries) {
    $id = [string]$entry.id
    $gitPath = [string]$entry.sourceGitPath
    if ([string]::IsNullOrWhiteSpace($id) -or $ids.Contains($id)) { throw "Missing or duplicate registration id: $id" }
    if ($gitPath.Contains("..") -or -not $gitPath.StartsWith($AnimationPrefix) -or -not $gitPath.EndsWith(".json")) {
        throw "Registration source path is outside the animation allowlist: $gitPath"
    }
    $ids.Add($id)
    if (-not $pathToEntries.ContainsKey($gitPath)) { $pathToEntries[$gitPath] = New-Object System.Collections.Generic.List[object] }
    $pathToEntries[$gitPath].Add($entry)
}
if ($ids.Count -ne 118 -or $pathToEntries.Keys.Count -ne 117) { throw "ID to file mapping is not 118 to 117" }

$dedupPaths = New-Object System.Collections.Generic.List[string]
foreach ($fileEntry in $dedup) {
    $gitPath = [string]$fileEntry.sourceGitPath
    if ($dedupPaths.Contains($gitPath)) { throw "Duplicate dedup path: $gitPath" }
    if (-not $pathToEntries.ContainsKey($gitPath)) { throw "Dedup path is not registered: $gitPath" }
    $registered = @($fileEntry.registrationIds)
    $fromEntries = @($pathToEntries[$gitPath] | ForEach-Object { [string]$_.id })
    if ($registered.Count -ne $fromEntries.Count) { throw "Registration id count mismatch for $gitPath" }
    foreach ($registeredId in $registered) {
        if ($fromEntries -notcontains [string]$registeredId) { throw "Dedup id $registeredId is not on $gitPath" }
    }
    $dedupPaths.Add($gitPath)
}
if ($dedupPaths.Count -ne 117) { throw "Dedup path count is not 117" }

$aliasEntries = @($entries | Where-Object { @($_.dependencies) -contains "alias_of:epicfight:biped/combat/sword_dual_airslash" })
if ($aliasEntries.Count -ne 1 -or [string]$aliasEntries[0].id -ne "epicfight:biped/combat/dagger_dual_airslash") {
    throw "The single alias is not dagger_dual_airslash"
}
if ([string]$aliasEntries[0].sourceGitPath -ne "src/main/resources/assets/epicfight/animmodels/animations/biped/combat/sword_dual_airslash.json") {
    throw "Alias does not share sword_dual_airslash.json"
}

$bipedBytes = Read-GitBlobBytes $BipedGitPath
$bipedSha = Get-Sha256Hex $bipedBytes
$biped = [System.Text.Encoding]::UTF8.GetString($bipedBytes) | ConvertFrom-Json
$jointNames = @($biped.armature.joints | ForEach-Object { [string]$_ })
if ($jointNames.Count -ne 20) { throw "Biped joint count is $($jointNames.Count)" }
for ($index = 0; $index -lt 20; $index++) {
    if ($jointNames[$index] -ne $ExpectedJoints[$index]) { throw "Biped joint order mismatch at $index" }
}
Assert-Hierarchy $biped.armature.hierarchy
$rigHierarchy = ConvertTo-RigNode $biped.armature.hierarchy
$jointList = New-Object System.Collections.Generic.List[string]
foreach ($jointName in $ExpectedJoints) { $jointList.Add($jointName) }
$rig = New-Object System.Collections.Specialized.OrderedDictionary
$rig["schemaVersion"] = 1
$rig["upstreamCommit"] = $ExpectedHead
$rig["sourceGitPath"] = $BipedGitPath
$rig["sourceFileSha256"] = $bipedSha
$rig["boneCount"] = 20
$rig["joints"] = $jointList
$rig["hierarchy"] = $rigHierarchy
$rig["playbackReady"] = $false
$rig["runtimeStatus"] = "pending-runtime"
$rigText = ConvertTo-PlainJson $rig
foreach ($token in @('"vertices"', '"mesh"', '"weights"', '"textures"', '"uv"')) {
    if ($rigText.Contains($token)) { throw "Extracted rig still contains $token" }
}
$rigBytes = ConvertTo-Utf8Bytes $rigText

$fileRecords = New-Object System.Collections.Generic.List[object]
$animationBytes = @{}
$totalBytes = 0L
$script:ExtraTrackNames = New-Object "System.Collections.Generic.HashSet[string]"
foreach ($gitPath in ($dedupPaths | Sort-Object)) {
    $blob = Read-GitBlobBytes $gitPath
    $parsed = [System.Text.Encoding]::UTF8.GetString($blob) | ConvertFrom-Json
    $facts = Get-AnimationFacts $parsed $gitPath
    $samplePaths = @(
        "src/main/resources/assets/epicfight/animmodels/animations/biped/combat/sword_auto1.json",
        "src/main/resources/assets/epicfight/animmodels/animations/biped/combat/uchigatana_auto1.json",
        "src/main/resources/assets/epicfight/animmodels/animations/biped/skill/roll_forward.json"
    )
    if ($samplePaths -contains $gitPath) {
        if ($facts.TrackCount -ne 20 -or $facts.HasCoord -or $facts.HasIk -or -not $facts.RootFirst) {
            throw "Audited sample $gitPath is not a 20-track Root-first clip without Coord"
        }
        if ($null -ne $parsed.format) { throw "Audited sample $gitPath has a format field" }
    }
    $animationBytes[$gitPath] = $blob
    $totalBytes += $blob.Length
    $registrations = New-Object System.Collections.Generic.List[object]
    foreach ($entry in $pathToEntries[$gitPath]) {
        $dependencyList = New-Object System.Collections.Generic.List[string]
        foreach ($dependency in @($entry.dependencies)) {
            if (-not [string]::IsNullOrWhiteSpace([string]$dependency)) { $dependencyList.Add([string]$dependency) }
        }
        $aliasOf = $null
        if ($dependencyList -contains "alias_of:epicfight:biped/combat/sword_dual_airslash") {
            $aliasOf = "epicfight:biped/combat/sword_dual_airslash"
        }
        $registration = New-Object System.Collections.Specialized.OrderedDictionary
        $registration["id"] = [string]$entry.id
        $registration["category"] = [string]$entry.category
        $registration["dependencies"] = $dependencyList
        $registration["aliasOf"] = $aliasOf
        $registration["expectedArmature"] = [string]$entry.expectedArmature
        $registrations.Add($registration)
    }
    $sourceSha = Get-Sha256Hex $blob
    $record = New-Object System.Collections.Specialized.OrderedDictionary
    $record["sourceGitPath"] = $gitPath
    $record["devRelativePath"] = "sources/$gitPath"
    $record["registrations"] = $registrations
    $record["sourceSha256"] = $sourceSha
    $record["importedSha256"] = $null
    $record["byteLength"] = $blob.Length
    $record["durationSeconds"] = $facts.DurationSeconds
    $record["armature"] = "epicfight:entity/biped"
    $record["trackCount"] = $facts.TrackCount
    $record["hasCoord"] = [bool]$facts.HasCoord
    $record["hasIk"] = [bool]$facts.HasIk
    $record["runtimeStatus"] = "pending-runtime"
    $record["playbackReady"] = $false
    $fileRecords.Add($record)
}
if ($script:ExtraTrackNames.Count -gt 0) {
    $extraList = @($script:ExtraTrackNames | Sort-Object) -join ","
    throw "Extra animation tracks outside the 20 bones and Coord: $extraList"
}

function Get-Duration([string]$suffix) {
    $match = $fileRecords | Where-Object { [string]$_["sourceGitPath"] -eq $suffix }
    if ($null -eq $match) { throw "Missing sample $suffix" }
    return [double]$match["durationSeconds"]
}
$swordDuration = Get-Duration "src/main/resources/assets/epicfight/animmodels/animations/biped/combat/sword_auto1.json"
$uchigatanaDuration = Get-Duration "src/main/resources/assets/epicfight/animmodels/animations/biped/combat/uchigatana_auto1.json"
$rollDuration = Get-Duration "src/main/resources/assets/epicfight/animmodels/animations/biped/skill/roll_forward.json"
if ([Math]::Abs($swordDuration - 0.6) -gt 0.00005) { throw "sword_auto1 duration is $swordDuration" }
if ([Math]::Abs($uchigatanaDuration - 0.85) -gt 0.00005) { throw "uchigatana_auto1 duration is $uchigatanaDuration" }
if ([Math]::Abs($rollDuration - 0.6333) -gt 0.00005) { throw "roll_forward duration is $rollDuration" }

$manifest = New-Object System.Collections.Specialized.OrderedDictionary
$manifest["schemaVersion"] = 1
$manifest["upstreamCommit"] = $ExpectedHead
$manifest["verifiedHead"] = $head
$manifest["whitelistSha256"] = $whitelistSha
$manifest["registrationCount"] = 118
$manifest["fileCount"] = 117
$manifest["aliasCount"] = 1
$manifest["totalAnimationBytes"] = $totalBytes
$manifest["armature"] = "epicfight:entity/biped"
$manifest["skeletonSourceGitPath"] = $BipedGitPath
$manifest["skeletonSourceSha256"] = $bipedSha
$manifest["skeletonExtractedRelativePath"] = "rig/biped-armature.json"
$manifest["skeletonExtractedSha256"] = $null
$manifest["skeletonBoneCount"] = 20
$manifest["runtimeStatus"] = "pending-runtime"
$manifest["playbackReady"] = $false
$manifest["files"] = $fileRecords

$plannedAssets = New-Object System.Collections.Generic.List[object]
$plannedAssets.Add([pscustomobject]@{ RelativePath = "audited-whitelist.json"; Bytes = $whitelistBytes })
$plannedAssets.Add([pscustomobject]@{ RelativePath = "rig/biped-armature.json"; Bytes = $rigBytes })
foreach ($gitPath in ($dedupPaths | Sort-Object)) {
    $plannedAssets.Add([pscustomobject]@{ RelativePath = "sources/$gitPath"; Bytes = $animationBytes[$gitPath] })
}
$publication = Publish-ImportAssets -Assets $plannedAssets -ManifestFactory {
    param($verifiedHashes)
    foreach ($record in $fileRecords) {
        $relative = [string]$record["devRelativePath"]
        $record["importedSha256"] = [string]$verifiedHashes[$relative]
    }
    $manifest["whitelistSha256"] = [string]$verifiedHashes["audited-whitelist.json"]
    $manifest["skeletonExtractedSha256"] = [string]$verifiedHashes["rig/biped-armature.json"]
    return ,(ConvertTo-Utf8Bytes (ConvertTo-PlainJson $manifest))
}
Write-Output ("HEAD=" + $head)
Write-Output ("whitelistSha256=" + $whitelistSha)
Write-Output ("ids=118 files=117 alias=1 bones=20")
Write-Output ("animationBytes=" + $totalBytes)
Write-Output ("durations sword_auto1=$swordDuration uchigatana_auto1=$uchigatanaDuration roll_forward=$rollDuration")
Write-Output ("write=$($publication.Written.Count) skip=$($publication.Skipped.Count) conflict=$($publication.Conflicts.Count)")
Write-Output "playbackReady=false runtimeStatus=pending-runtime"
Write-Output ("manifestCommitted=" + $publication.ManifestCommitted)
if (-not $publication.Ok) {
    foreach ($conflict in @($publication.Conflicts)) {
        Write-Output ("CONFLICT " + $conflict.Path + " existing=" + $conflict.Sha256 + " incoming=" + $conflict.Incoming)
    }
    if ($publication.Error) { Write-Output ("IMPORT phase=" + $publication.Phase + " " + $publication.Error) }
    exit 1
}
Invoke-PlayerMeshImport
$bipedLeak = Get-ChildItem -LiteralPath $DevRoot -Recurse -Filter "biped.json" -File -ErrorAction SilentlyContinue
if ($bipedLeak) { throw "Full biped.json was written into the dev directory" }
exit 0
}
