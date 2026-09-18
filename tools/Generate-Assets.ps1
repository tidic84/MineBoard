# Reproducible source assets. Windows PowerShell + System.Drawing; no external artwork.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$assetRoot = Join-Path $PSScriptRoot '../fabric-1.21.1/src/main/resources/assets/mineboard'
$utf8 = New-Object System.Text.UTF8Encoding($false)
function Write-Json($relative, $value) {
    $path = Join-Path $assetRoot $relative
    [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($path)) | Out-Null
    [IO.File]::WriteAllText($path, ($value | ConvertTo-Json -Depth 30), $utf8)
}
function Brush($hex) { New-Object Drawing.SolidBrush([Drawing.ColorTranslator]::FromHtml($hex)) }
function Save-Image($bitmap, $relative) {
    $path = Join-Path $assetRoot $relative
    [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($path)) | Out-Null
    $bitmap.Save($path, [Drawing.Imaging.ImageFormat]::Png)
    $bitmap.Dispose()
}
$paper = Brush '#F3ECDD'
$ink = Brush '#203638'
$gold = Brush '#DCC184'
$palette = @('#C64E49','#D3A93D','#36836C','#417BA7')
$font = New-Object Drawing.Font('Segoe UI', 41, [Drawing.FontStyle]::Bold, [Drawing.GraphicsUnit]::Pixel)
$small = New-Object Drawing.Font('Segoe UI', 13, [Drawing.FontStyle]::Bold, [Drawing.GraphicsUnit]::Pixel)
$fmt = New-Object Drawing.StringFormat
$fmt.Alignment = [Drawing.StringAlignment]::Center
$fmt.LineAlignment = [Drawing.StringAlignment]::Center
for ($id = -1; $id -lt 40; $id++) {
    $bitmap = New-Object Drawing.Bitmap(64,96)
    $g = [Drawing.Graphics]::FromImage($bitmap)
    $g.SmoothingMode = [Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.TextRenderingHint = [Drawing.Text.TextRenderingHint]::AntiAliasGridFit
    $g.FillRectangle($paper,0,0,64,96)
    if ($id -lt 0) {
        $g.FillRectangle($ink,4,4,56,88)
        $pen = New-Object Drawing.Pen([Drawing.ColorTranslator]::FromHtml('#47615F'),1)
        for ($y = -60; $y -lt 100; $y += 9) { $g.DrawLine($pen,5,$y,59,($y+54)); $g.DrawLine($pen,5,($y+54),59,$y) }
        $g.FillEllipse($gold,10,26,44,44)
        $g.FillEllipse($ink,13,29,38,38)
        $g.DrawString('M',$small,$paper,[Drawing.RectangleF]::new(12,28,40,40),$fmt)
        $g.DrawString('MB',$small,$gold,[Drawing.RectangleF]::new(10,72,44,18),$fmt)
        $texture = 'back'
        $pen.Dispose()
    } else {
        $color = [int][Math]::Floor($id / 10)
        $fill = Brush $palette[$color]
        $g.FillRectangle($fill,4,4,56,88)
        $g.FillEllipse($paper,8,23,48,50)
        $g.DrawString([string]($id % 10),$font,$fill,[Drawing.RectangleF]::new(8,21,48,52),$fmt)
        $g.DrawString([string]($id % 10),$small,$paper,7,5)
        $g.DrawString([string]($id % 10),$small,$paper,46,73)
        # A different small symbol for each color, in both corners.
        foreach ($point in @(@(47,10),@(9,79))) {
            $x = [int]$point[0]; $y = [int]$point[1]
            switch ($color) {
                0 { $g.FillEllipse($paper,$x,$y,8,8) }
                1 { $g.FillPolygon($paper,[Drawing.Point[]]@([Drawing.Point]::new($x+4,$y),[Drawing.Point]::new($x+8,$y+4),[Drawing.Point]::new($x+4,$y+8),[Drawing.Point]::new($x,$y+4))) }
                2 { $g.FillPolygon($paper,[Drawing.Point[]]@([Drawing.Point]::new($x+4,$y),[Drawing.Point]::new($x+8,$y+8),[Drawing.Point]::new($x,$y+8))) }
                3 { $g.FillRectangle($paper,$x,$y,8,8) }
            }
        }
        $fill.Dispose()
        $texture = "card_$id"
    }
    $g.Dispose()
    Save-Image $bitmap "textures/item/$texture.png"
    $faces = @{
        up=@{uv=@(0,0,16,16);texture='#front'}
        down=@{uv=@(0,0,16,16);texture='#back'}
    }
    foreach ($side in @('north','south','east','west')) { $faces[$side] = @{uv=@(0,0,16,1);texture='#edge'} }
    $model = @{
        textures=@{front="mineboard:item/$texture";back='mineboard:item/back';edge='minecraft:block/white_concrete';particle="mineboard:item/$texture"}
        elements=@(@{from=@(4,7.85,2);to=@(12,8.15,14);faces=$faces})
        display=@{
            gui=@{rotation=@(90,0,0);translation=@(0,0,0);scale=@(1,1,1)}
            fixed=@{rotation=@(90,0,0);scale=@(1,1,1)}
            ground=@{scale=@(0.5,0.5,0.5)}
            firstperson_righthand=@{rotation=@(80,0,0);translation=@(0,2,0);scale=@(1,1,1)}
        }
        gui_light='front'
    }
    Write-Json "models/item/$texture.json" $model
}
$overrides = @()
for ($id=0;$id -lt 40;$id++) { $overrides += @{predicate=@{custom_model_data=($id+1)};model="mineboard:item/card_$id"} }
Write-Json 'models/item/card.json' @{parent='mineboard:item/back';overrides=$overrides}
$bitmap = New-Object Drawing.Bitmap(128,128)
$g = [Drawing.Graphics]::FromImage($bitmap)
$g.Clear([Drawing.ColorTranslator]::FromHtml('#214B45'))
$rng = New-Object Random(18)
for ($i=0;$i -lt 7000;$i++) {
    $value = $rng.Next(0,15)
    $bitmap.SetPixel($rng.Next(128),$rng.Next(128),[Drawing.Color]::FromArgb(26+$value,64+$value,58+$value))
}
$pen = New-Object Drawing.Pen([Drawing.ColorTranslator]::FromHtml('#BCA574'),1)
$g.DrawRectangle($pen,5,5,117,117)
$g.DrawRectangle($pen,8,8,111,111)
$g.DrawRectangle($pen,25,40,29,46)
$g.DrawRectangle($pen,74,40,29,46)
$g.DrawString('MINEBOARD',$small,$gold,[Drawing.RectangleF]::new(10,12,108,18),$fmt)
$g.Dispose(); $pen.Dispose(); Save-Image $bitmap 'textures/block/felt.png'
$faces = @{}
foreach ($side in @('up','down','north','south','east','west')) { $faces[$side] = @{texture='#wood'} }
$feltFaces = @{}
foreach ($side in @('up','down','north','south','east','west')) { $feltFaces[$side] = @{texture='#felt'} }
Write-Json 'models/block/table.json' @{
    textures=@{wood='minecraft:block/dark_oak_planks';felt='mineboard:block/felt';particle='minecraft:block/dark_oak_planks'}
    elements=@(
        @{from=@(0,0,0);to=@(16,1.8,16);faces=$faces},
        @{from=@(0.5,1.8,0.5);to=@(15.5,2.1,15.5);faces=$feltFaces}
    )
}
Write-Json 'blockstates/table.json' @{variants=@{''=@{model='mineboard:block/table'}}}
Write-Json 'models/item/table.json' @{parent='mineboard:block/table';display=@{gui=@{rotation=@(30,45,0);scale=@(0.8,0.8,0.8)}}}
$font.Dispose(); $small.Dispose(); $fmt.Dispose(); $paper.Dispose(); $ink.Dispose(); $gold.Dispose()
Write-Host 'Generated 41 card textures, 3D models and felt board.'
