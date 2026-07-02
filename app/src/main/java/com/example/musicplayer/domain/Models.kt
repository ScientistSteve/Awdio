package com.example.musicplayer.domain

data class Song(val id: String, val title: String, val artist: String, val album: String, val durationMs: Long, val artworkUri: String?, val uri: String, val year: Int? = null)
data class Playlist(val id: String, val name: String, val songIds: List<String>)
enum class RepeatMode { Off, One, All }
enum class LibraryTab { Songs, Albums, Artists, Playlists }
enum class ThemeMode { System, Light, Dark }
enum class LibraryPermissionState { Unknown, Granted, Denied, PermanentlyDenied }
