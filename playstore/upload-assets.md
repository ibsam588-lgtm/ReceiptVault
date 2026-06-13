# Play Store Asset Uploads

Upload these files on the default English store listing.

## App Icon

`playstore/graphics/app-icon-512.png`

## Feature Graphic

`playstore/graphics/feature-graphic-1024x500.png`

## Phone Screenshots

All phone screenshots are 1080 x 1920 PNGs.

`playstore/screenshots/phone/01-home-currency.png`
`playstore/screenshots/phone/02-scan-upload-ocr.png`
`playstore/screenshots/phone/03-email-connectors.png`
`playstore/screenshots/phone/04-edit-fields.png`
`playstore/screenshots/phone/05-search-filters.png`
`playstore/screenshots/phone/06-warranties-returns.png`
`playstore/screenshots/phone/07-analytics.png`
`playstore/screenshots/phone/08-plus-backup.png`

Regenerate these assets with:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\generate_playstore_assets.ps1
```
