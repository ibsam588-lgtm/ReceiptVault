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
- Automatic labels for merchant, date, amount, category, and location when available.
- Secure receipt image storage.
- Receipt search and filters.
- Warranty and return-date reminders.
- Simple monthly expense totals.
- Free and paid subscription tiers through Google Play Billing.

## Revenue Model

- Free: 50 stored receipts, 5 warranty items, basic OCR, basic search.
- Plus: $4.99/month or $39.99/year, 1,000 receipts, unlimited warranties,
  reminders, advanced search, CSV/PDF export.
- Pro can be added later for freelancers and small businesses.

## Android Stack

- App: Kotlin, Jetpack Compose, Android camera/photo picker contracts.
- OCR: Google ML Kit Text Recognition through Google Play Services.
- Storage: local private app storage for receipt images.
- Data: local shared preferences JSON for MVP receipt metadata.
- Billing: Google Play Billing will be added after the Play Console app and product IDs exist.

## Built Features

- Native Android app under package `com.corsairlabs.receiptvault`.
- Camera receipt capture using Android's camera contract.
- Gallery/file image upload.
- Email attachment import through the Android share sheet for `image/*`.
- On-device OCR text extraction through ML Kit.
- Automatic merchant, date, amount, category, return date, and warranty suggestions.
- Local receipt vault, search, warranty screen, detail view, and Plus plan screen.

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
