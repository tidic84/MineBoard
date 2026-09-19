# Geometry and model routing checks shared by all four builds.
$ErrorActionPreference = 'Stop'
$root = Join-Path $PSScriptRoot '..'
$assets = Join-Path $root 'fabric-1.21.1/src/main/resources/assets/mineboard'
$names = @('cell_light','cell_dark','token_light','token_dark','token_king_light','token_king_dark')
foreach ($tone in @('light','dark')) {
    foreach ($piece in @('pawn','knight','bishop','rook','queen','king')) { $names += "chess_${piece}_$tone" }
}
$names += @('ages_brown','ages_grey','ages_yellow','ages_blue','ages_green','ages_red','ages_purple','wonder','coin')
$card = Get-Content (Join-Path $assets 'models/item/card.json') -Raw | ConvertFrom-Json
for ($i=0; $i -lt $names.Count; $i++) {
    $name = $names[$i]
    $route = @($card.overrides | Where-Object { $_.predicate.custom_model_data -eq (41+$i) })
    if ($route.Count -ne 1 -or $route[0].model -ne "mineboard:item/$name") { throw "Missing 1.21.1 route: $name" }
    $item = Get-Content (Join-Path $root "minecraft-26.2/src/main/resources/assets/mineboard/items/$name.json") -Raw | ConvertFrom-Json
    if ($item.model.model -ne "mineboard:item/$name") { throw "Missing 26.2 route: $name" }
    $model = Get-Content (Join-Path $assets "models/item/$name.json") -Raw | ConvertFrom-Json
    foreach ($element in $model.elements) {
        for ($axis=0; $axis -lt 3; $axis++) {
            if ($element.from[$axis] -ge $element.to[$axis]) { throw "Invalid volume: $name" }
        }
    }
    if ($name.StartsWith('token') -or $name.StartsWith('chess') -or $name -eq 'wonder') {
        for ($a=0; $a -lt $model.elements.Count; $a++) {
            for ($b=$a+1; $b -lt $model.elements.Count; $b++) {
                $one=$model.elements[$a]; $two=$model.elements[$b]
                if ($one.rotation -or $two.rotation) { throw "Unexpected rotated volume: $name" }
                $overlap=$true
                for ($axis=0; $axis -lt 3; $axis++) {
                    if ([Math]::Min([double]$one.to[$axis],[double]$two.to[$axis]) - [Math]::Max([double]$one.from[$axis],[double]$two.from[$axis]) -le 0.00001) { $overlap=$false }
                }
                if ($overlap) { throw "Intersecting token volumes: $name, $a/$b" }
            }
        }
    }
}
$silhouettes = @{}
foreach ($piece in @('pawn','knight','bishop','rook','queen','king')) {
    $model = Get-Content (Join-Path $assets "models/item/chess_${piece}_light.json") -Raw | ConvertFrom-Json
    $geometry = ($model.elements | ForEach-Object { "$($_.from -join ',')/$($_.to -join ',')" }) -join ';'
    if ($silhouettes.ContainsKey($geometry)) { throw "Identical chess silhouettes: $piece" }
    $silhouettes[$geometry] = $true
    foreach ($element in $model.elements) {
        if ($element.from[0] -lt 3 -or $element.to[0] -gt 13 -or $element.from[2] -lt 3 -or $element.to[2] -gt 13) {
            throw "Chess piece overflows its picking footprint: $piece"
        }
    }
}
Get-ChildItem (Join-Path $assets 'models') -Recurse -Filter '*.json' | ForEach-Object {
    $model = Get-Content $_.FullName -Raw | ConvertFrom-Json
    foreach ($texture in $model.textures.PSObject.Properties) {
        if ($texture.Value -like 'mineboard:*') {
            $relative = $texture.Value.Substring('mineboard:'.Length)
            if (-not (Test-Path (Join-Path $assets "textures/$relative.png"))) { throw "Missing texture: $($texture.Value)" }
        }
    }
}
Write-Host 'Validated model routes, textures, and non-intersecting token volumes.'
