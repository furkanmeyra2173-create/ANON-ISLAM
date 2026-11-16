package com.anonh4x.anonislam

import android.content.Context
import android.content.Intent
import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.util.Locale

private val QuranMuted = Color(0xFFB8C8C0)
private val QuranGold = Color(0xFFE5C36B)
private val QuranPanel = Color(0xFF10231C)
private val QuranGreen = Color(0xFF19B67A)

@Composable
internal fun QuranScreen(context: Context) {
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val preferences = remember(context) { context.getSharedPreferences("anon_islam", Context.MODE_PRIVATE) }
    var favoriteAyahs by remember {
        mutableStateOf(preferences.getStringSet("quran_favorites", emptySet()).orEmpty().toSet())
    }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    var surahs by remember { mutableStateOf<List<QuranSurahSummary>>(emptyList()) }
    var selectedSurah by remember { mutableStateOf<QuranSurah?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var isLoadingList by remember { mutableStateOf(true) }
    var loadingSurahNumber by remember { mutableStateOf<Int?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var fontSize by rememberSaveable { mutableStateOf(25) }
    var cachedBytes by remember { mutableStateOf(QuranRepository.storedSizeBytes(context)) }
    var isDownloadingComplete by remember { mutableStateOf(false) }
    var hasCompleteQuran by remember { mutableStateOf(QuranRepository.isCompleteQuranDownloaded(context)) }

    LaunchedEffect(context) {
        try {
            surahs = QuranRepository.loadSurahList(context)
        } catch (exception: Exception) {
            error = exception.message ?: "Sure listesi yüklenemedi. İnternet bağlantını kontrol et."
        } finally {
            isLoadingList = false
        }
    }

    BackHandler(enabled = selectedSurah != null) {
        selectedSurah = null
    }

    val openSurah: (QuranSurahSummary) -> Unit = { summary ->
        scope.launch {
            loadingSurahNumber = summary.number
            error = null
            try {
                selectedSurah = QuranRepository.loadSurah(context, summary)
                cachedBytes = QuranRepository.storedSizeBytes(context)
            } catch (exception: Exception) {
                error = exception.message ?: "Sure açılamadı. İndirilen sureleri internet olmadan okuyabilirsin."
            } finally {
                loadingSurahNumber = null
            }
        }
    }

    val filteredSurahs = remember(surahs, query) {
        val normalizedQuery = query.trim().lowercase(Locale.ROOT)
        if (normalizedQuery.isEmpty()) surahs else surahs.filter { surah ->
            surah.number.toString().contains(normalizedQuery) ||
                surah.englishName.lowercase(Locale.ROOT).contains(normalizedQuery) ||
                surah.englishMeaning.lowercase(Locale.ROOT).contains(normalizedQuery) ||
                surah.arabicName.contains(query.trim())
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (selectedSurah == null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AutoStories, contentDescription = null, tint = QuranGold)
                Text(
                    "Kur’an-ı Kerim",
                    modifier = Modifier.weight(1f).padding(start = 10.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(Formatter.formatShortFileSize(context, cachedBytes), color = QuranMuted, fontSize = 11.sp)
            }
            Text(
                "Sureleri tek tek açabilir veya yaklaşık 2,1 MB olan tam metni indirebilirsin (son API yanıtı; boyut değişebilir). İndirilenler bu cihazda çevrimdışı kullanılabilir. Mealler lisans doğrulaması beklediği için şimdilik yalnızca Arapça metin gösterilir.",
                color = QuranMuted,
                fontSize = 12.sp
            )
            Button(
                onClick = {
                    scope.launch {
                        isDownloadingComplete = true
                        error = null
                        try {
                            QuranRepository.downloadCompleteQuran(context)
                            hasCompleteQuran = true
                            cachedBytes = QuranRepository.storedSizeBytes(context)
                        } catch (exception: Exception) {
                            error = exception.message ?: "Kur’an indirilemedi. İnternet bağlantını kontrol et."
                        } finally {
                            isDownloadingComplete = false
                        }
                    }
                },
                enabled = !isDownloadingComplete && !hasCompleteQuran,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = QuranGreen, contentColor = Color(0xFF06150F))
            ) {
                if (isDownloadingComplete) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color(0xFF06150F), strokeWidth = 2.dp)
                    Text("  Kur’an indiriliyor…")
                } else {
                    Icon(Icons.Filled.Download, contentDescription = null)
                    Text(if (hasCompleteQuran) "Tüm Kur’an çevrimdışı hazır" else "Tüm Kur’an’ı indir (114 sure)")
                }
            }
            Text("Cihazda kullanılan alan: ${Formatter.formatShortFileSize(context, cachedBytes)}", color = QuranMuted, fontSize = 11.sp)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Sure ara: ad veya numara") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) }
            )
            if (error != null) Text(error.orEmpty(), color = Color(0xFFFF8A80), fontSize = 12.sp)
            if (isLoadingList) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = QuranGreen)
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    items(filteredSurahs, key = { it.number }) { surah ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { openSurah(surah) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = QuranPanel)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(surah.number.toString().padStart(3, '0'), color = QuranGold, fontWeight = FontWeight.Bold)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(surah.englishName, color = Color.White, fontWeight = FontWeight.SemiBold)
                                    Text("${surah.englishMeaning} · ${surah.ayahCount} ayet", color = QuranMuted, fontSize = 11.sp)
                                }
                                Text(surah.arabicName, color = QuranGold, fontSize = 20.sp)
                                if (QuranRepository.isDownloaded(context, surah.number)) {
                                    Icon(Icons.Filled.Download, contentDescription = "Çevrimdışı kayıtlı", tint = QuranGreen)
                                } else if (loadingSurahNumber == surah.number) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = QuranGreen, strokeWidth = 2.dp)
                                }
                            }
                        }
                    }
                }
            }
            TextButton(onClick = { uriHandler.openUri("https://tanzil.net") }) {
                Text("Metin: Tanzil Project · CC BY 3.0 · Kaynak: AlQuran Cloud", color = QuranMuted, fontSize = 10.sp)
            }
        } else {
            val surah = selectedSurah ?: return@Column
            val displayedAyahs = remember(surah, favoritesOnly, favoriteAyahs) {
                if (favoritesOnly) {
                    surah.ayahs.filter { "${surah.summary.number}:${it.number}" in favoriteAyahs }
                } else {
                    surah.ayahs
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selectedSurah = null }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Sure listesine dön", tint = QuranGold)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("${surah.summary.number}. ${surah.summary.englishName}", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("${surah.summary.arabicName} · ${surah.ayahs.size} ayet", color = QuranMuted, fontSize = 12.sp)
                }
                TextButton(onClick = { fontSize = (fontSize - 2).coerceAtLeast(18) }) { Text("A−", color = QuranGold) }
                TextButton(onClick = { fontSize = (fontSize + 2).coerceAtMost(40) }) { Text("A+", color = QuranGold) }
            }
            val favoriteCountInSurah = surah.ayahs.count { "${surah.summary.number}:${it.number}" in favoriteAyahs }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Arapça metin · Çevrimdışı kayıtlı", modifier = Modifier.weight(1f), color = QuranMuted, fontSize = 11.sp)
                TextButton(onClick = { favoritesOnly = !favoritesOnly }) {
                    Text(if (favoritesOnly) "Tüm ayetler" else "Favoriler ($favoriteCountInSurah)", color = QuranGold, fontSize = 11.sp)
                }
            }
            if (displayedAyahs.isEmpty()) {
                Text("Bu surede henüz favori ayetin yok.", color = QuranMuted, fontSize = 13.sp)
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(displayedAyahs, key = { it.number }) { ayah ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = QuranPanel)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                ayah.text,
                                modifier = Modifier.fillMaxWidth(),
                                style = TextStyle(
                                    fontSize = fontSize.sp,
                                    lineHeight = (fontSize * 1.9).sp,
                                    textDirection = TextDirection.Rtl
                                ),
                                textAlign = TextAlign.Right,
                                color = Color.White
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Ayet ${ayah.number}", modifier = Modifier.weight(1f), color = QuranGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                val favoriteKey = "${surah.summary.number}:${ayah.number}"
                                IconButton(onClick = {
                                    val updated = if (favoriteKey in favoriteAyahs) favoriteAyahs - favoriteKey else favoriteAyahs + favoriteKey
                                    favoriteAyahs = updated
                                    preferences.edit().putStringSet("quran_favorites", updated.toMutableSet()).apply()
                                }) {
                                    val isFavorite = favoriteKey in favoriteAyahs
                                    Icon(
                                        if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                        contentDescription = if (isFavorite) "Favorilerden çıkar" else "Favorilere ekle",
                                        tint = if (isFavorite) QuranGold else QuranMuted
                                    )
                                }
                                IconButton(onClick = {
                                    val shareText = "${surah.summary.englishName} ${surah.summary.number}:${ayah.number}\n${ayah.text}\nKaynak: Tanzil Project (CC BY 3.0), AlQuran Cloud"
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Ayet paylaş"))
                                }) {
                                    Icon(Icons.Filled.Share, contentDescription = "Ayeti paylaş", tint = QuranMuted)
                                }
                            }
                        }
                    }
                }
            }
            }
            TextButton(onClick = { uriHandler.openUri("https://tanzil.net") }) {
                Text("Tanzil Project · CC BY 3.0 · Metin değiştirilmeden gösterilir", color = QuranMuted, fontSize = 10.sp)
            }
        }
    }
}
