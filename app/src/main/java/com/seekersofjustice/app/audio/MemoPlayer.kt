package com.seekersofjustice.app.audio

import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Thin MediaPlayer wrapper with a polled position StateFlow. */
class MemoPlayer(private val scope: CoroutineScope) {

    data class PlaybackState(
        val isPlaying: Boolean = false,
        val positionMs: Long = 0,
        val durationMs: Long = 0,
    )

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state

    private var player: MediaPlayer? = null
    private var pollJob: Job? = null

    fun load(path: String) {
        release()
        player = MediaPlayer().apply {
            setDataSource(path)
            prepare()
            setOnCompletionListener {
                _state.value = _state.value.copy(isPlaying = false, positionMs = 0)
            }
        }
        _state.value = PlaybackState(durationMs = player!!.duration.toLong())
    }

    fun playPause() {
        val p = player ?: return
        if (p.isPlaying) {
            p.pause()
            _state.value = _state.value.copy(isPlaying = false)
            pollJob?.cancel()
        } else {
            p.start()
            _state.value = _state.value.copy(isPlaying = true)
            pollJob?.cancel()
            pollJob = scope.launch {
                while (isActive && player?.isPlaying == true) {
                    _state.value = _state.value.copy(positionMs = player!!.currentPosition.toLong())
                    delay(200)
                }
            }
        }
    }

    fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs.toInt())
        _state.value = _state.value.copy(positionMs = positionMs)
    }

    fun release() {
        pollJob?.cancel()
        player?.release()
        player = null
        _state.value = PlaybackState()
    }
}
