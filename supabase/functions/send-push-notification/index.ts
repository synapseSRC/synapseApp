import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"
import postgres from "npm:postgres@3.4.5"

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type, x-notification-webhook-secret",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
}

const ONESIGNAL_APP_ID = Deno.env.get("ONESIGNAL_APP_ID")
const ONESIGNAL_REST_API_KEY = Deno.env.get("ONESIGNAL_REST_API_KEY")
const SUPABASE_URL = Deno.env.get("SUPABASE_URL")
const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")
const SUPABASE_DB_URL = Deno.env.get("SUPABASE_DB_URL")
const ACTIVE_WINDOW_SECONDS = 90
const WEBHOOK_SECRET_NAME = "notification_push_webhook_secret"

type JsonRecord = Record<string, unknown>

function json(body: JsonRecord, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  })
}

function isRecord(value: unknown): value is JsonRecord {
  return Boolean(value && typeof value === "object" && !Array.isArray(value))
}

function constantTimeEqual(a: string, b: string): boolean {
  const aa = new TextEncoder().encode(a)
  const bb = new TextEncoder().encode(b)
  if (aa.length !== bb.length) return false
  let diff = 0
  for (let i = 0; i < aa.length; i++) diff |= aa[i] ^ bb[i]
  return diff === 0
}

function localizedString(value: unknown, fallback: string): string {
  if (typeof value === "string" && value.trim()) return value
  if (isRecord(value) && typeof value.en === "string" && value.en.trim()) return value.en
  return fallback
}

function typePreference(type: string): { section: string; key: string; legacy: string } | null {
  const map: Record<string, { section: string; key: string; legacy: string }> = {
    NEW_MESSAGE: { section: "social", key: "messages", legacy: "messages_enabled" },
    NEW_LIKE_POST: { section: "social", key: "likes", legacy: "likes_enabled" },
    NEW_LIKE_COMMENT: { section: "social", key: "likes", legacy: "likes_enabled" },
    NEW_COMMENT: { section: "social", key: "comments", legacy: "comments_enabled" },
    NEW_REPLY: { section: "social", key: "replies", legacy: "replies_enabled" },
    MENTION: { section: "social", key: "mentions", legacy: "mentions_enabled" },
    NEW_FOLLOWER: { section: "social", key: "follows", legacy: "follows_enabled" },
    FOLLOW_REQUEST: { section: "social", key: "follows", legacy: "follows_enabled" },
    NEW_POST: { section: "content", key: "new_posts", legacy: "new_posts_enabled" },
    SHARE: { section: "content", key: "shares", legacy: "shares_enabled" },
  }
  return map[type] ?? null
}

function minutesFromTime(value: unknown): number | null {
  if (typeof value !== "string") return null
  const match = /^(\d{1,2}):(\d{2})/.exec(value)
  if (!match) return null
  const hours = Number(match[1])
  const minutes = Number(match[2])
  if (hours < 0 || hours > 23 || minutes < 0 || minutes > 59) return null
  return hours * 60 + minutes
}

function isInsideQuietHours(quietHours: unknown, now = new Date()): boolean {
  if (!isRecord(quietHours) || quietHours.enabled !== true) return false
  const timeZone = typeof quietHours.timezone === "string" ? quietHours.timezone : ""
  const start = minutesFromTime(quietHours.start)
  const end = minutesFromTime(quietHours.end)
  // Do not guess the recipient's time zone for older rows that lack one.
  if (!timeZone || start === null || end === null || start === end) return false

  try {
    const parts = new Intl.DateTimeFormat("en-GB", {
      timeZone,
      hour: "2-digit",
      minute: "2-digit",
      hourCycle: "h23",
    }).formatToParts(now)
    const hour = Number(parts.find((part) => part.type === "hour")?.value)
    const minute = Number(parts.find((part) => part.type === "minute")?.value)
    if (!Number.isFinite(hour) || !Number.isFinite(minute)) return false
    const current = hour * 60 + minute
    return start < end
      ? current >= start && current < end
      : current >= start || current < end
  } catch {
    return false
  }
}

async function loadWebhookSecret(sql: ReturnType<typeof postgres>): Promise<string | null> {
  const rows = await sql.unsafe(
    "select decrypted_secret from vault.decrypted_secrets where name = $1 limit 1",
    [WEBHOOK_SECRET_NAME],
  )
  const secret = rows[0]?.decrypted_secret
  return typeof secret === "string" && secret.length > 0 ? secret : null
}

async function shouldSuppressForPreferences(
  admin: ReturnType<typeof createClient>,
  recipientId: string,
  type: string,
): Promise<boolean> {
  const { data: prefs, error } = await admin
    .from("notification_preferences")
    .select("enabled, settings, quiet_hours, do_not_disturb, dnd_until")
    .eq("user_id", recipientId)
    .maybeSingle()

  if (error) throw new Error("Could not load notification preferences: " + error.message)
  if (!prefs) return false
  if (prefs.enabled === false) return true

  if (prefs.do_not_disturb === true) {
    const until = typeof prefs.dnd_until === "string" ? Date.parse(prefs.dnd_until) : NaN
    if (!Number.isFinite(until) || until > Date.now()) return true
  }

  const setting = typePreference(type)
  if (setting) {
    const settings = isRecord(prefs.settings) ? prefs.settings : {}
    const section = isRecord(settings[setting.section]) ? settings[setting.section] as JsonRecord : {}
    if (section[setting.key] === false || settings[setting.legacy] === false) return true
  }

  return isInsideQuietHours(prefs.quiet_hours)
}

async function recipientIsActive(
  admin: ReturnType<typeof createClient>,
  recipientId: string,
): Promise<boolean> {
  const { data: presence, error } = await admin
    .from("user_presence")
    .select("is_online, last_seen")
    .eq("user_id", recipientId)
    .maybeSingle()

  if (error) throw new Error("Could not load recipient presence: " + error.message)
  if (!presence) return false
  if (presence.is_online === true) return true

  const lastSeen = typeof presence.last_seen === "string" ? Date.parse(presence.last_seen) : NaN
  return Number.isFinite(lastSeen) && (Date.now() - lastSeen) / 1000 < ACTIVE_WINDOW_SECONDS
}

async function loadSenderProfile(
  admin: ReturnType<typeof createClient>,
  senderId: string | null,
): Promise<{ name: string; avatar: string | null }> {
  if (!senderId) return { name: "Synapse", avatar: null }

  const { data: sender, error } = await admin
    .from("users")
    .select("display_name, username, avatar")
    .eq("uid", senderId)
    .maybeSingle()

  if (error) throw new Error("Could not load sender profile: " + error.message)
  return {
    name: sender?.display_name || sender?.username || "Synapse",
    avatar: sender?.avatar || null,
  }
}

async function sendOneSignalPush(
  recipientId: string,
  senderId: string | null,
  type: string,
  title: string,
  body: string,
  eventData: unknown,
  avatar: string | null,
): Promise<JsonRecord> {
  if (!ONESIGNAL_APP_ID || !ONESIGNAL_REST_API_KEY) throw new Error("Missing OneSignal configuration")

  const data: JsonRecord = isRecord(eventData) ? eventData : {}
  const payload: JsonRecord = {
    app_id: ONESIGNAL_APP_ID,
    headings: { en: title },
    contents: { en: body },
    data: { ...data, type },
    include_aliases: { external_id: [recipientId] },
    target_channel: "push",
  }
  if (avatar) payload.large_icon = avatar

  console.log("Dispatching push notification", { recipientId, senderId, type })
  const response = await fetch("https://onesignal.com/api/v1/notifications", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "Authorization": "Basic " + ONESIGNAL_REST_API_KEY,
    },
    body: JSON.stringify(payload),
  })

  const responseText = await response.text()
  let result: unknown
  try {
    result = JSON.parse(responseText)
  } catch {
    result = { response: responseText.slice(0, 500) }
  }

  if (!response.ok) {
    console.error("OneSignal rejected push notification", { status: response.status, result })
    throw new Error("OneSignal returned HTTP " + response.status)
  }
  return { accepted: true, result: isRecord(result) ? result : {} }
}

serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders })
  if (req.method !== "POST") return json({ error: "Method not allowed" }, 405)
  if (!SUPABASE_URL || !SUPABASE_SERVICE_ROLE_KEY || !SUPABASE_DB_URL) {
    console.error("Notification push service configuration is incomplete")
    return json({ error: "Service configuration is incomplete" }, 503)
  }

  const suppliedSecret = req.headers.get("x-notification-webhook-secret") ?? ""
  const sql = postgres(SUPABASE_DB_URL, { max: 1, prepare: false })

  try {
    const expectedSecret = await loadWebhookSecret(sql)
    if (!expectedSecret) {
      console.error("Notification webhook secret is not configured in Vault")
      return json({ error: "Service configuration is incomplete" }, 503)
    }
    if (!constantTimeEqual(suppliedSecret, expectedSecret)) return json({ error: "Unauthorized" }, 401)

    let body: JsonRecord
    try {
      const parsed: unknown = await req.json()
      if (!isRecord(parsed)) return json({ error: "Invalid JSON body" }, 400)
      body = parsed
    } catch {
      return json({ error: "Invalid JSON body" }, 400)
    }

    const notificationId = typeof body.notification_id === "string" ? body.notification_id.trim() : ""
    const messageId = typeof body.message_id === "string" ? body.message_id.trim() : ""
    if (Boolean(notificationId) === Boolean(messageId)) {
      return json({ error: "Supply exactly one notification_id or message_id" }, 400)
    }
    const isUuid = (value: string) =>
      /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(value)
    if ((notificationId && !isUuid(notificationId)) || (messageId && !isUuid(messageId))) {
      return json({ error: "Invalid event ID" }, 400)
    }

    const admin = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY, {
      auth: { persistSession: false, autoRefreshToken: false },
    })

    let recipientId = ""
    let senderId: string | null = null
    let type = ""
    let title = ""
    let notificationBody = ""
    let eventData: unknown = {}
    let isMessageEvent = false

    if (notificationId) {
      const { data: notification, error } = await admin
        .from("notifications")
        .select("id, recipient_id, sender_id, type, title, body, data, expires_at")
        .eq("id", notificationId)
        .maybeSingle()

      if (error) throw new Error("Could not load notification event: " + error.message)
      if (!notification) return json({ skipped: true, reason: "notification_not_found" })
      if (notification.expires_at && Date.parse(notification.expires_at) <= Date.now()) {
        return json({ skipped: true, reason: "notification_expired" })
      }

      recipientId = typeof notification.recipient_id === "string" ? notification.recipient_id : ""
      senderId = typeof notification.sender_id === "string" ? notification.sender_id : null
      type = typeof notification.type === "string" ? notification.type : "NOTIFICATION"
      title = localizedString(notification.title, "Synapse")
      notificationBody = localizedString(notification.body, "You have a new notification.")
      eventData = notification.data
    } else {
      const { data: message, error } = await admin
        .from("messages")
        .select("id, chat_id, sender_id, is_deleted, content_state, expires_at")
        .eq("id", messageId)
        .maybeSingle()

      if (error) throw new Error("Could not load message event: " + error.message)
      if (!message) return json({ skipped: true, reason: "message_not_found" })
      if (message.is_deleted === true || ["deleted", "expired"].includes(String(message.content_state ?? "").toLowerCase())) {
        return json({ skipped: true, reason: "message_not_active" })
      }
      if (message.expires_at && Date.parse(message.expires_at) <= Date.now()) {
        return json({ skipped: true, reason: "message_expired" })
      }

      const { data: chat, error: chatError } = await admin
        .from("chats")
        .select("is_group")
        .eq("id", message.chat_id)
        .maybeSingle()

      if (chatError) throw new Error("Could not load chat: " + chatError.message)
      if (!chat || chat.is_group !== false) return json({ skipped: true, reason: "not_a_direct_chat" })

      const { data: participants, error: participantsError } = await admin
        .from("chat_participants")
        .select("user_id, is_muted")
        .eq("chat_id", message.chat_id)

      if (participantsError) throw new Error("Could not load chat participants: " + participantsError.message)
      const recipients = (participants ?? []).filter((participant) => participant.user_id !== message.sender_id)
      if (recipients.length !== 1 || typeof recipients[0].user_id !== "string") {
        return json({ skipped: true, reason: "direct_chat_recipient_unresolved" })
      }
      if (recipients[0].is_muted === true) return json({ skipped: true, reason: "recipient_muted_chat" })

      recipientId = recipients[0].user_id
      senderId = typeof message.sender_id === "string" ? message.sender_id : null
      type = "NEW_MESSAGE"
      notificationBody = "Sent you a message"
      eventData = { chat_id: message.chat_id, message_id: message.id }
      isMessageEvent = true
    }

    if (!recipientId || recipientId === senderId) return json({ skipped: true, reason: "invalid_recipient" })
    if (await shouldSuppressForPreferences(admin, recipientId, type)) {
      return json({ skipped: true, reason: "recipient_preferences" })
    }
    if (await recipientIsActive(admin, recipientId)) return json({ skipped: true, reason: "recipient_active" })

    const sender = await loadSenderProfile(admin, senderId)
    if (isMessageEvent) title = sender.name
    const pushResult = await sendOneSignalPush(
      recipientId,
      senderId,
      type,
      title || sender.name,
      notificationBody,
      eventData,
      sender.avatar,
    )
    return json({ sent: true, type, result: pushResult })
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error)
    console.error("Notification push error:", message)
    return json({ error: "Notification push failed" }, 500)
  } finally {
    try {
      await sql.end({ timeout: 2 })
    } catch {
      console.warn("Could not close notification database connection")
    }
  }
})
