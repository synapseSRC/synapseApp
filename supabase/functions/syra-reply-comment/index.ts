import postgres from "npm:postgres@3.4.5";
import { generateGeminiText } from "../_shared/ai/gemini.ts";

const MODEL_NAME = "gemini-3.1-flash-lite-preview";

const FALLBACK_REPLIES = [
  "I'm here! 👋",
  "I've got you! 🌟",
  "Thanks for mentioning me!",
];

function constantTimeEqual(a: string, b: string): boolean {
  const aa = new TextEncoder().encode(a);
  const bb = new TextEncoder().encode(b);
  if (aa.length !== bb.length) return false;
  let diff = 0;
  for (let i = 0; i < aa.length; i++) diff |= aa[i] ^ bb[i];
  return diff === 0;
}

function intentionallyMentionsUsername(content: string, username: string): boolean {
  const target = username.trim().toLowerCase();
  if (!target) return false;
  const mentions = content.matchAll(/@([a-zA-Z0-9_.-]+)/g);
  for (const match of mentions) {
    if (match[1].toLowerCase() === target) return true;
  }
  return false;
}

function getDb() {
  const databaseUrl = Deno.env.get("SUPABASE_DB_URL");
  if (!databaseUrl) throw new Error("Database connection is not configured");
  return postgres(databaseUrl, { max: 1, prepare: false });
}

async function getWebhookSecret(sql: ReturnType<typeof postgres>): Promise<string> {
  const rows = await sql`
    select decrypted_secret
    from vault.decrypted_secrets
    where name = 'syra_comment_webhook_secret'
    limit 1
  `;
  const secret = rows[0]?.decrypted_secret;
  if (!secret) throw new Error("Comment webhook secret is not configured");
  return secret as string;
}

function getConfiguredAiUid(): string {
  const uid =
    Deno.env.get("SYRA_UID") ||
    Deno.env.get("AI_ACCOUNT_UID") ||
    Deno.env.get("AI_USER_UID") ||
    Deno.env.get("AI_UID");
  if (!uid?.trim()) throw new Error("AI account UID secret is not configured");
  return uid.trim();
}

async function getAiUser(sql: ReturnType<typeof postgres>) {
  const uid = getConfiguredAiUid();
  const rows = await sql`
    select uid, username, display_name
    from public.users
    where uid::text = ${uid}
    limit 1
  `;
  const user = rows[0];
  if (!user?.uid || typeof user.username !== "string" || !user.username.trim()) {
    throw new Error("AI account could not be resolved from the configured UID");
  }
  return {
    uid: String(user.uid),
    username: user.username.trim(),
    displayName: typeof user.display_name === "string" ? user.display_name.trim() : "",
  };
}

export default {
  async fetch(req: Request) {
    if (req.method !== "POST") {
      return Response.json({ error: "Method not allowed" }, { status: 405 });
    }

    const sql = getDb();

    try {
      const expectedSecret = await getWebhookSecret(sql);
      const suppliedSecret = req.headers.get("x-syra-webhook-secret") ?? "";

      if (!constantTimeEqual(suppliedSecret, expectedSecret)) {
        return Response.json({ error: "Unauthorized" }, { status: 401 });
      }

      const aiUser = await getAiUser(sql);
      const payload = await req.json();
      const record = payload?.record ?? payload;

      const postId = String(record?.post_id ?? "");
      const commentId = String(record?.comment_id ?? "");
      const content = String(record?.content ?? "");
      const authorUid = String(record?.author_uid ?? "");

      if (!postId || !commentId || !content || !authorUid) {
        return Response.json({ error: "Invalid webhook payload" }, { status: 400 });
      }

      if (!intentionallyMentionsUsername(content, aiUser.username)) {
        return Response.json({ success: true, ignored: true });
      }

      if (authorUid === aiUser.uid) {
        return Response.json({ success: true, ignored: true, reason: "ai-author" });
      }

      const existingRows = await sql`
        select id, status, syra_reply_post_id
        from public.syra_comment_invocations
        where comment_id = ${commentId}
        limit 1
      `;

      const existing = existingRows[0];

      if (existing?.status === "completed") {
        return Response.json({
          success: true,
          duplicate: true,
          reply_post_id: existing.syra_reply_post_id,
        });
      }

      if (existing?.status === "processing") {
        return Response.json({ success: true, duplicate: true, processing: true }, { status: 202 });
      }

      if (!existing) {
        await sql`
          insert into public.syra_comment_invocations
            (comment_id, root_post_id, author_uid, status)
          values
            (${commentId}, ${postId}, ${authorUid}, 'processing')
          on conflict (comment_id) do nothing
        `;
      } else {
        await sql`
          update public.syra_comment_invocations
          set status = 'processing', error_message = null, completed_at = null
          where comment_id = ${commentId}
        `;
      }

      const postRows = await sql`
        select post_text
        from public.posts
        where id = ${postId}
        limit 1
      `;
      const postContext = postRows[0]?.post_text ?? "";

      let replyText = "";
      try {
        const geminiKey = Deno.env.get("GEMINI_API_KEY");
        if (!geminiKey) throw new Error("Gemini key missing");

        const identity = aiUser.displayName
          ? `${aiUser.displayName} (@${aiUser.username})`
          : `@${aiUser.username}`;
        const prompt = [
          `You are ${identity}, an AI assistant on Synapse.`,
          postContext ? `The original post says: "${postContext}".` : "",
          `A user mentioned you in a comment saying: "${content}".`,
          "Reply naturally and helpfully in 1-2 short sentences. Do not claim to be human.",
        ].filter(Boolean).join(" ");

        replyText = await generateGeminiText({
          apiKey: geminiKey,
          model: MODEL_NAME,
          prompt,
        });
        if (!replyText) replyText = FALLBACK_REPLIES[0];
      } catch (error) {
        console.warn("Gemini error, using fallback:", error);
        replyText = FALLBACK_REPLIES[Math.floor(Math.random() * FALLBACK_REPLIES.length)];
      }

      const replyRows = await sql`
        insert into public.posts
          (author_uid, post_text, post_type, in_reply_to_post_id, root_post_id)
        values
          (${aiUser.uid}, ${replyText}, 'TEXT', ${commentId}, ${postId})
        returning id
      `;

      const replyPost = replyRows[0];
      if (!replyPost?.id) throw new Error("Failed to create AI reply");

      await sql`
        update public.syra_comment_invocations
        set status = 'completed',
            syra_reply_post_id = ${replyPost.id},
            completed_at = now(),
            error_message = null
        where comment_id = ${commentId}
      `;

      return Response.json({
        success: true,
        reply_post_id: replyPost.id,
        reply: replyText,
      });
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      console.error("AI comment reply error:", message);

      try {
        const body = await req.clone().json();
        const commentId = String((body?.record ?? body)?.comment_id ?? "");
        if (commentId) {
          await sql`
            update public.syra_comment_invocations
            set status = 'failed', error_message = ${message.slice(0, 1000)}
            where comment_id = ${commentId}
          `;
        }
      } catch (_) {}

      return Response.json({ error: message }, { status: 500 });
    } finally {
      await sql.end({ timeout: 2 });
    }
  },
};
