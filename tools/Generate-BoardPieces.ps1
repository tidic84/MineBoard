# Draughts cells and tokens. Same System.Drawing pipeline as Generate-Assets.ps1.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$root = Join-Path $PSScriptRoot '..'
$assetRoot = Join-Path $root 'fabric-1.21.1/src/main/resources/assets/mineboard'
$items26 = Join-Path $root 'minecraft-26.2/src/main/resources/assets/mineboard/items'
$utf8 = New-Object System.Text.UTF8Encoding($false)
function Write-Text($path, $text) {
    [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($path)) | Out-Null
    [IO.File]::WriteAllText($path, $text.Trim() + "`n", $utf8)
}
function Save-Image($bitmap, $relative) {
    $path = Join-Path $assetRoot $relative
    [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($path)) | Out-Null
    $bitmap.Save($path, [Drawing.Imaging.ImageFormat]::Png)
    $bitmap.Dispose()
}
function New-Cell($hex, $name) {
    $bitmap = New-Object Drawing.Bitmap(32, 32)
    $g = [Drawing.Graphics]::FromImage($bitmap)
    $g.SmoothingMode = [Drawing.Drawing2D.SmoothingMode]::None
    $base = [Drawing.ColorTranslator]::FromHtml($hex)
    $g.Clear($base)
    $rng = New-Object Random (($name.GetHashCode()) -band 0x7fffffff)
    for ($i = 0; $i -lt 180; $i++) {
        $d = $rng.Next(-10, 11)
        $c = [Drawing.Color]::FromArgb(
            [Math]::Max(0, [Math]::Min(255, $base.R + $d)),
            [Math]::Max(0, [Math]::Min(255, $base.G + $d)),
            [Math]::Max(0, [Math]::Min(255, $base.B + $d)))
        $bitmap.SetPixel($rng.Next(32), $rng.Next(32), $c)
    }
    $edge = New-Object Drawing.Pen ([Drawing.Color]::FromArgb(90, 0, 0, 0), 1)
    $g.DrawRectangle($edge, 0, 0, 31, 31)
    $g.Dispose(); $edge.Dispose()
    Save-Image $bitmap "textures/item/$name.png"
}
function New-Token($fillHex, $ringHex, $king, $name) {
    $bitmap = New-Object Drawing.Bitmap(64, 64)
    $g = [Drawing.Graphics]::FromImage($bitmap)
    $g.SmoothingMode = [Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.Clear([Drawing.Color]::FromArgb(0, 0, 0, 0))
    $fill = New-Object Drawing.SolidBrush ([Drawing.ColorTranslator]::FromHtml($fillHex))
    $ring = New-Object Drawing.Pen ([Drawing.ColorTranslator]::FromHtml($ringHex), 3)
    $g.FillEllipse($fill, 6, 6, 52, 52)
    $g.DrawEllipse($ring, 7, 7, 50, 50)
    $inner = New-Object Drawing.Pen ([Drawing.Color]::FromArgb(80, 255, 255, 255), 2)
    $g.DrawEllipse($inner, 14, 14, 36, 36)
    if ($king) {
        $gold = New-Object Drawing.SolidBrush ([Drawing.ColorTranslator]::FromHtml('#DCC184'))
        $g.FillEllipse($gold, 22, 22, 20, 20)
        $ink = New-Object Drawing.SolidBrush ([Drawing.ColorTranslator]::FromHtml('#203638'))
        $fmt = New-Object Drawing.StringFormat
        $fmt.Alignment = [Drawing.StringAlignment]::Center
        $fmt.LineAlignment = [Drawing.StringAlignment]::Center
        $font = New-Object Drawing.Font('Segoe UI', 11, [Drawing.FontStyle]::Bold, [Drawing.GraphicsUnit]::Pixel)
        $g.DrawString('D', $font, $ink, [Drawing.RectangleF]::new(22, 22, 20, 20), $fmt)
        $gold.Dispose(); $ink.Dispose(); $fmt.Dispose(); $font.Dispose()
    }
    $g.Dispose(); $fill.Dispose(); $ring.Dispose(); $inner.Dispose()
    Save-Image $bitmap "textures/item/$name.png"
}
function Cell-Model($name) {
    @"
{
  "gui_light": "front",
  "textures": { "all": "mineboard:item/$name", "particle": "mineboard:item/$name" },
  "elements": [
    {
      "from": [0.2, 7.94, 0.2],
      "to": [15.8, 8.06, 15.8],
      "faces": {
        "up": { "uv": [0, 0, 16, 16], "texture": "#all" },
        "down": { "uv": [0, 0, 16, 16], "texture": "#all" },
        "north": { "uv": [0, 0, 16, 1], "texture": "#all" },
        "south": { "uv": [0, 0, 16, 1], "texture": "#all" },
        "east": { "uv": [0, 0, 16, 1], "texture": "#all" },
        "west": { "uv": [0, 0, 16, 1], "texture": "#all" }
      }
    }
  ]
}
"@
}
function Token-Model($name, $king) {
    $top = if ($king) { 'minecraft:block/gold_block' } else { "mineboard:item/$name" }
    $crown = if ($king) {
@'
    ,{
      "from": [5.2, 8.5, 5.2],
      "to": [10.8, 9.2, 10.8],
      "faces": {
        "up": { "uv": [4, 4, 12, 12], "texture": "#crown" },
        "down": { "uv": [4, 4, 12, 12], "texture": "#crown" },
        "north": { "uv": [4, 4, 12, 8], "texture": "#crown" },
        "south": { "uv": [4, 4, 12, 8], "texture": "#crown" },
        "east": { "uv": [4, 4, 12, 8], "texture": "#crown" },
        "west": { "uv": [4, 4, 12, 8], "texture": "#crown" }
      }
    },
    {
      "from": [5.2, 8.5, 5.2],
      "to": [10.8, 9.2, 10.8],
      "rotation": { "angle": 45, "axis": "y", "origin": [8, 8, 8] },
      "faces": {
        "up": { "uv": [4, 4, 12, 12], "texture": "#crown" },
        "north": { "uv": [4, 4, 12, 8], "texture": "#crown" },
        "south": { "uv": [4, 4, 12, 8], "texture": "#crown" },
        "east": { "uv": [4, 4, 12, 8], "texture": "#crown" },
        "west": { "uv": [4, 4, 12, 8], "texture": "#crown" }
      }
    }
'@
    } else { '' }
    @"
{
  "gui_light": "front",
  "textures": { "all": "mineboard:item/$name", "crown": "$top", "particle": "mineboard:item/$name" },
  "elements": [
    {
      "from": [3, 7.7, 3],
      "to": [13, 8.5, 13],
      "faces": {
        "up": { "uv": [0, 0, 16, 16], "texture": "#all" },
        "down": { "uv": [0, 0, 16, 16], "texture": "#all" },
        "north": { "uv": [0, 12, 16, 16], "texture": "#all" },
        "south": { "uv": [0, 12, 16, 16], "texture": "#all" },
        "east": { "uv": [0, 12, 16, 16], "texture": "#all" },
        "west": { "uv": [0, 12, 16, 16], "texture": "#all" }
      }
    },
    {
      "from": [3, 7.7, 3],
      "to": [13, 8.5, 13],
      "rotation": { "angle": 45, "axis": "y", "origin": [8, 8, 8] },
      "faces": {
        "up": { "uv": [0, 0, 16, 16], "texture": "#all" },
        "north": { "uv": [0, 12, 16, 16], "texture": "#all" },
        "south": { "uv": [0, 12, 16, 16], "texture": "#all" },
        "east": { "uv": [0, 12, 16, 16], "texture": "#all" },
        "west": { "uv": [0, 12, 16, 16], "texture": "#all" }
      }
    }$crown
  ]
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

New-Cell '#D5C19A' 'cell_light'
New-Cell '#4E3426' 'cell_dark'
New-Token '#E6D5B0' '#8A6A3E' $false 'token_light'
New-Token '#2A2421' '#C4A574' $false 'token_dark'
New-Token '#E6D5B0' '#DCC184' $true 'token_king_light'
New-Token '#2A2421' '#DCC184' $true 'token_king_dark'

foreach ($name in @('cell_light', 'cell_dark')) {
    Write-Text (Join-Path $assetRoot "models/item/$name.json") (Cell-Model $name)
    Write-Text (Join-Path $items26 "$name.json") (Item-Def $name)
}
foreach ($pair in @(@('token_light', $false), @('token_dark', $false), @('token_king_light', $true), @('token_king_dark', $true))) {
    $name = $pair[0]; $king = $pair[1]
    Write-Text (Join-Path $assetRoot "models/item/$name.json") (Token-Model $name $king)
    Write-Text (Join-Path $items26 "$name.json") (Item-Def $name)
}
Write-Host 'Generated draughts cells, tokens and 26.2 item definitions.'
