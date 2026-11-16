package com.anonh4x.anonislam

import android.content.Context
import org.json.JSONObject

data class QuranSurah(
    val number: Int,
    val arabicName: String,
    val englishName: String,
    val englishTranslation: String,
    val ayahCount: Int,
    val revelationType: String
)

data class QuranAyah(
    val surahNumber: Int,
    val number: Int,
    val text: String
)

data class QuranLibrary(
    val surahs: List<QuranSurah>,
    val ayahsBySurah: Map<Int, List<QuranAyah>>
)

object QuranRepository {
    fun load(context: Context): QuranLibrary {
        val catalog = context.assets.open("surahs.json").bufferedReader(Charsets.UTF_8).use { reader ->
            val root = JSONObject(reader.readText())
            require(root.optInt("code") == 200) { "Surah catalog is unavailable." }
            val data = root.getJSONArray("data")
            List(data.length()) { index ->
                val item = data.getJSONObject(index)
                QuranSurah(
                    number = item.getInt("number"),
                    arabicName = item.getString("name"),
                    englishName = item.getString("englishName"),
                    englishTranslation = item.getString("englishNameTranslation"),
                    ayahCount = item.getInt("numberOfAyahs"),
                    revelationType = item.getString("revelationType")
                )
            }
        }

        require(catalog.size == 114 && catalog.map { it.number } == (1..114).toList()) {
            "Surah catalog is incomplete."
        }

        val ayahs = context.assets.open("quran-uthmani.txt").bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.mapNotNull { line ->
                if (line.isBlank() || !line.first().isDigit()) return@mapNotNull null
                val fields = line.split('|', limit = 3)
                if (fields.size != 3) return@mapNotNull null
                QuranAyah(
                    surahNumber = fields[0].toInt(),
                    number = fields[1].toInt(),
                    text = fields[2]
                )
            }.toList()
        }

        require(ayahs.size == 6236) { "Quran text is incomplete." }
        val groupedAyahs = ayahs.groupBy { it.surahNumber }
        require(catalog.all { surah ->
            val verses = groupedAyahs[surah.number].orEmpty()
            verses.size == surah.ayahCount && verses.map { it.number } == (1..surah.ayahCount).toList()
        }) { "Quran text does not match the surah catalog." }

        return QuranLibrary(catalog, groupedAyahs)
    }
}
