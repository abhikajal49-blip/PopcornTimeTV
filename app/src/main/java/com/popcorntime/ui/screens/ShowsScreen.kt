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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.popcorntime.model.MediaSortFilter
import com.popcorntime.ui.components.MediaPosterCard
import com.popcorntime.viewmodel.NavigationTarget
import com.popcorntime.viewmodel.PopcornViewModel

@Composable
fun ShowsScreen(
    viewModel: PopcornViewModel,
    modifier: Modifier = Modifier
) {
    val shows by viewModel.shows.collectAsState()
    val isLoading by viewModel.isShowsLoading.collectAsState()
    val activeSort by viewModel.showSortFilter.collectAsState()
    val activeGenre by viewModel.showGenre.collectAsState()

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
                            .clickable { viewModel.setShowSort(sort) }
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
                        onClick = { viewModel.setShowGenre(genre) },
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
                    .testTag("shows_grid")
            ) {
                items(shows, key = { it.id }) { show ->
                    MediaPosterCard(
                        title = show.title,
                        posterUrl = show.posterImage,
                        rating = show.rating,
                        year = show.year,
                        qualityBadge = "${show.seasonNumbers.size.coerceAtLeast(1)}S",
                        onClick = {
                            viewModel.navigateTo(NavigationTarget.ShowDetail(show.id))
                        }
                    )
                }
            }
        }
    }
}
