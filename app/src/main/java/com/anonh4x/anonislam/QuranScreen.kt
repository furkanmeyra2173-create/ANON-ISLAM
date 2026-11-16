package com.anonh4x.anonislam

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun QuranScreen() {
    val context = LocalContext.current
    var library by remember { mutableStateOf<QuranLibrary?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedSurahNumber by rememberSaveable { mutableStateOf<Int?>(null) }
    var showLicense by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(context) {
        try {
            library = withContext(Dispatchers.IO) { QuranRepository.load(context) }
        } catch (_: Exception) {
            hasError = true
        } finally {
            isLoading = false
        }
    }

    BackHandler(enabled = selectedSurahNumber != null) {
        selectedSurahNumber = null
    }

    val currentSurah = selectedSurahNumber?.let { number ->
        library?.surahs?.firstOrNull { it.number == number }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        if (currentSurah == null) {
            Text(
                text = stringResource(R.string.quran_title),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.quran_offline_available),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.quran_search_hint)) }
            )
        } else {
            TextButton(onClick = { selectedSurahNumber = null }) {
                Text(stringResource(R.string.quran_back_to_surahs))
            }
        }

        when {
            isLoading -> {
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    Text(
                        stringResource(R.string.quran_loading),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
            hasError || library == null -> {
                Text(
                    stringResource(R.string.quran_load_error),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
            currentSurah != null -> {
                val verses = library?.ayahsBySurah?.get(currentSurah.number).orEmpty()
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    currentSurah.arabicName,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 27.sp,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    "${currentSurah.number}. ${currentSurah.englishName}",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                Text(
                                    stringResource(R.string.quran_ayah_count, currentSurah.ayahCount),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                    items(verses, key = { it.number }) { ayah ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    ayah.text,
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 25.sp,
                                    lineHeight = 44.sp,
                                    textAlign = TextAlign.End
                                )
                                Text(
                                    stringResource(R.string.quran_ayah_reference, currentSurah.number, ayah.number),
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
            else -> {
                val normalizedQuery = query.trim().lowercase(Locale.ROOT)
                val filteredSurahs = library?.surahs.orEmpty().filter { surah ->
                    normalizedQuery.isBlank() ||
                        surah.englishName.lowercase(Locale.ROOT).contains(normalizedQuery) ||
                        surah.englishTranslation.lowercase(Locale.ROOT).contains(normalizedQuery) ||
                        surah.arabicName.contains(query.trim()) ||
                        surah.number.toString() == normalizedQuery
                }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (filteredSurahs.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.quran_no_results),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 20.dp)
                            )
                        }
                    }
                    items(filteredSurahs, key = { it.number }) { surah ->
                        Card(
                            onClick = { selectedSurahNumber = surah.number },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    surah.number.toString(),
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(
                                        surah.englishName,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        stringResource(R.string.quran_surah_summary, surah.englishTranslation, surah.ayahCount),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                }
                                Text(
                                    surah.arabicName,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 20.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        TextButton(onClick = { showLicense = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(stringResource(R.string.quran_source_credit))
        }
    }

    if (showLicense) {
        AlertDialog(
            onDismissRequest = { showLicense = false },
            title = { Text(stringResource(R.string.quran_source_title)) },
            text = { Text(stringResource(R.string.quran_license_notice)) },
            confirmButton = {
                TextButton(onClick = {
                    showLicense = false
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://tanzil.net")))
                }) {
                    Text(stringResource(R.string.quran_open_source))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLicense = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}
