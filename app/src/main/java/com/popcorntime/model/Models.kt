package com.popcorntime.model

enum class TorrentHealth(val label: String, val colorHex: Long) {
    EXCELLENT("Excellent", 0xFF5ABA00),
    GOOD("Good", 0xFFC9D400),
    MEDIUM("Medium", 0xFFD47800),
    BAD("Bad", 0xFFD40E00),
    UNKNOWN("Unknown", 0xFF696969)
}

data class Torrent(
    val quality: String, // "1080p", "720p", "480p", "3D"
    val seeds: Int,
    val peers: Int,
    val size: String,
    val url: String, // magnet or http stream url
    val health: TorrentHealth = calculateHealth(seeds, peers)
) {
    companion object {
        fun calculateHealth(seeds: Int, peers: Int): TorrentHealth {
            val ratio = if (peers > 0) seeds.toDouble() / peers.toDouble() else seeds.toDouble()
            val normalizedRatio = minOf(ratio / 5.0 * 100.0, 100.0)
            val normalizedSeeds = minOf(seeds.toDouble() / 30.0 * 100.0, 100.0)
            val weighted = (normalizedRatio * 0.6) + (normalizedSeeds * 0.4)
            val scaled = (weighted * 3.0) / 100.0
            return when {
                scaled >= 2.5 -> TorrentHealth.EXCELLENT
                scaled >= 1.7 -> TorrentHealth.GOOD
                scaled >= 0.8 -> TorrentHealth.MEDIUM
                scaled >= 0.0 -> TorrentHealth.BAD
                else -> TorrentHealth.UNKNOWN
            }
        }
    }
}

data class Subtitle(
    val language: String,
    val code: String,
    val url: String,
    val delaySeconds: Float = 0f
)

data class Episode(
    val id: String,
    val showId: String,
    val showTitle: String,
    val season: Int,
    val episode: Int,
    val title: String,
    val summary: String,
    val firstAired: String,
    val backdropImage: String,
    val torrents: List<Torrent> = emptyList(),
    val subtitles: List<Subtitle> = emptyList(),
    var isWatched: Boolean = false,
    var progressSeconds: Long = 0,
    var durationSeconds: Long = 0
)

data class Movie(
    val id: String,
    val title: String,
    val year: String,
    val rating: Double, // 0..10 or percentage
    val runtimeMinutes: Int,
    val summary: String,
    val certification: String = "PG-13",
    val posterImage: String,
    val backdropImage: String,
    val trailerUrl: String? = null,
    val genres: List<String> = emptyList(),
    val torrents: List<Torrent> = emptyList(),
    val subtitles: List<Subtitle> = emptyList(),
    var isWatched: Boolean = false,
    var isWatchlist: Boolean = false,
    var progressSeconds: Long = 0,
    var durationSeconds: Long = 0
)

data class Show(
    val id: String,
    val tvdbId: String,
    val title: String,
    val year: String,
    val rating: Double,
    val summary: String,
    val network: String? = "Netflix",
    val status: String? = "Returning Series",
    val posterImage: String,
    val backdropImage: String,
    val genres: List<String> = emptyList(),
    val episodes: List<Episode> = emptyList(),
    val seasonNumbers: List<Int> = episodes.map { it.season }.distinct().sorted(),
    var isWatchlist: Boolean = false
) {
    fun latestUnwatchedEpisode(): Episode? {
        return episodes.firstOrNull { !it.isWatched } ?: episodes.firstOrNull()
    }
}

enum class MediaSortFilter(val label: String, val apiParam: String) {
    TRENDING("Trending", "trending"),
    POPULARITY("Popular", "seeds"),
    TOP_RATED("Top Rated", "rating"),
    RECENTLY_ADDED("Recently Added", "last added"),
    YEAR("New Releases", "year")
}

data class DownloadItem(
    val id: String,
    val title: String,
    val subtitleInfo: String,
    val posterImage: String,
    val quality: String,
    val totalSize: String,
    var progress: Float = 0f, // 0..1
    var downloadSpeed: String = "0 KB/s",
    var seeds: Int = 0,
    var peers: Int = 0,
    var isPaused: Boolean = false,
    var isCompleted: Boolean = false,
    val streamUrl: String
)
