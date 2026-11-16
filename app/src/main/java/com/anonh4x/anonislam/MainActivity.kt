package com.anonh4x.anonislam

import android.content.Context
import android.os.Bundle
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Night = Color(0xFF07110E)
private val Panel = Color(0xFF10231C)
private val PanelLight = Color(0xFF17352A)
private val Emerald = Color(0xFF19B67A)
private val Gold = Color(0xFFE5C36B)
private val Muted = Color(0xFFB8C8C0)

private enum class AppTab(val label: String, val icon: ImageVector) {
    HOME("Ana Sayfa", Icons.Filled.Home),
    PRAYER("Namaz", Icons.Filled.Mosque),
    QURAN("Kur'an", Icons.Filled.AutoStories),
    MORE("Diğer", Icons.Filled.MoreHoriz)
}

private data class PrayerTime(val name: String, val time: String)
private data class PrayerData(
    val city: String,
    val date: String,
    val hijriDate: String,
    val times: List<PrayerTime>,
    val dateIso: String = "",
    val isCached: Boolean = false
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Emerald,
                    onPrimary = Night,
                    secondary = Gold,
                    background = Night,
                    surface = Panel,
                    onSurface = Color.White
                )
            ) {
                AnonIslamApp()
            }
        }
    }
}

@Composable
private fun AnonIslamApp() {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.HOME.name) }
    var city by rememberSaveable { mutableStateOf("Uhingen") }
    val context = androidx.compose.ui.platform.LocalContext.current
    val prayerPreferences = remember(context) {
        context.getSharedPreferences("anon_islam", Context.MODE_PRIVATE)
    }
    var prayerData by remember {
        mutableStateOf(
            loadCachedPrayerData(prayerPreferences.getString(prayerCacheKey(city), null))
        )
    }
    var isLoadingPrayer by remember { mutableStateOf(false) }
    var prayerError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val loadPrayerTimes: () -> Unit = {
        val requestedCity = city.trim()
        if (requestedCity.isBlank()) {
            prayerError = "Lütfen şehir adını yaz."
        } else {
            scope.launch {
                isLoadingPrayer = true
                prayerError = null
                prayerData = loadCachedPrayerData(
                    prayerPreferences.getString(prayerCacheKey(requestedCity), null)
                )
                try {
                    val latestPrayerData = fetchPrayerTimes(requestedCity)
                    prayerPreferences.edit()
                        .putString(prayerCacheKey(requestedCity), prayerDataToCache(latestPrayerData))
                        .apply()
                    prayerData = latestPrayerData
                } catch (e: Exception) {
                    prayerError = if (prayerData != null) {
                        "İnternet yok veya vakitler güncellenemedi. ${prayerData?.date} tarihli kayıtlı veri gösteriliyor."
                    } else {
                        e.message ?: "Namaz vakitleri alınamadı. İnternet bağlantını kontrol et."
                    }
                } finally {
                    isLoadingPrayer = false
                }
            }
        }
    }

    LaunchedEffect(Unit) { loadPrayerTimes() }

    val currentTab = AppTab.valueOf(selectedTab)

    Scaffold(
        containerColor = Night,
        bottomBar = {
            NavigationBar(containerColor = Panel, contentColor = Color.White) {
                AppTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { selectedTab = tab.name },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Night,
                            selectedTextColor = Gold,
                            indicatorColor = Emerald,
                            unselectedIconColor = Muted,
                            unselectedTextColor = Muted
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentTab,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            transitionSpec = {
                (fadeIn() + slideInHorizontally { it / 18 }) togetherWith
                    (fadeOut() + slideOutHorizontally { -it / 18 })
            },
            label = "main-tab-transition"
        ) { tab ->
            when (tab) {
                AppTab.HOME -> HomeScreen(prayerData) { selectedTab = it.name }
                AppTab.PRAYER -> PrayerScreen(
                    city, { city = it }, prayerData, isLoadingPrayer, prayerError, loadPrayerTimes
                )
                AppTab.QURAN -> QuranScreen(context)
                AppTab.MORE -> MoreScreen { selectedTab = AppTab.PRAYER.name }
            }
        }
    }
}

@Composable
private fun HomeScreen(prayerData: PrayerData?, onNavigate: (AppTab) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("ANON", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("İSLAM", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
                Text("Bilgiyle, huzurla, güvenilir kaynaklarla.", color = Muted, fontSize = 12.sp)
            }
            Icon(Icons.Filled.Mosque, contentDescription = null, tint = Gold, modifier = Modifier.size(42.dp))
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PanelLight),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("ESSELÂMÜ ALEYKÜM", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("Hoş geldin", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text("İbadet ve öğrenme yolculuğuna buradan devam et.", color = Muted, fontSize = 13.sp)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Panel),
            shape = RoundedCornerShape(20.dp),
            onClick = { onNavigate(AppTab.PRAYER) }
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Mosque, contentDescription = null, tint = Emerald)
                    Spacer(Modifier.width(8.dp))
                    Text("BUGÜNÜN NAMAZ VAKİTLERİ", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                if (prayerData == null) {
                    Text("Vakitler yükleniyor veya şehir seçimi bekleniyor…", color = Muted, fontSize = 12.sp)
                } else {
                    Text(
                        prayerData.city + " • " + prayerData.date + if (prayerData.isCached) " • kayıtlı" else "",
                        color = Muted,
                        fontSize = 12.sp
                    )
                    NextPrayerCountdown(prayerData)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        prayerData.times.filter { it.name != "Güneş" }.forEach { prayer ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(prayer.name, color = Muted, fontSize = 10.sp)
                                Text(prayer.time, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        Text("KEŞFET", color = Emerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FeatureCard("Kur'an-ı Kerim", "Sure listesi ve Arapça metin", Icons.Filled.AutoStories, Modifier.weight(1f)) {
                onNavigate(AppTab.QURAN)
            }
            FeatureCard("Namaz Vakitleri", "Şehrine göre", Icons.Filled.Mosque, Modifier.weight(1f)) {
                onNavigate(AppTab.PRAYER)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FeatureCard("Tesbih", "Çevrimdışı zikir sayacı", Icons.Filled.Favorite, Modifier.weight(1f)) {
                onNavigate(AppTab.MORE)
            }
        }
    }
}

@Composable
private fun FeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.height(132.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = Emerald, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(11.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(subtitle, color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun PrayerScreen(
    city: String,
    onCityChange: (String) -> Unit,
    prayerData: PrayerData?,
    isLoading: Boolean,
    error: String?,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenHeading("Namaz Vakitleri", "Şehrini seç, günlük vakitleri internetten güncelle.")
        OutlinedTextField(
            value = city,
            onValueChange = onCityChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Şehir (Almanya)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )
        Button(
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = Night)
        ) {
            if (isLoading) {
                CircularProgressIndicator(Modifier.size(18.dp), color = Night, strokeWidth = 2.dp)
                Spacer(Modifier.width(9.dp))
                Text("Vakitler alınıyor…")
            } else {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Vakitleri güncelle")
            }
        }

        if (error != null) Text(error, color = Color(0xFFFF8A80), fontSize = 13.sp)

        if (prayerData != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Panel),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Gold)
                        Spacer(Modifier.width(7.dp))
                        Column {
                            Text(prayerData.city, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(
                                prayerData.date + if (prayerData.isCached) " • çevrimdışı kayıt" else "",
                                color = Muted,
                                fontSize = 12.sp
                            )
                        }
                    }
                    NextPrayerCountdown(prayerData)
                    if (prayerData.hijriDate.isNotBlank()) {
                        Text("Hicrî tarih: " + prayerData.hijriDate, color = Gold, fontSize = 12.sp)
                    }
                    HorizontalDivider(color = PanelLight)
                    prayerData.times.forEach { time ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(time.name, color = Muted, fontSize = 14.sp)
                            Text(time.time, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
            Text(
                "Hesaplama yöntemi: Diyanet (13), ikindi için Hanefî. Yerel caminin vakitleri birkaç dakika farklı olabilir.",
                color = Muted,
                fontSize = 11.sp
            )
        } else if (!isLoading && error == null) {
            Text("Henüz vakit verisi yok. Şehir seçip güncelle.", color = Muted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun MoreScreen(onNavigateToPrayer: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferences = remember { context.getSharedPreferences("anon_islam", Context.MODE_PRIVATE) }
    var count by rememberSaveable { mutableIntStateOf(preferences.getInt("dhikr_count", 0)) }
    var target by rememberSaveable { mutableIntStateOf(preferences.getInt("dhikr_target", 33)) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenHeading("Tesbih ve araçlar", "Sayaç cihazında saklanır, internet gerekmez")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Panel),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("ZİKİR SAYACI", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("$count", color = Color.White, fontSize = 52.sp, fontWeight = FontWeight.Bold)
                Text("Hedef: $target", color = Muted, fontSize = 13.sp)
                LinearProgressIndicator(
                    progress = { (count.toFloat() / target).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = Emerald,
                    trackColor = PanelLight
                )
                Button(
                    onClick = {
                        count += 1
                        preferences.edit().putInt("dhikr_count", count).apply()
                    },
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = Night)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Bir zikir say", fontWeight = FontWeight.Bold)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = {
                        count = 0
                        preferences.edit().putInt("dhikr_count", 0).apply()
                    }) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Text(" Sıfırla")
                    }
                    TextButton(onClick = {
                        target = if (target == 33) 99 else 33
                        preferences.edit().putInt("dhikr_target", target).apply()
                    }) {
                        Text("Hedefi ${if (target == 33) "99" else "33"} yap")
                    }
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("Çevrimdışı kullanım", color = Gold, fontWeight = FontWeight.Bold)
                Text("Zikir sayacı internet olmadan çalışır ve sayım bu cihazda saklanır. Namaz vakitleri internetten güncellenir; son başarılı günün kayıtlı çizelgesi çevrimdışı görüntülenebilir. Güncel hesaplama ve kıble pusulası henüz sunulmamaktadır.", color = Muted, fontSize = 12.sp)
            }
        }
        OutlinedButton(onClick = onNavigateToPrayer, modifier = Modifier.fillMaxWidth()) {
            Text("Namaz vakitlerine git")
        }
    }
}

@Composable
private fun ScreenHeading(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
        Text(subtitle, color = Muted, fontSize = 12.sp)
    }
}

private suspend fun fetchPrayerTimes(city: String): PrayerData = withContext(Dispatchers.IO) {
    val safeCity = URLEncoder.encode(city, "UTF-8")
    val url = URL(
        "https://api.aladhan.com/v1/timingsByCity?city=$safeCity&country=Germany&method=13&school=1&latitudeAdjustmentMethod=3"
    )
    val connection = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 15000
        readTimeout = 15000
        setRequestProperty("Accept", "application/json")
    }

    try {
        val status = connection.responseCode
        val body = if (status in 200..299) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
        }
        if (status !in 200..299) {
            throw IllegalStateException("Namaz vakitleri servisine ulaşılamadı (HTTP $status).")
        }

        val root = JSONObject(body)
        if (root.optInt("code") != 200) {
            throw IllegalStateException("Şehir bulunamadı. Şehir adını kontrol edip tekrar dene.")
        }
        val data = root.getJSONObject("data")
        val timings = data.getJSONObject("timings")
        val date = data.optJSONObject("date")
        val hijri = date?.optJSONObject("hijri")?.optString("date").orEmpty()
        val readableDate = date?.optString("readable").orEmpty()
            .ifBlank { SimpleDateFormat("dd MMM yyyy", Locale("tr", "TR")).format(Date()) }
        val dateIso = date?.optJSONObject("gregorian")?.optString("iso8601")
            ?.substringBefore("T")
            .orEmpty()
            .ifBlank { LocalDate.now(ZoneId.of("Europe/Berlin")).toString() }

        val prayers = listOf(
            PrayerTime("İmsak", cleanTime(timings.optString("Fajr"))),
            PrayerTime("Güneş", cleanTime(timings.optString("Sunrise"))),
            PrayerTime("Öğle", cleanTime(timings.optString("Dhuhr"))),
            PrayerTime("İkindi", cleanTime(timings.optString("Asr"))),
            PrayerTime("Akşam", cleanTime(timings.optString("Maghrib"))),
            PrayerTime("Yatsı", cleanTime(timings.optString("Isha")))
        )
        PrayerData(city, readableDate, hijri, prayers, dateIso = dateIso)
    } finally {
        connection.disconnect()
    }
}

private fun cleanTime(value: String): String = value.substringBefore(" ").trim()

private fun prayerCacheKey(city: String): String =
    "prayer_cache_${city.trim().lowercase(Locale.ROOT)}"

private fun prayerDataToCache(prayerData: PrayerData): String = JSONObject().apply {
    put("city", prayerData.city)
    put("date", prayerData.date)
    put("hijriDate", prayerData.hijriDate)
    put("dateIso", prayerData.dateIso)
    put("times", org.json.JSONArray().apply {
        prayerData.times.forEach { prayer ->
            put(JSONObject().apply {
                put("name", prayer.name)
                put("time", prayer.time)
            })
        }
    })
}.toString()

private fun loadCachedPrayerData(serialized: String?): PrayerData? = runCatching {
    if (serialized.isNullOrBlank()) return null
    val cached = JSONObject(serialized)
    val times = cached.getJSONArray("times")
    val prayerTimes = (0 until times.length()).map { index ->
        val prayer = times.getJSONObject(index)
        PrayerTime(prayer.getString("name"), prayer.getString("time"))
    }
    if (prayerTimes.size < 5 || prayerTimes.any { runCatching { LocalTime.parse(it.time) }.isFailure }) {
        return null
    }
    PrayerData(
        city = cached.getString("city"),
        date = cached.getString("date"),
        hijriDate = cached.optString("hijriDate"),
        times = prayerTimes,
        dateIso = cached.optString("dateIso"),
        isCached = true
    )
}.getOrNull()

@Composable
private fun NextPrayerCountdown(prayerData: PrayerData) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(prayerData.city, prayerData.dateIso) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1_000)
        }
    }

    val nextPrayer = remember(prayerData, currentTime) {
        val zone = ZoneId.of("Europe/Berlin")
        val today = LocalDate.now(zone)
        val scheduleDate = runCatching { LocalDate.parse(prayerData.dateIso) }.getOrNull()
        if (scheduleDate != today) {
            null
        } else {
            val now = java.time.Instant.ofEpochMilli(currentTime).atZone(zone)
            prayerData.times
                .filter { it.name != "Güneş" }
                .mapNotNull { prayer ->
                    val time = runCatching { LocalTime.parse(prayer.time) }.getOrNull() ?: return@mapNotNull null
                    val target = today.atTime(time).atZone(zone)
                    if (target.isAfter(now)) prayer.name to java.time.Duration.between(now, target).seconds else null
                }
                .firstOrNull()
        }
    }

    if (nextPrayer == null) {
        Text("Sonraki namaz sayacı için bugünün güncel vakitleri gerekli.", color = Muted, fontSize = 12.sp)
    } else {
        val remaining = nextPrayer.second.coerceAtLeast(0)
        Text(
            "${nextPrayer.first} vaktine ${String.format(Locale.getDefault(), "%02d:%02d:%02d", remaining / 3600, (remaining % 3600) / 60, remaining % 60)}",
            color = Gold,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}
