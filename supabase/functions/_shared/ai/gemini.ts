export type GeminiTextRequest = {
  apiKey: string;
  model: string;
  prompt: string;
};

/**
 * Shared Gemini REST client for Synapse Edge Functions.
 * Feature-specific authorization, prompts, and fallback behavior stay in each caller.
 */
export async function generateGeminiText({
  apiKey,
  model,
  prompt,
}: GeminiTextRequest): Promise<string> {
  const url = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey}`;
  const response = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ contents: [{ parts: [{ text: prompt }] }] }),
  });

  const data = await response.json();
  if (!response.ok || data?.error) {
    throw new Error(
      data?.error?.message || `Gemini request failed with status ${response.status}`,
    );
  }

  return data?.candidates?.[0]?.content?.parts?.[0]?.text?.trim() || "";
}
