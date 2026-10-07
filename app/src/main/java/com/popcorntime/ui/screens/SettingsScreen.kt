package com.popcorntime.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    var autoDeleteCache by remember { mutableStateOf(true) }
    var hardwareAcceleration by remember { mutableStateOf(true) }
    var defaultQuality by remember { mutableStateOf("1080p") }
    var defaultSubtitleLang by remember { mutableStateOf("English") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0E12))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Playback & Video section
        SettingsSectionHeader(title = "Playback & Video")
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161822)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsRow(
                    icon = Icons.Default.HighQuality,
                    title = "Default Stream Quality",
                    subtitle = defaultQuality,
                    onClick = {
                        defaultQuality = if (defaultQuality == "1080p") "720p" else "1080p"
                    }
                )
                HorizontalDivider(color = Color(0xFF262A3B), modifier = Modifier.padding(vertical = 12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Hardware Acceleration", color = Color.White, fontSize = 14.sp)
                        Text("Decodes video using GPU", color = Color(0xFF9CA3AF), fontSize = 12.sp)
                    }
                    Switch(
                        checked = hardwareAcceleration,
                        onCheckedChange = { hardwareAcceleration = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFE50914)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Subtitles section
        SettingsSectionHeader(title = "Subtitles")
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161822)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsRow(
                    icon = Icons.Default.Subtitles,
                    title = "Default Language",
                    subtitle = defaultSubtitleLang,
                    onClick = {
                        defaultSubtitleLang = when (defaultSubtitleLang) {
                            "English" -> "Spanish"
                            "Spanish" -> "French"
                            "French" -> "German"
                            else -> "English"
                        }
                    }
                )
                HorizontalDivider(color = Color(0xFF262A3B), modifier = Modifier.padding(vertical = 12.dp))
                SettingsRow(
                    icon = Icons.Default.Subtitles,
                    title = "Font Size",
                    subtitle = "Medium (Standard)",
                    onClick = {}
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Storage & Cache
        SettingsSectionHeader(title = "Storage & Cache")
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161822)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Clear Cache on Player Exit", color = Color.White, fontSize = 14.sp)
                        Text("Saves storage by removing stream chunks", color = Color(0xFF9CA3AF), fontSize = 12.sp)
                    }
                    Switch(
                        checked = autoDeleteCache,
                        onCheckedChange = { autoDeleteCache = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFE50914)
                        )
                    )
                }
                HorizontalDivider(color = Color(0xFF262A3B), modifier = Modifier.padding(vertical = 12.dp))
                SettingsRow(
                    icon = Icons.Default.CleaningServices,
                    title = "Clear Download Cache Now",
                    subtitle = "Free up temporary torrent buffer files",
                    onClick = {
                        Toast.makeText(context, "Temporary stream cache cleared", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // About Popcorn Time
        SettingsSectionHeader(title = "About")
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161822)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsRow(
                    icon = Icons.Default.Info,
                    title = "Popcorn Time for Android",
                    subtitle = "Version 1.0.0 • Modern Jetpack Compose Rebuild",
                    onClick = {}
                )
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        color = Color(0xFF00B4D8),
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("settings_row_${title.lowercase().replace(" ", "_")}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFFE50914),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.size(14.dp))
        Column {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = Color(0xFF9CA3AF), fontSize = 12.sp)
        }
    }
}
