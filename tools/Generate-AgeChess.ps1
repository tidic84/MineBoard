# Age-city cards, wonder plaques, coins and chess men.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$root = Join-Path $PSScriptRoot '..'
$assetRoot = Join-Path $root 'fabric-1.21.1/src/main/resources/assets/mineboard'
$items26 = Join-Path $root 'minecraft-26.2/src/main/resources/assets/mineboard/items'
$utf8 = New-Object System.Text.UTF8Encoding($false)
function Write-Text($path, $text) {
    [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($path)) | Out-Null
    [IO.File]::WriteAllText($path, $text.Trim().Replace("`r`n", "`n") + "`n", $utf8)
}
function Save-Image($bitmap, $relative) {
    $path = Join-Path $assetRoot $relative
    [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($path)) | Out-Null
    $bitmap.Save($path, [Drawing.Imaging.ImageFormat]::Png)
    $bitmap.Dispose()
}
function New-Face($hex, $mark, $name) {
    $bitmap = New-Object Drawing.Bitmap(32, 48)
    $g = [Drawing.Graphics]::FromImage($bitmap)
    $g.SmoothingMode = [Drawing.Drawing2D.SmoothingMode]::None
    $base = [Drawing.ColorTranslator]::FromHtml($hex)
    $g.Clear($base)
    $ink = New-Object Drawing.SolidBrush ([Drawing.ColorTranslator]::FromHtml('#F4EFE4'))
    $g.FillRectangle($ink, 3, 3, 26, 42)
    $fill = New-Object Drawing.SolidBrush $base
    $g.FillRectangle($fill, 5, 5, 22, 38)
    $fmt = New-Object Drawing.StringFormat
    $fmt.Alignment = [Drawing.StringAlignment]::Center
    $fmt.LineAlignment = [Drawing.StringAlignment]::Center
    $font = New-Object Drawing.Font('Segoe UI', 12, [Drawing.FontStyle]::Bold, [Drawing.GraphicsUnit]::Pixel)
    $g.DrawString($mark, $font, $ink, [Drawing.RectangleF]::new(0, 0, 32, 48), $fmt)
    $g.Dispose(); $ink.Dispose(); $fill.Dispose(); $fmt.Dispose(); $font.Dispose()
    Save-Image $bitmap "textures/item/$name.png"
}
function New-Letter($fillHex, $ringHex, $letter, $name) {
    $bitmap = New-Object Drawing.Bitmap(64, 64)
    $g = [Drawing.Graphics]::FromImage($bitmap)
    $g.SmoothingMode = [Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.Clear([Drawing.ColorTranslator]::FromHtml($fillHex))
    $fill = New-Object Drawing.SolidBrush ([Drawing.ColorTranslator]::FromHtml($fillHex))
    $ring = New-Object Drawing.Pen ([Drawing.ColorTranslator]::FromHtml($ringHex), 3)
    $g.FillEllipse($fill, 8, 8, 48, 48)
    $g.DrawEllipse($ring, 9, 9, 46, 46)
    $ink = New-Object Drawing.SolidBrush ([Drawing.ColorTranslator]::FromHtml('#F4EFE4'))
    $fmt = New-Object Drawing.StringFormat
    $fmt.Alignment = [Drawing.StringAlignment]::Center
    $fmt.LineAlignment = [Drawing.StringAlignment]::Center
    $font = New-Object Drawing.Font('Segoe UI', 18, [Drawing.FontStyle]::Bold, [Drawing.GraphicsUnit]::Pixel)
    $g.DrawString($letter, $font, $ink, [Drawing.RectangleF]::new(0, 0, 64, 64), $fmt)
    $g.Dispose(); $fill.Dispose(); $ring.Dispose(); $ink.Dispose(); $fmt.Dispose(); $font.Dispose()
    Save-Image $bitmap "textures/item/$name.png"
}
function Card-Model($name) {
@"
{
  "parent": "mineboard:item/card_0",
  "textures": {
    "front": "mineboard:item/$name",
    "particle": "mineboard:item/$name",
    "back": "mineboard:item/back"
  }
}
"@
}
function Item-Def($name) {
@"
{
  "model": {
    "type": "minecraft:model",
    "model": "mineboard:item/$name"
  }
}
"@
}

$ages = @(
    @('#8B5A2B', 'W', 'ages_brown'),
    @('#9A9A9A', 'G', 'ages_grey'),
    @('#C9A227', '$', 'ages_yellow'),
    @('#3D6EA8', 'V', 'ages_blue'),
    @('#3F8F5B', 'S', 'ages_green'),
    @('#A33B3B', 'M', 'ages_red'),
    @('#6B3FA0', 'P', 'ages_purple')
)
foreach ($row in $ages) {
    New-Face $row[0] $row[1] $row[2]
    Write-Text (Join-Path $assetRoot "models/item/$($row[2]).json") (Card-Model $row[2])
    Write-Text (Join-Path $items26 "$($row[2]).json") (Item-Def $row[2])
}
New-Face '#6E4B2E' 'W' 'wonder'
New-Letter '#D4B24A' '#8A6A3E' 'C' 'coin'
Write-Text (Join-Path $assetRoot 'models/item/wonder.json') (Card-Model 'wonder')
Write-Text (Join-Path $items26 'wonder.json') (Item-Def 'wonder')
Write-Text (Join-Path $assetRoot 'models/item/coin.json') ((Get-Content (Join-Path $assetRoot 'models/item/token_light.json') -Raw).Replace('token_light', 'coin'))
Write-Text (Join-Path $items26 'coin.json') (Item-Def 'coin')

$letters = @('P','N','B','R','Q','K')
$names = @('pawn','knight','bishop','rook','queen','king')
for ($i = 0; $i -lt 6; $i++) {
    New-Letter '#E6D5B0' '#8A6A3E' $letters[$i] "chess_$($names[$i])_light"
    New-Letter '#2A2421' '#C4A574' $letters[$i] "chess_$($names[$i])_dark"
    foreach ($tone in @('light','dark')) {
        $name = "chess_$($names[$i])_$tone"
        $src = if ($tone -eq 'light') { 'token_light' } else { 'token_dark' }
        Write-Text (Join-Path $assetRoot "models/item/$name.json") ((Get-Content (Join-Path $assetRoot "models/item/$src.json") -Raw).Replace($src, $name))
        Write-Text (Join-Path $items26 "$name.json") (Item-Def $name)
    }
}

$card = Join-Path $assetRoot 'models/item/card.json'
$model = Get-Content $card -Raw | ConvertFrom-Json
$routes = @($model.overrides | Where-Object { $_.predicate.custom_model_data -lt 47 })
$models = @()
foreach ($tone in @('light','dark')) { foreach ($name in $names) { $models += "chess_${name}_$tone" } }
$models += @('ages_brown','ages_grey','ages_yellow','ages_blue','ages_green','ages_red','ages_purple','wonder','coin')
for ($i=0; $i -lt $models.Count; $i++) {
    $routes += @{ predicate=@{custom_model_data=(47+$i)}; model="mineboard:item/$($models[$i])" }
}
$model.overrides = $routes
Write-Text $card ($model | ConvertTo-Json -Depth 30)
Write-Host 'Generated age cards, wonder, coin and chess pieces.'
& (Join-Path $PSScriptRoot 'Generate-ChessModels.ps1')
& (Join-Path $PSScriptRoot 'Generate-AgeFaces.ps1')
