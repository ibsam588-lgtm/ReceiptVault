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
- Google Play subscription products and `standard` base plans are active as of
  2026-06-08. `Configure Play subscriptions` workflow run `27130931877`
  configured and activated all four products through the Android Publisher API.
- The Play deploy service account
  `play-deploy@sacred-evening-412414.iam.gserviceaccount.com` has app-level
  Admin permissions for ReceiptVault. This resolved the prior Android Publisher
  API `PERMISSION_DENIED` failure and regional price conversion access.
- Confirm the Android app can query the live Google Play Billing products on
  the internal testing track.
- Google Play service account JSON is configured as the GitHub secret
  `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` and deployed to the Cloudflare Worker as
  the Worker secret of the same name for server-side subscription verification.
- If launching automatic email connectors, confirm the prominent mailbox
  consent screen, per-account disconnect/delete controls, and updated Data
  Safety are present in the release.
- For Google SSO, register the upload-key and Play App Signing SHA-1
  fingerprints in the Firebase project `receiptvault-corsair`, set the
  `GOOGLE_SIGN_IN_WEB_CLIENT_ID` GitHub secret to the Web OAuth client ID, and
  re-commit a `google-services.json` whose `oauth_client` array is populated.
  Until then "Continue with Google" fails with `DEVELOPER_ERROR` (status 10).
  See the SSO troubleshooting section in `README.md`.
- If launching Gmail sync, complete OAuth verification for the restricted Gmail
  scope and any required security assessment before production access.
- Gemini billing alignment is complete. Funded prepay project is
  `gen-lang-client-0451935558`; a funded-project key is stored as
  `GEMINI_API_KEY`, Cloudflare workflow run `27056198992` deployed it, and the
  live `/v1/ai/categorize` smoke test returned HTTP 200.
- Renew the Microsoft Entra Outlook connector secret before 2026-12-02, or
  replace it with a certificate/federated credential. Microsoft client secrets
  cannot be set to never expire.
- GitHub secret `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` is configured.
- Cloudflare deployment secrets `CLOUDFLARE_API_TOKEN` and
  `CLOUDFLARE_ACCOUNT_ID` are configured.
- Internal testing is active after the GitHub Android workflow upload. The app
  remains Draft/Not reviewed in Play Console.
- Production access on this Play account requires a closed test with at least
  12 opted-in testers for at least 14 days before applying for production.
- As of 2026-06-06, the app dashboard shows 0 closed-test testers opted in.
  Developer-account Policy status has no issues; ReceiptVault app Policy status
  will show app-specific compliance information after review.
