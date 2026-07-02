package com.example.musicplayer.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.musicplayer.data.MusicRepository
import com.example.musicplayer.domain.RepeatMode
import com.example.musicplayer.domain.Song
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class PlaybackController(private val context: Context, private val repository: MusicRepository) {
    data class PlaybackState(val currentSong: Song? = null, val isPlaying: Boolean = false, val positionMs: Long = 0, val durationMs: Long = 0, val queue: List<Song> = emptyList(), val shuffle: Boolean = false, val repeatMode: RepeatMode = RepeatMode.Off, val sleepTimerMinutes: Int? = null, val bassBoost: Boolean = false)
    private var controller: MediaController? = null
    private val _state = MutableStateFlow(PlaybackState(queue = repository.songs.value))
    val state: StateFlow<PlaybackState> = _state

    fun connect() {
        if (controller != null) return
        val future = MediaController.Builder(context, SessionToken(context, ComponentName(context, MusicService::class.java))).buildAsync()
        future.addListener({
            controller = future.get().also { player ->
                player.addListener(object : Player.Listener {
                    override fun onEvents(player: Player, events: Player.Events) { sync(player) }
                })
                setQueue(repository.songs.value)
                sync(player)
            }
        }, MoreExecutors.directExecutor())
    }
    private fun sync(player: Player) = _state.update { s -> s.copy(isPlaying = player.isPlaying, positionMs = player.currentPosition.coerceAtLeast(0), durationMs = if (player.duration > 0) player.duration else s.currentSong?.durationMs ?: 0, currentSong = repository.songs.value.getOrNull(player.currentMediaItemIndex) ?: s.currentSong) }
    fun tick() { controller?.let(::sync) }
    fun setQueue(songs: List<Song>, start: Int = 0, play: Boolean = false) {
        val items = songs.map { MediaItem.Builder().setUri(it.uri).setMediaId(it.id).setMediaMetadata(MediaMetadata.Builder().setTitle(it.title).setArtist(it.artist).setAlbumTitle(it.album).build()).build() }
        controller?.setMediaItems(items, start, 0); controller?.prepare(); if (play) controller?.play()
        _state.update { it.copy(queue = songs, currentSong = songs.getOrNull(start)) }
    }
    fun playSong(song: Song) { val songs = repository.songs.value; setQueue(songs, songs.indexOfFirst { it.id == song.id }.coerceAtLeast(0), true) }
    fun playPause() { controller?.let { if (it.isPlaying) it.pause() else it.play() } }
    fun next() { controller?.seekToNextMediaItem() }
    fun previous() { controller?.seekToPreviousMediaItem() }
    fun seekTo(ms: Long) { controller?.seekTo(ms) }
    fun toggleShuffle() { controller?.shuffleModeEnabled = !(controller?.shuffleModeEnabled ?: false); _state.update { it.copy(shuffle = !it.shuffle) } }
    fun cycleRepeat() { val next = when (_state.value.repeatMode) { RepeatMode.Off -> RepeatMode.All; RepeatMode.All -> RepeatMode.One; RepeatMode.One -> RepeatMode.Off }; controller?.repeatMode = when (next) { RepeatMode.Off -> Player.REPEAT_MODE_OFF; RepeatMode.One -> Player.REPEAT_MODE_ONE; RepeatMode.All -> Player.REPEAT_MODE_ALL }; _state.update { it.copy(repeatMode = next) } }
    fun removeFromQueue(id: String) { _state.update { s -> s.copy(queue = s.queue.filterNot { it.id == id }) } }
    fun moveQueue(from: Int, to: Int) { _state.update { s -> val m = s.queue.toMutableList(); if (from in m.indices && to in m.indices) m.add(to, m.removeAt(from)); s.copy(queue = m) } }
    fun setSleepTimer(minutes: Int?) { _state.update { it.copy(sleepTimerMinutes = minutes) } }
    fun toggleBassBoost() { _state.update { it.copy(bassBoost = !it.bassBoost) } }
}
