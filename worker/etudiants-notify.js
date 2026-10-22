// etudiants-notify: Cloudflare Worker لإرسال الإشعارات (FCM)
// المتغيرات اللي خاصها تكون فـ Cloudflare (Settings > Variables and Secrets):
//   FIREBASE_SERVICE_ACCOUNT  (Secret)  : JSON ديال service account ديال Firebase (كيبقى كيف هو)
//   APP_SECRET                (Secret)  : الكلمة السرية القديمة، فقط للنسخ القديمة من التطبيق (مؤقتا)
//   LEGACY_OFF                (اختياري) : حطها "true" باش تقفل الكلمة السرية القديمة نهائيا
//   ADMIN_UID                 (اختياري) : معرّف الأدمين (إلا ما تحطش، كنستعملو الثابت اللي تحت)

const ADMIN_UID = "pndM3rNT9DRVCPxODaHFFnBO9jU2";
const ALLOWED_ORIGIN = "https://etudiants-droit-maroc.netlify.app";
const JWK_URL = "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com";

// ---------- base64url ----------
function b64urlToBytes(s) {
  s = s.replace(/-/g, "+").replace(/_/g, "/");
  while (s.length % 4) s += "=";
  const bin = atob(s);
  const out = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) out[i] = bin.charCodeAt(i);
  return out;
}
function b64urlToString(s) {
  return new TextDecoder().decode(b64urlToBytes(s));
}
function base64urlEncodeString(str) {
  return btoa(str).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}
function base64urlEncodeArrayBuffer(buf) {
  const bytes = new Uint8Array(buf);
  let binary = "";
  for (let i = 0; i < bytes.byteLength; i++) binary += String.fromCharCode(bytes[i]);
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

class HttpError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

// ---------- التحقق من Firebase ID token ----------
let jwkCache = { keys: null, exp: 0 };
async function getJwks(force) {
  const now = Date.now();
  if (!force && jwkCache.keys && now < jwkCache.exp) return jwkCache.keys;
  const res = await fetch(JWK_URL);
  if (!res.ok) throw new HttpError(503, "jwk_unavailable");
  const json = await res.json();
  jwkCache = { keys: json.keys || [], exp: now + 3600 * 1000 };
  return jwkCache.keys;
}

async function verifyIdToken(idToken, projectId) {
  const parts = String(idToken || "").split(".");
  if (parts.length !== 3) throw new HttpError(401, "bad_token");
  let header, payload;
  try {
    header = JSON.parse(b64urlToString(parts[0]));
    payload = JSON.parse(b64urlToString(parts[1]));
  } catch (e) {
    throw new HttpError(401, "bad_token");
  }
  if (header.alg !== "RS256" || !header.kid) throw new HttpError(401, "bad_token");

  let keys = await getJwks(false);
  let jwk = keys.find((k) => k.kid === header.kid);
  if (!jwk) {
    keys = await getJwks(true); // مفتاح جديد: نعاودو نجيبو
    jwk = keys.find((k) => k.kid === header.kid);
  }
  if (!jwk) throw new HttpError(401, "unknown_key");

  const key = await crypto.subtle.importKey(
    "jwk",
    { kty: jwk.kty, n: jwk.n, e: jwk.e, alg: "RS256", ext: true },
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["verify"]
  );
  const ok = await crypto.subtle.verify(
    "RSASSA-PKCS1-v1_5",
    key,
    b64urlToBytes(parts[2]),
    new TextEncoder().encode(parts[0] + "." + parts[1])
  );
  if (!ok) throw new HttpError(401, "bad_signature");

  const now = Math.floor(Date.now() / 1000);
  if (payload.aud !== projectId) throw new HttpError(401, "bad_audience");
  if (payload.iss !== "https://securetoken.google.com/" + projectId) throw new HttpError(401, "bad_issuer");
  if (!payload.exp || payload.exp < now - 30) throw new HttpError(401, "expired");
  if (payload.iat && payload.iat > now + 60) throw new HttpError(401, "bad_iat");
  if (!payload.sub || typeof payload.sub !== "string") throw new HttpError(401, "bad_subject");
  return { uid: payload.sub, name: payload.name || "" };
}

// ---------- Firestore REST (بصلاحيات المستعمل نفسو، والقواعد هي اللي كتحكم) ----------
async function fsGet(projectId, path, idToken) {
  const res = await fetch(
    `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents/${path}`,
    { headers: { Authorization: "Bearer " + idToken } }
  );
  if (res.status === 404) return null;
  if (res.status === 401 || res.status === 403) throw new HttpError(403, "forbidden");
  if (!res.ok) throw new HttpError(502, "firestore_error");
  return await res.json();
}

function strField(doc, name) {
  return doc && doc.fields && doc.fields[name] && typeof doc.fields[name].stringValue === "string"
    ? doc.fields[name].stringValue
    : "";
}
function listField(doc, name) {
  const vals = doc && doc.fields && doc.fields[name] && doc.fields[name].arrayValue && doc.fields[name].arrayValue.values;
  return (vals || []).map((v) => v.stringValue).filter((v) => typeof v === "string");
}

// ---------- FCM ----------
let accessCache = { token: null, exp: 0 };
async function getAccessToken(serviceAccount) {
  const nowMs = Date.now();
  if (accessCache.token && nowMs < accessCache.exp) return accessCache.token;

  const header = { alg: "RS256", typ: "JWT" };
  const now = Math.floor(nowMs / 1000);
  const claim = {
    iss: serviceAccount.client_email,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    exp: now + 3600,
    iat: now
  };
  const unsigned = `${base64urlEncodeString(JSON.stringify(header))}.${base64urlEncodeString(JSON.stringify(claim))}`;
  const pemContents = serviceAccount.private_key
    .replace(/-----BEGIN PRIVATE KEY-----/, "")
    .replace(/-----END PRIVATE KEY-----/, "")
    .replace(/\s/g, "");
  const binaryDer = Uint8Array.from(atob(pemContents), (c) => c.charCodeAt(0));
  const cryptoKey = await crypto.subtle.importKey(
    "pkcs8",
    binaryDer.buffer,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"]
  );
  const signature = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", cryptoKey, new TextEncoder().encode(unsigned));
  const jwt = `${unsigned}.${base64urlEncodeArrayBuffer(signature)}`;

  const tokenRes = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=${jwt}`
  });
  const tokenJson = await tokenRes.json();
  if (!tokenJson.access_token) throw new HttpError(500, "fcm_auth_failed");
  accessCache = { token: tokenJson.access_token, exp: nowMs + 50 * 60 * 1000 };
  return tokenJson.access_token;
}

async function sendFcm(serviceAccount, fcmMessage) {
  const accessToken = await getAccessToken(serviceAccount);
  const res = await fetch(`https://fcm.googleapis.com/v1/projects/${serviceAccount.project_id}/messages:send`, {
    method: "POST",
    headers: { Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json" },
    body: JSON.stringify({ message: fcmMessage })
  });
  const json = await res.json().catch(() => ({}));
  return { status: res.status, json };
}

// ---------- أدوات ----------
const ID_RE = /^[A-Za-z0-9_-]{1,128}$/;
function clip(v, n) {
  return String(v == null ? "" : v).slice(0, n);
}
function corsHeaders(request) {
  const origin = request.headers.get("Origin");
  const h = {
    "Access-Control-Allow-Methods": "POST, OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type, Authorization, X-App-Secret",
    Vary: "Origin"
  };
  if (origin && origin === ALLOWED_ORIGIN) h["Access-Control-Allow-Origin"] = origin;
  return h;
}
function json(request, status, obj) {
  return new Response(JSON.stringify(obj), {
    status,
    headers: { "Content-Type": "application/json", ...corsHeaders(request) }
  });
}

// حد بسيط (best effort) لعدد الإشعارات فالدقيقة لكل مستعمل
const rate = new Map();
function rateLimited(uid) {
  const now = Date.now();
  const arr = (rate.get(uid) || []).filter((t) => now - t < 60000);
  if (arr.length >= 30) {
    rate.set(uid, arr);
    return true;
  }
  arr.push(now);
  rate.set(uid, arr);
  return false;
}

// ---------- الأنواع ----------
async function handleChat(body, caller, idToken, sa) {
  const { threadId, title, message, messageId } = body;
  if (!ID_RE.test(String(threadId || ""))) throw new HttpError(400, "bad_threadId");
  const thread = await fsGet(sa.project_id, `chat_threads/${threadId}`, idToken);
  if (!thread) throw new HttpError(403, "forbidden");
  const uids = listField(thread, "participantUids");
  if (!uids.includes(caller.uid)) throw new HttpError(403, "forbidden");
  const other = uids.find((u) => u !== caller.uid);
  if (!other) throw new HttpError(400, "no_recipient");

  const userDoc = await fsGet(sa.project_id, `users/${other}`, idToken);
  const token = strField(userDoc, "fcmToken");
  if (!token) return { status: 200, json: { sent: false, reason: "no_token" } };

  const t = clip(title, 100);
  const data = { title: t, body: clip(message, 300), kind: "chat", threadId, otherUid: caller.uid, otherName: t };
  if (messageId && ID_RE.test(String(messageId))) data.messageId = String(messageId);
  return sendFcm(sa, { token, data, android: { priority: "high" } });
}

async function handleFriendRequest(body, caller, idToken, sa) {
  const { toUid, title, message } = body;
  if (!ID_RE.test(String(toUid || ""))) throw new HttpError(400, "bad_toUid");
  // لازم يكون كاين طلب الصداقة ديال هاد المستعمل عند الطرف الآخر (القواعد كتسمح للمرسل يقراه)
  const req = await fsGet(sa.project_id, `users/${toUid}/incoming_requests/${caller.uid}`, idToken);
  if (!req) throw new HttpError(403, "forbidden");
  const userDoc = await fsGet(sa.project_id, `users/${toUid}`, idToken);
  const token = strField(userDoc, "fcmToken");
  if (!token) return { status: 200, json: { sent: false, reason: "no_token" } };
  return sendFcm(sa, { token, notification: { title: clip(title, 100), body: clip(message, 300) } });
}

async function handleTopic(body, caller, adminUid, sa) {
  if (caller.uid !== adminUid) throw new HttpError(403, "admin_only");
  const { topic, title, message, data } = body;
  if (!topic || !title) throw new HttpError(400, "missing_fields");
  const fcm = { topic };
  if (data && typeof data === "object") {
    fcm.data = { title: clip(title, 100), body: clip(message, 500), ...data };
    fcm.android = { priority: "high" };
  } else {
    fcm.notification = { title: clip(title, 100), body: clip(message, 500) };
  }
  return sendFcm(sa, fcm);
}

// الطريقة القديمة (X-App-Secret): كتبقى مؤقتا للنسخ القديمة من التطبيق
async function handleLegacy(body, sa) {
  const { token, topic, title, message, data } = body;
  if (!token && !topic) throw new HttpError(400, "missing_token_or_topic");
  if (!title) throw new HttpError(400, "missing_title");
  const fcm = {};
  if (token) fcm.token = token;
  if (topic) fcm.topic = topic;
  if (data) {
    fcm.data = { title, body: message || "", ...data };
    fcm.android = { priority: "high" };
  } else {
    fcm.notification = { title, body: message || "" };
  }
  return sendFcm(sa, fcm);
}

export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: corsHeaders(request) });
    }
    if (request.method !== "POST") return json(request, 405, { error: "method_not_allowed" });

    let body;
    try {
      body = await request.json();
    } catch (e) {
      return json(request, 400, { error: "invalid_json" });
    }

    let sa;
    try {
      sa = JSON.parse(env.FIREBASE_SERVICE_ACCOUNT);
    } catch (e) {
      return json(request, 500, { error: "bad_service_account_config" });
    }

    try {
      const auth = request.headers.get("Authorization") || "";
      let result;

      if (auth.startsWith("Bearer ")) {
        const idToken = auth.slice(7).trim();
        const caller = await verifyIdToken(idToken, sa.project_id);
        const adminUid = env.ADMIN_UID || ADMIN_UID;
        if (caller.uid !== adminUid && rateLimited(caller.uid)) return json(request, 429, { error: "rate_limited" });

        if (body.kind === "chat") result = await handleChat(body, caller, idToken, sa);
        else if (body.kind === "friend_request") result = await handleFriendRequest(body, caller, idToken, sa);
        else if (body.topic) result = await handleTopic(body, caller, adminUid, sa);
        else throw new HttpError(400, "unknown_kind");
      } else {
        const secret = request.headers.get("X-App-Secret");
        const legacyOn = env.APP_SECRET && env.LEGACY_OFF !== "true";
        if (!legacyOn || !secret || secret !== env.APP_SECRET) return json(request, 401, { error: "unauthorized" });
        result = await handleLegacy(body, sa);
      }
      return json(request, result.status, result.json);
    } catch (e) {
      if (e instanceof HttpError) return json(request, e.status, { error: e.message });
      return json(request, 500, { error: "server_error" });
    }
  }
};
