package com.seekersofjustice.app.ui.cases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.seekersofjustice.app.App
import com.seekersofjustice.app.data.repo.AddCaseResult
import com.seekersofjustice.app.data.repo.CaseRepository
import com.seekersofjustice.app.domain.model.Case
import com.seekersofjustice.app.domain.model.RelationToCrime
import com.seekersofjustice.app.domain.model.Sex
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CaseListViewModel(private val caseRepository: CaseRepository) : ViewModel() {

    val cases: StateFlow<List<Case>> = caseRepository.observeCases()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** True while the add-case dialog should show a duplicate-FIR error. */
    private val _duplicateFirError = MutableStateFlow(false)
    val duplicateFirError: StateFlow<Boolean> = _duplicateFirError

    fun clearDuplicateFirError() {
        _duplicateFirError.value = false
    }

    fun deleteCase(caseId: Long) {
        viewModelScope.launch { caseRepository.deleteCase(caseId) }
    }

    fun addCase(
        firNumber: String,
        fullName: String,
        age: Int,
        sex: Sex,
        relation: RelationToCrime,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            val result = caseRepository.addCase(
                Case(
                    firNumber = firNumber.trim(),
                    fullName = fullName.trim(),
                    age = age,
                    sex = sex,
                    relation = relation,
                    createdAt = System.currentTimeMillis(),
                )
            )
            when (result) {
                is AddCaseResult.Success -> {
                    _duplicateFirError.value = false
                    onSuccess()
                }
                AddCaseResult.DuplicateFir -> _duplicateFirError.value = true
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as App
                CaseListViewModel(app.container.caseRepository)
            }
        }
    }
}
