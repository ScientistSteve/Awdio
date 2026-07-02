package com.example.musicplayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.data.MusicRepository
import com.example.musicplayer.domain.*
import com.example.musicplayer.playback.PlaybackController
import kotlinx.coroutines.flow.*

data class MainUiState(
    val songs: List<Song> = emptyList(), val blacklistedSongs: List<Song> = emptyList(), val favorites: Set<String> = emptySet(), val playlists: List<Playlist> = emptyList(), val playback: PlaybackController.PlaybackState = PlaybackController.PlaybackState(), val searchQuery: String = "", val libraryTab: LibraryTab = LibraryTab.Songs, val themeMode: ThemeMode = ThemeMode.System, val permissionState: LibraryPermissionState = LibraryPermissionState.Unknown
) { val filteredSongs: List<Song> get() = songs.filter { searchQuery.isBlank() || it.title.contains(searchQuery, true) || it.artist.contains(searchQuery, true) || it.album.contains(searchQuery, true) } }

class MainViewModel(private val repo: MusicRepository, private val playback: PlaybackController) : ViewModel() {
    private val query = MutableStateFlow(""); private val tab = MutableStateFlow(LibraryTab.Songs); private val theme = MutableStateFlow(ThemeMode.System); private val permission = MutableStateFlow(LibraryPermissionState.Unknown)
    val uiState: StateFlow<MainUiState> = combine(repo.songs, repo.blacklistedSongs, repo.favorites, repo.playlists, playback.state, query, tab, theme, permission) { a -> MainUiState(a[0] as List<Song>, a[1] as List<Song>, a[2] as Set<String>, a[3] as List<Playlist>, a[4] as PlaybackController.PlaybackState, a[5] as String, a[6] as LibraryTab, a[7] as ThemeMode, a[8] as LibraryPermissionState) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainUiState())
    init { playback.connect() }
    fun permission(state: LibraryPermissionState) { permission.value = state; if (state == LibraryPermissionState.Granted) { repo.refreshLibrary(); playback.setQueue(repo.songs.value) } }
    fun refreshLibrary() = repo.refreshLibrary()
    fun search(q: String) { query.value = q }; fun tab(t: LibraryTab) { tab.value = t }; fun theme(t: ThemeMode) { theme.value = t }
    fun play(song: Song) = playback.playSong(song); fun playPause() = playback.playPause(); fun next() = playback.next(); fun previous() = playback.previous(); fun seek(ms: Long) = playback.seekTo(ms); fun shuffle() = playback.toggleShuffle(); fun repeat() = playback.cycleRepeat(); fun favorite(id: String) = repo.toggleFavorite(id); fun hide(id: String) = repo.blacklistSong(id); fun restore(id: String) = repo.restoreSong(id)
    fun createPlaylist(n: String) = repo.createPlaylist(n); fun deletePlaylist(id: String) = repo.deletePlaylist(id); fun addToPlaylist(pid: String, sid: String) = repo.addToPlaylist(pid, sid); fun removeFromPlaylist(pid: String, sid: String) = repo.removeFromPlaylist(pid, sid)
    fun bass() = playback.toggleBassBoost(); fun sleep(m: Int?) = playback.setSleepTimer(m); fun removeQueue(id: String) = playback.removeFromQueue(id)
}
class MainViewModelFactory(private val r: MusicRepository, private val p: PlaybackController) : ViewModelProvider.Factory { override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(r,p) as T }
