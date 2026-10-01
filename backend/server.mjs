import http from "node:http";

const PORT = Number(process.env.PORT || 3000);
const OPENAI_API_KEY = process.env.OPENAI_API_KEY || "";
const GKP_APP_TOKEN = process.env.GKP_APP_TOKEN || "";
const MODEL = process.env.OPENAI_MODEL || "gpt-5.6";

function send(res, status, body) {
  const json = JSON.stringify(body);
  res.writeHead(status, {
    "content-type": "application/json; charset=utf-8",
    "content-length": Buffer.byteLength(json),
    "cache-control": "no-store"
  });
  res.end(json);
}

async function readJson(req) {
  let raw = "";
  for await (const chunk of req) {
    raw += chunk;
    if (raw.length > 200000) throw new Error("Request too large");
  }
  return JSON.parse(raw || "{}");
}

function extractText(response) {
  if (typeof response.output_text === "string" && response.output_text.trim()) {
    return response.output_text.trim();
  }
  const parts = [];
  for (const item of response.output || []) {
    for (const content of item.content || []) {
      if (content.type === "output_text" && content.text) parts.push(content.text);
    }
  }
  return parts.join("\n").trim();
}

const server = http.createServer(async (req, res) => {
  if (req.method === "GET" && req.url === "/health") {
    return send(res, 200, {
      ok: true,
      service: "GKP v.04",
      openaiConfigured: Boolean(OPENAI_API_KEY),
      appTokenConfigured: Boolean(GKP_APP_TOKEN)
    });
  }

  if (req.method !== "POST" || req.url !== "/ask") {
    return send(res, 404, { error: "Not found" });
  }

  if (!GKP_APP_TOKEN) {
    return send(res, 503, { error: "GKP app token is not configured on the server" });
  }

  const auth = req.headers.authorization || "";
  if (auth !== `Bearer ${GKP_APP_TOKEN}`) {
    return send(res, 401, { error: "Invalid GKP app token" });
  }

  if (!OPENAI_API_KEY) {
    return send(res, 503, { error: "OpenAI API key is not configured on the server" });
  }

  try {
    const body = await readJson(req);
    const text = String(body.text || "").trim();
    if (!text) return send(res, 400, { error: "No text supplied" });

    const upstream = await fetch("https://api.openai.com/v1/responses", {
      method: "POST",
      headers: {
        "authorization": `Bearer ${OPENAI_API_KEY}`,
        "content-type": "application/json"
      },
      body: JSON.stringify({
        model: MODEL,
        instructions: "You are the assistant inside GKP. Review the visible Google Keep note and respond directly, clearly, and concisely. If it is a task list, identify what needs attention. If it is a question, answer it. Preserve useful formatting.",
        input: text
      })
    });

    const data = await upstream.json();
    if (!upstream.ok) {
      const message = data?.error?.message || "OpenAI request failed";
      return send(res, upstream.status, { error: message });
    }

    const answer = extractText(data);
    if (!answer) return send(res, 502, { error: "OpenAI returned no text" });

    return send(res, 200, { answer });
  } catch (err) {
    return send(res, 500, { error: err?.message || "Server error" });
  }
});

server.listen(PORT, () => {
  console.log(`GKP backend listening on port ${PORT}`);
});
