package com.anonh4x.anonislam

import android.content.Context
import android.content.res.Configuration
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
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
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

private enum class AppTab(val labelRes: Int, val icon: ImageVector) {
    HOME(R.string.tab_home, Icons.Filled.Home),
    PRAYER(R.string.tab_prayer, Icons.Filled.Mosque),
    QURAN(R.string.tab_quran, Icons.Filled.AutoStories),
    MORE(R.string.tab_more, Icons.Filled.MoreHoriz)
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
    var languageCode by rememberSaveable {
        mutableStateOf(preferences.getString("app_language", "tr") ?: "tr")
    }
    val baseConfiguration = LocalConfiguration.current
    val appConfiguration = remember(languageCode, baseConfiguration) {
        Configuration(baseConfiguration).apply {
            setLocale(Locale.forLanguageTag(languageCode))
        }
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
                    prayerData = fetchPrayerTimes(requestedCity, Locale.forLanguageTag(languageCode))
                } catch (e: Exception) {
                    prayerError = e.message ?: "Namaz vakitleri alınamadı. İnternet bağlantını kontrol et."
                } finally {
                    isLoadingPrayer = false
                }
            }
        }
    }

    LaunchedEffect(languageCode) { loadPrayerTimes() }

    val currentTab = AppTab.valueOf(selectedTab)

    androidx.compose.runtime.CompositionLocalProvider(
        LocalConfiguration provides appConfiguration
    ) {
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
                        icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelRes)) },
                        label = { Text(stringResource(tab.labelRes), fontSize = 10.sp) },
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
                    languageCode = languageCode,
                    onLanguageChange = { code ->
                        languageCode = code
                        preferences.edit().putString("app_language", code).apply()
                    },
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
                Text(stringResource(R.string.home_tagline), color = Muted, fontSize = 12.sp)
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
                Text(stringResource(R.string.greeting), color = Foreground, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.home_intro), color = Muted, fontSize = 13.sp)
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
                    Text(stringResource(R.string.today_prayers), color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                if (prayerData == null) {
                    Text(stringResource(R.string.prayers_loading), color = Muted, fontSize = 12.sp)
                } else {
                    Text(prayerData.city + " • " + prayerData.date, color = Muted, fontSize = 12.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        prayerData.times.filter { it.name != "Güneş" }.forEach { prayer ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(prayerLabelResource(prayer.name)), color = Muted, fontSize = 10.sp)
                                Text(prayer.time, color = Foreground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        Text(stringResource(R.string.discover), color = Emerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FeatureCard(stringResource(R.string.feature_quran), stringResource(R.string.feature_quran_subtitle), Icons.Filled.AutoStories, Modifier.weight(1f)) {
                onNavigate(AppTab.QURAN)
            }
            FeatureCard(stringResource(R.string.feature_prayer), stringResource(R.string.feature_prayer_subtitle), Icons.Filled.Mosque, Modifier.weight(1f)) {
                onNavigate(AppTab.PRAYER)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FeatureCard(stringResource(R.string.feature_dhikr), stringResource(R.string.feature_dhikr_subtitle), Icons.Filled.Favorite, Modifier.weight(1f)) {
                onNavigate(AppTab.MORE)
            }
            FeatureCard(stringResource(R.string.feature_duas), stringResource(R.string.feature_preparing), Icons.Filled.Favorite, Modifier.weight(1f)) {
                onNavigate(AppTab.MORE)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FeatureCard(stringResource(R.string.feature_qibla), stringResource(R.string.feature_preparing), Icons.Filled.Explore, Modifier.weight(1f)) {
                onNavigate(AppTab.MORE)
            }
            FeatureCard(stringResource(R.string.feature_more), stringResource(R.string.feature_roadmap), Icons.Filled.MoreHoriz, Modifier.weight(1f)) {
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
        ScreenHeading(stringResource(R.string.prayer_screen_title), stringResource(R.string.prayer_screen_subtitle))
        OutlinedTextField(
            value = city,
            onValueChange = onCityChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.city_label)) },
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
                Text(stringResource(R.string.prayers_updating))
            } else {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.refresh_prayers))
            }
        }

        if (error != null) {
            val localizedError = when {
                error.startsWith("Lütfen şehir") -> stringResource(R.string.city_required)
                error.startsWith("Şehir bulunamadı") -> stringResource(R.string.city_not_found)
                error.startsWith("Namaz vakitleri servisine") -> {
                    val status = error.substringAfter("HTTP ").substringBefore(")").toIntOrNull() ?: 500
                    stringResource(R.string.prayer_service_error, status)
                }
                else -> stringResource(R.string.prayers_unavailable)
            }
            Text(localizedError, color = Color(0xFFFF8A80), fontSize = 13.sp)
        }

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
                        Text(stringResource(R.string.hijri_date, prayerData.hijriDate), color = Gold, fontSize = 12.sp)
                    }
                    HorizontalDivider(color = PanelLight)
                    prayerData.times.forEach { time ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(prayerLabelResource(time.name)), color = Muted, fontSize = 14.sp)
                            Text(time.time, color = Foreground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
            Text(
                stringResource(R.string.prayer_calculation_note),
                color = Muted,
                fontSize = 11.sp
            )
        } else if (!isLoading && error == null) {
            Text(stringResource(R.string.prayers_empty), color = Muted, fontSize = 13.sp)
        }
    }
}


@Composable
private fun MoreScreen(
    isLightTheme: Boolean,
    languageCode: String,
    onLanguageChange: (String) -> Unit,
    onThemeChange: (Boolean) -> Unit,
    onNavigateToPrayer: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferences = remember { context.getSharedPreferences("anon_islam", Context.MODE_PRIVATE) }
    var count by rememberSaveable { mutableIntStateOf(preferences.getInt("dhikr_count", 0)) }
    var target by rememberSaveable { mutableIntStateOf(preferences.getInt("dhikr_target", 33)) }
    val themeLabel = stringResource(if (isLightTheme) R.string.light_theme else R.string.dark_theme)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenHeading(stringResource(R.string.tools_title), stringResource(R.string.tools_subtitle))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Panel),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.language), color = Foreground, fontWeight = FontWeight.SemiBold)
                listOf("tr" to "Türkçe", "de" to "Deutsch", "en" to "English").forEach { (code, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = languageCode == code,
                                onClick = { onLanguageChange(code) },
                                role = Role.RadioButton
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = languageCode == code,
                            onClick = null
                        )
                        Text(label, color = Foreground)
                    }
                }
            }
        }
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
                    Text(stringResource(R.string.appearance), color = Foreground, fontWeight = FontWeight.SemiBold)
                    Text(themeLabel, color = Muted, fontSize = 12.sp)
                }
                Switch(
                    modifier = Modifier.semantics {
                        contentDescription = themeLabel
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
                Text(stringResource(R.string.dhikr_counter), color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("$count", color = Foreground, fontSize = 52.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.target_count, target), color = Muted, fontSize = 13.sp)
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
                    Text(stringResource(R.string.count_dhikr), fontWeight = FontWeight.Bold)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = {
                        count = 0
                        preferences.edit().putInt("dhikr_count", 0).apply()
                    }) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Text(" ${stringResource(R.string.reset)}")
                    }
                    TextButton(onClick = {
                        target = if (target == 33) 99 else 33
                        preferences.edit().putInt("dhikr_target", target).apply()
                    }) {
                        Text(stringResource(R.string.set_target, if (target == 33) "99" else "33"))
                    }
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(stringResource(R.string.offline_use), color = Gold, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.offline_note), color = Muted, fontSize = 12.sp)
            }
        }
        OutlinedButton(onClick = onNavigateToPrayer, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.go_to_prayers))
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

private suspend fun fetchPrayerTimes(city: String, locale: Locale): PrayerData = withContext(Dispatchers.IO) {
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
            .ifBlank { SimpleDateFormat("dd MMM yyyy", locale).format(Date()) }

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

private fun prayerLabelResource(name: String): Int = when (name) {
    "İmsak" -> R.string.prayer_fajr
    "Güneş" -> R.string.prayer_sunrise
    "Öğle" -> R.string.prayer_dhuhr
    "İkindi" -> R.string.prayer_asr
    "Akşam" -> R.string.prayer_maghrib
    "Yatsı" -> R.string.prayer_isha
    else -> R.string.prayer_fajr
}

private fun cleanTime(value: String): String = value.substringBefore(" ").trim()
