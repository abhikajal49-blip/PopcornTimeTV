package com.popcorntime.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.popcorntime.model.Episode
import com.popcorntime.model.Movie
import com.popcorntime.model.Show
import com.popcorntime.model.Torrent
import com.popcorntime.ui.components.TorrentHealthBadge
import com.popcorntime.viewmodel.NavigationTarget
import com.popcorntime.viewmodel.PopcornViewModel

@Composable
fun MovieDetailScreen(
    movieId: String,
    viewModel: PopcornViewModel,
    onBack: () -> Unit
) {
    val movie = viewModel.getMovie(movieId) ?: return
    val watchlistIds by viewModel.watchlistMovieIds.collectAsState()
    val isWatchlist = watchlistIds.contains(movie.id)

    var selectedTorrentIndex by remember { mutableIntStateOf(0) }
    val selectedTorrent = movie.torrents.getOrNull(selectedTorrentIndex) ?: movie.torrents.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0E12))
            .verticalScroll(rememberScrollState())
    ) {
        // Hero Backdrop
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.6f)
        ) {
            AsyncImage(
                model = movie.backdropImage.ifBlank { movie.posterImage },
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.5f),
                                Color.Transparent,
                                Color(0xFF0D0E12)
                            )
                        )
                    )
            )

            // Top Bar
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopStart)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    .testTag("detail_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            // Quick Play FAB in Hero
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE50914))
                    .clickable {
                        viewModel.navigateTo(
                            NavigationTarget.Player(
                                title = movie.title,
                                subtitleInfo = "${movie.year} • ${selectedTorrent?.quality ?: "1080p"}",
                                streamUrl = selectedTorrent?.url ?: "",
                                torrent = selectedTorrent,
                                subtitles = movie.subtitles,
                                mediaId = movie.id
                            )
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // Details Container
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = movie.title,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Metadata row: Year, Certification, Runtime, Rating
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(movie.year, color = Color(0xFF9CA3AF), fontSize = 13.sp)

                Box(
                    modifier = Modifier
                        .background(Color(0xFF222634), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        movie.certification,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text("${movie.runtimeMinutes} min", color = Color(0xFF9CA3AF), fontSize = 13.sp)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = Color(0xFFFFB703),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format("%.1f", movie.rating),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Genres
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                movie.genres.forEach { genre ->
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF1E2230), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(genre, color = Color(0xFF00B4D8), fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.navigateTo(
                            NavigationTarget.Player(
                                title = movie.title,
                                subtitleInfo = "${movie.year} • ${selectedTorrent?.quality ?: "1080p"}",
                                streamUrl = selectedTorrent?.url ?: "",
                                torrent = selectedTorrent,
                                subtitles = movie.subtitles,
                                mediaId = movie.id
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("watch_now_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Watch Now", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        if (selectedTorrent != null) {
                            viewModel.startDownload(
                                id = "dl_${movie.id}_${selectedTorrent.quality}",
                                title = movie.title,
                                subtitleInfo = "${movie.year} • ${selectedTorrent.quality}",
                                posterImage = movie.posterImage,
                                quality = selectedTorrent.quality,
                                size = selectedTorrent.size,
                                streamUrl = selectedTorrent.url
                            )
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier.testTag("download_button")
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Download")
                }

                IconButton(
                    onClick = { viewModel.toggleWatchlistMovie(movie.id) },
                    modifier = Modifier
                        .background(Color(0xFF1E2230), RoundedCornerShape(8.dp))
                        .testTag("watchlist_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isWatchlist) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Watchlist",
                        tint = if (isWatchlist) Color(0xFFE50914) else Color.White
                    )
                }

                IconButton(
                    onClick = { viewModel.toggleWatched(movie.id) },
                    modifier = Modifier
                        .background(Color(0xFF1E2230), RoundedCornerShape(8.dp))
                        .testTag("watched_toggle_button")
                ) {
                    Icon(
                        imageVector = if (movie.isWatched) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                        contentDescription = "Watched",
                        tint = if (movie.isWatched) Color(0xFF5ABA00) else Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Torrents & Health Section
            Text(
                text = "Stream Quality & Torrent Health",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                movie.torrents.forEachIndexed { index, torrent ->
                    val isSelected = index == selectedTorrentIndex
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFF2B3248) else Color(0xFF161822))
                            .clickable { selectedTorrentIndex = index }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        TorrentHealthBadge(torrent = torrent)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Synopsis
            Text(
                text = "Synopsis",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = movie.summary,
                color = Color(0xFFD1D5DB),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun ShowDetailScreen(
    showId: String,
    viewModel: PopcornViewModel,
    onBack: () -> Unit
) {
    val show = viewModel.getShow(showId) ?: return
    val watchlistIds by viewModel.watchlistShowIds.collectAsState()
    val isWatchlist = watchlistIds.contains(show.id)

    val availableSeasons = show.seasonNumbers.ifEmpty { listOf(1) }
    var selectedSeason by remember { mutableIntStateOf(availableSeasons.first()) }

    val seasonEpisodes = show.episodes.filter { it.season == selectedSeason }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0E12))
            .verticalScroll(rememberScrollState())
    ) {
        // Hero Backdrop
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.6f)
        ) {
            AsyncImage(
                model = show.backdropImage.ifBlank { show.posterImage },
                contentDescription = show.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.5f),
                                Color.Transparent,
                                Color(0xFF0D0E12)
                            )
                        )
                    )
            )

            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopStart)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    .testTag("detail_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = show.title,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(show.year, color = Color(0xFF9CA3AF), fontSize = 13.sp)
                Text(show.network ?: "TV", color = Color(0xFF00B4D8), fontSize = 13.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = Color(0xFFFFB703),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format("%.1f", show.rating),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val firstEp = show.latestUnwatchedEpisode() ?: seasonEpisodes.firstOrNull()
                Button(
                    onClick = {
                        if (firstEp != null) {
                            val torrent = firstEp.torrents.firstOrNull()
                            viewModel.navigateTo(
                                NavigationTarget.Player(
                                    title = "${show.title} S${firstEp.season}E${firstEp.episode}",
                                    subtitleInfo = firstEp.title,
                                    streamUrl = torrent?.url ?: "",
                                    torrent = torrent,
                                    subtitles = firstEp.subtitles,
                                    mediaId = firstEp.id
                                )
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (firstEp != null) "Play S${firstEp.season}E${firstEp.episode}" else "Watch Show")
                }

                IconButton(
                    onClick = { viewModel.toggleWatchlistShow(show.id) },
                    modifier = Modifier.background(Color(0xFF1E2230), RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = if (isWatchlist) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Watchlist",
                        tint = if (isWatchlist) Color(0xFFE50914) else Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = show.summary,
                color = Color(0xFFD1D5DB),
                fontSize = 13.sp,
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color(0xFF2D3349))
            Spacer(modifier = Modifier.height(14.dp))

            // Season Tabs
            Text(
                text = "Seasons & Episodes",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            ScrollableTabRow(
                selectedTabIndex = availableSeasons.indexOf(selectedSeason).coerceAtLeast(0),
                containerColor = Color(0xFF13151D),
                contentColor = Color(0xFFE50914),
                edgePadding = 0.dp
            ) {
                availableSeasons.forEach { seasonNum ->
                    Tab(
                        selected = selectedSeason == seasonNum,
                        onClick = { selectedSeason = seasonNum },
                        text = {
                            Text(
                                "Season $seasonNum",
                                color = if (selectedSeason == seasonNum) Color(0xFFE50914) else Color(0xFF9CA3AF)
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Episode List
            seasonEpisodes.forEach { ep ->
                EpisodeRowItem(
                    episode = ep,
                    onPlay = {
                        val torrent = ep.torrents.firstOrNull()
                        viewModel.navigateTo(
                            NavigationTarget.Player(
                                title = "${show.title} S${ep.season}E${ep.episode}",
                                subtitleInfo = ep.title,
                                streamUrl = torrent?.url ?: "",
                                torrent = torrent,
                                subtitles = ep.subtitles,
                                mediaId = ep.id
                            )
                        )
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun EpisodeRowItem(
    episode: Episode,
    onPlay: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161822)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 90.dp, height = 55.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF222634))
            ) {
                AsyncImage(
                    model = episode.backdropImage,
                    contentDescription = episode.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(24.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${episode.episode}. ${episode.title}",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = episode.summary,
                    color = Color(0xFF9CA3AF),
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
