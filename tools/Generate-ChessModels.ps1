# Solid voxel chess silhouettes. Called by Generate-AgeChess.ps1.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$assets = Join-Path $PSScriptRoot '../fabric-1.21.1/src/main/resources/assets/mineboard'
$utf8 = [Text.UTF8Encoding]::new($false)
function Box($x0,$y0,$z0,$x1,$y1,$z1,$material=0) {
    $v = 1 + $material * 4
    $faces = [ordered]@{}
    foreach ($side in @('up','down','north','south','east','west')) {
        $faces[$side] = @{texture='#all'; uv=@(1,$v,15,($v+2))}
    }
    $script:parts.Add(@{from=@($x0,$y0,$z0); to=@($x1,$y1,$z1); faces=$faces})
}
function Ring($width,$bottom,$top,$material=0) {
    # Three adjoining rectangular bands give clipped corners without intersecting solids.
    $r=$width/2; $cut=$width/5
    Box (8-$r+$cut) $bottom (8-$r) (8+$r-$cut) $top (8-$r+$cut) $material
    Box (8-$r) $bottom (8-$r+$cut) (8+$r) $top (8+$r-$cut) $material
    Box (8-$r+$cut) $bottom (8+$r-$cut) (8+$r-$cut) $top (8+$r) $material
}
foreach ($tone in @('light','dark')) {
    foreach ($piece in @('pawn','knight','bishop','rook','queen','king')) {
        $name="chess_${piece}_$tone"
        $bitmap=[Drawing.Bitmap]::new(16,16)
        $colors=if ($tone -eq 'light') { @('#E4D3AC','#AA8454','#D1AD5B','#675037') } else { @('#443735','#271F21','#C6A05C','#F0D6A0') }
        for ($y=0;$y -lt 16;$y++) { for ($x=0;$x -lt 16;$x++) {
            $bitmap.SetPixel($x,$y,[Drawing.ColorTranslator]::FromHtml($colors[[int][Math]::Floor($y/4)]))
        }}
        $bitmap.Save((Join-Path $assets "textures/item/$name.png"),[Drawing.Imaging.ImageFormat]::Png)
        $bitmap.Dispose()
        $script:parts=[Collections.Generic.List[object]]::new()
        Ring 10 6.8 7.6 1
        Ring 10 7.6 8.1 2
        Ring 8.5 8.1 9.1
        Ring 6.5 9.1 10
        switch ($piece) {
            pawn {
                Ring 4 10 12
                Ring 5.5 12 12.7 2
                Ring 4.5 12.7 13.3
                Ring 5.5 13.3 15.2
                Ring 3.5 15.2 16
            }
            rook {
                Ring 6 10 15.3
                Ring 8 15.3 16.2 2
                Ring 8 16.2 17
                foreach ($x in @(4,9.5)) { foreach ($z in @(4,9.5)) { Box $x 17 $z ($x+2.5) 19 ($z+2.5) } }
            }
            knight {
                Ring 6.5 10 10.8 2
                Box 6 10.8 6 10 13 11
                Box 6 13 5 10 15 10.5
                Box 6 15 3.5 10 17 10
                Box 6.4 17 4.2 9.6 18.3 9.8
                Box 6.4 18.3 8 7.5 20 9.8
                Box 8.5 18.3 8 9.6 20 9.8
            }
            bishop {
                Ring 4.5 10 14
                Ring 6 14 14.8 2
                Ring 4 14.8 15.5
                Ring 6 15.5 17
                Box 5.5 17 6 7.5 19 10
                Box 8.3 17 6 10.5 18.5 10
                Box 6.2 19 6.8 7.5 20 9.2
            }
            queen {
                Ring 4.5 10 15
                Ring 6 15 15.8 2
                Ring 5 15.8 17
                Ring 7 17 18.4
                foreach ($x in @(4.5,9.5)) { foreach ($z in @(4.5,9.5)) { Box $x 18.4 $z ($x+2) 20.6 ($z+2) 2 } }
                Ring 2.5 18.4 21 2
            }
            king {
                Ring 5 10 16
                Ring 6.5 16 16.8 2
                Ring 5.5 16.8 18
                Ring 7 18 19 2
                Box 7 19 7 9 20.5 9
                Box 5.2 20.5 7 10.8 22 9 2
                Box 7 22 7 9 24 9 2
            }
        }
        $model=[ordered]@{textures=@{all="mineboard:item/$name";particle="mineboard:item/$name"};elements=$script:parts.ToArray()}
        [IO.File]::WriteAllText((Join-Path $assets "models/item/$name.json"),($model | ConvertTo-Json -Depth 20).Replace("`r`n", "`n")+"`n",$utf8)
    }
}
Write-Host 'Generated twelve solid chess models with distinct silhouettes.'
# An actual framed plaque, matching the 14 x 10 footprint used for picking.
$script:parts=[Collections.Generic.List[object]]::new()
Box 1 7.5 3 15 8.3 13 1
Box 1.5 8.3 3.5 14.5 8.6 12.5 2
Box 2 8.6 4 14 8.7 12 0
Box 4 8.7 6 12 9.2 10 1
Box 5 9.2 6.8 11 9.8 9.2 0
Box 6 9.8 7.4 10 10.4 8.6 2
$model=[ordered]@{textures=@{all='mineboard:item/chess_pawn_light';particle='mineboard:item/chess_pawn_light'};elements=$script:parts.ToArray()}
[IO.File]::WriteAllText((Join-Path $assets 'models/item/wonder.json'),($model | ConvertTo-Json -Depth 20).Replace("`r`n", "`n")+"`n",$utf8)
