$ErrorActionPreference='Stop'
$ffmpeg='D:\ffmpeg-2023-03-30-git-4d216654ca-essentials_build\bin\ffmpeg.exe'
$assetDir='D:\alianximod\docs\资源库\特效\水流\水流长带_v1'
$inputFrames=Join-Path $assetDir '单帧\frame_%02d.png'
& $ffmpeg -hide_banner -loglevel error -framerate 10 -i $inputFrames -filter_complex '[0:v]scale=768:192:flags=neighbor,split[a][b];[a]palettegen=reserve_transparent=1:stats_mode=full[p];[b][p]paletteuse=dither=none:alpha_threshold=128' -loop 0 -y (Join-Path $assetDir 'water_ribbon_transparent.gif')
if($LASTEXITCODE){throw 'Transparent GIF failed'}
& $ffmpeg -hide_banner -loglevel error -f lavfi -i 'color=c=0x0b2037:s=768x192:r=10:d=1.6' -framerate 10 -i $inputFrames -filter_complex '[1:v]scale=768:192:flags=neighbor[fg];[0:v][fg]overlay=shortest=1,split[a][b];[a]palettegen[p];[b][p]paletteuse=dither=none' -loop 0 -y (Join-Path $assetDir 'water_ribbon_preview.gif')
if($LASTEXITCODE){throw 'Preview GIF failed'}
& $ffmpeg -hide_banner -loglevel error -framerate 10 -i $inputFrames -filter_complex '[0:v]split[l][r];[l][r]hstack,scale=1024:128:flags=neighbor,split[a][b];[a]palettegen=reserve_transparent=1[p];[b][p]paletteuse=dither=none:alpha_threshold=128' -loop 0 -y (Join-Path $assetDir 'water_ribbon_tile_preview.gif')
if($LASTEXITCODE){throw 'Tiled preview failed'}
& $ffmpeg -hide_banner -loglevel error -framerate 10 -i $inputFrames -vf 'tile=2x8,scale=1024:1024:flags=neighbor' -frames:v 1 -y (Join-Path $assetDir 'water_ribbon_contact.png')
if($LASTEXITCODE){throw 'Contact sheet failed'}
