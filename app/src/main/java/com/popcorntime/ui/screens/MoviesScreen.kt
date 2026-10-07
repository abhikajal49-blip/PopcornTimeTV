package com.popcorntime.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import com.popcorntime.model.MediaSortFilter
import com.popcorntime.model.Movie
import com.popcorntime.ui.components.MediaPosterCard
import com.popcorntime.viewmodel.NavigationTarget
import com.popcorntime.viewmodel.PopcornViewModel

@Composable
fun MoviesScreen(
    viewModel: PopcornViewModel,
    modifier: Modifier = Modifier
) {
    val movies by viewModel.movies.collectAsState()
    val isLoading by viewModel.isMoviesLoading.collectAsState()
    val activeSort by viewModel.movieSortFilter.collectAsState()
    val activeGenre by viewModel.movieGenre.collectAsState()

    // Continue watching items (movies with progress > 0)
    val continueWatching = movies.filter { it.progressSeconds > 0 }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0E12))
    ) {
        // Sort & Genre Filters Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF13151D))
                .padding(vertical = 8.dp)
        ) {
            // Sort Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Sort,
                    contentDescription = "Sort",
                    tint = Color(0xFFE50914),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))

                MediaSortFilter.entries.forEach { sort ->
                    val isSelected = activeSort == sort
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) Color(0xFFE50914) else Color(0xFF222634))
                            .clickable { viewModel.setMovieSort(sort) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = sort.label,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Genre Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                viewModel.availableGenres.forEach { genre ->
                    val isSelected = activeGenre == genre
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setMovieGenre(genre) },
                        label = { Text(genre, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00B4D8).copy(alpha = 0.25f),
                            selectedLabelColor = Color(0xFF00B4D8),
                            containerColor = Color(0xFF181B24),
                            labelColor = Color(0xFF9CA3AF)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (isSelected) Color(0xFF00B4D8) else Color(0xFF2D3349),
                            enabled = true,
                            selected = isSelected
                        ),
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFE50914))
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("movies_grid")
            ) {
                // Continue Watching Section
                if (continueWatching.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        ContinueWatchingSection(
                            items = continueWatching,
                            onItemClick = { movie ->
                                val torrent = movie.torrents.firstOrNull()
                                viewModel.navigateTo(
                                    NavigationTarget.Player(
                                        title = movie.title,
                                        subtitleInfo = movie.year,
                                        streamUrl = torrent?.url ?: "",
                                        torrent = torrent,
                                        subtitles = movie.subtitles,
                                        mediaId = movie.id
                                    )
                                )
                            }
                        )
                    }
                }

                items(movies, key = { it.id }) { movie ->
                    MediaPosterCard(
                        title = movie.title,
                        posterUrl = movie.posterImage,
                        rating = movie.rating,
                        year = movie.year,
                        qualityBadge = movie.torrents.firstOrNull()?.quality ?: "HD",
                        onClick = {
                            viewModel.navigateTo(NavigationTarget.MovieDetail(movie.id))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ContinueWatchingSection(
    items: List<Movie>,
    onItemClick: (Movie) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Text(
            text = "Continue Watching",
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items.forEach { movie ->
                val progressFraction = if (movie.durationSeconds > 0) {
                    (movie.progressSeconds.toFloat() / movie.durationSeconds.toFloat()).coerceIn(0f, 1f)
                } else 0.4f

                Column(
                    modifier = Modifier
                        .width(180.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF161822))
                        .clickable { onItemClick(movie) }
                ) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                        AsyncImage(
                            model = movie.backdropImage.ifBlank { movie.posterImage },
                            contentDescription = movie.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(36.dp)
                                .background(Color.Black.copy(alpha = 0.65f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Resume",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { progressFraction },
                        color = Color(0xFFE50914),
                        trackColor = Color(0xFF2D3349),
                        modifier = Modifier.fillMaxWidth().height(3.dp)
                    )

                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = movie.title,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Resume playback",
                            color = Color(0xFF00B4D8),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
