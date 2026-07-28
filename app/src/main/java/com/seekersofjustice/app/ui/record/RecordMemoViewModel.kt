package com.seekersofjustice.app.ui.record

import android.content.Context
import android.net.Uri
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
import com.seekersofjustice.app.audio.AudioImporter
import com.seekersofjustice.app.audio.WavRecorder
import com.seekersofjustice.app.data.repo.CaseRepository
import com.seekersofjustice.app.data.repo.MemoRepository
import com.seekersofjustice.app.domain.model.Memo
import com.seekersofjustice.app.domain.model.MemoLanguage
import com.seekersofjustice.app.domain.model.MemoStatus
import com.seekersofjustice.app.domain.model.RelationToCrime
import com.seekersofjustice.app.transcription.TranscriptionQueue
import com.seekersofjustice.app.ui.navigation.RecordMemoRoute
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class RecordMemoViewModel(
    savedStateHandle: SavedStateHandle,
    private val memoRepository: MemoRepository,
    private val caseRepository: CaseRepository,
    private val transcriptionQueue: TranscriptionQueue,
    private val audioImporter: AudioImporter,
    private val currentUserId: () -> Long?,
) : ViewModel() {

    private val caseId: Long = savedStateHandle.toRoute<RecordMemoRoute>().caseId

    sealed interface Step {
        data object Idle : Step
        data object Recording : Step
        data class Ready(val file: File, val durationMs: Long) : Step
    }

    val recorder = WavRecorder()

    private val _step = MutableStateFlow<Step>(Step.Idle)
    val step: StateFlow<Step> = _step

    private val _language = MutableStateFlow(MemoLanguage.URDU)
    val language: StateFlow<MemoLanguage> = _language

    // Speaker defaults to the case's primary person; the officer can change it
    // so one case can hold statements from different people.
    private val _speakerName = MutableStateFlow("")
    val speakerName: StateFlow<String> = _speakerName

    private val _speakerRelation = MutableStateFlow(RelationToCrime.WITNESS)
    val speakerRelation: StateFlow<RelationToCrime> = _speakerRelation

    private val _importError = MutableStateFlow(false)
    val importError: StateFlow<Boolean> = _importError

    init {
        viewModelScope.launch {
            caseRepository.observeCaseDetails(caseId).first()?.case?.let { case ->
                _speakerName.value = case.fullName
                _speakerRelation.value = case.relation
            }
        }
    }

    fun setLanguage(language: MemoLanguage) {
        _language.value = language
    }

    fun setSpeakerName(name: String) {
        _speakerName.value = name
    }

    fun setSpeakerRelation(relation: RelationToCrime) {
        _speakerRelation.value = relation
    }

    fun clearImportError() {
        _importError.value = false
    }

    fun startRecording(context: Context) {
        if (_step.value != Step.Idle) return
        val file = File(File(context.filesDir, "audio"), "memo_${UUID.randomUUID()}.wav")
        recorder.start(viewModelScope, file)
        _step.value = Step.Recording
    }

    fun stopRecording() {
        viewModelScope.launch {
            val result = recorder.stop()
            _step.value = result
                ?.let { (file, durationMs) -> Step.Ready(file, durationMs) }
                ?: Step.Idle
        }
    }

    fun importFile(uri: Uri) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { audioImporter.import(uri) }
            when (result) {
                is AudioImporter.Result.Success ->
                    _step.value = Step.Ready(result.file, result.durationMs)
                AudioImporter.Result.UnsupportedFile -> _importError.value = true
            }
        }
    }

    fun discard() {
        (_step.value as? Step.Ready)?.file?.delete()
        _step.value = Step.Idle
    }

    fun save(onSaved: () -> Unit) {
        val ready = _step.value as? Step.Ready ?: return
        viewModelScope.launch {
            val memoId = memoRepository.addMemo(
                Memo(
                    caseId = caseId,
                    audioPath = ready.file.absolutePath,
                    durationMs = ready.durationMs,
                    language = _language.value,
                    status = MemoStatus.PENDING,
                    speakerName = _speakerName.value.trim().ifBlank { null },
                    speakerRelation = _speakerRelation.value,
                    recordedByUserId = currentUserId(),
                    createdAt = System.currentTimeMillis(),
                )
            )
            transcriptionQueue.enqueue(memoId)
            _step.value = Step.Idle
            onSaved()
        }
    }

    override fun onCleared() {
        // Abandoning the screen mid-recording: stop hardware and drop the file.
        if (_step.value == Step.Recording) {
            recorder.abort()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as App
                RecordMemoViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    memoRepository = app.container.memoRepository,
                    caseRepository = app.container.caseRepository,
                    transcriptionQueue = app.container.transcriptionQueue,
                    audioImporter = app.container.audioImporter,
                    currentUserId = { app.container.currentOfficer.value?.id },
                )
            }
        }
    }
}
