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
- Billing: Google Play Billing Library with subscription product IDs for Plus
  and Business, plus Worker-side Google Play token verification when the service
  account secret is configured.
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
  with encrypted server-side connector token storage.
- Google Play Billing subscription cards for Plus monthly/yearly and Business
  monthly/yearly, using live Play pricing when the products are active.
- Worker endpoint for Google Play purchase verification:
  `POST /v1/billing/google-play/purchase`.
- Local receipt vault, search, warranty screen, detail view, and Plus plan screen.

## Live Environment

- Android package: `com.corsairlabs.receiptvault`.
- Firebase project: `receiptvault-corsair`.
- Cloudflare Worker: `https://receiptvault-backup.ibsam588.workers.dev`.
- GitHub Actions deploy workflow: `.github/workflows/cloudflare-worker.yml`.
- Worker deploy branch: `codex/receiptvault-android`.
- Auth: Firebase email/password plus Google SSO. Android release builds read
  `GOOGLE_SIGN_IN_WEB_CLIENT_ID`, falling back to `GOOGLE_OAUTH_CLIENT_ID`, from
  GitHub Actions secrets.

Current connector status, verified on 2026-06-05:

- Gmail: configured. Uses `gmail.readonly` and the receipt-only search query
  `newer_than:90d (receipt OR order OR invoice OR "purchase confirmation" OR warranty)`.
  Public production use still needs Google OAuth restricted-scope verification,
  and Google may require a security assessment.
- Outlook: configured. Microsoft Entra app `ReceiptVault Outlook Connector`
  uses delegated Microsoft Graph `User.Read` and `Mail.Read`. Microsoft does
  not support never-expiring client secrets; the current client secret expires
  on 2026-12-02 and must be renewed or replaced with a certificate/federated
  credential before that date.
- Yahoo: configured. Uses the Worker Yahoo OAuth callback and receipt/order
  import flow.
- Other IMAP: configured for manual per-user setup. Users provide email, IMAP
  host, port, username, and app password in the Android app; the Worker stores
  those settings encrypted in R2.

Gemini 2.5 Flash-Lite is configured through the Worker secret. AI Studio
billing/prepay must be active for live Gemini calls; if Gemini is unavailable,
the app falls back to local OCR parsing.

Gemini project status:

- The funded prepay project is `gen-lang-client-0451935558` (`Gemini Project`,
  Tier 1 Prepay, Firebase Payment billing account).
- A funded-project API key was created on 2026-06-06 and stored as the GitHub
  secret `GEMINI_API_KEY`.
- Cloudflare workflow run `27056198992` deployed the updated Worker secret.
- Live Worker categorization smoke test passed with `gemini-2.5-flash-lite` and
  returned structured receipt fields.

Full automatic mailbox import is not part of the MVP runtime yet. The current
connector work stores OAuth/IMAP credentials, runs scheduled Gmail and Outlook
receipt-query candidate scans, writes encrypted sync reports, and keeps
Yahoo/IMAP marked ready without reading mailbox content. Full body/attachment
import still needs the candidate-only importer before receipt/order records are
created automatically.

Google Play Billing product IDs:

- `receiptvault_plus_monthly`
- `receiptvault_plus_yearly`
- `receiptvault_business_monthly`
- `receiptvault_business_yearly`

These products and their `standard` base plans are active in Play Console as of
2026-06-08. GitHub Action run `27130931877` configured and activated them
through the Android Publisher API after granting the Play deploy service account
app-level Admin permissions for ReceiptVault.

Production subscription verification uses the Cloudflare Worker secret
`GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`, deployed from the matching GitHub secret.

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
The Play Console app exists and internal-track upload is handled by GitHub
Actions when the Google Play service account secret is present. CI builds assign
a unique Play `versionCode` from the GitHub run number so repeated internal-track
uploads are not rejected. Play subscription products are active with monthly and
yearly Plus/Business base plans. The Firebase privacy policy URL is live, App
Content still needs the Advertising ID declaration, and public production access
is blocked until a closed test has at least 12 opted-in testers for at least 14
days. Gmail verification and public rollout approvals still need final Play
Console or provider review before production launch.
