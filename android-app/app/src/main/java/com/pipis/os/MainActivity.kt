package com.pipis.os

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.style.TextAlign

private val Background = Color(0xFF09070F)
private val Panel = Color(0xFF171222)
private val Muted = Color(0xFFB8AEC9)

private val themePresets = listOf(
    "Void Purple" to "#9B6DFF",
    "Cyber Blue" to "#42B8FF",
    "Neon Green" to "#65E68A",
    "Crimson" to "#FF5577",
    "Amber" to "#FFB74D",
    "Ice" to "#75E5E8"
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PipOSApp() }
    }
}

@Composable
private fun PipOSApp() {
    var selected by remember { mutableStateOf("Главная") }
    var settings by remember { mutableStateOf(PipOSSettings()) }
    var status by remember { mutableStateOf("Резервная копия хранит настройки PipOS в ZIP-архиве.") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accent = remember(settings.accentColor) {
        runCatching { Color(android.graphics.Color.parseColor(settings.accentColor)) }
            .getOrDefault(Color(0xFF9B6DFF))
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri == null) {
            status = "Экспорт отменён."
        } else {
            scope.launch {
                val result = runCatching {
                    kotlinx.coroutines.withContext(Dispatchers.IO) {
                        PipOSBackup.export(context, uri, settings)
                    }
                }
                status = result.fold(
                    onSuccess = { "Архив PipOS успешно сохранён." },
                    onFailure = { "Ошибка экспорта: ${it.message ?: "неизвестная ошибка"}" }
                )
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) {
            status = "Импорт отменён."
        } else {
            scope.launch {
                val result = runCatching {
                    kotlinx.coroutines.withContext(Dispatchers.IO) {
                        PipOSBackup.import(context, uri)
                    }
                }
                result.onSuccess {
                    settings = it
                    status = "Настройки успешно восстановлены из архива."
                }.onFailure {
                    status = "Ошибка импорта: ${it.message ?: "неизвестная ошибка"}"
                }
            }
        }
    }

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

                Text("ИНСТРУМЕНТЫ", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                            ToolCard(tool, accent, Modifier.weight(1f)) { selected = tool.title }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }

                when (selected) {
                    "Резервная копия" -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Panel),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Перенос PipOS", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                Text("Создай ZIP-файл, чтобы перенести текущие настройки на другое устройство.", color = Muted, fontSize = 12.sp)
                                Button(
                                    onClick = { exportLauncher.launch("PipOS-backup.zip") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                                ) { Text("Экспортировать ZIP", color = Color.White) }
                                OutlinedButton(
                                    onClick = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Импортировать ZIP", color = Color.White) }
                                Text(status, color = Muted, fontSize = 11.sp)
                            }
                        }
                    }
                    "Темы" -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Panel),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Цветовая тема", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text("Выбери акцентный цвет PipOS. Он сразу применяется к интерфейсу приложения и сохраняется в резервной копии.", color = Muted, fontSize = 12.sp)
                                themePresets.chunked(2).forEach { pair ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        pair.forEach { preset ->
                                            val presetColor = Color(android.graphics.Color.parseColor(preset.second))
                                            Card(
                                                modifier = Modifier.weight(1f).clickable {
                                                    settings = settings.copy(themeName = preset.first, accentColor = preset.second)
                                                },
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFF211A30)),
                                                shape = RoundedCornerShape(14.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Rounded.Brush, contentDescription = null, tint = presetColor)
                                                    Column {
                                                        Text(preset.first, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                        Text(preset.second, color = Muted, fontSize = 10.sp)
                                                    }
                                                }
                                            }
                                        }
                                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                                    }
                                }
                                Text("Активная тема: ${settings.themeName}", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    "Главная" -> Text(
                        "PipOS Android • версия 0.2.0",
                        color = Muted,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    else -> Text(
                        "$selected — этот раздел появится в следующем обновлении.",
                        color = Muted,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private data class Tool(val title: String, val subtitle: String, val icon: ImageVector)

@Composable
private fun ToolCard(tool: Tool, accent: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(tool.icon, contentDescription = null, tint = accent)
            Text(tool.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(tool.subtitle, color = Muted, fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}
