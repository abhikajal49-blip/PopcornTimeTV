package com.popcorntime.data

import android.content.Context
import android.content.SharedPreferences
import com.popcorntime.model.DownloadItem
import com.popcorntime.model.Episode
import com.popcorntime.model.MediaSortFilter
import com.popcorntime.model.Movie
import com.popcorntime.model.Show
import com.popcorntime.model.Subtitle
import com.popcorntime.model.Torrent
import com.popcorntime.model.TorrentHealth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class PopcornRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("popcorn_prefs", Context.MODE_PRIVATE)

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val _watchlistMovies = MutableStateFlow<Set<String>>(loadStringSet("watchlist_movies"))
    val watchlistMovies: StateFlow<Set<String>> = _watchlistMovies.asStateFlow()

    private val _watchlistShows = MutableStateFlow<Set<String>>(loadStringSet("watchlist_shows"))
    val watchlistShows: StateFlow<Set<String>> = _watchlistShows.asStateFlow()

    private val _watchedMedia = MutableStateFlow<Set<String>>(loadStringSet("watched_media"))
    val watchedMedia: StateFlow<Set<String>> = _watchedMedia.asStateFlow()

    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    // In-memory catalog cache
    private val localMovies = mutableListOf<Movie>()
    private val localShows = mutableListOf<Show>()

    init {
        initializeCuratedCatalog()
    }

    private fun loadStringSet(key: String): Set<String> {
        return prefs.getStringSet(key, emptySet()) ?: emptySet()
    }

    private fun saveStringSet(key: String, set: Set<String>) {
        prefs.edit().putStringSet(key, set).apply()
    }

    fun toggleWatchlistMovie(movieId: String) {
        val current = _watchlistMovies.value.toMutableSet()
        if (current.contains(movieId)) current.remove(movieId) else current.add(movieId)
        _watchlistMovies.value = current
        saveStringSet("watchlist_movies", current)
    }

    fun toggleWatchlistShow(showId: String) {
        val current = _watchlistShows.value.toMutableSet()
        if (current.contains(showId)) current.remove(showId) else current.add(showId)
        _watchlistShows.value = current
        saveStringSet("watchlist_shows", current)
    }

    fun toggleWatched(mediaId: String) {
        val current = _watchedMedia.value.toMutableSet()
        if (current.contains(mediaId)) current.remove(mediaId) else current.add(mediaId)
        _watchedMedia.value = current
        saveStringSet("watched_media", current)
    }

    fun isWatchlistMovie(id: String): Boolean = _watchlistMovies.value.contains(id)
    fun isWatchlistShow(id: String): Boolean = _watchlistShows.value.contains(id)
    fun isWatched(id: String): Boolean = _watchedMedia.value.contains(id)

    fun saveProgress(mediaId: String, progressSec: Long, durationSec: Long) {
        prefs.edit()
            .putLong("progress_$mediaId", progressSec)
            .putLong("duration_$mediaId", durationSec)
            .apply()
    }

    fun getProgress(mediaId: String): Pair<Long, Long> {
        val progress = prefs.getLong("progress_$mediaId", 0L)
        val duration = prefs.getLong("duration_$mediaId", 0L)
        return Pair(progress, duration)
    }

    suspend fun getMovies(
        sort: MediaSortFilter = MediaSortFilter.TRENDING,
        genre: String = "All",
        search: String = ""
    ): List<Movie> = withContext(Dispatchers.IO) {
        // Try fetching from popcorn movies API if reachable
        try {
            val genreParam = if (genre == "All") "" else "&genre=${genre.lowercase()}"
            val searchParam = if (search.isNotBlank()) "&keywords=${search.trim()}" else ""
            val url = "https://movies-v2.api-fetch.sh/movies/1?sort=${sort.apiParam}&order=-1$genreParam$searchParam"

            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonStr = response.body?.string()
                if (!jsonStr.isNullOrBlank()) {
                    val jsonArray = JSONArray(jsonStr)
                    val apiMovies = mutableListOf<Movie>()
                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.getJSONObject(i)
                        val movie = parseMovieJson(item)
                        if (movie != null) apiMovies.add(movie)
                    }
                    if (apiMovies.isNotEmpty()) {
                        return@withContext apiMovies
                    }
                }
            }
        } catch (_: Exception) {
            // Fallback to rich curated local catalog
        }

        // Curated local filter
        var list = localMovies.toList()
        if (search.isNotBlank()) {
            list = list.filter { it.title.contains(search, ignoreCase = true) || it.genres.any { g -> g.contains(search, ignoreCase = true) } }
        }
        if (genre != "All") {
            list = list.filter { it.genres.any { g -> g.equals(genre, ignoreCase = true) } }
        }
        when (sort) {
            MediaSortFilter.TRENDING, MediaSortFilter.POPULARITY -> list.sortedByDescending { it.rating }
            MediaSortFilter.TOP_RATED -> list.sortedByDescending { it.rating }
            MediaSortFilter.RECENTLY_ADDED, MediaSortFilter.YEAR -> list.sortedByDescending { it.year }
        }
    }

    suspend fun getShows(
        sort: MediaSortFilter = MediaSortFilter.POPULARITY,
        genre: String = "All",
        search: String = ""
    ): List<Show> = withContext(Dispatchers.IO) {
        try {
            val genreParam = if (genre == "All") "" else "&genre=${genre.lowercase()}"
            val searchParam = if (search.isNotBlank()) "&keywords=${search.trim()}" else ""
            val url = "https://tv-v2.api-fetch.sh/shows/1?sort=${sort.apiParam}&order=-1$genreParam$searchParam"

            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonStr = response.body?.string()
                if (!jsonStr.isNullOrBlank()) {
                    val jsonArray = JSONArray(jsonStr)
                    val apiShows = mutableListOf<Show>()
                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.getJSONObject(i)
                        val show = parseShowJson(item)
                        if (show != null) apiShows.add(show)
                    }
                    if (apiShows.isNotEmpty()) {
                        return@withContext apiShows
                    }
                }
            }
        } catch (_: Exception) {
            // Fallback to local curated
        }

        var list = localShows.toList()
        if (search.isNotBlank()) {
            list = list.filter { it.title.contains(search, ignoreCase = true) || it.genres.any { g -> g.contains(search, ignoreCase = true) } }
        }
        if (genre != "All") {
            list = list.filter { it.genres.any { g -> g.equals(genre, ignoreCase = true) } }
        }
        when (sort) {
            MediaSortFilter.TRENDING, MediaSortFilter.POPULARITY -> list.sortedByDescending { it.rating }
            MediaSortFilter.TOP_RATED -> list.sortedByDescending { it.rating }
            MediaSortFilter.RECENTLY_ADDED, MediaSortFilter.YEAR -> list.sortedByDescending { it.year }
        }
    }

    fun getMovieById(id: String): Movie? {
        val m = localMovies.find { it.id == id } ?: return null
        val (prog, dur) = getProgress(id)
        return m.copy(
            isWatchlist = isWatchlistMovie(id),
            isWatched = isWatched(id),
            progressSeconds = prog,
            durationSeconds = dur
        )
    }

    fun getShowById(id: String): Show? {
        val s = localShows.find { it.id == id } ?: return null
        return s.copy(
            isWatchlist = isWatchlistShow(id),
            episodes = s.episodes.map { ep ->
                val (prog, dur) = getProgress(ep.id)
                ep.copy(
                    isWatched = isWatched(ep.id),
                    progressSeconds = prog,
                    durationSeconds = dur
                )
            }
        )
    }

    fun addDownload(item: DownloadItem) {
        val current = _downloads.value.toMutableList()
        if (current.none { it.id == item.id }) {
            current.add(0, item)
            _downloads.value = current
        }
    }

    fun updateDownload(updated: DownloadItem) {
        val current = _downloads.value.map { if (it.id == updated.id) updated else it }
        _downloads.value = current
    }

    fun removeDownload(id: String) {
        _downloads.value = _downloads.value.filter { it.id != id }
    }

    fun addCustomTorrent(title: String, url: String, quality: String = "1080p"): Movie {
        val customId = "magnet_${System.currentTimeMillis()}"
        val sampleStreams = listOf(
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"
        )
        val streamUrl = if (url.startsWith("http://") || url.startsWith("https://")) url else sampleStreams.random()

        val movie = Movie(
            id = customId,
            title = title.ifBlank { "Custom Torrent Stream" },
            year = "2024",
            rating = 8.8,
            runtimeMinutes = 115,
            summary = "Custom imported magnet link: $url",
            certification = "NR",
            posterImage = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=500&q=80",
            backdropImage = "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=1280&q=80",
            genres = listOf("Imported", "Torrent"),
            torrents = listOf(
                Torrent(quality, 64, 18, "1.85 GB", streamUrl, TorrentHealth.EXCELLENT)
            ),
            subtitles = listOf(
                Subtitle("English", "en", "https://example.com/subs/en.vtt"),
                Subtitle("Spanish", "es", "https://example.com/subs/es.vtt")
            )
        )
        localMovies.add(0, movie)
        return movie
    }

    private fun parseMovieJson(obj: JSONObject): Movie? {
        return try {
            val id = obj.optString("imdb_id").ifEmpty { obj.optString("_id") }
            val title = obj.optString("title", "Unknown")
            val year = obj.optString("year", "2024")
            val rating = obj.optJSONObject("rating")?.optDouble("percentage", 80.0)?.div(10.0) ?: 8.0
            val runtime = obj.optInt("runtime", 110)
            val synopsis = obj.optString("synopsis", "No synopsis available.")
            val certification = obj.optString("certification", "PG-13")
            val images = obj.optJSONObject("images")
            val poster = images?.optString("poster", "")?.replace("w500", "w780")
                ?: "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=500"
            val fanart = images?.optString("fanart", "")?.replace("w500", "original")
                ?: "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1280"

            val genresList = mutableListOf<String>()
            val genresArr = obj.optJSONArray("genres")
            if (genresArr != null) {
                for (j in 0 until genresArr.length()) {
                    genresList.add(genresArr.getString(j))
                }
            }

            val torrentsList = mutableListOf<Torrent>()
            val torrentsObj = obj.optJSONObject("torrents")?.optJSONObject("en")
            if (torrentsObj != null) {
                val keys = torrentsObj.keys()
                while (keys.hasNext()) {
                    val q = keys.next()
                    val tObj = torrentsObj.getJSONObject(q)
                    val seeds = tObj.optInt("seed", tObj.optInt("seeds", 20))
                    val peers = tObj.optInt("peer", tObj.optInt("peers", 10))
                    val size = tObj.optString("filesize", "1.5 GB")
                    val url = tObj.optString("url", "")
                    torrentsList.add(
                        Torrent(
                            quality = q,
                            seeds = seeds,
                            peers = peers,
                            size = size,
                            url = url,
                            health = Torrent.calculateHealth(seeds, peers)
                        )
                    )
                }
            }

            Movie(
                id = id,
                title = title,
                year = year,
                rating = rating,
                runtimeMinutes = runtime,
                summary = synopsis,
                certification = certification,
                posterImage = poster,
                backdropImage = fanart,
                genres = genresList,
                torrents = torrentsList,
                isWatchlist = isWatchlistMovie(id),
                isWatched = isWatched(id)
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseShowJson(obj: JSONObject): Show? {
        return try {
            val id = obj.optString("imdb_id").ifEmpty { obj.optString("_id") }
            val tvdbId = obj.optString("tvdb_id", "")
            val title = obj.optString("title", "Unknown Show")
            val year = obj.optString("year", "2024")
            val rating = obj.optJSONObject("rating")?.optDouble("percentage", 85.0)?.div(10.0) ?: 8.5
            val synopsis = obj.optString("synopsis", "No synopsis available.")
            val images = obj.optJSONObject("images")
            val poster = images?.optString("poster", "") ?: ""
            val fanart = images?.optString("fanart", "") ?: ""

            val genresList = mutableListOf<String>()
            val genresArr = obj.optJSONArray("genres")
            if (genresArr != null) {
                for (j in 0 until genresArr.length()) {
                    genresList.add(genresArr.getString(j))
                }
            }

            Show(
                id = id,
                tvdbId = tvdbId,
                title = title,
                year = year,
                rating = rating,
                summary = synopsis,
                posterImage = poster,
                backdropImage = fanart,
                genres = genresList,
                episodes = emptyList(),
                isWatchlist = isWatchlistShow(id)
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun initializeCuratedCatalog() {
        val sampleVideo1 = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        val sampleVideo2 = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
        val sampleVideo3 = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"
        val sampleVideo4 = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"

        val defaultSubs = listOf(
            Subtitle("English", "en", "https://example.com/subs/en.vtt"),
            Subtitle("Spanish", "es", "https://example.com/subs/es.vtt"),
            Subtitle("French", "fr", "https://example.com/subs/fr.vtt"),
            Subtitle("German", "de", "https://example.com/subs/de.vtt")
        )

        localMovies.addAll(
            listOf(
                Movie(
                    id = "tt0137523",
                    title = "Fight Club",
                    year = "1999",
                    rating = 8.8,
                    runtimeMinutes = 139,
                    summary = "An insomniac office worker and a devil-may-care soap maker form an underground fight club that evolves into much more.",
                    certification = "R",
                    posterImage = "https://images.unsplash.com/photo-1594909122845-11baa439b7bf?w=500&q=80",
                    backdropImage = "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=1280&q=80",
                    genres = listOf("Drama", "Thriller"),
                    torrents = listOf(
                        Torrent("1080p", 420, 48, "2.1 GB", sampleVideo1, TorrentHealth.EXCELLENT),
                        Torrent("720p", 195, 22, "1.1 GB", sampleVideo2, TorrentHealth.GOOD),
                        Torrent("480p", 45, 8, "750 MB", sampleVideo3, TorrentHealth.MEDIUM)
                    ),
                    subtitles = defaultSubs
                ),
                Movie(
                    id = "tt1375666",
                    title = "Inception",
                    year = "2010",
                    rating = 8.8,
                    runtimeMinutes = 148,
                    summary = "A thief who steals corporate secrets through the use of dream-sharing technology is given the inverse task of planting an idea into the mind of a C.E.O.",
                    certification = "PG-13",
                    posterImage = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=500&q=80",
                    backdropImage = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1280&q=80",
                    genres = listOf("Action", "Sci-Fi", "Adventure"),
                    torrents = listOf(
                        Torrent("1080p", 650, 85, "2.4 GB", sampleVideo2, TorrentHealth.EXCELLENT),
                        Torrent("720p", 320, 40, "1.3 GB", sampleVideo1, TorrentHealth.EXCELLENT)
                    ),
                    subtitles = defaultSubs
                ),
                Movie(
                    id = "tt0816692",
                    title = "Interstellar",
                    year = "2014",
                    rating = 8.7,
                    runtimeMinutes = 169,
                    summary = "When Earth becomes uninhabitable in the future, a farmer and ex-NASA pilot, Joseph Cooper, is tasked to pilot a spacecraft along with a team of researchers to find a new planet for humans.",
                    certification = "PG-13",
                    posterImage = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=500&q=80",
                    backdropImage = "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=1280&q=80",
                    genres = listOf("Adventure", "Drama", "Sci-Fi"),
                    torrents = listOf(
                        Torrent("1080p", 540, 60, "2.8 GB", sampleVideo3, TorrentHealth.EXCELLENT),
                        Torrent("720p", 280, 35, "1.5 GB", sampleVideo4, TorrentHealth.GOOD)
                    ),
                    subtitles = defaultSubs
                ),
                Movie(
                    id = "tt1160419",
                    title = "Dune: Part Two",
                    year = "2024",
                    rating = 8.6,
                    runtimeMinutes = 166,
                    summary = "Paul Atreides unites with Chani and the Fremen while seeking revenge against the conspirators who destroyed his family.",
                    certification = "PG-13",
                    posterImage = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=500&q=80",
                    backdropImage = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1280&q=80",
                    genres = listOf("Action", "Adventure", "Sci-Fi"),
                    torrents = listOf(
                        Torrent("1080p", 890, 110, "2.6 GB", sampleVideo1, TorrentHealth.EXCELLENT),
                        Torrent("720p", 410, 50, "1.4 GB", sampleVideo2, TorrentHealth.EXCELLENT)
                    ),
                    subtitles = defaultSubs
                ),
                Movie(
                    id = "tt0468569",
                    title = "The Dark Knight",
                    year = "2008",
                    rating = 9.0,
                    runtimeMinutes = 152,
                    summary = "When the menace known as the Joker wreaks havoc and chaos on the people of Gotham, Batman must accept one of the greatest psychological and physical tests of his ability to fight injustice.",
                    certification = "PG-13",
                    posterImage = "https://images.unsplash.com/photo-1509347528160-9a9e33742cdb?w=500&q=80",
                    backdropImage = "https://images.unsplash.com/photo-1514565131-fce0801e5785?w=1280&q=80",
                    genres = listOf("Action", "Crime", "Drama"),
                    torrents = listOf(
                        Torrent("1080p", 720, 90, "2.3 GB", sampleVideo2, TorrentHealth.EXCELLENT),
                        Torrent("720p", 360, 45, "1.2 GB", sampleVideo3, TorrentHealth.GOOD)
                    ),
                    subtitles = defaultSubs
                ),
                Movie(
                    id = "tt0110912",
                    title = "Pulp Fiction",
                    year = "1994",
                    rating = 8.9,
                    runtimeMinutes = 154,
                    summary = "The lives of two mob hitmen, a boxer, a gangster and his wife, and a pair of diner bandits intertwine in four tales of violence and redemption.",
                    certification = "R",
                    posterImage = "https://images.unsplash.com/photo-1594909122845-11baa439b7bf?w=500&q=80",
                    backdropImage = "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=1280&q=80",
                    genres = listOf("Crime", "Drama"),
                    torrents = listOf(
                        Torrent("1080p", 480, 52, "2.0 GB", sampleVideo4, TorrentHealth.EXCELLENT),
                        Torrent("720p", 210, 25, "1.1 GB", sampleVideo1, TorrentHealth.GOOD)
                    ),
                    subtitles = defaultSubs
                ),
                Movie(
                    id = "tt15398776",
                    title = "Oppenheimer",
                    year = "2023",
                    rating = 8.9,
                    runtimeMinutes = 180,
                    summary = "The story of American scientist J. Robert Oppenheimer and his role in the development of the atomic bomb during World War II.",
                    certification = "R",
                    posterImage = "https://images.unsplash.com/photo-1579783902614-a3fb3927b675?w=500&q=80",
                    backdropImage = "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=1280&q=80",
                    genres = listOf("Biography", "Drama", "History"),
                    torrents = listOf(
                        Torrent("1080p", 950, 120, "3.1 GB", sampleVideo3, TorrentHealth.EXCELLENT),
                        Torrent("720p", 520, 60, "1.6 GB", sampleVideo2, TorrentHealth.EXCELLENT)
                    ),
                    subtitles = defaultSubs
                ),
                Movie(
                    id = "tt2322441",
                    title = "Spider-Man: Across the Spider-Verse",
                    year = "2023",
                    rating = 8.7,
                    runtimeMinutes = 140,
                    summary = "Miles Morales catapults across the Multiverse, where he encounters a team of Spider-People charged with protecting its very existence.",
                    certification = "PG",
                    posterImage = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=500&q=80",
                    backdropImage = "https://images.unsplash.com/photo-1635805737707-575885ab0820?w=1280&q=80",
                    genres = listOf("Animation", "Action", "Adventure"),
                    torrents = listOf(
                        Torrent("1080p", 840, 95, "2.2 GB", sampleVideo1, TorrentHealth.EXCELLENT),
                        Torrent("720p", 400, 48, "1.2 GB", sampleVideo4, TorrentHealth.GOOD)
                    ),
                    subtitles = defaultSubs
                )
            )
        )

        // Populate curated TV Shows with seasons & episodes
        val strangerThingsEpisodes = listOf(
            Episode("st_s1e1", "tt4574334", "Stranger Things", 1, 1, "The Vanishing of Will Byers", "On his way home from a friend's house, young Will sees something terrifying. Nearby, a sinister secret lurks in the depths of a government lab.", "2016-07-15", "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=800", listOf(Torrent("1080p", 320, 40, "850 MB", sampleVideo1)), defaultSubs),
            Episode("st_s1e2", "tt4574334", "Stranger Things", 1, 2, "The Weirdo on Maple Street", "Lucas, Mike and Dustin try to talk to the girl they found in the woods. Hopper questions an anxious Joyce about a suspicious phone call.", "2016-07-15", "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800", listOf(Torrent("1080p", 290, 35, "820 MB", sampleVideo2)), defaultSubs),
            Episode("st_s1e3", "tt4574334", "Stranger Things", 1, 3, "Holly, Jolly", "An increasingly concerned Nancy looks for Barb and finds out what Jonathan's been up to. Joyce is convinced Will is trying to talk to her.", "2016-07-15", "https://images.unsplash.com/photo-1514565131-fce0801e5785?w=800", listOf(Torrent("1080p", 280, 30, "800 MB", sampleVideo3)), defaultSubs),
            Episode("st_s1e4", "tt4574334", "Stranger Things", 1, 4, "The Body", "Refusing to believe Will is dead, Joyce tries to connect with her son. The boys give Eleven a makeover.", "2016-07-15", "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=800", listOf(Torrent("1080p", 250, 28, "810 MB", sampleVideo4)), defaultSubs),
            Episode("st_s2e1", "tt4574334", "Stranger Things", 2, 1, "MADMAX", "As the town preps for Halloween, a high-scoring rival shakes things up at the arcade, and a skeptical Hopper inspects a field of rotting pumpkins.", "2017-10-27", "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=800", listOf(Torrent("1080p", 310, 36, "880 MB", sampleVideo1)), defaultSubs)
        )

        val severanceEpisodes = listOf(
            Episode("sev_s1e1", "tt11280740", "Severance", 1, 1, "Good News About Hell", "Mark Scout leads a team at Lumon Industries, whose employees have undergone a severance procedure that divides their memories.", "2022-02-18", "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=800", listOf(Torrent("1080p", 420, 50, "920 MB", sampleVideo2)), defaultSubs),
            Episode("sev_s1e2", "tt11280740", "Severance", 1, 2, "Half Loop", "The macrodata refinement team trains Helly on data refinement. Mark meets with a mysterious former colleague off the clock.", "2022-02-18", "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=800", listOf(Torrent("1080p", 380, 42, "900 MB", sampleVideo3)), defaultSubs),
            Episode("sev_s1e3", "tt11280740", "Severance", 1, 3, "In Perpetuity", "Mark takes the team on a field trip to the Perpetuity Wing, but Helly continues to rebel against the Lumon corporate protocol.", "2022-02-25", "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=800", listOf(Torrent("1080p", 360, 40, "890 MB", sampleVideo4)), defaultSubs)
        )

        val arcaneEpisodes = listOf(
            Episode("arc_s1e1", "tt11126994", "Arcane", 1, 1, "Welcome to the Playground", "Orphaned sisters Vi and Powder bring trouble to Zaun's underground streets following a heist in posh Piltover.", "2021-11-06", "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=800", listOf(Torrent("1080p", 620, 80, "1.1 GB", sampleVideo1)), defaultSubs),
            Episode("arc_s1e2", "tt11126994", "Arcane", 1, 2, "Some Mysteries Are Better Left Unsolved", "Idealistic inventor Jayce risks expulsion when his illicit research into magic is exposed by city councilors.", "2021-11-06", "https://images.unsplash.com/photo-1635805737707-575885ab0820?w=800", listOf(Torrent("1080p", 580, 75, "1.0 GB", sampleVideo2)), defaultSubs),
            Episode("arc_s1e3", "tt11126994", "Arcane", 1, 3, "The Base Violence Necessary for Change", "An epic showdown between old rivals results in a fateful moment for Zaun. Jayce and Viktor risk everything for science.", "2021-11-06", "https://images.unsplash.com/photo-1579783902614-a3fb3927b675?w=800", listOf(Torrent("1080p", 600, 78, "1.05 GB", sampleVideo3)), defaultSubs)
        )

        localShows.addAll(
            listOf(
                Show(
                    id = "tt4574334",
                    tvdbId = "305288",
                    title = "Stranger Things",
                    year = "2016-2024",
                    rating = 8.7,
                    summary = "When a young boy vanishes, a small town uncovers a mystery involving secret experiments, terrifying supernatural forces and one strange little girl.",
                    network = "Netflix",
                    status = "Returning Series",
                    posterImage = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=500&q=80",
                    backdropImage = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1280&q=80",
                    genres = listOf("Drama", "Fantasy", "Horror", "Sci-Fi"),
                    episodes = strangerThingsEpisodes
                ),
                Show(
                    id = "tt11280740",
                    tvdbId = "371980",
                    title = "Severance",
                    year = "2022-",
                    rating = 8.7,
                    summary = "Mark leads a team of office workers whose memories have been surgically divided between their work and personal lives.",
                    network = "Apple TV+",
                    status = "Returning Series",
                    posterImage = "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=500&q=80",
                    backdropImage = "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=1280&q=80",
                    genres = listOf("Drama", "Mystery", "Sci-Fi", "Thriller"),
                    episodes = severanceEpisodes
                ),
                Show(
                    id = "tt11126994",
                    tvdbId = "370886",
                    title = "Arcane",
                    year = "2021-2024",
                    rating = 9.0,
                    summary = "Set in the utopian region of Piltover and the oppressed underground of Zaun, the story follows the origins of two iconic League champions-and the power that will tear them apart.",
                    network = "Netflix",
                    status = "Ended",
                    posterImage = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=500&q=80",
                    backdropImage = "https://images.unsplash.com/photo-1635805737707-575885ab0820?w=1280&q=80",
                    genres = listOf("Animation", "Action", "Adventure", "Sci-Fi"),
                    episodes = arcaneEpisodes
                )
            )
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: PopcornRepository? = null

        fun getInstance(context: Context): PopcornRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PopcornRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
