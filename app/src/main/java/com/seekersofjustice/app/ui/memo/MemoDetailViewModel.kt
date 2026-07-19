package com.seekersofjustice.app.ui.memo

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.seekersofjustice.app.App
import com.seekersofjustice.app.audio.MemoPlayer
import com.seekersofjustice.app.data.repo.MemoRepository
import com.seekersofjustice.app.domain.model.Memo
import com.seekersofjustice.app.transcription.TranscriptionQueue
import com.seekersofjustice.app.ui.navigation.MemoDetailRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MemoDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val memoRepository: MemoRepository,
    private val transcriptionQueue: TranscriptionQueue,
) : ViewModel() {

    private val memoId: Long = savedStateHandle.toRoute<MemoDetailRoute>().memoId

    val memo: StateFlow<Memo?> = memoRepository.observeMemo(memoId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val player = MemoPlayer(viewModelScope)

    private var loadedPath: String? = null

    fun ensurePlayerLoaded(path: String) {
        if (loadedPath == path) return
        try {
            player.load(path)
            loadedPath = path
        } catch (_: Exception) {
            // Missing/corrupt audio file: playback controls stay inert.
        }
    }

    fun retry() {
        viewModelScope.launch {
            memoRepository.markPending(memoId)
            transcriptionQueue.enqueue(memoId)
        }
    }

    override fun onCleared() {
        player.release()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as App
                MemoDetailViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    memoRepository = app.container.memoRepository,
                    transcriptionQueue = app.container.transcriptionQueue,
                )
            }
        }
    }
}
