# Data Safety Notes

ReceiptVault's MVP stores receipt images and extracted receipt metadata locally
on the user's device.

## Data Types

- Photos and videos: receipt images selected or captured by the user.
- Financial info: purchase total, store name, category, purchase date, return
  date, and warranty date extracted from the receipt.
- App activity: locally saved receipt and warranty records.

## Current MVP Handling

- Receipt images are stored in private app storage.
- Receipt metadata is stored locally in app preferences.
- OCR runs through Google ML Kit text recognition.
- No Firebase, Cloudflare, Stripe, or external app backend is connected yet.
- No data is sold.

## Play Store Requirements To Finish

- Publish a Corsair Labs privacy policy URL before production release.
- Confirm whether future cloud sync, email forwarding, analytics, or billing
  changes data collection.
- Complete Play Console Data Safety using the final production behavior, not
  only this local MVP behavior.
