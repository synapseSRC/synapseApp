// ============================================================================
// Synapse Social — turn-credentials Edge Function
// ============================================================================
//
// WHAT THIS IS
//   The only place TURN credentials for a call are produced. An authenticated
//   client POSTs here (client.functions.invoke, same as the passkey functions)
//   and receives a short-lived coturn RESTAPI credential pair plus the STUN/TURN
//   URLs to put in its ICE configuration.
//
// WHY IT EXISTS
//   A static TURN username/password cannot live in an APK: the binary is
//   extractable, so a shared permanent credential is effectively public, and
//   anyone with your relay's credentials can burn bandwidth through it or enumerate
//   peers. coturn's `use-auth-secret` mode exists for exactly this: the server and
//   this function hold a shared HMAC secret, and each credential is
//       username = "<unix expiry>:<user id>"
//       credential = base64(HMAC-SHA1(secret, username))
//   which coturn re-derives and checks, and which stops working at `expiry`
//   without any revocation call. The user id is embedded so coturn logs (and your
//   own quota accounting) attribute relay usage to the account that caused it.
//
// SECRETS
//   TURN_SHARED_SECRET is read from Supabase secrets at runtime
//       supabase secrets set TURN_SHARED_SECRET=...
//   It is never in this repository, never in a client, and never in a response.
//   If it is missing this function refuses with 503 TURN_NOT_CONFIGURED instead
//   of handing back something that looks like a credential: the client is built
//   to fail a call visibly rather than silently degrade to STUN-only, because a
//   call that half-connects behind NAT is worse than one that says it failed.
//
//   CLOUDFLARE_TURN_KEY_ID/CLOUDFLARE_TURN_API_TOKEN select Cloudflare's managed
//   TURN service instead of a self-hosted coturn: this function asks Cloudflare
//   for a short-lived ICE server list and passes it through. Used only when
//   TURN_SHARED_SECRET is absent, so a project switches providers by secrets.
//
//   TURN_USERNAME/TURN_PASSWORD are a fallback for a development TURN server with
//   fixed accounts (e.g. node-turn on a dev machine), which has no REST API secret
//   to derive from. They are only used when neither of the above is configured;
//   they are still Supabase secrets, never compiled into the app.
//
// AUTH
//   Supabase verifies the JWT signature for functions by default (deploy without
//   --no-verify-jwt). This code then re-validates the token by resolving the
//   session server-side via auth.getUser(), which additionally rejects a token
//   that was revoked by sign-out, and yields the uid used in the username. The
//   JWT secret is not needed here on purpose — embedding it would just move the
//   same secret problem into a deployed artifact.
//
// SETUP (once, outside this repository)
//   1. coturn reachable from both devices: UDP 3478 plus the relay port range,
//      and 443/tcp for TURN-TLS if restrictive networks block UDP outbound.
//      In etc/turnserver.conf:
//          use-auth-secret
//          static-auth-secret=<same value as TURN_SHARED_SECRET>
//          realm=turn.yourdomain.tld
//  2. supabase secrets set TURN_SHARED_SECRET=<static-auth-secret>
//     supabase secrets set TURN_URLS="stun:stun.l.google.com:19302,turn:turn.yourdomain.tld:3478?transport=udp,turns:turn.yourdomain.tld:443?transport=tcp"
//     supabase secrets set TURN_TTL_SECONDS=3600            # optional, 300..86400
//  3. supabase functions deploy turn-credentials
// ============================================================================

/// <reference types="https://esm.sh/@supabase/functions-js/src/edge-runtime.d.ts" />

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const CORS_HEADERS: Record<string, string> = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

const DEFAULT_TTL_SECONDS = 3600;
const MIN_TTL_SECONDS = 300;
const MAX_TTL_SECONDS = 86_400;

/** Default ice server list when TURN_URLS is not configured; always includes at least STUN. */
const DEFAULT_URLS = [
  "stun:stun.l.google.com:19302",
  "stun:stun1.l.google.com:19302",
];

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...CORS_HEADERS, "Content-Type": "application/json" },
  });
}

function error(message: string, status: number): Response {
  // `error` is the key the client already reads from failing functions
  // (see PasskeyRepositoryImpl), so a rejection surfaces with its real reason.
  return json({ error: message, code: message.split(" ")[0] }, status);
}

/**
 * RESTAPI-style credential. coturn recomputes base64(HMAC-SHA1(secret, username))
 * and accepts the credential while `now < first username component`.
 */
async function buildCredential(
  secret: string,
  userId: string,
  ttlSeconds: number,
): Promise<{ username: string; credential: string; expiresAtUnix: number }> {
  const expiresAtUnix = Math.floor(Date.now() / 1000) + ttlSeconds;
  const username = `${expiresAtUnix}:${userId}`;

  const key = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(secret),
    { name: "HMAC", hash: "SHA-1" },
    false,
    ["sign"],
  );
  const signature = await crypto.subtle.sign(
    "HMAC",
    key,
    new TextEncoder().encode(username),
  );

  let binary = "";
  const bytes = new Uint8Array(signature);
  for (let i = 0; i < bytes.length; i++) binary += String.fromCharCode(bytes[i]);
  const credential = btoa(binary);

  return { username, credential, expiresAtUnix };
}

/**
 * Asks Cloudflare Realtime TURN for a short-lived ICE server list. The TURN key
 * token stays in Supabase secrets; the app only receives the minted credential,
 * which Cloudflare expires after `ttlSeconds`.
 *
 * `iceServers` comes back as an array: a STUN-only entry plus a TURN entry that
 * carries the username/credential. Both are passed through as-is.
 */
async function cloudflareIceServers(
  keyId: string,
  apiToken: string,
  ttlSeconds: number,
): Promise<Array<{ urls: string[]; username?: string; credential?: string }>> {
  const response = await fetch(
    `https://rtc.live.cloudflare.com/v1/turn/keys/${encodeURIComponent(keyId)}/credentials/generate-ice-servers`,
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${apiToken}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ ttl: ttlSeconds }),
    },
  );
  if (!response.ok) {
    throw new Error(`Cloudflare TURN returned HTTP ${response.status}`);
  }
  const body = await response.json() as {
    iceServers?: Array<{ urls?: string[] | string; username?: string; credential?: string }>;
  };
  const entries = (body.iceServers ?? [])
    .map((entry) => ({
      urls: Array.isArray(entry.urls) ? entry.urls : entry.urls ? [entry.urls] : [],
      username: entry.username,
      credential: entry.credential,
    }))
    .filter((entry) => entry.urls.length > 0);
  if (entries.length === 0) {
    throw new Error("Cloudflare TURN response did not contain iceServers");
  }
  return entries;
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: CORS_HEADERS });
  }
  if (req.method !== "POST") {
    return error("METHOD_NOT_ALLOWED: POST required", 405);
  }

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  if (!supabaseUrl) return error("SERVER_MISCONFIGURED: SUPABASE_URL missing", 500);

  const authorization = req.headers.get("Authorization");
  if (!authorization) return error("UNAUTHENTICATED: missing Authorization header", 401);

  // Anon key, not the service role key: this function resolves the caller's own
  // session and touches no other user's rows, so it never needs bypass-RLS
  // credentials. SUPABASE_ANON_KEY is not injected automatically — set it once
  // (`supabase secrets set SUPABASE_ANON_KEY=<publishable/anon key>`), or this
  // 500 tells you so instead of failing the call opaquely.
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY") ?? Deno.env.get("SUPABASE_PUBLISHABLE_KEY");
  if (!anonKey) return error("SERVER_MISCONFIGURED: SUPABASE_ANON_KEY not set", 500);

  let userId: string;
  try {
    const supabase = createClient(supabaseUrl, anonKey, {
      auth: { persistSession: false },
      global: { headers: { Authorization: authorization } },
    });
    const { data, error: userError } = await supabase.auth.getUser();
    if (userError || !data?.user) return error("UNAUTHENTICATED: invalid or revoked session", 401);
    userId = data.user.id;
  } catch (cause) {
    console.error("turn-credentials: session resolution failed", cause);
    return error("UNAUTHENTICATED: session could not be verified", 401);
  }

  const secret = Deno.env.get("TURN_SHARED_SECRET");
  const cloudflareKeyId = Deno.env.get("CLOUDFLARE_TURN_KEY_ID");
  const cloudflareToken = Deno.env.get("CLOUDFLARE_TURN_API_TOKEN");
  const staticUsername = Deno.env.get("TURN_USERNAME");
  const staticPassword = Deno.env.get("TURN_PASSWORD");
  const hasCloudflare = !!cloudflareKeyId && !!cloudflareToken;
  const hasStaticCredentials = !!staticUsername && !!staticPassword;
  if (!secret && !hasCloudflare && !hasStaticCredentials) {
    console.error("turn-credentials: no TURN credentials configured");
    return error(
      "TURN_NOT_CONFIGURED: set TURN_SHARED_SECRET, CLOUDFLARE_TURN_KEY_ID/CLOUDFLARE_TURN_API_TOKEN, or TURN_USERNAME/TURN_PASSWORD",
      503,
    );
  }

  const rawTtl = Number(Deno.env.get("TURN_TTL_SECONDS") ?? DEFAULT_TTL_SECONDS);
  const ttlSeconds = Number.isFinite(rawTtl)
    ? Math.min(MAX_TTL_SECONDS, Math.max(MIN_TTL_SECONDS, Math.floor(rawTtl)))
    : DEFAULT_TTL_SECONDS;

  let iceServers: Array<{ urls: string[]; username?: string; credential?: string }>;
  let expiresAtMillis = 0;
  let ttlOut = 0;
  let turnAvailable = false;

  if (!secret && hasCloudflare) {
    try {
      const ice = await cloudflareIceServers(cloudflareKeyId!, cloudflareToken!, ttlSeconds);
      turnAvailable = ice.some((server) => server.urls.some((url) => url.startsWith("turn:")));
      if (!turnAvailable) {
        throw new Error("Cloudflare TURN response contained no turn: URLs");
      }
      iceServers = ice;
      expiresAtMillis = Date.now() + ttlSeconds * 1000;
      ttlOut = ttlSeconds;
    } catch (cause) {
      console.error("turn-credentials: Cloudflare TURN request failed", cause);
      return error("TURN_UNAVAILABLE: Cloudflare credential request failed", 503);
    }
  } else {
    const urls = (Deno.env.get("TURN_URLS") ?? DEFAULT_URLS.join(","))
      .split(",")
      .map((entry) => entry.trim())
      .filter((entry) => entry.length > 0);

    const turnUrls = urls.filter((url) => url.startsWith("turn:"));
    const relay = urls.filter((url) => !url.startsWith("turn:"));

    // Report the configuration honestly rather than pretending: the client reads
    // `turn_available` and refuses to place a call it knows cannot survive a
    // restrictive NAT, instead of half-connecting and dropping.
    turnAvailable = turnUrls.length > 0;

    if (turnAvailable) {
      if (secret) {
        const { username, credential, expiresAtUnix } = await buildCredential(secret, userId, ttlSeconds);
        expiresAtMillis = expiresAtUnix * 1000;
        ttlOut = ttlSeconds;
        iceServers = [
          ...(relay.length > 0 ? [{ urls: relay }] : []),
          ...turnUrls.map((url) => ({ urls: [url], username, credential })),
        ];
      } else {
        // Fixed development account: it has no expiry, so expires_at/ttl_seconds
        // stay 0 and the client re-requests instead of caching a lifetime.
        iceServers = [
          ...(relay.length > 0 ? [{ urls: relay }] : []),
          ...turnUrls.map((url) => ({
            urls: [url],
            username: staticUsername!,
            credential: staticPassword!,
          })),
        ];
      }
    } else {
      iceServers = relay.length > 0 ? [{ urls: relay }] : [];
    }
  }

  return json({
    ice_servers: iceServers,
    expires_at: expiresAtMillis,
    ttl_seconds: ttlOut,
    turn_available: turnAvailable,
  });
});
