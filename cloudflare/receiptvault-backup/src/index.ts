import { createRemoteJWKSet, jwtVerify, type JWTPayload } from "jose";

type Env = {
  RECEIPTS_BUCKET: R2Bucket;
  FIREBASE_PROJECT_ID: string;
  REQUIRE_PLUS_CLAIM: string;
  GEMINI_API_KEY?: string;
  GEMINI_MODEL?: string;
  GOOGLE_PLAY_PACKAGE_NAME?: string;
  GOOGLE_PLAY_SERVICE_ACCOUNT_JSON?: string;
  CONNECTOR_TOKEN_ENCRYPTION_KEY?: string;
  GOOGLE_OAUTH_CLIENT_ID?: string;
  GOOGLE_OAUTH_CLIENT_SECRET?: string;
  MICROSOFT_OAUTH_CLIENT_ID?: string;
  MICROSOFT_OAUTH_CLIENT_SECRET?: string;
  YAHOO_OAUTH_CLIENT_ID?: string;
  YAHOO_OAUTH_CLIENT_SECRET?: string;
};

type CategorizeRequest = {
  ocrText?: string;
  emailSubject?: string;
  emailFrom?: string;
  emailDate?: string;
};

type ConnectorCandidateRequest = {
  provider?: string;
  subject?: string;
  from?: string;
  snippet?: string;
  hasAttachments?: boolean;
};

type OAuthStartRequest = {
  provider?: string;
  returnUrl?: string;
};

type ImapManualSetupRequest = {
  emailAddress?: string;
  host?: string;
  port?: number | string;
  username?: string;
  password?: string;
  useTls?: boolean;
};

type ConnectorSyncRequest = {
  provider?: string;
  maxCandidates?: number | string;
};

type PlayBillingPurchaseRequest = {
  productId?: string;
  purchaseToken?: string;
};

type ConnectorProviderId = "gmail" | "outlook" | "yahoo" | "imap";
type PurchaseDocumentType = "receipt" | "order" | "invoice" | "bill" | "statement" | "warranty" | "return" | "subscription" | "other";

type OAuthProviderConfig = {
  id: ConnectorProviderId;
  label: string;
  authUrl: string;
  tokenUrl: string;
  clientId?: string;
  clientSecret?: string;
  scope: string;
  query: string;
  restrictedScope: boolean;
  reviewRequired: string;
};

type EncryptedJsonPayload = {
  alg: string;
  iv: string;
  ciphertext: string;
};

type StoredConnectorMetadata = {
  provider?: ConnectorProviderId;
  label?: string;
  scope?: string;
  storedAt?: string;
  lastSuccessfulSyncAt?: string;
  emailAddress?: string;
  host?: string;
  port?: number;
  useTls?: boolean;
  encrypted?: EncryptedJsonPayload;
};

type OAuthTokenSet = Record<string, unknown> & {
  access_token?: string;
  refresh_token?: string;
  expires_in?: number;
  expiresAt?: string;
};

type ProviderMessageCandidate = {
  id: string;
  subject?: string;
  from?: string;
  date?: string;
  snippet?: string;
  hasAttachments?: boolean;
  webUrl?: string;
  rfc822MessageId?: string;
};

type ProviderMessageContent = {
  text: string;
  attachments: EmailAttachmentRecord[];
};

type EmailAttachmentRecord = {
  id: string;
  filename: string;
  mimeType: string;
  size: number;
  storageKey?: string;
  stored: boolean;
  skippedReason?: string;
};

type GmailAttachmentRef = {
  id: string;
  filename: string;
  mimeType: string;
  size: number;
  attachmentId?: string;
};

type ConnectorSyncReport = {
  ok: boolean;
  provider: ConnectorProviderId;
  status: string;
  syncedAt: string;
  scanned: number;
  candidates: number;
  imported: number;
  matchedSignals: string[];
  message: string;
  receipts?: Record<string, unknown>[];
  error?: string;
  plan?: EffectivePlan;
  monthlyImportLimit?: number;
  monthlyImportUsed?: number;
  monthlyImportRemaining?: number;
};

type EffectivePlan = "free" | "plus" | "business";

type MonthlyImportUsage = {
  period: string;
  used: number;
  limit: number;
  remaining: number;
};

type OAuthState = {
  provider: ConnectorProviderId;
  userId: string;
  returnUrl?: string;
  iat: number;
  nonce: string;
};

const firebaseKeys = createRemoteJWKSet(
  new URL("https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com")
);

const BILLING_PRODUCT_PLANS: Record<string, "plus" | "business"> = {
  receiptvault_plus_monthly: "plus",
  receiptvault_plus_yearly: "plus",
  receiptvault_business_monthly: "business",
  receiptvault_business_yearly: "business"
};

const EMAIL_BODY_TEXT_LIMIT = 12000;
const GEMINI_EMAIL_TEXT_LIMIT = 6000;
const MAX_ATTACHMENTS_PER_DOCUMENT = 8;
const MAX_ATTACHMENT_BYTES = 6 * 1024 * 1024;
const MAX_ATTACHMENT_TOTAL_BYTES = 16 * 1024 * 1024;
const MONTHLY_IMPORT_LIMITS: Record<EffectivePlan, number> = {
  free: 10,
  plus: 250,
  business: 1000
};

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);

    if (url.pathname === "/health") {
      return json({ ok: true, service: "receiptvault-backup" });
    }

    if (url.pathname === "/v1/ai/categorize") {
      if (request.method !== "POST") return json({ error: "method_not_allowed" }, 405);

      const user = await authenticate(request, env);
      if (!user) return json({ error: "unauthorized" }, 401);

      return categorizeReceipt(request, env);
    }

    if (url.pathname === "/v1/connectors/providers") {
      if (request.method !== "GET") return json({ error: "method_not_allowed" }, 405);
      return json({ ok: true, providers: connectorProviders(request, env) });
    }

    if (url.pathname === "/v1/connectors/candidate") {
      if (request.method !== "POST") return json({ error: "method_not_allowed" }, 405);

      const user = await authenticate(request, env);
      if (!user) return json({ error: "unauthorized" }, 401);

      return connectorCandidate(request);
    }

    if (url.pathname === "/v1/billing/google-play/purchase") {
      if (request.method !== "POST") return json({ error: "method_not_allowed" }, 405);

      const user = await authenticate(request, env);
      if (!user) return json({ error: "unauthorized" }, 401);

      return verifyGooglePlayPurchase(request, env, user);
    }

    if (url.pathname === "/v1/connectors/sync") {
      if (request.method !== "POST") return json({ error: "method_not_allowed" }, 405);

      const user = await authenticate(request, env);
      if (!user) return json({ error: "unauthorized" }, 401);

      return syncConnectorAccounts(request, env, user);
    }

    if (url.pathname === "/v1/connectors/sync/status") {
      if (request.method !== "GET") return json({ error: "method_not_allowed" }, 405);

      const user = await authenticate(request, env);
      if (!user) return json({ error: "unauthorized" }, 401);

      return listConnectorSyncReports(env, user);
    }

    if (url.pathname === "/v1/connectors/oauth/start") {
      if (request.method !== "POST") return json({ error: "method_not_allowed" }, 405);

      const user = await authenticate(request, env);
      if (!user) return json({ error: "unauthorized" }, 401);

      return startConnectorOAuth(request, env, user);
    }

    if (url.pathname === "/v1/connectors/imap/manual") {
      if (request.method !== "POST") return json({ error: "method_not_allowed" }, 405);

      const user = await authenticate(request, env);
      if (!user) return json({ error: "unauthorized" }, 401);

      return storeManualImapConnector(request, env, user);
    }

    if (url.pathname.startsWith("/v1/connectors/oauth/callback/")) {
      if (request.method !== "GET") return html("Method not allowed", 405);
      return finishConnectorOAuth(request, env);
    }

    if (url.pathname === "/v1/connectors/accounts") {
      const user = await authenticate(request, env);
      if (!user) return json({ error: "unauthorized" }, 401);
      if (request.method === "GET") return listConnectorAccounts(env, user);
      return json({ error: "method_not_allowed" }, 405);
    }

    if (url.pathname.startsWith("/v1/connectors/accounts/")) {
      const user = await authenticate(request, env);
      if (!user) return json({ error: "unauthorized" }, 401);
      if (request.method === "DELETE") return deleteConnectorAccount(request, env, user);
      return json({ error: "method_not_allowed" }, 405);
    }

    if (!url.pathname.startsWith("/v1/receipts/")) {
      return json({ error: "not_found" }, 404);
    }

    const user = await authenticate(request, env);
    if (!user) return json({ error: "unauthorized" }, 401);

    if (env.REQUIRE_PLUS_CLAIM === "true" && !hasPlus(user) && !(await hasActiveBillingEntitlement(env, String(user.sub || "")))) {
      return json({ error: "plus_required" }, 402);
    }

    if (url.pathname === "/v1/receipts/attachments") {
      if (request.method !== "GET") return json({ error: "method_not_allowed" }, 405);
      return getReceiptAttachment(request, env, user);
    }

    const objectName = objectNameFor(url.pathname, user.sub);
    if (!objectName) return json({ error: "bad_receipt_path" }, 400);

    if (request.method === "PUT") {
      await env.RECEIPTS_BUCKET.put(objectName, request.body, {
        httpMetadata: { contentType: request.headers.get("content-type") || "application/octet-stream" }
      });
      return json({ ok: true, objectName });
    }

    if (request.method === "GET") {
      const object = await env.RECEIPTS_BUCKET.get(objectName);
      if (!object) return json({ error: "not_found" }, 404);
      return new Response(object.body, {
        headers: { "content-type": object.httpMetadata?.contentType || "application/octet-stream" }
      });
    }

    if (request.method === "DELETE") {
      await env.RECEIPTS_BUCKET.delete(objectName);
      return json({ ok: true });
    }

    return json({ error: "method_not_allowed" }, 405);
  },

  async scheduled(_controller: ScheduledController, env: Env, ctx: ExecutionContext): Promise<void> {
    ctx.waitUntil(syncIndexedConnectorAccounts(env));
  }
};

async function verifyGooglePlayPurchase(request: Request, env: Env, user: JWTPayload): Promise<Response> {
  const body = await readPlayBillingPurchaseBody(request);
  const productId = typeof body.productId === "string" ? body.productId.trim() : "";
  const purchaseToken = typeof body.purchaseToken === "string" ? body.purchaseToken.trim() : "";
  const plan = BILLING_PRODUCT_PLANS[productId];

  if (!productId || !purchaseToken || !plan) return json({ error: "valid_product_and_purchase_token_required" }, 400);
  if (!env.GOOGLE_PLAY_SERVICE_ACCOUNT_JSON) return json({ error: "google_play_service_account_not_configured" }, 503);

  const packageName = env.GOOGLE_PLAY_PACKAGE_NAME || "com.corsairlabs.receiptvault";
  const accessToken = await googleServiceAccountAccessToken(env.GOOGLE_PLAY_SERVICE_ACCOUNT_JSON);
  const verifyUrl = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodeURIComponent(packageName)}/purchases/subscriptionsv2/tokens/${encodeURIComponent(purchaseToken)}`;
  const response = await fetch(verifyUrl, {
    headers: { authorization: `Bearer ${accessToken}` }
  });
  const verification: Record<string, unknown> = await response.json<Record<string, unknown>>().catch(() => ({}));
  if (!response.ok) {
    return json({ error: "google_play_verification_failed", status: response.status, detail: verification }, 502);
  }

  const state = typeof verification.subscriptionState === "string" ? verification.subscriptionState : "";
  const lineItems: Array<Record<string, unknown>> = Array.isArray(verification.lineItems) ? verification.lineItems.filter(isRecord) : [];
  const matchingLineItem = lineItems.find((item: Record<string, unknown>) => item.productId === productId) || lineItems[0] || {};
  const verifiedProductId = typeof matchingLineItem.productId === "string" ? matchingLineItem.productId : productId;
  const expiryTime = typeof matchingLineItem.expiryTime === "string" ? matchingLineItem.expiryTime : null;
  const expiryMillis = expiryTime ? Date.parse(expiryTime) : Number.NaN;
  const activeState = state === "SUBSCRIPTION_STATE_ACTIVE" || state === "SUBSCRIPTION_STATE_IN_GRACE_PERIOD";
  const productMatches = verifiedProductId === productId;
  const notExpired = !expiryTime || (!Number.isNaN(expiryMillis) && expiryMillis > Date.now());
  const active = activeState && productMatches && notExpired;

  const entitlement = {
    active,
    provider: "google_play",
    productId,
    verifiedProductId,
    plan,
    subscriptionState: state,
    expiryTime,
    purchaseTokenHash: await sha256Hex(purchaseToken),
    verifiedAt: new Date().toISOString()
  };
  await env.RECEIPTS_BUCKET.put(billingEntitlementObjectName(String(user.sub || "")), JSON.stringify(entitlement), {
    httpMetadata: { contentType: "application/json" }
  });

  return json({ ok: true, active, plan, productId, subscriptionState: state, expiryTime });
}

async function readPlayBillingPurchaseBody(request: Request): Promise<PlayBillingPurchaseRequest> {
  try {
    return (await request.json()) as PlayBillingPurchaseRequest;
  } catch {
    return {};
  }
}

async function hasActiveBillingEntitlement(env: Env, userId: string): Promise<boolean> {
  const object = await env.RECEIPTS_BUCKET.get(billingEntitlementObjectName(userId));
  if (!object) return false;
  const entitlement: Record<string, unknown> = await object.json<Record<string, unknown>>().catch(() => ({}));
  if (entitlement.active !== true) return false;
  if (entitlement.plan !== "plus" && entitlement.plan !== "business") return false;

  const expiryTime = typeof entitlement.expiryTime === "string" ? entitlement.expiryTime : "";
  if (!expiryTime) return true;
  const expiryMillis = Date.parse(expiryTime);
  return !Number.isNaN(expiryMillis) && expiryMillis > Date.now();
}

function billingEntitlementObjectName(userId: string): string {
  return `users/${userId}/billing/google-play-entitlement.json`;
}

async function googleServiceAccountAccessToken(serviceAccountJson: string): Promise<string> {
  const serviceAccount = JSON.parse(serviceAccountJson) as Record<string, unknown>;
  const clientEmail = typeof serviceAccount.client_email === "string" ? serviceAccount.client_email : "";
  const privateKey = typeof serviceAccount.private_key === "string" ? serviceAccount.private_key : "";
  const tokenUri = typeof serviceAccount.token_uri === "string"
    ? serviceAccount.token_uri
    : "https://oauth2.googleapis.com/token";
  if (!clientEmail || !privateKey) throw new Error("invalid_google_play_service_account_json");

  const now = Math.floor(Date.now() / 1000);
  const assertion = await signServiceAccountJwt(privateKey, {
    iss: clientEmail,
    scope: "https://www.googleapis.com/auth/androidpublisher",
    aud: tokenUri,
    iat: now,
    exp: now + 3600
  });
  const body = new URLSearchParams();
  body.set("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
  body.set("assertion", assertion);

  const response = await fetch(tokenUri, {
    method: "POST",
    headers: { "content-type": "application/x-www-form-urlencoded" },
    body
  });
  const token: Record<string, unknown> = await response.json<Record<string, unknown>>().catch(() => ({}));
  if (!response.ok || typeof token.access_token !== "string") {
    throw new Error(`google_service_account_token_failed:${response.status}`);
  }
  return token.access_token;
}

async function signServiceAccountJwt(privateKeyPem: string, claims: Record<string, unknown>): Promise<string> {
  const header = base64UrlJson({ alg: "RS256", typ: "JWT" });
  const payload = base64UrlJson(claims);
  const signingInput = `${header}.${payload}`;
  const key = await crypto.subtle.importKey(
    "pkcs8",
    pemToArrayBuffer(privateKeyPem),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"]
  );
  const signature = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(signingInput));
  return `${signingInput}.${base64UrlEncode(new Uint8Array(signature))}`;
}

function base64UrlJson(value: unknown): string {
  return base64UrlEncode(new TextEncoder().encode(JSON.stringify(value)));
}

function pemToArrayBuffer(pem: string): ArrayBuffer {
  const base64 = pem
    .replace(/-----BEGIN PRIVATE KEY-----/g, "")
    .replace(/-----END PRIVATE KEY-----/g, "")
    .replace(/\s+/g, "");
  const bytes = base64UrlDecode(base64.replace(/\+/g, "-").replace(/\//g, "_"));
  const buffer = new ArrayBuffer(bytes.byteLength);
  new Uint8Array(buffer).set(bytes);
  return buffer;
}

async function sha256Hex(value: string): Promise<string> {
  const hash = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value));
  return Array.from(new Uint8Array(hash))
    .map((byte) => byte.toString(16).padStart(2, "0"))
    .join("");
}

function connectorProviders(request: Request, env: Env): Array<Record<string, unknown>> {
  const origin = new URL(request.url).origin;
  return connectorProviderConfigs(env).map((provider) => providerPublic(provider, origin));
}

function providerPublic(provider: OAuthProviderConfig, origin: string): Record<string, unknown> {
  return {
    id: provider.id,
    label: provider.label,
    scope: provider.scope,
    restrictedScope: provider.restrictedScope,
    query: provider.query,
    reviewRequired: provider.reviewRequired,
    configured: isProviderConfigured(provider),
    redirectUri: `${origin}/v1/connectors/oauth/callback/${provider.id}`
  };
}

function connectorProviderConfigs(env: Env): OAuthProviderConfig[] {
  return [
    providerConfig("gmail", env),
    providerConfig("outlook", env),
    providerConfig("yahoo", env),
    providerConfig("imap", env)
  ];
}

function providerConfig(provider: ConnectorProviderId, env: Env): OAuthProviderConfig {
  if (provider === "gmail") {
    return {
      id: "gmail",
      label: "Gmail",
      authUrl: "https://accounts.google.com/o/oauth2/v2/auth",
      tokenUrl: "https://oauth2.googleapis.com/token",
      clientId: env.GOOGLE_OAUTH_CLIENT_ID,
      clientSecret: env.GOOGLE_OAUTH_CLIENT_SECRET,
      scope: "https://www.googleapis.com/auth/gmail.readonly",
      restrictedScope: true,
      query: 'newer_than:90d (receipt OR order OR invoice OR "purchase confirmation" OR warranty OR bill OR "utility bill" OR "payment due" OR "statement available" OR "monthly statement") -from:google.com -from:googleplay.com',
      reviewRequired: "Google OAuth restricted-scope verification and possible security assessment"
    };
  }

  if (provider === "outlook") {
    return {
      id: "outlook",
      label: "Outlook",
      authUrl: "https://login.microsoftonline.com/common/oauth2/v2.0/authorize",
      tokenUrl: "https://login.microsoftonline.com/common/oauth2/v2.0/token",
      clientId: env.MICROSOFT_OAUTH_CLIENT_ID,
      clientSecret: env.MICROSOFT_OAUTH_CLIENT_SECRET,
      scope: "offline_access User.Read Mail.Read",
      restrictedScope: false,
      query: "receipt OR order OR invoice OR purchase confirmation OR warranty",
      reviewRequired: "Microsoft Entra app registration and user consent"
    };
  }

  if (provider === "yahoo") {
    return {
      id: "yahoo",
      label: "Yahoo",
      authUrl: "https://api.login.yahoo.com/oauth2/request_auth",
      tokenUrl: "https://api.login.yahoo.com/oauth2/get_token",
      clientId: env.YAHOO_OAUTH_CLIENT_ID,
      clientSecret: env.YAHOO_OAUTH_CLIENT_SECRET,
      scope: "mail-r",
      restrictedScope: false,
      query: "receipt OR order OR invoice OR purchase confirmation OR warranty OR bill OR statement",
      reviewRequired: "Yahoo developer app registration"
    };
  }

  return {
    id: "imap",
    label: "Other IMAP",
    authUrl: "",
    tokenUrl: "",
    clientId: env.CONNECTOR_TOKEN_ENCRYPTION_KEY ? "manual-imap" : undefined,
    scope: "Provider-specific OAuth/IMAP read",
    restrictedScope: false,
    query: "receipt OR order OR invoice OR purchase confirmation OR warranty",
    reviewRequired: "Per-user IMAP host, port, username, and app password"
  };
}

function isProviderConfigured(provider: OAuthProviderConfig): boolean {
  if (provider.id === "imap") return Boolean(provider.clientId);
  return Boolean(provider.clientId && provider.clientSecret);
}

async function startConnectorOAuth(request: Request, env: Env, user: JWTPayload): Promise<Response> {
  if (!env.CONNECTOR_TOKEN_ENCRYPTION_KEY) return json({ error: "connector_encryption_not_configured" }, 503);
  const body = await readOAuthStartBody(request);
  const providerId = normalizeProvider(body.provider);
  if (!providerId) return json({ error: "unsupported_provider" }, 400);

  const provider = providerConfig(providerId, env);
  const origin = new URL(request.url).origin;
  const redirectUri = `${origin}/v1/connectors/oauth/callback/${provider.id}`;

  if (provider.id === "imap") {
    return json({
      error: "imap_manual_setup_required",
      provider: providerPublic(provider, origin),
      manualSetupEndpoint: `${origin}/v1/connectors/imap/manual`
    }, 400);
  }

  if (!isProviderConfigured(provider)) {
    return json({
      error: "provider_not_configured",
      provider: providerPublic(provider, origin),
      missingSecrets: missingProviderSecrets(provider.id, env)
    }, 503);
  }

  const state = await signOAuthState({
    provider: provider.id,
    userId: String(user.sub || ""),
    returnUrl: body.returnUrl,
    iat: Date.now(),
    nonce: crypto.randomUUID()
  }, env.CONNECTOR_TOKEN_ENCRYPTION_KEY);

  const authorizationUrl = buildAuthorizationUrl(provider, redirectUri, state);
  return json({ ok: true, provider: providerPublic(provider, origin), authorizationUrl });
}

async function finishConnectorOAuth(request: Request, env: Env): Promise<Response> {
  if (!env.CONNECTOR_TOKEN_ENCRYPTION_KEY) return html("Connector encryption is not configured.", 503);

  const url = new URL(request.url);
  const pathProvider = normalizeProvider(url.pathname.split("/").pop());
  const code = url.searchParams.get("code");
  const stateParam = url.searchParams.get("state");
  const error = url.searchParams.get("error");

  if (error) return html(`ReceiptVault connector authorization failed: ${escapeHtml(error)}`, 400);
  if (!pathProvider || !code || !stateParam) return html("Missing OAuth callback data.", 400);

  const state = await verifyOAuthState(stateParam, env.CONNECTOR_TOKEN_ENCRYPTION_KEY);
  if (!state || state.provider !== pathProvider || Date.now() - state.iat > 10 * 60 * 1000) {
    return html("OAuth state is invalid or expired.", 400);
  }

  const provider = providerConfig(pathProvider, env);
  if (!isProviderConfigured(provider)) return html(`${provider.label} is not configured.`, 503);

  const redirectUri = `${url.origin}/v1/connectors/oauth/callback/${provider.id}`;
  const tokenResponse = await exchangeOAuthCode(provider, code, redirectUri);
  if (!tokenResponse.ok) {
    return html(`Could not connect ${provider.label}: ${escapeHtml(tokenResponse.error)}`, 502);
  }

  const emailAddress = await fetchConnectorEmailAddress(provider, tokenResponse.tokens);
  await storeConnectorToken(env, state.userId, provider, tokenResponse.tokens, emailAddress);
  const returnLink = state.returnUrl ? `<p><a href="${escapeHtml(state.returnUrl)}">Return to ReceiptVault</a></p>` : "";
  return html(`<h1>${provider.label} connected</h1><p>ReceiptVault can now run purchase-document imports for this account.</p>${returnLink}`);
}

async function readOAuthStartBody(request: Request): Promise<OAuthStartRequest> {
  try {
    return (await request.json()) as OAuthStartRequest;
  } catch {
    return {};
  }
}

async function storeManualImapConnector(request: Request, env: Env, user: JWTPayload): Promise<Response> {
  if (!env.CONNECTOR_TOKEN_ENCRYPTION_KEY) return json({ error: "connector_encryption_not_configured" }, 503);

  const body = await readImapManualSetupBody(request);
  const emailAddress = normalizeEmail(body.emailAddress);
  const host = normalizeImapHost(body.host);
  const port = normalizePort(body.port);
  const username = typeof body.username === "string" ? body.username.trim() : "";
  const password = typeof body.password === "string" ? body.password : "";
  const useTls = body.useTls !== false;

  if (!emailAddress) return json({ error: "valid_email_required" }, 400);
  if (!host) return json({ error: "valid_imap_host_required" }, 400);
  if (!port) return json({ error: "valid_imap_port_required" }, 400);
  if (!username) return json({ error: "imap_username_required" }, 400);
  if (!password) return json({ error: "imap_password_required" }, 400);

  const provider = providerConfig("imap", env);
  const encrypted = await encryptJson({
    host,
    port,
    username,
    password,
    useTls
  }, env.CONNECTOR_TOKEN_ENCRYPTION_KEY);

  await env.RECEIPTS_BUCKET.put(connectorTokenObjectName(String(user.sub || ""), "imap"), JSON.stringify({
    provider: "imap",
    label: provider.label,
    scope: provider.scope,
    storedAt: new Date().toISOString(),
    emailAddress,
    host,
    port,
    useTls,
    encrypted
  }), {
    httpMetadata: { contentType: "application/json" }
  });
  await indexConnectorUser(env, String(user.sub || ""));

  return json({
    ok: true,
    provider: "imap",
    label: provider.label,
    emailAddress,
    host,
    port,
    useTls
  });
}

async function readImapManualSetupBody(request: Request): Promise<ImapManualSetupRequest> {
  try {
    return (await request.json()) as ImapManualSetupRequest;
  } catch {
    return {};
  }
}

function normalizeEmail(value: unknown): string | null {
  if (typeof value !== "string") return null;
  const email = value.trim().toLowerCase();
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) ? email : null;
}

function normalizeImapHost(value: unknown): string | null {
  if (typeof value !== "string") return null;
  const host = value.trim().toLowerCase();
  return /^(?!-)[a-z0-9.-]{2,253}(?<!-)$/.test(host) && host.includes(".") ? host : null;
}

function normalizePort(value: unknown): number | null {
  const port = typeof value === "number" ? value : typeof value === "string" ? Number(value.trim()) : Number.NaN;
  return Number.isInteger(port) && port > 0 && port <= 65535 ? port : null;
}

function normalizeProvider(provider: unknown): ConnectorProviderId | null {
  if (typeof provider !== "string") return null;
  const normalized = provider.toLowerCase();
  if (normalized === "gmail" || normalized === "google") return "gmail";
  if (normalized === "outlook" || normalized === "microsoft") return "outlook";
  if (normalized === "yahoo") return "yahoo";
  if (normalized === "imap") return "imap";
  return null;
}

function missingProviderSecrets(provider: ConnectorProviderId, env: Env): string[] {
  const missing: string[] = [];
  if (provider === "gmail") {
    if (!env.GOOGLE_OAUTH_CLIENT_ID) missing.push("GOOGLE_OAUTH_CLIENT_ID");
    if (!env.GOOGLE_OAUTH_CLIENT_SECRET) missing.push("GOOGLE_OAUTH_CLIENT_SECRET");
  }
  if (provider === "outlook") {
    if (!env.MICROSOFT_OAUTH_CLIENT_ID) missing.push("MICROSOFT_OAUTH_CLIENT_ID");
    if (!env.MICROSOFT_OAUTH_CLIENT_SECRET) missing.push("MICROSOFT_OAUTH_CLIENT_SECRET");
  }
  if (provider === "yahoo") {
    if (!env.YAHOO_OAUTH_CLIENT_ID) missing.push("YAHOO_OAUTH_CLIENT_ID");
    if (!env.YAHOO_OAUTH_CLIENT_SECRET) missing.push("YAHOO_OAUTH_CLIENT_SECRET");
  }
  return missing;
}

function buildAuthorizationUrl(provider: OAuthProviderConfig, redirectUri: string, state: string): string {
  const url = new URL(provider.authUrl);
  url.searchParams.set("client_id", provider.clientId || "");
  url.searchParams.set("redirect_uri", redirectUri);
  url.searchParams.set("response_type", "code");
  url.searchParams.set("scope", provider.scope);
  url.searchParams.set("state", state);

  if (provider.id === "gmail") {
    url.searchParams.set("access_type", "offline");
    url.searchParams.set("prompt", "consent");
  }

  if (provider.id === "outlook") {
    url.searchParams.set("prompt", "select_account");
  }

  return url.toString();
}

async function exchangeOAuthCode(
  provider: OAuthProviderConfig,
  code: string,
  redirectUri: string
): Promise<{ ok: true; tokens: Record<string, unknown> } | { ok: false; error: string }> {
  const body = new URLSearchParams();
  body.set("grant_type", "authorization_code");
  body.set("code", code);
  body.set("redirect_uri", redirectUri);

  const headers: Record<string, string> = { "content-type": "application/x-www-form-urlencoded" };

  if (provider.id === "yahoo") {
    headers.authorization = `Basic ${btoa(`${provider.clientId}:${provider.clientSecret}`)}`;
  } else {
    body.set("client_id", provider.clientId || "");
    body.set("client_secret", provider.clientSecret || "");
  }

  const response = await fetch(provider.tokenUrl, { method: "POST", headers, body });
  const tokens = await response.json<Record<string, unknown>>().catch(() => ({}));
  if (!response.ok) {
    return { ok: false, error: JSON.stringify(tokens) };
  }
  return { ok: true, tokens };
}

async function storeConnectorToken(
  env: Env,
  userId: string,
  provider: OAuthProviderConfig,
  tokens: Record<string, unknown>,
  emailAddress?: string | null
): Promise<void> {
  if (!env.CONNECTOR_TOKEN_ENCRYPTION_KEY) throw new Error("missing encryption key");
  const tokenSet = tokenSetWithExpiry(tokens);
  const encrypted = await encryptJson(tokenSet, env.CONNECTOR_TOKEN_ENCRYPTION_KEY);
  const key = connectorTokenObjectName(userId, provider.id);
  const existingMetadata = await readConnectorMetadata(env, userId, provider.id);
  // Preserve metadata such as the email address and sync cursor when refreshing tokens.
  const existingEmail = emailAddress ? null : existingMetadata?.emailAddress;
  await env.RECEIPTS_BUCKET.put(key, JSON.stringify({
    provider: provider.id,
    label: provider.label,
    scope: provider.scope,
    storedAt: existingMetadata?.storedAt || new Date().toISOString(),
    expiresAt: tokenSet.expiresAt || null,
    emailAddress: emailAddress || existingEmail || null,
    lastSuccessfulSyncAt: existingMetadata?.lastSuccessfulSyncAt || null,
    encrypted
  }), {
    httpMetadata: { contentType: "application/json" }
  });
  await indexConnectorUser(env, userId);
}

/**
 * Best-effort lookup of the connected mailbox's email address right after the
 * OAuth code exchange, so the connectors list can show "user@gmail.com" instead
 * of a generic provider label. Never throws — the address is cosmetic and must
 * not fail the OAuth flow.
 */
async function fetchConnectorEmailAddress(
  provider: OAuthProviderConfig,
  tokens: Record<string, unknown>
): Promise<string | null> {
  const accessToken = typeof tokens.access_token === "string" ? tokens.access_token : "";
  try {
    if (provider.id === "gmail" && accessToken) {
      const response = await fetch("https://gmail.googleapis.com/gmail/v1/users/me/profile", {
        headers: { authorization: `Bearer ${accessToken}` }
      });
      if (response.ok) {
        const body = await response.json<Record<string, unknown>>();
        const email = normalizeEmail(body.emailAddress);
        if (email) return email;
      }
    }

    if (provider.id === "outlook" && accessToken) {
      const response = await fetch("https://graph.microsoft.com/v1.0/me?$select=mail,userPrincipalName", {
        headers: { authorization: `Bearer ${accessToken}` }
      });
      if (response.ok) {
        const body = await response.json<Record<string, unknown>>();
        const email = normalizeEmail(body.mail) || normalizeEmail(body.userPrincipalName);
        if (email) return email;
      }
    }

    // Fallback for providers that return an OpenID Connect id_token (e.g. Yahoo
    // when OpenID scopes are granted): read the email claim from the JWT payload.
    const idToken = typeof tokens.id_token === "string" ? tokens.id_token : "";
    return emailFromIdToken(idToken);
  } catch {
    return null;
  }
}

function emailFromIdToken(idToken: string): string | null {
  const parts = idToken.split(".");
  if (parts.length < 2) return null;
  try {
    const payload = JSON.parse(new TextDecoder().decode(base64UrlDecode(parts[1]))) as Record<string, unknown>;
    return normalizeEmail(payload.email);
  } catch {
    return null;
  }
}

function tokenSetWithExpiry(tokens: Record<string, unknown>): OAuthTokenSet {
  const expiresIn = numberFromUnknown(tokens.expires_in);
  const existingExpiry = typeof tokens.expiresAt === "string" ? tokens.expiresAt : undefined;
  return {
    ...tokens,
    expiresAt: existingExpiry || (expiresIn ? new Date(Date.now() + expiresIn * 1000).toISOString() : undefined)
  };
}

async function syncConnectorAccounts(request: Request, env: Env, user: JWTPayload): Promise<Response> {
  const body = await readConnectorSyncBody(request);
  const provider = body.provider ? normalizeProvider(body.provider) : null;
  if (body.provider && !provider) return json({ error: "unsupported_provider" }, 400);

  // Determine the user's plan before syncing so paid users get higher scan limits.
  const plan = await getEffectivePlan(env, user);
  const limit = planSyncLimit(plan, body.maxCandidates);

  const reports = await syncProvidersForUser(
    env,
    String(user.sub || ""),
    provider || undefined,
    limit,
    plan !== "free",
    plan
  );
  return json({ ok: true, reports });
}

async function getEffectivePlan(env: Env, user: JWTPayload): Promise<EffectivePlan> {
  if (typeof user.plan === "string") {
    if (user.plan === "business") return "business";
    if (user.plan === "plus" || user.plan === "pro") return "plus";
  }
  if (hasPlus(user)) return "plus";
  const entitlement = await loadBillingEntitlement(env, String(user.sub || ""));
  if (entitlement?.active) {
    return entitlement.plan === "business" ? "business" : "plus";
  }
  return "free";
}

async function loadBillingEntitlement(env: Env, userId: string): Promise<{ active: boolean; plan: string } | null> {
  const object = await env.RECEIPTS_BUCKET.get(billingEntitlementObjectName(userId));
  if (!object) return null;
  const data: Record<string, unknown> = await object.json<Record<string, unknown>>().catch((): Record<string, unknown> => ({}));
  return { active: data.active === true, plan: typeof data.plan === "string" ? data.plan : "free" };
}

function planSyncLimit(plan: EffectivePlan, requested: unknown): number {
  const max = MONTHLY_IMPORT_LIMITS[plan];
  const r = typeof requested === "number" ? requested : typeof requested === "string" ? Number(requested) : max;
  if (!Number.isFinite(r) || r <= 0) return max;
  return Math.min(Math.floor(r), max);
}

async function readConnectorSyncBody(request: Request): Promise<ConnectorSyncRequest> {
  try {
    return (await request.json()) as ConnectorSyncRequest;
  } catch {
    return {};
  }
}

function normalizeSyncLimit(value: unknown): number {
  const requested = typeof value === "number" ? value : typeof value === "string" ? Number(value.trim()) : 10;
  if (!Number.isFinite(requested)) return 10;
  return Math.max(1, Math.min(25, Math.floor(requested)));
}

async function syncIndexedConnectorAccounts(env: Env): Promise<void> {
  const listed = await env.RECEIPTS_BUCKET.list({ prefix: "connectors/users/", limit: 25 });
  for (const object of listed.objects) {
    const userId = object.key.replace(/^connectors\/users\//, "").replace(/\.json$/, "");
    if (userId) await syncProvidersForUser(env, userId, undefined, 10);
  }
}

async function syncProvidersForUser(
  env: Env,
  userId: string,
  provider?: ConnectorProviderId,
  maxCandidates = 10,
  paginate = false,
  plan: EffectivePlan = "free"
): Promise<ConnectorSyncReport[]> {
  const providers: ConnectorProviderId[] = provider ? [provider] : ["gmail", "outlook", "yahoo", "imap"];
  const reports: ConnectorSyncReport[] = [];

  for (const providerId of providers) {
    const metadata = await readConnectorMetadata(env, userId, providerId);
    if (!metadata) continue;
    const report = await syncProviderForUser(env, userId, providerId, metadata, maxCandidates, paginate, plan);
    await writeSyncReport(env, userId, report);
    reports.push(report);
  }

  return reports;
}

async function syncProviderForUser(
  env: Env,
  userId: string,
  providerId: ConnectorProviderId,
  metadata: StoredConnectorMetadata,
  maxCandidates: number,
  paginate = false,
  plan: EffectivePlan = "free"
): Promise<ConnectorSyncReport> {
  const syncedAt = new Date().toISOString();

  try {
    if (providerId === "imap") {
      return syncUnavailable(
        providerId,
        syncedAt,
        "Manual IMAP settings are stored encrypted, but live IMAP mailbox polling is not available in this Worker build.",
        plan
      );
    }

    if (providerId === "yahoo") {
      return syncUnavailable(
        providerId,
        syncedAt,
        "Yahoo OAuth is connected, but live Yahoo mailbox polling is not available in this Worker build.",
        plan
      );
    }

    const provider = providerConfig(providerId, env);
    if (!isProviderConfigured(provider)) return syncError(providerId, syncedAt, "provider_not_configured");
    if (!metadata.encrypted) return syncError(providerId, syncedAt, "missing_encrypted_token");
    if (!env.CONNECTOR_TOKEN_ENCRYPTION_KEY) return syncError(providerId, syncedAt, "connector_encryption_not_configured");

    const tokens = await decryptJson<OAuthTokenSet>(metadata.encrypted, env.CONNECTOR_TOKEN_ENCRYPTION_KEY);
    if (!tokens?.access_token && !tokens?.refresh_token) return syncError(providerId, syncedAt, "missing_oauth_token");

    const refreshed = await ensureAccessToken(env, userId, provider, tokens);
    if (!refreshed.access_token) return syncError(providerId, syncedAt, "missing_access_token");

    const startingUsage = await loadMonthlyImportUsage(env, userId, plan);
    if (startingUsage.remaining <= 0) {
      return syncImportLimitReached(providerId, syncedAt, plan, startingUsage);
    }

    const sinceIso = connectorSyncSince(metadata);
    const candidates = providerId === "gmail"
      ? await fetchGmailCandidates(provider, refreshed.access_token, maxCandidates, paginate, metadata.emailAddress, sinceIso)
      : await fetchOutlookCandidates(provider, refreshed.access_token, maxCandidates, sinceIso);
    const receiptCandidates = candidates
      .filter((candidate) => !isExcludedSender(candidate.from))
      .map((candidate) => ({
        ...candidate,
        signals: receiptSignals(candidateText(candidate))
      }))
      .filter((candidate) => candidate.signals.length > 0);
    const matchedSignals = unique(receiptCandidates.flatMap((candidate) => candidate.signals));

    // Fetch full body for each receipt candidate and create receipt records
    const importedReceipts: Record<string, unknown>[] = [];
    let remainingImports = startingUsage.remaining;
    for (const candidate of receiptCandidates) {
      if (remainingImports <= 0) break;

      // Use provider-prefixed message ID as receipt ID so duplicate syncs can be skipped.
      const receiptId = `${providerId}-${candidate.id}`;
      const objectName = receiptMetadataObjectName(userId, receiptId);
      const existing = await env.RECEIPTS_BUCKET.head(objectName);
      if (existing) continue;

      const content = providerId === "gmail"
        ? await fetchGmailMessageContent(env, userId, receiptId, refreshed.access_token as string, candidate.id)
        : await fetchOutlookMessageContent(env, userId, receiptId, refreshed.access_token as string, candidate.id, candidate.snippet || "");
      const bodyText = (content.text || candidate.snippet || "").slice(0, EMAIL_BODY_TEXT_LIMIT);
      const documentText = [
        candidate.subject || "",
        candidate.from || "",
        candidate.snippet || "",
        bodyText,
        content.attachments.map((attachment) => `${attachment.filename} ${attachment.mimeType}`).join(" ")
      ].join(" ");
      const purchasedAtMs = candidate.date ? Date.parse(candidate.date) : Date.now();

      // Gemini categorization — best-effort, falls back to regex if unavailable
      const ai = await callGeminiForEmail(
        env,
        candidate.subject || "",
        candidate.from || "",
        candidate.date || "",
        bodyText
      );
      const signalSet = unique([...candidate.signals, ...receiptSignals(documentText)]);
      const documentType = normalizeDocumentType(typeof ai?.documentType === "string" ? ai.documentType : undefined)
        || documentTypeFromText(documentText, signalSet);

      const merchant = (typeof ai?.merchant === "string" && ai.merchant.trim())
        ? ai.merchant.trim()
        : extractMerchant(candidate.from || "");
      const amountCents = (typeof ai?.total === "number" && ai.total > 0)
        ? Math.round(ai.total * 100)
        : extractAmountCents(`${candidate.subject || ""} ${bodyText}`);
      const category = normalizeReceiptCategory(typeof ai?.category === "string" && ai.category !== "Uncategorized"
        ? ai.category
        : "Uncategorized");
      const aiConfidence = typeof ai?.confidence === "number" ? ai.confidence : 0;
      const purchasedAtMillis = Number.isNaN(purchasedAtMs) ? Date.now() : purchasedAtMs;
      const sourceLabel = documentTypeLabel(documentType);

      // Use bodyText only in memory for extraction — do NOT persist raw email content
      const receipt: Record<string, unknown> = {
        id: receiptId,
        emailMessageId: receiptId,
        documentType,
        merchant,
        amountCents,
        purchasedAtMillis,
        category,
        location: "Location not detected",
        returnByMillis: null,
        warrantyUntilMillis: null,
        metadataPattern: receiptMetadataPattern(purchasedAtMillis, category, null, null, documentType),
        notes: aiConfidence > 0.6
          ? `Gemini categorized ${Math.round(aiConfidence * 100)}% — ${candidate.from || "email"} · ${candidate.subject || "no subject"}`
          : `Imported from ${candidate.from || "email"} — ${candidate.subject || "no subject"}`,
        rawText: bodyText,
        imagePath: "",
        source: "EmailShare",
        emailSubject: candidate.subject || "",
        emailFrom: candidate.from || "",
        emailDate: candidate.date || "",
        emailAttachments: content.attachments,
        emailUrl: candidate.webUrl || (providerId === "gmail"
          ? `https://mail.google.com/mail/u/0/#all/${candidate.id}`
          : providerId === "outlook"
          ? `https://outlook.live.com/mail/0/inbox/id/${encodeURIComponent(candidate.id)}`
          : null)
      };
      await env.RECEIPTS_BUCKET.put(
        objectName,
        JSON.stringify(receipt),
        { httpMetadata: { contentType: "application/json" } }
      );
      importedReceipts.push(receipt);
      remainingImports--;
    }
    const finalUsage = await incrementMonthlyImportUsage(env, userId, plan, importedReceipts.length);
    await updateConnectorLastSuccessfulSyncAt(env, userId, providerId, syncedAt);

    const reachedLimit = finalUsage.remaining <= 0 && importedReceipts.length < receiptCandidates.length;
    return {
      ok: true,
      provider: providerId,
      status: reachedLimit ? "import_limit_reached" : "import_complete",
      syncedAt,
      scanned: candidates.length,
      candidates: receiptCandidates.length,
      imported: importedReceipts.length,
      matchedSignals,
      receipts: importedReceipts,
      plan,
      monthlyImportLimit: finalUsage.limit,
      monthlyImportUsed: finalUsage.used,
      monthlyImportRemaining: finalUsage.remaining,
      message: importedReceipts.length > 0
        ? reachedLimit
          ? `Imported ${importedReceipts.length} purchase document${importedReceipts.length === 1 ? "" : "s"} and reached the ${finalUsage.limit}/month ${plan} import limit.`
          : `Imported ${importedReceipts.length} purchase document${importedReceipts.length === 1 ? "" : "s"} from ${candidates.length} scanned messages.`
        : receiptCandidates.length > 0
        ? `Scanned ${candidates.length} messages; no new purchase documents to import.`
        : `Scanned ${candidates.length} messages; none matched receipt, bill, invoice, warranty, order, or statement signals strongly enough to import.`
    };
  } catch (error) {
    return syncError(providerId, syncedAt, error instanceof Error ? error.message : "sync_failed");
  }
}

function syncError(provider: ConnectorProviderId, syncedAt: string, error: string): ConnectorSyncReport {
  return {
    ok: false,
    provider,
    status: "sync_error",
    syncedAt,
    scanned: 0,
    candidates: 0,
    imported: 0,
    matchedSignals: [],
    message: "Connector sync could not complete.",
    error
  };
}

function syncUnavailable(
  provider: ConnectorProviderId,
  syncedAt: string,
  message: string,
  plan: EffectivePlan
): ConnectorSyncReport {
  return {
    ok: false,
    provider,
    status: "sync_unavailable",
    syncedAt,
    scanned: 0,
    candidates: 0,
    imported: 0,
    matchedSignals: [],
    message,
    error: "provider_sync_not_implemented",
    plan
  };
}

async function readConnectorMetadata(
  env: Env,
  userId: string,
  provider: ConnectorProviderId
): Promise<StoredConnectorMetadata | null> {
  const object = await env.RECEIPTS_BUCKET.get(connectorTokenObjectName(userId, provider));
  if (!object) return null;
  return object.json<StoredConnectorMetadata>().catch(() => null);
}

function connectorSyncSince(metadata: StoredConnectorMetadata): string | undefined {
  const value = metadata.lastSuccessfulSyncAt || metadata.storedAt;
  if (!value) return undefined;
  const millis = Date.parse(value);
  return Number.isNaN(millis) ? undefined : new Date(millis).toISOString();
}

async function updateConnectorLastSuccessfulSyncAt(
  env: Env,
  userId: string,
  provider: ConnectorProviderId,
  syncedAt: string
): Promise<void> {
  const metadata = await readConnectorMetadata(env, userId, provider);
  if (!metadata) return;
  await env.RECEIPTS_BUCKET.put(connectorTokenObjectName(userId, provider), JSON.stringify({
    ...metadata,
    lastSuccessfulSyncAt: syncedAt
  }), {
    httpMetadata: { contentType: "application/json" }
  });
}

async function ensureAccessToken(
  env: Env,
  userId: string,
  provider: OAuthProviderConfig,
  tokens: OAuthTokenSet
): Promise<OAuthTokenSet> {
  if (!tokenNeedsRefresh(tokens)) return tokens;
  if (!tokens.refresh_token) return tokens;

  const refreshed = await refreshOAuthToken(provider, tokens.refresh_token);
  const merged = tokenSetWithExpiry({
    ...tokens,
    ...refreshed,
    refresh_token: refreshed.refresh_token || tokens.refresh_token
  });
  await storeConnectorToken(env, userId, provider, merged);
  return merged;
}

function tokenNeedsRefresh(tokens: OAuthTokenSet): boolean {
  if (!tokens.access_token) return true;
  if (!tokens.expiresAt) return Boolean(tokens.refresh_token);
  const expiresAt = Date.parse(tokens.expiresAt);
  return Number.isNaN(expiresAt) || expiresAt - Date.now() < 5 * 60 * 1000;
}

async function refreshOAuthToken(provider: OAuthProviderConfig, refreshToken: string): Promise<Record<string, unknown>> {
  const body = new URLSearchParams();
  body.set("grant_type", "refresh_token");
  body.set("refresh_token", refreshToken);

  const headers: Record<string, string> = { "content-type": "application/x-www-form-urlencoded" };
  if (provider.id === "yahoo") {
    headers.authorization = `Basic ${btoa(`${provider.clientId}:${provider.clientSecret}`)}`;
  } else {
    body.set("client_id", provider.clientId || "");
    body.set("client_secret", provider.clientSecret || "");
  }

  const response = await fetch(provider.tokenUrl, { method: "POST", headers, body });
  const tokens = await response.json<Record<string, unknown>>().catch(() => ({}));
  if (!response.ok) throw new Error(`token_refresh_failed:${response.status}`);
  return tokens;
}

async function fetchGmailCandidates(
  provider: OAuthProviderConfig,
  accessToken: string,
  maxCandidates: number,
  paginate = false,
  accountEmail?: string,
  sinceIso?: string
): Promise<ProviderMessageCandidate[]> {
  const candidates: ProviderMessageCandidate[] = [];
  let pageToken: string | undefined;

  do {
    const listUrl = new URL("https://gmail.googleapis.com/gmail/v1/users/me/messages");
    listUrl.searchParams.set("q", gmailQuerySince(provider.query, sinceIso));
    listUrl.searchParams.set("maxResults", String(Math.min(50, maxCandidates - candidates.length)));
    if (pageToken) listUrl.searchParams.set("pageToken", pageToken);

    const listResponse = await fetch(listUrl, { headers: { authorization: `Bearer ${accessToken}` } });
    if (!listResponse.ok) throw new Error(`gmail_list_failed:${listResponse.status}`);

    const list = await listResponse.json<Record<string, unknown>>();
    const messages = Array.isArray(list.messages) ? list.messages.filter(isRecord) : [];

    for (const message of messages) {
      if (candidates.length >= maxCandidates) break;
      const id = typeof message.id === "string" ? message.id : "";
      if (!id) continue;

      const detailUrl = new URL(`https://gmail.googleapis.com/gmail/v1/users/me/messages/${encodeURIComponent(id)}`);
      detailUrl.searchParams.set("format", "metadata");
      detailUrl.searchParams.append("metadataHeaders", "Subject");
      detailUrl.searchParams.append("metadataHeaders", "From");
      detailUrl.searchParams.append("metadataHeaders", "Date");
      detailUrl.searchParams.append("metadataHeaders", "Message-ID");

      const detailResponse = await fetch(detailUrl, { headers: { authorization: `Bearer ${accessToken}` } });
      if (!detailResponse.ok) continue;
      const detail: Record<string, unknown> = await detailResponse.json<Record<string, unknown>>().catch((): Record<string, unknown> => ({}));
      const headers = gmailHeaders(detail);
      const threadId = typeof detail.threadId === "string" ? detail.threadId : id;
      const rfc822MessageId = typeof headers["Message-ID"] === "string" ? headers["Message-ID"] : "";
      if (!isMessageAfterSince(headers.Date, sinceIso)) continue;
      const webUrl = gmailWebUrl(accountEmail, threadId, rfc822MessageId);
      candidates.push({
        id,
        subject: headers.Subject,
        from: headers.From,
        date: headers.Date,
        snippet: typeof detail.snippet === "string" ? detail.snippet : "",
        hasAttachments: false,
        webUrl,
        rfc822MessageId
      });
    }

    const nextToken = typeof list.nextPageToken === "string" ? list.nextPageToken : undefined;
    pageToken = (paginate && candidates.length < maxCandidates) ? nextToken : undefined;
  } while (pageToken);

  return candidates;
}

function gmailQuerySince(baseQuery: string, sinceIso?: string): string {
  if (!sinceIso) return baseQuery;
  const millis = Date.parse(sinceIso);
  if (Number.isNaN(millis)) return baseQuery;
  const date = new Date(millis).toISOString().slice(0, 10).replace(/-/g, "/");
  return `${baseQuery} after:${date}`;
}

function isMessageAfterSince(messageDate: string | undefined, sinceIso?: string): boolean {
  if (!sinceIso) return true;
  const sinceMs = Date.parse(sinceIso);
  if (Number.isNaN(sinceMs)) return true;
  const messageMs = Date.parse(messageDate || "");
  return !Number.isNaN(messageMs) && messageMs > sinceMs;
}

function gmailWebUrl(accountEmail: string | undefined, threadId: string, rfc822MessageId?: string): string {
  const base = accountEmail
    ? `https://mail.google.com/mail/u/${encodeURIComponent(accountEmail)}/`
    : "https://mail.google.com/mail/u/0/";
  const cleanMessageId = (rfc822MessageId || "").trim().replace(/^<|>$/g, "");
  if (cleanMessageId) {
    return `${base}#search/rfc822msgid%3A${encodeURIComponent(cleanMessageId)}`;
  }
  return `${base}#all/${encodeURIComponent(threadId)}`;
}

async function fetchGmailMessageContent(
  env: Env,
  userId: string,
  receiptId: string,
  accessToken: string,
  messageId: string
): Promise<ProviderMessageContent> {
  const url = new URL(`https://gmail.googleapis.com/gmail/v1/users/me/messages/${encodeURIComponent(messageId)}`);
  url.searchParams.set("format", "full");
  const response = await fetch(url, { headers: { authorization: `Bearer ${accessToken}` } });
  if (!response.ok) return { text: "", attachments: [] };
  const message: Record<string, unknown> = await response.json<Record<string, unknown>>().catch((): Record<string, unknown> => ({}));
  const payload: Record<string, unknown> = isRecord(message.payload) ? message.payload : {};
  const textParts: string[] = [];
  const attachmentRefs: GmailAttachmentRef[] = [];
  collectGmailMessageContent(payload, textParts, attachmentRefs);

  const attachments: EmailAttachmentRecord[] = [];
  let totalBytes = 0;
  for (const attachment of attachmentRefs.slice(0, MAX_ATTACHMENTS_PER_DOCUMENT)) {
    const record = baseAttachmentRecord(attachment.id, attachment.filename, attachment.mimeType, attachment.size);
    if (!attachment.attachmentId) {
      record.skippedReason = "missing_attachment_id";
    } else if (attachment.size > MAX_ATTACHMENT_BYTES) {
      record.skippedReason = "too_large";
    } else if (totalBytes + attachment.size > MAX_ATTACHMENT_TOTAL_BYTES) {
      record.skippedReason = "total_limit";
    } else {
      const stored = await fetchAndStoreGmailAttachment(env, userId, receiptId, accessToken, messageId, attachment);
      record.size = stored.size || record.size;
      record.storageKey = stored.storageKey;
      record.stored = Boolean(stored.storageKey);
      record.skippedReason = stored.skippedReason;
      totalBytes += stored.size || 0;
    }
    attachments.push(record);
  }

  return {
    text: textParts.join("\n").replace(/\s{3,}/g, " ").trim().slice(0, EMAIL_BODY_TEXT_LIMIT),
    attachments
  };
}

function syncImportLimitReached(
  provider: ConnectorProviderId,
  syncedAt: string,
  plan: EffectivePlan,
  usage: MonthlyImportUsage
): ConnectorSyncReport {
  return {
    ok: true,
    provider,
    status: "import_limit_reached",
    syncedAt,
    scanned: 0,
    candidates: 0,
    imported: 0,
    matchedSignals: [],
    plan,
    monthlyImportLimit: usage.limit,
    monthlyImportUsed: usage.used,
    monthlyImportRemaining: usage.remaining,
    message: `${planLabel(plan)} import limit reached for ${usage.period}: ${usage.used}/${usage.limit}. Upgrade to Plus for more monthly imports.`
  };
}

function collectGmailMessageContent(
  part: Record<string, unknown>,
  textParts: string[],
  attachments: GmailAttachmentRef[]
): void {
  const mimeType = typeof part.mimeType === "string" ? part.mimeType : "";
  const filename = typeof part.filename === "string" ? part.filename.trim() : "";
  const partId = typeof part.partId === "string" ? part.partId : crypto.randomUUID();
  const body: Record<string, unknown> = isRecord(part.body) ? part.body : {};
  const data = typeof body.data === "string" ? body.data : "";
  const attachmentId = typeof body.attachmentId === "string" ? body.attachmentId : undefined;
  const size = numberFromUnknown(body.size) || 0;
  if (data && mimeType.startsWith("text/")) {
    const decoded = new TextDecoder().decode(base64UrlDecode(data));
    textParts.push(mimeType === "text/html" ? stripHtml(decoded) : decoded);
  } else if ((filename || attachmentId) && !mimeType.startsWith("text/")) {
    attachments.push({
      id: partId,
      filename: filename || `attachment-${partId}`,
      mimeType: mimeType || "application/octet-stream",
      size,
      attachmentId
    });
  }
  const parts = Array.isArray(part.parts) ? part.parts.filter(isRecord) : [];
  for (const child of parts) collectGmailMessageContent(child, textParts, attachments);
}

async function fetchAndStoreGmailAttachment(
  env: Env,
  userId: string,
  receiptId: string,
  accessToken: string,
  messageId: string,
  attachment: GmailAttachmentRef
): Promise<{ storageKey?: string; size?: number; skippedReason?: string }> {
  const url = new URL(`https://gmail.googleapis.com/gmail/v1/users/me/messages/${encodeURIComponent(messageId)}/attachments/${encodeURIComponent(attachment.attachmentId || "")}`);
  const response = await fetch(url, { headers: { authorization: `Bearer ${accessToken}` } });
  if (!response.ok) return { skippedReason: `gmail_attachment_failed:${response.status}` };
  const body = await response.json<Record<string, unknown>>().catch((): Record<string, unknown> => ({}));
  const data = typeof body.data === "string" ? body.data : "";
  if (!data) return { skippedReason: "empty_attachment" };
  const bytes = base64UrlDecode(data);
  if (bytes.byteLength > MAX_ATTACHMENT_BYTES) return { size: bytes.byteLength, skippedReason: "too_large" };
  const storageKey = emailAttachmentObjectName(userId, receiptId, attachment.id, attachment.filename);
  await env.RECEIPTS_BUCKET.put(storageKey, bytes, {
    httpMetadata: {
      contentType: attachment.mimeType || "application/octet-stream"
    }
  });
  return { storageKey, size: bytes.byteLength };
}

async function fetchOutlookMessageContent(
  env: Env,
  userId: string,
  receiptId: string,
  accessToken: string,
  messageId: string,
  fallbackText: string
): Promise<ProviderMessageContent> {
  const messageUrl = new URL(`https://graph.microsoft.com/v1.0/me/messages/${encodeURIComponent(messageId)}`);
  messageUrl.searchParams.set("$select", "body,bodyPreview");
  const response = await fetch(messageUrl, { headers: { authorization: `Bearer ${accessToken}` } });
  let text = fallbackText;
  if (response.ok) {
    const body = await response.json<Record<string, unknown>>().catch((): Record<string, unknown> => ({}));
    const messageBody = isRecord(body.body) ? body.body : {};
    const content = typeof messageBody.content === "string" ? messageBody.content : "";
    const contentType = typeof messageBody.contentType === "string" ? messageBody.contentType.toLowerCase() : "";
    text = contentType === "html" ? stripHtml(content) : content || fallbackText;
  }

  const attachments = await fetchAndStoreOutlookAttachments(env, userId, receiptId, accessToken, messageId);
  return {
    text: text.replace(/\s{3,}/g, " ").trim().slice(0, EMAIL_BODY_TEXT_LIMIT),
    attachments
  };
}

async function fetchAndStoreOutlookAttachments(
  env: Env,
  userId: string,
  receiptId: string,
  accessToken: string,
  messageId: string
): Promise<EmailAttachmentRecord[]> {
  const listUrl = new URL(`https://graph.microsoft.com/v1.0/me/messages/${encodeURIComponent(messageId)}/attachments`);
  listUrl.searchParams.set("$top", String(MAX_ATTACHMENTS_PER_DOCUMENT));
  listUrl.searchParams.set("$select", "id,name,contentType,size,isInline");
  const response = await fetch(listUrl, { headers: { authorization: `Bearer ${accessToken}` } });
  if (!response.ok) return [];
  const body = await response.json<Record<string, unknown>>().catch((): Record<string, unknown> => ({}));
  const values = Array.isArray(body.value) ? body.value.filter(isRecord).slice(0, MAX_ATTACHMENTS_PER_DOCUMENT) : [];
  const attachments: EmailAttachmentRecord[] = [];
  let totalBytes = 0;

  for (const attachment of values) {
    const id = typeof attachment.id === "string" ? attachment.id : crypto.randomUUID();
    const filename = typeof attachment.name === "string" && attachment.name.trim() ? attachment.name.trim() : `attachment-${id}`;
    const mimeType = typeof attachment.contentType === "string" && attachment.contentType.trim()
      ? attachment.contentType.trim()
      : "application/octet-stream";
    const size = numberFromUnknown(attachment.size) || 0;
    const record = baseAttachmentRecord(id, filename, mimeType, size);
    if (attachment.isInline === true) {
      record.skippedReason = "inline";
      attachments.push(record);
      continue;
    }
    if (size > MAX_ATTACHMENT_BYTES) {
      record.skippedReason = "too_large";
      attachments.push(record);
      continue;
    }
    if (totalBytes + size > MAX_ATTACHMENT_TOTAL_BYTES) {
      record.skippedReason = "total_limit";
      attachments.push(record);
      continue;
    }

    const stored = await fetchAndStoreOutlookAttachment(env, userId, receiptId, accessToken, messageId, record);
    record.size = stored.size || record.size;
    record.storageKey = stored.storageKey;
    record.stored = Boolean(stored.storageKey);
    record.skippedReason = stored.skippedReason;
    totalBytes += stored.size || 0;
    attachments.push(record);
  }

  return attachments;
}

async function fetchAndStoreOutlookAttachment(
  env: Env,
  userId: string,
  receiptId: string,
  accessToken: string,
  messageId: string,
  attachment: EmailAttachmentRecord
): Promise<{ storageKey?: string; size?: number; skippedReason?: string }> {
  const url = new URL(`https://graph.microsoft.com/v1.0/me/messages/${encodeURIComponent(messageId)}/attachments/${encodeURIComponent(attachment.id)}`);
  const response = await fetch(url, { headers: { authorization: `Bearer ${accessToken}` } });
  if (!response.ok) return { skippedReason: `outlook_attachment_failed:${response.status}` };
  const body = await response.json<Record<string, unknown>>().catch((): Record<string, unknown> => ({}));
  const contentBytes = typeof body.contentBytes === "string" ? body.contentBytes : "";
  if (!contentBytes) return { skippedReason: "metadata_only" };
  const bytes = base64Decode(contentBytes);
  if (bytes.byteLength > MAX_ATTACHMENT_BYTES) return { size: bytes.byteLength, skippedReason: "too_large" };
  const storageKey = emailAttachmentObjectName(userId, receiptId, attachment.id, attachment.filename);
  await env.RECEIPTS_BUCKET.put(storageKey, bytes, {
    httpMetadata: {
      contentType: attachment.mimeType || "application/octet-stream"
    }
  });
  return { storageKey, size: bytes.byteLength };
}

function stripHtml(html: string): string {
  return html
    .replace(/<style[^>]*>[\s\S]*?<\/style>/gi, " ")
    .replace(/<script[^>]*>[\s\S]*?<\/script>/gi, " ")
    .replace(/<[^>]+>/g, " ")
    .replace(/&nbsp;/gi, " ")
    .replace(/&amp;/gi, "&")
    .replace(/&lt;/gi, "<")
    .replace(/&gt;/gi, ">")
    .replace(/&quot;/gi, "\"")
    .replace(/&#39;/gi, "'")
    .replace(/\s{2,}/g, " ")
    .trim();
}

function extractMerchant(from: string): string {
  // "Amazon.com <auto-confirm@amazon.com>" → "Amazon.com"
  const nameMatch = from.match(/^"?([^"<]+?)"?\s*</);
  if (nameMatch) return nameMatch[1].trim();
  // "no-reply@bestbuy.com" → "Bestbuy"
  const domainMatch = from.match(/@([^.@]+)\./);
  if (domainMatch) {
    const d = domainMatch[1];
    return d.charAt(0).toUpperCase() + d.slice(1);
  }
  return from.trim() || "Unknown store";
}

function extractAmountCents(text: string): number {
  const amounts: number[] = [];
  const patterns = [
    /\$\s*([0-9]{1,4}(?:,[0-9]{3})*\.[0-9]{2})/g,
    /USD\s*([0-9]{1,4}(?:,[0-9]{3})*\.[0-9]{2})/gi,
    /total[^$\d]{0,20}\$?\s*([0-9]{1,4}(?:,[0-9]{3})*\.[0-9]{2})/gi,
  ];
  for (const pattern of patterns) {
    let match;
    while ((match = pattern.exec(text)) !== null) {
      const value = parseFloat(match[1].replace(/,/g, ""));
      if (!Number.isNaN(value) && value > 0) amounts.push(Math.round(value * 100));
    }
  }
  if (amounts.length === 0) return 0;
  // Prefer largest amount ≤ $10,000 (likely the order total, not a line item)
  const valid = amounts.filter((a) => a <= 1000000);
  return valid.length > 0 ? Math.max(...valid) : 0;
}

function normalizeReceiptCategory(value: string | undefined): string {
  const normalized = (value || "")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, " ")
    .trim();
  if (!normalized) return "Uncategorized";
  if (["grocery", "groceries", "supermarket", "market"].includes(normalized)) return "Groceries";
  if (["electronic", "electronics", "tech", "computer", "phone"].includes(normalized)) return "Electronics";
  if (["home", "house", "household", "utilities", "utility"].includes(normalized)) return "Home";
  if (["business", "office", "work", "professional"].includes(normalized)) return "Business";
  if (["shopping", "retail", "store", "purchase"].includes(normalized)) return "Shopping";
  if (["food", "restaurant", "restaurants", "dining", "coffee", "cafe"].includes(normalized)) return "Food";
  if (["travel", "trip", "hotel", "flight", "airline", "rideshare"].includes(normalized)) return "Travel";
  if (["health", "healthcare", "medical", "pharmacy", "doctor", "clinic"].includes(normalized)) return "Health";
  if (["auto", "car", "vehicle", "gas", "fuel", "automotive"].includes(normalized)) return "Auto";
  if (normalized === "other") return "Other";
  if (normalized === "uncategorized") return "Uncategorized";
  const categories = ["Groceries", "Electronics", "Home", "Business", "Shopping", "Food", "Travel", "Health", "Auto", "Other", "Uncategorized"];
  return categories.find((category) => toPatternToken(category) === toPatternToken(normalized)) || "Other";
}

function receiptMetadataPattern(
  purchasedAtMillis: number,
  category: string,
  returnByMillis: number | null,
  warrantyUntilMillis: number | null,
  documentType: PurchaseDocumentType = "receipt"
): string {
  const purchased = new Date(purchasedAtMillis).toISOString().slice(0, 10);
  const month = purchased.slice(0, 7);
  const returnValue = returnByMillis ? new Date(returnByMillis).toISOString().slice(0, 10) : "not-set";
  const warrantyValue = warrantyUntilMillis ? new Date(warrantyUntilMillis).toISOString().slice(0, 10) : "not-set";
  return [
    `purchased:${purchased}`,
    `purchase:${purchased}`,
    `date:${purchased}`,
    `month:${month}`,
    `type:${documentType}`,
    `document:${documentType}`,
    `category:${toPatternToken(category)}`,
    `return:${returnByMillis ? "set" : "not-set"}`,
    `return:${returnValue}`,
    `warranty:${warrantyUntilMillis ? "set" : "not-set"}`,
    `warranty:${warrantyValue}`
  ].join(" ");
}

function toPatternToken(value: string): string {
  return value.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-+|-+$/g, "") || "uncategorized";
}

function normalizeDocumentType(value: string | undefined): PurchaseDocumentType | null {
  const normalized = (value || "").toLowerCase().replace(/[^a-z0-9]+/g, " ").trim();
  if (!normalized) return null;
  if (["receipt", "sales receipt"].includes(normalized)) return "receipt";
  if (["order", "order confirmation", "purchase order"].includes(normalized)) return "order";
  if (["invoice"].includes(normalized)) return "invoice";
  if (["bill", "utility bill", "payment due"].includes(normalized)) return "bill";
  if (["statement", "account statement"].includes(normalized)) return "statement";
  if (["warranty", "protection plan", "coverage"].includes(normalized)) return "warranty";
  if (["return", "return label", "return confirmation"].includes(normalized)) return "return";
  if (["subscription", "recurring charge"].includes(normalized)) return "subscription";
  if (normalized === "other") return "other";
  return null;
}

function documentTypeFromText(text: string, signals: string[]): PurchaseDocumentType {
  const normalized = text.toLowerCase();
  if (signals.includes("warranty")) return "warranty";
  if (signals.includes("return")) return "return";
  if (signals.includes("invoice")) return "invoice";
  if (signals.includes("bill")) return "bill";
  if (signals.includes("statement")) return "statement";
  if (signals.includes("subscription")) return "subscription";
  if (signals.includes("order") || signals.includes("shipped")) return "order";
  if (signals.includes("receipt") || signals.includes("purchase")) return "receipt";
  if (/\bstatement\b|\baccount summary\b/.test(normalized)) return "statement";
  return "other";
}

function documentTypeLabel(type: PurchaseDocumentType): string {
  return type.charAt(0).toUpperCase() + type.slice(1);
}

function baseAttachmentRecord(id: string, filename: string, mimeType: string, size: number): EmailAttachmentRecord {
  return {
    id,
    filename,
    mimeType,
    size,
    stored: false
  };
}

function gmailHeaders(detail: Record<string, unknown>): Record<string, string> {
  const payload = isRecord(detail.payload) ? detail.payload : {};
  const rawHeaders = Array.isArray(payload.headers) ? payload.headers : [];
  return rawHeaders.filter(isRecord).reduce<Record<string, string>>((headers, header) => {
    const name = typeof header.name === "string" ? header.name : "";
    const value = typeof header.value === "string" ? header.value : "";
    if (name && value) headers[name] = value;
    return headers;
  }, {});
}

async function fetchOutlookCandidates(
  _provider: OAuthProviderConfig,
  accessToken: string,
  maxCandidates: number,
  sinceIso?: string
): Promise<ProviderMessageCandidate[]> {
  const searchUrl = new URL("https://graph.microsoft.com/v1.0/me/messages");
  searchUrl.searchParams.set("$top", String(maxCandidates));
  searchUrl.searchParams.set("$select", "id,subject,from,receivedDateTime,bodyPreview,hasAttachments,webLink,internetMessageId");
  searchUrl.searchParams.set("$orderby", "receivedDateTime desc");
  if (sinceIso) searchUrl.searchParams.set("$filter", `receivedDateTime gt ${sinceIso}`);

  let response = await fetch(searchUrl, {
    headers: { authorization: `Bearer ${accessToken}` }
  });

  if (!response.ok) {
    const fallbackUrl = new URL("https://graph.microsoft.com/v1.0/me/messages");
    fallbackUrl.searchParams.set("$top", String(maxCandidates));
    fallbackUrl.searchParams.set("$select", "id,subject,from,receivedDateTime,bodyPreview,hasAttachments,webLink,internetMessageId");
    fallbackUrl.searchParams.set("$orderby", "receivedDateTime desc");
    response = await fetch(fallbackUrl, {
      headers: { authorization: `Bearer ${accessToken}` }
    });
  }

  if (!response.ok) throw new Error(`outlook_list_failed:${response.status}`);
  const body = await response.json<Record<string, unknown>>();
  const values = Array.isArray(body.value)
    ? body.value.filter(isRecord).filter((message) => {
      const received = typeof message.receivedDateTime === "string" ? message.receivedDateTime : "";
      return isMessageAfterSince(received, sinceIso);
    }).slice(0, maxCandidates)
    : [];
  return values.map((message) => {
    const from = isRecord(message.from) && isRecord(message.from.emailAddress)
      ? String(message.from.emailAddress.address || message.from.emailAddress.name || "")
      : "";
    const id = typeof message.id === "string" ? message.id : crypto.randomUUID();
    const internetMessageId = typeof message.internetMessageId === "string" ? message.internetMessageId : "";
    return {
      id,
      subject: typeof message.subject === "string" ? message.subject : "",
      from,
      date: typeof message.receivedDateTime === "string" ? message.receivedDateTime : "",
      snippet: typeof message.bodyPreview === "string" ? message.bodyPreview : "",
      hasAttachments: message.hasAttachments === true,
      webUrl: typeof message.webLink === "string" ? message.webLink : outlookWebUrl(id),
      rfc822MessageId: internetMessageId
    };
  });
}

function outlookWebUrl(messageId: string): string {
  return `https://outlook.live.com/mail/0/inbox/id/${encodeURIComponent(messageId)}`;
}

function candidateText(candidate: ProviderMessageCandidate): string {
  return [candidate.subject, candidate.from, candidate.snippet]
    .filter((value): value is string => typeof value === "string")
    .join(" ")
    .toLowerCase();
}

async function writeSyncReport(env: Env, userId: string, report: ConnectorSyncReport): Promise<void> {
  await env.RECEIPTS_BUCKET.put(connectorSyncReportObjectName(userId, report.provider), JSON.stringify(report), {
    httpMetadata: { contentType: "application/json" }
  });
}

async function listConnectorSyncReports(env: Env, user: JWTPayload): Promise<Response> {
  const userId = String(user.sub || "");
  const reports: ConnectorSyncReport[] = [];
  for (const provider of ["gmail", "outlook", "yahoo", "imap"] as ConnectorProviderId[]) {
    const object = await env.RECEIPTS_BUCKET.get(connectorSyncReportObjectName(userId, provider));
    if (object) {
      const report = await object.json<ConnectorSyncReport>().catch(() => null);
      if (report) reports.push(report);
    }
  }
  return json({ ok: true, reports });
}

function connectorSyncReportObjectName(userId: string, provider: ConnectorProviderId): string {
  return `users/${userId}/connectors/${provider}/last-sync.json`;
}

function monthlyImportUsageObjectName(userId: string, period: string): string {
  return `users/${userId}/connectors/import-usage/${period}.json`;
}

function currentUsagePeriod(date = new Date()): string {
  return date.toISOString().slice(0, 7);
}

async function loadMonthlyImportUsage(env: Env, userId: string, plan: EffectivePlan): Promise<MonthlyImportUsage> {
  const period = currentUsagePeriod();
  const limit = monthlyImportLimit(plan);
  const object = await env.RECEIPTS_BUCKET.get(monthlyImportUsageObjectName(userId, period));
  const data = object
    ? await object.json<Record<string, unknown>>().catch((): Record<string, unknown> => ({}))
    : {};
  const used = Math.max(0, Math.floor(numberFromUnknown(data.used) || 0));
  return {
    period,
    used,
    limit,
    remaining: Math.max(0, limit - used)
  };
}

async function incrementMonthlyImportUsage(
  env: Env,
  userId: string,
  plan: EffectivePlan,
  imported: number
): Promise<MonthlyImportUsage> {
  const current = await loadMonthlyImportUsage(env, userId, plan);
  const used = Math.min(current.limit, current.used + Math.max(0, Math.floor(imported)));
  const updated: MonthlyImportUsage = {
    ...current,
    used,
    remaining: Math.max(0, current.limit - used)
  };
  await env.RECEIPTS_BUCKET.put(monthlyImportUsageObjectName(userId, updated.period), JSON.stringify({
    period: updated.period,
    plan,
    used: updated.used,
    limit: updated.limit,
    remaining: updated.remaining,
    updatedAt: new Date().toISOString()
  }), {
    httpMetadata: { contentType: "application/json" }
  });
  return updated;
}

function monthlyImportLimit(plan: EffectivePlan): number {
  return MONTHLY_IMPORT_LIMITS[plan];
}

function planLabel(plan: EffectivePlan): string {
  return plan.charAt(0).toUpperCase() + plan.slice(1);
}

function receiptMetadataObjectName(userId: string, receiptId: string): string {
  return `users/${userId}/receipts/${receiptId}/metadata.json`;
}

function emailAttachmentObjectName(userId: string, receiptId: string, attachmentId: string, filename: string): string {
  return `users/${userId}/receipts/${receiptId}/attachments/${safeObjectName(attachmentId)}-${safeObjectName(filename)}`;
}

async function getReceiptAttachment(request: Request, env: Env, user: JWTPayload): Promise<Response> {
  const userId = String(user.sub || "");
  const key = new URL(request.url).searchParams.get("key") || "";
  if (!isUserAttachmentKey(userId, key)) return json({ error: "bad_attachment_key" }, 400);

  const object = await env.RECEIPTS_BUCKET.get(key);
  if (!object) return json({ error: "attachment_not_found" }, 404);

  const headers = new Headers();
  object.writeHttpMetadata(headers);
  if (!headers.has("content-type")) headers.set("content-type", "application/octet-stream");
  headers.set("cache-control", "private, max-age=60");
  headers.set("content-length", String(object.size));
  headers.set("content-disposition", inlineContentDisposition(key.split("/").pop() || "attachment"));

  return new Response(object.body, { headers });
}

function isUserAttachmentKey(userId: string, key: string): boolean {
  if (!userId || !key || key.includes("..") || key.includes("\\") || key.includes("\0")) return false;
  const prefix = `users/${userId}/receipts/`;
  return key.startsWith(prefix) && key.includes("/attachments/");
}

function inlineContentDisposition(filename: string): string {
  const fallback = filename.replace(/[\r\n"\\]/g, "_").slice(0, 120) || "attachment";
  return `inline; filename="${fallback}"; filename*=UTF-8''${encodeRfc5987ValueChars(filename)}`;
}

function encodeRfc5987ValueChars(value: string): string {
  return encodeURIComponent(value)
    .replace(/['()]/g, (char) => `%${char.charCodeAt(0).toString(16).toUpperCase()}`)
    .replace(/\*/g, "%2A");
}

function safeObjectName(value: string): string {
  return value
    .replace(/[/\\?#%:<>|"^`{}[\]\s]+/g, "-")
    .replace(/-+/g, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, 120) || "attachment";
}

async function indexConnectorUser(env: Env, userId: string): Promise<void> {
  await env.RECEIPTS_BUCKET.put(`connectors/users/${userId}.json`, JSON.stringify({
    userId,
    updatedAt: new Date().toISOString()
  }), {
    httpMetadata: { contentType: "application/json" }
  });
}

async function removeConnectorUserIndexIfEmpty(env: Env, userId: string): Promise<void> {
  for (const provider of ["gmail", "outlook", "yahoo", "imap"] as ConnectorProviderId[]) {
    const object = await env.RECEIPTS_BUCKET.get(connectorTokenObjectName(userId, provider));
    if (object) return;
  }
  await env.RECEIPTS_BUCKET.delete(`connectors/users/${userId}.json`);
}

async function listConnectorAccounts(env: Env, user: JWTPayload): Promise<Response> {
  const userId = String(user.sub || "");
  const providers: ConnectorProviderId[] = ["gmail", "outlook", "yahoo", "imap"];
  const accounts: Array<Record<string, unknown>> = [];

  for (const provider of providers) {
    const object = await env.RECEIPTS_BUCKET.get(connectorTokenObjectName(userId, provider));
    if (object) {
      const metadata = await object.json<Record<string, unknown>>().catch((): Record<string, unknown> => ({}));
      accounts.push({
        provider,
        label: metadata.label || provider,
        scope: metadata.scope || "",
        storedAt: metadata.storedAt || null,
        emailAddress: metadata.emailAddress || null,
        host: metadata.host || null,
        port: metadata.port || null,
        connected: true
      });
    }
  }

  return json({ ok: true, accounts });
}

async function deleteConnectorAccount(request: Request, env: Env, user: JWTPayload): Promise<Response> {
  const provider = normalizeProvider(new URL(request.url).pathname.split("/").pop());
  if (!provider) return json({ error: "unsupported_provider" }, 400);
  const userId = String(user.sub || "");
  await env.RECEIPTS_BUCKET.delete(connectorTokenObjectName(userId, provider));
  await env.RECEIPTS_BUCKET.delete(connectorSyncReportObjectName(userId, provider));
  await deleteProviderReceipts(env, userId, provider);
  await removeConnectorUserIndexIfEmpty(env, userId);
  return json({ ok: true, provider });
}

async function deleteProviderReceipts(env: Env, userId: string, provider: ConnectorProviderId): Promise<void> {
  const prefix = `users/${userId}/receipts/${provider}-`;
  let cursor: string | undefined;
  do {
    const listed = await env.RECEIPTS_BUCKET.list({ prefix, cursor, limit: 100 });
    await Promise.all(listed.objects.map((obj) => env.RECEIPTS_BUCKET.delete(obj.key)));
    cursor = listed.truncated ? listed.cursor : undefined;
  } while (cursor);
}

function connectorTokenObjectName(userId: string, provider: ConnectorProviderId): string {
  return `users/${userId}/connectors/${provider}/oauth-token.json`;
}

async function signOAuthState(state: OAuthState, secret: string): Promise<string> {
  const payload = base64UrlEncode(new TextEncoder().encode(JSON.stringify(state)));
  const key = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(secret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"]
  );
  const signature = await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(payload));
  return `${payload}.${base64UrlEncode(new Uint8Array(signature))}`;
}

async function verifyOAuthState(stateParam: string, secret: string): Promise<OAuthState | null> {
  const [payload, signature] = stateParam.split(".");
  if (!payload || !signature) return null;

  const key = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(secret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["verify"]
  );
  const ok = await crypto.subtle.verify(
    "HMAC",
    key,
    base64UrlDecode(signature),
    new TextEncoder().encode(payload)
  );
  if (!ok) return null;

  return JSON.parse(new TextDecoder().decode(base64UrlDecode(payload))) as OAuthState;
}

async function encryptJson(value: unknown, secret: string): Promise<Record<string, string>> {
  const iv = crypto.getRandomValues(new Uint8Array(12));
  const key = await aesKey(secret);
  const plaintext = new TextEncoder().encode(JSON.stringify(value));
  const ciphertext = await crypto.subtle.encrypt({ name: "AES-GCM", iv }, key, plaintext);
  return {
    alg: "AES-GCM",
    iv: base64UrlEncode(iv),
    ciphertext: base64UrlEncode(new Uint8Array(ciphertext))
  };
}

async function decryptJson<T>(encrypted: EncryptedJsonPayload, secret: string): Promise<T | null> {
  if (encrypted.alg !== "AES-GCM") return null;
  const key = await aesKey(secret);
  const plaintext = await crypto.subtle.decrypt(
    { name: "AES-GCM", iv: base64UrlDecode(encrypted.iv) },
    key,
    base64UrlDecode(encrypted.ciphertext)
  );
  return JSON.parse(new TextDecoder().decode(plaintext)) as T;
}

async function aesKey(secret: string): Promise<CryptoKey> {
  const hash = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(secret));
  return crypto.subtle.importKey("raw", hash, "AES-GCM", false, ["encrypt", "decrypt"]);
}

function base64UrlEncode(bytes: Uint8Array): string {
  let raw = "";
  bytes.forEach((byte) => {
    raw += String.fromCharCode(byte);
  });
  return btoa(raw).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
}

function base64UrlDecode(value: string): Uint8Array {
  const padded = value.replace(/-/g, "+").replace(/_/g, "/").padEnd(Math.ceil(value.length / 4) * 4, "=");
  const raw = atob(padded);
  return Uint8Array.from(raw, (char) => char.charCodeAt(0));
}

function base64Decode(value: string): Uint8Array {
  const padded = value.padEnd(Math.ceil(value.length / 4) * 4, "=");
  const raw = atob(padded);
  return Uint8Array.from(raw, (char) => char.charCodeAt(0));
}

async function connectorCandidate(request: Request): Promise<Response> {
  const body = await readConnectorCandidateBody(request);
  const text = [body.subject, body.from, body.snippet]
    .filter((value): value is string => typeof value === "string")
    .join(" ")
    .toLowerCase();

  const signals = isExcludedSender(body.from) ? [] : receiptSignals(text);
  const shouldInspect = signals.length > 0;
  return json({
    ok: true,
    provider: body.provider || "unknown",
    shouldInspectBody: shouldInspect,
    shouldInspectAttachments: shouldInspect && body.hasAttachments === true,
    shouldStoreMessage: false,
    matchedSignals: signals,
    reason: shouldInspect
      ? "Candidate looks like a receipt, bill, invoice, warranty, order, statement, or return document. Inspect only the body or attachments needed to import document data."
      : "No purchase-document signal. Discard headers/snippet and do not inspect body or attachments."
  });
}

async function readConnectorCandidateBody(request: Request): Promise<ConnectorCandidateRequest> {
  try {
    return (await request.json()) as ConnectorCandidateRequest;
  } catch {
    return {};
  }
}

function receiptSignals(text: string): string[] {
  const patterns: Array<[string, RegExp]> = [
    ["receipt", /\breceipt\b/],
    ["order", /\border\b|\border confirmation\b/],
    ["invoice", /\binvoice\b/],
    ["purchase", /\bpurchase\b|\bpurchase confirmation\b/],
    ["shipped", /\bshipped\b|\bdelivered\b/],
    ["statement", /\bstatement\b|\baccount summary\b|\bmonthly summary\b/],
    ["subscription", /\bsubscription\b|\brecurring charge\b|\bmembership renewal\b/],
    // "return" alone is far too broad (matches "return to sender", "returning customer", etc.)
    // — require purchase-return context.
    ["return", /\breturn window\b|\bdays? to return\b|\breturn eligible\b|\bfree returns?\b/],
    // Bare "warranty" matches Google Play / device setup emails — require product-warranty context.
    ["warranty", /\b(?:extended|limited|manufacturer'?s?|\d+[\s-]?(?:year|yr|month))\s+warranty\b|\bwarranty\s+(?:card|period|coverage|registration|information|claim)\b|\bprotection plan\b/],
    ["bill", /\b(?:utility|water|electric(?:ity)?|gas|phone|cable|internet|monthly|annual|quarterly|your|new)\s+(?:e-?)?bill\b|\be-?bill\b|\bbill\s*[-–—]?\s*(?:due|payment|summary|statement)\b|\bamount\s+due\b|\bpayment\s+due\b|\bstatement\s+(?:available|ready|summary)\b/i],
    ["merchant", /\bamazon\b|\bwalmart\b|\btarget\b|\bcostco\b|\bbest buy\b|\bhome depot\b|\blowe'?s\b|\bapple\b|\bstaples\b/]
  ];
  return patterns
    .filter(([, pattern]) => pattern.test(text))
    .map(([label]) => label);
}

// Google system / Play Store senders are not merchant receipts even when they
// mention "order", "receipt", or "warranty" (account notices, Play setup emails, etc.).
function isExcludedSender(from: string | undefined): boolean {
  if (!from) return false;
  const sender = from.toLowerCase();
  return (
    sender.includes("@google.com") ||
    sender.includes("noreply@google.com") ||
    sender.includes("no-reply@accounts.google.com") ||
    sender.includes("googleplay")
  );
}

const geminiResponseSchema = {
  type: "OBJECT",
  properties: {
    isReceipt: { type: "BOOLEAN" },
    merchant: { type: "STRING" },
    total: { type: "NUMBER" },
    purchaseDate: { type: "STRING" },
    category: {
      type: "STRING",
      enum: [
        "Groceries",
        "Electronics",
        "Home",
        "Business",
        "Shopping",
        "Food",
        "Travel",
        "Health",
        "Auto",
        "Other",
        "Uncategorized"
      ]
    },
    documentType: {
      type: "STRING",
      enum: ["receipt", "order", "invoice", "bill", "statement", "warranty", "return", "subscription", "other"]
    },
    warrantyCandidate: { type: "BOOLEAN" },
    returnWindowDays: { type: "INTEGER" },
    confidence: { type: "NUMBER" },
    notes: { type: "STRING" }
  },
  required: ["isReceipt", "confidence", "category", "documentType"]
};

async function categorizeReceipt(request: Request, env: Env): Promise<Response> {
  if (!env.GEMINI_API_KEY) {
    return json({ error: "ai_not_configured" }, 503);
  }

  const body = await readCategorizeBody(request);
  const ocrText = (body.ocrText || "").trim();
  if (!ocrText) return json({ error: "ocr_text_required" }, 400);
  if (ocrText.length > 12000) return json({ error: "ocr_text_too_large" }, 413);

  const model = env.GEMINI_MODEL || "gemini-2.5-flash-lite";
  const prompt = buildCategorizationPrompt(body, ocrText);
  const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${env.GEMINI_API_KEY}`;
  const response = await fetch(geminiUrl, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({
      contents: [{ role: "user", parts: [{ text: prompt }] }],
      generationConfig: {
        temperature: 0.1,
        responseMimeType: "application/json",
        responseSchema: geminiResponseSchema
      }
    })
  });

  if (!response.ok) {
    return json({ error: "gemini_request_failed", status: response.status }, 502);
  }

  const result = await response.json<Record<string, unknown>>();
  const text = extractGeminiText(result);
  if (!text) return json({ error: "gemini_empty_response" }, 502);

  try {
    return json({ ok: true, model, result: JSON.parse(text) });
  } catch {
    return json({ ok: true, model, result: { raw: text, confidence: 0.2, isReceipt: false } });
  }
}

async function readCategorizeBody(request: Request): Promise<CategorizeRequest> {
  try {
    return (await request.json()) as CategorizeRequest;
  } catch {
    return {};
  }
}

function buildCategorizationPrompt(body: CategorizeRequest, ocrText: string): string {
  return [
    "You classify purchase documents for ReceiptVault.",
    "Use only the provided document text and optional email headers.",
    "Classify receipts, order confirmations, invoices, bills, utility bills, statements, returns, subscriptions, and warranty/protection records.",
    "If the text is not one of those records, set isReceipt to false and confidence to 0.3 or lower.",
    "Set warrantyCandidate true only when the text explicitly says warranty, guarantee, protection plan, or coverage for a product or service.",
    "Do not infer warranty from category, card/bank receipts, or standard return/exchange wording.",
    "Do not include unrelated email content. Return only JSON.",
    "",
    "JSON schema:",
    "{",
    '  "isReceipt": boolean,',
    '  "merchant": string,',
    '  "total": number | null,',
    '  "currencyCode": "USD" | "CAD" | "AUD" | "EUR" | "GBP" | "INR" | "PKR" | "AED" | "SAR" | "JPY" | "CNY" | "KRW" | "TRY" | "BRL" | "MXN" | null,',
    '  "purchaseDate": "YYYY-MM-DD" | null,',
    '  "documentType": "receipt" | "order" | "invoice" | "bill" | "statement" | "warranty" | "return" | "subscription" | "other",',
    '  "category": "Groceries" | "Electronics" | "Home" | "Business" | "Shopping" | "Food" | "Travel" | "Health" | "Auto" | "Other" | "Uncategorized",',
    '  "warrantyCandidate": boolean,',
    '  "returnWindowDays": number | null,',
    '  "confidence": number,',
    '  "notes": string',
    "}",
    "",
    "Category guidance:",
    '- For government fees, municipal payments, taxes, tolls, DMV: use "Other"',
    '- For utility bills (electric, gas, water, internet, phone): use "Home"',
    '- For restaurants, cafes, fast food, coffee shops: use "Food"',
    '- For department stores, retail, online shopping (non-grocery, non-electronics): use "Shopping"',
    '- Only use "Business" for B2B purchases, office supplies, professional services, or vendor invoices',
    "",
    `Email subject: ${body.emailSubject || ""}`,
    `Email from: ${body.emailFrom || ""}`,
    `Email date: ${body.emailDate || ""}`,
    "",
    "Receipt/order text:",
    ocrText
  ].join("\n");
}

async function callGeminiForEmail(
  env: Env,
  subject: string,
  from: string,
  date: string,
  bodyText: string
): Promise<Record<string, unknown> | null> {
  if (!env.GEMINI_API_KEY || !bodyText.trim()) return null;
  const prompt = buildCategorizationPrompt(
    { emailSubject: subject, emailFrom: from, emailDate: date },
    bodyText.slice(0, GEMINI_EMAIL_TEXT_LIMIT)
  );
  const model = env.GEMINI_MODEL || "gemini-2.5-flash-lite";
  const url = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${env.GEMINI_API_KEY}`;
  try {
    const res = await fetch(url, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({
        contents: [{ role: "user", parts: [{ text: prompt }] }],
        generationConfig: {
          temperature: 0.1,
          responseMimeType: "application/json",
          responseSchema: geminiResponseSchema
        }
      })
    });
    if (!res.ok) return null;
    const json = await res.json<Record<string, unknown>>().catch(() => null);
    if (!json) return null;
    const text = extractGeminiText(json);
    if (!text) return null;
    return JSON.parse(text) as Record<string, unknown>;
  } catch {
    return null;
  }
}

function extractGeminiText(result: Record<string, unknown>): string | null {
  const candidates = result.candidates;
  if (!Array.isArray(candidates)) return null;
  const first = candidates[0];
  if (!isRecord(first)) return null;
  const content = first.content;
  if (!isRecord(content)) return null;
  const parts = content.parts;
  if (!Array.isArray(parts)) return null;
  const textPart = parts.find((part) => isRecord(part) && typeof part.text === "string");
  return isRecord(textPart) && typeof textPart.text === "string" ? textPart.text : null;
}

async function authenticate(request: Request, env: Env): Promise<JWTPayload | null> {
  const auth = request.headers.get("authorization") || "";
  const token = auth.startsWith("Bearer ") ? auth.slice("Bearer ".length) : "";
  if (!token) return null;

  const issuer = `https://securetoken.google.com/${env.FIREBASE_PROJECT_ID}`;
  const result = await jwtVerify(token, firebaseKeys, {
    issuer,
    audience: env.FIREBASE_PROJECT_ID
  });
  return result.payload;
}

function hasPlus(payload: JWTPayload): boolean {
  return payload.receiptvault_plus === true || payload.plan === "plus" || payload.plan === "pro";
}

function objectNameFor(pathname: string, userId: unknown): string | null {
  if (typeof userId !== "string" || userId.length === 0) return null;
  const parts = pathname.split("/").filter(Boolean);
  const receiptId = parts[2];
  const fileName = parts[3] || "metadata.json";
  if (!receiptId || !/^[a-zA-Z0-9_-]+$/.test(receiptId)) return null;
  if (!/^[a-zA-Z0-9_.-]+$/.test(fileName)) return null;
  return `users/${userId}/receipts/${receiptId}/${fileName}`;
}

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json" }
  });
}

function html(body: string, status = 200): Response {
  return new Response(`<!doctype html><html><body>${body}</body></html>`, {
    status,
    headers: { "content-type": "text/html; charset=utf-8" }
  });
}

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function numberFromUnknown(value: unknown): number | null {
  const number = typeof value === "number" ? value : typeof value === "string" ? Number(value) : Number.NaN;
  return Number.isFinite(number) ? number : null;
}

function unique(values: string[]): string[] {
  return Array.from(new Set(values));
}
