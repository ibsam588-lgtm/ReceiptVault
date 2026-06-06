# Launch Checklist

- Create app in Corsair Labs Google Play Console.
- Confirm package name: `com.corsairlabs.receiptvault`.
- Add app signing key and generate a release bundle.
- Add store listing copy from `listing-en-US.md`.
- Upload store graphics from `graphics/` and screenshots from `screenshots/phone/`.
- Add privacy policy URL under the Corsair Labs or Firebase Hosting URL.
- Privacy policy URL is live:
  `https://receiptvault-corsair.web.app/privacy/receiptvault/`.
- Complete Data Safety using `data-safety-notes.md`.
- Complete content rating questionnaire and target audience forms.
- Complete the Advertising ID declaration. The Android manifest and dependency
  scan show no advertising ID permission, ads SDK, or AdMob dependency, so the
  expected declaration is that ReceiptVault does not use Advertising ID.
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
- As of 2026-06-06, the Play payments profile exists, but the Payments profile
  page still prompts "Add a payment method to receive your earnings." Add the
  payout/payment method in Play Console, then retry the base-plan save and rerun
  the `Configure Play subscriptions` workflow.
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
- Gemini billing alignment is complete. Funded prepay project is
  `gen-lang-client-0451935558`; a funded-project key is stored as
  `GEMINI_API_KEY`, Cloudflare workflow run `27056198992` deployed it, and the
  live `/v1/ai/categorize` smoke test returned HTTP 200.
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
- As of 2026-06-06, the app dashboard shows 0 closed-test testers opted in.
  Developer-account Policy status has no issues; ReceiptVault app Policy status
  will show app-specific compliance information after review.
