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

    const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${geminiKey}`
    const response = await fetch(geminiUrl, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        contents: [{ parts: [{ text: prompt }] }],
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
