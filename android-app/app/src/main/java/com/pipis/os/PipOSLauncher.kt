package com.pipis.os

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val LauncherBackground = Color(0xFF08060D)
private val LauncherPanel = Color(0xFF171020)
private val LauncherAccent = Color(0xFFB58AFF)
private val LauncherText = Color(0xFFF7F2FF)
private val LauncherMuted = Color(0xFFB8AEC9)

private data class LauncherApp(val label: String, val packageName: String)
private data class DesktopEntry(val title: String, val children: List<String> = emptyList()) {
    val isFolder: Boolean get() = children.isNotEmpty()
}

private val trashApps = listOf(
    "YT Music", "Галерея", "Калькулятор", "Файлы", "Погода", "Диспетчер файлов",
    "Диктофон", "Заметки", "Часы", "Камера", "Календарь", "Плеер Visha", "SIM-меню",
    "Умное сканирование", "Центр приложений", "My Health", "Сообщения", "Ассистент",
    "Play Games", "Google Карты", "Контакты", "Игры", "Soplay", "HiParty", "FM",
    "Документы", "Яндекс карты", "Hi Тема", "Клонирование телефона", "Carclare",
    "Auto clicker", "SHAREit"
)

private val myProjectsApps = listOf("Pipis Binnary Translator", "PipVault")
private val vpnApps = listOf("Happ", "V2RayTun", "Zip VPN")
private val fileManagerApps = listOf("PipisEdit", "Files", "Zarchiver", "Файлы", "CX проводник")
private val aiApps = listOf("ChatGPT", "Claude", "Grok", "Gemini", "DeepSeek", "Pipis-AI")
private val creatorApps = listOf("MAM", "Blockbench", "Termux", "Pixel studio", "GitHub")
private val bankApps = listOf("Т-банк", "Альфа Банк")
private val antivirusApps = listOf("Диспетчер", "AppWatch", "Kaspersky")

private val desktopRows = listOf(
    listOf(
        DesktopEntry("XBOX"), DesktopEntry("Заморозка"),
        DesktopEntry("Lotus Lantern"), DesktopEntry("Настройки")
    ),
    listOf(
        DesktopEntry("Zaycev.net"), DesktopEntry("Мусор", trashApps),
        DesktopEntry("Моя школа"), DesktopEntry("Телефон")
    ),
    listOf(
        DesktopEntry("MyProjects", myProjectsApps), DesktopEntry("VPN", vpnApps),
        DesktopEntry("Files Managers", fileManagerApps)
    ),
    listOf(
        DesktopEntry("Штаб ИИ", aiApps), DesktopEntry("Creators", creatorApps),
        DesktopEntry("Banks", bankApps), DesktopEntry("Antivirus", antivirusApps)
    ),
    listOf(DesktopEntry("PipOS"))
)

private val dockFolders = listOf(
    DesktopEntry("Video", listOf("YouTube", "TikTok", "CapCut", "Творческая студия YouTube")),
    DesktopEntry("Games", listOf("Minecraft", "Brawl Stars", "Tomb of the mask", "Roblox", "Shadow Knight", "GB27", "Постройки для Minecraft")),
    DesktopEntry("Messengers", listOf("Telegram", "WhatsApp", "MAX")),
    DesktopEntry("Browsers", listOf("Фото", "Gmail", "Google Play", "Яндекс", "Яндекс браузер", "Google", "Chrome", "Uptodown")),
    DesktopEntry("Marketplaces", listOf("OZON", "Wildberries"))
)

class HomeScreenActivity : ComponentActivity() {
    private var launcherApps by mutableStateOf<List<LauncherApp>>(emptyList())
    private var wallpaperEnabled by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompatCompat.configure(window)
        setContent {
            MaterialTheme {
                PipOSLauncher(
                    apps = launcherApps,
                    showWallpaper = wallpaperEnabled,
                    onLaunch = { packageName ->
                        packageManager.getLaunchIntentForPackage(packageName)?.let { startActivity(it) }
                    },
                    onOpenSettings = { startActivity(Intent(this, MainActivity::class.java)) }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        wallpaperEnabled = getSharedPreferences("pipos_preferences", MODE_PRIVATE)
            .getBoolean("wallpaper_enabled", true)
        lifecycleScope.launch {
            val loadedApps = withContext(Dispatchers.IO) { loadLauncherApps() }
            launcherApps = loadedApps
        }
    }

    @Suppress("DEPRECATION")
    private fun loadLauncherApps(): List<LauncherApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val results = if (Build.VERSION.SDK_INT >= 33) {
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            packageManager.queryIntentActivities(intent, 0)
        }
        return results.mapNotNull { info ->
            val activityInfo = info.activityInfo ?: return@mapNotNull null
            if (activityInfo.packageName == packageName) return@mapNotNull null
            LauncherApp(info.loadLabel(packageManager).toString(), activityInfo.packageName)
        }.distinctBy { it.packageName }.sortedBy { it.label.lowercase(Locale.getDefault()) }
    }
}

private object WindowCompatCompat {
    fun configure(window: android.view.Window) {
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, true)
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        window.navigationBarColor = android.graphics.Color.BLACK
        window.statusBarColor = android.graphics.Color.rgb(8, 6, 13)
    }
}

@Composable
private fun PipOSLauncher(
    apps: List<LauncherApp>,
    showWallpaper: Boolean,
    onLaunch: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    var page by remember { mutableStateOf(0) }
    var openedFolder by remember { mutableStateOf<DesktopEntry?>(null) }
    var dragTotal by remember { mutableStateOf(0f) }

    Surface(modifier = Modifier.fillMaxSize(), color = LauncherBackground) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (showWallpaper) {
                BlackHoleWallpaper(Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.30f)))
            }
            Column(
            modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, amount -> dragTotal += amount },
                    onDragEnd = {
                        if (dragTotal < -55f) page = 1
                        if (dragTotal > 55f) page = 0
                        dragTotal = 0f
                    }
                )
            }
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (page == 0) {
                    ClockOnlyScreen()
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("PipOS", color = LauncherText, fontSize = 23.sp, fontWeight = FontWeight.Black)
                                Text("YOUR SPACE", color = LauncherAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp)
                            }
                            TextButton(onClick = onOpenSettings) { Text("Настройки", color = LauncherAccent) }
                        }
                        desktopRows.forEachIndexed { rowIndex, row ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 15.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                row.forEach { entry ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        DesktopTile(
                                            entry = entry,
                                            isAi = entry.title == "Штаб ИИ",
                                            app = findApp(entry.title, apps),
                                            onOpenFolder = { openedFolder = entry },
                                            onLaunch = onLaunch,
                                            onOpenSettings = onOpenSettings
                                        )
                                    }
                                }
                                repeat((4 - row.size).coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                dockFolders.forEach { folder ->
                    Box(modifier = Modifier.weight(1f)) {
                        DockFolder(folder = folder, onClick = { openedFolder = folder })
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(2) { index ->
                    Box(
                        modifier = Modifier.padding(horizontal = 4.dp)
                            .size(if (page == index) 7.dp else 5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (page == index) LauncherAccent else LauncherMuted.copy(alpha = 0.45f))
                    )
                }
            }
            }
        }
    }

    openedFolder?.let { folder ->
        FolderDialog(
            folder = folder,
            apps = apps,
            onDismiss = { openedFolder = null },
            onLaunch = { packageName ->
                openedFolder = null
                onLaunch(packageName)
            }
        )
    }
}


@Composable
internal fun BlackHoleWallpaper(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRect(Color(0xFF030108))
        val center = Offset(size.width * 0.5f, size.height * 0.59f)
        val radius = size.minDimension * 0.37f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF7B20E8).copy(alpha = 0.46f), Color(0xFF25064A).copy(alpha = 0.22f), Color.Transparent),
                center = center,
                radius = radius * 1.85f
            ),
            radius = radius * 1.85f,
            center = center
        )
        rotate(17f, pivot = center) {
            val ringColors = listOf(Color(0xFF3A087A), Color(0xFFC15BFF), Color(0xFF5D16C9), Color(0xFFE5B5FF), Color(0xFF32065F))
            drawOval(
                brush = Brush.linearGradient(ringColors),
                topLeft = Offset(center.x - radius * 1.75f, center.y - radius * 0.58f),
                size = androidx.compose.ui.geometry.Size(radius * 3.5f, radius * 1.16f),
                style = Stroke(width = radius * 0.20f)
            )
            drawOval(
                brush = Brush.linearGradient(listOf(Color(0xFF17032F), Color(0xFFB34BFF), Color(0xFF5B13C5), Color(0xFF160226))),
                topLeft = Offset(center.x - radius * 1.55f, center.y - radius * 0.43f),
                size = androidx.compose.ui.geometry.Size(radius * 3.1f, radius * 0.86f),
                style = Stroke(width = radius * 0.075f)
            )
            drawOval(
                color = Color(0xFFD7A0FF).copy(alpha = 0.82f),
                topLeft = Offset(center.x - radius * 1.42f, center.y - radius * 0.30f),
                size = androidx.compose.ui.geometry.Size(radius * 2.84f, radius * 0.60f),
                style = Stroke(width = radius * 0.018f)
            )
        }
        drawCircle(Color(0xFF020104), radius = radius * 0.72f, center = center)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.Black, Color(0xFF05010A), Color(0xFF35105A).copy(alpha = 0.8f), Color.Transparent),
                center = center,
                radius = radius * 0.94f
            ),
            radius = radius * 0.94f,
            center = center
        )
        val stars = listOf(
            0.08f to 0.12f, 0.19f to 0.24f, 0.83f to 0.14f, 0.72f to 0.32f,
            0.12f to 0.48f, 0.91f to 0.56f, 0.25f to 0.78f, 0.82f to 0.86f,
            0.48f to 0.08f, 0.61f to 0.93f, 0.04f to 0.91f, 0.96f to 0.38f
        )
        stars.forEachIndexed { index, point ->
            drawCircle(
                color = if (index % 3 == 0) Color(0xFFD8A8FF).copy(alpha = 0.9f) else Color(0xFF9B58E8).copy(alpha = 0.65f),
                radius = if (index % 3 == 0) 2.2f else 1.2f,
                center = Offset(size.width * point.first, size.height * point.second)
            )
        }
    }
}

@Composable
private fun ClockOnlyScreen() {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(60_000 - (System.currentTimeMillis() % 60_000))
        }
    }
    val timeText = remember(currentTime) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(currentTime)) }
    val dateText = remember(currentTime) {
        SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date(currentTime))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(bottom = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(timeText, color = LauncherText, fontSize = 76.sp, lineHeight = 82.sp, fontWeight = FontWeight.Light)
        Text(dateText, color = LauncherAccent, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DesktopTile(
    entry: DesktopEntry,
    isAi: Boolean,
    app: LauncherApp?,
    onOpenFolder: () -> Unit,
    onLaunch: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp))
            .clickable {
                when {
                    entry.isFolder -> onOpenFolder()
                    entry.title == "PipOS" -> onOpenSettings()
                    app != null -> onLaunch(app.packageName)
                }
            }.padding(vertical = 5.dp, horizontal = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (entry.isFolder) {
            FolderIcon(entry.title, Modifier.size(54.dp))
        } else {
            AppIcon(entry.title, isAi, Modifier.size(54.dp))
        }
        Spacer(Modifier.height(5.dp))
        Text(
            entry.title, color = LauncherText, fontSize = 9.sp,
            lineHeight = 11.sp, textAlign = TextAlign.Center, maxLines = 2
        )
    }
}

@Composable
private fun DockFolder(folder: DesktopEntry, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
            .clickable(onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FolderIcon(folder.title, Modifier.size(43.dp))
        Text(folder.title, color = LauncherText, fontSize = 8.sp, maxLines = 1, textAlign = TextAlign.Center)
    }
}

@Composable
private fun FolderDialog(
    folder: DesktopEntry,
    apps: List<LauncherApp>,
    onDismiss: () -> Unit,
    onLaunch: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LauncherPanel,
        title = { Text(folder.title, color = LauncherText, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 430.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                items(folder.children) { name ->
                    val app = findApp(name, apps)
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = app != null) { app?.let { onLaunch(it.packageName) } }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIcon(name, name in aiApps, Modifier.size(39.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(name, color = LauncherText, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        if (app == null) Text("не найдено", color = LauncherMuted, fontSize = 9.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть", color = LauncherAccent) }
        }
    )
}

@Composable
private fun FolderIcon(label: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(156, 77, 255)
                style = Paint.Style.FILL
            }
            val path = android.graphics.Path().apply {
                moveTo(size.width * .12f, size.height * .29f)
                lineTo(size.width * .43f, size.height * .29f)
                lineTo(size.width * .53f, size.height * .40f)
                lineTo(size.width * .88f, size.height * .40f)
                lineTo(size.width * .88f, size.height * .80f)
                quadTo(size.width * .88f, size.height * .88f, size.width * .79f, size.height * .88f)
                lineTo(size.width * .17f, size.height * .88f)
                quadTo(size.width * .10f, size.height * .88f, size.width * .10f, size.height * .80f)
                close()
            }
            drawContext.canvas.nativeCanvas.drawPath(path, p)
        }
        Text(label.take(1).uppercase(Locale.getDefault()), color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun AppIcon(label: String, isAi: Boolean, modifier: Modifier = Modifier) {
    val symbol = remember(label) { iconSymbol(label) }
    val bg = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            style = Paint.Style.FILL
        }
    }
    val purple = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(156, 77, 255)
            style = Paint.Style.FILL
        }
    }
    val glyph = remember(symbol) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
            textSize = when {
                symbol.length >= 4 -> 14f
                symbol.length == 3 -> 18f
                symbol.length == 2 -> 24f
                else -> 32f
            }
        }
    }
    Canvas(modifier = modifier) {
        val canvas = drawContext.canvas.nativeCanvas
        val save = canvas.save()
        canvas.scale(size.width / 108f, size.height / 108f)
        canvas.drawRoundRect(2f, 2f, 106f, 106f, 25f, 25f, bg)
        val path = android.graphics.Path().apply {
            moveTo(30f, 35f); lineTo(48f, 35f); lineTo(54f, 42f); lineTo(78f, 42f)
            cubicTo(79.7f, 42f, 81f, 43.3f, 81f, 45f); lineTo(81f, 73f)
            cubicTo(81f, 74.7f, 79.7f, 76f, 78f, 76f); lineTo(30f, 76f)
            cubicTo(28.3f, 76f, 27f, 74.7f, 27f, 73f); lineTo(27f, 38f)
            cubicTo(27f, 36.3f, 28.3f, 35f, 30f, 35f); close()
        }
        canvas.drawPath(path, purple)
        val baseline = 59f - (glyph.ascent() + glyph.descent()) / 2f
        canvas.drawText(symbol, 54f, baseline, glyph)
        if (isAi) {
            val badge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(224, 190, 255)
                style = Paint.Style.FILL
            }
            canvas.drawCircle(87f, 20f, 13f, badge)
            val star = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(75, 28, 120)
                textAlign = Paint.Align.CENTER
                textSize = 19f
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText("★", 87f, 27f, star)
        }
        canvas.restoreToCount(save)
    }
}

private fun findApp(requested: String, apps: List<LauncherApp>): LauncherApp? {
    val key = normalize(requested)
    val aliases = when (key) {
        "yt music" -> listOf("youtube music", "yt music")
        "google play" -> listOf("play store", "google play")
        "тбанк" -> listOf("т-банк", "тинькофф")
        "телефон" -> listOf("телефон", "phone")
        "фото" -> listOf("фото", "photos", "галерея")
        "заморозка" -> listOf("заморозка", "freeze")
        "lotus lantern" -> listOf("lotus lantern")
        "pipis" -> listOf("pipos")
        else -> listOf(key)
    }
    return apps.firstOrNull { normalize(it.label) in aliases }
        ?: apps.firstOrNull { candidate ->
            val label = normalize(candidate.label)
            label.length > 3 && aliases.any { alias ->
                alias.length > 3 && (label.contains(alias) || alias.contains(label))
            }
        }
}

private fun normalize(value: String): String =
    value.lowercase(Locale.ROOT).replace("ё", "е").replace(Regex("[^\\p{L}\\p{N}]"), "")

private fun iconSymbol(label: String): String {
    val value = normalize(label)
    return when {
        "youtube" in value || value == "ytmusic" -> "YT"
        "tiktok" in value -> "TT"
        "minecraft" in value -> "MC"
        "chrome" in value -> "CH"
        "telegram" in value -> "TG"
        "whatsapp" in value -> "WA"
        "instagram" in value -> "IG"
        "gmail" in value -> "GM"
        "googleplay" in value || "playstore" in value -> "GP"
        "googlemaps" in value || "googleкарты" in value || "яндекскарты" in value -> "MAP"
        "google" in value -> "G"
        "github" in value -> "GH"
        "chatgpt" in value -> "GPT"
        "claude" in value -> "CL"
        "gemini" in value -> "GE"
        "deepseek" in value -> "DS"
        "grok" in value -> "GK"
        "pipis" in value -> "PP"
        "folder" in value || "папка" in value -> "DIR"
        else -> label.trim().take(2).uppercase(Locale.getDefault()).ifBlank { "?" }
    }
}
