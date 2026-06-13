# ReceiptVault Email Connector Requirements

ReceiptVault can support Gmail, Outlook, Yahoo, and other mailbox providers, but
automatic imports must be implemented as receipt-only processing.

## Product Rules

- Users must opt in separately for each connected mailbox.
- Users can connect multiple accounts across supported providers.
- Free users get 1 connected account and 10 automatic receipt/order imports per
  month.
- Plus users get up to 3 connected email connectors and paid automatic
  receipt/order imports.
- Business users get up to 10 connected email connectors and expanded
  automatic receipt/order imports.
- Every connected account must show provider, email address, last sync time,
  monthly import count, sync status, disconnect, and delete imported data.

## Receipt-Only Import Scope

The connector must only store messages that are likely receipts, orders,
invoices, purchase confirmations, returns, or warranty records.

Recommended flow:

1. Search provider mail using receipt/order keywords, sender allowlists, and
   recent-date filters.
2. Read candidate headers and snippets first.
3. Call `POST /v1/connectors/candidate` to decide whether the message may be
   inspected.
4. Read body text and attachments only for likely receipt/order candidates.
5. Run OCR and Gemini 2.5 Flash-Lite categorization only on candidate
   receipt/order text.
6. Store only imported receipt/order records, receipt images or PDFs, source
   provider, message id, merchant, purchase date, total, category, return date,
   warranty candidate, and confidence.
7. Discard non-receipt message headers, snippets, bodies, attachments, and AI
   outputs immediately.

## Background Sync Status

Credential setup is live for Gmail, Outlook, Yahoo, and manual IMAP. The Worker
now has:

- Reads encrypted provider tokens/settings from R2.
- Searches only the provider's receipt/order query.
- Records last sync time, imported count, and disconnect/delete state.
- Runs a scheduled scan every 30 minutes for indexed connector users.
- Stores sync reports without storing unrelated mailbox content.

The production importer still needs:

- Candidate-only body/attachment reading after header/snippet checks pass.
- OCR and Gemini categorization for eligible receipt/order attachments.
- Storage of imported receipt/order records and attachment images/PDFs only.
- Free, Plus, and Business monthly import limit enforcement in the Worker.
- Provider-specific IMAP polling for Yahoo/manual IMAP.

## Provider Scope Notes

- Gmail: automatic background import likely requires `gmail.readonly`, which is
  a restricted scope. Use the narrowest viable scope, complete OAuth
  verification, and complete any required security assessment before production.
- Outlook: use delegated Microsoft Graph permissions for the signed-in user's
  mailbox. Start with header/search access where possible and request broader
  mail read access only when body text or attachments are needed for confirmed
  candidates.
- Yahoo and generic IMAP: use OAuth where the provider supports it. If IMAP
  technically exposes the mailbox, ReceiptVault must still apply the
  receipt-only search, discard, disclosure, and deletion rules above. Manual
  IMAP setup should prefer provider app passwords over account passwords.

## OAuth App Redirect URIs

- Gmail: `https://receiptvault-backup.ibsam588.workers.dev/v1/connectors/oauth/callback/gmail`
- Outlook: `https://receiptvault-backup.ibsam588.workers.dev/v1/connectors/oauth/callback/outlook`
- Yahoo: `https://receiptvault-backup.ibsam588.workers.dev/v1/connectors/oauth/callback/yahoo`
- Other IMAP: manual setup posts to `https://receiptvault-backup.ibsam588.workers.dev/v1/connectors/imap/manual`

If a provider shows "invalid request" during sign-in, confirm its developer
console has the matching `ibsam588.workers.dev` redirect URI, not the previous
`everytools4u.workers.dev` callback.

Repository and Worker secrets:

- `CONNECTOR_TOKEN_ENCRYPTION_KEY`
- `GOOGLE_OAUTH_CLIENT_ID`
- `GOOGLE_OAUTH_CLIENT_SECRET`
- `MICROSOFT_OAUTH_CLIENT_ID`
- `MICROSOFT_OAUTH_CLIENT_SECRET`
- `YAHOO_OAUTH_CLIENT_ID`
- `YAHOO_OAUTH_CLIENT_SECRET`

These provider apps can be created before production, but Gmail restricted-scope
verification and any required security assessment must be completed by the
business owner before public connector launch.

## Play And OAuth Compliance

- Add a prominent in-app disclosure before account connection.
- Explain that ReceiptVault searches for receipt/order messages only.
- Explain what data is stored, where it is stored, and how to disconnect/delete.
- Do not use mailbox data for ads, resale, surveillance, or unrelated
  profiling.
- Do not ship Google OAuth login in an embedded webview. Use provider-supported
  OAuth flows.
- Keep API keys and provider refresh tokens server-side or encrypted according
  to provider requirements.
- Update Play Data Safety and the privacy policy before enabling production
  connector access.

Official references:

- Google Play User Data policy: https://support.google.com/googleplay/android-developer/answer/10144311
- Google API Services User Data Policy: https://developers.google.com/terms/api-services-user-data-policy
- Gmail API scopes: https://developers.google.com/workspace/gmail/api/auth/scopes
- Gmail API services user data policy: https://developers.google.com/gmail/api/policy
- Microsoft Graph permissions: https://learn.microsoft.com/en-us/graph/permissions-reference
