$ErrorActionPreference='Stop'
$ff='D:/ffmpeg-2023-03-30-git-4d216654ca-essentials_build/bin/ffmpeg.exe'
$root='D:/alianximod/docs/资源库/特效/水流/周身水环_Aseprite_v2'
foreach($spec in @(@('ring',768,384),@('band',768,96),@('body',768,576))){
$kind=$spec[0];$w=$spec[1];$h=$spec[2]
& $ff -hide_banner -loglevel error -f lavfi -i "color=c=0x172D38:s=$($w)x$($h):r=10:d=1.6" -framerate 10 -i (Join-Path $root "单帧/$kind/frame_%02d.png") -filter_complex "[1:v]scale=$($w):$($h):flags=neighbor[fg];[0:v][fg]overlay=shortest=1,split[a][b];[a]palettegen[p];[b][p]paletteuse=dither=none" -loop 0 -y (Join-Path $root "water_$kind`_preview.gif")
if($LASTEXITCODE){throw 'Preview export failed'}
}
& $ff -hide_banner -loglevel error -i (Join-Path $root 'water_ring_preview.gif') -frames:v 1 -y (Join-Path $root 'water_ring_preview.png')

