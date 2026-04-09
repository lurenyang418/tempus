# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Tempus is an open-source Android music client for Subsonic-compatible servers (Navidrome, LMS, Gonic, Airsonic, etc.). It's a fork of [Tempo](https://github.com/CappielloAntonio) (v3.9.0).

The codebase is in transition from Java to Kotlin (approximately 120 Java files remaining, ~230 Kotlin files converted).

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Clean build
./gradlew clean

# Build with specific ABI split (arm64-v8a or armeabi-v7a)
./gradlew assembleArm64Debug
```

## Architecture

### Package Structure
```
com.cappielloantonio.tempo/
├── ui/                    # UI layer
│   ├── activity/          # Activities (MainActivity, LoginActivity, etc.)
│   ├── fragment/          # Fragments (Home, Album, Artist, Player, etc.)
│   ├── adapter/          # RecyclerView adapters
│   ├── dialog/           # Dialogs (Rating, Playlist, Download, etc.)
│   └── controller/       # UI controllers (BottomSheet, etc.)
├── viewmodel/            # ViewModels (MVVM)
├── service/              # Android services (MediaManager, DownloaderService)
├── repository/           # Data repositories (Album, Artist, Song, Playlist, etc.)
├── database/             # Room database
│   ├── dao/             # Data Access Objects
│   └── converter/       # Type converters
├── subsonic/            # Subsonic API integration
│   ├── api/             # Retrofit service interfaces grouped by domain
│   │   ├── browsing/
│   │   ├── mediaretrieval/
│   │   ├── playlist/
│   │   ├── searching/
│   │   └── ...
│   ├── models/          # Subsonic API response models
│   ├── base/           # Base classes (ApiResponse)
│   └── utils/          # Utilities
├── model/               # App-level models (Queue, Download, Favorite, etc.)
├── navigation/         # Navigation controller and helper
├── glide/              # Glide module for image loading
└── util/               # Utilities (Preferences, MappingUtil, etc.)
```

### Key Technologies
- **Media Playback**: Media3 (ExoPlayer) with MediaBrowser service
- **Database**: Room with auto-migrations
- **Networking**: Retrofit + Gson for Subsonic API
- **Image Loading**: Glide
- **Navigation**: AndroidX Navigation Component with NavController
- **DI**: Manual dependency injection via Application singletons

### API Design
Subsonic API calls are organized by domain into service interfaces (e.g., `BrowsingService`, `MediaRetrievalService`, `PlaylistService`). Each has a corresponding Client class that provides type-safe access. See `RetrofitClient.kt` for the singleton instance.

### Build Flavors
- Only one flavor (no Google Play services, no Cast)

## Development Notes

### Current Migration Status
The project is mid-migration from Java to Kotlin. Use Android Studio's "Convert Java to Kotlin" feature for remaining Java files. When contributing, prefer writing new code in Kotlin.

### Media Playback
`MediaManager.java` handles MediaBrowser connections and playback state. `BaseMediaService.kt` provides the MediaSession service.

### Database Schema
Room database `AppDatabase` uses auto-migrations. Models in `model/` and `subsonic/models/` are entities.

### ViewBinding
All UI components use ViewBinding (enabled in build.gradle). Access views via `binding.viewName`.

### Preferences
App preferences are accessed via `Preferences.java` utility class wrapping SharedPreferences.
