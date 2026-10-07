# Popcorn Time for Android

Modern Android rebuild of Popcorn Time built with **Kotlin** and **Jetpack Compose**.

## Features

- **Movies & TV Shows Catalogs**: Browse Trending, Popular, Top Rated, and Recently Added titles with comprehensive genre filtering and real-time search.
- **Rich Media Details**: Full metadata including IMDb ratings, certification badges (PG-13, R), runtimes, synopsis, and season/episode guides.
- **Torrent Health Telemetry**: Live torrent health monitoring (Seeds, Peers, Stream Speed, File Size, and calculated health score: Excellent, Good, Medium, Bad).
- **Video Streaming Dashboard (Media3 ExoPlayer)**:
  - High-performance video streaming with auto-hiding playback controls.
  - Live torrent buffer progress indicator and telemetry overlay.
  - Multi-language subtitle track selection with subtitle delay synchronization (+/- 3s).
  - Aspect ratio toggle (Fit, Zoom, Fill).
  - 10-second skip forward / backward.
- **Watchlist & Continue Watching**: Bookmark favorite content and seamlessly resume playback where you left off.
- **Custom Magnet & Torrent Links**: Paste any magnet URI or torrent streaming link to stream directly.
- **Offline Downloads Manager**: Background download queue with simulated torrent piece progress and offline playback.
- **Settings & Caching**: Customizable playback quality preferences, subtitle styling, and cache management.

## Tech Stack

- **UI**: Jetpack Compose & Material 3
- **Language**: Kotlin 2.2.10
- **Build System**: Gradle 9.3.1 with Android Gradle Plugin 9.1.1
- **Media Playback**: AndroidX Media3 ExoPlayer
- **Image Loading**: Coil Compose
- **Networking**: OkHttp
