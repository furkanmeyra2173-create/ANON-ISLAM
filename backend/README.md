# ANON İSLAM AI Backend

Vercel serverless API for the Android app.

## Environment variable

Set `OPENAI_API_KEY` in Vercel Project Settings → Environment Variables.
Never commit the API key to Git.

## Endpoint

`POST /api/chat`

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

The endpoint uses OpenAI Responses API with the `gpt-6-luna` model. It limits each request to 12 messages, 4,000 characters per message, and 700 output tokens.

Before public release, add user authentication and persistent per-user/IP rate limiting to protect API spend.
