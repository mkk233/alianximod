$ErrorActionPreference='Stop'
$ffmpeg='D:\ffmpeg-2023-03-30-git-4d216654ca-essentials_build\bin\ffmpeg.exe'
$assetDir='D:\alianximod\docs\资源库\特效\次元斩\柳叶单斩_v2'
$inputFrames=Join-Path $assetDir '单帧\frame_%02d.png'
& $ffmpeg -hide_banner -loglevel error -f lavfi -i 'color=c=0x253248:s=768x384:r=20:d=1.7' -framerate 20 -i $inputFrames -filter_complex '[1:v]scale=768:384:flags=neighbor,tpad=stop_mode=clone:stop_duration=0.4[fg];[0:v][fg]overlay=shortest=1,split[a][b];[a]palettegen[p];[b][p]paletteuse=dither=none' -loop 0 -y (Join-Path $assetDir 'dimension_willow_v2_preview.gif')
if($LASTEXITCODE){throw 'GIF failed'}
& $ffmpeg -hide_banner -loglevel error -framerate 20 -i $inputFrames -vf 'tile=7x4,scale=1344:384:flags=neighbor' -frames:v 1 -y (Join-Path $assetDir 'dimension_willow_v2_contact.png')
if($LASTEXITCODE){throw 'Contact failed'}
& $ffmpeg -hide_banner -loglevel error -f lavfi -i 'color=c=0x253248:s=768x384:r=1' -i (Join-Path $assetDir '单帧/frame_05.png') -filter_complex '[1:v]scale=768:384:flags=neighbor[fg];[0:v][fg]overlay=shortest=1' -frames:v 1 -y (Join-Path $assetDir 'dimension_willow_v2_keyframe_8x.png')
if($LASTEXITCODE){throw 'Keyframe failed'}


