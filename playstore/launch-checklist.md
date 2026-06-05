# Launch Checklist

- Create app in Corsair Labs Google Play Console.
- Confirm package name: `com.corsairlabs.receiptvault`.
- Add app signing key and generate a release bundle.
- Add store listing copy from `listing-en-US.md`.
- Upload store graphics from `graphics/` and screenshots from `screenshots/phone/`.
- Add privacy policy URL under the Corsair Labs or Firebase Hosting URL.
- Complete Data Safety using `data-safety-notes.md`.
- Complete content rating questionnaire and target audience forms.
- Create Plus subscription products if launching paid plan.
- Add Google Play Billing product IDs to the Android app.
- If launching automatic email connectors, add the prominent consent screen,
  per-account disconnect/delete controls, and update Data Safety before upload.
- If launching Gmail sync, complete OAuth verification for the restricted Gmail
  scope and any required security assessment before production access.
- Add the Gemini server secret to the Worker environment after billing and
  data-safety disclosures are ready: `GEMINI_API_KEY`.
- Add the Google Play service account JSON as the GitHub secret
  `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`.
- Add Cloudflare deployment secrets: `CLOUDFLARE_API_TOKEN` and
  `CLOUDFLARE_ACCOUNT_ID`.
- Run internal testing track before production.
