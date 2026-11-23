package com.pipis.os

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val LauncherBackground = Color(0xFF08060D)
private val LauncherPanel = Color(0xFF171020)
private val LauncherAccent = Color(0xFFB58AFF)
private val LauncherText = Color(0xFFF7F2FF)
private val LauncherMuted = Color(0xFFB8AEC9)

private data class LauncherApp(val label: String, val packageName: String)

class HomeScreenActivity : ComponentActivity() {
    private var launcherApps by mutableStateOf<List<LauncherApp>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                PipOSLauncher(
                    apps = launcherApps,
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
        launcherApps = loadLauncherApps()
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
            val appPackage = activityInfo.packageName
            if (appPackage == packageName) return@mapNotNull null

            val appInfo = runCatching {
                if (Build.VERSION.SDK_INT >= 33) {
                    packageManager.getApplicationInfo(appPackage, PackageManager.ApplicationInfoFlags.of(0))
                } else {
                    packageManager.getApplicationInfo(appPackage, 0)
                }
            }.getOrNull() ?: return@mapNotNull null

            val isSystem = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
            if (isSystem && !isAllowedBuiltInApp(appPackage)) return@mapNotNull null

            val label = info.loadLabel(packageManager).toString()
            LauncherApp(label, appPackage)
        }.distinctBy { it.packageName }.sortedBy { it.label.lowercase(Locale.getDefault()) }
    }

    private fun isAllowedBuiltInApp(appPackage: String): Boolean {
        val visibleBuiltIns = listOf(
            "com.android.settings", "com.android.camera", "com.android.camera2",
            "com.transsion.camera", "com.android.dialer", "com.google.android.dialer",
            "com.android.contacts", "com.google.android.contacts",
            "com.android.mms", "com.google.android.apps.messaging",
            "com.android.gallery3d", "com.google.android.apps.photos",
            "com.google.android.apps.nbu.files", "com.google.android.documentsui",
            "com.android.documentsui", "com.google.android.calculator",
            "com.android.calculator2", "com.google.android.calendar",
            "com.android.deskclock", "com.google.android.deskclock",
            "com.android.chrome", "com.google.android.youtube",
            "com.android.vending"
        )
        return appPackage in visibleBuiltIns
    }


}

@Composable
private fun PipOSLauncher(
    apps: List<LauncherApp>,
    onLaunch: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1_000)
        }
    }

    val timeText = remember(currentTime) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(currentTime))
    }
    val dateText = remember(currentTime) {
        SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
            .format(Date(currentTime))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }
    val filtered = remember(apps, search) {
        apps.filter { it.label.contains(search, ignoreCase = true) }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = LauncherBackground) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("PipOS", color = LauncherText, fontSize = 25.sp, fontWeight = FontWeight.Black)
                    Text("YOUR SPACE", color = LauncherAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp)
                }
                TextButton(onClick = onOpenSettings) {
                    Text("Настройки", color = LauncherAccent, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(20.dp))

            Column(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(LauncherPanel)
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Text(timeText, color = LauncherText, fontSize = 66.sp, lineHeight = 72.sp, fontWeight = FontWeight.Light)
                Spacer(Modifier.height(2.dp))
                Text(dateText, color = LauncherAccent, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(18.dp))

            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Поиск приложений", color = LauncherMuted) },
                shape = RoundedCornerShape(18.dp)
            )

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ПРИЛОЖЕНИЯ", color = LauncherAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Text("${filtered.size}", color = LauncherMuted, fontSize = 11.sp)
            }

            Spacer(Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                items(filtered, key = { it.packageName }) { app ->
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onLaunch(app.packageName) }
                            .padding(vertical = 7.dp, horizontal = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        PipOSAppIcon(app = app, modifier = Modifier.size(58.dp))
                        Spacer(Modifier.height(7.dp))
                        Text(
                            app.label,
                            color = LauncherText,
                            fontSize = 10.sp,
                            lineHeight = 13.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }
                }
            }

            Text(
                "PipOS  •  сделано под твой стиль",
                color = LauncherMuted,
                fontSize = 10.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}


@Composable
private fun PipOSAppIcon(app: LauncherApp, modifier: Modifier = Modifier) {
    val symbol = remember(app.packageName, app.label) { appSymbol(app.packageName, app.label) }
    Canvas(modifier = modifier) {
        val native = drawContext.canvas.nativeCanvas
        val save = native.save()
        native.scale(size.width / 108f, size.height / 108f)

        val background = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            style = Paint.Style.FILL
        }
        native.drawRoundRect(2f, 2f, 106f, 106f, 25f, 25f, background)

        // The same purple folder silhouette used by PipisEdit.
        val folder = Path().apply {
            moveTo(30f, 35f)
            lineTo(48f, 35f)
            lineTo(54f, 42f)
            lineTo(78f, 42f)
            cubicTo(79.7f, 42f, 81f, 43.3f, 81f, 45f)
            lineTo(81f, 73f)
            cubicTo(81f, 74.7f, 79.7f, 76f, 78f, 76f)
            lineTo(30f, 76f)
            cubicTo(28.3f, 76f, 27f, 74.7f, 27f, 73f)
            lineTo(27f, 38f)
            cubicTo(27f, 36.3f, 28.3f, 35f, 30f, 35f)
            close()
        }
        val purple = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(156, 77, 255)
            style = Paint.Style.FILL
        }
        native.drawPath(folder, purple)

        val glyph = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
            textSize = when {
                symbol.length >= 4 -> 15f
                symbol.length == 3 -> 19f
                symbol.length == 2 -> 25f
                else -> 34f
            }
        }
        val baseline = 59f - (glyph.ascent() + glyph.descent()) / 2f
        native.drawText(symbol, 54f, baseline, glyph)
        native.restoreToCount(save)
    }
}

private fun appSymbol(packageName: String, label: String): String {
    val id = packageName.lowercase(Locale.ROOT)
    return when {
        "youtube" in id -> "YT"
        "tiktok" in id -> "TT"
        "minecraft" in id -> "MC"
        "chrome" in id -> "CH"
        "telegram" in id -> "TG"
        "whatsapp" in id -> "WA"
        "instagram" in id -> "IG"
        "facebook" in id -> "FB"
        "messenger" in id -> "MSG"
        "gmail" in id -> "GM"
        "google" in id -> "G"
        "github" in id -> "GH"
        "camera" in id -> "CAM"
        "gallery" in id || "photos" in id -> "PH"
        "settings" in id -> "SET"
        "dialer" in id || "phone" in id -> "TEL"
        "contacts" in id -> "CT"
        "calendar" in id -> "CAL"
        "clock" in id || "deskclock" in id -> "CLK"
        "calculator" in id -> "CALC"
        "files" in id || "documentsui" in id -> "FILE"
        "play" in id && "store" in id -> "PLAY"
        else -> label.trim().take(2).uppercase(Locale.getDefault()).ifBlank { "?" }
    }
}
