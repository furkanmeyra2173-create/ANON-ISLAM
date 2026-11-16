package com.anonh4x.anonislam

import android.content.Context
import android.os.Bundle
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
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
import androidx.compose.material.icons.filled.Explore
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class AppPalette(
    val background: Color,
    val panel: Color,
    val panelRaised: Color,
    val accent: Color,
    val highlight: Color,
    val muted: Color,
    val foreground: Color
)

private val darkPalette = AppPalette(
    Color(0xFF07110E), Color(0xFF10231C), Color(0xFF17352A),
    Color(0xFF19B67A), Color(0xFFE5C36B), Color(0xFFB8C8C0), Color.White
)
private val lightPalette = AppPalette(
    Color(0xFFF5F7F3), Color.White, Color(0xFFE8F1EB),
    Color(0xFF087A50), Color(0xFF856313), Color(0xFF56675F), Color(0xFF14211A)
)
private val LocalAppPalette = androidx.compose.runtime.staticCompositionLocalOf { darkPalette }
private val Night: Color @Composable get() = LocalAppPalette.current.background
private val Panel: Color @Composable get() = LocalAppPalette.current.panel
private val PanelLight: Color @Composable get() = LocalAppPalette.current.panelRaised
private val Emerald: Color @Composable get() = LocalAppPalette.current.accent
private val Gold: Color @Composable get() = LocalAppPalette.current.highlight
private val Muted: Color @Composable get() = LocalAppPalette.current.muted
private val Foreground: Color @Composable get() = LocalAppPalette.current.foreground

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
    val times: List<PrayerTime>
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AnonIslamApp()
        }
    }
}

@Composable
private fun AnonIslamApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences("anon_islam", Context.MODE_PRIVATE) }
    var isLightTheme by rememberSaveable {
        mutableStateOf(preferences.getBoolean("light_theme", false))
    }
    val palette = if (isLightTheme) lightPalette else darkPalette
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.HOME.name) }
    var city by rememberSaveable { mutableStateOf("Uhingen") }
    var prayerData by remember { mutableStateOf<PrayerData?>(null) }
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
                try {
                    prayerData = fetchPrayerTimes(requestedCity)
                } catch (e: Exception) {
                    prayerError = e.message ?: "Namaz vakitleri alınamadı. İnternet bağlantını kontrol et."
                } finally {
                    isLoadingPrayer = false
                }
            }
        }
    }

    LaunchedEffect(Unit) { loadPrayerTimes() }

    val currentTab = AppTab.valueOf(selectedTab)

    MaterialTheme(
        colorScheme = if (isLightTheme) {
            lightColorScheme(
                primary = palette.accent,
                onPrimary = Color.White,
                secondary = palette.highlight,
                background = palette.background,
                surface = palette.panel,
                onSurface = palette.foreground
            )
        } else {
            darkColorScheme(
                primary = palette.accent,
                onPrimary = palette.background,
                secondary = palette.highlight,
                background = palette.background,
                surface = palette.panel,
                onSurface = palette.foreground
            )
        }
    ) {
      androidx.compose.runtime.CompositionLocalProvider(LocalAppPalette provides palette) {
    Scaffold(
        containerColor = Night,
        bottomBar = {
            NavigationBar(containerColor = Panel, contentColor = Foreground) {
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
                AppTab.QURAN -> QuranScreen()
                AppTab.MORE -> MoreScreen(
                    isLightTheme = isLightTheme,
                    onThemeChange = { enabled ->
                        isLightTheme = enabled
                        preferences.edit().putBoolean("light_theme", enabled).apply()
                    },
                    onNavigateToPrayer = { selectedTab = AppTab.PRAYER.name }
                )
            }
        }
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
                Text("İSLAM", color = Foreground, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
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
                Text("Hoş geldin", color = Foreground, fontSize = 25.sp, fontWeight = FontWeight.Bold)
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
                    Text(prayerData.city + " • " + prayerData.date, color = Muted, fontSize = 12.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        prayerData.times.filter { it.name != "Güneş" }.forEach { prayer ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(prayer.name, color = Muted, fontSize = 10.sp)
                                Text(prayer.time, color = Foreground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        Text("KEŞFET", color = Emerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FeatureCard("Kur'an-ı Kerim", "Sureler ve mealler", Icons.Filled.AutoStories, Modifier.weight(1f)) {
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
            FeatureCard("Tesbih ve Dualar", "Hazırlanıyor", Icons.Filled.Favorite, Modifier.weight(1f)) {
                onNavigate(AppTab.MORE)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FeatureCard("Kıble Pusulası", "Hazırlanıyor", Icons.Filled.Explore, Modifier.weight(1f)) {
                onNavigate(AppTab.MORE)
            }
            FeatureCard("Diğer Özellikler", "Yol haritası", Icons.Filled.MoreHoriz, Modifier.weight(1f)) {
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
            Text(title, color = Foreground, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                            Text(prayerData.city, color = Foreground, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(prayerData.date, color = Muted, fontSize = 12.sp)
                        }
                    }
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
                            Text(time.time, color = Foreground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
private fun QuranScreen() {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenHeading("Kur'an-ı Kerim", "Sureler, Arapça metin, meal ve tilavet")
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.AutoStories, contentDescription = null, tint = Emerald, modifier = Modifier.size(32.dp))
                Text("Kur'an bölümü hazırlanıyor", color = Foreground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Bir sonraki geliştirmede sure listesi, Arapça metin, Türkçe meal ve sesli tilavet eklenecek. Meal ve kaynak bilgileri açıkça gösterilecek.",
                    color = Muted,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun MoreScreen(
    isLightTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onNavigateToPrayer: () -> Unit
) {
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
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Görünüm", color = Foreground, fontWeight = FontWeight.SemiBold)
                    Text(if (isLightTheme) "Açık tema" else "Koyu tema", color = Muted, fontSize = 12.sp)
                }
                Switch(
                    modifier = Modifier.semantics {
                        contentDescription = if (isLightTheme) "Açık tema" else "Koyu tema"
                    },
                    checked = isLightTheme,
                    onCheckedChange = onThemeChange,
                    colors = SwitchDefaults.colors(checkedTrackColor = Emerald)
                )
            }
        }
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
                Text("$count", color = Foreground, fontSize = 52.sp, fontWeight = FontWeight.Bold)
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
                Text("Zikir sayacı internet olmadan çalışır ve sayım bu cihazda saklanır. Namaz vakitleri internetten alınır; çevrimdışı hesaplama ve konum pusulası henüz sunulmamaktadır.", color = Muted, fontSize = 12.sp)
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
        Text(title, color = Foreground, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
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

        val prayers = listOf(
            PrayerTime("İmsak", cleanTime(timings.optString("Fajr"))),
            PrayerTime("Güneş", cleanTime(timings.optString("Sunrise"))),
            PrayerTime("Öğle", cleanTime(timings.optString("Dhuhr"))),
            PrayerTime("İkindi", cleanTime(timings.optString("Asr"))),
            PrayerTime("Akşam", cleanTime(timings.optString("Maghrib"))),
            PrayerTime("Yatsı", cleanTime(timings.optString("Isha")))
        )
        PrayerData(city, readableDate, hijri, prayers)
    } finally {
        connection.disconnect()
    }
}

private fun cleanTime(value: String): String = value.substringBefore(" ").trim()
