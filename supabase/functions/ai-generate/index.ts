import { serve } from "https://deno.land/std@0.168.0/http/server.ts"

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
}

function json(body: Record<string, unknown>, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  })
}

async function getAiAccountProfile() {
  // Support common names for the UID secret so the UID itself is never hardcoded.
  const uid = Deno.env.get("AI_ACCOUNT_UID")
    || Deno.env.get("AI_USER_UID")
    || Deno.env.get("SYRA_UID")
    || Deno.env.get("AI_UID")
  const supabaseUrl = Deno.env.get("SUPABASE_URL")
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")

  if (!uid || !supabaseUrl || !serviceRoleKey) return null

  const url = new URL("/rest/v1/users", supabaseUrl)
  url.searchParams.set("uid", `eq.${uid}`)
  url.searchParams.set("select", "uid,username,display_name")
  url.searchParams.set("limit", "1")

  const response = await fetch(url, {
    headers: {
      apikey: serviceRoleKey,
      Authorization: `Bearer ${serviceRoleKey}`,
    },
  })

  if (!response.ok) {
    throw new Error(`Could not resolve AI account profile (HTTP ${response.status})`)
  }

  const users = await response.json()
  const user = Array.isArray(users) ? users[0] : null
  if (!user) return null

  return {
    username: typeof user.username === "string" ? user.username : "",
    displayName: typeof user.display_name === "string" ? user.display_name : "",
  }
}

serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders })
  }

  if (req.method !== "POST") {
    return json({ error: "Method not allowed" }, 405)
  }

  const geminiKey = Deno.env.get("GEMINI_API_KEY")
  if (!geminiKey) {
    return json({ error: "Gemini API key is not configured on server" }, 500)
  }

  try {
    const body = await req.json()
    const prompt = typeof body.prompt === "string" ? body.prompt.trim() : ""
    const model = typeof body.model === "string" && body.model.trim()
      ? body.model.trim()
      : "gemini-1.5-flash"

    if (!prompt) {
      return json({ error: "Prompt is required" }, 400)
    }

    const profile = await getAiAccountProfile()
    const identityContext = profile
      ? [
          "AI account profile resolved from its configured UID in the users database:",
          profile.username ? `Username: @${profile.username}` : "",
          profile.displayName ? `Display name: ${profile.displayName}` : "",
          "Use this current profile information when the request asks you to identify or refer to the app's AI account. Do not invent a different username.",
        ].filter(Boolean).join("\n")
      : ""

    const fullPrompt = identityContext
      ? `${identityContext}\n\nUser request:\n${prompt}`
      : prompt

    const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${geminiKey}`
    const response = await fetch(geminiUrl, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        contents: [{ parts: [{ text: fullPrompt }] }],
      }),
    })

    const data = await response.json()
    if (!response.ok || data.error) {
      throw new Error(data.error?.message || `Gemini request failed with status ${response.status}`)
    }

    const text = data.candidates?.[0]?.content?.parts?.[0]?.text?.trim() || ""
    return json({ text })
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error)
    return json({ error: message }, 500)
  }
})
