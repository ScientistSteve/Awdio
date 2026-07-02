package com.example.musicplayer.data

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.musicplayer.domain.Playlist
import com.example.musicplayer.domain.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore("awdio_library")

class MusicRepository(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val blacklistKey = stringSetPreferencesKey("blacklisted_song_ids")
    private val playlistsKey = stringSetPreferencesKey("playlists")
    private val favoritesKey = stringSetPreferencesKey("favorites")
    private val allSongs = MutableStateFlow<List<Song>>(emptyList())

    val blacklistedIds: StateFlow<Set<String>> = context.dataStore.data.map { it[blacklistKey] ?: emptySet() }.stateIn(scope, SharingStarted.Eagerly, emptySet())
    val songs: StateFlow<List<Song>> = combine(allSongs, blacklistedIds) { songs, blocked -> songs.filterNot { it.id in blocked } }.stateIn(scope, SharingStarted.Eagerly, emptyList())
    val blacklistedSongs: StateFlow<List<Song>> = combine(allSongs, blacklistedIds) { songs, blocked -> songs.filter { it.id in blocked } }.stateIn(scope, SharingStarted.Eagerly, emptyList())
    val favorites: StateFlow<Set<String>> = context.dataStore.data.map { it[favoritesKey] ?: emptySet() }.stateIn(scope, SharingStarted.Eagerly, emptySet())
    val playlists: StateFlow<List<Playlist>> = context.dataStore.data.map { prefs -> (prefs[playlistsKey] ?: emptySet()).mapNotNull(::decodePlaylist).sortedBy { it.name } }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun refreshLibrary() { scope.launch { allSongs.value = querySongs() } }
    fun toggleFavorite(id: String) { scope.launch { context.dataStore.edit { p -> val cur = p[favoritesKey] ?: emptySet(); p[favoritesKey] = if (id in cur) cur - id else cur + id } } }
    fun blacklistSong(id: String) { scope.launch { context.dataStore.edit { it[blacklistKey] = (it[blacklistKey] ?: emptySet()) + id } } }
    fun restoreSong(id: String) { scope.launch { context.dataStore.edit { it[blacklistKey] = (it[blacklistKey] ?: emptySet()) - id } } }
    fun createPlaylist(name: String) = updatePlaylists { it + Playlist("pl-${System.nanoTime()}", name.ifBlank { "New Playlist" }, emptyList()) }
    fun renamePlaylist(id: String, name: String) = updatePlaylists { list -> list.map { if (it.id == id) it.copy(name = name.ifBlank { it.name }) else it } }
    fun deletePlaylist(id: String) = updatePlaylists { it.filterNot { p -> p.id == id } }
    fun addToPlaylist(playlistId: String, songId: String) = updatePlaylists { list -> list.map { if (it.id == playlistId && songId !in it.songIds) it.copy(songIds = it.songIds + songId) else it } }
    fun removeFromPlaylist(playlistId: String, songId: String) = updatePlaylists { list -> list.map { if (it.id == playlistId) it.copy(songIds = it.songIds - songId) else it } }

    private fun updatePlaylists(block: (List<Playlist>) -> List<Playlist>) { scope.launch { context.dataStore.edit { p -> p[playlistsKey] = block((p[playlistsKey] ?: emptySet()).mapNotNull(::decodePlaylist)).map(::encodePlaylist).toSet() } } }
    private fun encodePlaylist(p: Playlist) = listOf(p.id, p.name, p.songIds.joinToString(",")).joinToString("|") { Uri.encode(it) }
    private fun decodePlaylist(raw: String): Playlist? = runCatching { val parts = raw.split("|").map { Uri.decode(it) }; Playlist(parts[0], parts[1], parts.getOrNull(2)?.split(',')?.filter { it.isNotBlank() } ?: emptyList()) }.getOrNull()

    private fun querySongs(): List<Song> {
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.YEAR)
        val sort = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
        val out = mutableListOf<Song>()
        context.contentResolver.query(uri, projection, "${MediaStore.Audio.Media.IS_MUSIC}!=0", null, sort)?.use { c ->
            val id = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID); val title = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE); val artist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST); val album = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM); val duration = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION); val year = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            while (c.moveToNext()) { val sid = c.getLong(id); val content = Uri.withAppendedPath(uri, sid.toString()).toString(); out += Song(sid.toString(), c.getString(title) ?: "Unknown title", c.getString(artist) ?: "Unknown artist", c.getString(album) ?: "Unknown album", c.getLong(duration), "content://media/external/audio/media/$sid/albumart", content, c.getInt(year).takeIf { it > 0 }) }
        }
        return out
    }
}
