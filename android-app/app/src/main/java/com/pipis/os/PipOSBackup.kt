package com.pipis.os

import android.content.Context
import android.net.Uri
import org.json.JSONObject
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class PipOSSettings(
    val themeName: String = "Void Purple",
    val accentColor: String = "#9B6DFF",
    val fontName: String = "System",
    val iconPackName: String = "Default"
)

object PipOSBackup {
    private const val SETTINGS_FILE = "settings.json"
    private const val MAX_SETTINGS_BYTES = 256 * 1024

    fun export(context: Context, destination: Uri, settings: PipOSSettings) {
        val json = JSONObject().apply {
            put("themeName", settings.themeName)
            put("accentColor", settings.accentColor)
            put("fontName", settings.fontName)
            put("iconPackName", settings.iconPackName)
        }
        val manifest = JSONObject().apply {
            put("format", "pipOS-backup")
            put("formatVersion", 1)
            put("appVersion", "0.1.0")
            put("createdAt", System.currentTimeMillis())
        }

        context.contentResolver.openOutputStream(destination)?.use { output ->
            ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write(manifest.toString(2).toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                zip.putNextEntry(ZipEntry(SETTINGS_FILE))
                zip.write(json.toString(2).toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        } ?: error("Не удалось открыть файл для записи")
    }

    fun import(context: Context, source: Uri): PipOSSettings {
        var settingsJson: String? = null
        context.contentResolver.openInputStream(source)?.use { input ->
            ZipInputStream(input).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (!entry.isDirectory && entry.name == SETTINGS_FILE) {
                        val bytes = zip.readNBytes(MAX_SETTINGS_BYTES + 1)
                        require(bytes.size <= MAX_SETTINGS_BYTES) { "Файл настроек слишком большой" }
                        settingsJson = bytes.toString(Charsets.UTF_8)
                        break
                    }
                    zip.closeEntry()
                }
            }
        } ?: error("Не удалось открыть архив")

        val json = JSONObject(settingsJson ?: error("В архиве нет settings.json"))
        val color = json.optString("accentColor", "#9B6DFF")
        require(color.matches(Regex("#[0-9A-Fa-f]{6}"))) { "Некорректный цвет темы" }

        return PipOSSettings(
            themeName = json.optString("themeName", "Void Purple").take(40),
            accentColor = color.uppercase(),
            fontName = json.optString("fontName", "System").take(80),
            iconPackName = json.optString("iconPackName", "Default").take(80)
        )
    }
}
