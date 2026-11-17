$ErrorActionPreference='Stop'
$ff='D:/ffmpeg-2023-03-30-git-4d216654ca-essentials_build/bin/ffmpeg.exe'
$root='D:/alianximod/docs/资源库/过程归档/周身水环3D验证_v4_像素旋冲-20261027'
foreach($spec in @(@('fixed','1.6'),@('orbit','4.8'))){
$kind=$spec[0];$duration=$spec[1]
& $ff -hide_banner -loglevel error -f lavfi -i "color=c=0x172D38:s=720x720:r=20:d=$duration" -framerate 20 -i (Join-Path $root "frames-$kind/frame_%02d.png") -filter_complex "[0:v][1:v]overlay=shortest=1,split[a][b];[a]palettegen[p];[b][p]paletteuse=dither=none" -loop 0 -y (Join-Path $root "water_surge_$kind.gif")
if($LASTEXITCODE){throw 'GIF export failed'}
}
& $ff -hide_banner -loglevel error -i (Join-Path $root 'water_surge_fixed.gif') -frames:v 1 -y (Join-Path $root 'water_surge_preview.png')
