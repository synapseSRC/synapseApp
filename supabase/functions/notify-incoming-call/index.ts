// ============================================================================
// Synapse Social — notify-incoming-call Edge Function
// ============================================================================
//
// WHAT THIS IS
//   Sends a OneSignal push for a freshly created `call_sessions` row, so a
//   callee whose app is backgrounded or killed still learns about the ring.
//
// WHY IT EXISTS
//   Realtime cannot wake a suspended process. The app already handles an
//   `incoming_call` push as a *hint* (it reconciles against the backend before it
//   rings), but nothing in this repository sent one. This function is that sender.
//
// WHY IT IS DORMANT BY DEFAULT
//   Deploying it does nothing on its own. It only runs when a project owner
//   configures a Database Webhook on `public.call_sessions` (INSERT) that points
//   at it — the SETUP block below lists the exact steps. That ordering is
//   deliberate: a function that pushed on a table it guessed at is worse than no
//   push at all.
//
// WHAT IT TRUSTS
//   Nothing in the payload except an id. The row is re-read with the service
//   role, and the push is only sent while the call is genuinely `ringing` and its
//   server deadline has not passed. A forged payload, a delayed webhook, or a
//   call cancelled during the round trip all collapse to `{ sent: false }`.
//
// SECRETS (all Supabase function secrets; none belong in the app or repo)
//   INCOMING_CALL_WEBHOOK_SECRET  shared secret the Database Webhook must send in
//                                 `x-webhook-secret`. Required: without it this
//                                 function refuses every request.
//   ONESIGNAL_APP_ID              OneSignal app id.
//   ONESIGNAL_REST_API_KEY        OneSignal REST API key (server-side only).
//   ONESIGNAL_API_URL             optional, defaults to https://api.onesignal.com
//   SUPABASE_SERVICE_ROLE_KEY     injected by the platform; used only to re-read
//                                 the call row and the two profiles.
//
// SETUP (once, outside this repository)
//   supabase secrets set INCOMING_CALL_WEBHOOK_SECRET=<long random string>
//   supabase secrets set ONESIGNAL_APP_ID=<app id>
//   supabase secrets set ONESIGNAL_REST_API_KEY=<REST key>
//   supabase functions deploy notify-incoming-call --no-verify-jwt
//   -- then add a Database Webhook: table public.call_sessions, event INSERT,
//      type HTTP, URL <project>.supabase.co/functions/v1/notify-incoming-call,
//      header x-webhook-secret: <same value>.
//   --no-verify-jwt is required because a Database Webhook carries no user JWT;
//   the shared secret above is this function's authentication instead.
// ============================================================================

/// <reference types="https://esm.sh/@supabase/functions-js/src/edge-runtime.d.ts" />

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const CORS_HEADERS: Record<string, string> = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type, x-webhook-secret",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

const DEFAULT_ONESIGNAL_API_URL = "https://api.onesignal.com";
const ANDROID_CHANNEL_ID = "incoming_call_channel";
const IOS_CATEGORY = "INCOMING_CALL";

/** Fallback when the row has no usable deadline; matches the column default. */
const DEFAULT_RING_SECONDS = 45;
const MIN_TTL_SECONDS = 5;
const MAX_TTL_SECONDS = 120;

const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...CORS_HEADERS, "Content-Type": "application/json" },
  });
}

function error(message: string, status: number): Response {
  // `error` is the key the client already reads from failing functions, so a
  // rejection surfaces with its real reason instead of a bare 500.
  return json({ error: message, code: message.split(" ")[0] }, status);
}

/** Length-aware comparison; the webhook secret is the only thing protecting this endpoint. */
function secretMatches(provided: string | null, expected: string): boolean {
  if (!provided) return false;
  const a = new TextEncoder().encode(provided);
  const b = new TextEncoder().encode(expected);
  if (a.length !== b.length) return false;
  let diff = 0;
  for (let i = 0; i < a.length; i++) diff |= a[i] ^ b[i];
  return diff === 0;
}

interface CallRow {
  id: string;
  chat_id: string | null;
  caller_id: string;
  callee_id: string;
  media_type: string;
  status: string;
  ringing_expires_at: string | null;
}

interface ProfileRow {
  uid: string;
  display_name: string | null;
  username: string | null;
  one_signal_player_id: string | null;
}

function nameOf(profile: ProfileRow | undefined): string {
  const display = profile?.display_name?.trim();
  if (display) return display;
  const username = profile?.username?.trim();
  if (username) return username;
  return "Synapse";
}

/** Remaining ring time, clamped so a stale row cannot mint a long-lived notification. */
function ringTtlSeconds(expiresAt: string | null): number {
  const deadline = expiresAt ? Date.parse(expiresAt) : Number.NaN;
  if (!Number.isFinite(deadline)) return DEFAULT_RING_SECONDS;
  const remaining = Math.ceil((deadline - Date.now()) / 1000);
  return Math.min(MAX_TTL_SECONDS, Math.max(MIN_TTL_SECONDS, remaining));
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: CORS_HEADERS });
  }
  if (req.method !== "POST") {
    return error("METHOD_NOT_ALLOWED: POST required", 405);
  }

  // Fail closed on configuration, in the order that matters: an unauthenticated
  // caller must not be able to learn whether OneSignal or the database is set up.
  const webhookSecret = Deno.env.get("INCOMING_CALL_WEBHOOK_SECRET");
  if (!webhookSecret) {
    console.error("notify-incoming-call: INCOMING_CALL_WEBHOOK_SECRET is not configured");
    return error("WEBHOOK_NOT_CONFIGURED: set INCOMING_CALL_WEBHOOK_SECRET and redeploy", 503);
  }
  if (!secretMatches(req.headers.get("x-webhook-secret"), webhookSecret)) {
    return error("UNAUTHORIZED: invalid webhook secret", 401);
  }

  const oneSignalAppId = Deno.env.get("ONESIGNAL_APP_ID");
  const oneSignalRestKey = Deno.env.get("ONESIGNAL_REST_API_KEY");
  if (!oneSignalAppId || !oneSignalRestKey) {
    console.error("notify-incoming-call: OneSignal is not configured");
    return error("ONESIGNAL_NOT_CONFIGURED: set ONESIGNAL_APP_ID and ONESIGNAL_REST_API_KEY", 503);
  }

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!supabaseUrl || !serviceRoleKey) {
    console.error("notify-incoming-call: server Supabase credentials are missing");
    return error("SERVER_MISCONFIGURED: SUPABASE_URL or SUPABASE_SERVICE_ROLE_KEY missing", 500);
  }

  let payload: Record<string, unknown>;
  try {
    payload = await req.json();
  } catch {
    return error("BAD_REQUEST: body is not JSON", 400);
  }

  const table = typeof payload.table === "string" ? payload.table : null;
  if (table !== null && table !== "call_sessions") {
    return error("UNSUPPORTED_TABLE: this webhook only accepts call_sessions", 400);
  }

  // A Database Webhook configured for UPDATE/DELETE would push duplicate rings;
  // a call row is only interesting at INSERT.
  const eventType = typeof payload.type === "string" ? payload.type : null;
  if (eventType !== null && eventType !== "INSERT") {
    return json({ sent: false, reason: "not_an_insert" });
  }

  const record = (payload.record ?? {}) as Record<string, unknown>;
  const rawCallId = record.id ?? payload.call_id ?? payload.callId;
  const callId = typeof rawCallId === "string" ? rawCallId.trim() : "";
  if (!UUID_PATTERN.test(callId)) {
    return error("BAD_REQUEST: a valid call id is required", 400);
  }

  // Service role is created here and nowhere else, and only after a caller has
  // proven it is our webhook. Nothing privileged is ever returned or logged.
  const supabase = createClient(supabaseUrl, serviceRoleKey, {
    auth: { persistSession: false },
  });

  const { data: call, error: callError } = await supabase
    .from("call_sessions")
    .select("id, chat_id, caller_id, callee_id, media_type, status, ringing_expires_at")
    .eq("id", callId)
    .maybeSingle<CallRow>();

  if (callError) {
    console.error("notify-incoming-call: call lookup failed", callError.message);
    return error("SERVER_ERROR: the call could not be read", 500);
  }
  if (!call) return json({ sent: false, reason: "not_found" });
  if (call.status !== "ringing") return json({ sent: false, reason: "not_ringing" });

  const expiresAt = call.ringing_expires_at ? Date.parse(call.ringing_expires_at) : Number.NaN;
  if (Number.isFinite(expiresAt) && expiresAt <= Date.now()) {
    return json({ sent: false, reason: "ring_expired" });
  }

  const { data: profiles, error: profileError } = await supabase
    .from("users")
    .select("uid, display_name, username, one_signal_player_id")
    .in("uid", [call.caller_id, call.callee_id]);

  if (profileError) {
    console.error("notify-incoming-call: profile lookup failed", profileError.message);
    return error("SERVER_ERROR: the call participants could not be read", 500);
  }

  const rows = (profiles ?? []) as ProfileRow[];
  const caller = rows.find((profile) => profile.uid === call.caller_id);
  const callee = rows.find((profile) => profile.uid === call.callee_id);
  const subscriptionId = callee?.one_signal_player_id?.trim();

  if (!subscriptionId) {
    // Nobody to ring. A 200 is right: the webhook did its job, and retrying will
    // not produce a subscription where there is none.
    return json({ sent: false, reason: "callee_has_no_push_subscription" });
  }

  const callerName = nameOf(caller);
  const mediaType = call.media_type === "video" ? "video" : "voice";
  const ttl = ringTtlSeconds(call.ringing_expires_at);
  const baseUrl = (Deno.env.get("ONESIGNAL_API_URL") ?? DEFAULT_ONESIGNAL_API_URL).replace(/\/+$/, "");

  const notification = {
    app_id: oneSignalAppId,
    include_subscription_ids: [subscriptionId],
    headings: { en: callerName },
    // English only: server-side copy has no locale to go on. The app renders its
    // own localized UI once it opens, so this only affects the notification text.
    contents: { en: mediaType === "video" ? "Incoming video call" : "Incoming voice call" },
    data: {
      type: "incoming_call",
      call_id: call.id,
      caller_id: call.caller_id,
      caller_name: callerName,
      chat_id: call.chat_id ?? "",
      media_type: mediaType,
      navigation: {
        target: "CALL",
        callId: call.id,
        chatId: call.chat_id ?? "",
        mediaType,
      },
    },
    priority: 10,
    ttl,
    collapse_id: `call_${call.id}`,
    android_group: "incoming_call",
    thread_id: `call_${call.id}`,
    // The channel the app already created; without this name the notification
    // would land in OneSignal's default channel and lose its ringtone settings.
    existing_android_channel_id: ANDROID_CHANNEL_ID,
    ios_category: IOS_CATEGORY,
  };

  let response: Response;
  try {
    response = await fetch(`${baseUrl}/notifications`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Key ${oneSignalRestKey}`,
      },
      body: JSON.stringify(notification),
    });
  } catch (cause) {
    console.error("notify-incoming-call: OneSignal request failed", cause instanceof Error ? cause.message : "network error");
    return error("ONESIGNAL_UNREACHABLE: the push could not be sent", 502);
  }

  if (!response.ok) {
    // The body can echo identifiers, so only the status is recorded.
    console.error(`notify-incoming-call: OneSignal rejected the push (${response.status})`);
    return json({ sent: false, reason: "onesignal_rejected", status: response.status }, 502);
  }

  return json({ sent: true, recipients: 1 });
});
