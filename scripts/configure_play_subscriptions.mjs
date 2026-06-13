import crypto from "node:crypto";

const packageName = "com.corsairlabs.receiptvault";
const baseUrl = "https://androidpublisher.googleapis.com/androidpublisher/v3";
const latencyTolerance = "PRODUCT_UPDATE_LATENCY_TOLERANCE_LATENCY_TOLERANT";
const updateMask = "listings,basePlans,taxAndComplianceSettings";

const products = [
  {
    id: "receiptvault_plus_monthly",
    title: "ReceiptVault Plus Monthly",
    description: "ReceiptVault Plus monthly no-ad cloud backup and smart receipt tools.",
    tag: "plus",
    period: "P1M",
    price: "4.99",
    benefits: [
      "No ads",
      "Cloud receipt backup",
      "3 email connectors",
      "Unlimited manual uploads",
      "Smart categorization",
      "Warranty tracking"
    ]
  },
  {
    id: "receiptvault_plus_yearly",
    title: "ReceiptVault Plus Yearly",
    description: "ReceiptVault Plus yearly no-ad cloud backup and smart receipt tools.",
    tag: "plus",
    period: "P1Y",
    price: "47.99",
    benefits: [
      "No ads",
      "Cloud receipt backup",
      "3 email connectors",
      "Unlimited manual uploads",
      "Smart categorization",
      "Warranty tracking"
    ]
  },
  {
    id: "receiptvault_business_monthly",
    title: "ReceiptVault Business Monthly",
    description: "ReceiptVault Business monthly no-ad receipt backup and team-ready expense tools.",
    tag: "business",
    period: "P1M",
    price: "12.99",
    benefits: [
      "No ads",
      "10 email connectors",
      "Business receipt backup",
      "Unlimited manual uploads",
      "Smart categorization",
      "Warranty tracking"
    ]
  },
  {
    id: "receiptvault_business_yearly",
    title: "ReceiptVault Business Yearly",
    description: "ReceiptVault Business yearly no-ad receipt backup and team-ready expense tools.",
    tag: "business",
    period: "P1Y",
    price: "124.99",
    benefits: [
      "No ads",
      "10 email connectors",
      "Business receipt backup",
      "Unlimited manual uploads",
      "Smart categorization",
      "Warranty tracking"
    ]
  }
];

function base64url(value) {
  return Buffer.from(JSON.stringify(value)).toString("base64url");
}

function assertEnv(name) {
  const value = process.env[name];
  if (!value) throw new Error(`${name} is required`);
  return value;
}

async function getAccessToken() {
  const serviceAccount = JSON.parse(assertEnv("GOOGLE_PLAY_SERVICE_ACCOUNT_JSON"));
  console.log(`Using Google Play service account: ${serviceAccount.client_email}`);
  const now = Math.floor(Date.now() / 1000);
  const header = { alg: "RS256", typ: "JWT" };
  const payload = {
    iss: serviceAccount.client_email,
    scope: "https://www.googleapis.com/auth/androidpublisher",
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600
  };
  const signingInput = `${base64url(header)}.${base64url(payload)}`;
  const signature = crypto
    .createSign("RSA-SHA256")
    .update(signingInput)
    .sign(serviceAccount.private_key, "base64url");
  const assertion = `${signingInput}.${signature}`;
  const response = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion
    })
  });
  const json = await response.json();
  if (!response.ok) {
    throw new Error(`google_token_failed:${response.status}:${JSON.stringify(safeError(json))}`);
  }
  return json.access_token;
}

function safeError(json) {
  if (!json || typeof json !== "object") return json;
  const clone = { ...json };
  delete clone.access_token;
  return clone;
}

async function request(accessToken, method, url, body) {
  const response = await fetch(url, {
    method,
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "Content-Type": "application/json"
    },
    body: body === undefined ? undefined : JSON.stringify(body)
  });
  const text = await response.text();
  const json = text ? JSON.parse(text) : {};
  if (!response.ok) {
    const error = new Error(`${method} ${url} failed:${response.status}:${JSON.stringify(safeError(json))}`);
    error.status = response.status;
    error.body = json;
    throw error;
  }
  return json;
}

function moneyFromUsd(value) {
  const [whole, fraction = ""] = value.split(".");
  const cents = fraction.padEnd(2, "0").slice(0, 2);
  return {
    currencyCode: "USD",
    units: whole,
    nanos: Number(cents) * 10_000_000
  };
}

function params(entries) {
  const query = new URLSearchParams();
  for (const [key, value] of entries) query.set(key, value);
  return query.toString();
}

async function getSubscription(accessToken, productId) {
  const url = `${baseUrl}/applications/${encodeURIComponent(packageName)}/subscriptions/${encodeURIComponent(productId)}`;
  try {
    return await request(accessToken, "GET", url);
  } catch (error) {
    if (error.status === 404) return null;
    throw error;
  }
}

async function convertRegionPrices(accessToken, price) {
  const url = `${baseUrl}/applications/${encodeURIComponent(packageName)}/pricing:convertRegionPrices`;
  return await request(accessToken, "POST", url, { price });
}

async function regionalPricing(accessToken, product) {
  const price = moneyFromUsd(product.price);
  try {
    return await convertRegionPrices(accessToken, price);
  } catch (error) {
    if (error.status !== 403) throw error;
    console.log(`${product.id}: convertRegionPrices is permission-denied; using US-only pricing fallback`);
    return {
      regionVersion: { version: "2022/02" },
      convertedRegionPrices: {
        US: { price }
      }
    };
  }
}

function subscriptionBody(product, conversion) {
  const regionalConfigs = Object.entries(conversion.convertedRegionPrices ?? {}).map(
    ([regionCode, converted]) => ({
      regionCode,
      newSubscriberAvailability: true,
      price: converted.price
    })
  );
  const otherRegionsConfig = conversion.convertedOtherRegionsPrice
    ? {
        newSubscriberAvailability: true,
        usdPrice: conversion.convertedOtherRegionsPrice.usdPrice,
        eurPrice: conversion.convertedOtherRegionsPrice.eurPrice
      }
    : undefined;

  return {
    packageName,
    productId: product.id,
    listings: [
      {
        languageCode: "en-US",
        title: product.title,
        description: product.description,
        benefits: product.benefits
      }
    ],
    basePlans: [
      {
        basePlanId: "standard",
        regionalConfigs,
        offerTags: [{ tag: product.tag }],
        otherRegionsConfig,
        autoRenewingBasePlanType: {
          billingPeriodDuration: product.period,
          gracePeriodDuration: "P7D",
          accountHoldDuration: "P53D",
          resubscribeState: "RESUBSCRIBE_STATE_ACTIVE",
          prorationMode: "SUBSCRIPTION_PRORATION_MODE_CHARGE_ON_NEXT_BILLING_DATE"
        }
      }
    ],
    taxAndComplianceSettings: {
      isTokenizedDigitalAsset: false
    }
  };
}

async function patchSubscription(accessToken, product, conversion) {
  const regionVersion = conversion.regionVersion?.version;
  if (!regionVersion) throw new Error(`missing_region_version:${product.id}`);
  const query = params([
    ["updateMask", updateMask],
    ["regionsVersion.version", regionVersion],
    ["allowMissing", "true"],
    ["latencyTolerance", latencyTolerance]
  ]);
  const url = `${baseUrl}/applications/${encodeURIComponent(packageName)}/subscriptions/${encodeURIComponent(product.id)}?${query}`;
  return await request(accessToken, "PATCH", url, subscriptionBody(product, conversion));
}

async function activateBasePlan(accessToken, productId) {
  const url = `${baseUrl}/applications/${encodeURIComponent(packageName)}/subscriptions/${encodeURIComponent(productId)}/basePlans/standard:activate`;
  try {
    await request(accessToken, "POST", url, { latencyTolerance });
    console.log(`${productId}: activated standard base plan`);
  } catch (error) {
    const message = JSON.stringify(safeError(error.body ?? {}));
    if (error.status === 400 && message.toLowerCase().includes("active")) {
      console.log(`${productId}: standard base plan already active`);
      return;
    }
    throw error;
  }
}

async function main() {
  const accessToken = await getAccessToken();
  for (const product of products) {
    const existing = await getSubscription(accessToken, product.id);
    const standardPlan = existing?.basePlans?.find((basePlan) => basePlan.basePlanId === "standard");
    if (standardPlan?.state === "ACTIVE") {
      console.log(`${product.id}: standard base plan already active`);
      continue;
    }

    const conversion = await regionalPricing(accessToken, product);
    const updated = await patchSubscription(accessToken, product, conversion);
    const updatedPlan = updated.basePlans?.find((basePlan) => basePlan.basePlanId === "standard");
    console.log(`${product.id}: configured standard base plan (${updatedPlan?.state ?? "unknown state"})`);
    await activateBasePlan(accessToken, product.id);
  }
}

main().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
