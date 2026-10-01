import http from "node:http";

const PORT = Number(process.env.PORT || 3000);
const OPENAI_API_KEY = process.env.OPENAI_API_KEY;
const GKP_APP_TOKEN = process.env.GKP_APP_TOKEN;
const MODEL = process.env.OPENAI_MODEL || "gpt-5.6";

if (!OPENAI_API_KEY) {
  console.error("Missing OPENAI_API_KEY");
  process.exit(1);
}
if (!GKP_APP_TOKEN) {
  console.error("Missing GKP_APP_TOKEN");
  process.exit(1);
}

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
    return send(res, 200, { ok: true, service: "GKP v.03" });
  }

  if (req.method !== "POST" || req.url !== "/ask") {
    return send(res, 404, { error: "Not found" });
  }

  const auth = req.headers.authorization || "";
  if (auth !== `Bearer ${GKP_APP_TOKEN}`) {
    return send(res, 401, { error: "Invalid GKP app token" });
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
        instructions: "You are the assistant inside GKP. Answer the user's shared Google Keep note or request directly, clearly, and concisely. Preserve useful formatting.",
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
