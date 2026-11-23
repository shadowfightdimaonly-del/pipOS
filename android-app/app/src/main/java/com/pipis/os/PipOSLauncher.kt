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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

private data class LauncherApp(val label: String, val packageName: String)
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
    var editMode by remember { mutableStateOf(false) }
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
                                    if (editMode) {
                                        TextButton(onClick = { showCreateFolder = true }) {
                                            Text("+ Папка", color = LauncherAccent, fontSize = 11.sp)
                                        }
                                        TextButton(onClick = {
                                            appPickerFolder = null
                                            showAppPicker = true
                                        }) {
                                            Text("+ App", color = LauncherAccent, fontSize = 11.sp)
                                        }
                                        TextButton(onClick = { editMode = false }) {
                                            Text("Готово", color = LauncherText, fontSize = 11.sp)
                                        }
                                    } else {
                                        TextButton(onClick = { editMode = true }) {
                                            Text("Изменить", color = LauncherAccent, fontSize = 11.sp)
                                        }
                                        TextButton(onClick = onOpenSettings) {
                                            Text("Настройки", color = LauncherAccent, fontSize = 11.sp)
                                        }
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
                                                onEdit = if (editMode) {
                                                    { editingPosition = rowIndex to entryIndex }
                                                } else null,
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

@Composable
private fun DesktopTile(
    entry: DesktopEntry,
    isAi: Boolean,
    app: LauncherApp?,
    onEdit: (() -> Unit)?,
    onOpenFolder: () -> Unit,
    onLaunch: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp))
            .clickable {
                if (onEdit != null) {
                    onEdit()
                } else when {
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
                        AppIcon(app.label, false, Modifier.size(34.dp))
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
    val paint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(156, 77, 255)
            style = Paint.Style.FILL
        }
    }
    val path = remember {
        android.graphics.Path().apply {
            moveTo(13f, 31f)
            lineTo(46f, 31f)
            lineTo(57f, 43f)
            lineTo(95f, 43f)
            lineTo(95f, 86f)
            quadTo(95f, 95f, 86f, 95f)
            lineTo(18f, 95f)
            quadTo(11f, 95f, 11f, 86f)
            close()
        }
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val canvas = drawContext.canvas.nativeCanvas
            val save = canvas.save()
            canvas.scale(size.width / 108f, size.height / 108f)
            canvas.drawPath(path, paint)
            canvas.restoreToCount(save)
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
    val iconPath = remember {
        android.graphics.Path().apply {
            moveTo(30f, 35f); lineTo(48f, 35f); lineTo(54f, 42f); lineTo(78f, 42f)
            cubicTo(79.7f, 42f, 81f, 43.3f, 81f, 45f); lineTo(81f, 73f)
            cubicTo(81f, 74.7f, 79.7f, 76f, 78f, 76f); lineTo(30f, 76f)
            cubicTo(28.3f, 76f, 27f, 74.7f, 27f, 73f); lineTo(27f, 38f)
            cubicTo(27f, 36.3f, 28.3f, 35f, 30f, 35f); close()
        }
    }
    val badgePaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(224, 190, 255)
            style = Paint.Style.FILL
        }
    }
    val starPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(75, 28, 120)
            textAlign = Paint.Align.CENTER
            textSize = 19f
            typeface = Typeface.DEFAULT_BOLD
        }
    }
    Canvas(modifier = modifier) {
        val canvas = drawContext.canvas.nativeCanvas
        val save = canvas.save()
        canvas.scale(size.width / 108f, size.height / 108f)
        canvas.drawRoundRect(2f, 2f, 106f, 106f, 25f, 25f, bg)
        canvas.drawPath(iconPath, purple)
        val baseline = 59f - (glyph.ascent() + glyph.descent()) / 2f
        canvas.drawText(symbol, 54f, baseline, glyph)
        if (isAi) {
            canvas.drawCircle(87f, 20f, 13f, badgePaint)
            canvas.drawText("★", 87f, 27f, starPaint)
        }
        canvas.restoreToCount(save)
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
