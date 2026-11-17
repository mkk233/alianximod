$ErrorActionPreference='Stop'
$ff='D:/ffmpeg-2023-03-30-git-4d216654ca-essentials_build/bin/ffmpeg.exe'
$assetRoot=$PSScriptRoot
foreach ($view in @('fixed','top')) {
  & $ff -hide_banner -loglevel error -f lavfi -i 'color=c=0x172D38:s=720x720:r=20:d=4.8' -framerate 20 -i (Join-Path $assetRoot ('frames-'+$view+'/frame_%02d.png')) -filter_complex '[0:v][1:v]overlay=shortest=1,split[a][b];[a]palettegen[p];[b][p]paletteuse=dither=none' -loop 0 -y (Join-Path $assetRoot ('water_ragged_'+$view+'.gif'))
  if ($LASTEXITCODE -ne 0) { throw ('GIF export failed: '+$view) }
}

