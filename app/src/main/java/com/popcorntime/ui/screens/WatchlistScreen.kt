package com.popcorntime.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.popcorntime.ui.components.MediaPosterCard
import com.popcorntime.viewmodel.NavigationTarget
import com.popcorntime.viewmodel.PopcornViewModel

@Composable
fun WatchlistScreen(
    viewModel: PopcornViewModel,
    modifier: Modifier = Modifier
) {
    val movies by viewModel.movies.collectAsState()
    val shows by viewModel.shows.collectAsState()
    val watchlistMovieIds by viewModel.watchlistMovieIds.collectAsState()
    val watchlistShowIds by viewModel.watchlistShowIds.collectAsState()

    val bookmarkedMovies = movies.filter { watchlistMovieIds.contains(it.id) }
    val bookmarkedShows = shows.filter { watchlistShowIds.contains(it.id) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0E12))
            .padding(top = 8.dp)
    ) {
        if (bookmarkedMovies.isEmpty() && bookmarkedShows.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("watchlist_empty_view"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = "Empty Watchlist",
                        tint = Color(0xFF4B5563),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Your Watchlist is empty",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Bookmark movies and TV shows to watch them later",
                        color = Color(0xFF9CA3AF),
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("watchlist_grid")
            ) {
                items(bookmarkedMovies, key = { "m_${it.id}" }) { movie ->
                    MediaPosterCard(
                        title = movie.title,
                        posterUrl = movie.posterImage,
                        rating = movie.rating,
                        year = movie.year,
                        qualityBadge = "Movie",
                        onClick = {
                            viewModel.navigateTo(NavigationTarget.MovieDetail(movie.id))
                        }
                    )
                }

                items(bookmarkedShows, key = { "s_${it.id}" }) { show ->
                    MediaPosterCard(
                        title = show.title,
                        posterUrl = show.posterImage,
                        rating = show.rating,
                        year = show.year,
                        qualityBadge = "Show",
                        onClick = {
                            viewModel.navigateTo(NavigationTarget.ShowDetail(show.id))
                        }
                    )
                }
            }
        }
    }
}
