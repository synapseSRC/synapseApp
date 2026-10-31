import { createClient } from "npm:@supabase/supabase-js@2";
import { AccessToken, TrackSource } from "npm:livekit-server-sdk@2";

/**
 * Mints a room-scoped LiveKit token for the calling user.
 *
 * The client is never trusted about its own role: everything the grant depends on (identity,
 * room name, host/speaker/audience) comes from `get_echo_token_context`, which resolves the
 * caller from their JWT inside Postgres and re-checks live membership on every call.
 */

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

interface TokenContext {
  user_id: string;
  livekit_room: string;
  role: string;
  display_name: string | null;
}

function json(body: unknown, status: number): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json", Connection: "keep-alive" },
  });
}

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") {
    return json({ error: "method_not_allowed" }, 405);
  }

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const supabaseKey = Deno.env.get("SUPABASE_ANON_KEY") ??
    Deno.env.get("SUPABASE_PUBLISHABLE_KEY");
  const livekitUrl = Deno.env.get("LIVEKIT_URL");
  const apiKey = Deno.env.get("LIVEKIT_API_KEY");
  const apiSecret = Deno.env.get("LIVEKIT_API_SECRET");

  if (!supabaseUrl || !supabaseKey || !livekitUrl || !apiKey || !apiSecret) {
    console.error("livekit-token: missing environment configuration");
    return json({ error: "server_misconfigured" }, 500);
  }

  const authHeader = req.headers.get("Authorization");
  if (!authHeader) {
    return json({ error: "unauthorized" }, 401);
  }

  let roomId: unknown;
  try {
    const body = await req.json();
    roomId = body?.room_id;
  } catch {
    return json({ error: "invalid_json" }, 400);
  }
  if (typeof roomId !== "string" || !UUID_RE.test(roomId)) {
    return json({ error: "invalid_room_id" }, 400);
  }

  const supabase = createClient(supabaseUrl, supabaseKey, {
    global: { headers: { Authorization: authHeader } },
    auth: { persistSession: false, autoRefreshToken: false },
  });

  const { data, error } = await supabase.rpc("get_echo_token_context", {
    p_room_id: roomId,
  });

  if (error) {
    // P0001 is the SYNAPSE_ECHO_* raise from the RPC (not live, not a member);
    // 42501 is "permission denied for function". Neither should mint a token.
    if (error.code === "P0001" || error.code === "42501") {
      return json({ error: "forbidden" }, 403);
    }
    console.error("livekit-token: context lookup failed", error.code, error.message);
    return json({ error: "context_unavailable" }, 500);
  }

  const context = (Array.isArray(data) ? data[0] : data) as TokenContext | undefined;
  if (!context?.user_id || !context?.livekit_room) {
    return json({ error: "forbidden" }, 403);
  }

  const canPublish = context.role === "host" || context.role === "speaker";

  const token = new AccessToken(apiKey, apiSecret, {
    identity: context.user_id,
    name: context.display_name ?? undefined,
    ttl: "2h",
  });
  token.addGrant({
    roomJoin: true,
    room: context.livekit_room,
    canPublish,
    canPublishSources: [TrackSource.MICROPHONE],
    canSubscribe: true,
    canPublishData: true,
  });

  return json({ url: livekitUrl, token: await token.toJwt() }, 200);
});
