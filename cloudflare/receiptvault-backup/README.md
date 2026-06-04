# ReceiptVault Backup Worker

Cloudflare Worker API for paid ReceiptVault backups in R2.

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

The Worker checks Firebase ID tokens and requires a Plus/pro custom claim when
`REQUIRE_PLUS_CLAIM=true`.
