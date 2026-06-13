Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$graphicsDir = Join-Path $root "playstore\graphics"
$phoneDir = Join-Path $root "playstore\screenshots\phone"
New-Item -ItemType Directory -Force -Path $graphicsDir | Out-Null
New-Item -ItemType Directory -Force -Path $phoneDir | Out-Null
Get-ChildItem $phoneDir -Filter *.png | Remove-Item -Force

$Ink = [System.Drawing.Color]::FromArgb(19, 32, 43)
$Muted = [System.Drawing.Color]::FromArgb(105, 118, 131)
$Soft = [System.Drawing.Color]::FromArgb(244, 247, 248)
$Teal = [System.Drawing.Color]::FromArgb(23, 169, 154)
$TealDark = [System.Drawing.Color]::FromArgb(15, 119, 109)
$Amber = [System.Drawing.Color]::FromArgb(244, 166, 42)
$Coral = [System.Drawing.Color]::FromArgb(239, 105, 89)
$Blue = [System.Drawing.Color]::FromArgb(67, 103, 220)
$White = [System.Drawing.Color]::White
$Dark = [System.Drawing.Color]::FromArgb(23, 57, 67)

function Font($size, $style = [System.Drawing.FontStyle]::Regular) {
    New-Object System.Drawing.Font("Segoe UI", $size, $style, [System.Drawing.GraphicsUnit]::Pixel)
}

function Brush($color) {
    New-Object System.Drawing.SolidBrush($color)
}

function Pen($color, $width = 1) {
    New-Object System.Drawing.Pen($color, $width)
}

function Rect($x, $y, $w, $h) {
    New-Object System.Drawing.RectangleF($x, $y, $w, $h)
}

function Fill-RoundRect($g, $brush, $x, $y, $w, $h, $r) {
    if ($r -le 0) {
        $g.FillRectangle($brush, $x, $y, $w, $h)
        return
    }
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $d = $r * 2
    $path.AddArc($x, $y, $d, $d, 180, 90)
    $path.AddArc($x + $w - $d, $y, $d, $d, 270, 90)
    $path.AddArc($x + $w - $d, $y + $h - $d, $d, $d, 0, 90)
    $path.AddArc($x, $y + $h - $d, $d, $d, 90, 90)
    $path.CloseFigure()
    $g.FillPath($brush, $path)
    $path.Dispose()
}

function Draw-RoundRect($g, $pen, $x, $y, $w, $h, $r) {
    if ($r -le 0) {
        $g.DrawRectangle($pen, $x, $y, $w, $h)
        return
    }
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $d = $r * 2
    $path.AddArc($x, $y, $d, $d, 180, 90)
    $path.AddArc($x + $w - $d, $y, $d, $d, 270, 90)
    $path.AddArc($x + $w - $d, $y + $h - $d, $d, $d, 0, 90)
    $path.AddArc($x, $y + $h - $d, $d, $d, 90, 90)
    $path.CloseFigure()
    $g.DrawPath($pen, $path)
    $path.Dispose()
}

function Draw-Text($g, $text, $font, $brush, $x, $y, $w, $h, $align = "Near") {
    $fmt = New-Object System.Drawing.StringFormat
    $fmt.Alignment = [System.Drawing.StringAlignment]::$align
    $fmt.LineAlignment = [System.Drawing.StringAlignment]::Near
    $fmt.Trimming = [System.Drawing.StringTrimming]::EllipsisWord
    $g.DrawString($text, $font, $brush, (Rect $x $y $w $h), $fmt)
    $fmt.Dispose()
}

function Draw-Icon($g, $kind, $x, $y, $color) {
    $p = Pen $color 8
    $b = Brush $color
    switch ($kind) {
        "camera" {
            Draw-RoundRect $g $p ($x + 4) ($y + 14) 58 44 10
            $g.DrawEllipse($p, $x + 24, $y + 26, 20, 20)
            $g.FillRectangle($b, $x + 18, $y + 6, 30, 12)
        }
        "email" {
            Draw-RoundRect $g $p ($x + 5) ($y + 12) 58 42 8
            $g.DrawLine($p, $x + 8, $y + 16, $x + 34, $y + 38)
            $g.DrawLine($p, $x + 60, $y + 16, $x + 34, $y + 38)
        }
        "shield" {
            $points = @(
                [System.Drawing.PointF]::new($x + 34, $y + 5),
                [System.Drawing.PointF]::new($x + 58, $y + 16),
                [System.Drawing.PointF]::new($x + 55, $y + 46),
                [System.Drawing.PointF]::new($x + 34, $y + 62),
                [System.Drawing.PointF]::new($x + 13, $y + 46),
                [System.Drawing.PointF]::new($x + 10, $y + 16)
            )
            $g.DrawPolygon($p, $points)
        }
        "search" {
            $g.DrawEllipse($p, $x + 9, $y + 9, 34, 34)
            $g.DrawLine($p, $x + 38, $y + 38, $x + 58, $y + 58)
        }
        "chart" {
            $g.FillRectangle($b, $x + 10, $y + 40, 10, 20)
            $g.FillRectangle($b, $x + 28, $y + 24, 10, 36)
            $g.FillRectangle($b, $x + 46, $y + 10, 10, 50)
        }
        default {
            $g.FillEllipse($b, $x + 8, $y + 8, 52, 52)
        }
    }
    $p.Dispose(); $b.Dispose()
}

function Draw-Logo($g, $x, $y, $size) {
    Fill-RoundRect $g (Brush $Teal) $x $y $size $size ($size * 0.18)
    $p = Pen $White ($size * 0.06)
    $g.DrawLine($p, $x + $size * 0.28, $y + $size * 0.30, $x + $size * 0.72, $y + $size * 0.30)
    $g.DrawLine($p, $x + $size * 0.28, $y + $size * 0.48, $x + $size * 0.72, $y + $size * 0.48)
    $g.DrawLine($p, $x + $size * 0.28, $y + $size * 0.66, $x + $size * 0.55, $y + $size * 0.66)
    $p.Dispose()
}

function Save-Png($bitmap, $path) {
    $bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bitmap.Dispose()
}

function New-Canvas($w, $h, $bg = $Soft) {
    $bmp = New-Object System.Drawing.Bitmap($w, $h, [System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::ClearTypeGridFit
    $g.Clear($bg)
    return @($bmp, $g)
}

function Draw-AppShell($g, $title, $email = "receiptvault@example.com") {
    Fill-RoundRect $g (Brush $White) 42 1710 996 120 0
    $icons = @("home", "search", "camera", "email", "shield", "chart", "star")
    for ($i = 0; $i -lt $icons.Count; $i++) {
        $x = 86 + ($i * 143)
        $c = if ($icons[$i] -eq $title) { $TealDark } else { [System.Drawing.Color]::FromArgb(100, 116, 128) }
        Draw-Icon $g $icons[$i] $x 1738 $c
    }
}

function Draw-Card($g, $x, $y, $w, $h, $title, $body, $icon, $accent = $Teal) {
    Fill-RoundRect $g (Brush $White) $x $y $w $h 18
    Fill-RoundRect $g (Brush ([System.Drawing.Color]::FromArgb(235, 248, 246))) ($x + 34) ($y + 34) 88 88 18
    Draw-Icon $g $icon ($x + 44) ($y + 44) $accent
    Draw-Text $g $title (Font 34 Bold) (Brush $Ink) ($x + 34) ($y + 142) ($w - 68) 46
    Draw-Text $g $body (Font 25 Regular) (Brush $Muted) ($x + 34) ($y + 198) ($w - 68) ($h - 218)
}

function Draw-ReceiptRow($g, $x, $y, $merchant, $meta, $amount, $color = $Coral) {
    Fill-RoundRect $g (Brush $White) $x $y 966 116 16
    Fill-RoundRect $g (Brush $color) ($x + 28) ($y + 24) 68 68 15
    Draw-Text $g $merchant (Font 31 Bold) (Brush $Ink) ($x + 126) ($y + 22) 520 42
    Draw-Text $g $meta (Font 24 Regular) (Brush $Muted) ($x + 126) ($y + 62) 540 34
    Draw-Text $g $amount (Font 31 Bold) (Brush $Ink) ($x + 680) ($y + 36) 240 48 "Far"
}

function Draw-Header($g, $headline, $subhead) {
    Draw-Text $g $headline (Font 45 Bold) (Brush $Ink) 64 60 952 120
    Draw-Text $g $subhead (Font 27 Regular) (Brush $Muted) 66 174 930 78
}

function Create-AppIcon {
    $pair = New-Canvas 512 512 ([System.Drawing.Color]::FromArgb(23, 57, 67))
    $bmp = $pair[0]; $g = $pair[1]
    Fill-RoundRect $g (Brush $Teal) 96 82 320 348 48
    Fill-RoundRect $g (Brush ([System.Drawing.Color]::FromArgb(246, 251, 250))) 139 138 234 36 16
    Fill-RoundRect $g (Brush ([System.Drawing.Color]::FromArgb(246, 251, 250))) 139 213 234 36 16
    Fill-RoundRect $g (Brush ([System.Drawing.Color]::FromArgb(246, 251, 250))) 139 288 162 36 16
    $p = Pen $White 28
    $g.DrawLine($p, 154, 360, 225, 424)
    $g.DrawLine($p, 225, 424, 377, 252)
    $p.Dispose()
    $g.Dispose()
    Save-Png $bmp (Join-Path $graphicsDir "app-icon-512.png")
}

function Create-FeatureGraphic {
    $pair = New-Canvas 1024 500 ([System.Drawing.Color]::FromArgb(245, 241, 232))
    $bmp = $pair[0]; $g = $pair[1]
    Fill-RoundRect $g (Brush $Dark) 0 0 1024 146 0
    Draw-Text $g "ReceiptVault" (Font 60 Bold) (Brush $White) 84 42 420 76
    Draw-Text $g "Receipts, bills, warranties." (Font 37 Bold) (Brush $Ink) 84 176 640 60
    Draw-Text $g "OCR, email connectors, all currencies, cloud backup." (Font 25 Regular) (Brush ([System.Drawing.Color]::FromArgb(52, 84, 89))) 86 246 620 44
    $chips = @("OCR scan", "Email import", "Currencies")
    for ($i = 0; $i -lt $chips.Count; $i++) {
        $x = 86 + ($i * 200)
        Fill-RoundRect $g (Brush $White) $x 332 176 58 29
        Draw-Text $g $chips[$i] (Font 23 Bold) (Brush $TealDark) ($x + 12) 345 152 32 "Center"
    }
    Fill-RoundRect $g (Brush $Dark) 704 134 214 304 36
    Fill-RoundRect $g (Brush ([System.Drawing.Color]::FromArgb(252, 246, 233))) 728 162 166 248 30
    Draw-Logo $g 762 204 96
    $p = Pen $Teal 18
    $g.DrawLine($p, 752, 338, 796, 382)
    $g.DrawLine($p, 796, 382, 868, 300)
    $p.Dispose()
    $g.FillEllipse((Brush $Amber), 820, -8, 190, 190)
    $g.Dispose()
    Save-Png $bmp (Join-Path $graphicsDir "feature-graphic-1024x500.png")
}

function Screenshot-Home {
    $pair = New-Canvas 1080 1920
    $bmp = $pair[0]; $g = $pair[1]
    Draw-AppShell $g "home" "sams@example.com"
    Fill-RoundRect $g (Brush $Dark) 54 215 972 292 18
    Draw-Text $g "Total tracked" (Font 31 Regular) (Brush ([System.Drawing.Color]::FromArgb(190, 255, 255, 255))) 104 268 420 44
    Draw-Text $g '$344.52' (Font 56 Bold) (Brush $White) 104 324 560 72
    Draw-Text $g "4 receipts saved" (Font 30 Regular) (Brush ([System.Drawing.Color]::FromArgb(190, 255, 255, 255))) 104 402 360 44
    Fill-RoundRect $g (Brush $White) 776 246 138 54 18
    Draw-Text $g "USD" (Font 26 Bold) (Brush $TealDark) 796 256 98 34 "Center"
    Fill-RoundRect $g (Brush $Teal) 786 330 142 142 71
    Draw-Icon $g "camera" 824 366 $White
    Draw-Card $g 54 558 466 260 "Upload image" "Gallery or files" "camera"
    Draw-Card $g 560 558 466 260 "Email import" "Auto or share" "email"
    Draw-Text $g "Recent receipts" (Font 34 Bold) (Brush $Ink) 54 892 420 48
    Draw-Text $g "Search" (Font 28 Bold) (Brush $TealDark) 832 894 150 42 "Far"
    Draw-ReceiptRow $g 54 966 "City Utilities" "Bill - Home - Jun 13, 2026" '$142.38'
    Draw-ReceiptRow $g 54 1106 "Home Internet" "Bill - Home - Jun 11, 2026" '$89.99' $Amber
    Draw-ReceiptRow $g 54 1246 "Northside Grocer" "Receipt - Groceries - Jun 9, 2026" '$48.15' $Blue
    Draw-Header $g "Track bills and receipts in USD" "USD is used for scans, uploads, OCR and email imports."
    $g.Dispose(); Save-Png $bmp (Join-Path $phoneDir "01-home-currency.png")
}

function Screenshot-Scan {
    $pair = New-Canvas 1080 1920
    $bmp = $pair[0]; $g = $pair[1]
    Draw-Header $g "Scan or upload purchase documents" "ReceiptVault reads totals, dates, stores and categories from receipts, bills and orders."
    Fill-RoundRect $g (Brush $White) 74 300 932 1040 24
    Fill-RoundRect $g (Brush ([System.Drawing.Color]::FromArgb(250, 250, 250))) 160 382 760 560 18
    Draw-Text $g "City Utilities" (Font 42 Bold) (Brush $Ink) 245 432 590 56 "Center"
    Draw-Text $g "Monthly Bill" (Font 60 Bold) (Brush $Ink) 272 500 540 82 "Center"
    Draw-Text $g "ACCOUNT: 1029-4481`nBILL DATE: 06-13-2026`nDUE DATE: 06-28-2026`nAMOUNT DUE: `$142.38`nSERVICE: WATER + ELECTRIC" (Font 31 Regular) (Brush $Ink) 220 632 640 270 "Center"
    Fill-RoundRect $g (Brush ([System.Drawing.Color]::FromArgb(235, 248, 246))) 124 990 390 190 18
    Draw-Text $g "Purchased" (Font 25 Bold) (Brush $Muted) 158 1025 300 34
    Draw-Text $g "Jun 13, 2026" (Font 37 Bold) (Brush $Ink) 158 1072 310 50
    Fill-RoundRect $g (Brush ([System.Drawing.Color]::FromArgb(235, 248, 246))) 566 990 390 190 18
    Draw-Text $g "Category" (Font 25 Bold) (Brush $Muted) 600 1025 300 34
    Draw-Text $g "Home" (Font 37 Bold) (Brush $Ink) 600 1072 310 50
    Fill-RoundRect $g (Brush $Teal) 220 1230 640 82 24
    Draw-Text $g "OCR text saved with the receipt" (Font 31 Bold) (Brush $White) 260 1248 560 42 "Center"
    Draw-AppShell $g "camera"
    $g.Dispose(); Save-Png $bmp (Join-Path $phoneDir "02-scan-upload-ocr.png")
}

function Screenshot-Email {
    $pair = New-Canvas 1080 1920
    $bmp = $pair[0]; $g = $pair[1]
    Draw-Header $g "Connect mailboxes safely" "Import receipts, orders, bills and warranty records from Gmail, Outlook, Yahoo or IMAP."
    Fill-RoundRect $g (Brush $Dark) 54 300 972 220 18
    Draw-Text $g "Email connectors" (Font 36 Bold) (Brush $White) 94 342 520 50
    Draw-Text $g "Plus: 2/3 connected email connectors" (Font 27 Regular) (Brush ([System.Drawing.Color]::FromArgb(205, 255, 255, 255))) 94 402 780 40
    Draw-Text $g "Only matching purchase documents are imported." (Font 25 Regular) (Brush ([System.Drawing.Color]::FromArgb(205, 255, 255, 255))) 94 446 800 36
    $providers = @(
        @("Gmail", "gmail.readonly", "Ready", $Teal),
        @("Outlook", "Microsoft Graph Mail.Read", "Ready", $Blue),
        @("Yahoo", "OAuth + IMAP read", "Available", $Amber),
        @("Other IMAP", "Provider mailbox import", "Available", $Muted)
    )
    for ($i = 0; $i -lt $providers.Count; $i++) {
        $y = 570 + ($i * 170)
        Fill-RoundRect $g (Brush $White) 54 $y 972 136 18
        Fill-RoundRect $g (Brush ([System.Drawing.Color]::FromArgb(235, 248, 246))) 86 ($y + 28) 80 80 16
        Draw-Icon $g "email" 93 ($y + 35) $providers[$i][3]
        Draw-Text $g $providers[$i][0] (Font 32 Bold) (Brush $Ink) 196 ($y + 30) 420 42
        Draw-Text $g $providers[$i][1] (Font 24 Regular) (Brush $Muted) 196 ($y + 72) 520 34
        Draw-Text $g $providers[$i][2] (Font 25 Bold) (Brush $TealDark) 760 ($y + 50) 210 34 "Far"
    }
    Draw-AppShell $g "email"
    $g.Dispose(); Save-Png $bmp (Join-Path $phoneDir "03-email-connectors.png")
}

function Screenshot-Edit {
    $pair = New-Canvas 1080 1920
    $bmp = $pair[0]; $g = $pair[1]
    Draw-Header $g "Edit and structure every receipt" "Standard fields make purchase date, category, return date and warranty searchable."
    Fill-RoundRect $g (Brush $White) 54 290 972 950 20
    Draw-Text $g "Edit receipt details" (Font 34 Bold) (Brush $Ink) 96 340 500 50
    $fields = @(
        @("Amount", "299.21", 96, 420, 420, 118),
        @("Currency", "USD", 564, 420, 420, 118),
        @("Purchase date", "2026-06-12", 96, 575, 888, 118),
        @("Category", "Home", 96, 730, 888, 118),
        @("Return by", "Select date", 96, 885, 420, 118),
        @("Warranty", "Blank for none", 564, 885, 420, 118)
    )
    foreach ($f in $fields) {
        Draw-RoundRect $g (Pen ([System.Drawing.Color]::FromArgb(132, 132, 132)) 2) $f[2] $f[3] $f[4] $f[5] 8
        Draw-Text $g $f[0] (Font 24 Regular) (Brush $Muted) ($f[2] + 28) ($f[3] - 20) 220 34
        Draw-Text $g $f[1] (Font 32 Regular) (Brush $Ink) ($f[2] + 32) ($f[3] + 36) ($f[4] - 64) 48
    }
    Fill-RoundRect $g (Brush $Teal) 564 1058 420 88 18
    Draw-Text $g "Save" (Font 31 Bold) (Brush $White) 564 1078 420 44 "Center"
    Draw-RoundRect $g (Pen ([System.Drawing.Color]::FromArgb(190, 190, 190)) 2) 96 1058 420 88 18
    Draw-Text $g "Cancel" (Font 31 Bold) (Brush $Muted) 96 1078 420 44 "Center"
    Draw-AppShell $g "home"
    $g.Dispose(); Save-Png $bmp (Join-Path $phoneDir "04-edit-fields.png")
}

function Screenshot-Search {
    $pair = New-Canvas 1080 1920
    $bmp = $pair[0]; $g = $pair[1]
    Draw-Header $g "Find proof of purchase fast" "Search by store, item, date, category, return status or warranty status."
    Draw-RoundRect $g (Pen $Muted 2) 54 310 972 110 8
    Draw-Icon $g "search" 82 334 $Muted
    Draw-Text $g "Search store, item, date, category" (Font 32 Regular) (Brush $Muted) 178 342 760 46
    $chips = @("All", "Warranty", "Groceries", "Home", "Any date", "Today", "7 days", "Custom")
    for ($i = 0; $i -lt $chips.Count; $i++) {
        $row = [Math]::Floor($i / 4); $col = $i % 4
        $x = 54 + ($col * 242); $y = 470 + ($row * 92)
        $fill = if ($chips[$i] -eq "Custom") { [System.Drawing.Color]::FromArgb(234, 218, 249) } else { $White }
        Fill-RoundRect $g (Brush $fill) $x $y 206 62 16
        Draw-Text $g $chips[$i] (Font 25 Bold) (Brush $Ink) $x ($y + 14) 206 34 "Center"
    }
    Draw-ReceiptRow $g 54 690 "City Utilities" "Home - Jun 13, 2026" '$142.38'
    Draw-ReceiptRow $g 54 830 "Home Internet" "Home - Jun 11, 2026" '$89.99' $Amber
    Draw-ReceiptRow $g 54 970 "Northside Grocer" "Groceries - Jun 9, 2026" '$48.15' $Blue
    Fill-RoundRect $g (Brush $White) 54 1540 972 120 18
    Draw-Text $g "Select all" (Font 30 Bold) (Brush $TealDark) 92 1578 220 40
    Fill-RoundRect $g (Brush $Coral) 612 1560 360 76 38
    Draw-Text $g "Delete selected" (Font 29 Bold) (Brush $White) 612 1578 360 40 "Center"
    Draw-AppShell $g "search"
    $g.Dispose(); Save-Png $bmp (Join-Path $phoneDir "05-search-filters.png")
}

function Screenshot-Warranty {
    $pair = New-Canvas 1080 1920
    $bmp = $pair[0]; $g = $pair[1]
    Draw-Header $g "Track returns and warranties" "Keep dates attached to the receipt so proof is ready when something breaks or needs returning."
    Fill-RoundRect $g (Brush ([System.Drawing.Color]::FromArgb(74, 95, 51))) 54 308 972 214 18
    Draw-Text $g "Coverage value" (Font 31 Regular) (Brush ([System.Drawing.Color]::FromArgb(205, 255, 255, 255))) 104 360 420 44
    Draw-Text $g '$1,245.62' (Font 58 Bold) (Brush $White) 104 410 460 72
    Draw-Icon $g "shield" 850 374 ([System.Drawing.Color]::FromArgb(216, 246, 175))
    Draw-ReceiptRow $g 54 600 "Laptop Outlet" "Return: Jul 12, 2026 | Warranty: Jun 12, 2027" '$699.00' $Blue
    Draw-ReceiptRow $g 54 740 "Appliance Depot" "Return: Not set | Warranty: Nov 6, 2027" '$248.25' $Amber
    Draw-ReceiptRow $g 54 880 "Best Buy" "Return: Jun 30, 2026 | Warranty: Jun 12, 2028" '$298.37'
    Draw-AppShell $g "shield"
    $g.Dispose(); Save-Png $bmp (Join-Path $phoneDir "06-warranties-returns.png")
}

function Screenshot-Analytics {
    $pair = New-Canvas 1080 1920
    $bmp = $pair[0]; $g = $pair[1]
    Draw-Header $g "Analytics by selected currency" "Separate totals by currency and understand where spending is going."
    $stats = @(
        @('$344.52', "Total spent", $Teal),
        @('$86.13', "Avg receipt", $Coral),
        @("4", "Receipts", $Amber),
        @("4", "Categorized", $Blue),
        @("1", "Returns", $Teal),
        @("2", "Warranties", $Amber)
    )
    for ($i = 0; $i -lt $stats.Count; $i++) {
        $row = [Math]::Floor($i / 3); $col = $i % 3
        $x = 54 + ($col * 324); $y = 300 + ($row * 168)
        Fill-RoundRect $g (Brush $White) $x $y 292 136 16
        $g.FillEllipse((Brush $stats[$i][2]), $x + 30, $y + 28, 18, 18)
        Draw-Text $g $stats[$i][0] (Font 31 Bold) (Brush $Ink) ($x + 30) ($y + 60) 230 40
        Draw-Text $g $stats[$i][1] (Font 23 Regular) (Brush $Muted) ($x + 30) ($y + 98) 230 30
    }
    Fill-RoundRect $g (Brush $White) 54 672 972 405 18
    Draw-Text $g "Monthly spending" (Font 34 Bold) (Brush $Ink) 94 724 500 46
    $bars = @(24, 34, 48, 44, 72, 250)
    for ($i = 0; $i -lt $bars.Count; $i++) {
        $x = 134 + ($i * 136); $h = $bars[$i]
        Fill-RoundRect $g (Brush $(if ($i -eq 5) { $Teal } else { [System.Drawing.Color]::FromArgb(130, 23, 169, 154) })) $x (996 - $h) 90 $h 10
    }
    Fill-RoundRect $g (Brush $White) 54 1128 972 430 18
    Draw-Text $g "By category" (Font 34 Bold) (Brush $Ink) 94 1180 500 46
    $g.FillPie((Brush $Teal), 136, 1260, 250, 250, -90, 210)
    $g.FillPie((Brush $Coral), 136, 1260, 250, 250, 120, 90)
    $g.FillPie((Brush $Amber), 136, 1260, 250, 250, 210, 60)
    $g.FillEllipse((Brush $White), 205, 1329, 112, 112)
    Draw-Text $g 'Home        $232.37' (Font 28 Bold) (Brush $Ink) 460 1290 470 40
    Draw-Text $g 'Groceries   $48.15' (Font 28 Bold) (Brush $Ink) 460 1355 470 40
    Draw-Text $g 'Services    $64.00' (Font 28 Bold) (Brush $Ink) 460 1420 470 40
    Draw-AppShell $g "chart"
    $g.Dispose(); Save-Png $bmp (Join-Path $phoneDir "07-analytics.png")
}

function Screenshot-Plus {
    $pair = New-Canvas 1080 1920
    $bmp = $pair[0]; $g = $pair[1]
    Draw-Header $g "Upgrade for backup and no ads" "Plus adds cloud backup, no ads, email connectors, reminders and Gemini categorization."
    Fill-RoundRect $g (Brush ([System.Drawing.Color]::FromArgb(38, 55, 78))) 54 300 972 280 18
    Draw-Text $g "ReceiptVault Plus" (Font 31 Regular) (Brush ([System.Drawing.Color]::FromArgb(205, 255, 255, 255))) 96 352 500 44
    Draw-Text $g '$4.99' (Font 68 Bold) (Brush $White) 96 398 360 90
    Draw-Text $g "per month" (Font 28 Regular) (Brush ([System.Drawing.Color]::FromArgb(205, 255, 255, 255))) 98 488 300 42
    Draw-Text $g '$47.99 yearly' (Font 28 Regular) (Brush ([System.Drawing.Color]::FromArgb(205, 255, 255, 255))) 660 488 290 42 "Far"
    $features = @(
        @("No banner or video ads", "shield"),
        @("Cloud backup", "shield"),
        @("3 connected email connectors", "email"),
        @("Unlimited manual receipt uploads", "camera"),
        @("Return and warranty reminders", "shield"),
        @("CSV and PDF exports", "chart")
    )
    for ($i = 0; $i -lt $features.Count; $i++) {
        $y = 640 + ($i * 130)
        Fill-RoundRect $g (Brush $White) 54 $y 972 102 16
        Draw-Icon $g $features[$i][1] 86 ($y + 18) $TealDark
        Draw-Text $g $features[$i][0] (Font 31 Bold) (Brush $Ink) 180 ($y + 32) 720 44
    }
    Draw-AppShell $g "star"
    $g.Dispose(); Save-Png $bmp (Join-Path $phoneDir "08-plus-backup.png")
}

Create-AppIcon
Create-FeatureGraphic
Screenshot-Home
Screenshot-Scan
Screenshot-Email
Screenshot-Edit
Screenshot-Search
Screenshot-Warranty
Screenshot-Analytics
Screenshot-Plus

Write-Host "Generated Play Store assets:"
Get-ChildItem $graphicsDir, $phoneDir -Filter *.png | Sort-Object FullName | ForEach-Object { Write-Host $_.FullName }
