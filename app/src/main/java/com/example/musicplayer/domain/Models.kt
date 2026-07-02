package com.example.musicplayer.domain

data class Song(val id: String, val title: String, val artist: String, val album: String, val durationMs: Long, val artworkSeed: Long, val uri: String)
data class Playlist(val id: String, val name: String, val songIds: List<String>)
enum class RepeatMode { Off, One, All }
enum class LibraryTab { Songs, Albums, Artists, Playlists }
enum class ThemeMode { System, Light, Dark }
