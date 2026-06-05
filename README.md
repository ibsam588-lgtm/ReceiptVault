# ReceiptVault Android

ReceiptVault is an Android-first receipt vault for storing proof of purchase,
tracking simple expenses, and managing warranty or return deadlines.

## App Positioning

ReceiptVault is not a full accounting app. It is a simple personal and small
business tool for answering one question fast: "Where is my receipt?"

Core promise:

> Scan receipts, track expenses, and never lose warranty proof again.

## MVP Screens

- Home: monthly tracked spending, open returns, warranty count, recent receipts.
- Scan: camera receipt capture, OCR confidence, editable merchant/date/total/category.
- Receipt Detail: original receipt image, store, amount, location, return date, warranty.
- Warranties: protected purchases, expiry reminders, covered value.
- Search: find by store, item, date, category, return status, or warranty status.
- Upgrade: Free to Plus subscription conversion.

## MVP Features

- Android receipt photo capture.
- On-device OCR using Google ML Kit Text Recognition.
- Server-side AI categorization with Gemini 2.5 Flash-Lite after OCR text is extracted.
- Automatic labels for merchant, date, amount, category, warranty, and location when available.
- Secure receipt image storage.
- Receipt search and filters.
- Warranty and return-date reminders.
- Simple monthly expense totals.
- Free and paid subscription tiers through Google Play Billing.
- Optional receipt-only email connectors for Gmail, Outlook, Yahoo, and other mailboxes after OAuth and Play disclosures are complete.

## Revenue Model

- Free: $0, local-first storage, 50 stored receipts, 5 warranty items,
  manual camera/image/share imports, 10 automatic email receipt imports per month,
  basic OCR, and basic categories.
- Plus: $4.99/month or $47.99/year, 1,000 receipts, Cloudflare R2 backup,
  up to 3 connected email accounts, 250 automatic email receipt imports per month,
  unlimited warranties, reminders, advanced search, CSV/PDF export, and Gemini
  categorization.
- Business: $12.99/month or $124.99/year, higher receipt and email import limits,
  up to 10 connected email accounts, business folders, tax/export reports, and
  priority support.

## Android Stack

- App: Kotlin, Jetpack Compose, Android camera/photo picker contracts.
- OCR: Google ML Kit Text Recognition through Google Play Services.
- AI categorization: Gemini 2.5 Flash-Lite through the Cloudflare Worker, keeping
  the Gemini API key off the Android client.
- Storage: local private app storage for receipt images.
- Data: local shared preferences JSON for MVP receipt metadata.
- Billing: Google Play Billing will be added after the Play Console app and product IDs exist.
- Cloud backup: Cloudflare Worker plus R2, gated by Firebase Auth and paid plan claims.

## Built Features

- Native Android app under package `com.corsairlabs.receiptvault`.
- Camera receipt capture using Android's camera contract.
- Gallery/file image upload.
- Email attachment import through the Android share sheet for `image/*`.
- On-device OCR text extraction through ML Kit.
- Firebase-authenticated Gemini 2.5 Flash-Lite categorization after OCR, with
  local parser fallback.
- Automatic merchant, date, amount, category, return date, and warranty suggestions.
- Email connector screen for Gmail, Outlook, Yahoo, and IMAP account tracking,
  sync status, disconnect, delete-data controls, and plan limit enforcement.
- OAuth launch flow for Gmail, Outlook, and Yahoo through the Cloudflare Worker,
  with encrypted server-side connector token storage once provider credentials
  are configured.
- Local receipt vault, search, warranty screen, detail view, and Plus plan screen.

## Email Connector Policy

Automatic mailbox connectors must only import messages that are likely receipts,
orders, invoices, purchase confirmations, or warranty documents. The connector
should use provider search and header/snippet checks before reading body text or
attachments, discard non-receipt messages immediately, and store only receipt or
order data the user can see in ReceiptVault.

Users should be able to connect multiple accounts, including Gmail, Outlook,
Yahoo, and other OAuth or IMAP-compatible providers. Each account must have its
own consent, import limit, sync status, disconnect action, and delete-data action.

## Build And Run

Android Studio can open this folder directly.

Command-line build:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug
```

Debug APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install on a connected Android device or emulator:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install app\build\outputs\apk\debug\app-debug.apk
```

## Prototype

Open `index.html` in a browser to view the original clickable design prototype.

## Play Store Prep

Play Store listing copy, data safety notes, and launch checklist are in `playstore/`.
The Play Console app cannot be created from code alone; it must be created inside
the Corsair Labs Google Play Console account, then wired to this project using
the final app signing, package name, subscription product IDs, and privacy URL.
