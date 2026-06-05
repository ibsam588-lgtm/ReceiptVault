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
  error?: string;
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
      query: 'newer_than:90d (receipt OR order OR invoice OR "purchase confirmation" OR warranty)',
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
      query: "receipt OR order OR invoice OR purchase confirmation OR warranty",
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

  await storeConnectorToken(env, state.userId, provider, tokenResponse.tokens);
  const returnLink = state.returnUrl ? `<p><a href="${escapeHtml(state.returnUrl)}">Return to ReceiptVault</a></p>` : "";
  return html(`<h1>${provider.label} connected</h1><p>ReceiptVault can now run receipt-only imports for this account.</p>${returnLink}`);
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
  tokens: Record<string, unknown>
): Promise<void> {
  if (!env.CONNECTOR_TOKEN_ENCRYPTION_KEY) throw new Error("missing encryption key");
  const tokenSet = tokenSetWithExpiry(tokens);
  const encrypted = await encryptJson(tokenSet, env.CONNECTOR_TOKEN_ENCRYPTION_KEY);
  const key = connectorTokenObjectName(userId, provider.id);
  await env.RECEIPTS_BUCKET.put(key, JSON.stringify({
    provider: provider.id,
    label: provider.label,
    scope: provider.scope,
    storedAt: new Date().toISOString(),
    expiresAt: tokenSet.expiresAt || null,
    encrypted
  }), {
    httpMetadata: { contentType: "application/json" }
  });
  await indexConnectorUser(env, userId);
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

  const reports = await syncProvidersForUser(
    env,
    String(user.sub || ""),
    provider || undefined,
    normalizeSyncLimit(body.maxCandidates)
  );
  return json({ ok: true, reports });
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
  maxCandidates = 10
): Promise<ConnectorSyncReport[]> {
  const providers: ConnectorProviderId[] = provider ? [provider] : ["gmail", "outlook", "yahoo", "imap"];
  const reports: ConnectorSyncReport[] = [];

  for (const providerId of providers) {
    const metadata = await readConnectorMetadata(env, userId, providerId);
    if (!metadata) continue;
    const report = await syncProviderForUser(env, userId, providerId, metadata, maxCandidates);
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
  maxCandidates: number
): Promise<ConnectorSyncReport> {
  const syncedAt = new Date().toISOString();

  try {
    if (providerId === "imap") {
      return {
        ok: true,
        provider: providerId,
        status: "imap_settings_ready",
        syncedAt,
        scanned: 0,
        candidates: 0,
        imported: 0,
        matchedSignals: [],
        message: "Manual IMAP settings are stored encrypted. Full IMAP message polling requires the provider-specific IMAP fetch worker before import."
      };
    }

    if (providerId === "yahoo") {
      return {
        ok: true,
        provider: providerId,
        status: "yahoo_oauth_ready",
        syncedAt,
        scanned: 0,
        candidates: 0,
        imported: 0,
        matchedSignals: [],
        message: "Yahoo OAuth is connected. Yahoo receipt import will use the IMAP fetch worker after provider-specific mailbox polling is enabled."
      };
    }

    const provider = providerConfig(providerId, env);
    if (!isProviderConfigured(provider)) return syncError(providerId, syncedAt, "provider_not_configured");
    if (!metadata.encrypted) return syncError(providerId, syncedAt, "missing_encrypted_token");
    if (!env.CONNECTOR_TOKEN_ENCRYPTION_KEY) return syncError(providerId, syncedAt, "connector_encryption_not_configured");

    const tokens = await decryptJson<OAuthTokenSet>(metadata.encrypted, env.CONNECTOR_TOKEN_ENCRYPTION_KEY);
    if (!tokens?.access_token && !tokens?.refresh_token) return syncError(providerId, syncedAt, "missing_oauth_token");

    const refreshed = await ensureAccessToken(env, userId, provider, tokens);
    if (!refreshed.access_token) return syncError(providerId, syncedAt, "missing_access_token");

    const candidates = providerId === "gmail"
      ? await fetchGmailCandidates(provider, refreshed.access_token, maxCandidates)
      : await fetchOutlookCandidates(provider, refreshed.access_token, maxCandidates);
    const receiptCandidates = candidates.map((candidate) => ({
      ...candidate,
      signals: receiptSignals(candidateText(candidate))
    })).filter((candidate) => candidate.signals.length > 0);
    const matchedSignals = unique(receiptCandidates.flatMap((candidate) => candidate.signals));

    return {
      ok: true,
      provider: providerId,
      status: "candidate_scan_complete",
      syncedAt,
      scanned: candidates.length,
      candidates: receiptCandidates.length,
      imported: 0,
      matchedSignals,
      message: `Scanned ${candidates.length} receipt-query messages and found ${receiptCandidates.length} likely receipt/order candidates. Body/attachment import remains gated to candidates only.`
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

async function readConnectorMetadata(
  env: Env,
  userId: string,
  provider: ConnectorProviderId
): Promise<StoredConnectorMetadata | null> {
  const object = await env.RECEIPTS_BUCKET.get(connectorTokenObjectName(userId, provider));
  if (!object) return null;
  return object.json<StoredConnectorMetadata>().catch(() => null);
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
  maxCandidates: number
): Promise<ProviderMessageCandidate[]> {
  const listUrl = new URL("https://gmail.googleapis.com/gmail/v1/users/me/messages");
  listUrl.searchParams.set("q", provider.query);
  listUrl.searchParams.set("maxResults", String(maxCandidates));

  const listResponse = await fetch(listUrl, {
    headers: { authorization: `Bearer ${accessToken}` }
  });
  if (!listResponse.ok) throw new Error(`gmail_list_failed:${listResponse.status}`);

  const list = await listResponse.json<Record<string, unknown>>();
  const messages = Array.isArray(list.messages) ? list.messages.filter(isRecord).slice(0, maxCandidates) : [];
  const candidates: ProviderMessageCandidate[] = [];

  for (const message of messages) {
    const id = typeof message.id === "string" ? message.id : "";
    if (!id) continue;
    const detailUrl = new URL(`https://gmail.googleapis.com/gmail/v1/users/me/messages/${encodeURIComponent(id)}`);
    detailUrl.searchParams.set("format", "metadata");
    detailUrl.searchParams.append("metadataHeaders", "Subject");
    detailUrl.searchParams.append("metadataHeaders", "From");
    detailUrl.searchParams.append("metadataHeaders", "Date");

    const detailResponse = await fetch(detailUrl, {
      headers: { authorization: `Bearer ${accessToken}` }
    });
    if (!detailResponse.ok) continue;
    const detail: Record<string, unknown> = await detailResponse.json<Record<string, unknown>>().catch(() => ({}));
    const headers = gmailHeaders(detail);
    candidates.push({
      id,
      subject: headers.Subject,
      from: headers.From,
      date: headers.Date,
      snippet: typeof detail.snippet === "string" ? detail.snippet : "",
      hasAttachments: false
    });
  }

  return candidates;
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
  maxCandidates: number
): Promise<ProviderMessageCandidate[]> {
  const searchUrl = new URL("https://graph.microsoft.com/v1.0/me/messages");
  searchUrl.searchParams.set("$top", String(maxCandidates));
  searchUrl.searchParams.set("$select", "id,subject,from,receivedDateTime,bodyPreview,hasAttachments");
  searchUrl.searchParams.set("$search", "\"receipt OR order OR invoice OR purchase confirmation OR warranty\"");

  let response = await fetch(searchUrl, {
    headers: { authorization: `Bearer ${accessToken}` }
  });

  if (!response.ok) {
    const fallbackUrl = new URL("https://graph.microsoft.com/v1.0/me/messages");
    fallbackUrl.searchParams.set("$top", String(maxCandidates));
    fallbackUrl.searchParams.set("$select", "id,subject,from,receivedDateTime,bodyPreview,hasAttachments");
    fallbackUrl.searchParams.set("$orderby", "receivedDateTime desc");
    response = await fetch(fallbackUrl, {
      headers: { authorization: `Bearer ${accessToken}` }
    });
  }

  if (!response.ok) throw new Error(`outlook_list_failed:${response.status}`);
  const body = await response.json<Record<string, unknown>>();
  const values = Array.isArray(body.value) ? body.value.filter(isRecord).slice(0, maxCandidates) : [];
  return values.map((message) => {
    const from = isRecord(message.from) && isRecord(message.from.emailAddress)
      ? String(message.from.emailAddress.address || message.from.emailAddress.name || "")
      : "";
    return {
      id: typeof message.id === "string" ? message.id : crypto.randomUUID(),
      subject: typeof message.subject === "string" ? message.subject : "",
      from,
      date: typeof message.receivedDateTime === "string" ? message.receivedDateTime : "",
      snippet: typeof message.bodyPreview === "string" ? message.bodyPreview : "",
      hasAttachments: message.hasAttachments === true
    };
  });
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
  await removeConnectorUserIndexIfEmpty(env, userId);
  return json({ ok: true, provider });
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

async function connectorCandidate(request: Request): Promise<Response> {
  const body = await readConnectorCandidateBody(request);
  const text = [body.subject, body.from, body.snippet]
    .filter((value): value is string => typeof value === "string")
    .join(" ")
    .toLowerCase();

  const signals = receiptSignals(text);
  const shouldInspect = signals.length > 0;
  return json({
    ok: true,
    provider: body.provider || "unknown",
    shouldInspectBody: shouldInspect,
    shouldInspectAttachments: shouldInspect && body.hasAttachments === true,
    shouldStoreMessage: false,
    matchedSignals: signals,
    reason: shouldInspect
      ? "Candidate looks like a receipt/order. Inspect only the body or attachments needed to import receipt data."
      : "No receipt/order signal. Discard headers/snippet and do not inspect body or attachments."
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
    ["return", /\breturn\b|\breturn window\b/],
    ["warranty", /\bwarranty\b|\bprotection plan\b/],
    ["merchant", /\bamazon\b|\bwalmart\b|\btarget\b|\bcostco\b|\bbest buy\b|\bhome depot\b|\blowe'?s\b|\bapple\b|\bstaples\b/]
  ];
  return patterns
    .filter(([, pattern]) => pattern.test(text))
    .map(([label]) => label);
}

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
        responseMimeType: "application/json"
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
    "You classify receipt or order text for ReceiptVault.",
    "Use only the provided receipt/order text and optional email headers.",
    "If the text is not a receipt, order, invoice, return, or warranty record, set isReceipt to false and confidence to 0.3 or lower.",
    "Do not include unrelated email content. Return only JSON.",
    "",
    "JSON schema:",
    "{",
    '  "isReceipt": boolean,',
    '  "merchant": string,',
    '  "total": number | null,',
    '  "purchaseDate": "YYYY-MM-DD" | null,',
    '  "category": "Groceries" | "Electronics" | "Home" | "Business" | "Shopping" | "Food" | "Travel" | "Health" | "Auto" | "Other" | "Uncategorized",',
    '  "warrantyCandidate": boolean,',
    '  "returnWindowDays": number | null,',
    '  "confidence": number,',
    '  "notes": string',
    "}",
    "",
    `Email subject: ${body.emailSubject || ""}`,
    `Email from: ${body.emailFrom || ""}`,
    `Email date: ${body.emailDate || ""}`,
    "",
    "Receipt/order text:",
    ocrText
  ].join("\n");
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
