package com.popcorntime.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.popcorntime.model.Episode
import com.popcorntime.model.Subtitle
import com.popcorntime.model.Torrent
import com.popcorntime.viewmodel.NavigationTarget
import com.popcorntime.viewmodel.PopcornViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material3.ExperimentalMaterial3Api
import java.util.Locale
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    title: String,
    subtitleInfo: String,
    streamUrl: String,
    torrent: Torrent?,
    subtitles: List<Subtitle>,
    mediaId: String,
    nextEpisode: Episode?,
    viewModel: PopcornViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Handle back button to save progress
    BackHandler {
        onClose()
    }

    var isControlsVisible by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var bufferPercentage by remember { mutableFloatStateOf(0.15f) }
    var downloadSpeed by remember { mutableStateOf("3.4 MB/s") }
    var seedsCount by remember { mutableStateOf(torrent?.seeds ?: 48) }
    var peersCount by remember { mutableStateOf(torrent?.peers ?: 14) }

    var selectedSubtitle by remember { mutableStateOf<Subtitle?>(subtitles.firstOrNull()) }
    var subtitleDelaySeconds by remember { mutableFloatStateOf(0f) }
    var showSubtitleSheet by remember { mutableStateOf(false) }

    // Aspect ratio modes: RESIZE_MODE_FIT (0), RESIZE_MODE_FILL (3), RESIZE_MODE_ZOOM (4)
    var resizeMode by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    val fallbackVideo = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
    val actualUrl = if (streamUrl.isNotBlank() && (streamUrl.startsWith("http://") || streamUrl.startsWith("https://"))) {
        streamUrl
    } else fallbackVideo

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val item = MediaItem.fromUri(actualUrl)
            setMediaItem(item)
            prepare()
            playWhenReady = true
        }
    }

    // Set screen orientation to sensor landscape while player is open, reset on exit
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val originalOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = (playbackState == Player.STATE_BUFFERING)
                if (playbackState == Player.STATE_READY) {
                    durationMs = exoPlayer.duration.coerceAtLeast(0L)
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            viewModel.saveProgress(mediaId, currentPositionMs / 1000, durationMs / 1000)
            exoPlayer.removeListener(listener)
            exoPlayer.release()
            activity?.requestedOrientation = originalOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Position & telemetry polling loop
    LaunchedEffect(exoPlayer) {
        while (true) {
            delay(500)
            if (exoPlayer.isPlaying) {
                currentPositionMs = exoPlayer.currentPosition
                durationMs = exoPlayer.duration.coerceAtLeast(0L)
                bufferPercentage = (exoPlayer.bufferedPercentage / 100f).coerceIn(0f, 1f)
                // Telemetry simulation
                seedsCount = (torrent?.seeds ?: 48) + Random.nextInt(-3, 4)
                peersCount = (torrent?.peers ?: 14) + Random.nextInt(-1, 2)
                downloadSpeed = "${Random.nextInt(2, 5)}.${Random.nextInt(1, 9)} MB/s"
            }
        }
    }

    // Auto-hide controls idle timer (5 seconds)
    LaunchedEffect(isControlsVisible, isPlaying) {
        if (isControlsVisible && isPlaying) {
            delay(5000)
            isControlsVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { isControlsVisible = !isControlsVisible }
    ) {
        // Video Surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    this.resizeMode = resizeMode
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.resizeMode = resizeMode
            },
            modifier = Modifier.fillMaxSize()
        )

        // Subtitle display overlay if selected
        if (selectedSubtitle != null && isPlaying && !isBuffering) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (isControlsVisible) 80.dp else 24.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "[Subtitle: ${selectedSubtitle?.language}${if (subtitleDelaySeconds != 0f) " (Delay: ${subtitleDelaySeconds}s)" else ""}]",
                    color = Color.Yellow,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Torrent Telemetry Dashboard Top Left (Torrent health, speed, buffer status)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = if (isControlsVisible) 64.dp else 16.dp, start = 16.dp)
                .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFF5ABA00), CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${torrent?.quality ?: "1080p"} • $downloadSpeed",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "(${seedsCount}s / ${peersCount}p)",
                    color = Color(0xFF00B4D8),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "• ${(bufferPercentage * 100).toInt()}% buffered",
                    color = Color(0xFF9CA3AF),
                    fontSize = 11.sp
                )
            }
        }

        // Center Buffering Spinner
        if (isBuffering) {
            Box(
                modifier = Modifier.align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFFE50914),
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(54.dp)
                )
            }
        }

        // Player Controls Overlay (Animated Fade In/Out)
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.7f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .testTag("player_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (subtitleInfo.isNotBlank()) {
                            Text(
                                text = subtitleInfo,
                                color = Color(0xFF9CA3AF),
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Subtitles Button
                    IconButton(
                        onClick = { showSubtitleSheet = true },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .testTag("player_subtitles_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Subtitles,
                            contentDescription = "Subtitles",
                            tint = if (selectedSubtitle != null) Color(0xFF00B4D8) else Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Aspect Ratio Button
                    IconButton(
                        onClick = {
                            resizeMode = when (resizeMode) {
                                AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .testTag("player_aspect_ratio_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AspectRatio,
                            contentDescription = "Aspect Ratio",
                            tint = Color.White
                        )
                    }
                }

                // Center Controls (Rewind 10, Play/Pause, Fast Forward 10)
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val target = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                            exoPlayer.seekTo(target)
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .testTag("player_rewind_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Rewind 10s",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE50914))
                            .clickable {
                                if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                            }
                            .testTag("player_play_pause_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val target = (exoPlayer.currentPosition + 10000).coerceAtMost(durationMs)
                            exoPlayer.seekTo(target)
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .testTag("player_forward_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Forward 10s",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Bottom Scrubber Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(currentPositionMs),
                            color = Color.White,
                            fontSize = 12.sp
                        )
                        Text(
                            text = formatTime(durationMs),
                            color = Color(0xFF9CA3AF),
                            fontSize = 12.sp
                        )
                    }

                    val progressFraction = if (durationMs > 0) {
                        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    Slider(
                        value = progressFraction,
                        onValueChange = { frac ->
                            val seekTarget = (frac * durationMs).toLong()
                            exoPlayer.seekTo(seekTarget)
                            currentPositionMs = seekTarget
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFE50914),
                            activeTrackColor = Color(0xFFE50914),
                            inactiveTrackColor = Color(0xFF374151)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("player_scrubber")
                    )
                }
            }
        }

        // Subtitles Selection Bottom Sheet
        if (showSubtitleSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSubtitleSheet = false },
                containerColor = Color(0xFF161822),
                sheetState = rememberModalBottomSheetState()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Subtitle Selection",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Option: Off
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedSubtitle = null
                                showSubtitleSheet = false
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Off (No Subtitles)",
                            color = if (selectedSubtitle == null) Color(0xFFE50914) else Color.White,
                            fontSize = 14.sp,
                            fontWeight = if (selectedSubtitle == null) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    // Available subtitles
                    subtitles.forEach { sub ->
                        val isSelected = selectedSubtitle?.language == sub.language
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedSubtitle = sub
                                    showSubtitleSheet = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${sub.language} (${sub.code})",
                                color = if (isSelected) Color(0xFF00B4D8) else Color.White,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Subtitle Offset / Delay: ${String.format(Locale.US, "%.1f", subtitleDelaySeconds)}s",
                        color = Color(0xFF9CA3AF),
                        fontSize = 13.sp
                    )
                    Slider(
                        value = subtitleDelaySeconds,
                        onValueChange = { subtitleDelaySeconds = it },
                        valueRange = -3.0f..3.0f,
                        steps = 11,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00B4D8),
                            activeTrackColor = Color(0xFF00B4D8)
                        )
                    )
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hours = minutes / 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes % 60, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
