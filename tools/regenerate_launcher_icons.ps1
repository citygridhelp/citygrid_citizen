# Regenerate adaptive launcher mipmaps from city_grid_icon_short.png.
#
# Android adaptive icons (108dp canvas) apply different masks per launcher (circle,
# squircle, rounded square, etc.). Keep artwork inside the central safe circle
# (66dp diameter) so "City Grid" is never clipped. Foreground layers are filled
# with the launcher background (#FCFCFC) so scaling does not show a white halo.
#
# safeZoneRatio: 0.66 = less inset than 0.58; 1.0 = edge-to-edge (may clip on circle icons).
param(
    [double]$SafeZoneRatio = 0.66,
    [string]$BackgroundHex = '#FCFCFC'
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

function Convert-HexToColor {
    param([string]$Hex)
    $h = $Hex.TrimStart('#')
    if ($h.Length -ne 6) { throw "Expected #RRGGBB, got $Hex" }
    return [System.Drawing.Color]::FromArgb(
        255,
        [Convert]::ToInt32($h.Substring(0, 2), 16),
        [Convert]::ToInt32($h.Substring(2, 2), 16),
        [Convert]::ToInt32($h.Substring(4, 2), 16)
    )
}

$resRoot = Join-Path $PSScriptRoot '..\app\src\main\res'
$source = Join-Path $resRoot 'drawable-nodpi\city_grid_icon_short.png'
if (-not (Test-Path $source)) { throw "Missing source: $source" }

$bgColor = Convert-HexToColor $BackgroundHex

function Save-AdaptiveIcon {
    param(
        [string]$InputPath,
        [string]$OutputPath,
        [int]$Size,
        [double]$SafeZone,
        [bool]$FillBackground = $true
    )
    $src = [System.Drawing.Image]::FromFile($InputPath)
    try {
        $bmp = New-Object System.Drawing.Bitmap $Size, $Size
        $g = [System.Drawing.Graphics]::FromImage($bmp)
        try {
            $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
            $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
            $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
            $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality

            if ($FillBackground) {
                $g.Clear($bgColor)
            } else {
                $g.Clear([System.Drawing.Color]::Transparent)
            }

            $fitScale = [Math]::Min($Size / $src.Width, $Size / $src.Height) * $SafeZone
            $w = [int][Math]::Round($src.Width * $fitScale)
            $h = [int][Math]::Round($src.Height * $fitScale)
            $x = [int][Math]::Round(($Size - $w) / 2.0)
            $y = [int][Math]::Round(($Size - $h) / 2.0)
            $g.DrawImage($src, $x, $y, $w, $h)
        } finally { $g.Dispose() }
        $dir = Split-Path $OutputPath -Parent
        New-Item -ItemType Directory -Force -Path $dir | Out-Null
        $bmp.Save($OutputPath, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $bmp.Dispose()
        $src.Dispose()
    }
}

Get-ChildItem -Path $resRoot -Recurse -Filter 'new_app_icon_launcher*' |
    Where-Object { $_.Extension -in '.webp', '.png' } |
    Remove-Item -Force

$foregroundSizes = @{
    'mipmap-mdpi'    = 108
    'mipmap-hdpi'    = 162
    'mipmap-xhdpi'   = 216
    'mipmap-xxhdpi'  = 324
    'mipmap-xxxhdpi' = 432
}
$legacySizes = @{
    'mipmap-mdpi'    = 48
    'mipmap-hdpi'    = 72
    'mipmap-xhdpi'   = 96
    'mipmap-xxhdpi'  = 144
    'mipmap-xxxhdpi' = 192
}

foreach ($entry in $foregroundSizes.GetEnumerator()) {
    $folder = $entry.Key
    $size = $entry.Value
    $base = Join-Path $resRoot $folder
    Save-AdaptiveIcon -InputPath $source -OutputPath (Join-Path $base 'new_app_icon_launcher_foreground.png') -Size $size -SafeZone $SafeZoneRatio -FillBackground $true
    Save-AdaptiveIcon -InputPath $source -OutputPath (Join-Path $base 'new_app_icon_launcher_monochrome.png') -Size $size -SafeZone $SafeZoneRatio -FillBackground $false
}

foreach ($entry in $legacySizes.GetEnumerator()) {
    $folder = $entry.Key
    $size = $entry.Value
    $base = Join-Path $resRoot $folder
    Save-AdaptiveIcon -InputPath $source -OutputPath (Join-Path $base 'new_app_icon_launcher.png') -Size $size -SafeZone $SafeZoneRatio -FillBackground $true
    Save-AdaptiveIcon -InputPath $source -OutputPath (Join-Path $base 'new_app_icon_launcher_round.png') -Size $size -SafeZone $SafeZoneRatio -FillBackground $true
}

$playstore = Join-Path $PSScriptRoot '..\app\src\main\new_app_icon_launcher-playstore.png'
Save-AdaptiveIcon -InputPath $source -OutputPath $playstore -Size 512 -SafeZone $SafeZoneRatio -FillBackground $true

$circleSafe = [Math]::Round(66.0 / 108.0, 3)
Write-Output "Launcher icons regenerated (safe zone = $SafeZoneRatio, Android circle safe = $circleSafe, bg = $BackgroundHex)"
