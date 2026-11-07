import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient, type SupabaseClient } from "https://esm.sh/@supabase/supabase-js@2.119.0"
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

// Read-only projections of the database columns used by this dispatcher.
// https://supabase.com/docs/reference/javascript/typescript-support
// Supplying a concrete schema avoids ReturnType of a generic factory becoming unknown/never.
type ReadTable<Row extends JsonRecord> = {
  Row: Row
  Insert: never
  Update: never
  Relationships: []
}
type PushDatabase = {
  public: {
    Tables: {
      notification_preferences: ReadTable<{
        user_id: string; enabled: boolean | null; settings: unknown; quiet_hours: unknown
        do_not_disturb: boolean | null; dnd_until: string | null
      }>
      user_presence: ReadTable<{ user_id: string; is_online: boolean; last_seen: string | null }>
      users: ReadTable<{ uid: string | null; display_name: string | null; username: string | null; avatar: string | null }>
      posts: ReadTable<{ id: string; post_text: string | null; is_deleted: boolean | null }>
      messages: ReadTable<{
        id: string; chat_id: string; sender_id: string; is_deleted: boolean | null
        content_state: string | null; expires_at: string | null; message_type: string | null
      }>
      chats: ReadTable<{ id: string; is_group: boolean | null }>
      chat_participants: ReadTable<{ chat_id: string; user_id: string; is_muted: boolean | null }>
      notifications: ReadTable<{
        id: string; recipient_id: string; sender_id: string | null; type: string
        title: unknown; body: unknown; data: unknown; expires_at: string | null
      }>
    }
    Views: Record<string, never>
    Functions: Record<string, never>
  }
}
type AdminClient = SupabaseClient<PushDatabase>

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
  admin: AdminClient,
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
  admin: AdminClient,
  recipientId: string,
): Promise<boolean> {
  const { data: presence, error } = await admin
    .from("user_presence")
    .select("is_online, last_seen")
    .eq("user_id", recipientId)
    .maybeSingle()

  if (error) throw new Error("Could not load recipient presence: " + error.message)
  if (!presence) return false

  const lastSeen = typeof presence.last_seen === "string" ? Date.parse(presence.last_seen) : NaN
  if (!Number.isFinite(lastSeen)) return presence.is_online === true
  return presence.is_online === true && (Date.now() - lastSeen) / 1000 < ACTIVE_WINDOW_SECONDS
}

// Android decodes large_icon through BitmapFactory, which cannot rasterize SVG, so a Dicebear SVG
// URL silently drops the sender's picture from the push. Dicebear serves the same image as PNG at
// the same path with a different extension; every other URL is passed through untouched and is
// carried in data.avatarUrl for the client-side extension service to render instead.
function toRasterAvatarUrl(avatar: string): string {
  return avatar.replace("api.dicebear.com/9.x/initials/svg", "api.dicebear.com/9.x/initials/png")
}

async function loadSenderProfile(
  admin: AdminClient,
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
    avatar: sender?.avatar ? toRasterAvatarUrl(sender.avatar) : null,
  }
}

function firstString(record: JsonRecord, keys: string[]): string | null {
  for (const key of keys) {
    const value = record[key]
    if (typeof value === "string" && value.trim()) return value.trim()
  }
  return null
}

function concisePreview(value: unknown, maxLength = 140): string | null {
  if (typeof value !== "string") return null
  const normalized = value.replace(/\s+/g, " ").trim()
  if (!normalized) return null
  const characters = Array.from(normalized)
  if (characters.length <= maxLength) return normalized
  return characters.slice(0, maxLength - 1).join("").trimEnd() + "\u2026"
}

function getPreviewPostId(type: string, eventData: unknown): string | null {
  if (!isRecord(eventData)) return null
  const normalizedType = type.toUpperCase()
  const targetType = (firstString(eventData, ["targetType", "target_type"]) ?? "").toUpperCase()
  const targetId = firstString(eventData, ["target_id", "targetId"])
  const postId = firstString(eventData, ["postId", "post_id"])
  const commentId = firstString(eventData, ["commentId", "comment_id"])

  if (["NEW_REPLY", "NEW_COMMENT", "NEW_LIKE_COMMENT", "REACTION", "NEW_REACTION"].includes(normalizedType) ||
      targetType === "POST_COMMENT") {
    return commentId ?? targetId
  }
  if (["NEW_POST", "NEW_LIKE_POST", "SHARE", "MENTION"].includes(normalizedType)) {
    return postId ?? targetId
  }
  return postId ?? targetId
}

async function loadPostPreview(
  admin: AdminClient,
  postId: string,
): Promise<string | null> {
  const { data: post, error } = await admin
    .from("posts")
    .select("post_text, is_deleted")
    .eq("id", postId)
    .maybeSingle()

  if (error) {
    console.warn("Could not load notification content preview; using fallback copy")
    return null
  }
  if (!post || post.is_deleted === true) return null
  return concisePreview(post.post_text)
}

function buildNotificationCopy(
  type: string,
  senderName: string,
  fallbackTitle: string,
  fallbackBody: string,
  preview: string | null,
  eventData: unknown,
): { title: string; body: string } {
  const name = senderName.trim() || "Someone"
  const normalizedType = type.toUpperCase()
  const targetType = isRecord(eventData)
    ? (firstString(eventData, ["targetType", "target_type"]) ?? "").toUpperCase()
    : ""

  switch (normalizedType) {
    case "NEW_MESSAGE":
      return { title: name, body: "Sent you a message." }
    case "NEW_REPLY":
      return { title: name + " replied to your comment", body: preview ?? "Tap to view the conversation." }
    case "NEW_COMMENT":
      return { title: name + " commented on your post", body: preview ?? "Tap to view the comment." }
    case "NEW_LIKE_COMMENT":
      return { title: name + " liked your comment", body: preview ?? "Tap to view the comment." }
    case "NEW_LIKE_POST":
      return { title: name + " liked your post", body: preview ?? "Tap to view the post." }
    case "NEW_FOLLOWER":
    case "FOLLOW":
      return { title: name + " started following you", body: "Tap to view their profile." }
    case "FOLLOW_REQUEST":
      return { title: name + " requested to follow you", body: "Tap to review the request." }
    case "NEW_POST":
      return { title: name + " shared a new post", body: preview ?? "Tap to view the post." }
    case "SHARE":
      return { title: name + " shared a post", body: preview ?? "Tap to view the post." }
    case "MENTION":
      return { title: name + " mentioned you", body: preview ?? "Tap to view the post." }
    case "REACTION":
    case "NEW_REACTION": {
      const target = targetType === "POST_COMMENT" ? "comment" : "post"
      return { title: name + " reacted to your " + target, body: preview ?? "Tap to view the reaction." }
    }
    default:
      return {
        title: fallbackTitle.trim() || name,
        body: fallbackBody.trim() || "You have a new notification.",
      }
  }
}

function buildNavigationPayload(type: string, eventData: JsonRecord, senderId: string | null): JsonRecord {
  const normalizedType = type.toUpperCase()
  const targetType = (firstString(eventData, ["targetType", "target_type"]) ?? "").toUpperCase()
  const targetId = firstString(eventData, ["target_id", "targetId"])
  const postId = firstString(eventData, ["postId", "post_id"])
  const commentId = firstString(eventData, ["commentId", "comment_id"])
  const chatId = firstString(eventData, ["chat_id", "chatId"])
  const messageId = firstString(eventData, ["message_id", "messageId"])
  const userId = firstString(eventData, ["userId", "user_id", "followerId", "follower_id"]) ?? senderId

  if (normalizedType === "NEW_MESSAGE") {
    return chatId
      ? { target: "CHAT", chatId, ...(messageId ? { messageId } : {}), ...(senderId ? { userId: senderId } : {}) }
      : { target: "NOTIFICATIONS" }
  }
  if (normalizedType === "NEW_FOLLOWER" || normalizedType === "FOLLOW") {
    return userId ? { target: "PROFILE", userId } : { target: "NOTIFICATIONS" }
  }
  if (normalizedType === "FOLLOW_REQUEST") {
    return userId ? { target: "FOLLOW_REQUESTS", userId } : { target: "NOTIFICATIONS" }
  }
  if (["NEW_COMMENT", "NEW_REPLY", "NEW_LIKE_COMMENT", "COMMENT", "REPLY", "LIKE_COMMENT"].includes(normalizedType)) {
    const resolvedPostId = postId ?? (targetType === "POST" ? targetId : null)
    const resolvedCommentId = commentId ?? (targetType === "POST_COMMENT" ? targetId : null)
    if (resolvedPostId) {
      return { target: "POST_COMMENT", postId: resolvedPostId, ...(resolvedCommentId ? { commentId: resolvedCommentId } : {}) }
    }
    return { target: "NOTIFICATIONS" }
  }
  if (normalizedType === "MENTION" && targetType === "POST_COMMENT") {
    const resolvedPostId = postId
    const resolvedCommentId = commentId ?? targetId
    if (resolvedPostId) {
      return { target: "POST_COMMENT", postId: resolvedPostId, ...(resolvedCommentId ? { commentId: resolvedCommentId } : {}) }
    }
    return { target: "NOTIFICATIONS" }
  }
  if (["NEW_POST", "NEW_LIKE_POST", "SHARE", "POST", "LIKE_POST"].includes(normalizedType) || targetType === "POST") {
    const resolvedPostId = postId ?? targetId
    return resolvedPostId ? { target: "POST", postId: resolvedPostId } : { target: "NOTIFICATIONS" }
  }
  if (targetType === "PROFILE" || targetType === "USER") {
    return userId ? { target: "PROFILE", userId } : { target: "NOTIFICATIONS" }
  }
  if (targetType === "POST_COMMENT" && postId) {
    const resolvedCommentId = commentId ?? targetId
    return { target: "POST_COMMENT", postId, ...(resolvedCommentId ? { commentId: resolvedCommentId } : {}) }
  }
  if (postId) return { target: "POST", postId }
  if (userId && ["PROFILE", "USER"].includes(targetType)) return { target: "PROFILE", userId }
  return { target: "NOTIFICATIONS" }
}

// Stable across webhook retries and silent/visible policy changes. Provider idempotency prevents
// a partial group fan-out retry from notifying already-processed participants again.
async function messagePushKey(messageId: string, recipientId: string): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(`message-push:v1:${messageId}:${recipientId}`))
  const bytes = new Uint8Array(digest).slice(0, 16)
  bytes[6] = (bytes[6] & 0x0f) | 0x40
  bytes[8] = (bytes[8] & 0x3f) | 0x80
  const hex = Array.from(bytes, (byte) => byte.toString(16).padStart(2, "0")).join("")
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`
}

async function sendOneSignalPush(
  recipientId: string,
  senderId: string | null,
  type: string,
  title: string,
  body: string,
  eventData: unknown,
  avatar: string | null,
  notificationId: string | null,
  silent = false,
  idempotencyKey?: string,
): Promise<JsonRecord> {
  if (!ONESIGNAL_APP_ID || !ONESIGNAL_REST_API_KEY) throw new Error("Missing OneSignal configuration")
  const rawData: JsonRecord = isRecord(eventData) ? eventData : {}
  const data: JsonRecord = {
    ...rawData,
    type: silent ? "MESSAGE_DELIVERY_SYNC" : type,
    ...(silent ? {} : { navigation: buildNavigationPayload(type, rawData, senderId) }),
  }
  if (!silent && senderId) data.senderId = senderId
  if (notificationId) data.notificationId = notificationId
  if (!silent && avatar) data.avatarUrl = avatar

  const payload: JsonRecord = {
    app_id: ONESIGNAL_APP_ID,
    data,
    include_aliases: { external_id: [recipientId] },
    target_channel: "push",
    // https://documentation.onesignal.com/docs/en/data-notifications
    // Silent hints respect alert preferences; they are normal-priority, best-effort background work.
    ...(silent ? { content_available: true, priority: 5 } : { headings: { en: title }, contents: { en: body } }),
  }
  if (!silent && avatar) payload.large_icon = avatar
  if (idempotencyKey) payload.idempotency_key = idempotencyKey

  const response = await fetch("https://api.onesignal.com/notifications", {
    method: "POST",
    headers: { "Content-Type": "application/json", "Authorization": "Key " + ONESIGNAL_REST_API_KEY },
    body: JSON.stringify(payload),
    signal: AbortSignal.timeout(15_000),
  })
  const responseText = await response.text()
  let result: unknown
  try {
    result = JSON.parse(responseText)
  } catch {
    result = { response: responseText.slice(0, 500) }
  }
  if (!response.ok) {
    console.error("OneSignal rejected push notification", { status: response.status })
    throw new Error("OneSignal returned HTTP " + response.status)
  }
  // Acceptance/target count is NOT evidence that a device has received a message.
  return { accepted: true, result: isRecord(result) ? result : {} }
}

async function dispatchMessage(
  admin: AdminClient,
  messageId: string,
  recipientId: string | null,
): Promise<Response> {
  const { data: message, error } = await admin.from("messages")
    .select("id, chat_id, sender_id, is_deleted, content_state, expires_at, message_type")
    .eq("id", messageId).maybeSingle()
  if (error) throw new Error("Could not load message event: " + error.message)
  if (!message) return json({ skipped: true, reason: "message_not_found" })
  if (message.is_deleted === true || ["deleted", "expired"].includes(String(message.content_state ?? "").toLowerCase()) ||
      String(message.message_type).toLowerCase() === "call") {
    return json({ skipped: true, reason: "message_not_active" })
  }
  if (message.expires_at && Date.parse(message.expires_at) <= Date.now()) {
    return json({ skipped: true, reason: "message_expired" })
  }
  const { data: chat, error: chatError } = await admin.from("chats").select("is_group")
    .eq("id", message.chat_id).maybeSingle()
  if (chatError) throw new Error("Could not load chat: " + chatError.message)
  if (!chat || typeof message.sender_id !== "string") return json({ skipped: true, reason: "chat_not_found" })

  const sender = await loadSenderProfile(admin, message.sender_id)
  let dispatched = 0
  let failures = 0
  let afterUserId: string | null = null
  while (true) {
    let query = admin.from("chat_participants").select("user_id, is_muted")
      .eq("chat_id", message.chat_id).neq("user_id", message.sender_id).order("user_id").limit(100)
    // New trigger events target one participant, so a large group does not have to
    // finish all provider calls within a single webhook's execution budget.
    // Membership is rechecked here; the payload alone never authorizes a recipient.
    if (recipientId !== null) query = query.eq("user_id", recipientId)
    if (afterUserId !== null) query = query.gt("user_id", afterUserId)
    const { data: participants, error: participantsError } = await query
    if (participantsError) throw new Error("Could not load chat participants: " + participantsError.message)
    if (!participants?.length) break

    for (let offset = 0; offset < participants.length; offset += 8) {
      const results = await Promise.allSettled(participants.slice(offset, offset + 8).map(async (participant) => {
        if (typeof participant.user_id !== "string" || !participant.user_id) throw new Error("Invalid participant")
        const silent = participant.is_muted === true ||
          await shouldSuppressForPreferences(admin, participant.user_id, "NEW_MESSAGE") ||
          await recipientIsActive(admin, participant.user_id)
        await sendOneSignalPush(
          participant.user_id, message.sender_id, "NEW_MESSAGE", sender.name,
          chat.is_group ? "Sent a message to your group." : "Sent you a message.",
          { chat_id: message.chat_id, message_id: message.id }, sender.avatar, null,
          silent, await messagePushKey(message.id, participant.user_id),
        )
      }))
      dispatched += results.filter((result) => result.status === "fulfilled").length
      failures += results.filter((result) => result.status === "rejected").length
    }
    if (recipientId !== null) break
    const nextUserId = participants[participants.length - 1].user_id
    if (typeof nextUserId !== "string" || nextUserId === afterUserId) throw new Error("Invalid participant cursor")
    afterUserId = nextUserId
  }
  // Retryable by the caller; pg_net itself does not automatically retry HTTP failures.
  // Replaying an event reuses the provider idempotency key for successful recipients.
  if (failures) return json({ error: "Message push fan-out incomplete", dispatched, failures }, 503)
  return json({ sent: true, type: "NEW_MESSAGE", dispatched })
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
    if (!expectedSecret) return json({ error: "Service configuration is incomplete" }, 503)
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
    if (Boolean(notificationId) === Boolean(messageId)) return json({ error: "Supply exactly one notification_id or message_id" }, 400)
    const isUuid = (value: string) => /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(value)
    if ((notificationId && !isUuid(notificationId)) || (messageId && !isUuid(messageId))) return json({ error: "Invalid event ID" }, 400)
    const recipientIdHint = body.recipient_id
    if (recipientIdHint !== undefined &&
        (!messageId || typeof recipientIdHint !== "string" || !isUuid(recipientIdHint))) {
      return json({ error: "Invalid message recipient" }, 400)
    }

    const admin = createClient<PushDatabase>(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY, {
      auth: { persistSession: false, autoRefreshToken: false },
    })
    if (messageId) return await dispatchMessage(admin, messageId, typeof recipientIdHint === "string" ? recipientIdHint : null)

    const { data: notification, error } = await admin.from("notifications")
      .select("id, recipient_id, sender_id, type, title, body, data, expires_at")
      .eq("id", notificationId).maybeSingle()
    if (error) throw new Error("Could not load notification event: " + error.message)
    if (!notification) return json({ skipped: true, reason: "notification_not_found" })
    if (notification.expires_at && Date.parse(notification.expires_at) <= Date.now()) return json({ skipped: true, reason: "notification_expired" })

    const recipientId = typeof notification.recipient_id === "string" ? notification.recipient_id : ""
    const senderId = typeof notification.sender_id === "string" ? notification.sender_id : null
    const type = typeof notification.type === "string" ? notification.type : "NOTIFICATION"
    const title = localizedString(notification.title, "Synapse")
    const notificationBody = localizedString(notification.body, "You have a new notification.")
    const eventData = notification.data
    if (!recipientId || recipientId === senderId) return json({ skipped: true, reason: "invalid_recipient" })
    if (await shouldSuppressForPreferences(admin, recipientId, type)) return json({ skipped: true, reason: "recipient_preferences" })
    if (await recipientIsActive(admin, recipientId)) return json({ skipped: true, reason: "recipient_active" })

    const sender = await loadSenderProfile(admin, senderId)
    const previewPostId = getPreviewPostId(type, eventData)
    const contentPreview = previewPostId ? await loadPostPreview(admin, previewPostId) : null
    const copy = buildNotificationCopy(type, sender.name, title, notificationBody, contentPreview, eventData)
    const pushResult = await sendOneSignalPush(recipientId, senderId, type, copy.title, copy.body, eventData, sender.avatar, notificationId)
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
