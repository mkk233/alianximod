$ErrorActionPreference='Stop'
$ff='D:/ffmpeg-2023-03-30-git-4d216654ca-essentials_build/bin/ffmpeg.exe'
$assetRoot=$PSScriptRoot
foreach ($view in @('fixed','orbit','wide')) {
 $expected=if ($view -eq 'orbit') {96} else {32}
 $framePath=Join-Path $assetRoot ('frames-'+$view)
 $count=@(Get-ChildItem -LiteralPath $framePath -Filter '*.png' -File).Count
 if ($count -ne $expected) { Write-Output ('Pending '+$view+': '+$count+'/'+$expected); continue }
 & $ff -hide_banner -loglevel error -framerate 20 -i (Join-Path $framePath 'frame_%03d.png') -filter_complex 'split[a][b];[a]palettegen=max_colors=128[p];[b][p]paletteuse=dither=none' -loop 0 -y (Join-Path $assetRoot ('water_prison_billboard_'+$view+'.gif'))
 if ($LASTEXITCODE -ne 0) { throw ('GIF export failed: '+$view) }
 Write-Output ('Exported '+$view)
}
