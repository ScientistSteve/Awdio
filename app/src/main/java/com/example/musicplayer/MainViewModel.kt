package com.example.musicplayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.data.MusicRepository
import com.example.musicplayer.domain.*
import com.example.musicplayer.playback.PlaybackController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MainUiState(val songs: List<Song> = emptyList(), val favorites: Set<String> = emptySet(), val playlists: List<Playlist> = emptyList(), val playback: PlaybackController.PlaybackState = PlaybackController.PlaybackState(), val searchQuery: String = "", val libraryTab: LibraryTab = LibraryTab.Songs, val themeMode: ThemeMode = ThemeMode.System) {
    val filteredSongs get() = if (searchQuery.isBlank()) songs else songs.filter { listOf(it.title, it.artist, it.album).any { f -> f.contains(searchQuery, true) } }
}
class MainViewModel(private val repo: MusicRepository, private val playback: PlaybackController) : ViewModel() {
    private val query = MutableStateFlow(""); private val tab = MutableStateFlow(LibraryTab.Songs); private val theme = MutableStateFlow(ThemeMode.System)
    val uiState: StateFlow<MainUiState> = combine(repo.songs, repo.favorites, repo.playlists, playback.state, query, tab, theme) { a -> MainUiState(a[0] as List<Song>, a[1] as Set<String>, a[2] as List<Playlist>, a[3] as PlaybackController.PlaybackState, a[4] as String, a[5] as LibraryTab, a[6] as ThemeMode) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainUiState())
    init { playback.connect(); viewModelScope.launch { while (true) { playback.tick(); delay(1000) } } }
    fun search(v: String) { query.value = v }; fun tab(v: LibraryTab) { tab.value = v }; fun theme(v: ThemeMode) { theme.value = v }
    fun play(song: Song) = playback.playSong(song); fun playPause() = playback.playPause(); fun next() = playback.next(); fun previous() = playback.previous(); fun seek(v: Long) = playback.seekTo(v)
    fun favorite(id: String) = repo.toggleFavorite(id); fun shuffle() = playback.toggleShuffle(); fun repeat() = playback.cycleRepeat(); fun sleep(m: Int?) = playback.setSleepTimer(m); fun bass() = playback.toggleBassBoost()
    fun createPlaylist(n: String) = repo.createPlaylist(n); fun renamePlaylist(id: String, n: String) = repo.renamePlaylist(id, n); fun deletePlaylist(id: String) = repo.deletePlaylist(id); fun addToPlaylist(pid: String, sid: String) = repo.addToPlaylist(pid, sid); fun removeFromPlaylist(pid: String, sid: String) = repo.removeFromPlaylist(pid, sid)
    fun removeQueue(id: String) = playback.removeFromQueue(id); fun moveQueue(from: Int, to: Int) = playback.moveQueue(from, to)
}
class MainViewModelFactory(private val repo: MusicRepository, private val playback: PlaybackController) : ViewModelProvider.Factory { override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(repo, playback) as T }
