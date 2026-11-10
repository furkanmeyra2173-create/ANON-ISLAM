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

module.exports = async function handler(req, res) {
  res.setHeader("Content-Type", "application/json; charset=utf-8");
  res.setHeader("Cache-Control", "no-store");

  if (req.method !== "POST") {
    res.setHeader("Allow", "POST");
    return res.status(405).json({ error: "Yalnızca POST isteği kabul edilir." });
  }

  const apiKey = process.env.OPENAI_API_KEY;
  if (!apiKey) {
    return res.status(503).json({ error: "AI servisi henüz yapılandırılmadı." });
  }

  try {
    const body = req.body || {};
    const messages = body.messages;

    if (!Array.isArray(messages) || messages.length === 0 || messages.length > 12) {
      return res.status(400).json({ error: "Mesaj geçmişi geçersiz. En fazla 12 mesaj gönderilebilir." });
    }

    const input = [];
    for (const item of messages) {
      if (!item || !["user", "assistant"].includes(item.role) ||
          typeof item.content !== "string" || item.content.trim().length === 0 ||
          item.content.length > 4000) {
        return res.status(400).json({ error: "Mesaj biçimi geçersiz." });
      }
      input.push({ role: item.role, content: item.content.trim() });
    }

    if (input[input.length - 1].role !== "user") {
      return res.status(400).json({ error: "Son mesaj kullanıcıya ait olmalı." });
    }

    const upstream = await fetch("https://api.openai.com/v1/responses", {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${apiKey}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        model: "gpt-6-luna",
        instructions: SYSTEM_INSTRUCTIONS,
        input,
        max_output_tokens: 700,
        store: false
      })
    });

    const data = await upstream.json();

    if (!upstream.ok) {
      console.error("OpenAI API error:", upstream.status, data.error?.type || "unknown");
      const status = upstream.status === 429 ? 429 : 502;
      return res.status(status).json({
        error: upstream.status === 429
          ? "AI kullanım limiti veya bakiye sınırı nedeniyle şu anda yanıt veremiyor."
          : "AI servisine bağlanırken bir hata oluştu."
      });
    }

    const answer = (data.output || [])
      .flatMap(item => item.content || [])
      .filter(part => part.type === "output_text")
      .map(part => part.text || "")
      .join("\n")
      .trim();

    if (!answer) {
      return res.status(502).json({ error: "AI boş yanıt döndürdü. Lütfen tekrar dene." });
    }

    return res.status(200).json({ answer });
  } catch (error) {
    console.error("ANON AI backend error:", error?.message || "unknown");
    return res.status(500).json({ error: "Sunucuda beklenmeyen bir hata oluştu." });
  }
};
