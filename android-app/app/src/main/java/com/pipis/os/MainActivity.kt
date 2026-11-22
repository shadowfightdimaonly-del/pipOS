package com.pipis.os

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Background = Color(0xFF09070F)
private val Panel = Color(0xFF171222)
private val Purple = Color(0xFF9B6DFF)
private val Muted = Color(0xFFB8AEC9)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PipOSApp() }
    }
}

@Composable
private fun PipOSApp() {
    var selected by remember { mutableStateOf("Главная") }
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Background) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text("PipOS", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black)
                Text("Твоя система. Твои правила.", color = Muted, fontSize = 14.sp)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Panel),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text("Центр кастомизации", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text("Настраивай внешний вид Android и собирай собственный набор оформления.", color = Muted, fontSize = 13.sp)
                    }
                }
                Text("ИНСТРУМЕНТЫ", color = Purple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                val items = listOf(
                    Tool("Темы", "Цвета и оформление", Icons.Rounded.Brush),
                    Tool("Иконки", "Набор значков приложений", Icons.Rounded.Apps),
                    Tool("Шрифты", "PipisCraft Sans и другие", Icons.Rounded.FontDownload),
                    Tool("Обои", "Фоны рабочего стола", Icons.Rounded.Wallpaper),
                    Tool("Коллекция", "Твои пакеты оформления", Icons.Rounded.Collections),
                    Tool("Резервная копия", "Экспорт и импорт настроек", Icons.Rounded.Backup)
                )
                items.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        pair.forEach { tool ->
                            ToolCard(tool, Modifier.weight(1f)) { selected = tool.title }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(
                    if (selected == "Главная") "PipOS Android • ранняя сборка 0.1.0"
                    else "$selected — скоро здесь появятся настройки",
                    color = Muted,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

private data class Tool(val title: String, val subtitle: String, val icon: ImageVector)

@Composable
private fun ToolCard(tool: Tool, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(tool.icon, contentDescription = null, tint = Purple)
            Text(tool.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(tool.subtitle, color = Muted, fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}
