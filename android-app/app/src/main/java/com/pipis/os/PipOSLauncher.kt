package com.pipis.os

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
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

private data class LauncherApp(val label: String, val packageName: String, val icon: Bitmap)

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
            LauncherApp(label, appPackage, makeAppIcon(info.activityInfo.loadIcon(packageManager)))
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

    private fun makeAppIcon(drawable: android.graphics.drawable.Drawable): Bitmap {
        val bitmap = Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val icon = drawable.mutate()
        val width = (icon.intrinsicWidth.takeIf { it > 0 } ?: 128).toFloat()
        val height = (icon.intrinsicHeight.takeIf { it > 0 } ?: 128).toFloat()
        val scale = 152f / maxOf(width, height)
        val iconWidth = width * scale
        val iconHeight = height * scale
        val left = ((192f - iconWidth) / 2f).toInt()
        val top = ((192f - iconHeight) / 2f).toInt()
        icon.setBounds(left, top, (left + iconWidth).toInt(), (top + iconHeight).toInt())
        icon.draw(canvas)
        return bitmap
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
                Text(timeText, color = LauncherText, fontSize = 58.sp, lineHeight = 64.sp, fontWeight = FontWeight.Light)
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
                        Image(
                            bitmap = app.icon.asImageBitmap(),
                            contentDescription = "Иконка ${app.label}",
                            modifier = Modifier.size(52.dp)
                        )
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
