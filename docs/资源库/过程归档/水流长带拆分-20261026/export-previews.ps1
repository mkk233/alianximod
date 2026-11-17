$ErrorActionPreference='Stop'
$ff='D:/ffmpeg-2023-03-30-git-4d216654ca-essentials_build/bin/ffmpeg.exe'
$root='D:/alianximod/docs/资源库/特效/水流/水流长带_可扩宽_v2'
foreach($kind in @('taper','charge')){
 & $ff -hide_banner -loglevel error -f lavfi -i 'color=c=0x132833:s=768x576:r=10:d=1.6' -framerate 10 -i (Join-Path $root "单帧/$kind/frame_%02d.png") -filter_complex '[1:v]scale=768:576:flags=neighbor[fg];[0:v][fg]overlay=shortest=1,split[a][b];[a]palettegen[p];[b][p]paletteuse=dither=none' -loop 0 -y (Join-Path $root "example_$kind.gif")
 if($LASTEXITCODE){throw 'Preview export failed'}
 & $ff -hide_banner -loglevel error -i (Join-Path $root "example_$kind.gif") -frames:v 1 -y (Join-Path $root "example_$kind`_preview.png")
}
