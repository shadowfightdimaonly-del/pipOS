package com.pipis.os

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
import org.json.JSONArray
import org.json.JSONObject

private val LauncherBackground = Color(0xFF08060D)
private val LauncherPanel = Color(0xFF171020)
private val LauncherAccent = Color(0xFFB58AFF)
private val LauncherText = Color(0xFFF7F2FF)
private val LauncherMuted = Color(0xFFB8AEC9)

private data class LauncherApp(
    val label: String,
    val packageName: String
)
private data class DesktopEntry(
    val title: String,
    val children: List<String> = emptyList(),
    val folder: Boolean = false
) {
    val isFolder: Boolean get() = folder || children.isNotEmpty()
}

private class AppLookup(apps: List<LauncherApp>) {
    private val normalizedApps = apps.map { it to normalize(it.label) }
    private val exactApps = HashMap<String, LauncherApp>(apps.size).apply {
        normalizedApps.forEach { (app, key) -> putIfAbsent(key, app) }
    }

    fun find(requested: String): LauncherApp? {
        val key = normalize(requested)
        val aliases = when (key) {
            "ytmusic" -> listOf("youtubemusic", "ytmusic")
            "googleplay" -> listOf("playstore", "googleplay")
            "тбанк" -> listOf("тбанк", "тинькофф")
            "телефон" -> listOf("телефон", "phone")
            "фото" -> listOf("фото", "photos", "галерея")
            "заморозка" -> listOf("заморозка", "freeze")
            "lotuslantern" -> listOf("lotuslantern")
            "pipis" -> listOf("pipos")
            else -> listOf(key)
        }

        aliases.forEach { alias -> exactApps[alias]?.let { return it } }
        return normalizedApps.firstOrNull { (_, label) ->
            label.length > 3 && aliases.any { alias ->
                alias.length > 3 && (label.contains(alias) || alias.contains(label))
            }
        }?.first
    }
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

private val defaultDesktopRows = listOf(
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
    private var desktopLayout by mutableStateOf(defaultDesktopRows)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        desktopLayout = DesktopLayoutStore.load(getSharedPreferences("pipos_preferences", MODE_PRIVATE))
        WindowCompatCompat.configure(window)
        setContent {
            MaterialTheme {
                PipOSLauncher(
                    apps = launcherApps,
                    desktopRows = desktopLayout,
                    onDesktopRowsChanged = { updated ->
                        desktopLayout = updated
                        DesktopLayoutStore.save(
                            getSharedPreferences("pipos_preferences", MODE_PRIVATE),
                            updated
                        )
                    },
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
    desktopRows: List<List<DesktopEntry>>,
    onDesktopRowsChanged: (List<List<DesktopEntry>>) -> Unit,
    showWallpaper: Boolean,
    onLaunch: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val page = pagerState.currentPage
    val appLookup = remember(apps) { AppLookup(apps) }
    var openedFolder by remember { mutableStateOf<DesktopEntry?>(null) }
    var editingPosition by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var showCreateFolder by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var showAppPicker by remember { mutableStateOf(false) }
    var appPickerFolder by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    fun updateEntry(position: Pair<Int, Int>, transform: (DesktopEntry) -> DesktopEntry) {
        val updated = desktopRows.mapIndexed { rowIndex, row ->
            if (rowIndex != position.first) row
            else row.mapIndexed { entryIndex, entry ->
                if (entryIndex == position.second) transform(entry) else entry
            }
        }
        onDesktopRowsChanged(updated)
    }

    fun appendEntry(entry: DesktopEntry) {
        val updated = desktopRows.toMutableList()
        if (updated.isEmpty() || updated.last().size >= 4) updated.add(listOf(entry))
        else updated[updated.lastIndex] = updated.last() + entry
        onDesktopRowsChanged(updated)
    }

    fun removeEntry(position: Pair<Int, Int>) {
        val updated = desktopRows.mapIndexedNotNull { rowIndex, row ->
            if (rowIndex != position.first) row
            else row.filterIndexed { entryIndex, _ -> entryIndex != position.second }.takeIf { it.isNotEmpty() }
        }
        onDesktopRowsChanged(updated)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = LauncherBackground) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (showWallpaper) {
                BlackHoleWallpaper(Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.30f)))
            }

            Column(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    beyondViewportPageCount = 1
                ) { pageIndex ->
                    if (pageIndex == 0) {
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(onClick = { showCreateFolder = true }) {
                                        Text("+ Папка", color = LauncherAccent, fontSize = 11.sp)
                                    }
                                    TextButton(onClick = {
                                        appPickerFolder = null
                                        showAppPicker = true
                                    }) {
                                        Text("+ App", color = LauncherAccent, fontSize = 11.sp)
                                    }
                                    TextButton(onClick = onOpenSettings) {
                                        Text("Настройки", color = LauncherAccent, fontSize = 11.sp)
                                    }
                                }
                            }
                            desktopRows.forEachIndexed { rowIndex, row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 15.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    row.forEachIndexed { entryIndex, entry ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            DesktopTile(
                                                entry = entry,
                                                isAi = entry.title == "Штаб ИИ",
                                                app = appLookup.find(entry.title),
                                                onLongPress = { editingPosition = rowIndex to entryIndex },
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
            appLookup = appLookup,
            onDismiss = { openedFolder = null },
            onLaunch = { packageName ->
                openedFolder = null
                onLaunch(packageName)
            }
        )
    }

    editingPosition?.let { position ->
        val entry = desktopRows.getOrNull(position.first)?.getOrNull(position.second)
        if (entry != null) {
            DesktopEntryEditorDialog(
                entry = entry,
                onDismiss = { editingPosition = null },
                onSave = { newTitle ->
                    updateEntry(position) { it.copy(title = newTitle.trim()) }
                    editingPosition = null
                },
                onDelete = {
                    removeEntry(position)
                    editingPosition = null
                },
                onRemoveChild = { child ->
                    updateEntry(position) { current ->
                        current.copy(children = current.children.filterNot { it == child }, folder = true)
                    }
                },
                onAddApp = {
                    appPickerFolder = position
                    editingPosition = null
                    showAppPicker = true
                }
            )
        }
    }

    if (showCreateFolder) {
        AlertDialog(
            onDismissRequest = { showCreateFolder = false },
            containerColor = LauncherPanel,
            title = { Text("Новая папка", color = LauncherText) },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    singleLine = true,
                    label = { Text("Название папки") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = newFolderName.trim()
                    if (name.isNotEmpty()) appendEntry(DesktopEntry(name, folder = true))
                    newFolderName = ""
                    showCreateFolder = false
                }) { Text("Создать", color = LauncherAccent) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolder = false }) { Text("Отмена", color = LauncherMuted) }
            }
        )
    }

    if (showAppPicker) {
        AppPickerDialog(
            apps = apps,
            onDismiss = { showAppPicker = false; appPickerFolder = null },
            onSelect = { selectedApp ->
                val target = appPickerFolder
                if (target == null) {
                    if (desktopRows.flatten().none { !it.isFolder && it.title == selectedApp.label }) {
                        appendEntry(DesktopEntry(selectedApp.label))
                    }
                } else {
                    updateEntry(target) { folder ->
                        if (folder.children.contains(selectedApp.label)) folder
                        else folder.copy(children = folder.children + selectedApp.label, folder = true)
                    }
                }
                showAppPicker = false
                appPickerFolder = null
            }
        )
    }
}

@Composable
internal fun BlackHoleWallpaper(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.pipos_blackhole_wallpaper),
        contentDescription = "Фиолетовая чёрная дыра",
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DesktopTile(
    entry: DesktopEntry,
    isAi: Boolean,
    app: LauncherApp?,
    onLongPress: () -> Unit,
    onOpenFolder: () -> Unit,
    onLaunch: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp))
            .combinedClickable(
                onClick = {
                    when {
                        entry.isFolder -> onOpenFolder()
                        entry.title == "PipOS" -> onOpenSettings()
                        app != null -> onLaunch(app.packageName)
                    }
                },
                onLongClick = onLongPress
            ).padding(vertical = 5.dp, horizontal = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (entry.isFolder) {
            FolderIcon(entry.title, Modifier.size(54.dp))
        } else {
            AppIcon(entry.title, isAi, app, Modifier.size(54.dp))
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
    appLookup: AppLookup,
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
                    val app = appLookup.find(name)
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = app != null) { app?.let { onLaunch(it.packageName) } }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIcon(name, name in aiApps, app, Modifier.size(39.dp))
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
private fun DesktopEntryEditorDialog(
    entry: DesktopEntry,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
    onRemoveChild: (String) -> Unit,
    onAddApp: () -> Unit
) {
    var title by remember(entry.title) { mutableStateOf(entry.title) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LauncherPanel,
        title = { Text(if (entry.isFolder) "Редактировать папку" else "Редактировать приложение", color = LauncherText) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (entry.isFolder) {
                    Text("Приложения в папке", color = LauncherAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    entry.children.forEach { child ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(child, color = LauncherText, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            TextButton(onClick = { onRemoveChild(child) }) {
                                Text("Убрать", color = LauncherMuted, fontSize = 11.sp)
                            }
                        }
                    }
                    TextButton(onClick = onAddApp) { Text("+ Добавить приложение", color = LauncherAccent) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (title.trim().isNotEmpty()) onSave(title) }) {
                Text("Сохранить", color = LauncherAccent)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("Удалить", color = Color(0xFFFF7A91)) }
                TextButton(onClick = onDismiss) { Text("Отмена", color = LauncherMuted) }
            }
        }
    )
}

@Composable
private fun AppPickerDialog(
    apps: List<LauncherApp>,
    onDismiss: () -> Unit,
    onSelect: (LauncherApp) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LauncherPanel,
        title = { Text("Добавить приложение", color = LauncherText) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 430.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(apps, key = { it.packageName }) { app ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .clickable { onSelect(app) }.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIcon(app.label, false, app, Modifier.size(34.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(app.label, color = LauncherText, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть", color = LauncherAccent) }
        }
    )
}

private object DesktopLayoutStore {
    fun load(preferences: SharedPreferences): List<List<DesktopEntry>> {
        val raw = preferences.getString("desktop_layout_v1", null) ?: return defaultDesktopRows
        return runCatching {
            val root = JSONArray(raw)
            buildList {
                for (rowIndex in 0 until root.length()) {
                    val rowJson = root.getJSONArray(rowIndex)
                    val row = buildList {
                        for (entryIndex in 0 until rowJson.length()) {
                            val item = rowJson.getJSONObject(entryIndex)
                            val childrenJson = item.optJSONArray("children") ?: JSONArray()
                            val children = buildList {
                                for (childIndex in 0 until childrenJson.length()) add(childrenJson.getString(childIndex))
                            }
                            add(DesktopEntry(
                                title = item.optString("title"),
                                children = children,
                                folder = item.optBoolean("folder", children.isNotEmpty())
                            ))
                        }
                    }
                    if (row.isNotEmpty()) add(row)
                }
            }.ifEmpty { defaultDesktopRows }
        }.getOrDefault(defaultDesktopRows)
    }

    fun save(preferences: SharedPreferences, rows: List<List<DesktopEntry>>) {
        val root = JSONArray()
        rows.forEach { row ->
            val rowJson = JSONArray()
            row.forEach { entry ->
                val children = JSONArray()
                entry.children.forEach { children.put(it) }
                rowJson.put(
                    JSONObject()
                        .put("title", entry.title)
                        .put("folder", entry.isFolder)
                        .put("children", children)
                )
            }
            root.put(rowJson)
        }
        preferences.edit().putString("desktop_layout_v1", root.toString()).apply()
    }
}

@Composable
private fun FolderIcon(label: String, modifier: Modifier = Modifier) {
    val backPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(190, 132, 255)
            style = Paint.Style.FILL
        }
    }
    val frontPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(132, 62, 224)
            style = Paint.Style.FILL
        }
    }
    val previewPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(48, 24, 72)
            style = Paint.Style.FILL
        }
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val canvas = drawContext.canvas.nativeCanvas
            val save = canvas.save()
            canvas.scale(size.width / 108f, size.height / 108f)

            val folderBack = android.graphics.Path().apply {
                moveTo(10f, 28f)
                lineTo(43f, 28f)
                lineTo(54f, 39f)
                lineTo(92f, 39f)
                quadTo(99f, 39f, 99f, 46f)
                lineTo(99f, 84f)
                quadTo(99f, 92f, 91f, 92f)
                lineTo(17f, 92f)
                quadTo(9f, 92f, 9f, 84f)
                lineTo(9f, 36f)
                quadTo(9f, 28f, 10f, 28f)
                close()
            }
            canvas.drawPath(folderBack, backPaint)
            canvas.drawRoundRect(8f, 43f, 100f, 96f, 13f, 13f, frontPaint)

            // Four tiny tiles make the folder contents immediately recognizable.
            canvas.drawRoundRect(29f, 56f, 48f, 74f, 5f, 5f, previewPaint)
            canvas.drawRoundRect(55f, 56f, 74f, 74f, 5f, 5f, previewPaint)
            canvas.drawRoundRect(29f, 78f, 48f, 91f, 4f, 4f, previewPaint)
            canvas.drawRoundRect(55f, 78f, 74f, 91f, 4f, 4f, previewPaint)
            canvas.restoreToCount(save)
        }
    }
}

@Composable
private fun AppIcon(
    label: String,
    isAi: Boolean,
    app: LauncherApp? = null,
    modifier: Modifier = Modifier
) {
    val kind = remember(label) { iconKind(label) }
    val bgPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(19, 13, 30)
            style = Paint.Style.FILL
        }
    }
    val glowPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(105, 57, 157)
            style = Paint.Style.FILL
        }
    }
    val glyphPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(220, 190, 255)
            style = Paint.Style.STROKE
            strokeWidth = 7f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
    }
    val fillPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(184, 132, 255)
            style = Paint.Style.FILL
        }
    }
    Canvas(modifier = modifier) {
        val canvas = drawContext.canvas.nativeCanvas
        val save = canvas.save()
        canvas.scale(size.width / 108f, size.height / 108f)
        canvas.drawRoundRect(3f, 3f, 105f, 105f, 27f, 27f, bgPaint)
        canvas.drawCircle(54f, 54f, 35f, glowPaint)
        drawPipIcon(canvas, kind, glyphPaint, fillPaint)
        if (isAi) {
            canvas.drawCircle(88f, 20f, 9f, fillPaint)
        }
        canvas.restoreToCount(save)
    }
}

private fun iconKind(label: String): String {
    val v = normalize(label)
    return when {
        "telegram" in v -> "telegram"
        "whatsapp" in v || "сообщения" in v -> "chat"
        "max" == v -> "max"
        "youtube" in v || v == "ytmusic" -> "video"
        "tiktok" in v -> "music"
        "capcut" in v -> "cut"
        "minecraft" in v -> "block"
        "brawl" in v -> "star"
        "roblox" in v -> "diamond"
        "chrome" in v || "браузер" in v -> "orbit"
        "gmail" in v || "почта" in v -> "mail"
        "googleplay" in v || "playstore" in v -> "play"
        "chatgpt" in v -> "knot"
        "claude" in v -> "sun"
        "gemini" in v -> "spark"
        "grok" in v -> "x"
        "deepseek" in v -> "wave"
        "github" in v -> "cat"
        "pipis" in v -> "pencil"
        "termux" in v -> "terminal"
        "камера" in v -> "camera"
        "календар" in v -> "calendar"
        "настрой" in v -> "gear"
        "фото" in v || "галере" in v -> "photo"
        "калькулятор" in v -> "calculator"
        "телефон" in v || "phone" in v -> "phone"
        "карты" in v || "maps" in v -> "pin"
        else -> "app"
    }
}

private fun drawPipIcon(
    canvas: android.graphics.Canvas,
    kind: String,
    line: Paint,
    fill: Paint
) {
    val p = android.graphics.Path()
    when (kind) {
        "telegram" -> {
            p.moveTo(19f, 51f); p.lineTo(88f, 24f); p.lineTo(72f, 87f)
            p.lineTo(52f, 68f); p.lineTo(41f, 78f); p.lineTo(43f, 58f); p.close()
            canvas.drawPath(p, fill)
            canvas.drawLine(43f, 58f, 88f, 24f, line)
        }
        "chat", "max" -> {
            canvas.drawRoundRect(20f, 23f, 88f, 76f, 19f, 19f, line)
            p.moveTo(37f, 74f); p.lineTo(31f, 88f); p.lineTo(53f, 75f)
            canvas.drawPath(p, line)
            canvas.drawCircle(40f, 50f, 4f, fill); canvas.drawCircle(54f, 50f, 4f, fill); canvas.drawCircle(68f, 50f, 4f, fill)
        }
        "video" -> {
            canvas.drawRoundRect(17f, 29f, 91f, 79f, 15f, 15f, line)
            p.moveTo(46f, 39f); p.lineTo(46f, 69f); p.lineTo(70f, 54f); p.close()
            canvas.drawPath(p, fill)
        }
        "music" -> {
            p.moveTo(60f, 25f); p.lineTo(82f, 20f); p.lineTo(82f, 68f)
            canvas.drawPath(p, line); canvas.drawLine(60f, 25f, 60f, 70f, line)
            canvas.drawOval(34f, 64f, 61f, 84f, fill); canvas.drawOval(57f, 60f, 84f, 80f, fill)
        }
        "cut", "x" -> {
            canvas.drawLine(27f, 27f, 81f, 81f, line); canvas.drawLine(81f, 27f, 27f, 81f, line)
            canvas.drawCircle(26f, 25f, 9f, fill); canvas.drawCircle(82f, 25f, 9f, fill)
        }
        "block" -> {
            p.moveTo(54f, 19f); p.lineTo(88f, 37f); p.lineTo(54f, 56f); p.lineTo(20f, 37f); p.close()
            canvas.drawPath(p, fill)
            p.reset(); p.moveTo(20f, 37f); p.lineTo(20f, 75f); p.lineTo(54f, 94f); p.lineTo(54f, 56f); p.close(); canvas.drawPath(p, line)
            p.reset(); p.moveTo(88f, 37f); p.lineTo(88f, 75f); p.lineTo(54f, 94f); p.lineTo(54f, 56f); p.close(); canvas.drawPath(p, line)
        }
        "star" -> {
            for (i in 0 until 10) {
                val a = Math.PI * i / 5.0 - Math.PI / 2
                val r = if (i % 2 == 0) 38.0 else 19.0
                val x = (54 + kotlin.math.cos(a) * r).toFloat()
                val y = (54 + kotlin.math.sin(a) * r).toFloat()
                if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
            }
            p.close(); canvas.drawPath(p, fill)
        }
        "diamond" -> {
            p.moveTo(54f, 18f); p.lineTo(88f, 54f); p.lineTo(54f, 90f); p.lineTo(20f, 54f); p.close()
            canvas.drawPath(p, line); canvas.drawLine(54f, 18f, 54f, 90f, line)
        }
        "orbit" -> {
            canvas.drawCircle(54f, 54f, 31f, line); canvas.drawCircle(54f, 54f, 14f, fill)
            canvas.drawArc(17f, 28f, 91f, 80f, -35f, 220f, false, fill)
        }
        "mail" -> {
            canvas.drawRoundRect(18f, 29f, 90f, 79f, 8f, 8f, line)
            canvas.drawLine(20f, 32f, 54f, 58f, line); canvas.drawLine(88f, 32f, 54f, 58f, line)
        }
        "play" -> {
            p.moveTo(35f, 20f); p.lineTo(85f, 54f); p.lineTo(35f, 88f); p.close()
            canvas.drawPath(p, fill); canvas.drawLine(35f, 20f, 54f, 54f, line); canvas.drawLine(35f, 88f, 54f, 54f, line)
        }
        "knot" -> {
            canvas.drawOval(25f, 29f, 72f, 76f, line); canvas.drawOval(38f, 29f, 85f, 76f, line)
            canvas.drawLine(35f, 42f, 73f, 67f, line); canvas.drawLine(73f, 42f, 35f, 67f, line)
        }
        "sun", "spark" -> {
            canvas.drawCircle(54f, 54f, 14f, fill)
            for (i in 0 until 8) {
                val a = Math.PI * i / 4
                val x1 = (54 + kotlin.math.cos(a) * 25).toFloat()
                val y1 = (54 + kotlin.math.sin(a) * 25).toFloat()
                val x2 = (54 + kotlin.math.cos(a) * 40).toFloat()
                val y2 = (54 + kotlin.math.sin(a) * 40).toFloat()
                canvas.drawLine(x1, y1, x2, y2, line)
            }
        }
        "wave" -> {
            for (j in 0..2) {
                val y = 39f + j * 15f
                p.reset(); p.moveTo(20f, y)
                p.cubicTo(37f, y - 20f, 67f, y + 20f, 88f, y)
                canvas.drawPath(p, line)
            }
        }
        "cat" -> {
            p.moveTo(26f, 42f); p.lineTo(23f, 22f); p.lineTo(43f, 33f)
            p.quadTo(54f, 28f, 65f, 33f); p.lineTo(85f, 22f); p.lineTo(82f, 42f)
            p.quadTo(94f, 72f, 70f, 82f); p.lineTo(38f, 82f); p.quadTo(14f, 72f, 26f, 42f); p.close()
            canvas.drawPath(p, line); canvas.drawCircle(42f, 57f, 3f, fill); canvas.drawCircle(66f, 57f, 3f, fill)
        }
        "pencil" -> {
            canvas.drawLine(31f, 77f, 75f, 33f, line); canvas.drawLine(24f, 84f, 31f, 77f, line)
            canvas.drawLine(75f, 33f, 84f, 42f, line); canvas.drawLine(31f, 77f, 40f, 86f, line)
            canvas.drawLine(40f, 86f, 84f, 42f, line)
        }
        "terminal" -> {
            canvas.drawRoundRect(17f, 25f, 91f, 82f, 9f, 9f, line)
            canvas.drawLine(30f, 42f, 43f, 54f, line); canvas.drawLine(43f, 54f, 30f, 66f, line)
            canvas.drawLine(52f, 67f, 76f, 67f, line)
        }
        "camera" -> {
            canvas.drawRoundRect(19f, 33f, 89f, 79f, 10f, 10f, line)
            canvas.drawCircle(54f, 56f, 16f, line); canvas.drawCircle(54f, 56f, 7f, fill)
        }
        "calendar" -> {
            canvas.drawRoundRect(22f, 25f, 86f, 84f, 8f, 8f, line)
            canvas.drawLine(22f, 43f, 86f, 43f, line); canvas.drawLine(39f, 18f, 39f, 33f, line); canvas.drawLine(69f, 18f, 69f, 33f, line)
            canvas.drawCircle(43f, 59f, 4f, fill); canvas.drawCircle(65f, 59f, 4f, fill)
        }
        "phone" -> {
            canvas.drawArc(27f, 25f, 81f, 83f, 120f, 125f, false, line)
            canvas.drawLine(33f, 34f, 43f, 45f, line); canvas.drawLine(65f, 64f, 77f, 75f, line)
        }
        "pin" -> {
            canvas.drawCircle(54f, 45f, 25f, line); canvas.drawCircle(54f, 45f, 8f, fill)
            p.moveTo(31f, 58f); p.lineTo(54f, 91f); p.lineTo(77f, 58f); canvas.drawPath(p, line)
        }
        else -> {
            canvas.drawRoundRect(28f, 28f, 80f, 80f, 13f, 13f, line)
            canvas.drawCircle(54f, 54f, 13f, fill)
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