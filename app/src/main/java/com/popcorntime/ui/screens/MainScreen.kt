package com.popcorntime.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalMovies
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.popcorntime.R
import com.popcorntime.ui.components.MagnetInputDialog
import com.popcorntime.viewmodel.NavigationTarget
import com.popcorntime.viewmodel.PopcornViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: PopcornViewModel) {
    val destination by viewModel.currentDestination.collectAsState()
    val showMagnetDialog by viewModel.showMagnetDialog.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var isSearchExpanded by remember { mutableStateOf(false) }

    // Intercept back button when on sub-screens
    val isTopLevelTab = destination is NavigationTarget.MoviesTab ||
            destination is NavigationTarget.ShowsTab ||
            destination is NavigationTarget.WatchlistTab ||
            destination is NavigationTarget.DownloadsTab ||
            destination is NavigationTarget.SettingsTab

    BackHandler(enabled = !isTopLevelTab) {
        viewModel.navigateBack()
    }

    if (showMagnetDialog) {
        MagnetInputDialog(
            onDismiss = { viewModel.showMagnetDialog(false) },
            onSubmit = { title, url -> viewModel.importMagnet(title, url) }
        )
    }

    // When playing a video, render the full-screen player without Scaffold chrome
    if (destination is NavigationTarget.Player) {
        val playerTarget = destination as NavigationTarget.Player
        PlayerScreen(
            title = playerTarget.title,
            subtitleInfo = playerTarget.subtitleInfo,
            streamUrl = playerTarget.streamUrl,
            torrent = playerTarget.torrent,
            subtitles = playerTarget.subtitles,
            mediaId = playerTarget.mediaId,
            nextEpisode = playerTarget.nextEpisode,
            viewModel = viewModel,
            onClose = { viewModel.navigateBack() }
        )
        return
    }

    Scaffold(
        topBar = {
            if (isTopLevelTab) {
                TopAppBar(
                    title = {
                        if (isSearchExpanded) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                placeholder = { Text("Search title, genre…", fontSize = 14.sp) },
                                singleLine = true,
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White)
                                        }
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFFE50914),
                                    unfocusedBorderColor = Color(0xFF2D3349)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("search_text_field")
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE50914)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = R.drawable.ic_popcorn_foreground,
                                        contentDescription = "Logo",
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Popcorn Time",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Stream Anything",
                                        color = Color(0xFF00B4D8),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                isSearchExpanded = !isSearchExpanded
                                if (!isSearchExpanded) viewModel.setSearchQuery("")
                            },
                            modifier = Modifier.testTag("search_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color.White
                            )
                        }

                        // Open Torrent / Magnet button
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF202434))
                                .clickable { viewModel.showMagnetDialog(true) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("open_magnet_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = "Magnet Link",
                                    tint = Color(0xFF00B4D8),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+ Magnet",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF0D0E12)
                    )
                )
            }
        },
        bottomBar = {
            if (isTopLevelTab) {
                NavigationBar(
                    containerColor = Color(0xFF13151D),
                    tonalElevation = 8.dp,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    NavigationBarItem(
                        selected = destination is NavigationTarget.MoviesTab,
                        onClick = { viewModel.navigateTo(NavigationTarget.MoviesTab) },
                        icon = { Icon(Icons.Default.LocalMovies, contentDescription = "Movies") },
                        label = { Text("Movies") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFE50914),
                            selectedTextColor = Color(0xFFE50914),
                            unselectedIconColor = Color(0xFF9CA3AF),
                            unselectedTextColor = Color(0xFF9CA3AF),
                            indicatorColor = Color(0xFFE50914).copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("nav_movies")
                    )

                    NavigationBarItem(
                        selected = destination is NavigationTarget.ShowsTab,
                        onClick = { viewModel.navigateTo(NavigationTarget.ShowsTab) },
                        icon = { Icon(Icons.Default.Tv, contentDescription = "TV Shows") },
                        label = { Text("TV Shows") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFE50914),
                            selectedTextColor = Color(0xFFE50914),
                            unselectedIconColor = Color(0xFF9CA3AF),
                            unselectedTextColor = Color(0xFF9CA3AF),
                            indicatorColor = Color(0xFFE50914).copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("nav_shows")
                    )

                    NavigationBarItem(
                        selected = destination is NavigationTarget.WatchlistTab,
                        onClick = { viewModel.navigateTo(NavigationTarget.WatchlistTab) },
                        icon = { Icon(Icons.Default.Bookmark, contentDescription = "Watchlist") },
                        label = { Text("Watchlist") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFE50914),
                            selectedTextColor = Color(0xFFE50914),
                            unselectedIconColor = Color(0xFF9CA3AF),
                            unselectedTextColor = Color(0xFF9CA3AF),
                            indicatorColor = Color(0xFFE50914).copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("nav_watchlist")
                    )

                    NavigationBarItem(
                        selected = destination is NavigationTarget.DownloadsTab,
                        onClick = { viewModel.navigateTo(NavigationTarget.DownloadsTab) },
                        icon = { Icon(Icons.Default.Download, contentDescription = "Downloads") },
                        label = { Text("Downloads") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFE50914),
                            selectedTextColor = Color(0xFFE50914),
                            unselectedIconColor = Color(0xFF9CA3AF),
                            unselectedTextColor = Color(0xFF9CA3AF),
                            indicatorColor = Color(0xFFE50914).copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("nav_downloads")
                    )

                    NavigationBarItem(
                        selected = destination is NavigationTarget.SettingsTab,
                        onClick = { viewModel.navigateTo(NavigationTarget.SettingsTab) },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFFE50914),
                            selectedTextColor = Color(0xFFE50914),
                            unselectedIconColor = Color(0xFF9CA3AF),
                            unselectedTextColor = Color(0xFF9CA3AF),
                            indicatorColor = Color(0xFFE50914).copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("nav_settings")
                    )
                }
            }
        },
        containerColor = Color(0xFF0D0E12)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (destination) {
                is NavigationTarget.MoviesTab -> MoviesScreen(viewModel = viewModel)
                is NavigationTarget.ShowsTab -> ShowsScreen(viewModel = viewModel)
                is NavigationTarget.WatchlistTab -> WatchlistScreen(viewModel = viewModel)
                is NavigationTarget.DownloadsTab -> DownloadsScreen(viewModel = viewModel)
                is NavigationTarget.SettingsTab -> SettingsScreen()
                is NavigationTarget.MovieDetail -> {
                    MovieDetailScreen(
                        movieId = (destination as NavigationTarget.MovieDetail).movieId,
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() }
                    )
                }
                is NavigationTarget.ShowDetail -> {
                    ShowDetailScreen(
                        showId = (destination as NavigationTarget.ShowDetail).showId,
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() }
                    )
                }
                is NavigationTarget.Player -> {
                    // Handled above before Scaffold
                }
            }
        }
    }
}
