package com.popcorntime.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.popcorntime.data.PopcornRepository
import com.popcorntime.model.DownloadItem
import com.popcorntime.model.Episode
import com.popcorntime.model.MediaSortFilter
import com.popcorntime.model.Movie
import com.popcorntime.model.Show
import com.popcorntime.model.Subtitle
import com.popcorntime.model.Torrent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

sealed class NavigationTarget {
    data object MoviesTab : NavigationTarget()
    data object ShowsTab : NavigationTarget()
    data object WatchlistTab : NavigationTarget()
    data object DownloadsTab : NavigationTarget()
    data object SettingsTab : NavigationTarget()
    data class MovieDetail(val movieId: String) : NavigationTarget()
    data class ShowDetail(val showId: String) : NavigationTarget()
    data class Player(
        val title: String,
        val subtitleInfo: String,
        val streamUrl: String,
        val torrent: Torrent?,
        val subtitles: List<Subtitle>,
        val mediaId: String,
        val nextEpisode: Episode? = null
    ) : NavigationTarget()
}

class PopcornViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PopcornRepository.getInstance(application)

    private val _currentDestination = MutableStateFlow<NavigationTarget>(NavigationTarget.MoviesTab)
    val currentDestination: StateFlow<NavigationTarget> = _currentDestination.asStateFlow()

    // Navigation stack for back navigation
    private val backStack = mutableListOf<NavigationTarget>()

    // Movies
    private val _movies = MutableStateFlow<List<Movie>>(emptyList())
    val movies: StateFlow<List<Movie>> = _movies.asStateFlow()

    private val _movieSortFilter = MutableStateFlow(MediaSortFilter.TRENDING)
    val movieSortFilter: StateFlow<MediaSortFilter> = _movieSortFilter.asStateFlow()

    private val _movieGenre = MutableStateFlow("All")
    val movieGenre: StateFlow<String> = _movieGenre.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isMoviesLoading = MutableStateFlow(false)
    val isMoviesLoading: StateFlow<Boolean> = _isMoviesLoading.asStateFlow()

    // Shows
    private val _shows = MutableStateFlow<List<Show>>(emptyList())
    val shows: StateFlow<List<Show>> = _shows.asStateFlow()

    private val _showSortFilter = MutableStateFlow(MediaSortFilter.POPULARITY)
    val showSortFilter: StateFlow<MediaSortFilter> = _showSortFilter.asStateFlow()

    private val _showGenre = MutableStateFlow("All")
    val showGenre: StateFlow<String> = _showGenre.asStateFlow()

    private val _isShowsLoading = MutableStateFlow(false)
    val isShowsLoading: StateFlow<Boolean> = _isShowsLoading.asStateFlow()

    // Watchlists & Downloads from repo
    val watchlistMovieIds = repository.watchlistMovies
    val watchlistShowIds = repository.watchlistShows
    val downloads = repository.downloads

    // Downloads simulation job
    private var downloadSimulationJob: Job? = null

    // Magnet dialog state
    private val _showMagnetDialog = MutableStateFlow(false)
    val showMagnetDialog: StateFlow<Boolean> = _showMagnetDialog.asStateFlow()

    val availableGenres = listOf(
        "All", "Action", "Adventure", "Animation", "Comedy", "Crime",
        "Drama", "Fantasy", "Horror", "Mystery", "Romance", "Sci-Fi", "Thriller"
    )

    init {
        loadMovies()
        loadShows()
        startDownloadSimulationLoop()
    }

    fun navigateTo(target: NavigationTarget) {
        if (_currentDestination.value != target) {
            backStack.add(_currentDestination.value)
            _currentDestination.value = target
        }
    }

    fun navigateBack(): Boolean {
        if (backStack.isNotEmpty()) {
            val prev = backStack.removeAt(backStack.size - 1)
            _currentDestination.value = prev
            return true
        }
        return false
    }

    fun setMovieSort(sort: MediaSortFilter) {
        _movieSortFilter.value = sort
        loadMovies()
    }

    fun setMovieGenre(genre: String) {
        _movieGenre.value = genre
        loadMovies()
    }

    fun setShowSort(sort: MediaSortFilter) {
        _showSortFilter.value = sort
        loadShows()
    }

    fun setShowGenre(genre: String) {
        _showGenre.value = genre
        loadShows()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        loadMovies()
        loadShows()
    }

    fun loadMovies() {
        viewModelScope.launch {
            _isMoviesLoading.value = true
            val list = repository.getMovies(
                sort = _movieSortFilter.value,
                genre = _movieGenre.value,
                search = _searchQuery.value
            )
            _movies.value = list
            _isMoviesLoading.value = false
        }
    }

    fun loadShows() {
        viewModelScope.launch {
            _isShowsLoading.value = true
            val list = repository.getShows(
                sort = _showSortFilter.value,
                genre = _showGenre.value,
                search = _searchQuery.value
            )
            _shows.value = list
            _isShowsLoading.value = false
        }
    }

    fun getMovie(id: String): Movie? = repository.getMovieById(id)
    fun getShow(id: String): Show? = repository.getShowById(id)

    fun toggleWatchlistMovie(id: String) {
        repository.toggleWatchlistMovie(id)
        loadMovies()
    }

    fun toggleWatchlistShow(id: String) {
        repository.toggleWatchlistShow(id)
        loadShows()
    }

    fun toggleWatched(id: String) {
        repository.toggleWatched(id)
    }

    fun saveProgress(id: String, progressSec: Long, durationSec: Long) {
        repository.saveProgress(id, progressSec, durationSec)
    }

    fun showMagnetDialog(show: Boolean) {
        _showMagnetDialog.value = show
    }

    fun importMagnet(title: String, url: String) {
        val movie = repository.addCustomTorrent(title, url)
        loadMovies()
        _showMagnetDialog.value = false
        // Immediately launch detail or player
        navigateTo(NavigationTarget.MovieDetail(movie.id))
    }

    fun startDownload(
        id: String,
        title: String,
        subtitleInfo: String,
        posterImage: String,
        quality: String,
        size: String,
        streamUrl: String
    ) {
        val item = DownloadItem(
            id = id,
            title = title,
            subtitleInfo = subtitleInfo,
            posterImage = posterImage,
            quality = quality,
            totalSize = size,
            progress = 0.05f,
            downloadSpeed = "2.8 MB/s",
            seeds = 45,
            peers = 12,
            isPaused = false,
            isCompleted = false,
            streamUrl = streamUrl
        )
        repository.addDownload(item)
    }

    fun pauseDownload(id: String) {
        val item = downloads.value.find { it.id == id } ?: return
        repository.updateDownload(item.copy(isPaused = !item.isPaused))
    }

    fun cancelDownload(id: String) {
        repository.removeDownload(id)
    }

    private fun startDownloadSimulationLoop() {
        downloadSimulationJob?.cancel()
        downloadSimulationJob = viewModelScope.launch {
            while (true) {
                delay(1500)
                val currentList = downloads.value
                if (currentList.isNotEmpty()) {
                    currentList.forEach { item ->
                        if (!item.isPaused && !item.isCompleted) {
                            val newProgress = (item.progress + 0.04f).coerceAtMost(1f)
                            val isNowDone = newProgress >= 1f
                            val speed = if (isNowDone) "0 KB/s" else "${Random.nextInt(2, 5)}.${Random.nextInt(1, 9)} MB/s"
                            repository.updateDownload(
                                item.copy(
                                    progress = newProgress,
                                    isCompleted = isNowDone,
                                    downloadSpeed = speed,
                                    seeds = Random.nextInt(35, 75),
                                    peers = Random.nextInt(10, 25)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
