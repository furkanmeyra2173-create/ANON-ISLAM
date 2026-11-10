const SYSTEM_INSTRUCTIONS = `
Sen ANON İSLAM uygulamasının Türkçe konuşan yapay zekâ asistanısın.
- Kullanıcıya nazik, açık ve anlaşılır Türkçe ile cevap ver.
- İslam hakkında genel bilgi verirken Kur'an, sahih hadis ve güvenilir ilmihal kaynakları konusunda dikkatli ol.
- Emin olmadığın ayet, sure numarası, hadis metni, hadis numarası veya kaynak bilgisi uydurma. Kaynağı doğrulayamıyorsan bunu açıkça söyle.
- Fıkhî konularda mezhepler arasında görüş farkı olabileceğini belirt; kesin fetva makamı gibi davranma. Karmaşık veya kişiye özel meselelerde ehil bir din âlimine danışılmasını öner.
- Sağlık, hukuk ve güvenlik gibi konularda dinî yorumun profesyonel desteğin yerini tutmadığını belirt.
- Saygılı, tarafsız ve yargılamayan bir üslup kullan. Başka dinlere ve inançlara hakaret etme.
- Kullanıcı yalnızca selam verirse doğal biçimde selamını al.
- Gereksiz uzun cevap verme; istenirse ayrıntılandır.
`;

function json(data, status = 200, extraHeaders = {}) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      "Content-Type": "application/json; charset=utf-8",
      "Cache-Control": "no-store",
      ...extraHeaders
    }
  });
}

export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") {
      return new Response(null, {
        status: 204,
        headers: {
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Methods": "POST, OPTIONS",
          "Access-Control-Allow-Headers": "Content-Type"
        }
      });
    }

    if (request.method !== "POST") {
      return json({ error: "Yalnızca POST isteği kabul edilir." }, 405, {
        "Allow": "POST, OPTIONS"
      });
    }

    // Approximate per-IP abuse protection. Cloudflare applies counters per location.
    const clientIp = request.headers.get("CF-Connecting-IP") || "unknown";
    if (env.ANON_AI_LIMITER) {
      const { success } = await env.ANON_AI_LIMITER.limit({ key: clientIp });
      if (!success) {
        return json({ error: "Çok fazla istek gönderdin. Bir dakika bekleyip tekrar dene." }, 429, {
          "Retry-After": "60"
        });
      }
    }

    if (!env.AI) {
      return json({ error: "Ücretsiz AI servisi henüz etkinleştirilmedi." }, 503);
    }

    let body;
    try {
      body = await request.json();
    } catch {
      return json({ error: "Geçersiz JSON isteği." }, 400);
    }

    const messages = body?.messages;
    if (!Array.isArray(messages) || messages.length === 0 || messages.length > 12) {
      return json({ error: "Mesaj geçmişi geçersiz. En fazla 12 mesaj gönderilebilir." }, 400);
    }

    const input = [];
    for (const item of messages) {
      if (!item || !["user", "assistant"].includes(item.role) ||
          typeof item.content !== "string" ||
          item.content.trim().length === 0 ||
          item.content.length > 4000) {
        return json({ error: "Mesaj biçimi geçersiz." }, 400);
      }
      input.push({ role: item.role, content: item.content.trim() });
    }

    if (input[input.length - 1].role !== "user") {
      return json({ error: "Son mesaj kullanıcıya ait olmalı." }, 400);
    }

    try {
      const result = await env.AI.run(
        "@cf/google/gemma-4-26b-a4b-it",
        {
          messages: [
            { role: "system", content: SYSTEM_INSTRUCTIONS },
            ...input
          ],
          max_tokens: 500,
          chat_template_kwargs: { enable_thinking: false }
        },
        { rejectIfBusy: true }
      );

      // Workers AI model output may expose either a direct response or chat-completion shape.
      const answer = (
        result?.response ||
        result?.choices?.[0]?.message?.content ||
        ""
      ).trim();

      if (!answer) {
        return json({ error: "AI boş yanıt döndürdü. Lütfen tekrar dene." }, 502);
      }

      return json({ answer });
    } catch (error) {
      console.error("ANON ISLAM Worker error:", error?.message || "unknown");
      return json({ error: "Sunucuda beklenmeyen bir hata oluştu." }, 500);
    }
  }
};
