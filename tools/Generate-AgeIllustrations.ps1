# Original deterministic engravings; shared resource pack for all four builds.
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$assets=Join-Path $PSScriptRoot '../fabric-1.21.1/src/main/resources/assets/mineboard/textures/item'
$preview=Join-Path $PSScriptRoot '../build/review'
[IO.Directory]::CreateDirectory($preview) | Out-Null
function ArtC($s) { [Drawing.ColorTranslator]::FromHtml($s) }
function ArtR($c,$x,$y,$w,$h) { $b=[Drawing.SolidBrush]::new((ArtC $c));$g.FillRectangle($b,[single]$x,[single]$y,[single]$w,[single]$h);$b.Dispose() }
function ArtL($c,$x,$y,$xx,$yy,$w=1) { $p=[Drawing.Pen]::new((ArtC $c),[single]$w);$g.DrawLine($p,[single]$x,[single]$y,[single]$xx,[single]$yy);$p.Dispose() }
function ArtO($c,$x,$y,$w,$h) { $b=[Drawing.SolidBrush]::new((ArtC $c));$g.FillEllipse($b,[single]$x,[single]$y,[single]$w,[single]$h);$b.Dispose() }
function ArtP($c,$a) { $pts=for($i=0;$i -lt $a.Count;$i+=2){[Drawing.PointF]::new($a[$i],$a[$i+1])};$b=[Drawing.SolidBrush]::new((ArtC $c));$g.FillPolygon($b,[Drawing.PointF[]]$pts);$b.Dispose() }
function ArtF($c,$x,$y,$w,$h) { ArtL $c $x $y ($x+$w) $y;ArtL $c $x ($y+$h) ($x+$w) ($y+$h);ArtL $c $x $y $x ($y+$h);ArtL $c ($x+$w) $y ($x+$w) ($y+$h) }
function Wall($x,$y,$w,$h,$c) {
    ArtR $c $x $y $w $h
    for($r=0;$r -lt $h;$r+=9){ArtL '#8D826B' $x ($y+$r) ($x+$w) ($y+$r);for($s=($r%18);$s -lt $w;$s+=18){ArtL '#9A8D72' ($x+$s) ($y+$r) ($x+$s) ([Math]::Min($y+$r+9,$y+$h))}}
}
function Temple($x,$y,$w,$h) {
    Wall ($x+5) ($y+22) ($w-10) ($h-22) '#807461'
    for($i=0;$i -lt 5;$i++) { $xx=$x+10+$i*($w-27)/4; ArtR '#655F54' ($xx+3) ($y+23) 12 ($h-23);ArtR '#E8D8AD' $xx ($y+23) 9 ($h-23);foreach($d in @(2,5,8)){ArtL '#B6A17B' ($xx+$d) ($y+28) ($xx+$d) ($y+$h-2)};ArtR '#F2DDB1' ($xx-3) ($y+23) 18 5;ArtR '#D8C396' ($xx-3) ($y+$h-4) 18 5 }
    ArtR '#C0A87C' ($x-4) ($y+17) ($w+8) 7
    ArtP '#E3CCA0' @(($x-8),($y+17),($x+$w/2),($y-15),($x+$w+8),($y+17))
    ArtP '#9D7E59' @(($x+7),($y+12),($x+$w/2),($y-7),($x+$w-7),($y+12))
    ArtO '#DCC190' ($x+$w/2-6) ($y+1) 12 9
    for($i=0;$i -lt 3;$i++){ArtR '#DECAA3' ($x-5-$i*4) ($y+$h+$i*5) ($w+10+$i*8) 4}
}
function Tree($x,$y,$s) {
    ArtL '#67523B' $x $y ($x-2) ($y-$s) 4
    for($i=0;$i -lt 4;$i++){ArtP '#344F43' @(($x-$s*.25),($y-$s*.3-$i*$s*.18),$x,($y-$s*1.1-$i*$s*.1),($x+$s*.25),($y-$s*.3-$i*$s*.18))}
    ArtL '#82917A' ($x-3) ($y-$s*1.25) ($x-$s*.16) ($y-$s*.6)
}
$families=@(@('brown','#78543D'),@('grey','#617A80'),@('yellow','#AE812D'),@('blue','#316C89'),@('green','#386E56'),@('red','#963E39'),@('purple','#6E507F'))
$sheet=[Drawing.Bitmap]::new(1904,416);$sg=[Drawing.Graphics]::FromImage($sheet);$sg.Clear((ArtC '#172725'));$index=0
foreach($pair in $families) {
    $name=$pair[0];$tone=$pair[1]
    $bitmap=[Drawing.Bitmap]::new(256,384);$g=[Drawing.Graphics]::FromImage($bitmap);$g.SmoothingMode=[Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.Clear((ArtC '#182D2D'));ArtR '#C3A16A' 4 4 248 376;ArtR '#E8DABD' 7 7 242 370
    $rng=[Random]::new(731)
    for($i=0;$i -lt 5000;$i++){ $v=$rng.Next(192,233);ArtR ('#{0:X2}{1:X2}{2:X2}' -f $v,($v-13),($v-34)) ($rng.Next(8,248)) ($rng.Next(8,376)) 1 1 }
    ArtR $tone 13 13 230 46;ArtF '#DDBD7B' 16 16 224 40
    for($x=23;$x -lt 233;$x+=24){ArtL '#DCC48E' $x 26 ($x+16) 26 2;ArtL '#DCC48E' ($x+16) 26 ($x+16) 44 2;ArtL '#DCC48E' ($x+16) 44 ($x+7) 44 2;ArtL '#DCC48E' ($x+7) 44 ($x+7) 34 2}
    $sky=[Drawing.Drawing2D.LinearGradientBrush]::new([Drawing.Rectangle]::new(16,65,224,237),(ArtC '#678D94'),(ArtC '#ECD6A6'),90)
    $g.FillRectangle($sky,16,65,224,237);$sky.Dispose();$g.SetClip([Drawing.Rectangle]::new(16,65,224,237))
    ArtO '#F2DEAA' 167 81 34 34
    ArtP '#93A7A0' @(16,166,57,115,99,163,152,128,202,159,240,121,240,240,16,240)
    ArtP '#718D86' @(16,193,72,153,125,181,179,154,240,179,240,246,16,246)
    ArtR '#B8AC89' 16 223 224 79
    for($i=0;$i -lt 25;$i++){ $x=$rng.Next(17,234);$y=$rng.Next(233,300);ArtL '#9B9276' $x $y ($x+$rng.Next(4,16)) $y }
    switch($name) {
        brown {
            foreach($a in @(@(38,247,83),@(65,231,104),@(198,239,88),@(221,247,120))){Tree $a[0] $a[1] $a[2]}
            Wall 87 208 78 45 '#B3A180';ArtP '#674C38' @(76,210,123,166,176,210)
            for($i=0;$i -lt 8;$i++){ArtL '#A9855A' (86+$i*10) 206 (123+$i*4) (176+$i*4)}
            ArtR '#3E4135' 118 222 17 31
            foreach($a in @(@(56,273),@(72,260),@(91,276),@(112,263),@(139,279))){ArtR '#75513A' $a[0] $a[1] 47 12;ArtO '#D1AC76' ($a[0]+38) $a[1] 14 12;ArtO '#917049' ($a[0]+42) ($a[1]+3) 7 6;ArtL '#BB9767' ($a[0]+3) ($a[1]+3) ($a[0]+35) ($a[1]+3)}
        }
        grey {
            Wall 40 159 175 118 '#BBA987';Wall 50 134 49 34 '#CFBB94';Wall 172 119 28 40 '#D5C29B'
            ArtR '#544B42' 65 199 46 78;ArtO '#544B42' 65 177 46 46;ArtO '#C67E42' 71 209 34 59;ArtO '#E7B66A' 79 227 18 39
            ArtR '#73624E' 127 182 70 9;ArtR '#73624E' 127 232 70 9
            foreach($a in @(@(135,163),@(164,164),@(135,213),@(173,211),@(183,267))){ArtO '#73A6A4' $a[0] $a[1] 17 20;ArtR '#A9D4C5' ($a[0]+5) ($a[1]-7) 6 10;ArtL '#C8E2D0' ($a[0]+4) ($a[1]+3) ($a[0]+4) ($a[1]+12) 2}
            ArtR '#766047' 120 263 77 7;ArtR '#766047' 125 270 5 25;ArtR '#766047' 186 270 5 25;ArtP '#D9B77A' @(135,249,157,247,169,261,131,261)
        }
        yellow {
            ArtR '#56878B' 16 211 224 91
            for($i=0;$i -lt 25;$i++){$x=$rng.Next(17,220);$y=$rng.Next(216,301);ArtL '#9FB7A1' $x $y ($x+18) $y}
            Wall 30 178 73 44 '#D8C59E';Temple 37 152 60 42;Wall 181 117 27 116 '#DCC99F';ArtR '#665E4D' 176 110 37 9;ArtR '#DEC89B' 180 93 29 17
            ArtP '#AC7950' @(175,92,194,78,214,92);ArtO '#F8D18A' 186 96 15 10
            ArtP '#614C3B' @(87,256,177,256,161,273,108,273);ArtL '#5F5140' 132 171 132 258 3
            ArtP '#F4E4BF' @(128,178,93,245,128,241);ArtP '#DBB578' @(137,181,166,239,137,239)
            ArtP '#D8C19A' @(16,269,79,276,104,302,16,302)
            foreach($x in @(25,46,68)){ArtO '#A57B4F' $x 262 15 24;ArtL '#D9B47C' ($x+5) 267 ($x+5) 281 2}
        }
        blue { Temple 46 160 164 102;Tree 28 255 65;Tree 225 257 72;ArtR '#5D8B8D' 53 288 154 14;ArtL '#C2CFB3' 68 294 194 294 2 }
        green {
            Wall 33 123 35 175 '#BAAE8A';Wall 190 123 32 175 '#BCAE8D';ArtR '#D8C99E' 29 117 197 12
            for($x=40;$x -lt 220;$x+=12){ArtR '#8C8166' $x 121 4 4}
            ArtR '#776A4E' 66 247 129 12;ArtR '#655E48' 75 259 8 43;ArtR '#655E48' 181 259 8 43
            ArtO '#C8AA66' 96 159 68 68;ArtO '#698981' 100 163 60 60
            $pen=[Drawing.Pen]::new((ArtC '#DFC98A'),2);$g.DrawEllipse($pen,113,162,33,62);$g.DrawEllipse($pen,100,181,60,23);$pen.Dispose()
            ArtL '#DDC17F' 130 155 130 236 3;ArtR '#D1B575' 115 235 31 6
            ArtP '#E5D7B3' @(79,239,104,233,125,242,150,234,176,240,172,249,127,252,83,248);ArtL '#968769' 126 244 127 250
            foreach($x in @(89,96,103,149,156,163)){ArtL '#AFA080' $x 241 ($x+6) 243}
        }
        red {
            Wall 27 172 204 103 '#A89A7B'
            foreach($x in @(30,183)){Wall $x 137 43 142 '#C1AB85';for($d=0;$d -lt 43;$d+=16){ArtR '#D3BD93' ($x+$d) 125 10 16}}
            ArtO '#4C5045' 100 204 55 62;ArtR '#4C5045' 100 235 55 43
            for($x=106;$x -lt 153;$x+=8){ArtL '#957F59' $x 225 $x 276 2}
            foreach($x in @(72,165)){ArtL '#645744' $x 151 $x 228 3;ArtP '#9D4236' @($x,155,($x+25),155,($x+25),211,($x+12),202,$x,211);ArtL '#D6AE68' ($x+12) 164 ($x+12) 191 2}
            ArtL '#DAC799' 171 229 197 292 3;ArtP '#B25E44' @(157,244,185,238,195,263,179,284,161,268);ArtP '#DDBE7E' @(168,248,173,247,184,270,179,275)
        }
        purple {
            Temple 39 155 178 116;ArtR '#715778' 113 182 30 89;ArtL '#D0AC6E' 119 184 119 269;ArtL '#D0AC6E' 137 184 137 269;ArtO '#DEC28A' 120 201 16 16
            foreach($x in @(48,193)){ArtR '#AD9166' $x 263 15 25;ArtO '#E0C690' ($x+2) 243 11 13;ArtP '#D5BE95' @(($x+2),255,($x+13),255,($x+16),275,($x-2),275)}
        }
    }
    for($i=0;$i -lt 40;$i++){$x=$rng.Next(17,239);$y=$rng.Next(286,302);ArtL '#95866A' $x $y ($x+3) ($y-1)}
    $g.ResetClip();ArtF '#8F774F' 15 64 225 239;ArtF '#E6C78C' 18 67 219 233
    ArtR $tone 13 310 230 61;ArtF '#D9B777' 16 313 224 55
    ArtO '#D8B676' 104 313 48 48;ArtO '#F3ECDD' 107 316 42 42
    # Bold, font-independent emblems stay legible when the cards are small.
    switch($name) {
        brown { foreach($y in @(326,335,344)){ArtR $tone 114 $y 27 6;ArtO '#D8B676' 135 $y 7 6;ArtL '#EDDBB0' 116 ($y+2) 132 ($y+2)} }
        grey { foreach($a in @(@(113,339),@(130,339),@(122,326))){ArtP $tone @($a[0],($a[1]+10),($a[0]+3),$a[1],($a[0]+12),$a[1],($a[0]+15),($a[1]+10));ArtL '#D8B676' ($a[0]+4) ($a[1]+2) ($a[0]+10) ($a[1]+2)} }
        yellow { foreach($y in @(340,333,326)){ArtO '#795D32' 115 ($y+3) 26 10;ArtO '#D3AA53' 115 $y 26 10;ArtL '#F0D59A' 122 ($y+3) 133 ($y+3)} }
        blue { ArtP $tone @(111,329,128,319,145,329);foreach($x in @(116,125,134)){ArtR $tone $x 332 5 14};ArtR $tone 112 348 32 3 }
        green { ArtO $tone 116 325 24 24;ArtO '#F3ECDD' 120 329 16 16;ArtL $tone 114 350 142 322 3;ArtL $tone 128 321 128 353 2 }
        red { ArtP $tone @(114,324,142,324,139,342,128,354,117,342);ArtL '#E5C788' 128 329 128 344 3;ArtL '#E5C788' 120 336 136 336 3 }
        purple { ArtP $tone @(113,327,120,333,128,321,136,333,143,327,140,347,116,347);ArtR '#D3AC63' 119 348 18 3;ArtO '#D3AC63' 125 336 6 6 }
    }
    foreach($side in @(0,1)){$x=29+$side*142;ArtL '#D1B378' $x 335 ($x+54) 335;ArtL '#D1B378' ($x+10) 340 ($x+44) 340;ArtP '#E7CB8D' @(($x+24),350,($x+27),347,($x+30),350,($x+27),353)}
    foreach($a in @(@(8,8),@(248,8),@(8,376),@(248,376))){ArtO '#F5DDA3' ($a[0]-2) ($a[1]-2) 4 4}
    $g.Dispose();$bitmap.Save((Join-Path $assets "ages_$name.png"),[Drawing.Imaging.ImageFormat]::Png)
    $sg.DrawImage($bitmap,($index*272+8),16,256,384);$bitmap.Dispose();$index++
}
$sg.Dispose();$sheet.Save((Join-Path $preview 'ages-cards.png'),[Drawing.Imaging.ImageFormat]::Png);$sheet.Dispose()
Write-Host 'Generated seven illustrated city-card families (256x384).'
