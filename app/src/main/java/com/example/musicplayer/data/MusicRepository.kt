package com.example.musicplayer.data

import com.example.musicplayer.domain.Playlist
import com.example.musicplayer.domain.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class MusicRepository {
    private val _songs = MutableStateFlow(sampleSongs)
    val songs: StateFlow<List<Song>> = _songs
    private val _favorites = MutableStateFlow(setOf("song-1", "song-4"))
    val favorites: StateFlow<Set<String>> = _favorites
    private val _playlists = MutableStateFlow(listOf(Playlist("pl-1", "Morning Focus", listOf("song-1", "song-3")), Playlist("pl-2", "Night Drive", listOf("song-2", "song-5"))))
    val playlists: StateFlow<List<Playlist>> = _playlists

    fun toggleFavorite(id: String) = _favorites.update { if (id in it) it - id else it + id }
    fun createPlaylist(name: String) = _playlists.update { it + Playlist("pl-${System.nanoTime()}", name.ifBlank { "New Playlist" }, emptyList()) }
    fun renamePlaylist(id: String, name: String) = _playlists.update { list -> list.map { if (it.id == id) it.copy(name = name.ifBlank { it.name }) else it } }
    fun deletePlaylist(id: String) = _playlists.update { it.filterNot { playlist -> playlist.id == id } }
    fun addToPlaylist(playlistId: String, songId: String) = _playlists.update { list -> list.map { if (it.id == playlistId && songId !in it.songIds) it.copy(songIds = it.songIds + songId) else it } }
    fun removeFromPlaylist(playlistId: String, songId: String) = _playlists.update { list -> list.map { if (it.id == playlistId) it.copy(songIds = it.songIds - songId) else it } }

    companion object {
        val sampleSongs = listOf(
            Song("song-1", "SoundHelix One", "T. Schürger", "Sample Waves", 372_000, 0x6750A4, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"),
            Song("song-2", "SoundHelix Two", "T. Schürger", "Night Signals", 431_000, 0x006C4C, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"),
            Song("song-3", "SoundHelix Three", "T. Schürger", "Sample Waves", 345_000, 0x7D5260, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"),
            Song("song-4", "SoundHelix Four", "T. Schürger", "City Lights", 305_000, 0xB3261E, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3"),
            Song("song-5", "SoundHelix Five", "T. Schürger", "Night Signals", 392_000, 0x386A20, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3")
        )
    }
}
