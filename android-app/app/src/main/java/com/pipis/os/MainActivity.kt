package com.pipis.os

import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope

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

private data class LaunchableApp(
    val label: String,
    val packageName: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PipOSApp() }
    }
}

@Composable
private fun PipOSApp() {
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences("pipos_preferences", android.content.Context.MODE_PRIVATE)
    }
    var selected by remember { mutableStateOf("Главная") }
    var settings by remember {
        mutableStateOf(
            PipOSSettings(
                themeName = preferences.getString("theme_name", "Void Purple") ?: "Void Purple",
                accentColor = preferences.getString("accent_color", "#9B6DFF") ?: "#9B6DFF"
            )
        )
    }
    var wallpaperEnabled by remember {
        mutableStateOf(preferences.getBoolean("wallpaper_enabled", true))
    }
    var status by remember { mutableStateOf("Резервная копия хранит настройки PipOS в ZIP-архиве.") }
    var iconStatus by remember { mutableStateOf("Выбери приложение и изображение для новой иконки.") }
    var appMenuExpanded by remember { mutableStateOf(false) }
    val apps = remember(context) {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        context.packageManager.queryIntentActivities(launcherIntent, 0)
            .mapNotNull { info ->
                val packageName = info.activityInfo?.packageName ?: return@mapNotNull null
                if (packageName == context.packageName) return@mapNotNull null
                LaunchableApp(
                    label = info.loadLabel(context.packageManager).toString(),
                    packageName = packageName
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }
    var chosenApp by remember { mutableStateOf<LaunchableApp?>(null) }
    var chosenImageUri by remember { mutableStateOf<Uri?>(null) }
    var chosenBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val scope = rememberCoroutineScope()
    val accent = remember(settings.accentColor) {
        runCatching { Color(android.graphics.Color.parseColor(settings.accentColor)) }
            .getOrDefault(Color(0xFF9B6DFF))
    }

    LaunchedEffect(settings.themeName, settings.accentColor) {
        preferences.edit()
            .putString("theme_name", settings.themeName)
            .putString("accent_color", settings.accentColor)
            .apply()
    }

    LaunchedEffect(wallpaperEnabled) {
        preferences.edit().putBoolean("wallpaper_enabled", wallpaperEnabled).apply()
    }

    LaunchedEffect(context) {
        val savedPackage = preferences.getString("icon_package", null)
        val savedUri = preferences.getString("icon_uri", null)
        if (savedPackage != null && savedUri != null) {
            chosenApp = apps.firstOrNull { it.packageName == savedPackage }
            chosenImageUri = runCatching { Uri.parse(savedUri) }.getOrNull()
            chosenImageUri?.let { uri ->
                val bitmap = runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                    }
                }.getOrNull()
                chosenBitmap = bitmap
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val bitmap = runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                    }
                }.getOrNull()
                if (bitmap == null) {
                    iconStatus = "Не удалось открыть изображение. Попробуй PNG или JPG."
                } else {
                    runCatching {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }
                    chosenImageUri = uri
                    chosenBitmap = bitmap
                    preferences.edit().putString("icon_uri", uri.toString()).apply()
                    iconStatus = "Изображение загружено. Теперь можно создать ярлык."
                }
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri == null) {
            status = "Экспорт отменён."
        } else {
            scope.launch {
                val result = runCatching {
                    withContext(Dispatchers.IO) { PipOSBackup.export(context, uri, settings) }
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
                    withContext(Dispatchers.IO) { PipOSBackup.import(context, uri) }
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
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text("PipOS", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black)
                Text("Твоя система. Твои правила.", color = Muted, fontSize = 14.sp)
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Text("Центр кастомизации", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text("Настраивай внешний вид Android и собирай собственный набор оформления.", color = Muted, fontSize = 13.sp)
                    }
                }

                Text("ИНСТРУМЕНТЫ", color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                val items = listOf(
                    Tool("Темы", "Цвета и оформление", Icons.Rounded.Brush),
                    Tool("Иконки", "Свои значки приложений", Icons.Rounded.Apps),
                    Tool("Шрифты", "PipisCraft Sans и другие", Icons.Rounded.FontDownload),
                    Tool("Обои", "Фоны рабочего стола", Icons.Rounded.Wallpaper),
                    Tool("Коллекция", "Твои пакеты оформления", Icons.Rounded.Collections),
                    Tool("Резервная копия", "Экспорт и импорт настроек", Icons.Rounded.Backup)
                )
                items.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        pair.forEach { tool -> ToolCard(tool, accent, Modifier.weight(1f)) { selected = tool.title } }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }

                when (selected) {
                    "Резервная копия" -> {
                        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Перенос PipOS", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                Text("Создай ZIP-файл, чтобы перенести текущие настройки на другое устройство.", color = Muted, fontSize = 12.sp)
                                Button(onClick = { exportLauncher.launch("PipOS-backup.zip") }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = accent)) {
                                    Text("Экспортировать ZIP", color = Color.White)
                                }
                                OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Импортировать ZIP", color = Color.White)
                                }
                                Text(status, color = Muted, fontSize = 11.sp)
                            }
                        }
                    }
                    "Темы" -> {
                        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Цветовая тема", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text("Выбери акцентный цвет PipOS. Он сразу применяется к интерфейсу приложения и сохраняется на устройстве.", color = Muted, fontSize = 12.sp)
                                themePresets.chunked(2).forEach { pair ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        pair.forEach { preset ->
                                            val presetColor = Color(android.graphics.Color.parseColor(preset.second))
                                            Card(modifier = Modifier.weight(1f).clickable {
                                                settings = settings.copy(themeName = preset.first, accentColor = preset.second)
                                            }, colors = CardDefaults.cardColors(containerColor = Color(0xFF211A30)), shape = RoundedCornerShape(14.dp)) {
                                                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
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
                    "Иконки" -> {
                        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Автоматические иконки PipOS", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text("PipOS сам создаёт значки для приложений и применяет их в своей сетке лаунчера. YouTube, TikTok, Minecraft и GitHub получают тематические символы, AI-приложения — короны. Для остальных используется автоматически созданная монограмма. Ничего загружать и подтверждать не нужно.", color = Muted, fontSize = 12.sp)
                                Text("Новые приложения появляются в списке лаунчера после установки и обновления рабочего стола.", color = accent, fontSize = 12.sp)
                            }
                        }
                    }
                    "Обои" -> {
                        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Космические обои", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text("Чёрная дыра с глубокими фиолетовыми кольцами. Фон применяется к обеим страницам рабочего стола и сохраняется после перезапуска.", color = Muted, fontSize = 12.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier.weight(1f).height(92.dp).clip(RoundedCornerShape(14.dp))
                                            .background(Color(0xFF08030F))
                                    ) {
                                        BlackHoleWallpaper(Modifier.fillMaxSize())
                                    }
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(if (wallpaperEnabled) "Обои включены" else "Обои выключены", color = accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Button(onClick = { wallpaperEnabled = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = accent)) {
                                            Text("Применить", color = Color.White)
                                        }
                                        OutlinedButton(onClick = { wallpaperEnabled = false }, modifier = Modifier.fillMaxWidth()) {
                                            Text("Отключить", color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "Главная" -> Text("PipOS Android • версия 0.4.2", color = Muted, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    else -> Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(selected, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(when (selected) {
                                "Шрифты" -> "Здесь появится импорт шрифтов и предпросмотр внутри PipOS."
                                "Обои" -> "Здесь появится выбор изображения и установка обоев."
                                else -> "Здесь будут собираться и храниться твои наборы оформления."
                            }, color = Muted, fontSize = 12.sp)
                            OutlinedButton(onClick = { selected = "Главная" }) { Text("На главную", color = Color.White) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxLikeDropdown(
    selectedLabel: String,
    expanded: Boolean,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
    apps: List<LaunchableApp>,
    onSelect: (LaunchableApp) -> Unit
) {
    Column {
        OutlinedButton(onClick = onExpand, modifier = Modifier.fillMaxWidth()) {
            Text(selectedLabel, color = Color.White)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
            apps.take(80).forEach { app ->
                DropdownMenuItem(text = { Text(app.label) }, onClick = { onSelect(app) })
            }
        }
        if (apps.isEmpty()) {
            Text("Не удалось получить список приложений на этом устройстве.", color = Muted, fontSize = 11.sp)
        } else if (apps.size > 80) {
            Text("Показаны первые 80 приложений.", color = Muted, fontSize = 10.sp)
        }
    }
}

private data class Tool(val title: String, val subtitle: String, val icon: ImageVector)

@Composable
private fun ToolCard(tool: Tool, accent: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(tool.icon, contentDescription = null, tint = accent)
            Text(tool.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(tool.subtitle, color = Muted, fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}
