package com.anonh4x.anonislam

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

internal data class QuranSurahSummary(
    val number: Int,
    val arabicName: String,
    val englishName: String,
    val englishMeaning: String,
    val ayahCount: Int
)

internal data class QuranAyah(val number: Int, val text: String)

internal data class QuranSurah(
    val summary: QuranSurahSummary,
    val ayahs: List<QuranAyah>
)

internal object QuranRepository {
    private const val API_BASE = "https://api.alquran.cloud/v1"
    private const val CACHE_DIRECTORY = "quran"

    suspend fun loadSurahList(context: Context): List<QuranSurahSummary> = withContext(Dispatchers.IO) {
        val cache = File(context.filesDir, "$CACHE_DIRECTORY/surahs.json")
        readSurahList(cache)?.let { return@withContext it }
        completeQuranFile(context).takeIf { it.isFile }?.let { file ->
            val summaries = runCatching { parseCompleteQuran(file.readText(Charsets.UTF_8)).map(QuranSurah::summary) }
                .getOrNull()
            if (summaries?.size == 114) return@withContext summaries
        }

        val response = requestJson("$API_BASE/surah")
        val summaries = parseSurahList(response)
        if (summaries.size != 114) {
            throw IllegalStateException("Sure listesi doğrulanamadı. Lütfen daha sonra tekrar dene.")
        }
        writeAtomically(cache, encodeSurahList(summaries).toByteArray(Charsets.UTF_8))
        summaries
    }

    suspend fun loadSurah(context: Context, summary: QuranSurahSummary): QuranSurah =
        withContext(Dispatchers.IO) {
            val completeQuran = completeQuranFile(context)
            if (completeQuran.isFile) {
                val completeCache = readCompleteQuran(completeQuran)
                completeCache?.getOrNull(summary.number - 1)?.let { return@withContext it }
            }

            val cache = surahFile(context, summary.number)
            readSurah(cache)?.let { return@withContext it }

            val response = requestJson("$API_BASE/surah/${summary.number}/quran-uthmani")
            val surah = parseSurah(response)
            if (surah.summary.number != summary.number || surah.ayahs.size != summary.ayahCount) {
                throw IllegalStateException("Sure metni doğrulanamadı. Lütfen daha sonra tekrar dene.")
            }
            writeAtomically(cache, response.toByteArray(Charsets.UTF_8))
            surah
        }

    suspend fun downloadCompleteQuran(context: Context): Long = withContext(Dispatchers.IO) {
        val response = requestJson("$API_BASE/quran/quran-uthmani")
        val surahs = parseCompleteQuran(response)
        if (surahs.size != 114 || surahs.sumOf { it.ayahs.size } != 6_236) {
            throw IllegalStateException("İndirilen Kur’an metni doğrulanamadı; eksik dosya kaydedilmedi.")
        }
        writeAtomically(completeQuranFile(context), response.toByteArray(Charsets.UTF_8))
        response.toByteArray(Charsets.UTF_8).size.toLong()
    }

    fun isCompleteQuranDownloaded(context: Context): Boolean = completeQuranFile(context).isFile

    fun isDownloaded(context: Context, number: Int): Boolean =
        isCompleteQuranDownloaded(context) || surahFile(context, number).isFile

    fun storedSizeBytes(context: Context): Long =
        File(context.filesDir, CACHE_DIRECTORY).walkTopDown().filter { it.isFile }.sumOf { it.length() }

    private fun requestJson(endpoint: String): String {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "ANON-ISLAM-Android")
        }
        try {
            val status = connection.responseCode
            if (status !in 200..299) {
                throw IllegalStateException("Kur’an kaynağına ulaşılamadı (HTTP $status).")
            }
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseSurahList(response: String): List<QuranSurahSummary> {
        val data = JSONObject(response).getJSONArray("data")
        return (0 until data.length()).map { index ->
            val item = data.getJSONObject(index)
            QuranSurahSummary(
                number = item.getInt("number"),
                arabicName = item.getString("name"),
                englishName = item.getString("englishName"),
                englishMeaning = item.getString("englishNameTranslation"),
                ayahCount = item.getInt("numberOfAyahs")
            )
        }
    }

    private fun parseSurah(response: String): QuranSurah =
        parseSurahData(JSONObject(response).getJSONObject("data"))

    private fun parseCompleteQuran(response: String): List<QuranSurah> {
        val items = JSONObject(response).getJSONObject("data").getJSONArray("surahs")
        val surahs = (0 until items.length()).map { index -> parseSurahData(items.getJSONObject(index)) }
        if (
            surahs.size != 114 ||
            surahs.withIndex().any { (index, surah) -> surah.summary.number != index + 1 } ||
            surahs.sumOf { it.ayahs.size } != 6_236
        ) {
            throw IllegalStateException("Surelerin sırası veya ayet sayısı doğrulanamadı.")
        }
        return surahs
    }

    private fun parseSurahData(data: JSONObject): QuranSurah {
        val ayahs = data.getJSONArray("ayahs").let { items ->
            (0 until items.length()).map { index ->
                val ayah = items.getJSONObject(index)
                QuranAyah(ayah.getInt("numberInSurah"), ayah.getString("text"))
            }
        }
        if (ayahs.isEmpty() || ayahs.any { it.text.isBlank() }) {
            throw IllegalStateException("Sure metni eksik veya boş geldi.")
        }
        val summary = QuranSurahSummary(
            number = data.getInt("number"),
            arabicName = data.getString("name"),
            englishName = data.getString("englishName"),
            englishMeaning = data.getString("englishNameTranslation"),
            ayahCount = data.optInt("numberOfAyahs", ayahs.size)
        )
        if (summary.ayahCount != ayahs.size) {
            throw IllegalStateException("Sure ayet sayısı doğrulanamadı.")
        }
        return QuranSurah(summary, ayahs)
    }

    private fun readCompleteQuran(file: File): List<QuranSurah>? = runCatching {
        if (!file.isFile) return null
        parseCompleteQuran(file.readText(Charsets.UTF_8))
    }.getOrNull()

    private fun encodeSurahList(surahs: List<QuranSurahSummary>): String = JSONArray().apply {
        surahs.forEach { surah ->
            put(JSONObject().apply {
                put("number", surah.number)
                put("arabicName", surah.arabicName)
                put("englishName", surah.englishName)
                put("englishMeaning", surah.englishMeaning)
                put("ayahCount", surah.ayahCount)
            })
        }
    }.toString()

    private fun readSurahList(file: File): List<QuranSurahSummary>? = runCatching {
        if (!file.isFile) return null
        val data = JSONArray(file.readText(Charsets.UTF_8))
        val surahs = (0 until data.length()).map { index ->
            val item = data.getJSONObject(index)
            QuranSurahSummary(
                number = item.getInt("number"),
                arabicName = item.getString("arabicName"),
                englishName = item.getString("englishName"),
                englishMeaning = item.getString("englishMeaning"),
                ayahCount = item.getInt("ayahCount")
            )
        }
        surahs.takeIf { it.size == 114 }
    }.getOrNull()

    private fun readSurah(file: File): QuranSurah? = runCatching {
        if (!file.isFile) return null
        parseSurah(file.readText(Charsets.UTF_8))
    }.getOrNull()

    private fun writeAtomically(destination: File, contents: ByteArray) {
        destination.parentFile?.mkdirs()
        val temporary = File(destination.parentFile, "${destination.name}.tmp")
        temporary.writeBytes(contents)
        if (!temporary.renameTo(destination)) {
            temporary.delete()
            throw IllegalStateException("Sure cihazda kaydedilemedi.")
        }
    }

    private fun completeQuranFile(context: Context): File =
        File(context.filesDir, "$CACHE_DIRECTORY/complete-quran-uthmani.json")

    private fun surahFile(context: Context, number: Int): File =
        File(context.filesDir, "$CACHE_DIRECTORY/surah-$number.json")
}
