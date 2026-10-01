# GKP v.03 backend

Environment variables:

- `OPENAI_API_KEY` — server-side OpenAI API key
- `GKP_APP_TOKEN` — a long random token shared only with your GKP installation
- `OPENAI_MODEL` — optional; defaults to `gpt-5.6`
- `PORT` — normally supplied by the hosting service

Run:

```bash
npm start
```

Endpoints:

- `GET /health`
- `POST /ask` with header `Authorization: Bearer <GKP_APP_TOKEN>` and JSON `{"text":"..."}`

Never put `OPENAI_API_KEY` into the Android project or GitHub repository.
