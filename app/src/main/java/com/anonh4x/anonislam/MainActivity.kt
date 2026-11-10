package com.anonh4x.anonislam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Night = Color(0xFF07110E)
private val Panel = Color(0xFF10231C)
private val Emerald = Color(0xFF19B67A)
private val Gold = Color(0xFFE5C36B)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                background = Night,
                surface = Panel,
                primary = Emerald,
                secondary = Gold
            )) {
                Dashboard()
            }
        }
    }
}

private data class Feature(
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
private fun Dashboard() {
    val features = listOf(
        Feature("Kur'an-ı Kerim", "Sureler ve mealler", Icons.Default.AutoStories),
        Feature("Namaz Vakitleri", "Günlük vakitler", Icons.Default.Mosque),
        Feature("Yapay Zekâ Asistanı", "Kaynaklı cevaplar", Icons.Default.SmartToy),
        Feature("Dijital Tesbih", "Zikirlerini takip et", Icons.Default.Timer),
        Feature("Dualar ve Hadisler", "Günlük hatırlatmalar", Icons.Default.Favorite),
        Feature("Kıble Pusulası", "Yön bulma", Icons.Default.Explore)
    )

    Surface(modifier = Modifier.fillMaxSize(), color = Night) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Text("ANON", color = Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("İSLAM", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
            Text("Bilgiyle, huzurla, güvenilir kaynaklarla.", color = Color.LightGray, fontSize = 13.sp)
            Spacer(Modifier.height(22.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Panel),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("ESSELÂMÜ ALEYKÜM", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("Hoş geldin", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Text("İbadet ve öğrenme yolculuğuna buradan devam et.", color = Color.LightGray, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("KEŞFET", color = Emerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(features) { feature ->
                    FeatureCard(feature)
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(feature: Feature) {
    Card(
        modifier = Modifier.fillMaxWidth().height(142.dp),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(feature.icon, contentDescription = null, tint = Emerald)
            Spacer(Modifier.height(12.dp))
            Text(feature.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(feature.subtitle, color = Color.LightGray, fontSize = 11.sp)
        }
    }
}
