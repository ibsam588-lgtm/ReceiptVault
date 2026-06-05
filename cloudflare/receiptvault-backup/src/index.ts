import { createRemoteJWKSet, jwtVerify, type JWTPayload } from "jose";

type Env = {
  RECEIPTS_BUCKET: R2Bucket;
  FIREBASE_PROJECT_ID: string;
  REQUIRE_PLUS_CLAIM: string;
  GEMINI_API_KEY?: string;
  GEMINI_MODEL?: string;
};

type CategorizeRequest = {
  ocrText?: string;
  emailSubject?: string;
  emailFrom?: string;
  emailDate?: string;
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

    if (url.pathname === "/v1/ai/categorize") {
      if (request.method !== "POST") return json({ error: "method_not_allowed" }, 405);

      const user = await authenticate(request, env);
      if (!user) return json({ error: "unauthorized" }, 401);

      return categorizeReceipt(request, env);
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

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}
