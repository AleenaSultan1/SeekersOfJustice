package com.seekersofjustice.app.ui.casedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import com.seekersofjustice.app.App
import com.seekersofjustice.app.data.repo.CaseDetails
import com.seekersofjustice.app.data.repo.CaseRepository
import com.seekersofjustice.app.transcription.TranscriptionQueue
import com.seekersofjustice.app.data.repo.MemoRepository
import com.seekersofjustice.app.ui.navigation.CaseDetailRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CaseDetailViewModel(
    savedStateHandle: SavedStateHandle,
    caseRepository: CaseRepository,
    private val memoRepository: MemoRepository,
    private val transcriptionQueue: TranscriptionQueue,
) : ViewModel() {

    val caseId: Long = savedStateHandle.toRoute<CaseDetailRoute>().caseId

    val details: StateFlow<CaseDetails?> = caseRepository.observeCaseDetails(caseId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun retryMemo(memoId: Long) {
        viewModelScope.launch {
            memoRepository.markPending(memoId)
            transcriptionQueue.enqueue(memoId)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as App
                CaseDetailViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    caseRepository = app.container.caseRepository,
                    memoRepository = app.container.memoRepository,
                    transcriptionQueue = app.container.transcriptionQueue,
                )
            }
        }
    }
}
