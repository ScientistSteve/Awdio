package com.example.musicplayer

import android.app.Application
import com.example.musicplayer.data.MusicRepository
import com.example.musicplayer.playback.PlaybackController

class AwdioApplication : Application() {
    val repository by lazy { MusicRepository(this) }
    val playbackController by lazy { PlaybackController(this, repository) }
}
