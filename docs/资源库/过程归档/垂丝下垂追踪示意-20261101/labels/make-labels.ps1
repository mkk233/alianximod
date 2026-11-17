
Add-Type -AssemblyName System.Drawing
$out = "D:/alianximod/docs/资源库/过程归档/垂丝下垂追踪示意-20261101/labels"
function Save-Text([string]$name, [string]$text, [int]$w, [int]$h, [int]$size, [int]$r, [int]$g, [int]$b) {
  $bmp = New-Object System.Drawing.Bitmap $w, $h
  $bmp.MakeTransparent()
  $gfx = [System.Drawing.Graphics]::FromImage($bmp)
  $gfx.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
  $gfx.Clear([System.Drawing.Color]::FromArgb(0, 0, 0, 0))
  $text = $text.Replace("||", [Environment]::NewLine)
  $font = New-Object System.Drawing.Font("Microsoft YaHei", [single]$size, [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)
  $brush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, $r, $g, $b))
  $format = New-Object System.Drawing.StringFormat
  $format.Alignment = [System.Drawing.StringAlignment]::Center
  $format.LineAlignment = [System.Drawing.StringAlignment]::Center
  $rect = New-Object System.Drawing.RectangleF 0, 1, $w, ($h - 2)
  $gfx.DrawString($text, $font, $brush, $rect, $format)
  $bmp.Save((Join-Path $out $name), [System.Drawing.Imaging.ImageFormat]::Png)
  $gfx.Dispose(); $font.Dispose(); $brush.Dispose(); $bmp.Dispose()
}
Save-Text "title-1.png" "锁定 → 垂丝" 180 22 13 215 244 255
Save-Text "title-2.png" "锁定 → 追踪 → 垂丝" 180 22 12 215 244 255
Save-Text "title-3.png" "领域 → 追踪 → 垂丝" 180 22 12 215 244 255
Save-Text "cap-1.png" "上沿沿锁定方向伸出后停住||水从这道上沿往下泻" 180 34 11 176 214 224
Save-Text "cap-2.png" "上沿改由追踪移动||帘身仍从新的上沿垂下" 180 34 11 176 214 224
Save-Text "cap-3.png" "上沿生在域内随机点||不再额外抬高" 180 34 11 176 214 224
Save-Text "note.png" "看得见的帘就是判定；上沿移动时，两拍之间扫过的水也算命中" 560 16 12 198 248 250
Write-Output "labels-ok"
