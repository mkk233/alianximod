$ErrorActionPreference='Stop'
$ffmpeg='D:\ffmpeg-2023-03-30-git-4d216654ca-essentials_build\bin\ffmpeg.exe'
$assetDir='D:\alianximod\docs\资源库\特效\水流\水雾细雨80度_v1'
$inputFrames=Join-Path $assetDir '单帧\frame_%02d.png'
& $ffmpeg -hide_banner -loglevel error -f lavfi -i 'color=c=0x102431:s=512x512:r=20:d=1.6' -framerate 20 -i $inputFrames -filter_complex '[1:v]scale=512:512:flags=neighbor[fg];[0:v][fg]overlay=shortest=1,split[a][b];[a]palettegen[p];[b][p]paletteuse=dither=none' -loop 0 -y (Join-Path $assetDir 'mist_rain_80_preview.gif')
if($LASTEXITCODE){throw 'GIF export failed'}
& $ffmpeg -hide_banner -loglevel error -framerate 20 -i $inputFrames -vf 'tile=8x4,scale=1024:512:flags=neighbor' -frames:v 1 -y (Join-Path $assetDir 'mist_rain_80_contact.png')
if($LASTEXITCODE){throw 'Contact export failed'}
& $ffmpeg -hide_banner -loglevel error -f lavfi -i 'color=c=0x102431:s=512x512:r=1' -i (Join-Path $assetDir 'mist_rain_80_keyframe_8x.png') -filter_complex '[0:v][1:v]overlay=shortest=1' -frames:v 1 -y (Join-Path $assetDir 'mist_rain_80_preview.png')
if($LASTEXITCODE){throw 'Still export failed'}

