# Launch Checklist

- Create app in Corsair Labs Google Play Console.
- Confirm package name: `com.corsairlabs.receiptvault`.
- Add app signing key and generate a release bundle.
- Add store listing copy from `listing-en-US.md`.
- Upload store graphics from `graphics/` and screenshots from `screenshots/phone/`.
- Add privacy policy URL under the Corsair Labs or Firebase Hosting URL.
- Complete Data Safety using `data-safety-notes.md`.
- Complete content rating questionnaire and target audience forms.
- Create Google Play subscription products and base plans:
  `receiptvault_plus_monthly`, `receiptvault_plus_yearly`,
  `receiptvault_business_monthly`, `receiptvault_business_yearly`.
- Confirm the Android app can query the live Google Play Billing products on
  the internal testing track.
- Add the Google Play service account JSON as the Cloudflare Worker secret
  `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` so server-side subscription verification
  can unlock R2 backup.
- If launching automatic email connectors, confirm the prominent mailbox
  consent screen, per-account disconnect/delete controls, and updated Data
  Safety are present in the release.
- If launching Gmail sync, complete OAuth verification for the restricted Gmail
  scope and any required security assessment before production access.
- Set up Gemini AI Studio billing/prepay for
  `https://aistudio.google.com/billing?project=gen-lang-client-0123839677&billing=012A8C-6B3188-636287`.
- Confirm the Gemini server secret is still present in the Worker environment:
  `GEMINI_API_KEY`.
- Renew the Microsoft Entra Outlook connector secret before 2026-12-02, or
  replace it with a certificate/federated credential. Microsoft client secrets
  cannot be set to never expire.
- Add the Google Play service account JSON as the GitHub secret
  `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`.
- Add Cloudflare deployment secrets: `CLOUDFLARE_API_TOKEN` and
  `CLOUDFLARE_ACCOUNT_ID`.
- Run internal testing track before production.
