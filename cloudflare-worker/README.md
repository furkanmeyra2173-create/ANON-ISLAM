# ANON İSLAM — Cloudflare AI Backend

This Worker securely proxies chat requests to the OpenAI Responses API. The OpenAI key must be stored as a Cloudflare Worker secret and must never be committed to Git.

## Deploy from the Cloudflare dashboard

1. Open **Workers & Pages** in the Cloudflare dashboard.
2. Choose **Create application** and import the GitHub repository.
3. Select `furkanmeyra2173-create/ANON-ISLAM`.
4. Set the project root directory to `cloudflare-worker`.
5. Deploy the Worker.
6. Open the Worker, then **Settings → Variables and Secrets → Add**.
7. Choose **Secret**, set the variable name to `OPENAI_API_KEY`, paste the key value, and deploy.

## API

POST `/api/chat` (the Worker accepts POST requests at its root URL too; Android should use the root URL).

Request:
```json
{
  "messages": [
    { "role": "user", "content": "Selam" }
  ]
}
```

Response:
```json
{ "answer": "Aleyküm selam..." }
```

The Worker uses the OpenAI Responses API with `gpt-6-luna`, `store: false`, up to 12 messages, 4,000 characters per message, and 700 output tokens.

## Before public release

The endpoint is not yet protected by user authentication or persistent rate limiting. Keep it for private testing until abuse protection and usage limits are configured. OpenAI API usage is billed separately from Cloudflare Workers.
