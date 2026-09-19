# Pixel pictograms for city-card families; no font-dependent placeholder letters.
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$assets=Join-Path $PSScriptRoot '../fabric-1.21.1/src/main/resources/assets/mineboard/textures/item'
foreach ($pair in @(@('brown','#805637'),@('grey','#667B80'),@('yellow','#BD8B31'),@('blue','#386991'),@('green','#427657'),@('red','#A44F42'),@('purple','#755284'))) {
    $name=$pair[0]
    $bitmap=[Drawing.Bitmap]::new(64,96)
    $g=[Drawing.Graphics]::FromImage($bitmap)
    $g.SmoothingMode=[Drawing.Drawing2D.SmoothingMode]::None
    $paper=[Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml('#F3ECDD'))
    $ink=[Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml($pair[1]))
    $gold=[Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml('#D3B470'))
    $dark=[Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml('#283D3D'))
    $g.Clear([Drawing.ColorTranslator]::FromHtml($pair[1]))
    $g.FillRectangle($paper,3,3,58,90)
    $g.FillRectangle($ink,6,6,52,15)
    $g.FillRectangle($gold,6,23,52,2)
    $g.FillRectangle($ink,6,76,52,14)
    $g.FillRectangle($gold,11,81,42,2)
    $g.FillRectangle($paper,18,86,28,1)
    switch ($name) {
        brown {
            foreach ($y in @(35,48,61)) {
                $g.FillRectangle($dark,13,$y,36,9)
                $g.FillRectangle($ink,13,$y,36,6)
                $g.FillRectangle($gold,17,($y+2),25,1)
            }
        }
        grey {
            foreach ($at in @(@(13,54),@(34,54),@(23,38))) {
                $x=$at[0];$y=$at[1]
                $g.FillRectangle($dark,$x,$y,18,13)
                $g.FillRectangle($ink,($x+1),$y,16,9)
                $g.FillRectangle($gold,($x+3),($y+2),12,2)
            }
        }
        yellow {
            foreach ($y in @(57,50,43)) { $g.FillEllipse($dark,13,$y,28,12); $g.FillEllipse($gold,13,($y-3),28,12) }
            $g.FillEllipse($ink,31,34,21,21);$g.FillEllipse($gold,34,37,15,15);$g.FillRectangle($paper,41,39,2,11)
        }
        blue {
            $g.FillPolygon($ink,[Drawing.Point[]]@([Drawing.Point]::new(10,43),[Drawing.Point]::new(32,30),[Drawing.Point]::new(54,43)))
            foreach ($x in @(15,29,43)) {$g.FillRectangle($ink,$x,46,6,18);$g.FillRectangle($gold,$x,46,2,18)}
            $g.FillRectangle($ink,11,65,42,4);$g.FillRectangle($gold,9,70,46,2)
        }
        green {
            $g.FillRectangle($ink,25,31,14,5);$g.FillRectangle($ink,28,36,8,14)
            $g.FillPolygon($ink,[Drawing.Point[]]@([Drawing.Point]::new(28,44),[Drawing.Point]::new(12,67),[Drawing.Point]::new(52,67),[Drawing.Point]::new(36,44)))
            $g.FillRectangle($gold,23,57,18,6);$g.FillRectangle($paper,29,40,2,11);$g.FillRectangle($paper,38,59,3,3)
        }
        red {
            $g.FillPolygon($ink,[Drawing.Point[]]@([Drawing.Point]::new(14,34),[Drawing.Point]::new(50,34),[Drawing.Point]::new(47,57),[Drawing.Point]::new(32,70),[Drawing.Point]::new(17,57)))
            $g.FillRectangle($gold,30,39,4,20);$g.FillRectangle($gold,23,49,18,4)
        }
        purple {
            $g.FillRectangle($ink,21,60,22,5)
            $g.FillPolygon($ink,[Drawing.Point[]]@([Drawing.Point]::new(18,41),[Drawing.Point]::new(25,48),[Drawing.Point]::new(32,34),[Drawing.Point]::new(39,48),[Drawing.Point]::new(46,41),[Drawing.Point]::new(43,58),[Drawing.Point]::new(21,58)))
            foreach ($x in @(12,48)) {foreach ($y in @(45,54,63)) {$g.FillRectangle($gold,$x,$y,4,6)}}
            $g.FillRectangle($gold,30,49,4,5)
        }
    }
    $g.Dispose();$paper.Dispose();$ink.Dispose();$gold.Dispose();$dark.Dispose()
    $bitmap.Save((Join-Path $assets "ages_$name.png"),[Drawing.Imaging.ImageFormat]::Png);$bitmap.Dispose()
}
Write-Host 'Generated seven city-card pictograms.'
Copy-Item (Join-Path $assets 'ages_blue.png') (Join-Path $assets 'wonder.png') -Force
$bitmap=[Drawing.Bitmap]::new(64,64)
$g=[Drawing.Graphics]::FromImage($bitmap)
$g.Clear([Drawing.ColorTranslator]::FromHtml('#C89A40'))
$rim=[Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml('#F1D18A'))
$center=[Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml('#AD7C2F'))
$g.FillEllipse($rim,5,5,54,54);$g.FillEllipse($center,10,10,44,44)
$g.FillRectangle($rim,27,19,10,26);$g.FillRectangle($rim,19,27,26,10)
$g.Dispose();$rim.Dispose();$center.Dispose()
$bitmap.Save((Join-Path $assets 'coin.png'),[Drawing.Imaging.ImageFormat]::Png);$bitmap.Dispose()
& (Join-Path $PSScriptRoot 'Generate-AgeIllustrations.ps1')
