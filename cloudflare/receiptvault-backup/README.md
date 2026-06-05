# ReceiptVault Backup Worker

Cloudflare Worker API for paid ReceiptVault backups in R2.

The Worker also provides the server-side AI categorization endpoint so Gemini
keys are never shipped in the Android app.

## Resources

- Worker name: `receiptvault-backup`
- Worker URL: `https://receiptvault-backup.everytools4u.workers.dev`
- R2 bucket: `receiptvault-receipts`
- Firebase project: `receiptvault-corsair`

## Deploy

Create the R2 bucket in Cloudflare, then deploy:

```powershell
npm ci
npm run typecheck
npm run deploy
```

GitHub Actions requires these repository secrets:

- `CLOUDFLARE_API_TOKEN`
- `CLOUDFLARE_ACCOUNT_ID`

Runtime secrets and variables:

- `GEMINI_API_KEY`: secret used by `/v1/ai/categorize`.
- `GEMINI_MODEL`: optional model override. Default is `gemini-2.5-flash-lite`.
- `CONNECTOR_TOKEN_ENCRYPTION_KEY`: random secret used to sign OAuth state and
  encrypt connector refresh tokens before storing them in R2.
- `GOOGLE_OAUTH_CLIENT_ID` and `GOOGLE_OAUTH_CLIENT_SECRET`: Gmail OAuth web
  client credentials.
- `MICROSOFT_OAUTH_CLIENT_ID` and `MICROSOFT_OAUTH_CLIENT_SECRET`: Microsoft
  Entra web app credentials for delegated Graph access.
- `YAHOO_OAUTH_CLIENT_ID` and `YAHOO_OAUTH_CLIENT_SECRET`: Yahoo OAuth app
  credentials.

The Worker checks Firebase ID tokens and requires a Plus/pro custom claim when
`REQUIRE_PLUS_CLAIM=true`.

## Current Provider Setup

Provider configuration verified on 2026-06-05:

- Gmail: configured with the Worker callback
  `/v1/connectors/oauth/callback/gmail`. This uses Google's restricted
  `gmail.readonly` scope, so production access requires Google's OAuth
  restricted-scope verification and any required security assessment.
- Outlook: configured with the Worker callback
  `/v1/connectors/oauth/callback/outlook`. The Microsoft Entra app is
  `ReceiptVault Outlook Connector` with delegated Graph `User.Read` and
  `Mail.Read`. The current client secret expires on 2026-12-02.
- Yahoo: configured with the Worker callback
  `/v1/connectors/oauth/callback/yahoo`.
- Other IMAP: configured through `POST /v1/connectors/imap/manual`. Users supply
  email address, IMAP host, port, username, app password, and TLS preference;
  the Worker encrypts the connection settings before saving them in R2.

Do not commit provider client secrets or token encryption keys. Keep them in
GitHub repository secrets and Cloudflare Worker secrets only.

## AI Categorization

`POST /v1/ai/categorize` requires a Firebase bearer token and accepts:

```json
{
  "ocrText": "receipt or order text",
  "emailSubject": "optional email subject",
  "emailFrom": "optional sender",
  "emailDate": "optional message date"
}
```

The endpoint asks Gemini 2.5 Flash-Lite to return structured JSON for merchant,
total, purchase date, category, warranty candidate, return window, and
confidence. Non-receipt text should return `isReceipt: false` with low
confidence.

## Email Connector Endpoints

`GET /v1/connectors/providers` returns Gmail, Outlook, Yahoo, and IMAP setup
metadata, including the recommended receipt-only search query and the external
review or registration requirement.

OAuth app redirect URIs:

- Gmail: `https://receiptvault-backup.everytools4u.workers.dev/v1/connectors/oauth/callback/gmail`
- Outlook: `https://receiptvault-backup.everytools4u.workers.dev/v1/connectors/oauth/callback/outlook`
- Yahoo: `https://receiptvault-backup.everytools4u.workers.dev/v1/connectors/oauth/callback/yahoo`

`POST /v1/connectors/oauth/start` requires a Firebase bearer token and returns a
provider authorization URL when that provider's client id/secret are configured.
`GET /v1/connectors/oauth/callback/:provider` exchanges the provider code and
stores encrypted OAuth token metadata under the authenticated user's R2 prefix.

`POST /v1/connectors/imap/manual` requires a Firebase bearer token and accepts:

```json
{
  "emailAddress": "user@example.com",
  "host": "imap.example.com",
  "port": 993,
  "username": "user@example.com",
  "password": "provider app password",
  "useTls": true
}
```

The endpoint stores encrypted IMAP connection settings and returns only safe
metadata such as email address, host, port, and TLS preference.

`POST /v1/connectors/candidate` requires a Firebase bearer token and accepts:

```json
{
  "provider": "gmail",
  "subject": "Your order confirmation",
  "from": "store@example.com",
  "snippet": "Thanks for your purchase",
  "hasAttachments": true
}
```

The response tells the connector whether it may inspect the body or attachments.
Non-receipt messages must be discarded without storing headers, snippets, body
text, attachments, or AI outputs.
