package com.popcorntime.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.popcorntime.model.DownloadItem
import com.popcorntime.viewmodel.NavigationTarget
import com.popcorntime.viewmodel.PopcornViewModel

@Composable
fun DownloadsScreen(
    viewModel: PopcornViewModel,
    modifier: Modifier = Modifier
) {
    val downloads by viewModel.downloads.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0E12))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Downloads & Offline",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${downloads.size} items",
                color = Color(0xFF9CA3AF),
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (downloads.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("downloads_empty_view"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "No Downloads",
                        tint = Color(0xFF4B5563),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Active Downloads",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Download movies or TV shows to watch offline anytime",
                        color = Color(0xFF9CA3AF),
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("downloads_list")
            ) {
                items(downloads, key = { it.id }) { item ->
                    DownloadCardItem(
                        item = item,
                        onPlay = {
                            viewModel.navigateTo(
                                NavigationTarget.Player(
                                    title = item.title,
                                    subtitleInfo = item.subtitleInfo,
                                    streamUrl = item.streamUrl,
                                    torrent = null,
                                    subtitles = emptyList(),
                                    mediaId = item.id
                                )
                            )
                        },
                        onTogglePause = { viewModel.pauseDownload(item.id) },
                        onDelete = { viewModel.cancelDownload(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadCardItem(
    item: DownloadItem,
    onPlay: () -> Unit,
    onTogglePause: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("download_card_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161822)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(width = 65.dp, height = 90.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF222634))
                ) {
                    AsyncImage(
                        model = item.posterImage,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${item.subtitleInfo} • ${item.quality} • ${item.totalSize}",
                        color = Color(0xFF9CA3AF),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (item.isCompleted) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DownloadDone,
                                contentDescription = "Completed",
                                tint = Color(0xFF5ABA00),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Downloaded & Ready",
                                color = Color(0xFF5ABA00),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Text(
                            text = if (item.isPaused) "Paused" else "${item.downloadSpeed} • ${item.seeds}s / ${item.peers}p",
                            color = if (item.isPaused) Color(0xFFD47800) else Color(0xFF00B4D8),
                            fontSize = 11.sp
                        )
                    }
                }

                // Action buttons
                Row {
                    if (item.isCompleted) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE50914))
                                .clickable(onClick = onPlay),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        IconButton(onClick = onTogglePause) {
                            Icon(
                                imageVector = if (item.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = "Pause/Resume",
                                tint = Color.White
                            )
                        }
                    }

                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = Color(0xFFEF4444)
                        )
                    }
                }
            }

            if (!item.isCompleted) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { item.progress },
                    color = Color(0xFFE50914),
                    trackColor = Color(0xFF2D3349),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${(item.progress * 100).toInt()}% completed",
                        color = Color(0xFF9CA3AF),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
