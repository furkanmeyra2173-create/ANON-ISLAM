# ANON İSLAM

Türkçe arayüzlü, Android için kapsamlı bir İslam yardımcı uygulaması.

## Mevcut sürüm
- Türkçe Jetpack Compose ana ekranı ve alt gezinme
- Şehir seçimiyle çevrimiçi namaz vakitleri; son başarılı günün vakitleri cihazda çevrimdışı görüntülenebilir ve kayıtlı olduğu belirtilir
- Sonraki namaza geri sayım yalnızca güncel tarihli vakit verisi varsa gösterilir
- Şehir, desteklenen hesaplama yöntemi ve ikindi tercihi seçilebilir; günlük namaz takibi cihazda tarih bazında saklanır
- Cihazda saklanan, internet gerektirmeyen dijital zikir sayacı
- Yapay zekâ sohbeti, API bağlantısı veya chatbot uygulamaya dahil değildir
- 114 sure listesi ve Arapça Kur’an metni AlQuran Cloud API üzerinden alınır; seçilen sureler çevrimdışı saklanabilir ve tüm metin tek indirmeyle cihazda tutulabilir
- Arapça metin Tanzil Project atfı ve CC BY 3.0 bildirimiyle aynen gösterilir; yazı boyutu ayarlanabilir, ayetler favorilenip paylaşılabilir; Türkçe meallerin lisansı doğrulanana kadar meal sunulmaz
- Doğrulanmış dualar/hadisler, kıble, bildirimler ve diğer kapsamlı içerikler henüz tamamlanmamıştır; kaynaklar doğrulanmadan gösterilmez

## Teknoloji
- Kotlin
- Jetpack Compose
- Material 3
- Android 8.0+ (API 26+) başlangıç desteği

## Derleme
GitHub Actions, `app-debug.apk` dosyasını otomatik oluşturur. Namaz vakitleri çevrimiçi kaynaktan alınır. Yapay zekâ özellikleri uygulamanın kapsamına dahil değildir.

> Not: Kur'an sesleri henüz uygulamada bulunmaz. Zikir sayacı çevrimdışıdır; namaz vakitleri için internet bağlantısı gerekir.
