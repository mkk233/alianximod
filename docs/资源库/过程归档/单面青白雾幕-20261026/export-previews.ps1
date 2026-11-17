$ErrorActionPreference='Stop'
$ff='D:/ffmpeg-2023-03-30-git-4d216654ca-essentials_build/bin/ffmpeg.exe'
$dir='D:/alianximod/docs/资源库/特效/水流/单面青白雾幕_v1'
$frames=Join-Path $dir '单帧/frame_%02d.png'
foreach($bg in @(@('dark','172D38'),@('grass','42683D'))){
 & $ff -hide_banner -loglevel error -f lavfi -i "color=c=0x$($bg[1]):s=768x384:r=20:d=1.6" -framerate 20 -i $frames -filter_complex '[1:v]scale=768:384:flags=neighbor[fg];[0:v][fg]overlay=shortest=1,split[a][b];[a]palettegen[p];[b][p]paletteuse=dither=none' -loop 0 -y (Join-Path $dir "mist_$($bg[0]).gif")
 if($LASTEXITCODE){throw 'Preview failed'}
}
& $ff -hide_banner -loglevel error -f lavfi -i 'color=c=0x172D38:s=768x384:r=20:d=1.6' -framerate 20 -i $frames -filter_complex '[1:v]scale=768:384:flags=neighbor,split=3[f1][f2][f3];[0:v][f1]overlay=shortest=1[o1];[o1][f2]overlay=shortest=1[o2];[o2][f3]overlay=shortest=1,split[a][b];[a]palettegen[p];[b][p]paletteuse=dither=none' -loop 0 -y (Join-Path $dir 'mist_three_layers.gif')
if($LASTEXITCODE){throw 'Stack preview failed'}
& $ff -hide_banner -loglevel error -i (Join-Path $dir 'mist_dark.gif') -frames:v 1 -y (Join-Path $dir 'mist_preview.png')
