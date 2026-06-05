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
- Run the `Configure Play subscriptions` GitHub Action to create or patch the
  subscriptions through the Android Publisher API and activate each `standard`
  base plan.
- As of 2026-06-05, the subscription action reaches
  `play-deploy@sacred-evening-412414.iam.gserviceaccount.com` but the Android
  Publisher subscription PATCH still returns `PERMISSION_DENIED`; the Play
  Console base-plan save for `receiptvault_plus_monthly` also fails with
  Google's generic "Your changes couldn't be saved" message.
- If the action logs a US-only pricing fallback, expand regional availability
  in Play Console or resolve `pricing:convertRegionPrices` access before
  production rollout.
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
- Gemini billing alignment: funded prepay project is
  `gen-lang-client-0451935558`; current `GEMINI_API_KEY` belongs to
  `gen-lang-client-0123839677`, which still requires prepay and returns upstream
  Gemini `429`. Create a key in the funded project or fund the old key project,
  then update `GEMINI_API_KEY` and rerun the Cloudflare Worker workflow.
- Renew the Microsoft Entra Outlook connector secret before 2026-12-02, or
  replace it with a certificate/federated credential. Microsoft client secrets
  cannot be set to never expire.
- Add the Google Play service account JSON as the GitHub secret
  `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`.
- Add Cloudflare deployment secrets: `CLOUDFLARE_API_TOKEN` and
  `CLOUDFLARE_ACCOUNT_ID`.
- Internal testing is active after the GitHub Android workflow upload. The app
  remains Draft/Not reviewed in Play Console.
- Production access on this Play account requires a closed test with at least
  12 opted-in testers for at least 14 days before applying for production.
