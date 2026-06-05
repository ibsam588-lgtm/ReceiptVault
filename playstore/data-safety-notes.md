# Data Safety Notes

ReceiptVault's MVP stores receipt images and extracted receipt metadata locally
on the user's device. Optional cloud backup, AI categorization, and automatic
email connectors must be disclosed when they are enabled in a release.

## Data Types

- Photos and videos: receipt images selected or captured by the user.
- Financial info: purchase total, store name, category, purchase date, return
  date, and warranty date extracted from the receipt.
- Personal info: email address and provider account identifier for connected
  mailbox accounts, if the user enables automatic email imports.
- Files and documents: receipt PDFs or order/warranty attachments, if supported
  by the release.
- App activity: locally saved receipt and warranty records.

## Current MVP Handling

- Receipt images are stored in private app storage.
- Receipt metadata is stored locally in app preferences.
- OCR runs through Google ML Kit text recognition.
- Gemini 2.5 Flash-Lite should be called only through the Cloudflare Worker so
  API keys are not shipped in the Android app.
- The Gemini request should send only receipt/order OCR text, email subject,
  sender, and date fields needed to categorize the purchase. Do not send
  unrelated mailbox content.
- Cloudflare R2 backup is optional and should require Firebase Auth plus a paid
  plan claim before receipt images or metadata are stored remotely.
- No data is sold.

## Email Connector Handling

- Connectors must be opt-in per account.
- Each Gmail, Outlook, Yahoo, or IMAP account must have its own consent, status,
  import limit, disconnect action, and delete-data action.
- The app should search for likely receipt/order messages first, then inspect
  only candidate headers, snippets, body text, and attachments needed to import
  the receipt.
- Non-receipt messages must not be stored. Any temporary snippets, headers, or
  body text used to reject non-receipts should be discarded immediately.
- Users should be able to review, edit, and delete each imported receipt.

## Play Store Requirements To Finish

- Publish a Corsair Labs privacy policy URL before production release.
- Confirm whether future cloud sync, email forwarding, analytics, or billing
  changes data collection.
- Complete Play Console Data Safety using the final production behavior, not
  only this local MVP behavior.
- If automatic Gmail sync launches, complete Google OAuth verification for the
  restricted Gmail scope used by the connector, and complete any required
  security assessment before production access.
- Use Google Play Billing for Plus and Business subscriptions sold inside the
  Android app.
- Add prominent in-app disclosure before connecting a mailbox. The disclosure
  must say that ReceiptVault searches for receipts/orders only, does not sell
  personal data, and stores only imported receipt/order records.
