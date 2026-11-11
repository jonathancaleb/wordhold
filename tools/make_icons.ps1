# Renders the launcher icon PNGs from the same geometry as
# app/src/main/res/drawable/ic_launcher_foreground.xml (108-unit adaptive-icon grid).
#   mipmap-*/ic_launcher.png, ic_launcher_round.png  - fallback for Android 7 (API 24-25)
#   branding/wordhold-icon-512.png                   - Play Store listing icon (full-bleed square)
#   branding/wordhold-feature-graphic.png            - Play Store feature graphic (1024 x 500)
# Run from the project root:  powershell -File tools\make_icons.ps1
Add-Type -AssemblyName System.Drawing

# Colours come from the app's single palette file, never from literals in this script.
$root = Split-Path $PSScriptRoot
[xml]$palette = Get-Content (Join-Path $root 'app\src\main\res\values\colors.xml')
function Get-Brand([string]$name) {
    $node = $palette.resources.color | Where-Object { $_.name -eq $name }
    if (-not $node) { throw "colors.xml has no colour named $name" }
    # colors.xml stores #AARRGGBB; drop the alpha.
    return [System.Drawing.ColorTranslator]::FromHtml('#' + $node.InnerText.Trim().Substring(3))
}

function New-RoundedRect([single]$x, [single]$y, [single]$w, [single]$h, [single]$r) {
    $p = New-Object System.Drawing.Drawing2D.GraphicsPath
    $d = 2 * $r
    $p.AddArc($x, $y, $d, $d, 180, 90)
    $p.AddArc($x + $w - $d, $y, $d, $d, 270, 90)
    $p.AddArc($x + $w - $d, $y + $h - $d, $d, $d, 0, 90)
    $p.AddArc($x, $y + $h - $d, $d, $d, 90, 90)
    $p.CloseFigure()
    return $p
}

# $shape: square (full bleed), rounded, circle. $from/$span: the slice of the 108 grid to show.
function Write-Icon([int]$px, [string]$shape, [single]$from, [single]$span, [string]$path) {
    $bmp = New-Object System.Drawing.Bitmap($px, $px, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = 'AntiAlias'
    $g.PixelOffsetMode = 'HighQuality'
    $g.Clear([System.Drawing.Color]::Transparent)

    $ink = New-Object System.Drawing.SolidBrush (Get-Brand 'wh_icon_accent')
    switch ($shape) {
        'square'  { $g.FillRectangle($ink, 0, 0, $px, $px) }
        'rounded' { $bg = New-RoundedRect 0 0 $px $px ($px * 0.22); $g.FillPath($ink, $bg) }
        'circle'  { $g.FillEllipse($ink, 0, 0, $px, $px) }
    }

    $s = $px / $span
    $g.TranslateTransform(-$from * $s, -$from * $s)
    $g.ScaleTransform($s, $s)

    $tile = New-RoundedRect 30 30 48 48 11
    $g.FillPath((New-Object System.Drawing.SolidBrush (Get-Brand 'wh_icon_tile')), $tile)

    $pen = New-Object System.Drawing.Pen (Get-Brand 'wh_icon_accent'), 5.5
    $pen.StartCap = 'Round'; $pen.EndCap = 'Round'; $pen.LineJoin = 'Round'
    $w = [System.Drawing.PointF[]]@(
        (New-Object System.Drawing.PointF 40.5, 44), (New-Object System.Drawing.PointF 47.25, 64),
        (New-Object System.Drawing.PointF 54, 47), (New-Object System.Drawing.PointF 60.75, 64),
        (New-Object System.Drawing.PointF 67.5, 44))
    $g.DrawLines($pen, $w)

    $g.Dispose()
    New-Item -ItemType Directory -Force (Split-Path $path) | Out-Null
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

$res = Join-Path $root 'app\src\main\res'
$sizes = @{ mdpi = 48; hdpi = 72; xhdpi = 96; xxhdpi = 144; xxxhdpi = 192 }
foreach ($density in $sizes.Keys) {
    # Legacy icons show a slightly wider slice than the adaptive mask so the tile has room.
    Write-Icon $sizes[$density] 'rounded' 21 66 (Join-Path $res "mipmap-$density\ic_launcher.png")
    Write-Icon $sizes[$density] 'circle' 19 70 (Join-Path $res "mipmap-$density\ic_launcher_round.png")
}
Write-Icon 512 'square' 18 72 (Join-Path $root 'branding\wordhold-icon-512.png')
Write-Icon 512 'rounded' 21 66 (Join-Path $root 'branding\wordhold-icon-preview.png')

# Feature graphic: the icon tile and the name, on the accent colour.
$fg = New-Object System.Drawing.Bitmap(1024, 500, [System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
$g = [System.Drawing.Graphics]::FromImage($fg)
$g.SmoothingMode = 'AntiAlias'; $g.TextRenderingHint = 'AntiAliasGridFit'
$accent = Get-Brand 'wh_icon_accent'
$g.Clear($accent)
$state = $g.Save()
$g.TranslateTransform(150, 130); $g.ScaleTransform(5, 5); $g.TranslateTransform(-30, -30)
$g.FillPath([System.Drawing.Brushes]::White, (New-RoundedRect 30 30 48 48 11))
$pen = New-Object System.Drawing.Pen $accent, 5.5
$pen.StartCap = 'Round'; $pen.EndCap = 'Round'; $pen.LineJoin = 'Round'
$g.DrawLines($pen, [System.Drawing.PointF[]]@(
    (New-Object System.Drawing.PointF 40.5, 44), (New-Object System.Drawing.PointF 47.25, 64),
    (New-Object System.Drawing.PointF 54, 47), (New-Object System.Drawing.PointF 60.75, 64),
    (New-Object System.Drawing.PointF 67.5, 44)))
$g.Restore($state)
$font = New-Object System.Drawing.Font('Segoe UI', 84, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
$small = New-Object System.Drawing.Font('Segoe UI', 34, [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)
$g.DrawString('Wordhold', $font, [System.Drawing.Brushes]::White, 440, 170)
$g.DrawString('Simple word and number puzzles', $small, (New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(215, 255, 255, 255))), 446, 280)
$g.Dispose()
$fg.Save((Join-Path $root 'branding\wordhold-feature-graphic.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$fg.Dispose()
"icons written"
