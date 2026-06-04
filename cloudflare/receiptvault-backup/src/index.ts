import { createRemoteJWKSet, jwtVerify, type JWTPayload } from "jose";

type Env = {
  RECEIPTS_BUCKET: R2Bucket;
  FIREBASE_PROJECT_ID: string;
  REQUIRE_PLUS_CLAIM: string;
};

const firebaseKeys = createRemoteJWKSet(
  new URL("https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com")
);

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);

    if (url.pathname === "/health") {
      return json({ ok: true, service: "receiptvault-backup" });
    }

    if (!url.pathname.startsWith("/v1/receipts/")) {
      return json({ error: "not_found" }, 404);
    }

    const user = await authenticate(request, env);
    if (!user) return json({ error: "unauthorized" }, 401);

    if (env.REQUIRE_PLUS_CLAIM === "true" && !hasPlus(user)) {
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
  }
};

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
