# ReceiptVault Pricing Model

## Free

Price: $0

- Unlimited manual camera, gallery, file, and share-sheet uploads.
- Local receipt and warranty storage.
- Camera scan.
- Image upload.
- Email image import through Android Share.
- 1 connected email connector after OAuth connectors launch.
- 10 automatic receipt/order email imports per month.
- Basic OCR.
- Basic Gemini 2.5 Flash-Lite categorization.
- Basic search.
- Local-only receipt storage unless the user upgrades.

## ReceiptVault Plus

Price: $4.99/month or $47.99/year

- Optional Cloudflare R2 cloud backup.
- Restore backed-up receipts on a new device.
- Up to 3 connected email connectors.
- Automatic receipt/order email imports.
- Unlimited manual camera, gallery, file, and share-sheet uploads.
- Unlimited warranty tracking.
- Return and warranty reminders.
- Advanced search.
- CSV/PDF export.
- Gemini 2.5 Flash-Lite categorization for receipts, orders, return windows, and
  warranty candidates.

## ReceiptVault Business

Price: $12.99/month or $124.99/year

- Up to 10 connected email connectors.
- Automatic receipt/order email imports.
- Unlimited manual camera, gallery, file, and share-sheet uploads.
- Business folders.
- Tax/export reports.
- Team-ready export package.
- Priority support.

Google Play Billing product IDs are active in Play Console. The Android app is
already wired to these IDs:

- `receiptvault_plus_monthly`
- `receiptvault_plus_yearly`
- `receiptvault_business_monthly`
- `receiptvault_business_yearly`

Annual pricing is set to roughly 20% off monthly pricing:

- Plus monthly for 12 months: $59.88. Yearly: $47.99.
- Business monthly for 12 months: $155.88. Yearly: $124.99.

Production billing verification requires the Cloudflare Worker secret
`GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`. The app sends completed subscription purchase
tokens to `/v1/billing/google-play/purchase`; the Worker verifies them with the
Google Play Developer API and stores the active Plus/Business entitlement for R2
backup access.
