# Awdio

Awdio is a native Android music player built with Kotlin, Jetpack Compose, Material 3, MVVM, StateFlow, Compose Navigation, and Media3/ExoPlayer.

## Implemented feature plan and architecture

- **Manual DI**: `AwdioApplication` owns the repository and playback controller. This is simpler than Hilt for a compact single-module app while retaining clear dependency boundaries.
- **Data**: `MusicRepository` queries the device audio library through `MediaStore`, filters user-hidden songs with a persisted DataStore blacklist, and exposes songs, favorites, and playlists with `StateFlow`.
- **Playback**: `PlaybackController` talks to a foreground `MediaSessionService` backed by ExoPlayer so playback can continue in the background and expose media notification/lock-screen controls through Media3.
- **UI**: Compose Material 3 screens consume a single `MainUiState` from `MainViewModel` using unidirectional data flow.

## Features

- Library screen with Songs, Albums, Artists, and Playlists tabs.
- Persistent mini-player with artwork, metadata, and play/pause; tap it to open Now Playing.
- Now Playing screen with large artwork, scrubber, previous/play-next, shuffle, repeat, queue, bass-boost toggle, and sleep timer chips.
- Dedicated live search across title, artist, and album with empty/no-results states.
- Playlists: create/delete and add/remove songs.
- Favorites/liked songs.
- Queue view with remove support.
- Dynamic Material You color on Android 12+ with static light/dark fallback and a theme mode menu.

## Build and run locally

```bash
export JAVA_HOME=/path/to/jdk17
./gradlew assembleDebug
```

Install the generated debug APK from `app/build/outputs/apk/debug/`. The build is restricted to `arm64-v8a` through `ndk.abiFilters` in `app/build.gradle.kts`.

## CI

`.github/workflows/build.yml` runs on pushes and pull requests to `main`, plus manual dispatch. It sets up Temurin 17 and the Android SDK, uses Gradle dependency caching via `actions/setup-java`, runs `./gradlew assembleDebug`, and uploads the debug APK artifact. Release signing is not configured because no keystore is present; to add it, create a keystore in GitHub Secrets, decode it during CI, and wire a `signingConfigs.release` block in `app/build.gradle.kts`.

## Known limitations

- Audio is loaded from the device library via MediaStore after the app receives the appropriate runtime media/storage permission.
- Bass boost is represented as an app-level toggle in this UI; production audio effects would need device-session-specific `AudioEffect` handling.
- Queue reordering is modeled in the ViewModel layer; a drag-and-drop UI can be added later.
