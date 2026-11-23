package com.pipis.os

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LauncherBackground = Color(0xFF08060D)
private val LauncherPurple = Color(0xFF241044)
private val LauncherAccent = Color(0xFFB58AFF)
private val LauncherText = Color(0xFFF7F2FF)
private val LauncherMuted = Color(0xFFB8AEC9)

private data class LauncherApp(val label: String, val packageName: String)

class HomeScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                PipOSLauncher(
                    apps = loadLauncherApps(),
                    onLaunch = { packageName ->
                        packageManager.getLaunchIntentForPackage(packageName)?.let { startActivity(it) }
                    },
                    onOpenSettings = { startActivity(Intent(this, MainActivity::class.java)) }
                )
            }
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
        }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
    }
}

@Composable
private fun PipOSLauncher(
    apps: List<LauncherApp>,
    onLaunch: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    val filtered = remember(apps, search) { apps.filter { it.label.contains(search, ignoreCase = true) } }
    Surface(modifier = Modifier.fillMaxSize(), color = LauncherBackground) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 22.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("PipOS", color = LauncherText, fontSize = 30.sp, fontWeight = FontWeight.Black)
                    Text("ТВОЙ РАБОЧИЙ СТОЛ", color = LauncherAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                }
                TextButton(onClick = onOpenSettings) { Text("Настройки", color = LauncherAccent) }
            }
            Spacer(Modifier.height(18.dp))
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Найти приложение", color = LauncherMuted) },
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(Modifier.height(18.dp))
            Text("ПРИЛОЖЕНИЯ  ·  ${filtered.size}", color = LauncherAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 76.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filtered, key = { it.packageName }) { app ->
                    Column(
                        modifier = Modifier.fillMaxWidth().clickable { onLaunch(app.packageName) }.padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(58.dp).background(LauncherPurple, RoundedCornerShape(19.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = app.label.firstOrNull()?.uppercase() ?: "•",
                                color = LauncherAccent,
                                fontSize = 27.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = app.label,
                            color = LauncherText,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }
                }
            }
            Text(
                "Чёрный фон · Глубокий фиолетовый",
                color = LauncherMuted,
                fontSize = 10.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}
