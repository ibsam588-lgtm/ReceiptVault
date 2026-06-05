# ReceiptVault Backup Worker

Cloudflare Worker API for paid ReceiptVault backups in R2.

The Worker also provides the server-side AI categorization endpoint so Gemini
keys are never shipped in the Android app.

## Resources

- Worker name: `receiptvault-backup`
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

The Worker checks Firebase ID tokens and requires a Plus/pro custom claim when
`REQUIRE_PLUS_CLAIM=true`.

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
